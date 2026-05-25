package com.kiteclass.core.module.enrollment.repository;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Enrollment} entity.
 *
 * <p>Provides database access methods for enrollment management with
 * support for multi-tenancy and soft deletes.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Find enrollment by ID excluding deleted.
     * Respects soft delete.
     *
     * <p><b>WARNING:</b> This method does NOT filter by tenant. It relies on the
     * Hibernate {@code tenantFilter} for multi-tenant isolation, which is fragile
     * across {@code REQUIRES_NEW} transaction boundaries and event listeners. Prefer
     * {@link #findByIdAndInstanceIdAndDeletedFalse(Long, UUID)} for explicit tenant
     * scoping. Tracked in GAP-746 (multi-tenant repo tenant filter sweep).
     *
     * @param id enrollment ID
     * @return optional enrollment
     * @deprecated use {@link #findByIdAndInstanceIdAndDeletedFalse(Long, UUID)} which
     *             passes tenant ID explicitly; this overload remains for places that
     *             cannot easily access {@code TenantContext} (e.g. system jobs).
     */
    @Deprecated(since = "GAP-746")
    Optional<Enrollment> findByIdAndDeletedFalse(Long id);

    /**
     * Find enrollment by ID, tenant ID and excluding deleted (explicit tenant filter).
     *
     * <p>Use this in place of {@link #findByIdAndDeletedFalse(Long)} whenever the
     * caller has a {@code TenantContext}. The explicit {@code instanceId} parameter
     * defends against Hibernate filter not being applied across {@code REQUIRES_NEW}
     * or async event listener boundaries (see GAP-746).
     *
     * @param id enrollment ID
     * @param instanceId tenant ID from {@code TenantContext.getCurrentTenant()}
     * @return optional enrollment
     * @since 2.x — GAP-746
     */
    Optional<Enrollment> findByIdAndInstanceIdAndDeletedFalse(Long id, UUID instanceId);

    /**
     * Find enrollment by student ID and class ID excluding deleted.
     * Used to check for duplicate enrollments.
     *
     * @param studentId student ID
     * @param classId class ID
     * @return optional enrollment
     */
    Optional<Enrollment> findByStudentIdAndClassIdAndDeletedFalse(Long studentId, Long classId);

    /**
     * Check if enrollment exists for student and class with specific status (excluding deleted).
     * Used to prevent duplicate active enrollments.
     *
     * @param studentId student ID
     * @param classId class ID
     * @param status enrollment status
     * @return true if exists, false otherwise
     */
    boolean existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
            Long studentId,
            Long classId,
            EnrollmentStatus status
    );

    /**
     * Count active enrollments for a class (excluding deleted).
     * Used for capacity validation.
     *
     * @param classId class ID
     * @param status enrollment status (ACTIVE)
     * @return count of active enrollments
     */
    long countByClassIdAndStatusAndDeletedFalse(Long classId, EnrollmentStatus status);

    /**
     * Find all enrollments for a student (excluding deleted).
     * Supports pagination and sorting.
     *
     * @param studentId student ID
     * @param pageable pagination info
     * @return page of enrollments
     */
    Page<Enrollment> findByStudentIdAndDeletedFalse(Long studentId, Pageable pageable);

    /**
     * Find all enrollments for a class (excluding deleted).
     * Supports pagination and sorting.
     *
     * @param classId class ID
     * @param pageable pagination info
     * @return page of enrollments
     */
    Page<Enrollment> findByClassIdAndDeletedFalse(Long classId, Pageable pageable);

    /**
     * Find all enrollments for a class with specific status (excluding deleted).
     *
     * @param classId class ID
     * @param status enrollment status
     * @param pageable pagination info
     * @return page of enrollments
     */
    Page<Enrollment> findByClassIdAndStatusAndDeletedFalse(
            Long classId,
            EnrollmentStatus status,
            Pageable pageable
    );

    /**
     * Find all enrollments for a student with specific status (excluding deleted).
     *
     * @param studentId student ID
     * @param status enrollment status
     * @param pageable pagination info
     * @return page of enrollments
     */
    Page<Enrollment> findByStudentIdAndStatusAndDeletedFalse(
            Long studentId,
            EnrollmentStatus status,
            Pageable pageable
    );

    /**
     * Find all active enrollments for a class (for roster display, excluding deleted).
     *
     * @param classId class ID
     * @return list of active enrollments
     */
    @Query("SELECT e FROM Enrollment e " +
           "WHERE e.classId = :classId " +
           "AND e.status = 'ACTIVE' " +
           "AND e.deleted = false " +
           "ORDER BY e.enrollmentDate ASC")
    List<Enrollment> findActiveEnrollmentsByClassId(@Param("classId") Long classId);
}
