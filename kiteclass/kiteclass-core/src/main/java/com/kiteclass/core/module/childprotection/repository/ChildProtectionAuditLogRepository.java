package com.kiteclass.core.module.childprotection.repository;

import com.kiteclass.core.module.childprotection.entity.ChildProtectionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ChildProtectionAuditLog} — append-only hash-chain
 * audit log (BR-CHILD-PROTECT-007, GAP-322c Phase 1C v1, Wave 19 Bucket A).
 *
 * <p><b>No DELETE / UPDATE methods exposed.</b> The application tier MUST
 * never mutate or remove rows; the V54 migration {@code REVOKE DELETE} on
 * the underlying table for the typical app role.
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
@Repository
public interface ChildProtectionAuditLogRepository
        extends JpaRepository<ChildProtectionAuditLog, Long> {

    /**
     * Find the most-recent entry for a given chain
     * {@code (instance_id, entity_type)}. Used by
     * {@code ChildProtectionAuditService.append(...)} to compute
     * {@code prev_hash}.
     *
     * <p>Returns {@link Optional#empty()} for the genesis case where no
     * prior entries exist for that (tenant, entity-type) chain.
     */
    @Query("SELECT a FROM ChildProtectionAuditLog a "
            + "WHERE a.instanceId = :instanceId "
            + "AND a.entityType = :entityType "
            + "ORDER BY a.id DESC")
    List<ChildProtectionAuditLog> findLatestForChain(
            @Param("instanceId") UUID instanceId,
            @Param("entityType") String entityType);

    /**
     * Convenience wrapper: returns the single latest entry for a chain
     * (or empty if genesis).
     */
    default Optional<ChildProtectionAuditLog> findHead(UUID instanceId, String entityType) {
        List<ChildProtectionAuditLog> latest = findLatestForChain(instanceId, entityType);
        return latest.isEmpty() ? Optional.empty() : Optional.of(latest.get(0));
    }

    /**
     * Read all entries for a (tenant, entity-type) chain in append order
     * (oldest first). Used by integrity-verification jobs (Phase 1C
     * remainder follow-up gap).
     */
    @Query("SELECT a FROM ChildProtectionAuditLog a "
            + "WHERE a.instanceId = :instanceId "
            + "AND a.entityType = :entityType "
            + "ORDER BY a.id ASC")
    List<ChildProtectionAuditLog> findChainAscending(
            @Param("instanceId") UUID instanceId,
            @Param("entityType") String entityType);

    /**
     * Read all entries for a single entity row (e.g. one Incident) across
     * its lifetime — useful for incident detail views + per-record
     * forensic timelines.
     */
    @Query("SELECT a FROM ChildProtectionAuditLog a "
            + "WHERE a.instanceId = :instanceId "
            + "AND a.entityType = :entityType "
            + "AND a.entityId = :entityId "
            + "ORDER BY a.id ASC")
    List<ChildProtectionAuditLog> findByEntity(
            @Param("instanceId") UUID instanceId,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    /**
     * Enumerate every distinct {@code (instance_id, entity_type)} chain
     * present in the table — used by the daily integrity verification cron
     * (Phase 1C v1.5, GAP-359 sub-task 359.5).
     *
     * <p>Returns {@code Object[]} pairs where {@code [0]} is the {@link UUID}
     * instance id and {@code [1]} is the {@link String} entity type. Used
     * sparingly (once per day) so the bare-Object[] contract is acceptable;
     * a dedicated DTO is overkill for a system-only caller.
     */
    @Query("SELECT DISTINCT a.instanceId, a.entityType "
            + "FROM ChildProtectionAuditLog a "
            + "ORDER BY a.instanceId ASC, a.entityType ASC")
    List<Object[]> findDistinctChains();
}
