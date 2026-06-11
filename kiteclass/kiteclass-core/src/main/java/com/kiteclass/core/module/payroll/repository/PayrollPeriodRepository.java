package com.kiteclass.core.module.payroll.repository;

import com.kiteclass.core.module.payroll.entity.PayrollPeriod;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long>, JpaSpecificationExecutor<PayrollPeriod> {

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
     *
     * <p><b>42P18 note (GAP-1109):</b> the previous JPQL form
     * {@code (:teacherId IS NULL OR p.teacherId = :teacherId)} bound an UNTYPED
     * null in the {@code IS NULL} position, which PostgreSQL rejects at PREPARE
     * time with {@code 42P18 could not determine data type of parameter} (H2
     * hides this). This is now built with the Criteria API: each predicate is
     * only added when its parameter is non-null, so no untyped-null bind is ever
     * emitted. The Hibernate {@code tenantFilter} still applies (Criteria →
     * JPQL-equivalent), unlike a native-SQL rewrite which would silently drop
     * tenant isolation.
     */
    default Page<PayrollPeriod> findByFilters(
            Long teacherId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Specification<PayrollPeriod> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (teacherId != null) {
                predicates.add(cb.equal(root.get("teacherId"), teacherId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
