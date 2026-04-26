package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.TrialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Isolated test for {@link MigrationRetryRunner} (Sub-PR 6.2 acceptance: 1+ test per
 * extracted class). Heavy retry-flow coverage stays in {@code TrialToPaidServiceRetryTest}
 * which exercises the runner via the facade — these tests verify direct invocation
 * + the {@code onTerminalFailure} callback wiring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationRetryRunner")
@SuppressWarnings("deprecation")
class MigrationRetryRunnerTest {

    @Mock
    private InstanceRepository instanceRepository;
    @Mock
    private TrialService trialService;
    @Mock
    private SubscriptionOutboxRepository outboxRepository;

    private TrialToPaidConfig config;
    private MigrationStateMachine stateMachine;
    private SubscriptionEventEmitter eventEmitter;
    private AtomicReference<UUID> failedInstance;
    private AtomicReference<String> failedReason;
    private MigrationRetryRunner runner;

    private UUID instanceId;
    private Instance instance;

    @BeforeEach
    void setUp() {
        config = new TrialToPaidConfig();
        config.setRetryAttempts(2);
        config.setRetryBackoffSeconds(List.of(0, 0));
        stateMachine = new MigrationStateMachine(config);
        eventEmitter = new SubscriptionEventEmitter(outboxRepository);
        failedInstance = new AtomicReference<>();
        failedReason = new AtomicReference<>();
        runner = new MigrationRetryRunner(instanceRepository, trialService, config,
            stateMachine, eventEmitter,
            (id, reason) -> { failedInstance.set(id); failedReason.set(reason); });

        instanceId = UUID.randomUUID();
        instance = new Instance();
        instance.setId(instanceId);
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.BASIC);
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setTrialExpiresAt(LocalDateTime.now().plusDays(5));
        instance.setMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
    }

    @Test
    @DisplayName("precondition fails fast when phase != PAYMENT_CAPTURED — callback NOT fired")
    void preconditionFailFast() {
        instance.setMigrationPhase(MigrationPhase.INITIATED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> runner.executeMigrationWithRetry(instanceId))
            .isInstanceOf(MigrationException.class)
            .extracting(e -> ((MigrationException) e).getCode())
            .isEqualTo(MigrationException.Code.INVALID_PHASE_TRANSITION);

        verify(trialService, never()).convertTrialToSubscription(any());
        assertThat(failedInstance.get()).isNull();
    }

    @Test
    @DisplayName("retry exhausted fires onTerminalFailure callback with last error message")
    void terminalFailureCallbackFired() {
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("boom")).when(trialService).convertTrialToSubscription(instanceId);

        assertThatThrownBy(() -> runner.executeMigrationWithRetry(instanceId))
            .isInstanceOf(MigrationException.class);

        assertThat(failedInstance.get()).isEqualTo(instanceId);
        assertThat(failedReason.get()).contains("boom");
    }
}
