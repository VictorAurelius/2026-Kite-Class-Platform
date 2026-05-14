package com.kitehub.subscription.feedback.dto;

import com.kitehub.subscription.feedback.entity.FeedbackSubmission;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response body for POST /api/v1/feedback (GAP-542 Wave 78 Bucket F).
 *
 * <p>Schema source: {@code documents/01-business/kitehub/feedback/api-contract.md}.
 * Intentionally does NOT echo back {@code comment} / {@code email} / {@code pageUrl}
 * — reduces payload + avoids leak via browser cache.</p>
 */
public record FeedbackSubmissionResponse(
        UUID id,
        Integer rating,
        String category,
        OffsetDateTime createdAt,
        String status
) {
    public static FeedbackSubmissionResponse from(FeedbackSubmission entity) {
        return new FeedbackSubmissionResponse(
                entity.getPublicId(),
                entity.getRating() == null ? null : entity.getRating().intValue(),
                entity.getCategory(),
                entity.getCreatedAt(),
                entity.getStatus()
        );
    }
}
