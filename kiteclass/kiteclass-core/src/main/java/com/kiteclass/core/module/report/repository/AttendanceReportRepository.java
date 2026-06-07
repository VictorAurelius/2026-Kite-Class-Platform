package com.kiteclass.core.module.report.repository;

import com.kiteclass.core.module.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregation repository for the attendance analytics report (GAP-775).
 *
 * <p>Extends {@link JpaRepository} on {@link Attendance} to reuse the entity's
 * managed persistence unit. Tenant scoping is enforced via an EXPLICIT
 * {@code instance_id = :tenantId} predicate in the aggregation query (GAP-1039
 * defense-in-depth) — NOT solely relying on the Hibernate {@code tenantFilter},
 * which is only enabled when an {@code X-Tenant-Id} header is present. Without the
 * explicit predicate, a header-less request would aggregate across ALL tenants.
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
@Repository
public interface AttendanceReportRepository extends JpaRepository<Attendance, Long> {

    /**
     * Counts attendance records grouped by marked year + month, splitting PRESENT
     * vs total, within the {@code [from, to)} window. Soft-deleted records excluded.
     *
     * <p>Returns {@code [year (int), month (int), presentCount (long), totalCount (long)]}
     * rows. Tenant isolation is enforced by the explicit {@code a.instanceId = :tenantId}
     * predicate (GAP-1039) so the aggregate is scoped to the caller's tenant even if
     * the Hibernate tenant filter is not enabled for the request.
     *
     * @param tenantId caller's tenant (instance) id — resolved from {@code TenantContext}
     * @param from inclusive lower bound on {@code markedDate}
     * @param to exclusive upper bound on {@code markedDate}
     * @return aggregated rows, unordered (service re-buckets by month key)
     */
    @Query("SELECT YEAR(a.markedDate), MONTH(a.markedDate), "
            + "SUM(CASE WHEN a.status = com.kiteclass.core.common.constant.AttendanceStatus.PRESENT "
            + "THEN 1L ELSE 0L END), "
            + "COUNT(a) "
            + "FROM Attendance a "
            + "WHERE a.instanceId = :tenantId "
            + "AND a.deleted = false "
            + "AND a.markedDate >= :from AND a.markedDate < :to "
            + "GROUP BY YEAR(a.markedDate), MONTH(a.markedDate)")
    List<Object[]> countAttendanceByMonth(@Param("tenantId") UUID tenantId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}
