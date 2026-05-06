package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.module.childprotection.entity.ChildProtectionAuditLog;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ChildProtectionAuditService — append-only hash-chain audit log for
 * child-protection domain mutations (BR-CHILD-PROTECT-007, GAP-322c Phase 1C v1).
 *
 * <p>The service computes
 * {@code content_hash = SHA-256(prev_hash || canonical-payload)} and
 * persists the entry. {@code prev_hash} is read from the latest entry in
 * the {@code (instance_id, entity_type)} chain (or the 64-char zero
 * string for genesis).
 *
 * <p><b>No mutation methods.</b> Only append + read.
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
public interface ChildProtectionAuditService {

    /**
     * Append a new entry to the chain identified by
     * {@code (currentTenant, entityType)}. The {@code payload} map is
     * canonicalized to JSON (sorted keys) before hashing so identical
     * inputs produce identical hashes regardless of insertion order.
     *
     * @param entityType e.g. {@code "Incident"} — must match the entity
     *                   class simple name to keep chains aligned with code
     * @param entityId   surrogate id of the row the action applied to
     * @param action     short action name (e.g.
     *                   {@code "INCIDENT_TRANSITION_CRITICAL"})
     * @param actorId    actor user id; nullable for system actions
     * @param payload    arbitrary structured payload to canonicalize +
     *                   hash; MUST be JSON-serializable
     * @return the persisted entry with id + hashes populated
     */
    ChildProtectionAuditLog append(
            String entityType,
            Long entityId,
            String action,
            Long actorId,
            Map<String, Object> payload);

    /**
     * Read all entries for one entity row, oldest first. Used for
     * forensic timelines + per-record audit views.
     */
    List<ChildProtectionAuditLog> findByEntity(String entityType, Long entityId);

    /**
     * Verify that the in-DB chain for the current tenant + entity type
     * recomputes correctly from genesis. Returns {@code true} if every
     * row's {@code content_hash} matches
     * {@code SHA-256(prev_hash || payload_json)}.
     *
     * <p>Phase 1C v1 exposes this for unit tests; the daily integrity cron
     * uses the explicit-instance overload {@link #verifyChain(UUID, String)}
     * because it runs system-wide without a {@link com.kiteclass.core.common.context.TenantContext}.
     */
    boolean verifyChainIntegrity(String entityType);

    /**
     * Explicit-instance variant of {@link #verifyChainIntegrity(String)}
     * used by the daily integrity verification cron (Phase 1C v1.5,
     * GAP-359 sub-task 359.5).
     *
     * <p>Bypasses {@code TenantContext} so the cron can iterate every
     * {@code (instance_id, entity_type)} chain returned by
     * {@code ChildProtectionAuditLogRepository.findDistinctChains()}
     * without imposing a per-tenant scheduling layer.
     *
     * @param instanceId the tenant whose chain to verify
     * @param entityType the entity type segment (e.g. {@code "Incident"})
     * @return {@code true} if every entry's {@code content_hash} recomputes
     *         to itself when fed {@code prev_hash || payload_json}
     */
    boolean verifyChain(UUID instanceId, String entityType);
}
