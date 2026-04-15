package com.kiteclass.core.module.moderation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Immutable outcome of a {@link ContentModerationService#check} call.
 *
 * <p>Returned to callers (typically {@code PublishPackageStep} in the AI branding pipeline
 * per ADR-010). Callers consult {@link #getStatus()} to decide whether to block DEPLOY;
 * {@link #getScore()} exposes the Stage 1 NSFW/keyword score for logging/metrics.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.1, GAP-018, ADR-010)
 */
@Value
@Builder
public class ModerationResult {

    /** Terminal or escalated state — never {@link ModerationStatus#PENDING}. */
    ModerationStatus status;

    /** Stage 1 score in [0.0, 1.0]; 0.0 = clean, 1.0 = unsafe. */
    double score;

    /** Keywords that triggered the block (empty if none). */
    List<String> flaggedKeywords;

    /** Short human-readable reason (e.g. "banned keyword: violence"). */
    String reason;

    public boolean isApproved() {
        return status == ModerationStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == ModerationStatus.REJECTED;
    }

    public boolean needsHumanReview() {
        return status == ModerationStatus.NEEDS_HUMAN_REVIEW;
    }
}
