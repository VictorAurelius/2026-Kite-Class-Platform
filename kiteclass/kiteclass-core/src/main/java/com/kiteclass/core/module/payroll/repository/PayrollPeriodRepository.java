package com.kiteclass.core.module.payroll.repository;

import com.kiteclass.core.module.payroll.entity.PayrollPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for {@link PayrollPeriod}.
 *
 * <p>All queries assume tenant filter is active (BaseEntity-level filter
 * "tenantFilter" enabled by TenantFilterInterceptor for API requests).
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    /**
     * Find non-deleted period by ID.
     *
     * @param id period PK
     * @return Optional with period if present and not deleted
     */
    Optional<PayrollPeriod> findByIdAndDeletedFalse(Long id);

    /**
     * Page of non-deleted periods, optionally filtered by teacher and date range.
     *
     * <p>Phase 1: simple read-only query. Phase 2 (GAP-057b) adds status filter +
     * approval audit join.
     *
     * @param teacherId optional teacher FK filter
     * @param startDate optional inclusive start date filter (period.startDate &gt;= this)
     * @param endDate   optional inclusive end date filter (period.endDate &lt;= this)
     * @param pageable  pagination + sorting
     * @return page of matching periods
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT p FROM PayrollPeriod p WHERE p.deleted = false " +
                    "AND (:teacherId IS NULL OR p.teacherId = :teacherId) " +
                    "AND (:startDate IS NULL OR p.startDate >= :startDate) " +
                    "AND (:endDate IS NULL OR p.endDate <= :endDate)"
    )
    Page<PayrollPeriod> findByFilters(
            @org.springframework.data.repository.query.Param("teacherId") Long teacherId,
            @org.springframework.data.repository.query.Param("startDate") LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
