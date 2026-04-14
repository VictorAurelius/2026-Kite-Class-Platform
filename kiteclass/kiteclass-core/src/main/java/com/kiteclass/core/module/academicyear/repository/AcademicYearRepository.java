package com.kiteclass.core.module.academicyear.repository;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
