package com.kiteclass.core.module.retention;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeletionRequestTest {

    @Test
    void transition_to_processing_stamps_timestamp() {
        DeletionRequest r = newPending();

        r.transitionTo(DeletionStatus.PROCESSING);

        assertThat(r.getStatus()).isEqualTo(DeletionStatus.PROCESSING);
        assertThat(r.getProcessingStartedAt()).isNotNull();
    }

    @Test
    void transition_to_completed_requires_processing_first() {
        DeletionRequest r = newPending();

        assertThatThrownBy(() -> r.transitionTo(DeletionStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING -> COMPLETED");
    }

    @Test
    void completed_stamps_completed_at() {
        DeletionRequest r = newPending();
        r.transitionTo(DeletionStatus.PROCESSING);

        r.transitionTo(DeletionStatus.COMPLETED);

        assertThat(r.getStatus()).isEqualTo(DeletionStatus.COMPLETED);
        assertThat(r.getCompletedAt()).isNotNull();
    }

    @Test
    void cancelled_stamps_cancelled_at() {
        DeletionRequest r = newPending();

        r.transitionTo(DeletionStatus.CANCELLED);

        assertThat(r.getStatus()).isEqualTo(DeletionStatus.CANCELLED);
        assertThat(r.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelled_is_terminal_rejects_further_transitions() {
        DeletionRequest r = newPending();
        r.transitionTo(DeletionStatus.CANCELLED);

        assertThatThrownBy(() -> r.transitionTo(DeletionStatus.PROCESSING))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pending_through_grace_to_processing_path_works() {
        DeletionRequest r = newPending();

        r.transitionTo(DeletionStatus.GRACE_PERIOD);
        assertThat(r.getStatus()).isEqualTo(DeletionStatus.GRACE_PERIOD);

        r.transitionTo(DeletionStatus.PROCESSING);
        assertThat(r.getStatus()).isEqualTo(DeletionStatus.PROCESSING);
        assertThat(r.getProcessingStartedAt()).isNotNull();
    }

    private DeletionRequest newPending() {
        return DeletionRequest.builder()
                .userId(42L)
                .tenantId(UUID.randomUUID())
                .status(DeletionStatus.PENDING)
                .build();
    }
}
