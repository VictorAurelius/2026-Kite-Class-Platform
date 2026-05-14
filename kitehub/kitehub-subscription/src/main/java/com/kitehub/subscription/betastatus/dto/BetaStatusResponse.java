package com.kitehub.subscription.betastatus.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GET /api/v1/beta-status response shape (Wave 78 GAP-539).
 *
 * <p>Schema source-of-truth:
 * {@code documents/01-business/kitehub/beta-status/api-contract.md}.</p>
 *
 * @since Wave 78 — GAP-539
 */
public record BetaStatusResponse(
        String version,
        OffsetDateTime lastUpdatedAt,
        String contentMarkdown,
        String currentStatus,
        List<BetaStatusKnownIssue> knownIssues
) {}
