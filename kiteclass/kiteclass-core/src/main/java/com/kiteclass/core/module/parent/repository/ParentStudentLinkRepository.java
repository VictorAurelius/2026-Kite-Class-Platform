package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data-access for {@link ParentStudentLink}.
 *
 * <p>Queries are written with explicit JPQL joins so that tests using
 * Hibernate's auto-ddl schema see identical SQL to production (no surprises
 * from derived-query snake_case conversion).
 *
 * @since 2.14.0
 */
@Repository
public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink, Long> {

    /**
     * Lists all non-deleted links owned by a parent, eagerly fetching the
     * linked student to avoid N+1 loading when rendering the dashboard.
     */
    @Query("""
            SELECT l FROM ParentStudentLink l
            JOIN FETCH l.student s
            WHERE l.parent.id = :parentId
              AND l.deleted = false
              AND s.deleted = false
            """)
    List<ParentStudentLink> findByParentIdWithStudent(@Param("parentId") Long parentId);

    /**
     * Returns the list of linked student ids for a given parent. Used to
     * populate the {@code linked_student_ids} JWT claim at login.
     */
    @Query("""
            SELECT l.student.id FROM ParentStudentLink l
            WHERE l.parent.id = :parentId AND l.deleted = false
            """)
    List<Long> findStudentIdsByParentId(@Param("parentId") Long parentId);

    /**
     * @return {@code true} iff there is a non-deleted link between the given
     * parent and student. Used for authorization checks on child-scoped reads.
     */
    boolean existsByParentIdAndStudentIdAndDeletedFalse(Long parentId, Long studentId);

    /**
     * Lists all non-deleted links attached to a student with the joined
     * {@link com.kiteclass.core.module.parent.entity.Parent} prefetched (GAP-134
     * anti-N+1). Symmetric sibling to {@link #findByParentIdWithStudent(Long)};
     * used by child-profile views that render "who are this student's guardians".
     *
     * @param studentId the student ID
     * @return list of links with parent prefetched
     * @since 3.17.0 (GAP-134 expansion — Wave 9.5)
     */
    @Query("""
            SELECT l FROM ParentStudentLink l
            JOIN FETCH l.parent p
            WHERE l.student.id = :studentId
              AND l.deleted = false
              AND p.deleted = false
            """)
    List<ParentStudentLink> findByStudentIdWithParent(@Param("studentId") Long studentId);

    /**
     * Bulk-updates the JSONB {@code parental_consent.version} field on every
     * non-deleted link in the given tenant whose current version is strictly
     * less than {@code newVersion}. Idempotent — links already at or above
     * the target version are left untouched. Implemented as a native UPDATE
     * with PostgreSQL {@code jsonb_set} so the operation is a single
     * round-trip regardless of row count.
     *
     * @param instanceId tenant UUID
     * @param newVersion new version to bump records up to
     * @return number of rows updated
     * @since 2.24.0 (Wave 24 — GAP-361 Phase 1C v1.5 — re-consent flow)
     */
    @Modifying
    @Query(value = """
            UPDATE parent_student_links
               SET parental_consent = jsonb_set(parental_consent, '{version}',
                                                to_jsonb(:newVersion::int), true)
             WHERE instance_id = :instanceId
               AND deleted = false
               AND COALESCE((parental_consent ->> 'version')::int, 1) < :newVersion
            """, nativeQuery = true)
    int bulkBumpConsentVersion(@Param("instanceId") UUID instanceId,
                               @Param("newVersion") int newVersion);
}
