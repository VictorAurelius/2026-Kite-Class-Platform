package com.kiteclass.core.module.moderation;

import org.junit.jupiter.api.Test;

import static com.kiteclass.core.module.moderation.ModerationStatus.APPROVED;
import static com.kiteclass.core.module.moderation.ModerationStatus.NEEDS_HUMAN_REVIEW;
import static com.kiteclass.core.module.moderation.ModerationStatus.PENDING;
import static com.kiteclass.core.module.moderation.ModerationStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

class ModerationStatusTest {

    @Test
    void pending_allows_all_three_outcomes() {
        assertThat(PENDING.canTransitionTo(APPROVED)).isTrue();
        assertThat(PENDING.canTransitionTo(REJECTED)).isTrue();
        assertThat(PENDING.canTransitionTo(NEEDS_HUMAN_REVIEW)).isTrue();
        assertThat(PENDING.canTransitionTo(PENDING)).isFalse();
    }

    @Test
    void needs_human_review_can_terminate_only() {
        assertThat(NEEDS_HUMAN_REVIEW.canTransitionTo(APPROVED)).isTrue();
        assertThat(NEEDS_HUMAN_REVIEW.canTransitionTo(REJECTED)).isTrue();
        assertThat(NEEDS_HUMAN_REVIEW.canTransitionTo(PENDING)).isFalse();
        assertThat(NEEDS_HUMAN_REVIEW.canTransitionTo(NEEDS_HUMAN_REVIEW)).isFalse();
    }

    @Test
    void approved_is_terminal() {
        assertThat(APPROVED.isTerminal()).isTrue();
        assertThat(APPROVED.canTransitionTo(REJECTED)).isFalse();
        assertThat(APPROVED.canTransitionTo(PENDING)).isFalse();
    }

    @Test
    void rejected_is_terminal() {
        assertThat(REJECTED.isTerminal()).isTrue();
        assertThat(REJECTED.canTransitionTo(APPROVED)).isFalse();
        assertThat(REJECTED.canTransitionTo(PENDING)).isFalse();
    }

    @Test
    void non_terminals_are_not_terminal() {
        assertThat(PENDING.isTerminal()).isFalse();
        assertThat(NEEDS_HUMAN_REVIEW.isTerminal()).isFalse();
    }
}
