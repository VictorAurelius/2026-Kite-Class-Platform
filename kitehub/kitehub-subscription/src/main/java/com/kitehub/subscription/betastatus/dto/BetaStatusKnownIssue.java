package com.kitehub.subscription.betastatus.dto;

import java.time.LocalDate;

/**
 * One known issue entry in the beta-status response (Wave 78 GAP-539).
 *
 * @param title short, Vietnamese, ≤200 chars
 * @param severity {@code MINOR | MAJOR | CRITICAL}
 * @param since date issue was first detected
 *
 * @since Wave 78 — GAP-539
 */
public record BetaStatusKnownIssue(
        String title,
        String severity,
        LocalDate since
) {}
