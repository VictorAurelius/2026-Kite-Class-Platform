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
 * Persistent record of an Owner→Staff invitation.
 *
 * <p>Backs {@code BR-ROLE-INVITE-001..005} (see
 * {@code documents/01-business/roles/rules.md}) and
 * {@code UC-ROLE-STAFF-INVITE} (see
 * {@code documents/01-business/roles/use-cases.md}).</p>
 *
 * <p>Security model: {@code token_hash} stored is SHA-256 of the opaque token
 * delivered to the recipient via email. The raw token is NEVER persisted; at
 * accept-time the URL token is re-hashed and compared. Single-use enforced by
 * {@link StaffInvitationStatus} transition to {@code ACCEPTED}.</p>
 *
 * @since Wave 79 — GAP-561 / GAP-562
 */
@Entity
@Table(name = "staff_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffInvitation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /** Owner user id who issued this invitation. */
    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    /** SHA-256 hex digest of the raw token sent via email. */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private StaffInvitationStatus status = StaffInvitationStatus.PENDING;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "accepted_user_id")
    private UUID acceptedUserId;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.expiresAt == null) {
            // BR-ROLE-INVITE-TTL: 7 days default
            this.expiresAt = now.plusDays(7);
        }
        if (this.status == null) {
            this.status = StaffInvitationStatus.PENDING;
        }
    }

    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(OffsetDateTime.now());
    }

    public boolean isPending() {
        return this.status == StaffInvitationStatus.PENDING && !isExpired();
    }
}
