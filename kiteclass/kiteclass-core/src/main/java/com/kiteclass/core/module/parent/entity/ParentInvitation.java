package com.kiteclass.core.module.parent.entity;

import com.kiteclass.core.common.constant.ParentInvitationStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import java.util.UUID;

/**
 * Token-based parent onboarding invitation.
 *
 * <p>Issued by an admin/teacher for a specific child; the parent follows a
 * link ({@code /parent-invite/{token}}) in the email to set a password and
 * complete profile. Upon redemption the service creates a {@link Parent}, a
 * {@link ParentStudentLink}, and a matching Gateway User (userType = PARENT,
 * referenceId = parent.id), and marks this row {@link
 * ParentInvitationStatus#REDEEMED}.
 *
 * <p>The token has 128-bit entropy ({@code UUID.randomUUID().toString()}) and
 * expires after the TTL configured by {@code kiteclass.parent-portal
 * .invitation-ttl-hours} (default 24 h). A scheduled job sweeps PENDING rows
 * past expiry into {@link ParentInvitationStatus#EXPIRED}.
 *
 * @author KiteClass Team
 * @since 2.14.0 (Wave 2 — GAP-052a)
 */
@Entity
@Table(
        name = "parent_invitations",
        indexes = {
                @Index(name = "idx_inv_token", columnList = "token", unique = true),
                @Index(name = "idx_inv_email", columnList = "email"),
                @Index(name = "idx_inv_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentInvitation extends BaseEntity {

    /** Email the invitation was sent to — will become the parent's login email. */
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** The child this invitation will be linked to upon redemption. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Opaque 128-bit token used as the unique redemption key. */
    @NotBlank
    @Size(max = 64)
    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    /** Current lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ParentInvitationStatus status = ParentInvitationStatus.PENDING;

    /** Absolute timestamp after which PENDING invitations are no longer valid. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Gateway user id (UUID, X-User-Id / JWT sub) of the admin/teacher who issued the invitation (GAP-795). */
    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;

    /** Set when the parent completes the redemption flow. */
    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    /**
     * FK to the {@link Parent} row produced by redemption. Kept as a plain Long
     * (not a {@code @ManyToOne}) to avoid a circular load path and to allow
     * REDEEMED invitations to persist even if the parent is later soft-deleted.
     */
    @Column(name = "redeemed_parent_id")
    private Long redeemedParentId;

    /**
     * Convenience — true when the row is PENDING and {@link #expiresAt} is still
     * in the future.
     */
    public boolean isRedeemable() {
        return status == ParentInvitationStatus.PENDING
                && expiresAt != null
                && expiresAt.isAfter(Instant.now());
    }
}
