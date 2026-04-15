package com.kiteclass.core.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled poller that drains outbox events to the broker.
 *
 * <p>Runs every {@code outbox.publisher.interval-ms} (default 5s). Each run:
 * <ol>
 *   <li>Fetch up to {@link #BATCH_SIZE} PENDING rows whose {@code next_attempt_at}
 *       has passed</li>
 *   <li>For each, call {@link EventDispatcher#dispatch}</li>
 *   <li>On success → mark PUBLISHED</li>
 *   <li>On {@link DispatchException} → record error, bump retry, schedule backoff
 *       (FAILED after {@link #MAX_RETRIES} attempts)</li>
 * </ol>
 *
 * <p>Each row is saved in its own transaction so one bad event doesn't block the batch.
 *
 * @since 3.17.0 (Wave 3 Sub-PR 3.1, ADR-007)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    public static final int BATCH_SIZE = 50;
    public static final int MAX_RETRIES = 10;
    public static final long BACKOFF_SECONDS = 5;

    private final OutboxEventRepository repository;
    private final EventDispatcher dispatcher;

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:5000}")
    public void drain() {
        List<OutboxEvent> batch = repository.findDispatchable(Instant.now(), PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) {
            return;
        }
        log.debug("[outbox] draining batch of {}", batch.size());
        for (OutboxEvent event : batch) {
            attempt(event);
        }
    }

    @Transactional
    protected void attempt(OutboxEvent event) {
        try {
            dispatcher.dispatch(event);
            event.markPublished();
        } catch (DispatchException e) {
            event.markFailureAndScheduleRetry(e.getMessage(), MAX_RETRIES, BACKOFF_SECONDS);
            log.warn("[outbox] dispatch failed id={} retry={}: {}",
                    event.getId(), event.getRetryCount(), e.getMessage());
        } catch (RuntimeException e) {
            event.markFailureAndScheduleRetry(e.getMessage(), MAX_RETRIES, BACKOFF_SECONDS);
            log.error("[outbox] unexpected dispatch failure id={}", event.getId(), e);
        }
        repository.save(event);
    }
}
