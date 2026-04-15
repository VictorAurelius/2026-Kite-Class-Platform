package com.kiteclass.core.module.retention;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeletionStatusTest {

    @Test
    void pending_allows_transition_to_grace_processing_or_cancelled() {
        assertThat(DeletionStatus.PENDING.canTransitionTo(DeletionStatus.GRACE_PERIOD)).isTrue();
        assertThat(DeletionStatus.PENDING.canTransitionTo(DeletionStatus.PROCESSING)).isTrue();
        assertThat(DeletionStatus.PENDING.canTransitionTo(DeletionStatus.CANCELLED)).isTrue();
        assertThat(DeletionStatus.PENDING.canTransitionTo(DeletionStatus.COMPLETED)).isFalse();
        assertThat(DeletionStatus.PENDING.canTransitionTo(DeletionStatus.PENDING)).isFalse();
    }

    @Test
    void grace_period_allows_processing_or_cancelled_only() {
        assertThat(DeletionStatus.GRACE_PERIOD.canTransitionTo(DeletionStatus.PROCESSING)).isTrue();
        assertThat(DeletionStatus.GRACE_PERIOD.canTransitionTo(DeletionStatus.CANCELLED)).isTrue();
        assertThat(DeletionStatus.GRACE_PERIOD.canTransitionTo(DeletionStatus.PENDING)).isFalse();
        assertThat(DeletionStatus.GRACE_PERIOD.canTransitionTo(DeletionStatus.COMPLETED)).isFalse();
    }

    @Test
    void processing_only_transitions_to_completed() {
        assertThat(DeletionStatus.PROCESSING.canTransitionTo(DeletionStatus.COMPLETED)).isTrue();
        for (DeletionStatus other : new DeletionStatus[]{
                DeletionStatus.PENDING, DeletionStatus.GRACE_PERIOD,
                DeletionStatus.PROCESSING, DeletionStatus.CANCELLED}) {
            assertThat(DeletionStatus.PROCESSING.canTransitionTo(other)).isFalse();
        }
    }

    @Test
    void completed_and_cancelled_are_terminal() {
        for (DeletionStatus terminal :
                new DeletionStatus[]{DeletionStatus.COMPLETED, DeletionStatus.CANCELLED}) {
            assertThat(terminal.isTerminal()).isTrue();
            for (DeletionStatus target : DeletionStatus.values()) {
                assertThat(terminal.canTransitionTo(target)).isFalse();
            }
        }
    }

    @Test
    void non_terminal_states_report_false() {
        assertThat(DeletionStatus.PENDING.isTerminal()).isFalse();
        assertThat(DeletionStatus.GRACE_PERIOD.isTerminal()).isFalse();
        assertThat(DeletionStatus.PROCESSING.isTerminal()).isFalse();
    }
}
