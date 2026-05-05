package com.kiteclass.core.module.childprotection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * ChildProtectionAuditLog — append-only hash-chain audit log for the
 * child-protection domain (per BR-CHILD-PROTECT-007, GAP-322c Phase 1C v1,
 * Wave 19 Bucket A).
 *
 * <p>Each entry records a single CRUD/state-change action on a child-protection
 * entity. The chain integrity is established via:
 * {@code content_hash = SHA-256(prev_hash || action_payload)}.
 *
 * <p><b>Append-only invariant:</b>
 * <ul>
 *   <li>This entity is intentionally NOT a {@code BaseEntity} subclass — it
 *       has no soft-delete flag. Once written, rows are immutable.</li>
 *   <li>The V54 migration {@code REVOKE DELETE} on this table from any role
 *       except superuser; application-tier never issues DELETE. Daily
 *       hash-chain integrity verification (Phase 1C remainder follow-up
 *       gap) detects break-ins where a superuser bypasses the grant.</li>
 *   <li>{@code instance_id} is populated explicitly by
 *       {@code ChildProtectionAuditService} from the current
 *       {@code TenantContext} — multi-tenant filtering happens via WHERE
 *       on read, never via Hibernate filter (the entity is not tenant-
 *       filtered to keep the chain readable from cross-tenant integrity
 *       cron jobs).</li>
 * </ul>
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
@Entity
@Table(
        name = "child_protection_audit_log",
        indexes = {
                @Index(name = "idx_cp_audit_instance_id", columnList = "instance_id"),
                @Index(name = "idx_cp_audit_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_cp_audit_actor", columnList = "actor_id"),
                @Index(name = "idx_cp_audit_occurred_at", columnList = "occurred_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildProtectionAuditLog {

    /**
     * Surrogate primary key. The hash chain is the integrity layer; this id
     * exists only for repository convenience.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tenant the audit entry belongs to. Populated by
     * {@code ChildProtectionAuditService} from {@code TenantContext}.
     * NOT subject to Hibernate {@code tenantFilter} — see class javadoc.
     */
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    /**
     * The entity type the action happened on (e.g. {@code "Incident"}).
     */
    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    /**
     * Surrogate id of the affected entity row (e.g. incidents.id).
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * High-level action name (e.g. {@code "INCIDENT_TRANSITION_CRITICAL"},
     * {@code "MANDATORY_REPORT_ACK"}). Free-form string — kept short for
     * grep-ability.
     */
    @Column(name = "action", nullable = false, length = 128)
    private String action;

    /**
     * User id of the actor (safeguarding officer, system listener, etc).
     * May be null for system-initiated transitions; the corresponding
     * {@link #action} should make the system actor explicit.
     */
    @Column(name = "actor_id")
    private Long actorId;

    /**
     * Wall-clock instant the action occurred. Populated by the service at
     * write time (NOT by JPA auditing — auditing is for mutable entities).
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * Hex-encoded SHA-256 of the previous chain entry's {@link #contentHash}.
     * For the first entry per (instance_id, entity_type) chain, this is the
     * 64-char zero string ({@code "0".repeat(64)}).
     */
    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;

    /**
     * Hex-encoded SHA-256 of {@code prev_hash || canonical-payload-json}.
     * Computed by {@code ChildProtectionAuditService.append(...)} at write
     * time. Verifying integrity = recomputing the chain from genesis.
     */
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    /**
     * Canonical JSON payload of the action (entity snapshot fragment +
     * actor + timestamps). Serialized at append-time. Bound length kept
     * generous — TEXT column at DB layer.
     */
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;
}
