package com.kiteclass.core.module.staff.entity;

import com.kiteclass.core.common.constant.StaffInvitationStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Token-based staff onboarding invitation.
 *
 * <p>Issued by an Owner (or Admin) for a future staff member at the tenant.
 * The invitee receives an email with a link {@code /staff/accept-invite/{token}}
 * (FE route — GAP-773 paired) that lets them set a password and complete
 * profile. Upon acceptance the Gateway creates a User row with role STAFF and
 * marks this invitation {@link StaffInvitationStatus#ACCEPTED}.
 *
 * <p>The token has 128-bit entropy ({@code UUID.randomUUID().toString()}) and
 * expires after the TTL configured by {@code
 * kiteclass.staff-invite.invitation-ttl-hours} (default 168 = 7 days). A
 * scheduled job sweeps PENDING rows past expiry into {@link
 * StaffInvitationStatus#EXPIRED}.
 *
 * <p>Mirror of {@link com.kiteclass.core.module.parent.entity.ParentInvitation}
 * but without student linkage — staff identity belongs to tenant scope only.
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
@Entity
@Table(
        name = "staff_invitations",
        indexes = {
                @Index(name = "idx_staff_inv_token", columnList = "token", unique = true),
                @Index(name = "idx_staff_inv_email", columnList = "email"),
                @Index(name = "idx_staff_inv_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffInvitation extends BaseEntity {

    /** Email the invitation was sent to — becomes the staff login email. */
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Staff role to provision on acceptance. Stored as enum string for forward
     * compatibility ({@code STAFF}, {@code TEACHER}, {@code MANAGER}). Owner
     * role cannot be invited via this flow (single owner per tenant invariant).
     */
    @NotBlank
    @Size(max = 32)
    @Column(name = "role", nullable = false, length = 32)
    private String role;

    /** Opaque 128-bit token used as the unique redemption key. */
    @NotBlank
    @Size(max = 64)
    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    /** Current lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StaffInvitationStatus status = StaffInvitationStatus.PENDING;

    /** Absolute timestamp after which PENDING invitations are no longer valid. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Gateway user id of the Owner/Admin who issued the invitation. */
    @Column(name = "invited_by_user_id")
    private Long invitedByUserId;

    /** Set when the staff completes the acceptance flow. */
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    /**
     * FK to the Gateway User row produced by acceptance. Kept as a plain Long
     * (not a {@code @ManyToOne}) because the User entity lives in the gateway
     * service, not in kiteclass-core. Allows ACCEPTED invitations to persist
     * even if the staff user is later soft-deleted.
     */
    @Column(name = "accepted_user_id")
    private Long acceptedUserId;

    /**
     * Convenience — true when the row is PENDING and {@link #expiresAt} is still
     * in the future.
     */
    public boolean isRedeemable() {
        return status == StaffInvitationStatus.PENDING
                && expiresAt != null
                && expiresAt.isAfter(Instant.now());
    }
}
