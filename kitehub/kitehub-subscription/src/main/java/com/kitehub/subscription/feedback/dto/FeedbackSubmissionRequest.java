package com.kitehub.subscription.feedback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/feedback (GAP-542 Wave 78 Bucket F).
 *
 * <p>Schema source: {@code documents/01-business/kitehub/feedback/api-contract.md}.
 * Field constraints mirror the contract table 1:1.</p>
 *
 * @param rating    1-5 (1 = very poor, 5 = excellent)
 * @param comment   5-2000 chars trimmed
 * @param email     optional — RFC-5321 ≤320 chars
 * @param pageUrl   optional — FE auto-populates window.location.href, ≤2000 chars
 * @param category  optional — BUG | USABILITY | FEATURE_REQUEST | GENERAL
 * @param honeypot  MUST equal empty string — bot trap
 */
public record FeedbackSubmissionRequest(
        @NotNull(message = "rating is required")
        @Min(value = 1, message = "rating must be in [1..5]")
        @Max(value = 5, message = "rating must be in [1..5]")
        Integer rating,

        @NotNull(message = "comment is required")
        @Size(min = 5, max = 2000, message = "comment must be 5-2000 chars")
        String comment,

        @Email(message = "email must be a valid email address")
        @Size(max = 320, message = "email too long")
        String email,

        @Size(max = 2000, message = "pageUrl too long")
        String pageUrl,

        @Pattern(
                regexp = "BUG|USABILITY|FEATURE_REQUEST|GENERAL",
                message = "category must be one of: BUG, USABILITY, FEATURE_REQUEST, GENERAL"
        )
        String category,

        @Size(max = 0, message = "honeypot must be empty")
        String honeypot
) {
}
