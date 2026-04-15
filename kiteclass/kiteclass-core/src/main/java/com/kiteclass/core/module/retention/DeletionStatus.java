package com.kiteclass.core.module.retention;

import java.util.EnumSet;
import java.util.Set;

/**
 * State machine for {@link DeletionRequest} (GDPR Art. 17 erasure workflow, ADR-013).
 *
 * <pre>
 *   PENDING       → GRACE_PERIOD | PROCESSING | CANCELLED
 *   GRACE_PERIOD  → PROCESSING  | CANCELLED
 *   PROCESSING    → COMPLETED
 *   COMPLETED / CANCELLED are terminal.
 * </pre>
 *
 * <p>Rationale:
 * <ul>
 *   <li>PENDING: initial row — reversible; {@code graceEndsAt} set to now + 7 days on
 *       creation. Scheduler promotes to PROCESSING once {@code graceEndsAt} passes.</li>
 *   <li>GRACE_PERIOD: optional intermediate (e.g. post-email-confirmation); still
 *       reversible until scheduler promotes it.</li>
 *   <li>PROCESSING: purge/pseudonymize pipeline running; no longer reversible.</li>
 *   <li>COMPLETED: all retention actions applied; tombstone persisted.</li>
 *   <li>CANCELLED: user reversed the request during PENDING or GRACE_PERIOD.</li>
 * </ul>
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.4, ADR-013, GAP-073)
 */
public enum DeletionStatus {

    PENDING {
        @Override
        public Set<DeletionStatus> allowedTransitions() {
            return EnumSet.of(GRACE_PERIOD, PROCESSING, CANCELLED);
        }
    },
    GRACE_PERIOD {
        @Override
        public Set<DeletionStatus> allowedTransitions() {
            return EnumSet.of(PROCESSING, CANCELLED);
        }
    },
    PROCESSING {
        @Override
        public Set<DeletionStatus> allowedTransitions() {
            return EnumSet.of(COMPLETED);
        }
    },
    COMPLETED {
        @Override
        public Set<DeletionStatus> allowedTransitions() {
            return EnumSet.noneOf(DeletionStatus.class);
        }
    },
    CANCELLED {
        @Override
        public Set<DeletionStatus> allowedTransitions() {
            return EnumSet.noneOf(DeletionStatus.class);
        }
    };

    public abstract Set<DeletionStatus> allowedTransitions();

    public boolean canTransitionTo(DeletionStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
