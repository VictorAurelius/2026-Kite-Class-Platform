package com.kiteclass.core.module.moderation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationQueueTest {

    private ModerationQueue newPendingRow() {
        return ModerationQueue.builder()
                .targetType("branding.banner")
                .targetId("res-42")
                .status(ModerationStatus.PENDING)
                .score(0.9)
                .reason("banned-keyword: nsfw")
                .build();
    }

    @Test
    void transition_pending_to_rejected_sets_decided_at() {
        ModerationQueue row = newPendingRow();

        row.transitionTo(ModerationStatus.REJECTED);

        assertThat(row.getStatus()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(row.getDecidedAt()).isNotNull();
    }

    @Test
    void transition_pending_to_needs_human_review_leaves_decided_at_null() {
        ModerationQueue row = newPendingRow();

        row.transitionTo(ModerationStatus.NEEDS_HUMAN_REVIEW);

        assertThat(row.getStatus()).isEqualTo(ModerationStatus.NEEDS_HUMAN_REVIEW);
        assertThat(row.getDecidedAt()).isNull();
    }

    @Test
    void transition_needs_human_review_to_approved_sets_decided_at() {
        ModerationQueue row = newPendingRow();
        row.transitionTo(ModerationStatus.NEEDS_HUMAN_REVIEW);

        row.transitionTo(ModerationStatus.APPROVED);

        assertThat(row.getStatus()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(row.getDecidedAt()).isNotNull();
    }

    @Test
    void terminal_row_cannot_transition_again() {
        ModerationQueue row = newPendingRow();
        row.transitionTo(ModerationStatus.APPROVED);

        assertThatThrownBy(() -> row.transitionTo(ModerationStatus.REJECTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid moderation transition");
    }

    @Test
    void pending_to_pending_is_invalid() {
        ModerationQueue row = newPendingRow();

        assertThatThrownBy(() -> row.transitionTo(ModerationStatus.PENDING))
                .isInstanceOf(IllegalStateException.class);
    }
}
