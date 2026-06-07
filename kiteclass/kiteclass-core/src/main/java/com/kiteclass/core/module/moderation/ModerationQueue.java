package com.kiteclass.core.module.moderation;

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
import java.util.UUID;

/**
 * Persistent row for content items flagged by {@link ContentModerationService}.
 *
 * <p>Primary use is the Stage X admin review queue (ADR-010) — rows with status
 * {@link ModerationStatus#NEEDS_HUMAN_REVIEW} wait here until a moderator transitions
 * them to APPROVED / REJECTED. Auto-rejected rows are ALSO persisted so the audit
 * trail and admin dashboards have a single source of truth.
 *
 * <p>Business Rules (see {@code documents/01-business/kiteclass/content-moderation/rules.md}):
 * <ul>
 *   <li>BR-MOD-001: status transitions enforced by {@link ModerationStatus} machine</li>
 *   <li>BR-MOD-002: APPROVED / REJECTED terminal — no further mutation</li>
 *   <li>BR-MOD-003: at most one NON-TERMINAL row per (targetType, targetId)</li>
 *   <li>BR-MOD-004: every transition emits an {@code AuditLog} row (BR-AUDIT-001)</li>
 * </ul>
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.1, GAP-018, ADR-010)
 */
@Entity
@Table(
        name = "moderation_queue",
        indexes = {
                @Index(name = "idx_moderation_status", columnList = "status"),
                @Index(name = "idx_moderation_target",
                        columnList = "target_type,target_id"),
                @Index(name = "idx_moderation_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationQueue extends BaseEntity {

    /** Logical type (e.g. {@code "branding.logo"}, {@code "branding.banner"}). */
    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;

    /** Identifier of the item under review (format owned by caller). */
    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private ModerationStatus status = ModerationStatus.PENDING;

    /** Stage 1 composite score in [0.0, 1.0]. */
    @Column(name = "score", nullable = false)
    @Builder.Default
    private Double score = 0.0;

    /** Keywords that triggered a block, serialized as JSON array. */
    @Column(name = "flagged_keywords", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)  // GAP-220
    private String flaggedKeywords;

    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Admin user (X-User-Id UUID, GAP-795) assigned to adjudicate when
     * status = NEEDS_HUMAN_REVIEW. GAP-877: column retyped BIGINT -> uuid in V94.
     */
    @Column(name = "assigned_reviewer_id")
    private UUID assignedReviewerId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /**
     * Transition this row to a target state; throws {@link IllegalStateException}
     * on invalid transitions (e.g. re-approving an already REJECTED row).
     *
     * <p>Callers MUST still write the matching {@code AuditLog} row inside the
     * SAME transaction — this method only mutates the entity.
     *
     * @param target non-null target state
     */
    public void transitionTo(ModerationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid moderation transition: " + status + " -> " + target);
        }
        this.status = target;
        if (target.isTerminal()) {
            this.decidedAt = Instant.now();
        }
    }
}
