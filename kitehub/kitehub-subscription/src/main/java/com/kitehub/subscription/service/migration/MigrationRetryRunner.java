package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.outbox.MigrationEventType;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.TrialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Retry-loop orchestrator extracted from {@code TrialToPaidService} (Sub-PR 6.2).
 *
 * <p>Owns the T2P-09 retry path: bounded attempts with backoff, phase reset between
 * attempts, and terminal-failure reporting (T2P-10).</p>
 *
 * <h3>Why a Spring bean + self-injection (GAP-1254)</h3>
 * <p>This class used to be {@code new}-instantiated by {@code TrialToPaidService} and its
 * per-attempt methods called via plain self-invocation — so the {@code @Transactional}
 * annotations on {@link #executeMigrationInternal} / {@link #resetToPaymentCapturedForRetry}
 * / {@link #markMigrationFailed} were INERT (no Spring proxy in the call path → no
 * transaction boundary, the per-attempt writes auto-committed individually). It is now a
 * {@link Component} and the retry loop invokes its own transactional methods through the
 * Spring proxy obtained via {@link #self()} (an {@link ObjectProvider} self-reference) so
 * each attempt runs in a real transaction and a failed attempt rolls back atomically.</p>
 */
@Slf4j
@Component
@SuppressWarnings("deprecation")  // intentional delegation to TrialService legacy flip during Phase 4b-i transition
public class MigrationRetryRunner {

    private final InstanceRepository instanceRepository;
    private final TrialService trialService;
    private final TrialToPaidConfig config;
    private final MigrationStateMachine stateMachine;
    private final SubscriptionEventEmitter eventEmitter;
    private final ObjectProvider<MigrationRetryRunner> selfProvider;

    public MigrationRetryRunner(InstanceRepository instanceRepository,
                                TrialService trialService,
                                TrialToPaidConfig config,
                                SubscriptionEventEmitter eventEmitter,
                                ObjectProvider<MigrationRetryRunner> selfProvider) {
        this.instanceRepository = instanceRepository;
        this.trialService = trialService;
        this.config = config;
        this.stateMachine = new MigrationStateMachine(config);
        this.eventEmitter = eventEmitter;
        this.selfProvider = selfProvider;
    }

    /**
     * The Spring-proxied reference to this bean. Calling {@code @Transactional} methods
     * through {@code self()} (instead of {@code this}) routes them via the proxy so the
     * transaction boundary actually applies (GAP-1254).
     */
    private MigrationRetryRunner self() {
        return selfProvider.getObject();
    }

    public void executeMigrationWithRetry(UUID instanceId) {
        int maxAttempts = Math.max(1, config.getRetryAttempts());
        List<Integer> backoff = config.getRetryBackoffSeconds();

        // Validate precondition once up front so illegal-phase callers don't enter
        // the retry loop at all (INVALID_PHASE_TRANSITION is not transient). Read-only
        // probe → plain findById (no pessimistic lock, no enclosing txn here).
        Instance preview = loadInstance(instanceId);
        if (preview.getMigrationPhase() != MigrationPhase.PAYMENT_CAPTURED) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Cannot execute migration from phase " + preview.getMigrationPhase());
        }

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                self().executeMigrationInternal(instanceId);
                log.info("Migration for {} succeeded on attempt {}", instanceId, attempt);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                if (attempt < maxAttempts) {
                    long delaySeconds = backoff != null && attempt - 1 < backoff.size()
                        ? backoff.get(attempt - 1) : 1L;
                    log.warn("Migration attempt {}/{} for {} failed — retrying in {}s: {}",
                        attempt, maxAttempts, instanceId, delaySeconds, ex.getMessage());
                    sleepSeconds(delaySeconds);
                    self().resetToPaymentCapturedForRetry(instanceId);
                }
            }
        }
        log.error("Migration exhausted {} attempts for instance {}", maxAttempts, instanceId);
        self().markMigrationFailed(instanceId, lastError == null ? "unknown" : lastError.getMessage());
        if (lastError instanceof MigrationException me) {
            throw me;
        }
        throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
            "Retry exhausted: " + (lastError == null ? "unknown" : lastError.getMessage()),
            lastError);
    }

    /**
     * One migration attempt — MIGRATING → COMPLETED (status flip + tier sync + events).
     * Must be {@code public} so the Spring proxy applies the {@code @Transactional}
     * boundary when invoked via {@link #self()} (GAP-1254). The instance is loaded with a
     * pessimistic write lock (GAP-1253, T2P-08) so concurrent workers can't both flip it.
     */
    @Transactional
    public void executeMigrationInternal(UUID instanceId) {
        log.info("Executing migration for instance {}", instanceId);
        Instance instance = loadInstanceForUpdate(instanceId);
        if (instance.getMigrationPhase() != MigrationPhase.PAYMENT_CAPTURED) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Cannot execute migration from phase " + instance.getMigrationPhase());
        }
        stateMachine.transitionPhase(instance, MigrationPhase.MIGRATING, LocalDateTime.now());
        instanceRepository.save(instance);
        try {
            // GAP-1095 — carry the requested paid tier (persisted on the instance at
            // initiateUpgrade time) into the status flip so instances.tier is synced via
            // the canonical SUB-21 sync point, not left on the FREE trial tier.
            trialService.convertTrialToSubscription(instanceId, instance.getTier());
            Instance refreshed = loadInstanceForUpdate(instanceId);
            LocalDateTime completedAt = LocalDateTime.now();
            stateMachine.transitionPhase(refreshed, MigrationPhase.COMPLETED, completedAt);
            refreshed.setMigrationCompletedAt(completedAt);
            eventEmitter.emit(refreshed, MigrationEventType.INSTANCE_MIGRATED,
                MigrationEventType.TOPIC_MIGRATION,
                String.format("{\"instanceId\":\"%s\",\"fromStatus\":\"TRIAL\",\"toStatus\":\"ACTIVE\",\"completedAt\":\"%s\"}",
                    refreshed.getId(), completedAt));
            eventEmitter.emit(refreshed, MigrationEventType.BRANDING_REFRESH_REQUIRED,
                MigrationEventType.TOPIC_BRANDING,
                String.format("{\"instanceId\":\"%s\",\"tier\":\"%s\"}",
                    refreshed.getId(), refreshed.getTier()));
            instanceRepository.save(refreshed);
            log.info("Migration COMPLETED for instance {}", instanceId);
        } catch (Exception ex) {
            // Terminal-failure marking is owned by the retry loop (executeMigrationWithRetry),
            // which decides when attempts are exhausted — re-throw so this attempt's txn rolls
            // back cleanly and the loop can retry or finalize.
            log.error("Migration attempt failed for instance {}", instanceId, ex);
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Migration failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Reset a failed attempt back to PAYMENT_CAPTURED so the next loop iteration can retry.
     * Must be {@code public} for the proxy (GAP-1254). FM-5: if a prior attempt actually
     * flipped the instance to ACTIVE (conversion succeeded but a later step threw), do NOT
     * reset/re-convert — that would double-convert an already-migrated instance.
     */
    @Transactional
    public void resetToPaymentCapturedForRetry(UUID instanceId) {
        Instance instance = loadInstanceForUpdate(instanceId);
        // FM-5 guard — conversion already succeeded in a prior attempt; the failure was
        // downstream. Leave the instance ACTIVE; the loop will surface the error without
        // re-running the (non-idempotent) trial→paid flip.
        if (instance.getStatus() == InstanceStatus.ACTIVE) {
            log.warn("Skipping retry reset for instance {} — already ACTIVE (conversion succeeded; "
                + "not re-converting)", instanceId);
            return;
        }
        if (instance.getMigrationPhase() == MigrationPhase.PAYMENT_CAPTURED) {
            return;
        }
        // Force-reset — bypass normal state-machine guard because we're inside the
        // retry control loop and the previous attempt was definitionally transient.
        instance.setMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
        instance.setMigrationFailureReason(null);
        instanceRepository.save(instance);
    }

    /**
     * UC-T2P-03 / T2P-10 — dead-letter terminal marking. Runs in its OWN transaction
     * ({@link Propagation#REQUIRES_NEW}) so the MIGRATION_FAILED state + DLQ event persist
     * even though the failed migration attempt's transaction rolled back (audit-isolation
     * principle — a best-effort failure record must not be lost to the parent rollback).
     * Invoked via {@link #self()} so the proxy applies the new-transaction boundary.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markMigrationFailed(UUID instanceId, String reason) {
        Instance instance = loadInstanceForUpdate(instanceId);
        instance.setMigrationPhase(MigrationPhase.MIGRATION_FAILED);
        instance.setMigrationFailureReason(reason);
        // Keep status TRIAL — do NOT leave it ACTIVE if a prior attempt flipped it.
        if (instance.getStatus() == InstanceStatus.ACTIVE) {
            instance.setStatus(InstanceStatus.TRIAL);
        }
        eventEmitter.emit(instance, MigrationEventType.MIGRATION_FAILED,
            MigrationEventType.TOPIC_MIGRATION_DLQ,
            String.format("{\"instanceId\":\"%s\",\"failureReason\":\"%s\",\"attempts\":%d}",
                instance.getId(), SubscriptionEventEmitter.escape(reason),
                Math.max(1, config.getRetryAttempts())));
        instanceRepository.save(instance);
    }

    private Instance loadInstance(UUID instanceId) {
        return instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));
    }

    /** Pessimistic-write load for mutating paths (GAP-1253, T2P-08). */
    private Instance loadInstanceForUpdate(UUID instanceId) {
        return instanceRepository.findByIdForUpdate(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));
    }

    private static void sleepSeconds(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
