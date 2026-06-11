package com.kiteclass.core.module.childprotection.repository;

import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.enums.IncidentVisibilityScope;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Incident} — child-protection ticket persistence.
 *
 * <p>All queries assume tenant filter is active (BaseEntity-level filter
 * "tenantFilter" enabled by TenantFilterInterceptor for API requests).
 *
 * <p>Encrypted columns (description, evidence_paths) are decrypted by
 * {@code AesGcmAttributeConverter} on read — repository methods return
 * plaintext to callers (which then enforce RBAC at the service layer in
 * Phase 1B; Phase 1A leaves this to integration tests).
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    /**
     * Find non-deleted incident by ID.
     */
    Optional<Incident> findByIdAndDeletedFalse(Long id);

    /**
     * Page of non-deleted incidents, optionally filtered by severity / category
     * / status. Phase 1A read-only listing.
     *
     * <p><b>42P18 note (GAP-1109):</b> the previous JPQL form
     * {@code (:severity IS NULL OR i.severity = :severity)} bound an UNTYPED null
     * in the {@code IS NULL} position, which PostgreSQL rejects at PREPARE time
     * with {@code 42P18 could not determine data type of parameter} (H2 hides
     * this). This is now built with the Criteria API: each predicate is only
     * added when its parameter is non-null, so no untyped-null bind is ever
     * emitted. The Hibernate {@code tenantFilter} still applies (Criteria →
     * JPQL-equivalent), unlike a native-SQL rewrite which would silently drop
     * tenant isolation.
     */
    default Page<Incident> findByFilters(
            IncidentSeverity severity,
            IncidentCategory category,
            IncidentStatus status,
            Pageable pageable
    ) {
        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }

    /**
     * Find non-deleted incidents for a given subject student filtered to a
     * caller-supplied set of {@link IncidentVisibilityScope} values — used by
     * the parent-portal conduct facet (Wave 19 Bucket D — GAP-321b-1-conduct).
     *
     * <p>Per BR-CHILD-PROTECT-005 (Phase 1C v1) the parent caller passes
     * {@code [PARENT_VISIBLE, PUBLIC]} so abuse / grooming / CSAM records
     * defaulting to {@code STAFF_ONLY} never leak through this surface.
     *
     * <p>{@link EntityGraph} is declared (with no attributePaths — the
     * {@code Incident} entity has no JPA collection associations, encrypted
     * fields are {@code @Convert} attribute converters which decrypt during
     * read regardless of fetch mode) so the query plan stays a single SELECT
     * + count and the parent-conduct N+1 IT
     * ({@code ParentConductFacetEntityGraphIT}) can assert ≤3 prepared
     * statements per facet call.
     *
     * <p>The service-layer caller is expected to have already verified
     * parent → child link via {@code ParentStudentLinkRepository}; this
     * method does NOT re-check the link.
     *
     * @param studentId      child id (subject of the incident)
     * @param visibleScopes  set of visibility scopes the caller may see
     *                       (parent-portal passes {@code PARENT_VISIBLE} +
     *                       {@code PUBLIC})
     * @param pageable       pagination — service-layer caller defaults to
     *                       {@code occurredAt DESC} when supported, falls
     *                       back to {@code id DESC} otherwise
     * @return page of visible non-deleted incidents
     * @since 5.x (Wave 19 Bucket D — GAP-321b-1-conduct real wiring)
     */
    @EntityGraph(attributePaths = {})
    @Query("SELECT i FROM Incident i "
            + "WHERE i.subjectStudentId = :studentId "
            + "AND i.visibilityScope IN :visibleScopes "
            + "AND i.deleted = false")
    Page<Incident> findVisibleForParent(@Param("studentId") Long studentId,
                                        @Param("visibleScopes") Collection<IncidentVisibilityScope> visibleScopes,
                                        Pageable pageable);

    /**
     * Convenience non-paged variant of {@link #findVisibleForParent} — the
     * parent-conduct facet contract is "list" not "page" per
     * {@code ParentConductFacetService} (Wave 18b2 Bucket C); a single
     * round-trip + ordered list is friendlier than forcing the caller to
     * unwrap a {@code Page} for a typically-tiny result set (≤ a few
     * incidents per child per academic year).
     *
     * @param studentId      child id (subject of the incident)
     * @param visibleScopes  set of visibility scopes the caller may see
     * @return list of visible non-deleted incidents, newest first by id
     * @since 5.x (Wave 19 Bucket D — GAP-321b-1-conduct real wiring)
     */
    @EntityGraph(attributePaths = {})
    @Query("SELECT i FROM Incident i "
            + "WHERE i.subjectStudentId = :studentId "
            + "AND i.visibilityScope IN :visibleScopes "
            + "AND i.deleted = false "
            + "ORDER BY i.id DESC")
    List<Incident> findVisibleForParentList(@Param("studentId") Long studentId,
                                            @Param("visibleScopes") Collection<IncidentVisibilityScope> visibleScopes);

    /**
     * Find non-deleted incidents whose {@code retention_until} deadline has
     * passed — used by {@code RetentionLifecycleService} cron to schedule
     * secure-delete (Phase 1C v1.5, GAP-359 sub-task 359.1).
     *
     * <p>The query intentionally bypasses the tenant filter: the lifecycle
     * job runs system-wide once per day and must reach every tenant chain.
     * Hibernate filters are session-scoped — the cron starts without a
     * {@code TenantContext}, so the filter stays disabled.
     *
     * @param now current instant; rows with {@code retention_until &lt; now}
     *            are returned for processing
     * @return non-deleted incidents past their retention deadline
     * @since 5.x (Wave 24 Bucket A — GAP-359 sub-task 359.1)
     */
    @Query("SELECT i FROM Incident i "
            + "WHERE i.deleted = false "
            + "AND i.retentionUntil IS NOT NULL "
            + "AND i.retentionUntil < :now")
    List<Incident> findExpiredRetention(@Param("now") Instant now);
}
