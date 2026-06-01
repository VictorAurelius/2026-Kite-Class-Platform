package com.kiteclass.core.module.report.repository;

import com.kiteclass.core.module.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only aggregation repository for the revenue analytics report (GAP-775).
 *
 * <p>Extends {@link JpaRepository} on {@link Payment} purely to reuse the entity's
 * managed persistence unit + the Hibernate {@code tenantFilter} (so every query
 * here is automatically tenant-scoped on {@code payments.instance_id}).
 *
 * <p>Grouping uses JPQL {@code YEAR()} / {@code MONTH()} extract functions which
 * Hibernate translates portably to both PostgreSQL ({@code EXTRACT}) and H2,
 * avoiding Postgres-specific {@code to_char} per
 * {@code postgres-specific-type-testcontainers.md} portability spirit.
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
@Repository
public interface RevenueReportRepository extends JpaRepository<Payment, Long> {

    /**
     * Sums COMPLETED payment amounts grouped by completion year + month, within
     * the inclusive {@code [from, to)} window. Soft-deleted payments excluded.
     *
     * <p>Returns {@code [year (int), month (int), sumAmount (BigDecimal)]} rows.
     * Tenant isolation is applied automatically by the Hibernate tenant filter.
     *
     * @param from inclusive lower bound on {@code completedAt}
     * @param to exclusive upper bound on {@code completedAt}
     * @return aggregated rows, unordered (service re-buckets by month key)
     */
    @Query("SELECT YEAR(p.completedAt), MONTH(p.completedAt), COALESCE(SUM(p.amount), 0) "
            + "FROM Payment p "
            + "WHERE p.deleted = false "
            + "AND p.paymentStatus = com.kiteclass.core.module.payment.enums.PaymentStatus.COMPLETED "
            + "AND p.completedAt >= :from AND p.completedAt < :to "
            + "GROUP BY YEAR(p.completedAt), MONTH(p.completedAt)")
    List<Object[]> sumCompletedRevenueByMonth(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
