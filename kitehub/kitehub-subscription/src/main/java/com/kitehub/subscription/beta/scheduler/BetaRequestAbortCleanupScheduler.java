package com.kitehub.subscription.beta.scheduler;

import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import lombok.RequiredArgsConstructor;
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
 * @since Wave 92 — GAP-600
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BetaRequestAbortCleanupScheduler {

    private final BetaAccessRequestRepository repository;

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
        }
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
