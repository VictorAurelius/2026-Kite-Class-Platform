package com.kitehub.subscription.onboarding.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Onboarding progress GET/PUT response shape (Wave 78 GAP-538).
 *
 * <p>Schema source-of-truth: {@code documents/01-business/kitehub/onboarding/api-contract.md}.</p>
 *
 * @since Wave 78 — GAP-538
 */
public record OnboardingProgressResponse(
        UUID tenantId,
        int completionPercent,
        int totalSteps,
        int completedSteps,
        OffsetDateTime lastUpdatedAt,
        List<OnboardingStepDto> steps
) {}
