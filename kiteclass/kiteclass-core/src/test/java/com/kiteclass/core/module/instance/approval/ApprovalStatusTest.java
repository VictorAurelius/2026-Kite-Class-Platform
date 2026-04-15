package com.kiteclass.core.module.instance.approval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalStatusTest {

    @Test
    void pending_allows_three_terminal_transitions() {
        assertThat(ApprovalStatus.PENDING.canTransitionTo(ApprovalStatus.APPROVED)).isTrue();
        assertThat(ApprovalStatus.PENDING.canTransitionTo(ApprovalStatus.REJECTED)).isTrue();
        assertThat(ApprovalStatus.PENDING.canTransitionTo(ApprovalStatus.EXPIRED)).isTrue();
        assertThat(ApprovalStatus.PENDING.canTransitionTo(ApprovalStatus.PENDING)).isFalse();
    }

    @Test
    void terminal_states_reject_any_further_transition() {
        for (ApprovalStatus terminal : new ApprovalStatus[]{
                ApprovalStatus.APPROVED, ApprovalStatus.REJECTED, ApprovalStatus.EXPIRED}) {
            assertThat(terminal.isTerminal()).isTrue();
            for (ApprovalStatus target : ApprovalStatus.values()) {
                assertThat(terminal.canTransitionTo(target)).isFalse();
            }
        }
    }

    @Test
    void pending_is_not_terminal() {
        assertThat(ApprovalStatus.PENDING.isTerminal()).isFalse();
    }
}
