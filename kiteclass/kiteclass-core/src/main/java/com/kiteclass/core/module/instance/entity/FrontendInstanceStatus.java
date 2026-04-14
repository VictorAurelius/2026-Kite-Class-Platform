package com.kiteclass.core.module.instance.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * State machine for frontend instance provisioning lifecycle (GAP-009, ADR-004).
 *
 * <pre>
 * NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING
 *                    ↓              ↓          ↑
 *                  FAILED ←───── FAILED ───────┘ (retry)
 * </pre>
 *
 * <p>Each enum value declares allowed transitions; {@link #canTransitionTo(FrontendInstanceStatus)}
 * is the only authority. Callers MUST NOT use switch/if cascades on status — this violates the
 * banned anti-pattern in {@code .claude/rules/design-patterns.md} §3.3.
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
            return EnumSet.of(REGENERATING);
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
    };

    public abstract Set<FrontendInstanceStatus> allowedTransitions();

    public boolean canTransitionTo(FrontendInstanceStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
