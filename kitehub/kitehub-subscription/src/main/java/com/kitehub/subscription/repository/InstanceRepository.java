package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Instance entity.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Repository
public interface InstanceRepository extends JpaRepository<Instance, UUID> {

    Optional<Instance> findBySubdomainAndDeletedFalse(String subdomain);

    /**
     * Non-deleted instances owned by {@code ownerId}, ordered DETERMINISTICALLY by
     * {@code createdAt ASC, id ASC} (GAP-1306). The explicit ORDER BY replaces the prior
     * derived query whose {@code List} had no defined order — Postgres heap/index scan
     * order made {@code .stream().findFirst()} callers (JWT {@code tenantId} / {@code tier}
     * claim resolution in {@link com.kitehub.subscription.service.AuthService} +
     * {@link com.kitehub.subscription.service.TokenService}) pick a non-deterministic
     * instance for owners with &gt;1 non-deleted instance → cross-tenant exposure risk.
     *
     * <p>Semantic: the OLDEST non-deleted instance wins (created first = primary), with
     * {@code id} as a stable tiebreaker if two rows share a {@code createdAt}. The method
     * signature is unchanged so every existing caller becomes deterministic at the source
     * (repository-level fix; zero blast radius — per
     * {@code .claude/rules/cross-flow-bug-class-sweep.md}).
     */
    @Query("SELECT i FROM Instance i WHERE i.ownerId = :ownerId AND i.deleted = false "
        + "ORDER BY i.createdAt ASC, i.id ASC")
    List<Instance> findByOwnerIdAndDeletedFalse(@Param("ownerId") UUID ownerId);

    List<Instance> findByStatusAndDeletedFalse(InstanceStatus status);

    /**
     * GAP-1253 (Wave kitehub-biz-100 Bucket 0): pessimistic-write lock for migration
     * mutating paths (rule T2P-08). Prevents two concurrent upgrade initiations from
     * both passing the can-start guard → double migration / double payment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Instance i WHERE i.id = :id")
    Optional<Instance> findByIdForUpdate(@Param("id") UUID id);

    /**
     * GAP-1255 (Wave kitehub-biz-100 Bucket 0): guard {@code migrationPhase = 'NONE'} so an
     * instance mid-migration (PAYMENT_CAPTURED / MIGRATING — still status=TRIAL) is NOT
     * suspended by the trial-expiry scheduler. {@code migrationPhase} is
     * {@code @Enumerated(STRING)} with NOT NULL DEFAULT 'NONE' (V19) → string-literal
     * comparison mirrors the existing {@code i.status = 'TRIAL'} pattern.
     */
    @Query("SELECT i FROM Instance i WHERE i.status = 'TRIAL' " +
           "AND i.trialExpiresAt < :now AND i.deleted = false " +
           "AND i.migrationPhase = 'NONE'")
    List<Instance> findExpiredTrials(@Param("now") LocalDateTime now);

    @Query("SELECT i FROM Instance i WHERE i.status = 'ACTIVE' " +
           "AND i.subscriptionExpiresAt < :now AND i.deleted = false")
    List<Instance> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    boolean existsBySubdomainAndDeletedFalse(String subdomain);

    long countByOwnerIdAndDeletedFalse(UUID ownerId);

    boolean existsByContactEmailAndDeletedFalse(String contactEmail);

    boolean existsByOwnerIdAndTrialStartedAtIsNotNull(UUID ownerId);

    boolean existsByContactEmailAndTrialStartedAtIsNotNull(String contactEmail);

    List<Instance> findByStatusAndDeletedFalseAndUpdatedAtBefore(
        InstanceStatus status, LocalDateTime before);

    @Query("SELECT i FROM Instance i WHERE i.status = :status AND i.updatedAt < :before")
    List<Instance> findByStatusAndUpdatedAtBefore(
        @Param("status") InstanceStatus status, @Param("before") LocalDateTime before);

    boolean existsByCustomDomainAndDeletedFalse(String customDomain);

    Optional<Instance> findByCustomDomainAndDeletedFalse(String customDomain);

    // =========================================================
    // GAP-823 Wave local-doable-9 Bucket B: slug field methods
    // (paired with V40 + Instance.slug + TenantSlugNormalizer wiring
    //  in InstanceService.registerInstance — closes triad drift)
    // =========================================================

    /**
     * Find instance by normalized slug.
     *
     * @param slug normalized slug (output of TenantSlugNormalizer.normalize)
     * @return matching instance or empty
     */
    Optional<Instance> findBySlug(String slug);

    /**
     * Check if any non-deleted instance has the exact slug.
     *
     * @param slug normalized slug
     * @return true if slug already taken by a live instance
     */
    boolean existsBySlugAndDeletedFalse(String slug);

    /**
     * Check if any instance (including deleted) has slug starting with prefix.
     * Used by collision-recovery loop to probe -1/-2/... suffix candidates.
     *
     * @param prefix slug prefix (typically the normalized base before suffix)
     * @return true if at least one row matches
     */
    boolean existsBySlugStartingWith(String prefix);

    List<Instance> findByMigrationPhase(MigrationPhase phase);

    // =========================================================
    // GAP-432 Wave 41 Bucket C: bounded analytics + listing queries
    // (replace prior unbounded findAll() callsites in AnalyticsService
    //  + InstanceService.listAllInstances).
    // =========================================================

    /**
     * Page through non-deleted instances. DB-side soft-delete filter eliminates
     * the need to load every row into memory.
     */
    Page<Instance> findByDeletedFalse(Pageable pageable);

    /** Count non-deleted instances. Replaces full-table fetch + size(). */
    long countByDeletedFalse();

    /**
     * Count non-deleted instances created strictly after the given timestamp.
     * Backs the "new signups last N days" dashboard widget.
     */
    long countByDeletedFalseAndCreatedAtAfter(LocalDateTime since);

    /** DB-side aggregation: status -> count for non-deleted instances. */
    @Query("SELECT i.status, COUNT(i) FROM Instance i WHERE i.deleted = false GROUP BY i.status")
    List<Object[]> countGroupByStatus();

    /** Convenience: status name -> count map (for dashboard DTO). */
    default Map<String, Long> countInstancesByStatus() {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : countGroupByStatus()) {
            InstanceStatus status = (InstanceStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            result.put(status.name(), count);
        }
        return result;
    }

    // =========================================================
    // Wave 85 Bucket D D-AC1: cursor-based (keyset) pagination
    // for instance lists >1M rows. Order fixed id ASC.
    //
    // GAP-1106: split the prior single `(:cursorId IS NULL OR i.id > :cursorId)`
    // JPQL into a first-page query (no cursor param) + an after-cursor query
    // (typed cursor param), branched by the default method below. Postgres could
    // not infer the bind type of the untyped null `:cursorId` in the `IS NULL`
    // position and rejected the prepared statement with 42P18 ("could not
    // determine data type of parameter"); H2 (test) hid it. Same class as
    // GAP-1028 (AdminAuditLogRepository) + GAP-1105 (branding lifecycle-events).
    // =========================================================

    /** First keyset page of non-deleted instances (no cursor). Order id ASC. */
    @Query("SELECT i FROM Instance i WHERE i.deleted = false ORDER BY i.id ASC")
    List<Instance> findFirstPage(Pageable pageable);

    /** Keyset page of non-deleted instances strictly AFTER {@code cursorId}. Order id ASC. */
    @Query("SELECT i FROM Instance i WHERE i.deleted = false "
        + "AND i.id > :cursorId ORDER BY i.id ASC")
    List<Instance> findAfterCursorId(@Param("cursorId") UUID cursorId, Pageable pageable);

    /**
     * Keyset-paginate non-deleted instances starting AFTER the given cursor id.
     * Pass {@code cursorId = null} for the first page. Branches to a cursor-free
     * query when null so no untyped null param is ever bound (GAP-1106).
     */
    default List<Instance> findAfterCursor(UUID cursorId, Pageable pageable) {
        return cursorId == null
            ? findFirstPage(pageable)
            : findAfterCursorId(cursorId, pageable);
    }
}
