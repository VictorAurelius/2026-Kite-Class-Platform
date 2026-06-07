package com.kiteclass.core.module.report.repository;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.module.attendance.entity.Attendance;
import com.kiteclass.core.module.payment.entity.Payment;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.testutil.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-1039 — proves the revenue + attendance report aggregations are scoped to the
 * caller's tenant by the EXPLICIT {@code instance_id = :tenantId} predicate, NOT by the
 * Hibernate {@code tenantFilter}.
 *
 * <p>This is a {@code @DataJpaTest} slice (via {@link IntegrationTestBase}) so the
 * {@code TenantFilterInterceptor} is NOT in the chain and the Hibernate filter is never
 * enabled — exactly the condition that produced the original cross-tenant leak (a request
 * with no {@code X-Tenant-Id}). With the explicit predicate in place, the aggregate must
 * still return ONLY the requested tenant's rows and must NOT sum across all tenants.
 *
 * <p>Runs against real PostgreSQL via Testcontainers (gated by
 * {@code ENABLE_INTEGRATION_TESTS=true} like the other repository slice ITs).
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
@DisplayName("Report repositories — cross-tenant isolation (GAP-1039)")
class ReportTenantIsolationIT extends IntegrationTestBase {

    @Autowired
    private RevenueReportRepository revenueReportRepository;

    @Autowired
    private AttendanceReportRepository attendanceReportRepository;

    private static final AtomicLong SEQ = new AtomicLong();

    /** Window covering the current month (mirrors ReportServiceImpl bounds). */
    private static LocalDateTime windowFrom() {
        return YearMonth.now().atDay(1).atStartOfDay();
    }

    private static LocalDateTime windowTo() {
        return YearMonth.now().plusMonths(1).atDay(1).atStartOfDay();
    }

    @Test
    @DisplayName("revenue — query for tenant A returns ONLY tenant A's sum, not all-tenant total")
    void revenue_scopedToRequestedTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        LocalDateTime when = YearMonth.now().atDay(1).atStartOfDay().plusDays(1);

        saveCompletedPayment(tenantA, new BigDecimal("2000000"), when);
        saveCompletedPayment(tenantB, new BigDecimal("1500000"), when);

        BigDecimal sumA = revenueSum(tenantA);
        BigDecimal sumB = revenueSum(tenantB);

        // Tenant A sees only its own 2,000,000 — NOT the 3,500,000 all-tenant total.
        assertThat(sumA).isEqualByComparingTo("2000000");
        assertThat(sumB).isEqualByComparingTo("1500000");
        assertThat(sumA.add(sumB)).isEqualByComparingTo("3500000");
    }

    @Test
    @DisplayName("revenue — query for a tenant with no data returns empty (no all-tenant aggregation)")
    void revenue_unknownTenantReturnsEmpty() {
        UUID tenantA = UUID.randomUUID();
        saveCompletedPayment(tenantA, new BigDecimal("2000000"),
                YearMonth.now().atDay(1).atStartOfDay().plusDays(1));

        List<Object[]> rows = revenueReportRepository.sumCompletedRevenueByMonth(
                UUID.randomUUID(), windowFrom(), windowTo());

        // The pre-fix bug would have summed tenant A's revenue for an unrelated tenant.
        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("attendance — query for tenant A returns ONLY tenant A's counts")
    void attendance_scopedToRequestedTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        LocalDateTime when = YearMonth.now().atDay(1).atStartOfDay().plusDays(2);

        // Tenant A: 2 PRESENT of 3 total. Tenant B: 1 PRESENT of 1 total.
        saveAttendance(tenantA, AttendanceStatus.PRESENT, when);
        saveAttendance(tenantA, AttendanceStatus.PRESENT, when);
        saveAttendance(tenantA, AttendanceStatus.ABSENT, when);
        saveAttendance(tenantB, AttendanceStatus.PRESENT, when);

        List<Object[]> rowsA = attendanceReportRepository.countAttendanceByMonth(
                tenantA, windowFrom(), windowTo());

        assertThat(rowsA).hasSize(1);
        long presentA = ((Number) rowsA.get(0)[2]).longValue();
        long totalA = ((Number) rowsA.get(0)[3]).longValue();
        // Must be tenant A's 2/3 — NOT 3/4 (which would include tenant B's row).
        assertThat(presentA).isEqualTo(2L);
        assertThat(totalA).isEqualTo(3L);
    }

    @Test
    @DisplayName("attendance — query for a tenant with no data returns empty")
    void attendance_unknownTenantReturnsEmpty() {
        UUID tenantA = UUID.randomUUID();
        saveAttendance(tenantA, AttendanceStatus.PRESENT,
                YearMonth.now().atDay(1).atStartOfDay().plusDays(2));

        List<Object[]> rows = attendanceReportRepository.countAttendanceByMonth(
                UUID.randomUUID(), windowFrom(), windowTo());

        assertThat(rows).isEmpty();
    }

    // ------------------------- helpers -------------------------

    private BigDecimal revenueSum(UUID tenantId) {
        List<Object[]> rows = revenueReportRepository.sumCompletedRevenueByMonth(
                tenantId, windowFrom(), windowTo());
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : rows) {
            total = total.add(new BigDecimal(row[2].toString()));
        }
        return total;
    }

    private void saveCompletedPayment(UUID tenantId, BigDecimal amount, LocalDateTime completedAt) {
        long n = SEQ.incrementAndGet();
        Payment payment = Payment.builder()
                .paymentNumber("PAY-" + n)
                .transactionId("TXN-" + n)
                .invoiceId(n)
                .amount(amount)
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.COMPLETED)
                .initiatedAt(completedAt.minusMinutes(5))
                .completedAt(completedAt)
                .build();
        payment.setInstanceId(tenantId);
        payment.setDeleted(false);
        // JPA auditing (@CreatedDate) is inactive in @DataJpaTest slice — set explicitly.
        payment.setCreatedAt(Instant.now());
        revenueReportRepository.saveAndFlush(payment);
    }

    private void saveAttendance(UUID tenantId, AttendanceStatus status, LocalDateTime markedDate) {
        long n = SEQ.incrementAndGet();
        Attendance attendance = Attendance.builder()
                .enrollmentId(n)
                .sessionId(n)
                .status(status)
                .markedDate(markedDate)
                .build();
        attendance.setInstanceId(tenantId);
        attendance.setDeleted(false);
        // JPA auditing (@CreatedDate) is inactive in @DataJpaTest slice — set explicitly.
        attendance.setCreatedAt(Instant.now());
        attendanceReportRepository.saveAndFlush(attendance);
    }
}
