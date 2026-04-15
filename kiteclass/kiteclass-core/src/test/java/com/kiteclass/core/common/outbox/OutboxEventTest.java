package com.kiteclass.core.common.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    private OutboxEvent pending() {
        return OutboxEvent.builder()
                .eventType("instance.deployed")
                .aggregateType("FrontendInstance")
                .aggregateId("1")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextAttemptAt(Instant.now())
                .build();
    }

    @Test
    void markPublished_transitions_and_sets_timestamp() {
        OutboxEvent e = pending();

        e.markPublished();

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(e.getPublishedAt()).isNotNull();
        assertThat(e.getLastError()).isNull();
    }

    @Test
    void failure_before_max_stays_pending_and_bumps_retry() {
        OutboxEvent e = pending();

        e.markFailureAndScheduleRetry("broker down", 3, 5);

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(e.getRetryCount()).isEqualTo(1);
        assertThat(e.getLastError()).isEqualTo("broker down");
        assertThat(e.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void failure_at_or_over_max_transitions_to_failed() {
        OutboxEvent e = pending();
        e.setRetryCount(2);

        e.markFailureAndScheduleRetry("broker down", 3, 5);

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(e.getRetryCount()).isEqualTo(3);
    }

    @Test
    void backoff_grows_exponentially() {
        OutboxEvent e1 = pending();
        e1.markFailureAndScheduleRetry("x", 10, 1);
        long delay1 = e1.getNextAttemptAt().getEpochSecond() - Instant.now().getEpochSecond();

        OutboxEvent e3 = pending();
        e3.setRetryCount(2);
        e3.markFailureAndScheduleRetry("x", 10, 1);
        long delay3 = e3.getNextAttemptAt().getEpochSecond() - Instant.now().getEpochSecond();

        assertThat(delay3).isGreaterThan(delay1);
    }
}
