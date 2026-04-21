package com.kitehub.platform.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Trial → Paid migration sub-state machine.
 *
 * Complements {@link InstanceStatus} (PENDING / TRIAL / ACTIVE / SUSPENDED / DELETED / PURGED)
 * by tracking the in-flight upgrade flow while {@code status} remains TRIAL until the final
 * atomic flip to ACTIVE at COMPLETED.
 *
 * <p>See {@code documents/01-business/kitehub/trial-to-paid-migration/rules.md §3} for the
 * canonical state diagram. Transitions are enforced by this enum's
 * {@link #canTransitionTo(MigrationPhase)} method; callers MUST check before mutating.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
public enum MigrationPhase {
    /** Default: no migration in flight. */
    NONE,

    /** User clicked Upgrade; payment request submitted. */
    INITIATED,

    /** Awaiting payment gateway confirmation. */
    PAYMENT_PENDING,

    /** Payment captured; migration queued. */
    PAYMENT_CAPTURED,

    /** Backend running validations + outbox events. */
    MIGRATING,

    /** status flipped TRIAL → ACTIVE; phase reset to NONE at next tick. */
    COMPLETED,

    /** Payment reversed within window; status rolled back to TRIAL. */
    REVERSED,

    /** Retries exhausted; alert ops; phase stays FAILED until manual intervention. */
    MIGRATION_FAILED;

    /**
     * Returns true if a transition from {@code this} phase to {@code next} is legal
     * per the state machine in rules.md §3.
     *
     * @param next target phase
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(MigrationPhase next) {
        return allowedTransitions().contains(next);
    }

    /**
     * Explicit allowed-next set for this phase. Returning an empty set means
     * this is a terminal phase (MIGRATION_FAILED requires manual intervention).
     *
     * @return phases that can legally follow {@code this}
     */
    public Set<MigrationPhase> allowedTransitions() {
        switch (this) {
            case NONE:
                return EnumSet.of(INITIATED);
            case INITIATED:
                return EnumSet.of(PAYMENT_PENDING, REVERSED);
            case PAYMENT_PENDING:
                return EnumSet.of(PAYMENT_CAPTURED, REVERSED);
            case PAYMENT_CAPTURED:
                return EnumSet.of(MIGRATING, REVERSED);
            case MIGRATING:
                return EnumSet.of(COMPLETED, MIGRATION_FAILED);
            case COMPLETED:
                // Can reset to NONE on next tick, or reverse within 24h window
                return EnumSet.of(NONE, REVERSED);
            case REVERSED:
                return EnumSet.of(NONE);
            case MIGRATION_FAILED:
                // Terminal until manual ops action — transition is admin-only
                return EnumSet.noneOf(MigrationPhase.class);
            default:
                return EnumSet.noneOf(MigrationPhase.class);
        }
    }

    /**
     * True if this phase is terminal (no further automatic transitions).
     *
     * @return true for MIGRATION_FAILED
     */
    public boolean isTerminal() {
        return this == MIGRATION_FAILED;
    }

    /**
     * True if this phase represents an in-flight migration (NONE and COMPLETED are the
     * only "settled" phases; others mean something is in progress).
     *
     * @return true when a migration is actively in flight
     */
    public boolean isInFlight() {
        return this != NONE && this != COMPLETED;
    }
}
