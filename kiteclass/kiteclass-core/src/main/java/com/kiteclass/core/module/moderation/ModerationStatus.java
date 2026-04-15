package com.kiteclass.core.module.moderation;

import java.util.EnumSet;
import java.util.Set;

/**
 * State machine for {@link ModerationQueue} — content moderation lifecycle per ADR-010.
 *
 * <pre>
 *   PENDING → APPROVED              (Stage 1 pre-check passed)
 *   PENDING → REJECTED              (auto-reject: NSFW / banned keyword)
 *   PENDING → NEEDS_HUMAN_REVIEW    (borderline — escalate to admin queue)
 *   NEEDS_HUMAN_REVIEW → APPROVED   (human moderator signs off)
 *   NEEDS_HUMAN_REVIEW → REJECTED   (human moderator blocks)
 * </pre>
 *
 * Terminal: APPROVED / REJECTED.
 *
 * <p>Callers MUST NOT switch/if on {@code status} (banned anti-pattern per
 * {@code .claude/rules/design-patterns.md} §3.3). Use
 * {@link #canTransitionTo(ModerationStatus)}.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.1, GAP-018, ADR-010)
 */
public enum ModerationStatus {

    PENDING {
        @Override
        public Set<ModerationStatus> allowedTransitions() {
            return EnumSet.of(APPROVED, REJECTED, NEEDS_HUMAN_REVIEW);
        }
    },

    NEEDS_HUMAN_REVIEW {
        @Override
        public Set<ModerationStatus> allowedTransitions() {
            return EnumSet.of(APPROVED, REJECTED);
        }
    },

    APPROVED {
        @Override
        public Set<ModerationStatus> allowedTransitions() {
            return EnumSet.noneOf(ModerationStatus.class);
        }
    },

    REJECTED {
        @Override
        public Set<ModerationStatus> allowedTransitions() {
            return EnumSet.noneOf(ModerationStatus.class);
        }
    };

    public abstract Set<ModerationStatus> allowedTransitions();

    public boolean canTransitionTo(ModerationStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
