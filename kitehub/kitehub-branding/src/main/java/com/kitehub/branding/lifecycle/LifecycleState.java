package com.kitehub.branding.lifecycle;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Branding instance lifecycle state per ai-branding-guidelines.md §6.
 *
 * <p>This is INSTANCE-level state — distinct from {@code BrandingJob.status}
 * which tracks per-job queue progression. The instance lifecycle answers
 * "what brand-state is this tenant in?", whereas job status answers
 * "what is this generation attempt doing right now?".</p>
 *
 * <p>State machine (per §6 of ai-branding-guidelines.md):
 * <pre>
 * NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING
 *                   ↓              ↓          ↑
 *                 FAILED ←────── FAILED ──────┘ (retry)
 * </pre>
 *
 * @since Wave 34 (GAP-272l)
 */
public enum LifecycleState {

    /** No branding generation has started for this instance yet. */
    NOT_STARTED,

    /** Provisioning underway (creating job row, validating tier quota, etc.). */
    INITIALIZING,

    /** AI / template pipeline is producing assets. */
    GENERATING,

    /** Quality gate passed; branding live for the tenant. */
    DEPLOYED,

    /** User-initiated regenerate while a previous DEPLOYED branding exists. */
    REGENERATING,

    /** Terminal failure (caller may transition back to INITIALIZING for retry). */
    FAILED;

    /**
     * Allowed forward transitions per §6 state diagram.
     */
    private static final Map<LifecycleState, Set<LifecycleState>> ALLOWED = Map.of(
        NOT_STARTED, EnumSet.of(INITIALIZING),
        INITIALIZING, EnumSet.of(GENERATING, FAILED),
        GENERATING, EnumSet.of(DEPLOYED, FAILED),
        DEPLOYED, EnumSet.of(REGENERATING),
        REGENERATING, EnumSet.of(DEPLOYED, FAILED),
        FAILED, EnumSet.of(INITIALIZING) // retry path
    );

    /** True if {@code from → this} is a permitted transition. */
    public boolean isReachableFrom(LifecycleState from) {
        if (from == null) {
            return this == NOT_STARTED || this == INITIALIZING;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(this);
    }
}
