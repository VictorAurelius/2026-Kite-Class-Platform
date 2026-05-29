package com.kiteclass.core.module.teacher.repository;

import com.kiteclass.core.module.teacher.entity.Teacher;
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
 * Repository interface for Teacher entity.
 *
 * <p>Provides data access methods including:
 * <ul>
 *   <li>Find by ID (excluding soft-deleted records)</li>
 *   <li>Check existence by email (tenant-scoped)</li>
 *   <li>Search teachers with filters (name, email, specialization, status)</li>
 *   <li>Count teachers by status</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * Finds a teacher by ID, excluding soft-deleted records.
     *
     * @param id the teacher ID
     * @return Optional containing the teacher if found and not deleted
     */
    Optional<Teacher> findByIdAndDeletedFalse(Long id);

    /**
     * Finds a teacher by email, excluding soft-deleted records.
     *
     * @param email the teacher email
     * @return Optional containing the teacher if found and not deleted
     */
    Optional<Teacher> findByEmailAndDeletedFalse(String email);

    /**
     * Checks if a teacher with given email exists (excluding deleted).
     *
     * @param email the email to check
     * @return true if email exists
     * @deprecated since 2.13.0, use {@link #existsByEmailAndInstanceIdAndDeletedFalse(String, UUID)} for tenant-scoped check
     */
    @Deprecated(since = "2.13.0", forRemoval = true)
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Checks if a teacher with given email exists in specific tenant (excluding deleted).
     *
     * <p>Multi-tenant version of email existence check.
     * Replaces global {@link #existsByEmailAndDeletedFalse(String)} check.
     *
     * @param email the email to check
     * @param instanceId the tenant instance ID
     * @return true if email exists in the tenant
     * @since 2.13.0
     */
    boolean existsByEmailAndInstanceIdAndDeletedFalse(String email, UUID instanceId);

    /**
     * Searches teachers by name/email/specialization and status with pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>search: matches name, email, or specialization (case-insensitive, partial match)</li>
     *   <li>status: filters by teacher status (null = all statuses)</li>
     * </ul>
     *
     * <p>Only returns non-deleted teachers.
     *
     * @param search the search keyword (can be null)
     * @param status the teacher status filter (can be null)
     * @param pageable pagination parameters
     * @return page of matching teachers
     */
    // GAP-791 cross-flow sweep: nativeQuery=true bypasses Hibernate @Filter("tenantFilter")
    // (JPQL-only). The instance_id predicate below — bound via SpEL to TenantContext, mirroring
    // StudentRepository.findBySearchCriteria — is MANDATORY to scope the teachers list to the
    // current tenant. Without it, GET /api/v1/teachers leaks other tenants' teachers (OWASP A01).
    @Query(value = """
            SELECT * FROM teachers t
            WHERE t.deleted = false
            AND t.instance_id = :#{T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()}
            AND (CAST(:search AS text) IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
                OR LOWER(t.email) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
                OR LOWER(t.specialization) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (CAST(:status AS text) IS NULL OR t.status = CAST(:status AS text))
            """,
            countQuery = """
            SELECT COUNT(*) FROM teachers t
            WHERE t.deleted = false
            AND t.instance_id = :#{T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()}
            AND (CAST(:search AS text) IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
                OR LOWER(t.email) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
                OR LOWER(t.specialization) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (CAST(:status AS text) IS NULL OR t.status = CAST(:status AS text))
            """,
            nativeQuery = true)
    Page<Teacher> findBySearchCriteria(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * Finds all teachers with given status (excluding deleted).
     *
     * @param status the teacher status
     * @return list of teachers with the status
     */
    List<Teacher> findByStatusAndDeletedFalse(String status);

    /**
     * Counts teachers with given status (excluding deleted).
     *
     * @param status the teacher status
     * @return count of teachers with the status
     */
    long countByStatusAndDeletedFalse(String status);
}
