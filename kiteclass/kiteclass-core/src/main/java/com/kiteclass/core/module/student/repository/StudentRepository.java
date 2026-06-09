package com.kiteclass.core.module.student.repository;

import com.kiteclass.core.module.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Student entity.
 *
 * <p>Provides data access methods including:
 * <ul>
 *   <li>Find by ID (excluding soft-deleted records)</li>
 *   <li>Check existence by email or phone (tenant-scoped)</li>
 *   <li>Search students with filters (name, email, status)</li>
 *   <li>Count students by status</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Finds a student by ID, excluding soft-deleted records.
     *
     * @param id the student ID
     * @return Optional containing the student if found and not deleted
     */
    Optional<Student> findByIdAndDeletedFalse(Long id);

    /**
     * Finds a student by ID with {@code parentLinks} prefetched (GAP-134 anti-N+1).
     *
     * <p>Use when the caller iterates {@code student.getParentLinks()} in the same
     * transaction — for example, parent-dashboard assembly and child-profile reads
     * that show "who the guardians are". Without this method Hibernate fires one
     * extra SELECT the first time the collection is touched; multiplied across a
     * list of students that becomes classic N+1.
     *
     * @param id the student ID
     * @return Optional containing student with parentLinks prefetched
     * @since 3.17.0 (GAP-134 expansion — Wave 9.5)
     */
    @EntityGraph(attributePaths = {"parentLinks"})
    @Query("SELECT s FROM Student s WHERE s.id = :id AND s.deleted = false")
    Optional<Student> findByIdWithParentLinks(@Param("id") Long id);

    /**
     * Finds a student by email (excluding deleted).
     *
     * @param email the email to find
     * @return Optional containing the student if found
     */
    Optional<Student> findByEmailAndDeletedFalse(String email);

    /**
     * Finds a student by email and tenant instance (excluding deleted).
     *
     * <p>Tenant-scoped lookup for multi-tenant email uniqueness check.
     * Use this method for validating email uniqueness within a tenant.
     *
     * @param email the email to find
     * @param instanceId the tenant instance ID
     * @return Optional containing the student if found
     * @since 2.3.1
     */
    Optional<Student> findByEmailAndInstanceIdAndDeletedFalse(String email, UUID instanceId);

    /**
     * Checks if a student with given email exists (excluding deleted).
     *
     * @param email the email to check
     * @return true if email exists
     * @deprecated Use {@link #existsByEmailAndInstanceIdAndDeletedFalse(String, UUID)} for tenant-scoped check
     */
    @Deprecated(since = "2.3.1", forRemoval = true)
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Checks if a student with given email exists within tenant instance (excluding deleted).
     *
     * <p>Tenant-scoped existence check for multi-tenant email uniqueness.
     * Replaces global {@link #existsByEmailAndDeletedFalse(String)} check.
     *
     * @param email the email to check
     * @param instanceId the tenant instance ID
     * @return true if email exists within this tenant
     * @since 2.3.1
     */
    boolean existsByEmailAndInstanceIdAndDeletedFalse(String email, UUID instanceId);

    /**
     * Checks if a student with given phone exists (excluding deleted) — GLOBAL.
     *
     * @param phone the phone number to check
     * @return true if phone exists in ANY tenant
     * @deprecated Use {@link #existsByPhoneAndInstanceIdAndDeletedFalse(String, UUID)} for
     *     tenant-scoped check. Global check leaks cross-tenant + blocks legitimate reuse
     *     (shared parent phone across centers) — see GAP-799.
     */
    @Deprecated
    boolean existsByPhoneAndDeletedFalse(String phone);

    /**
     * Checks if a student with given phone exists within a tenant (excluding deleted).
     *
     * <p>Tenant-scoped uniqueness per GAP-799. The shared {@code kiteclass_shared} DB
     * holds all tenants' students discriminated by {@code instance_id}; the Hibernate
     * {@code tenantFilter} is not applied to derived {@code existsBy} queries, so the
     * {@code instance_id} predicate must be explicit.
     *
     * @param phone the phone number to check
     * @param instanceId the current tenant id (from TenantContext)
     * @return true if phone exists within this tenant
     */
    boolean existsByPhoneAndInstanceIdAndDeletedFalse(String phone, UUID instanceId);

    /**
     * Finds a student by phone and tenant instance (excluding deleted).
     *
     * <p>Tenant-scoped resolver used by enrollment bulk-import (GAP-1104) when a
     * row supplies a phone number but no email. Phone is tenant-unique per
     * GAP-799 (global uniqueness relaxed), so this returns at most one student
     * within the tenant. The {@code instance_id} predicate is explicit because
     * the Hibernate {@code tenantFilter} is not applied to derived {@code findBy}
     * queries.
     *
     * @param phone the phone number to find
     * @param instanceId the tenant instance ID
     * @return Optional containing the student if found within this tenant
     * @since 2.7.0
     */
    Optional<Student> findByPhoneAndInstanceIdAndDeletedFalse(String phone, UUID instanceId);

    /**
     * Searches students by name/email and status with pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>search: matches name or email (case-insensitive, partial match)</li>
     *   <li>status: filters by student status (null = all statuses)</li>
     * </ul>
     *
     * <p>Only returns non-deleted students.
     *
     * @param search the search keyword (can be null)
     * @param status the student status filter (can be null)
     * @param pageable pagination parameters
     * @return page of matching students
     */
    @Query(value = """
            SELECT * FROM students s
            WHERE s.deleted = false
            AND s.instance_id = :#{T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()}
            AND (CAST(:search AS text) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
                OR LOWER(s.email) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (CAST(:status AS text) IS NULL OR s.status = CAST(:status AS text))
            """,
            countQuery = """
            SELECT COUNT(*) FROM students s
            WHERE s.deleted = false
            AND s.instance_id = :#{T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()}
            AND (CAST(:search AS text) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
                OR LOWER(s.email) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (CAST(:status AS text) IS NULL OR s.status = CAST(:status AS text))
            """,
            nativeQuery = true)
    Page<Student> findBySearchCriteria(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * Finds all students with given status (excluding deleted).
     *
     * @param status the student status
     * @return list of students with the status
     */
    List<Student> findByStatusAndDeletedFalse(String status);

    /**
     * Counts students with given status (excluding deleted).
     *
     * @param status the student status
     * @return count of students with the status
     */
    long countByStatusAndDeletedFalse(String status);
}
