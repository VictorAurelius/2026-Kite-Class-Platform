package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.outbox.MigrationEventType;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.TrialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Retry-loop orchestrator extracted from {@code TrialToPaidService} (Sub-PR 6.2).
 *
 * <p>Owns the T2P-09 retry path: bounded attempts with backoff, phase reset between
 * attempts, and terminal-failure reporting via a callback the facade owns
 * (so the non-retry {@code executeMigration} path can share the same fail-marking
 * logic without circular dependency).</p>
 */
@Slf4j
@SuppressWarnings("deprecation")  // intentional delegation to TrialService legacy flip during Phase 4b-i transition
public class MigrationRetryRunner {

    private final InstanceRepository instanceRepository;
    private final TrialService trialService;
    private final TrialToPaidConfig config;
    private final MigrationStateMachine stateMachine;
    private final MigrationEventEmitter eventEmitter;
    private final BiConsumer<UUID, String> onTerminalFailure;

    public MigrationRetryRunner(InstanceRepository instanceRepository,
                                TrialService trialService,
                                TrialToPaidConfig config,
                                MigrationStateMachine stateMachine,
                                MigrationEventEmitter eventEmitter,
                                BiConsumer<UUID, String> onTerminalFailure) {
        this.instanceRepository = instanceRepository;
        this.trialService = trialService;
        this.config = config;
        this.stateMachine = stateMachine;
        this.eventEmitter = eventEmitter;
        this.onTerminalFailure = onTerminalFailure;
    }

    public void executeMigrationWithRetry(UUID instanceId) {
        int maxAttempts = Math.max(1, config.getRetryAttempts());
        List<Integer> backoff = config.getRetryBackoffSeconds();

        // Validate precondition once up front so illegal-phase callers don't enter
        // the retry loop at all (INVALID_PHASE_TRANSITION is not transient).
        Instance preview = loadInstance(instanceId);
        if (preview.getMigrationPhase() != MigrationPhase.PAYMENT_CAPTURED) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Cannot execute migration from phase " + preview.getMigrationPhase());
        }

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean isFinalAttempt = (attempt == maxAttempts);
                executeMigrationInternal(instanceId, isFinalAttempt);
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
                    resetToPaymentCapturedForRetry(instanceId);
                }
            }
        }
        log.error("Migration exhausted {} attempts for instance {}", maxAttempts, instanceId);
        onTerminalFailure.accept(instanceId, lastError == null ? "unknown" : lastError.getMessage());
        if (lastError instanceof MigrationException me) {
            throw me;
        }
        throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
            "Retry exhausted: " + (lastError == null ? "unknown" : lastError.getMessage()),
            lastError);
    }

    @Transactional
    protected void executeMigrationInternal(UUID instanceId, boolean terminalOnFailure) {
        log.info("Executing migration for instance {}", instanceId);
        Instance instance = loadInstance(instanceId);
        if (instance.getMigrationPhase() != MigrationPhase.PAYMENT_CAPTURED) {
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Cannot execute migration from phase " + instance.getMigrationPhase());
        }
        stateMachine.transitionPhase(instance, MigrationPhase.MIGRATING, LocalDateTime.now());
        instanceRepository.save(instance);
        try {
            trialService.convertTrialToSubscription(instanceId);
            Instance refreshed = loadInstance(instanceId);
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
            log.error("Migration attempt failed for instance {}", instanceId, ex);
            if (terminalOnFailure) {
                onTerminalFailure.accept(instanceId, ex.getMessage());
            }
            throw new MigrationException(MigrationException.Code.INVALID_PHASE_TRANSITION,
                "Migration failed: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    protected void resetToPaymentCapturedForRetry(UUID instanceId) {
        Instance instance = loadInstance(instanceId);
        if (instance.getMigrationPhase() == MigrationPhase.PAYMENT_CAPTURED) {
            return;
        }
        // Force-reset — bypass normal state-machine guard because we're inside the
        // retry control loop and the previous attempt was definitionally transient.
        instance.setMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
        instance.setMigrationFailureReason(null);
        instanceRepository.save(instance);
    }

    private Instance loadInstance(UUID instanceId) {
        return instanceRepository.findById(instanceId)
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
