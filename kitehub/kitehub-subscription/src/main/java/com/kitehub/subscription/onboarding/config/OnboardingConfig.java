package com.kitehub.subscription.onboarding.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wires {@code kitehub.onboarding.*} config keys documented in
 * {@code documents/01-business/kitehub/onboarding/rules.md} §Config.
 *
 * <p>Closes GAP-555 Wave 79 Bucket A — 2 unwired onboarding config keys.
 * Defaults match the rules.md column verbatim; production overrides land via
 * {@code application*.yml} per {@code production-env-config-registry.md} v1.1.1.</p>
 *
 * @since Wave 79 Bucket A — GAP-555
 */
@Component
@Getter
public class OnboardingConfig {

    private final String stepIds;
    private final int putRateLimitPerMin;

    public OnboardingConfig(
            @Value("${kitehub.onboarding.step-ids:PROFILE_SETUP,INVITE_TEAM,IMPORT_DATA,CREATE_FIRST_CLASS,EXPLORE_FEATURES}") String stepIds,
            @Value("${kitehub.onboarding.put-rate-limit-per-min:60}") int putRateLimitPerMin
    ) {
        this.stepIds = stepIds;
        this.putRateLimitPerMin = putRateLimitPerMin;
    }
}
