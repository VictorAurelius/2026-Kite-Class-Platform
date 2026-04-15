package com.kiteclass.core.module.instance.approval;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Pre-rebrand approval gate (GAP-070). Enterprise tier requires a second admin to sign off
 * before {@code InstanceLifecycleService.rebrand} is invoked.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-APRV-001: status transitions enforced by {@link ApprovalStatus} machine</li>
 *   <li>BR-APRV-002: approver MUST be different from initiator (service-layer check)</li>
 *   <li>BR-APRV-003: auto-expires if not actioned before {@code expiresAt} (scheduler)</li>
 *   <li>BR-APRV-004: APPROVED / REJECTED / EXPIRED are terminal — no further mutation</li>
 * </ul>
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, GAP-070)
 */
@Entity
@Table(
        name = "rebrand_approvals",
        indexes = {
                @Index(name = "idx_rebrand_approval_target", columnList = "target_instance_id"),
                @Index(name = "idx_rebrand_approval_status", columnList = "status"),
                @Index(name = "idx_rebrand_approval_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RebrandApproval extends BaseEntity {

    @Column(name = "target_instance_id", nullable = false)
    private Long targetInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "initiator_user_id", nullable = false)
    private Long initiatorUserId;

    @Column(name = "approver_user_id")
    private Long approverUserId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Transition this approval to a target state; throws {@link IllegalStateException}
     * on invalid transitions (e.g. attempting to re-approve an already REJECTED request).
     */
    public void transitionTo(ApprovalStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid approval transition: " + status + " -> " + target);
        }
        this.status = target;
        Instant now = Instant.now();
        if (target == ApprovalStatus.APPROVED) {
            this.approvedAt = now;
        } else if (target == ApprovalStatus.REJECTED) {
            this.rejectedAt = now;
        }
    }
}
