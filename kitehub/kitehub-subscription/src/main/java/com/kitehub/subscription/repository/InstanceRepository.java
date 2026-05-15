package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    List<Instance> findByOwnerIdAndDeletedFalse(UUID ownerId);

    List<Instance> findByStatusAndDeletedFalse(InstanceStatus status);

    @Query("SELECT i FROM Instance i WHERE i.status = 'TRIAL' " +
           "AND i.trialExpiresAt < :now AND i.deleted = false")
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
    // =========================================================

    /**
     * Keyset-paginate non-deleted instances starting AFTER the given cursor id.
     * Pass {@code cursorId = null} for the first page.
     */
    @Query("SELECT i FROM Instance i WHERE i.deleted = false "
        + "AND (:cursorId IS NULL OR i.id > :cursorId) "
        + "ORDER BY i.id ASC")
    List<Instance> findAfterCursor(@Param("cursorId") UUID cursorId, Pageable pageable);
}
