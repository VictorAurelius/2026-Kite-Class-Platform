package com.kiteclass.core.module.clazz.repository;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.module.clazz.entity.Class;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Class entity.
 *
 * <p>All query methods include {@code deleted = false} filter.
 * Multi-tenant filtering is applied via Hibernate @Filter from BaseEntity.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public interface ClassRepository extends JpaRepository<Class, Long> {

    /**
     * Finds a class by ID that is not deleted.
     * NOTE: Use this instead of findById() to respect soft-delete.
     *
     * @param id class ID
     * @return Optional containing class if found and not deleted
     */
    Optional<Class> findByIdAndDeletedFalse(Long id);

    /**
     * Finds a class by ID with {@code PESSIMISTIC_WRITE} lock —
     * Wave beta-readiness-1 Bucket B capacity-race guard.
     *
     * <p>When N concurrent {@code POST /api/v1/enrollments} target the same
     * class, all transactions serialize on the DB-level row lock (SELECT FOR UPDATE).
     * Each transaction in turn:
     * <ol>
     *   <li>Acquires exclusive row lock on the Class row</li>
     *   <li>Reads the current {@code currentEnrolled} value</li>
     *   <li>If {@code currentEnrolled < maxStudents}: INSERT enrollment + increment counter</li>
     *   <li>Else: throws {@code ValidationException("CLASS_FULL")} → HTTP 400</li>
     * </ol>
     *
     * <p>Because threads serialize at the DB level, the first {@code maxStudents}
     * requests succeed and the rest receive CLASS_FULL — no optimistic retry needed.
     * This guarantees exactly {@code maxStudents} successful enrollments even under
     * high concurrency.
     *
     * @param id class ID
     * @return Optional containing class if found and not deleted (with exclusive lock)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Class c WHERE c.id = :id AND c.deleted = false")
    Optional<Class> findByIdForEnrollmentWithLock(@Param("id") Long id);

    /**
     * Finds all classes for a given course (not deleted), paginated.
     *
     * @param courseId course ID
     * @param pageable pagination params
     * @return page of classes
     */
    Page<Class> findByCourseIdAndDeletedFalse(Long courseId, Pageable pageable);

    /**
     * Finds classes for a course filtered by status (not deleted).
     *
     * @param courseId course ID
     * @param status   class status filter
     * @param pageable pagination params
     * @return page of classes
     */
    Page<Class> findByCourseIdAndStatusAndDeletedFalse(Long courseId, ClassStatus status, Pageable pageable);

    /**
     * Checks if a class name already exists within the same course and tenant.
     *
     * @param name       class name
     * @param courseId   course ID
     * @param instanceId tenant instance ID
     * @return true if name exists
     */
    boolean existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(String name, Long courseId, UUID instanceId);

    /**
     * Checks if a class code is already taken (globally, codes must be unique).
     *
     * @param classCode the class code to check
     * @return true if code already exists
     */
    boolean existsByClassCodeAndDeletedFalse(String classCode);

    /**
     * Finds a class by its enrollment code.
     * Used during student self-enrollment.
     *
     * @param classCode the enrollment code
     * @return Optional containing class if code is valid
     */
    Optional<Class> findByClassCodeAndDeletedFalse(String classCode);

    /**
     * Finds a class by code within a tenant instance (not deleted).
     *
     * <p>Tenant-scoped resolver used by enrollment bulk-import (GAP-1104) to map
     * a human-entered {@code class_code} to a class ID within the uploading
     * tenant only. The {@code instance_id} predicate is explicit because the
     * Hibernate {@code tenantFilter} is not applied to derived {@code findBy}
     * queries; this prevents resolving a class code belonging to another tenant.
     *
     * @param classCode the class code
     * @param instanceId the tenant instance ID
     * @return Optional containing class if found within this tenant
     * @since 2.7.0
     */
    Optional<Class> findByClassCodeAndInstanceIdAndDeletedFalse(String classCode, UUID instanceId);

    /**
     * Counts classes for a given course (not deleted).
     *
     * @param courseId course ID
     * @return count of classes
     */
    long countByCourseIdAndDeletedFalse(Long courseId);

    /**
     * Finds classes by status for a tenant (for background tasks).
     *
     * @param status     class status
     * @param instanceId tenant instance ID
     * @param pageable   pagination
     * @return page of classes
     */
    @Query("SELECT c FROM Class c WHERE c.status = :status AND c.instanceId = :instanceId AND c.deleted = false")
    Page<Class> findByStatusAndInstanceId(
            @Param("status") ClassStatus status,
            @Param("instanceId") UUID instanceId,
            Pageable pageable);

    /**
     * Finds all classes for the current tenant (not deleted), paginated.
     *
     * <p>Tenant isolation is enforced automatically by the Hibernate
     * {@code tenantFilter} (from {@link com.kiteclass.core.common.entity.BaseEntity})
     * — this derived query compiles to HQL, so the filter condition
     * {@code instance_id = :tenantId} is appended transparently. No explicit
     * {@code instanceId} parameter is required, and no cross-tenant leak is possible.
     *
     * @param pageable pagination + sort params
     * @return page of classes scoped to the current tenant
     */
    Page<Class> findAllByDeletedFalse(Pageable pageable);
}
