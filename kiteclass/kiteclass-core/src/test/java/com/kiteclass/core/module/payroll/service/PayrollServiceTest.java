package com.kiteclass.core.module.payroll.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.payroll.entity.PayrollConfig;
import com.kiteclass.core.module.payroll.entity.PayrollPeriod;
import com.kiteclass.core.module.payroll.enums.PayrollStatus;
import com.kiteclass.core.module.payroll.enums.PayrollType;
import com.kiteclass.core.module.payroll.repository.PayrollConfigRepository;
import com.kiteclass.core.module.payroll.repository.PayrollPeriodRepository;
import com.kiteclass.core.module.payroll.service.impl.PayrollServiceImpl;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PayrollServiceImpl} — Phase 1 HOURLY-only calculation.
 *
 * <p>Coverage:
 * <ul>
 *   <li>HOURLY happy path with whole + fractional hours</li>
 *   <li>HOURLY zero hours edge case</li>
 *   <li>HALF_EVEN rounding (banker's rounding) on grossAmount</li>
 *   <li>endDate &lt; startDate validation</li>
 *   <li>PayrollConfig missing → EntityNotFoundException</li>
 *   <li>Non-HOURLY type → UnsupportedOperationException (Phase 2 deferral)</li>
 *   <li>HOURLY with hourlyRate null → ValidationException (BR-PAYROLL-002)</li>
 *   <li>Persisted PayrollPeriod has DRAFT status + deductions=0 + netAmount=grossAmount</li>
 *   <li>Read-only listing endpoints (find by id, find filtered)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayrollService Phase 1 (HOURLY only)")
class PayrollServiceTest {

    @Mock
    private PayrollConfigRepository payrollConfigRepository;

    @Mock
    private PayrollPeriodRepository payrollPeriodRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private TeacherClassRepository teacherClassRepository;

    @InjectMocks
    private PayrollServiceImpl payrollService;

    private UUID tenantId;
    private Long teacherId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        teacherId = 42L;
        periodStart = LocalDate.of(2026, 5, 1);
        periodEnd = LocalDate.of(2026, 5, 31);
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ============= HOURLY happy path =============

    @Nested
    @DisplayName("calculate(HOURLY)")
    class HourlyCalculation {

        @Test
        @DisplayName("should compute gross = hours * rate for whole hours")
        void hourlyWholeHours() {
            // Given: 4 sessions × 2.0h each = 8 hours, rate 200,000 VND/h → 1,600,000 VND
            PayrollConfig config = hourlyConfig(new BigDecimal("200000.00"));
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));
            when(teacherClassRepository.findByTeacherId(teacherId))
                    .thenReturn(List.of(teacherClass(teacherId, 100L)));
            when(classSessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(100L))
                    .thenReturn(List.of(
                            session(100L, periodStart.plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0)),
                            session(100L, periodStart.plusDays(8), LocalTime.of(8, 0), LocalTime.of(10, 0)),
                            session(100L, periodStart.plusDays(15), LocalTime.of(8, 0), LocalTime.of(10, 0)),
                            session(100L, periodStart.plusDays(22), LocalTime.of(8, 0), LocalTime.of(10, 0))
                    ));
            when(payrollPeriodRepository.save(any(PayrollPeriod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            PayrollPeriod result = payrollService.calculate(teacherId, periodStart, periodEnd);

            // Then
            assertThat(result.getHoursWorked()).isEqualByComparingTo("8.00");
            assertThat(result.getGrossAmount()).isEqualByComparingTo("1600000.00");
            assertThat(result.getDeductions()).isEqualByComparingTo("0.00");
            assertThat(result.getNetAmount()).isEqualByComparingTo("1600000.00");
            assertThat(result.getStatus()).isEqualTo(PayrollStatus.DRAFT);
            assertThat(result.getTeacherId()).isEqualTo(teacherId);
            assertThat(result.getStartDate()).isEqualTo(periodStart);
            assertThat(result.getEndDate()).isEqualTo(periodEnd);
        }

        @Test
        @DisplayName("should handle fractional hours (1.5h sessions)")
        void hourlyFractionalHours() {
            // Given: 3 sessions × 1.5h = 4.5 hours, rate 100,000 → 450,000
            PayrollConfig config = hourlyConfig(new BigDecimal("100000.00"));
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));
            when(teacherClassRepository.findByTeacherId(teacherId))
                    .thenReturn(List.of(teacherClass(teacherId, 100L)));
            when(classSessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(100L))
                    .thenReturn(List.of(
                            session(100L, periodStart.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 30)),
                            session(100L, periodStart.plusDays(8), LocalTime.of(8, 0), LocalTime.of(9, 30)),
                            session(100L, periodStart.plusDays(15), LocalTime.of(8, 0), LocalTime.of(9, 30))
                    ));
            when(payrollPeriodRepository.save(any(PayrollPeriod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            PayrollPeriod result = payrollService.calculate(teacherId, periodStart, periodEnd);

            // Then
            assertThat(result.getHoursWorked()).isEqualByComparingTo("4.50");
            assertThat(result.getGrossAmount()).isEqualByComparingTo("450000.00");
        }

        @Test
        @DisplayName("should produce zero gross when teacher has no sessions in period")
        void hourlyZeroHours() {
            // Given: teacher exists, has class assignment, but no sessions in period
            PayrollConfig config = hourlyConfig(new BigDecimal("200000.00"));
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));
            when(teacherClassRepository.findByTeacherId(teacherId))
                    .thenReturn(List.of(teacherClass(teacherId, 100L)));
            when(classSessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(100L))
                    .thenReturn(Collections.emptyList());
            when(payrollPeriodRepository.save(any(PayrollPeriod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            PayrollPeriod result = payrollService.calculate(teacherId, periodStart, periodEnd);

            // Then
            assertThat(result.getHoursWorked()).isEqualByComparingTo("0.00");
            assertThat(result.getGrossAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getNetAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getStatus()).isEqualTo(PayrollStatus.DRAFT);
        }

        @Test
        @DisplayName("should ignore sessions outside the period date range")
        void hourlyExcludesOutOfRangeSessions() {
            // Given: 1 session inside, 2 sessions outside (before + after)
            PayrollConfig config = hourlyConfig(new BigDecimal("100000.00"));
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));
            when(teacherClassRepository.findByTeacherId(teacherId))
                    .thenReturn(List.of(teacherClass(teacherId, 100L)));
            when(classSessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(100L))
                    .thenReturn(List.of(
                            session(100L, periodStart.minusDays(5), LocalTime.of(8, 0), LocalTime.of(10, 0)), // before
                            session(100L, periodStart.plusDays(5), LocalTime.of(8, 0), LocalTime.of(10, 0)),  // inside (2h)
                            session(100L, periodEnd.plusDays(5), LocalTime.of(8, 0), LocalTime.of(10, 0))     // after
                    ));
            when(payrollPeriodRepository.save(any(PayrollPeriod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            PayrollPeriod result = payrollService.calculate(teacherId, periodStart, periodEnd);

            // Then: only the in-range session counts
            assertThat(result.getHoursWorked()).isEqualByComparingTo("2.00");
            assertThat(result.getGrossAmount()).isEqualByComparingTo("200000.00");
        }

        @Test
        @DisplayName("should apply HALF_EVEN rounding to scale 2 (BR-PAYROLL-007)")
        void hourlyHalfEvenRounding() {
            // Given: 1 session × 1.5h × 333.33 VND/h = 499.995 → HALF_EVEN to 500.00
            PayrollConfig config = hourlyConfig(new BigDecimal("333.33"));
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));
            when(teacherClassRepository.findByTeacherId(teacherId))
                    .thenReturn(List.of(teacherClass(teacherId, 100L)));
            when(classSessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(100L))
                    .thenReturn(List.of(
                            session(100L, periodStart.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 30))
                    ));
            when(payrollPeriodRepository.save(any(PayrollPeriod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            PayrollPeriod result = payrollService.calculate(teacherId, periodStart, periodEnd);

            // Then: 1.5 * 333.33 = 499.995 → HALF_EVEN scale 2 → 500.00
            assertThat(result.getGrossAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("should sum hours across multiple class assignments")
        void hourlyMultipleClasses() {
            // Given: teacher assigned to 2 classes, 1 session each (2h + 3h = 5h)
            PayrollConfig config = hourlyConfig(new BigDecimal("100000.00"));
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));
            when(teacherClassRepository.findByTeacherId(teacherId))
                    .thenReturn(List.of(teacherClass(teacherId, 100L), teacherClass(teacherId, 200L)));
            when(classSessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(100L))
                    .thenReturn(List.of(session(100L, periodStart.plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0))));
            when(classSessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(200L))
                    .thenReturn(List.of(session(200L, periodStart.plusDays(2), LocalTime.of(14, 0), LocalTime.of(17, 0))));
            when(payrollPeriodRepository.save(any(PayrollPeriod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            PayrollPeriod result = payrollService.calculate(teacherId, periodStart, periodEnd);

            // Then
            assertThat(result.getHoursWorked()).isEqualByComparingTo("5.00");
            assertThat(result.getGrossAmount()).isEqualByComparingTo("500000.00");
        }

        @Test
        @DisplayName("should persist with DRAFT status, deductions=0, netAmount=grossAmount")
        void hourlyPersistsDraftWithZeroDeductions() {
            PayrollConfig config = hourlyConfig(new BigDecimal("100000.00"));
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));
            when(teacherClassRepository.findByTeacherId(teacherId))
                    .thenReturn(List.of(teacherClass(teacherId, 100L)));
            when(classSessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(100L))
                    .thenReturn(List.of(session(100L, periodStart.plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0))));
            ArgumentCaptor<PayrollPeriod> captor = ArgumentCaptor.forClass(PayrollPeriod.class);
            when(payrollPeriodRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            payrollService.calculate(teacherId, periodStart, periodEnd);

            // Then: persisted entity has DRAFT, zero deductions, net=gross
            PayrollPeriod saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(PayrollStatus.DRAFT);
            assertThat(saved.getDeductions()).isEqualByComparingTo("0.00");
            assertThat(saved.getNetAmount()).isEqualByComparingTo(saved.getGrossAmount());
        }
    }

    // ============= Validation =============

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should reject endDate before startDate")
        void rejectEndBeforeStart() {
            assertThatThrownBy(() ->
                    payrollService.calculate(teacherId, periodEnd, periodStart))
                    .isInstanceOf(ValidationException.class);
            verify(payrollPeriodRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject when PayrollConfig missing for teacher")
        void rejectMissingConfig() {
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    payrollService.calculate(teacherId, periodStart, periodEnd))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(payrollPeriodRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject HOURLY config with null hourlyRate (BR-PAYROLL-002)")
        void rejectHourlyNullRate() {
            PayrollConfig config = PayrollConfig.builder()
                    .teacherId(teacherId)
                    .type(PayrollType.HOURLY)
                    .hourlyRate(null)
                    .build();
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));

            assertThatThrownBy(() ->
                    payrollService.calculate(teacherId, periodStart, periodEnd))
                    .isInstanceOf(ValidationException.class);
            verify(payrollPeriodRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject SALARY type with UnsupportedOperationException (Phase 2)")
        void rejectSalaryType() {
            PayrollConfig config = PayrollConfig.builder()
                    .teacherId(teacherId)
                    .type(PayrollType.SALARY)
                    .baseSalary(new BigDecimal("10000000"))
                    .build();
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));

            assertThatThrownBy(() ->
                    payrollService.calculate(teacherId, periodStart, periodEnd))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("GAP-057b");
        }

        @Test
        @DisplayName("should reject COMMISSION type with UnsupportedOperationException (Phase 2)")
        void rejectCommissionType() {
            PayrollConfig config = PayrollConfig.builder()
                    .teacherId(teacherId)
                    .type(PayrollType.COMMISSION)
                    .commissionPercent(new BigDecimal("10"))
                    .build();
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));

            assertThatThrownBy(() ->
                    payrollService.calculate(teacherId, periodStart, periodEnd))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("GAP-057b");
        }

        @Test
        @DisplayName("should reject HYBRID type with UnsupportedOperationException (Phase 2)")
        void rejectHybridType() {
            PayrollConfig config = PayrollConfig.builder()
                    .teacherId(teacherId)
                    .type(PayrollType.HYBRID)
                    .baseSalary(new BigDecimal("5000000"))
                    .commissionPercent(new BigDecimal("5"))
                    .build();
            when(payrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId))
                    .thenReturn(Optional.of(config));

            assertThatThrownBy(() ->
                    payrollService.calculate(teacherId, periodStart, periodEnd))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("GAP-057b");
        }
    }

    // ============= Read-only listing =============

    @Nested
    @DisplayName("read-only views")
    class ReadOnlyViews {

        @Test
        @DisplayName("getPeriod by id returns period when present")
        void getPeriodById() {
            PayrollPeriod period = PayrollPeriod.builder()
                    .teacherId(teacherId)
                    .startDate(periodStart)
                    .endDate(periodEnd)
                    .grossAmount(BigDecimal.ZERO)
                    .deductions(BigDecimal.ZERO)
                    .netAmount(BigDecimal.ZERO)
                    .status(PayrollStatus.DRAFT)
                    .build();
            period.setId(7L);
            when(payrollPeriodRepository.findByIdAndDeletedFalse(7L))
                    .thenReturn(Optional.of(period));

            PayrollPeriod result = payrollService.getPeriodById(7L);

            assertThat(result.getId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("getPeriod by missing id throws EntityNotFoundException")
        void getPeriodByIdMissing() {
            when(payrollPeriodRepository.findByIdAndDeletedFalse(anyLong()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> payrollService.getPeriodById(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ============= Helpers =============

    private PayrollConfig hourlyConfig(BigDecimal rate) {
        PayrollConfig c = PayrollConfig.builder()
                .teacherId(teacherId)
                .type(PayrollType.HOURLY)
                .hourlyRate(rate)
                .build();
        c.setInstanceId(tenantId);
        return c;
    }

    private TeacherClass teacherClass(Long teacherId, Long classId) {
        return TeacherClass.builder()
                .teacherId(teacherId)
                .classId(classId)
                .build();
    }

    private ClassSession session(Long classId, LocalDate date, LocalTime start, LocalTime end) {
        ClassSession s = ClassSession.builder()
                .classId(classId)
                .sessionNumber(1)
                .sessionDate(date)
                .startTime(start)
                .endTime(end)
                .build();
        s.setInstanceId(tenantId);
        s.setDeleted(false);
        return s;
    }
}
