package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.outbox.MigrationEventType;
import com.kitehub.subscription.outbox.SubscriptionOutboxEvent;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.TrialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Isolated test for {@link MigrationRetryRunner} (Sub-PR 6.2 acceptance: 1+ test per
 * extracted class). Heavy retry-flow coverage stays in {@code TrialToPaidServiceRetryTest}
 * which exercises the runner via the facade — these tests verify direct invocation
 * + the terminal MIGRATION_FAILED marking (GAP-1254 — markMigrationFailed now owned here).
 *
 * <p>The runner is a Spring bean using an {@link ObjectProvider} self-reference to route its
 * {@code @Transactional} methods through the proxy; in this plain unit test the provider just
 * returns the runner itself (no proxy → transactions are no-ops, which is fine for logic
 * assertions).</p>
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
    @Mock
    private ObjectProvider<MigrationRetryRunner> selfProvider;

    private TrialToPaidConfig config;
    private SubscriptionEventEmitter eventEmitter;
    private MigrationRetryRunner runner;

    private UUID instanceId;
    private Instance instance;

    @BeforeEach
    void setUp() {
        config = new TrialToPaidConfig();
        config.setRetryAttempts(2);
        config.setRetryBackoffSeconds(List.of(0, 0));
        eventEmitter = new SubscriptionEventEmitter(outboxRepository);
        runner = new MigrationRetryRunner(instanceRepository, trialService, config,
            eventEmitter, selfProvider);
        // Self-reference returns the raw runner (no Spring proxy in a unit test). Lenient
        // because the precondition-fail-fast test never enters the loop.
        lenient().when(selfProvider.getObject()).thenReturn(runner);

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
    @DisplayName("precondition fails fast when phase != PAYMENT_CAPTURED — instance not marked failed")
    void preconditionFailFast() {
        instance.setMigrationPhase(MigrationPhase.INITIATED);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> runner.executeMigrationWithRetry(instanceId))
            .isInstanceOf(MigrationException.class)
            .extracting(e -> ((MigrationException) e).getCode())
            .isEqualTo(MigrationException.Code.INVALID_PHASE_TRANSITION);

        verify(trialService, never()).convertTrialToSubscription(any(), any());
        // No terminal marking — bailed out before the retry loop.
        assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.INITIATED);
        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("retry exhausted marks MIGRATION_FAILED + emits DLQ event")
    void terminalFailureMarksFailed() {
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("boom")).when(trialService).convertTrialToSubscription(any(), any());

        assertThatThrownBy(() -> runner.executeMigrationWithRetry(instanceId))
            .isInstanceOf(MigrationException.class);

        assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.MIGRATION_FAILED);

        ArgumentCaptor<SubscriptionOutboxEvent> cap = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
        verify(outboxRepository, atLeastOnce()).save(cap.capture());
        assertThat(cap.getAllValues())
            .extracting(SubscriptionOutboxEvent::getEventType)
            .contains(MigrationEventType.MIGRATION_FAILED);
    }
}
