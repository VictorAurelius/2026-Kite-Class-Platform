package com.kiteclass.core.module.legal.entity;

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
 * DMCA takedown notice received via public intake (ADR-012 Track 2).
 *
 * <p>Business rules (see {@code documents/01-business/kiteclass/legal-ip-protection/rules.md}):
 * <ul>
 *   <li>BR-DMCA-001: status transitions enforced by {@link DmcaStatus} state machine</li>
 *   <li>BR-DMCA-002: VALID → EXECUTED reverts affected asset to TEMPLATE category</li>
 *   <li>BR-DMCA-003: VALID → CONTESTED preserves asset until court order / grace expires</li>
 *   <li>BR-DMCA-004: every transition writes an AuditLog row (BR-AUDIT-001)</li>
 *   <li>BR-DMCA-005: INVALID / EXECUTED / CONTESTED are terminal — no further mutation</li>
 * </ul>
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
@Entity
@Table(
        name = "dmca_takedown_requests",
        indexes = {
                @Index(name = "idx_dmca_takedown_status", columnList = "status"),
                @Index(name = "idx_dmca_takedown_deleted", columnList = "deleted"),
                @Index(name = "idx_dmca_takedown_reporter_email", columnList = "reporter_email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmcaTakedownRequest extends BaseEntity {

    @Column(name = "reporter_email", nullable = false, length = 255)
    private String reporterEmail;

    @Column(name = "reporter_name", nullable = false, length = 255)
    private String reporterName;

    @Column(name = "alleged_infringing_url", nullable = false, length = 2000)
    private String allegedInfringingUrl;

    @Column(name = "copyrighted_work_description", nullable = false, length = 4000)
    private String copyrightedWorkDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private DmcaStatus status = DmcaStatus.PENDING;

    @Column(name = "counter_notice_email", length = 255)
    private String counterNoticeEmail;

    @Column(name = "reviewer_user_id")
    private Long reviewerUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "contested_at")
    private Instant contestedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Transition this takedown to a target state; throws {@link IllegalStateException}
     * on invalid transitions per {@link DmcaStatus}.
     */
    public void transitionTo(DmcaStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid DMCA takedown transition: " + status + " -> " + target);
        }
        this.status = target;
        Instant now = Instant.now();
        if (target == DmcaStatus.REVIEWING || target == DmcaStatus.VALID
                || target == DmcaStatus.INVALID) {
            this.reviewedAt = now;
        } else if (target == DmcaStatus.EXECUTED) {
            this.executedAt = now;
        } else if (target == DmcaStatus.CONTESTED) {
            this.contestedAt = now;
        }
    }
}
