package com.kiteclass.core.module.report.repository;

import com.kiteclass.core.module.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregation repository for the revenue analytics report (GAP-775).
 *
 * <p>Extends {@link JpaRepository} on {@link Payment} purely to reuse the entity's
 * managed persistence unit. Tenant scoping is enforced via an EXPLICIT
 * {@code instance_id = :tenantId} predicate in every aggregation query (GAP-1039
 * defense-in-depth) — NOT solely relying on the Hibernate {@code tenantFilter},
 * which is only enabled when an {@code X-Tenant-Id} header is present. Without the
 * explicit predicate, a header-less request would aggregate across ALL tenants.
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
     * Tenant isolation is enforced by the explicit {@code p.instanceId = :tenantId}
     * predicate (GAP-1039) so the aggregate is scoped to the caller's tenant even
     * if the Hibernate tenant filter is not enabled for the request.
     *
     * @param tenantId caller's tenant (instance) id — resolved from {@code TenantContext}
     * @param from inclusive lower bound on {@code completedAt}
     * @param to exclusive upper bound on {@code completedAt}
     * @return aggregated rows, unordered (service re-buckets by month key)
     */
    @Query("SELECT YEAR(p.completedAt), MONTH(p.completedAt), COALESCE(SUM(p.amount), 0) "
            + "FROM Payment p "
            + "WHERE p.instanceId = :tenantId "
            + "AND p.deleted = false "
            + "AND p.paymentStatus = com.kiteclass.core.module.payment.enums.PaymentStatus.COMPLETED "
            + "AND p.completedAt >= :from AND p.completedAt < :to "
            + "GROUP BY YEAR(p.completedAt), MONTH(p.completedAt)")
    List<Object[]> sumCompletedRevenueByMonth(@Param("tenantId") UUID tenantId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
