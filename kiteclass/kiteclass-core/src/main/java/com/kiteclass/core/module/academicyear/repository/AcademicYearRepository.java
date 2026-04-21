package com.kiteclass.core.module.academicyear.repository;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for AcademicYear aggregate (per ADR-002, DDD Repository Pattern).
 *
 * <p>Multi-tenant isolation automatic via Hibernate "tenantFilter".
 *
 * @since 3.15.0 (GAP-053)
 */
@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    /**
     * Find academic year by name within current tenant.
     */
    Optional<AcademicYear> findByNameAndDeletedFalse(String name);

    /**
     * Find the CURRENT academic year for tenant.
     * BR-ACYR-003: Only 1 CURRENT at a time.
     */
    Optional<AcademicYear> findFirstByStatusAndDeletedFalse(AcademicYearStatus status);

    /**
     * Count years with given status (for uniqueness check on CURRENT).
     */
    long countByStatusAndDeletedFalse(AcademicYearStatus status);

    /**
     * Check if name already exists in tenant.
     */
    boolean existsByNameAndDeletedFalse(String name);

    /**
     * Finds the CURRENT academic year with {@code holidays} prefetched (GAP-134
     * anti-N+1). Used by {@code AcademicYearService#isHoliday} which otherwise
     * triggers an extra SELECT the first time the lazy collection is touched;
     * invoked on every holiday-aware date calculation.
     *
     * @return the CURRENT academic year with holidays prefetched
     * @since 3.17.0 (GAP-134 expansion — Wave 9.5)
     */
    @EntityGraph(attributePaths = {"holidays"})
    @Query("SELECT y FROM AcademicYear y WHERE y.status = :status AND y.deleted = false")
    Optional<AcademicYear> findFirstByStatusWithHolidays(@Param("status") AcademicYearStatus status);

    /**
     * Finds an academic year by ID with {@code semesters} prefetched (GAP-134
     * anti-N+1). Used by transcript + grade assembly flows that enumerate
     * semesters of a specific year.
     *
     * @param id the academic-year ID
     * @return Optional containing the year with semesters prefetched
     * @since 3.17.0 (GAP-134 expansion — Wave 9.5)
     */
    @EntityGraph(attributePaths = {"semesters"})
    @Query("SELECT y FROM AcademicYear y WHERE y.id = :id AND y.deleted = false")
    Optional<AcademicYear> findByIdWithSemesters(@Param("id") Long id);
}
