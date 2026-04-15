package com.kiteclass.core.module.instance.approval;

import java.util.EnumSet;
import java.util.Set;

/**
 * State machine for {@link RebrandApproval}.
 *
 * <pre>
 *   PENDING → APPROVED   (admin approves; lifecycle transition DEPLOYED → REGENERATING runs)
 *   PENDING → REJECTED   (admin rejects; no lifecycle change)
 *   PENDING → EXPIRED    (scheduler after expiresAt; no lifecycle change)
 * </pre>
 *
 * Terminal: APPROVED / REJECTED / EXPIRED.
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, GAP-070)
 */
public enum ApprovalStatus {

    PENDING {
        @Override
        public Set<ApprovalStatus> allowedTransitions() {
            return EnumSet.of(APPROVED, REJECTED, EXPIRED);
        }
    },
    APPROVED {
        @Override
        public Set<ApprovalStatus> allowedTransitions() {
            return EnumSet.noneOf(ApprovalStatus.class);
        }
    },
    REJECTED {
        @Override
        public Set<ApprovalStatus> allowedTransitions() {
            return EnumSet.noneOf(ApprovalStatus.class);
        }
    },
    EXPIRED {
        @Override
        public Set<ApprovalStatus> allowedTransitions() {
            return EnumSet.noneOf(ApprovalStatus.class);
        }
    };

    public abstract Set<ApprovalStatus> allowedTransitions();

    public boolean canTransitionTo(ApprovalStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
