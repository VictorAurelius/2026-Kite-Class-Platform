package com.kiteclass.core.module.course.repository;

import com.kiteclass.core.module.course.entity.Course;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Course entity.
 *
 * <p>Provides data access methods including:
 * <ul>
 *   <li>Find by ID or code (excluding soft-deleted records)</li>
 *   <li>Check existence by code</li>
 *   <li>Search courses with filters (name, code, status, teacher)</li>
 *   <li>Find courses by teacher or status</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    /**
     * Finds a course by ID, excluding soft-deleted records.
     *
     * @param id the course ID
     * @return Optional containing the course if found and not deleted
     */
    Optional<Course> findByIdAndDeletedFalse(Long id);

    /**
     * Finds a course by code, excluding soft-deleted records.
     *
     * @param code the course code
     * @return Optional containing the course if found and not deleted
     */
    Optional<Course> findByCodeAndDeletedFalse(String code);

    /**
     * Checks if a course with given code exists (excluding deleted) — GLOBAL.
     *
     * @param code the code to check
     * @return true if code exists in ANY tenant
     * @deprecated Use {@link #existsByCodeAndInstanceIdAndDeletedFalse(String, UUID)} for
     *     tenant-scoped check. Global check leaks cross-tenant + blocks legitimate reuse
     *     (two centers using the same course code) — see GAP-799.
     */
    @Deprecated
    boolean existsByCodeAndDeletedFalse(String code);

    /**
     * Checks if a course with given code exists within a tenant (excluding deleted).
     *
     * <p>Tenant-scoped uniqueness per GAP-799, mirroring the DB constraint
     * {@code uk_courses_instance_code (instance_id, code)}. The shared
     * {@code kiteclass_shared} DB holds all tenants' courses discriminated by
     * {@code instance_id}; the Hibernate {@code tenantFilter} is not applied to derived
     * {@code existsBy} queries, so the {@code instance_id} predicate must be explicit.
     *
     * @param code the code to check
     * @param instanceId the current tenant id (from TenantContext)
     * @return true if code exists within this tenant
     */
    boolean existsByCodeAndInstanceIdAndDeletedFalse(String code, UUID instanceId);

    /**
     * Finds all courses for a specific teacher with pagination.
     *
     * @param teacherId the teacher ID
     * @param pageable  pagination parameters
     * @return page of courses created by the teacher
     */
    Page<Course> findByTeacherIdAndDeletedFalse(Long teacherId, Pageable pageable);

    /**
     * Finds all courses with a specific status with pagination.
     *
     * @param status   the course status
     * @param pageable pagination parameters
     * @return page of courses with the status
     */
    Page<Course> findByStatusAndDeletedFalse(String status, Pageable pageable);

    /**
     * Searches courses by multiple criteria with pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>tenantId: the current tenant (instance) — REQUIRED, scopes result to one tenant</li>
     *   <li>search: matches name or code (case-insensitive, partial match)</li>
     *   <li>status: filters by course status (null = all statuses)</li>
     *   <li>teacherId: filters by teacher who created the course (null = all teachers)</li>
     * </ul>
     *
     * <p>Only returns non-deleted courses.
     *
     * <p><b>Multi-tenant note (GAP-791):</b> this query uses {@code nativeQuery = true},
     * so the Hibernate {@code @Filter("tenantFilter")} declared on {@link
     * com.kiteclass.core.common.entity.BaseEntity} does NOT apply (that filter only
     * targets JPQL/HQL, not native SQL). The {@code instance_id = :tenantId} predicate
     * below is therefore MANDATORY — it is the only thing scoping this query to the
     * current tenant. Callers MUST pass {@code TenantContext.getCurrentTenant()}.
     *
     * @param tenantId  the current tenant (instance) ID — required, must not be null
     * @param search    the search keyword (can be null)
     * @param status    the course status filter (can be null)
     * @param teacherId the teacher ID filter (can be null)
     * @param pageable  pagination parameters
     * @return page of matching courses scoped to the given tenant
     */
    @Query(value = """
            SELECT * FROM courses c
            WHERE c.deleted = false
            AND c.instance_id = CAST(:tenantId AS uuid)
            AND (CAST(:search AS text) IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
                OR LOWER(c.code) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (CAST(:status AS text) IS NULL OR c.status = CAST(:status AS text))
            AND (CAST(:teacherId AS bigint) IS NULL OR c.teacher_id = CAST(:teacherId AS bigint))
            """,
            countQuery = """
            SELECT COUNT(*) FROM courses c
            WHERE c.deleted = false
            AND c.instance_id = CAST(:tenantId AS uuid)
            AND (CAST(:search AS text) IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
                OR LOWER(c.code) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (CAST(:status AS text) IS NULL OR c.status = CAST(:status AS text))
            AND (CAST(:teacherId AS bigint) IS NULL OR c.teacher_id = CAST(:teacherId AS bigint))
            """,
            nativeQuery = true)
    Page<Course> findBySearchCriteria(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            @Param("status") String status,
            @Param("teacherId") Long teacherId,
            Pageable pageable
    );

    /**
     * Finds all courses with given status (excluding deleted).
     *
     * @param status the course status
     * @return list of courses with the status
     */
    List<Course> findByStatusAndDeletedFalse(String status);

    /**
     * Counts courses with given status (excluding deleted).
     *
     * @param status the course status
     * @return count of courses with the status
     */
    long countByStatusAndDeletedFalse(String status);

    /**
     * Counts courses created by a specific teacher (excluding deleted).
     *
     * @param teacherId the teacher ID
     * @return count of courses created by the teacher
     */
    long countByTeacherIdAndDeletedFalse(Long teacherId);

    /**
     * Finds all PUBLISHED courses with pagination.
     * Useful for course catalog display.
     *
     * @param pageable pagination parameters
     * @return page of published courses
     */
    default Page<Course> findPublishedCourses(Pageable pageable) {
        return findByStatusAndDeletedFalse("PUBLISHED", pageable);
    }

    /**
     * Finds all DRAFT courses for a teacher with pagination.
     * Useful for teacher's course management dashboard.
     *
     * @param teacherId the teacher ID
     * @param pageable  pagination parameters
     * @return page of draft courses
     */
    @Query("""
            SELECT c FROM Course c
            WHERE c.deleted = false
            AND c.teacherId = :teacherId
            AND c.status = 'DRAFT'
            """)
    Page<Course> findDraftCoursesByTeacher(
            @Param("teacherId") Long teacherId,
            Pageable pageable
    );

    /**
     * Searches courses by level and/or category with pagination.
     *
     * <p>Search criteria:
     * <ul>
     *   <li>level: filters by course difficulty level (null = all levels)</li>
     *   <li>category: filters by course category (null = all categories)</li>
     * </ul>
     *
     * <p>Only returns non-deleted courses.
     *
     * @param level    the course level filter (can be null)
     * @param category the course category filter (can be null)
     * @param pageable pagination parameters
     * @return page of matching courses
     *
     * <p><b>42P18 note (GAP-1109):</b> the previous JPQL form
     * {@code (:level IS NULL OR c.level = :level)} bound an UNTYPED null in the
     * {@code IS NULL} position, which PostgreSQL rejects at PREPARE time with
     * {@code 42P18 could not determine data type of parameter} (H2 hides this).
     * This is now built with the Criteria API: each predicate is only added when
     * its parameter is non-null, so no untyped-null bind is ever emitted. The
     * Hibernate {@code tenantFilter} still applies (Criteria → JPQL-equivalent),
     * unlike a native-SQL rewrite which would silently drop tenant isolation.
     */
    default Page<Course> findByLevelAndCategory(
            String level,
            String category,
            Pageable pageable
    ) {
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
