package com.kitehub.subscription.outbox;

import com.kitehub.subscription.config.EmailQueueConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduled reliability-net dispatcher cho subscription_outbox per GAP-605
 * (Wave 91 Bucket A — closes outbox-pattern Phase 2).
 *
 * <p>Poll undispatched rows (`dispatched_at IS NULL`) every
 * {@code outbox.dispatcher.poll-interval-ms} (default 10s). Each row được
 * publish lại tới {@code email.exchange} qua topic của row; thành công →
 * UPDATE {@code dispatched_at = NOW()}, fail → log warn + giữ transient
 * in-memory backoff để tránh tight-loop khi RMQ down toàn cục.</p>
 *
 * <p>Companion với fast-path trong {@link com.kitehub.subscription.service.migration.SubscriptionEventEmitter}:
 * happy-path → fast-path delivers ngay; RMQ down → outbox row stays NULL →
 * dispatcher picks up khi broker recovers.</p>
 *
 * <p>Backoff decision (deferred per design): tracking last-attempt-time qua
 * in-memory {@link java.util.concurrent.ConcurrentHashMap} thay vì DB column —
 * tránh schema change Wave 91, transient state acceptable vì restart =
 * fresh attempts ngay khi container recover. Phase 2 follow-up có thể add
 * {@code last_attempt_at} column nếu cluster-mode dispatcher cần share state.</p>
 *
 * <p>Metrics exposed via Micrometer (Prometheus endpoint):
 * <ul>
 *   <li>{@code outbox_undispatched_count} — gauge số rows pending mỗi poll</li>
 *   <li>{@code outbox_dispatcher_lag_seconds} — gauge age của oldest pending row</li>
 *   <li>{@code outbox_dispatcher_published_total} — counter rows published OK</li>
 *   <li>{@code outbox_dispatcher_failed_total} — counter publish failures</li>
 * </ul></p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "outbox.dispatcher.enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionOutboxDispatcher {

    private final SubscriptionOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${outbox.dispatcher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.dispatcher.backoff-min-minutes:5}")
    private long backoffMinutes;

    /** Transient backoff map: row id → last attempt timestamp. Cleared trên restart. */
    private final ConcurrentHashMap<UUID, LocalDateTime> lastAttemptAt = new ConcurrentHashMap<>();

    private final AtomicLong undispatchedCount = new AtomicLong(0);
    private final AtomicLong dispatcherLagSeconds = new AtomicLong(0);

    public SubscriptionOutboxDispatcher(SubscriptionOutboxRepository outboxRepository,
                                        RabbitTemplate rabbitTemplate,
                                        MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerMetrics() {
        if (meterRegistry != null) {
            Gauge.builder("outbox_undispatched_count", undispatchedCount, AtomicLong::doubleValue)
                .description("Số subscription_outbox rows pending publish")
                .register(meterRegistry);
            Gauge.builder("outbox_dispatcher_lag_seconds", dispatcherLagSeconds, AtomicLong::doubleValue)
                .description("Age của oldest pending outbox row (seconds)")
                .register(meterRegistry);
        }
    }

    /**
     * Poll undispatched outbox rows, publish to RMQ, update dispatched_at on success.
     *
     * <p>{@code @Transactional} ensures row update atomic per iteration; publish call
     * happens BEFORE flush nên publish-fail không leave row marked dispatched falsely.
     * Per row try/catch — 1 bad row không block batch.</p>
     */
    @Scheduled(fixedDelayString = "${outbox.dispatcher.poll-interval-ms:10000}")
    @Transactional
    public void dispatch() {
        List<SubscriptionOutboxEvent> pending = outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            undispatchedCount.set(0);
            dispatcherLagSeconds.set(0);
            return;
        }

        undispatchedCount.set(pending.size());
        LocalDateTime oldest = pending.get(0).getCreatedAt();
        if (oldest != null) {
            dispatcherLagSeconds.set(Duration.between(oldest, LocalDateTime.now()).getSeconds());
        }

        int processed = 0;
        int skipped = 0;
        int failed = 0;

        for (SubscriptionOutboxEvent event : pending) {
            if (processed >= batchSize) {
                break;
            }

            // Backoff check — skip rows attempted within last N minutes
            LocalDateTime lastAttempt = lastAttemptAt.get(event.getId());
            if (lastAttempt != null
                && lastAttempt.isAfter(LocalDateTime.now().minusMinutes(backoffMinutes))) {
                skipped++;
                continue;
            }

            try {
                rabbitTemplate.convertAndSend(
                    EmailQueueConfig.EMAIL_EXCHANGE,
                    event.getTopic(),
                    event.getPayload()
                );
                event.setDispatchedAt(LocalDateTime.now());
                outboxRepository.save(event);
                lastAttemptAt.remove(event.getId());
                processed++;
                if (meterRegistry != null) {
                    meterRegistry.counter("outbox_dispatcher_published_total",
                        "event_type", event.getEventType()).increment();
                }
                log.debug("Outbox dispatched: id={} eventType={} topic={}",
                    event.getId(), event.getEventType(), event.getTopic());
            } catch (Exception ex) {
                lastAttemptAt.put(event.getId(), LocalDateTime.now());
                failed++;
                if (meterRegistry != null) {
                    meterRegistry.counter("outbox_dispatcher_failed_total",
                        "event_type", event.getEventType()).increment();
                }
                log.warn("Outbox publish failed: id={} eventType={} topic={} — will retry after {}min: {}",
                    event.getId(), event.getEventType(), event.getTopic(), backoffMinutes, ex.getMessage());
            }
        }

        if (processed > 0 || failed > 0) {
            log.info("Outbox dispatch cycle: pending={} processed={} skipped(backoff)={} failed={}",
                pending.size(), processed, skipped, failed);
        }
    }
}
