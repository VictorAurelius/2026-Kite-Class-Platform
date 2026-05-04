package com.kiteclass.core.module.payroll.service.impl;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.payroll.dto.PayrollConfigResponse;
import com.kiteclass.core.module.payroll.dto.PayrollPeriodResponse;
import com.kiteclass.core.module.payroll.entity.PayrollConfig;
import com.kiteclass.core.module.payroll.entity.PayrollPeriod;
import com.kiteclass.core.module.payroll.enums.PayrollStatus;
import com.kiteclass.core.module.payroll.enums.PayrollType;
import com.kiteclass.core.module.payroll.repository.PayrollConfigRepository;
import com.kiteclass.core.module.payroll.repository.PayrollPeriodRepository;
import com.kiteclass.core.module.payroll.service.PayrollService;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Phase 1 implementation of {@link PayrollService}.
 *
 * <p>Only HOURLY type is calculable. SALARY/COMMISSION/HYBRID throw
 * {@link UnsupportedOperationException} naming GAP-057b.
 *
 * <p>Audit log: persistence flows through JPA auditing
 * ({@code BaseEntity.@CreatedBy / @LastModifiedBy} + Hibernate envers if
 * configured upstream). No domain events are emitted in Phase 1; Phase 2
 * (GAP-057b) ships an Outbox event per approval transition.
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private static final int MONEY_SCALE = 2;
    private static final int HOURS_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private final PayrollConfigRepository payrollConfigRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final ClassSessionRepository classSessionRepository;
    private final TeacherClassRepository teacherClassRepository;

    @Override
    @Transactional
    public PayrollPeriod calculate(Long teacherId, LocalDate startDate, LocalDate endDate) {
        // 1. Validate dates (BR-PAYROLL-004)
        if (startDate == null || endDate == null) {
            throw new ValidationException("PAYROLL_PERIOD_DATES_REQUIRED", (Object) null);
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("PAYROLL_PERIOD_END_BEFORE_START",
                    (Object) startDate.toString(), (Object) endDate.toString());
        }

        // 2. Lookup config (404 if missing)
        PayrollConfig config = payrollConfigRepository
                .findByTeacherIdAndDeletedFalse(teacherId)
                .orElseThrow(() -> {
                    log.warn("PayrollConfig not found for teacher {}", teacherId);
                    return new EntityNotFoundException("PAYROLL_CONFIG_NOT_FOUND", (Object) teacherId);
                });

        // 3. Phase 1 only supports HOURLY (BR-PAYROLL-006 deferral note)
        if (config.getType() != PayrollType.HOURLY) {
            throw new UnsupportedOperationException(String.format(
                    "Phase 2 GAP-057b will support payroll type %s for teacher %d. "
                            + "Phase 1 (Wave 18a) implements HOURLY only.",
                    config.getType(), teacherId));
        }

        // 4. Validate hourlyRate (BR-PAYROLL-002)
        BigDecimal rate = config.getHourlyRate();
        if (rate == null || rate.signum() <= 0) {
            throw new ValidationException("PAYROLL_HOURLY_RATE_REQUIRED", (Object) teacherId);
        }

        // 5. Sum hours across all teacher's class assignments (read-only access
        //    to clazz module — Bucket A's domain; SOFT overlap only).
        BigDecimal hoursWorked = sumTeacherHoursInPeriod(teacherId, startDate, endDate);

        // 6. gross = hours * rate, HALF_EVEN scale=2 (BR-PAYROLL-007)
        BigDecimal grossAmount = hoursWorked.multiply(rate)
                .setScale(MONEY_SCALE, ROUNDING);

        // 7. Phase 1: deductions = 0 (BR-PAYROLL-006); Phase 2 ships TNCN+BHXH+BHYT
        BigDecimal deductions = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        BigDecimal netAmount = grossAmount.subtract(deductions)
                .setScale(MONEY_SCALE, ROUNDING);

        PayrollPeriod period = PayrollPeriod.builder()
                .teacherId(teacherId)
                .startDate(startDate)
                .endDate(endDate)
                .hoursWorked(hoursWorked)
                .grossAmount(grossAmount)
                .deductions(deductions)
                .netAmount(netAmount)
                .status(PayrollStatus.DRAFT)
                .build();
        period.setInstanceId(config.getInstanceId());

        log.info("Phase 1 payroll calculated for teacher {} period {} to {}: "
                        + "hours={}, gross={}, status=DRAFT",
                teacherId, startDate, endDate, hoursWorked, grossAmount);

        return payrollPeriodRepository.save(period);
    }

    /**
     * Sum hours from all class sessions in [startDate, endDate] for any class
     * the teacher is assigned to. Read-only access into the {@code clazz} module
     * (Bucket A's domain — SOFT overlap, safe).
     */
    private BigDecimal sumTeacherHoursInPeriod(Long teacherId, LocalDate startDate, LocalDate endDate) {
        List<TeacherClass> assignments = teacherClassRepository.findByTeacherId(teacherId);
        BigDecimal total = BigDecimal.ZERO;

        for (TeacherClass assignment : assignments) {
            List<ClassSession> sessions = classSessionRepository
                    .findByClassIdAndDeletedFalseOrderBySessionNumberAsc(assignment.getClassId());

            for (ClassSession session : sessions) {
                LocalDate sessionDate = session.getSessionDate();
                if (sessionDate == null) {
                    continue;
                }
                if (sessionDate.isBefore(startDate) || sessionDate.isAfter(endDate)) {
                    continue;
                }

                BigDecimal sessionHours = sessionDurationHours(
                        session.getStartTime(), session.getEndTime());
                total = total.add(sessionHours);
            }
        }

        return total.setScale(HOURS_SCALE, ROUNDING);
    }

    /**
     * Duration between start and end times, in hours (decimal). Defensive against
     * null bounds (sessions without start/end are skipped via 0).
     */
    private BigDecimal sessionDurationHours(LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(start, end).toMinutes();
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60L), HOURS_SCALE, ROUNDING);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollPeriod getPeriodById(Long id) {
        return payrollPeriodRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("PAYROLL_PERIOD_NOT_FOUND", (Object) id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PayrollConfigResponse> listConfigs(Pageable pageable) {
        Page<PayrollConfigResponse> page = payrollConfigRepository
                .findAllByDeletedFalse(pageable)
                .map(PayrollConfigResponse::from);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PayrollPeriodResponse> listPeriods(
            Long teacherId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<PayrollPeriodResponse> page = payrollPeriodRepository
                .findByFilters(teacherId, startDate, endDate, pageable)
                .map(PayrollPeriodResponse::from);
        return PageResponse.from(page);
    }
}
