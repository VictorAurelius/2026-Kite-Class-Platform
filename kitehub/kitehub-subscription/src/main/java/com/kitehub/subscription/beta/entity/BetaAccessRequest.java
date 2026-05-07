package com.kitehub.subscription.beta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * Beta tenant invite request (GAP-372 Wave 33 Phase 1 BETA invite-only model).
 *
 * <p>Stand-alone {@code BIGSERIAL} entity (NOT extending {@code BaseEntity})
 * mirroring {@link com.kitehub.subscription.dsar.entity.DsarTicket} precedent —
 * the migration uses {@code BIGSERIAL} for {@code id} per gap spec; the
 * {@code invite_token} (UUID) is the public-surface reference exposed in
 * invitation emails.</p>
 *
 * <p>State machine encoded by {@link BetaAccessRequestStatus}; transitions are
 * enforced by {@code BetaAccessService}, not by direct setter calls — see
 * {@code design-patterns.md §3.3 Status Pattern}.</p>
 *
 * @since Wave 33 — GAP-372
 */
@Entity
@Table(
        name = "beta_access_request",
        indexes = {
                @Index(name = "idx_beta_access_request_status", columnList = "status"),
                @Index(name = "idx_beta_access_request_email", columnList = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BetaAccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "org_name", nullable = false, length = 200)
    private String orgName;

    @Column(name = "persona", nullable = false, length = 50)
    private String persona;

    @Column(name = "referral_source", length = 500)
    private String referralSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private BetaAccessRequestStatus status = BetaAccessRequestStatus.PENDING;

    /** Single-use UUID issued at approve time. NULL while PENDING/REJECTED. */
    @Column(name = "invite_token")
    private UUID inviteToken;

    @Column(name = "invite_token_expiry")
    private OffsetDateTime inviteTokenExpiry;

    @Column(name = "invite_sent_at")
    private OffsetDateTime inviteSentAt;

    /**
     * Short-lived 6-digit numeric claim code (GAP-388 Wave 36 Bucket A 2FA).
     * Emailed to invitee instead of the raw {@code invite_token} UUID.
     * Single-use; cleared together with {@code invite_token} on SIGNED_UP.
     */
    @Column(name = "claim_code", length = 6)
    private String claimCode;

    @Column(name = "approver_id", length = 100)
    private String approverId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * PDPL 2023 Art 11 — explicit consent indicator (Wave 35 GAP-385).
     * MUST be {@code true} for inserts created post-Wave 35; backfilled rows
     * retain {@code FALSE} per V32 migration default.
     */
    @Column(name = "consent_given", nullable = false)
    @Builder.Default
    private boolean consentGiven = false;

    /**
     * PDPL 2023 Art 16 — consent evidence timestamp (Wave 35 GAP-385). Set by
     * service to {@code OffsetDateTime.now()} on insert; backfilled rows
     * receive {@code created_at} per V32 migration.
     */
    @Column(name = "consent_at", nullable = false)
    private OffsetDateTime consentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = BetaAccessRequestStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /** True when invite_token_expiry is in the past. */
    public boolean isTokenExpired() {
        return this.inviteTokenExpiry != null
                && this.inviteTokenExpiry.isBefore(OffsetDateTime.now());
    }
}
