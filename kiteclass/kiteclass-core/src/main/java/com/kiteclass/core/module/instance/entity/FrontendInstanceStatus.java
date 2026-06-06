package com.kiteclass.core.module.instance.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * State machine for frontend instance provisioning + off-boarding lifecycle (GAP-009, ADR-004,
 * GAP-954 PDPL Art 23).
 *
 * <pre>
 * NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING
 *                    ↓              ↓          ↑  ↓
 *                  FAILED ←───── FAILED ───────┘ SUSPENDED ⇄ DEPLOYED
 *                                                   ↓
 *                                                 DELETED (terminal — 30d grace then hard purge)
 * </pre>
 *
 * <p>Each enum value declares allowed transitions; {@link #canTransitionTo(FrontendInstanceStatus)}
 * is the only authority. Callers MUST NOT use switch/if cascades on status — this violates the
 * banned anti-pattern in {@code .claude/rules/design-patterns.md} §3.3.
 *
 * <p>Off-boarding states (GAP-954): a DEPLOYED tenant can be SUSPENDED (subscription expired /
 * payment failed) and reactivated, or moved on to DELETED (soft-delete). DELETED is terminal in
 * this FSM — after the 30-day PDPL Art 23 retention grace, the cross-service hard purge runs in
 * {@code kitehub-subscription InstancePurgeService} (DB drop + MinIO/DNS/logo cascade + PURGED).
 *
 * @since 3.15.0 (GAP-009, ADR-004)
 */
public enum FrontendInstanceStatus {

    NOT_STARTED {
        @Override
        public Set<FrontendInstanceStatus> allowedTransitions() {
            return EnumSet.of(INITIALIZING);
        }
    },

    INITIALIZING {
        @Override
        public Set<FrontendInstanceStatus> allowedTransitions() {
            return EnumSet.of(GENERATING, FAILED);
        }
    },

    GENERATING {
        @Override
        public Set<FrontendInstanceStatus> allowedTransitions() {
            return EnumSet.of(DEPLOYED, FAILED);
        }
    },

    DEPLOYED {
        @Override
        public Set<FrontendInstanceStatus> allowedTransitions() {
            return EnumSet.of(REGENERATING, SUSPENDED);
        }
    },

    REGENERATING {
        @Override
        public Set<FrontendInstanceStatus> allowedTransitions() {
            return EnumSet.of(DEPLOYED, FAILED);
        }
    },

    FAILED {
        @Override
        public Set<FrontendInstanceStatus> allowedTransitions() {
            return EnumSet.of(INITIALIZING);
        }
    },

    /**
     * Tenant temporarily disabled (subscription expired / payment failed). Reversible — can be
     * reactivated back to DEPLOYED, or moved on to DELETED. GAP-954.
     */
    SUSPENDED {
        @Override
        public Set<FrontendInstanceStatus> allowedTransitions() {
            return EnumSet.of(DEPLOYED, DELETED);
        }
    },

    /**
     * Soft-deleted. Terminal in this FSM — the 30-day PDPL Art 23 retention grace runs, then the
     * cross-service hard purge (DB drop + MinIO/DNS/logo cascade) executes in kitehub-subscription.
     * GAP-954.
     */
    DELETED {
        @Override
        public Set<FrontendInstanceStatus> allowedTransitions() {
            return EnumSet.noneOf(FrontendInstanceStatus.class);
        }
    };

    public abstract Set<FrontendInstanceStatus> allowedTransitions();

    public boolean canTransitionTo(FrontendInstanceStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
