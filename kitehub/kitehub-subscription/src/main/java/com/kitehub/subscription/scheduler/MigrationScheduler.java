package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.idempotency.MigrationIdempotencyKeyService;
import com.kitehub.subscription.service.TrialToPaidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Async worker that picks up instances in {@link com.kitehub.platform.domain.enums.MigrationPhase#PAYMENT_CAPTURED
 * PAYMENT_CAPTURED} and drives them through MIGRATING → COMPLETED (GAP-192 Phase 4b-i).
 *
 * <p>Moves the state-machine from the synchronous MVP of Phase 4a (where the
 * controller thread executed the migration) to the documented async model
 * ({@code rules.md §Workflow}). Also reaps expired idempotency-key rows.</p>
 *
 * <h3>Concurrency</h3>
 * <p>A single in-process {@link AtomicBoolean} lock prevents overlapping ticks on
 * the same JVM. Cluster-wide locking (when we run >1 replica) belongs to
 * a follow-up gap — for now the @Scheduled worker runs on the leader container
 * only.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MigrationScheduler {

    private final TrialToPaidService trialToPaidService;
    private final MigrationIdempotencyKeyService idempotencyService;

    /** Prevents two ticks from running concurrently within the same JVM. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Pick up PAYMENT_CAPTURED instances and run the migration worker on each.
     * Fixed-delay (not fixed-rate) so a long-running migration does not pile up ticks.
     */
    @Scheduled(fixedDelayString = "${kitehub.trial-to-paid.scheduler-fixed-delay-ms:5000}")
    public void tick() {
        if (!running.compareAndSet(false, true)) {
            log.debug("Migration scheduler tick skipped — previous tick still running");
            return;
        }
        try {
            List<Instance> ready = trialToPaidService.findInstancesReadyForMigration();
            if (ready.isEmpty()) {
                return;
            }
            log.info("Migration scheduler found {} instance(s) ready to migrate", ready.size());
            for (Instance instance : ready) {
                try {
                    trialToPaidService.executeMigrationWithRetry(instance.getId());
                } catch (RuntimeException ex) {
                    // Retry was exhausted inside executeMigrationWithRetry, which already
                    // marked MIGRATION_FAILED + emitted a DLQ event. Log + move on so
                    // the rest of the batch still processes.
                    log.error("Migration worker failed for instance {}", instance.getId(), ex);
                }
            }
        } finally {
            running.set(false);
        }
    }

    /**
     * Reap idempotency-key rows past their TTL. Scheduled separately so it runs
     * on the top of every minute and stays out of the migration hot path.
     */
    @Scheduled(cron = "0 * * * * *")
    public void purgeExpiredIdempotencyKeys() {
        try {
            idempotencyService.purgeExpired();
        } catch (RuntimeException ex) {
            log.error("Idempotency-key purge failed", ex);
        }
    }
}
