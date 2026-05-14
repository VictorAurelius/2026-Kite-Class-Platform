package com.kitehub.subscription.onboarding.domain;

/**
 * Whitelisted Day-1 onboarding step identifiers (Wave 78 GAP-538).
 *
 * <p>Schema source-of-truth:
 * {@code documents/01-business/kitehub/onboarding/api-contract.md}.</p>
 *
 * <p>5 hardcoded steps for Phase 1 BETA. Client MUST send {@code stepId} that
 * matches one of these enum values; any other value yields HTTP 400
 * {@code ONBOARDING_INVALID_STEP_ID}.</p>
 *
 * <p>Step semantics:</p>
 * <ul>
 *   <li>{@link #PROFILE_SETUP} — tenant logo + name + persona confirmed.</li>
 *   <li>{@link #INVITE_TEAM} — add ≥1 other user, or skip.</li>
 *   <li>{@link #IMPORT_DATA} — opt-in sample/demo data seed (gated by
 *   {@code tenant.metadata.is_beta_demo_data} flag — explicit user opt-in).</li>
 *   <li>{@link #CREATE_FIRST_CLASS} — try core KiteClass feature.</li>
 *   <li>{@link #EXPLORE_FEATURES} — feature tour modal or skip.</li>
 * </ul>
 *
 * @since Wave 78 — GAP-538
 */
public enum OnboardingStepId {
    PROFILE_SETUP,
    INVITE_TEAM,
    IMPORT_DATA,
    CREATE_FIRST_CLASS,
    EXPLORE_FEATURES
}
