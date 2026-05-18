package com.kitehub.subscription.beta.scheduler;

import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * GAP-600 (Wave 92 Bucket C) — beta_access_request abort cleanup scheduler.
 *
 * <p>Sweeps stale PENDING beta_access_request rows (older than threshold) and
 * marks them ABORTED. Audit trail preserved — NOT deleted — per gap acceptance
 * criteria. Re-submit từ user cùng email được phép vì unique constraint only
 * on {@code invite_token}, not on email.</p>
 *
 * <h2>Why a scheduler instead of TTL trigger?</h2>
 * <ul>
 *   <li>Dev iteration friction: dev abort walkthrough mid-flow (Ctrl+C, browser
 *       close) leaves PENDING row → re-submit cùng email cũ trả 409. Sweep
 *       eliminates manual {@code docker compose down -v} (~10 min cost).</li>
 *   <li>Production hygiene: stale PENDING rows accumulate khi coordinator
 *       absent — cleanup keeps coordinator queue accurate.</li>
 * </ul>
 *
 * <h2>Configuration ({@code application.yml})</h2>
 * <pre>
 *   kitehub:
 *     beta:
 *       cleanup:
 *         stale-threshold-hours: 24    # PENDING older than this → ABORTED
 *         poll-cron: "0 0 *&#47;6 * * *"    # mỗi 6h
 *         enabled: true                # gate khi cần disable (vd test env)
 * </pre>
 *
 * <h2>Design pattern compliance</h2>
 * <p>Per {@code .claude/rules/design-patterns.md} §3.11 — audit/log/notification
 * services join parent transaction can poison caller. Ở đây scheduler là
 * top-level invoker (no parent txn), nên {@code @Transactional} default OK.
 * Side effect (mark ABORTED) atomic per batch via bulk UPDATE.</p>
 *
 * <h2>Observability (GAP-644)</h2>
 * <p>Khi drift được phát hiện ({@code staleCount != aborted}), scheduler emit
 * Micrometer counter {@code kitehub.scheduler.beta_request.abort.drift_count}
 * với tag dimensions {@code expected_count} + {@code actual_count}. Counter
 * được export sang CloudWatch qua existing Micrometer CloudWatch publisher.
 * CloudWatch Alarm threshold: {@code drift_count > 0} trong 3 consecutive
 * evaluations → SNS notification. Xem runbook:
 * {@code documents/05-guides/operations/scheduler-drift-runbook.md}.</p>
 *
 * @since Wave 92 — GAP-600 (scheduler), Wave 97 — GAP-644 (drift metric)
 */
@Slf4j
@Component
public class BetaRequestAbortCleanupScheduler {

    /** Metric name cho drift counter (GAP-644). */
    static final String METRIC_DRIFT_COUNT =
            "kitehub.scheduler.beta_request.abort.drift_count";

    private final BetaAccessRequestRepository repository;
    private final MeterRegistry meterRegistry;

    public BetaRequestAbortCleanupScheduler(BetaAccessRequestRepository repository,
                                             MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @Value("${kitehub.beta.cleanup.stale-threshold-hours:24}")
    private int staleThresholdHours;

    @Value("${kitehub.beta.cleanup.enabled:true}")
    private boolean enabled;

    /**
     * Cron-scheduled sweep of stale PENDING beta_access_request rows.
     *
     * <p>Default: every 6 hours (Spring cron syntax {@code 0 0 *&#47;6 * * *}).
     * Override via {@code kitehub.beta.cleanup.poll-cron} environment var.</p>
     *
     * <p>{@code @Transactional} so bulk UPDATE + count log share single txn —
     * count-before-flip lets us log expected vs actual delta if migration index
     * drift occurs.</p>
     */
    @Scheduled(cron = "${kitehub.beta.cleanup.poll-cron:0 0 */6 * * *}")
    @Transactional
    public void cleanupStalePendingRequests() {
        if (!enabled) {
            log.debug("BetaRequestAbortCleanupScheduler disabled via config — skipping sweep");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.minusHours(staleThresholdHours);

        long staleCount = repository.countStalePending(threshold);
        if (staleCount == 0) {
            log.debug("BetaRequestAbortCleanupScheduler: no stale PENDING rows older than {}h", staleThresholdHours);
            return;
        }

        int aborted = repository.markStaleAsAborted(threshold, now);

        log.info("BetaRequestAbortCleanupScheduler: swept {} stale PENDING beta_access_request rows "
                        + "(threshold={}h, cutoff={}) → ABORTED. countStale={}, actualAborted={}",
                aborted, staleThresholdHours, threshold, staleCount, aborted);

        if (staleCount != aborted) {
            log.warn("BetaRequestAbortCleanupScheduler: count drift detected — countStalePending={} "
                            + "but markStaleAsAborted updated {} rows. Possible race condition "
                            + "(concurrent admin approve/reject between count + update).",
                    staleCount, aborted);

            // GAP-644 — emit Micrometer CloudWatch drift metric.
            // Dimension tags allow CloudWatch Metric Math để cross-correlate với
            // admin approval throughput. Counter accumulates across scheduler runs;
            // CloudWatch Alarm threshold: drift_count > 0 trong 3 consecutive
            // evaluation periods → SNS alert. Xem runbook:
            // documents/05-guides/operations/scheduler-drift-runbook.md
            driftCounter(staleCount, aborted).increment();
        }
    }

    /**
     * Micrometer counter cho scheduler drift detection (GAP-644).
     *
     * <p>Lazy-build pattern (nhất quán với {@code InviteTokenService}) để tránh
     * eager registration trước khi MeterRegistry ready. Micrometer dedup-register
     * tự động nên multiple calls an toàn.</p>
     *
     * @param expectedCount {@code staleCount} từ {@code countStalePending()}
     * @param actualCount   {@code aborted} từ {@code markStaleAsAborted()}
     */
    private Counter driftCounter(long expectedCount, int actualCount) {
        return Counter.builder(METRIC_DRIFT_COUNT)
                .description("Số lần scheduler abort sweep phát hiện drift giữa "
                        + "countStalePending() và markStaleAsAborted() (GAP-644). "
                        + "Thường do concurrent admin approve/reject. "
                        + "Alarm khi > 0 trong 3 consecutive runs.")
                .tag("expected_count", String.valueOf(expectedCount))
                .tag("actual_count", String.valueOf(actualCount))
                .register(meterRegistry);
    }

    /**
     * Manual trigger for testing / admin recovery. Bypasses cron schedule but
     * respects {@code enabled} flag.
     */
    public int triggerManualCleanup() {
        log.info("Manual BetaRequestAbortCleanupScheduler invocation");
        if (!enabled) {
            log.warn("Manual trigger ignored — scheduler disabled via kitehub.beta.cleanup.enabled");
            return 0;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.minusHours(staleThresholdHours);
        return repository.markStaleAsAborted(threshold, now);
    }
}
