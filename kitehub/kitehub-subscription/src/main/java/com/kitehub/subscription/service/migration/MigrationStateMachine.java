package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.exception.MigrationException;

import java.time.LocalDateTime;

/**
 * Pure state-machine collaborator extracted from {@code TrialToPaidService}
 * (Sub-PR 6.2 — design-pattern audit hotspot fix per {@code .claude/rules/design-patterns.md} §3.1).
 *
 * <p>Owns the phase-transition invariants previously living as private helpers on the
 * facade: {@link MigrationPhase#canTransitionTo} guard, rescue-window check, reversal-window
 * check. No I/O — callers persist via the {@code Instance} they pass in.</p>
 */
public class MigrationStateMachine {

    private final TrialToPaidConfig config;

    public MigrationStateMachine(TrialToPaidConfig config) {
        this.config = config;
    }

    /**
     * Enforce state-machine transitions from {@link MigrationPhase#canTransitionTo}.
     * Callers must always use this helper — never set {@code migrationPhase} directly.
     */
    public void transitionPhase(Instance instance, MigrationPhase target, LocalDateTime ts) {
        MigrationPhase current = instance.getMigrationPhase();
        if (current == null) {
            current = MigrationPhase.NONE;
        }
        if (!current.canTransitionTo(target)) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Illegal transition " + current + " → " + target);
        }
        instance.setMigrationPhase(target);
        if (target == MigrationPhase.INITIATED) {
            instance.setMigrationStartedAt(ts);
        }
    }

    public void assertCanStartMigration(Instance instance) {
        if (instance.getMigrationPhase() == MigrationPhase.MIGRATION_FAILED) {
            throw new MigrationException(MigrationException.Code.MIGRATION_FAILED_LOCKED,
                "Instance is in MIGRATION_FAILED; manual ops action required");
        }
        if (instance.getMigrationPhase() != MigrationPhase.NONE
            && instance.getMigrationPhase() != MigrationPhase.COMPLETED
            && instance.getMigrationPhase() != MigrationPhase.REVERSED) {
            throw new MigrationException(MigrationException.Code.MIGRATION_IN_FLIGHT,
                "Another migration already in flight: " + instance.getMigrationPhase());
        }
    }

    public void assertWithinRescueWindowOrStillTrial(Instance instance) {
        if (instance.getStatus() != InstanceStatus.TRIAL) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Instance is not on TRIAL (status=" + instance.getStatus() + ")");
        }
        if (instance.getTrialExpiresAt() != null
            && LocalDateTime.now().isAfter(
                instance.getTrialExpiresAt().plusHours(config.getRescueWindowHours()))) {
            throw new MigrationException(MigrationException.Code.RESCUE_WINDOW_EXPIRED,
                "Trial expired beyond rescue window of " + config.getRescueWindowHours() + "h");
        }
    }

    public boolean isWithinReversalWindow(Instance instance) {
        if (instance.getMigrationCompletedAt() == null) {
            return false;
        }
        LocalDateTime deadline = instance.getMigrationCompletedAt()
            .plusHours(config.getReversalWindowHours());
        return LocalDateTime.now().isBefore(deadline);
    }
}
