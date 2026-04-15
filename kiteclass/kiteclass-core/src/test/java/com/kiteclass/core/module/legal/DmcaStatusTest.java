package com.kiteclass.core.module.legal;

import com.kiteclass.core.module.legal.entity.DmcaStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DmcaStatusTest {

    @Test
    void pending_only_allows_reviewing() {
        assertThat(DmcaStatus.PENDING.canTransitionTo(DmcaStatus.REVIEWING)).isTrue();
        for (DmcaStatus other : DmcaStatus.values()) {
            if (other == DmcaStatus.REVIEWING) {
                continue;
            }
            assertThat(DmcaStatus.PENDING.canTransitionTo(other))
                    .as("PENDING should not allow -> %s", other)
                    .isFalse();
        }
    }

    @Test
    void reviewing_allows_valid_and_invalid_only() {
        assertThat(DmcaStatus.REVIEWING.canTransitionTo(DmcaStatus.VALID)).isTrue();
        assertThat(DmcaStatus.REVIEWING.canTransitionTo(DmcaStatus.INVALID)).isTrue();
        assertThat(DmcaStatus.REVIEWING.canTransitionTo(DmcaStatus.PENDING)).isFalse();
        assertThat(DmcaStatus.REVIEWING.canTransitionTo(DmcaStatus.EXECUTED)).isFalse();
        assertThat(DmcaStatus.REVIEWING.canTransitionTo(DmcaStatus.CONTESTED)).isFalse();
    }

    @Test
    void valid_allows_executed_and_contested_only() {
        assertThat(DmcaStatus.VALID.canTransitionTo(DmcaStatus.EXECUTED)).isTrue();
        assertThat(DmcaStatus.VALID.canTransitionTo(DmcaStatus.CONTESTED)).isTrue();
        assertThat(DmcaStatus.VALID.canTransitionTo(DmcaStatus.INVALID)).isFalse();
        assertThat(DmcaStatus.VALID.canTransitionTo(DmcaStatus.REVIEWING)).isFalse();
    }

    @Test
    void invalid_executed_contested_are_terminal() {
        for (DmcaStatus terminal : new DmcaStatus[]{
                DmcaStatus.INVALID, DmcaStatus.EXECUTED, DmcaStatus.CONTESTED}) {
            assertThat(terminal.isTerminal())
                    .as("%s should be terminal", terminal)
                    .isTrue();
            for (DmcaStatus target : DmcaStatus.values()) {
                assertThat(terminal.canTransitionTo(target))
                        .as("%s -> %s should be blocked", terminal, target)
                        .isFalse();
            }
        }
    }

    @Test
    void pending_reviewing_valid_are_not_terminal() {
        assertThat(DmcaStatus.PENDING.isTerminal()).isFalse();
        assertThat(DmcaStatus.REVIEWING.isTerminal()).isFalse();
        assertThat(DmcaStatus.VALID.isTerminal()).isFalse();
    }
}
