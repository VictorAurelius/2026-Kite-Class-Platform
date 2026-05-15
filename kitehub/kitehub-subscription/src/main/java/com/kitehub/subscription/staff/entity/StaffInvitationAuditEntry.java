package com.kitehub.subscription.staff.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Audit trail for state transitions of a {@link StaffInvitation}
 * (Wave 80 Bucket B, GAP-561b).
 *
 * <p>Mirrors the pattern of
 * {@link com.kitehub.subscription.impersonation.ImpersonationAuditEntry} — one
 * row per lifecycle event (CREATED / SENT / ACCEPTED / REVOKED / RESENT /
 * EXPIRED). Used for OWASP A09 admin audit log compliance per
 * {@code pre-launch-auth-hardening-checklist.md} §2.7.</p>
 *
 * <p>Persisted via JPA against table {@code staff_invitation_audit_log} created
 * lazily by Hibernate (see {@code ddl-auto} dev profile). Production profile
 * relies on a follow-up Flyway migration tracked GAP-561b §"Future scope".</p>
 *
 * @since Wave 80 — GAP-561b
 */
@Entity
@Table(name = "staff_invitation_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffInvitationAuditEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "invitation_id", nullable = false)
    private UUID invitationId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    /** Actor (Owner user id) or {@code null} for system events (accept / expire). */
    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "details", length = 512)
    private String details;

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.occurredAt == null) {
            this.occurredAt = OffsetDateTime.now();
        }
    }

    /**
     * Lifecycle events captured in the audit trail. Mirrors
     * {@link StaffInvitationStatus} transitions with the addition of {@code SENT}
     * (email dispatched) and {@code RESENT} (re-dispatch on same token until
     * expiry).
     */
    public enum EventType {
        /** Owner issued a fresh invitation row. */
        CREATED,
        /** Email payload handed off to email service. */
        SENT,
        /** Owner re-sent email for an existing invitation. */
        RESENT,
        /** Recipient successfully accepted, password set, user row created. */
        ACCEPTED,
        /** Owner cancelled before recipient accepted. */
        REVOKED,
        /** TTL crossed without acceptance (set by reaper). */
        EXPIRED
    }
}
