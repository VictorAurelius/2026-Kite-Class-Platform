package com.kitehub.subscription.impersonation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * Audit-log row for one admin "View as tenant" impersonation session
 * (GAP-040 Wave 79 Bucket F-bis).
 *
 * <p>Sister-entity to {@link com.kitehub.subscription.audit.AdminAuditLog}.
 * That entity captures generic admin actions; this one specializes in
 * 30-second-bounded support workflows where the admin assumes the tenant's
 * identity via a short-lived scoped JWT.</p>
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Admin POSTs {@code /api/v1/admin/impersonate/{tenantSlug}} —
 *       row created, {@code started_at} set, {@code ended_at} NULL.</li>
 *   <li>Either admin POSTs {@code /api/v1/admin/impersonate/end} (manual exit)
 *       OR 30-second window expires server-side — row updated with
 *       {@code ended_at} + {@code ended_reason}.</li>
 *   <li>Audit log queried via {@code GET /api/v1/admin/impersonate/audit-log}.</li>
 * </ol>
 *
 * @since Wave 79 (GAP-040)
 */
@Entity
@Table(name = "impersonation_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpersonationAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private UUID adminUserId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_slug", nullable = false, length = 100)
    private String tenantSlug;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ended_reason", length = 32)
    private EndedReason endedReason;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    /**
     * Why the impersonation session ended.
     *
     * <ul>
     *   <li>{@link #MANUAL_EXIT} — admin clicked "Thoát ra" (POST .../end).</li>
     *   <li>{@link #AUTO_TIMEOUT} — 30-second TTL elapsed without manual exit.</li>
     *   <li>{@link #NEVER} — reserved for future scope (e.g. session interrupted by
     *       admin logout); not yet emitted in v1.</li>
     * </ul>
     */
    public enum EndedReason {
        MANUAL_EXIT,
        AUTO_TIMEOUT,
        NEVER
    }
}
