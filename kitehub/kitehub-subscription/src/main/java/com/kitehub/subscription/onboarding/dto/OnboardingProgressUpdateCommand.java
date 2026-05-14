package com.kitehub.subscription.onboarding.dto;

import com.kitehub.subscription.onboarding.domain.OnboardingStepId;
import jakarta.validation.constraints.NotNull;

/**
 * PUT /api/v1/onboarding-progress body (Wave 78 GAP-538).
 *
 * @param stepId one of {@link OnboardingStepId} enum values; null/unknown → 400
 * @param completed true to mark step done; false to un-mark (idempotent uncheck)
 *
 * @since Wave 78 — GAP-538
 */
public record OnboardingProgressUpdateCommand(
        @NotNull OnboardingStepId stepId,
        @NotNull Boolean completed
) {}
