package com.kitehub.subscription.onboarding.dto;

import com.kitehub.subscription.onboarding.domain.OnboardingStepId;

import java.time.OffsetDateTime;

/**
 * One step entry inside {@link OnboardingProgressResponse#steps()} (Wave 78 GAP-538).
 *
 * <p>{@code completedAt} is null when {@code completed=false} (or when never
 * completed).</p>
 *
 * @since Wave 78 — GAP-538
 */
public record OnboardingStepDto(
        OnboardingStepId stepId,
        boolean completed,
        OffsetDateTime completedAt
) {}
