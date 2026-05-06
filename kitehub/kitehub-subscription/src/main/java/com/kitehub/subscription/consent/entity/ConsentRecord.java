package com.kitehub.subscription.consent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Server-side PDPL consent audit record.
 *
 * <p>Backs `BR-PDPL-CONSENT-001..004` (see
 * {@code documents/01-business/kitehub/marketing/rules.md}). Stand-alone entity
 * (NOT extending {@code BaseEntity}) because the migration uses {@code BIGSERIAL}
 * for {@code id} per gap spec — {@code BaseEntity} is UUID-based.</p>
 *
 * <p>visitor_id is the pseudonymous key — UUID v4 generated client-side and
 * persisted in LocalStorage. {@code userId} + {@code tenantId} stay nullable
 * for marketing-surface visitors who haven't signed up.</p>
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@Entity
@Table(name = "consent_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Pseudonymous client-generated UUID (LocalStorage key {@code kite_visitor_id}). */
    @Column(name = "visitor_id", nullable = false)
    private UUID visitorId;

    /** Nullable: populated after login; back-fillable. */
    @Column(name = "user_id")
    private Long userId;

    /** Nullable: marketing-surface visitors pre-tenant. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "essential_consented", nullable = false)
    @Builder.Default
    private Boolean essentialConsented = Boolean.TRUE;

    @Column(name = "analytics_consented", nullable = false)
    @Builder.Default
    private Boolean analyticsConsented = Boolean.FALSE;

    @Column(name = "marketing_consented", nullable = false)
    @Builder.Default
    private Boolean marketingConsented = Boolean.FALSE;

    @Column(name = "consent_version", nullable = false)
    @Builder.Default
    private Integer consentVersion = 1;

    /**
     * IPv4 or IPv6 textual form (max 45 chars per RFC 4291). Stored as VARCHAR
     * for H2-test ↔ Postgres-prod schema parity.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Re-prompt deadline: 12 months from creation per BR-PDPL-CONSENT-002.
     * Distinct from DR-03 36-month retention enforced by
     * {@code ConsentRetentionCron}.
     */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.expiresAt == null) {
            this.expiresAt = now.plusMonths(12);
        }
        if (this.essentialConsented == null) {
            this.essentialConsented = Boolean.TRUE;
        }
        if (this.analyticsConsented == null) {
            this.analyticsConsented = Boolean.FALSE;
        }
        if (this.marketingConsented == null) {
            this.marketingConsented = Boolean.FALSE;
        }
        if (this.consentVersion == null) {
            this.consentVersion = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /** True iff this record has been explicitly revoked. */
    public boolean isRevoked() {
        return this.revokedAt != null;
    }
}
