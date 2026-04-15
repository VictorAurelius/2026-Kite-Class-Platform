package com.kiteclass.core.module.legal;

import com.kiteclass.core.module.legal.entity.DmcaStatus;
import com.kiteclass.core.module.legal.entity.DmcaTakedownRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DmcaTakedownRequestTest {

    private DmcaTakedownRequest pending() {
        return DmcaTakedownRequest.builder()
                .reporterEmail("r@example.com")
                .reporterName("Reporter")
                .allegedInfringingUrl("https://example.com/x")
                .copyrightedWorkDescription("desc")
                .status(DmcaStatus.PENDING)
                .build();
    }

    @Test
    void transition_pending_to_reviewing_sets_reviewedAt() {
        DmcaTakedownRequest r = pending();
        r.transitionTo(DmcaStatus.REVIEWING);
        assertThat(r.getStatus()).isEqualTo(DmcaStatus.REVIEWING);
        assertThat(r.getReviewedAt()).isNotNull();
    }

    @Test
    void transition_reviewing_to_valid_sets_reviewedAt() {
        DmcaTakedownRequest r = pending();
        r.transitionTo(DmcaStatus.REVIEWING);
        r.transitionTo(DmcaStatus.VALID);
        assertThat(r.getStatus()).isEqualTo(DmcaStatus.VALID);
    }

    @Test
    void transition_valid_to_executed_sets_executedAt() {
        DmcaTakedownRequest r = pending();
        r.transitionTo(DmcaStatus.REVIEWING);
        r.transitionTo(DmcaStatus.VALID);
        r.transitionTo(DmcaStatus.EXECUTED);
        assertThat(r.getStatus()).isEqualTo(DmcaStatus.EXECUTED);
        assertThat(r.getExecutedAt()).isNotNull();
    }

    @Test
    void transition_valid_to_contested_sets_contestedAt() {
        DmcaTakedownRequest r = pending();
        r.transitionTo(DmcaStatus.REVIEWING);
        r.transitionTo(DmcaStatus.VALID);
        r.transitionTo(DmcaStatus.CONTESTED);
        assertThat(r.getStatus()).isEqualTo(DmcaStatus.CONTESTED);
        assertThat(r.getContestedAt()).isNotNull();
    }

    @Test
    void invalid_transition_throws() {
        DmcaTakedownRequest r = pending();
        assertThatThrownBy(() -> r.transitionTo(DmcaStatus.EXECUTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid DMCA takedown transition");
    }

    @Test
    void terminal_state_cannot_mutate() {
        DmcaTakedownRequest r = pending();
        r.transitionTo(DmcaStatus.REVIEWING);
        r.transitionTo(DmcaStatus.INVALID);
        assertThatThrownBy(() -> r.transitionTo(DmcaStatus.VALID))
                .isInstanceOf(IllegalStateException.class);
    }
}
