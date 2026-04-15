package com.kiteclass.core.module.legal.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * State machine for {@link DmcaTakedownRequest} per ADR-012.
 *
 * <pre>
 *   PENDING → REVIEWING                   (intake accepted, human reviewer picks up)
 *   REVIEWING → VALID                     (notice deemed legitimate)
 *   REVIEWING → INVALID                   (notice deemed frivolous/invalid — terminal)
 *   VALID → EXECUTED                      (asset reverted to TEMPLATE — terminal)
 *   VALID → CONTESTED                     (counter-notice received — terminal)
 * </pre>
 *
 * Terminal: INVALID, EXECUTED, CONTESTED.
 *
 * <p>Mirrors the ApprovalStatus pattern — callers check {@link #canTransitionTo(DmcaStatus)}
 * before mutating the entity so invalid transitions throw immediately.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
public enum DmcaStatus {

    PENDING {
        @Override
        public Set<DmcaStatus> allowedTransitions() {
            return EnumSet.of(REVIEWING);
        }
    },
    REVIEWING {
        @Override
        public Set<DmcaStatus> allowedTransitions() {
            return EnumSet.of(VALID, INVALID);
        }
    },
    VALID {
        @Override
        public Set<DmcaStatus> allowedTransitions() {
            return EnumSet.of(EXECUTED, CONTESTED);
        }
    },
    INVALID {
        @Override
        public Set<DmcaStatus> allowedTransitions() {
            return EnumSet.noneOf(DmcaStatus.class);
        }
    },
    EXECUTED {
        @Override
        public Set<DmcaStatus> allowedTransitions() {
            return EnumSet.noneOf(DmcaStatus.class);
        }
    },
    CONTESTED {
        @Override
        public Set<DmcaStatus> allowedTransitions() {
            return EnumSet.noneOf(DmcaStatus.class);
        }
    };

    public abstract Set<DmcaStatus> allowedTransitions();

    public boolean canTransitionTo(DmcaStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
