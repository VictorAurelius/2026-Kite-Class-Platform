package com.kiteclass.core.module.childprotection.repository;

import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.enums.IncidentVisibilityScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Find non-deleted incident by ID.
     */
    Optional<Incident> findByIdAndDeletedFalse(Long id);

    /**
     * Page of non-deleted incidents, optionally filtered by severity / category
     * / status. Phase 1A read-only listing.
     */
    @Query(
            "SELECT i FROM Incident i WHERE i.deleted = false "
                    + "AND (:severity IS NULL OR i.severity = :severity) "
                    + "AND (:category IS NULL OR i.category = :category) "
                    + "AND (:status IS NULL OR i.status = :status)"
    )
    Page<Incident> findByFilters(
            @Param("severity") IncidentSeverity severity,
            @Param("category") IncidentCategory category,
            @Param("status") IncidentStatus status,
            Pageable pageable
    );

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
}
