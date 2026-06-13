package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.dto.RollbackResponse;
import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.idempotency.MigrationIdempotencyKeyService;
import com.kitehub.subscription.outbox.MigrationEventType;
import com.kitehub.subscription.outbox.SubscriptionOutboxEvent;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import com.kitehub.subscription.service.migration.MigrationRetryRunner;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TrialToPaidService} (GAP-192 Phase 4a).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrialToPaidService")
@SuppressWarnings("deprecation")  // tests delegation to legacy TrialService.convertTrialToSubscription during Phase 4b-i transition
class TrialToPaidServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private SubscriptionOutboxRepository outboxRepository;

    @Mock
    private TrialService trialService;

    @Mock
    private MigrationIdempotencyKeyService idempotencyService;

    @Mock
    private ObjectProvider<MigrationRetryRunner> retryRunnerSelfProvider;

    private TrialToPaidConfig config;
    private SubscriptionEventEmitter eventEmitter;

    private TrialToPaidService service;

    private UUID instanceId;
    private Instance instance;

    @BeforeEach
    void setUp() {
        config = new TrialToPaidConfig();
        eventEmitter = new SubscriptionEventEmitter(outboxRepository);
        // Real tier-sync helper (no I/O — sets instance.tier) so GAP-1095/1256 tier behavior
        // is observable; retry runner is a real instance (executeMigrationWithRetry not
        // exercised here — only its markMigrationFailed via executeMigration's failure path).
        InstanceTierSyncService tierSyncService = new InstanceTierSyncService();
        MigrationRetryRunner retryRunner = new MigrationRetryRunner(
            instanceRepository, trialService, config, eventEmitter, retryRunnerSelfProvider);
        service = new TrialToPaidService(instanceRepository, eventEmitter, config, trialService,
            idempotencyService, tierSyncService, retryRunner);

        instanceId = UUID.randomUUID();
        instance = new Instance();
        instance.setId(instanceId);
        instance.setSubdomain("test-org");
        instance.setOrganizationName("Test Org");
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.BASIC);
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setTrialStartedAt(LocalDateTime.now().minusDays(2));
        instance.setTrialExpiresAt(LocalDateTime.now().plusDays(12));
        instance.setMigrationPhase(MigrationPhase.NONE);
    }

    private UpgradeRequest upgradeRequest() {
        return UpgradeRequest.builder()
            .tier(PricingTier.PREMIUM)
            .billingCycle("MONTHLY")
            .paymentMethodId("pm_test_1")
            .idempotencyKey(UUID.randomUUID().toString())
            .build();
    }

    @Nested
    @DisplayName("initiateUpgrade()")
    class InitiateUpgrade {

        @Test
        @DisplayName("moves phase from NONE through INITIATED to PAYMENT_PENDING")
        void happyPath() {
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));

            UpgradeResponse resp = service.initiateUpgrade(instanceId, upgradeRequest());

            assertThat(resp.getInstanceId()).isEqualTo(instanceId);
            assertThat(resp.getMigrationPhase()).isEqualTo(MigrationPhase.PAYMENT_PENDING);
            assertThat(resp.getPollUrl()).contains(instanceId.toString());
            assertThat(instance.getMigrationStartedAt()).isNotNull();
            // GAP-1095 — requested paid tier persisted at initiate so it's carried to completion.
            assertThat(instance.getTier()).isEqualTo(PricingTier.PREMIUM);
            // INITIATED event emitted
            ArgumentCaptor<SubscriptionOutboxEvent> cap = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
            verify(outboxRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues())
                .extracting(SubscriptionOutboxEvent::getEventType)
                .contains(MigrationEventType.TRIAL_UPGRADE_INITIATED);
        }

        @Test
        @DisplayName("rejects with MIGRATION_IN_FLIGHT when another phase is active")
        void rejectsInFlight() {
            instance.setMigrationPhase(MigrationPhase.MIGRATING);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.initiateUpgrade(instanceId, upgradeRequest()))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.MIGRATION_IN_FLIGHT);
        }

        @Test
        @DisplayName("rejects with MIGRATION_FAILED_LOCKED when phase is MIGRATION_FAILED")
        void rejectsFailedLocked() {
            instance.setMigrationPhase(MigrationPhase.MIGRATION_FAILED);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.initiateUpgrade(instanceId, upgradeRequest()))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.MIGRATION_FAILED_LOCKED);
        }

        @Test
        @DisplayName("rejects with RESCUE_WINDOW_EXPIRED beyond rescue window")
        void rejectsBeyondRescueWindow() {
            // Trial expired 48h ago — past 24h rescue window.
            instance.setTrialExpiresAt(LocalDateTime.now().minusHours(48));
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.initiateUpgrade(instanceId, upgradeRequest()))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.RESCUE_WINDOW_EXPIRED);
        }

        @Test
        @DisplayName("accepts upgrade inside rescue window even after expiry")
        void acceptsWithinRescueWindow() {
            // Trial expired 5h ago — still within 24h rescue window.
            instance.setTrialExpiresAt(LocalDateTime.now().minusHours(5));
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));

            UpgradeResponse resp = service.initiateUpgrade(instanceId, upgradeRequest());

            assertThat(resp.getMigrationPhase()).isEqualTo(MigrationPhase.PAYMENT_PENDING);
        }

        @Test
        @DisplayName("rejects when instance status is not TRIAL")
        void rejectsNonTrialStatus() {
            instance.setStatus(InstanceStatus.ACTIVE);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.initiateUpgrade(instanceId, upgradeRequest()))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.INVALID_PHASE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("handlePaymentCaptured()")
    class HandlePaymentCaptured {

        @Test
        @DisplayName("advances PAYMENT_PENDING → PAYMENT_CAPTURED and emits event")
        void happyPath() {
            instance.setMigrationPhase(MigrationPhase.PAYMENT_PENDING);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));

            service.handlePaymentCaptured(instanceId, "txn_1234");

            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.PAYMENT_CAPTURED);
            ArgumentCaptor<SubscriptionOutboxEvent> cap = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
            verify(outboxRepository).save(cap.capture());
            assertThat(cap.getValue().getEventType())
                .isEqualTo(MigrationEventType.PAYMENT_CAPTURED);
            assertThat(cap.getValue().getPayload()).contains("txn_1234");
        }

        @Test
        @DisplayName("is idempotent when called on already-captured phase")
        void idempotent() {
            instance.setMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            service.handlePaymentCaptured(instanceId, "txn_dup");

            // No mutation, no emit on duplicate
            verify(outboxRepository, never()).save(any());
            verify(instanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects when phase is not PAYMENT_PENDING (illegal transition)")
        void rejectsBadPhase() {
            instance.setMigrationPhase(MigrationPhase.NONE);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.handlePaymentCaptured(instanceId, "t"))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.INVALID_PHASE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("executeMigration()")
    class ExecuteMigration {

        @Test
        @DisplayName("happy path: PAYMENT_CAPTURED → MIGRATING → COMPLETED with events")
        void happyPath() {
            instance.setMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
            // Simulate the delegate performing the status flip.
            doAnswer(i -> {
                instance.setStatus(InstanceStatus.ACTIVE);
                return null;
            }).when(trialService).convertTrialToSubscription(eq(instanceId), any());

            service.executeMigration(instanceId);

            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.COMPLETED);
            assertThat(instance.getStatus()).isEqualTo(InstanceStatus.ACTIVE);
            assertThat(instance.getMigrationCompletedAt()).isNotNull();

            ArgumentCaptor<SubscriptionOutboxEvent> cap = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
            verify(outboxRepository, atLeast(2)).save(cap.capture());
            List<String> emitted = cap.getAllValues().stream()
                .map(SubscriptionOutboxEvent::getEventType)
                .toList();
            assertThat(emitted).contains(
                MigrationEventType.INSTANCE_MIGRATED,
                MigrationEventType.BRANDING_REFRESH_REQUIRED);
        }

        @Test
        @DisplayName("on exception: marks MIGRATION_FAILED + emits DLQ event + rethrows")
        void retryExhausted() {
            instance.setMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
            doThrow(new RuntimeException("DB down"))
                .when(trialService).convertTrialToSubscription(eq(instanceId), any());

            assertThatThrownBy(() -> service.executeMigration(instanceId))
                .isInstanceOf(MigrationException.class);

            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.MIGRATION_FAILED);
            assertThat(instance.getMigrationFailureReason()).contains("DB down");

            // DLQ event emitted
            ArgumentCaptor<SubscriptionOutboxEvent> cap = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
            verify(outboxRepository, atLeastOnce()).save(cap.capture());
            assertThat(cap.getAllValues())
                .extracting(SubscriptionOutboxEvent::getEventType)
                .contains(MigrationEventType.MIGRATION_FAILED);
        }

        @Test
        @DisplayName("rejects when phase is not PAYMENT_CAPTURED")
        void rejectsBadPhase() {
            instance.setMigrationPhase(MigrationPhase.INITIATED);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.executeMigration(instanceId))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.INVALID_PHASE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("rollback()")
    class Rollback {

        @Test
        @DisplayName("happy path: ACTIVE within window → TRIAL + REVERSED + 3 events")
        void happyPath() {
            instance.setStatus(InstanceStatus.ACTIVE);
            instance.setMigrationPhase(MigrationPhase.COMPLETED);
            instance.setMigrationCompletedAt(LocalDateTime.now().minusHours(2));
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));

            RollbackResponse resp = service.rollback(instanceId, "Gateway chargeback");

            assertThat(instance.getStatus()).isEqualTo(InstanceStatus.TRIAL);
            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.REVERSED);
            assertThat(resp.getNewStatus()).isEqualTo(InstanceStatus.TRIAL);
            assertThat(resp.getTrialExpiresAt()).isEqualTo(instance.getTrialExpiresAt());
            // GAP-1256 — denormalized tier reset to FREE on rollback (was BASIC in setUp).
            assertThat(instance.getTier()).isEqualTo(PricingTier.FREE);

            ArgumentCaptor<SubscriptionOutboxEvent> cap = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
            verify(outboxRepository, times(3)).save(cap.capture());
            assertThat(cap.getAllValues())
                .extracting(SubscriptionOutboxEvent::getEventType)
                .containsExactlyInAnyOrder(
                    MigrationEventType.PAYMENT_REVERSED,
                    MigrationEventType.MIGRATION_ROLLED_BACK,
                    MigrationEventType.BRANDING_REFRESH_REQUIRED);
        }

        @Test
        @DisplayName("rejects REVERSAL_WINDOW_EXPIRED beyond 24h")
        void windowExpired() {
            instance.setStatus(InstanceStatus.ACTIVE);
            instance.setMigrationPhase(MigrationPhase.COMPLETED);
            instance.setMigrationCompletedAt(LocalDateTime.now().minusHours(30));
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.rollback(instanceId, "too late"))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.REVERSAL_WINDOW_EXPIRED);
        }

        @Test
        @DisplayName("rejects when instance is not ACTIVE")
        void rejectsNonActive() {
            instance.setStatus(InstanceStatus.TRIAL);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.rollback(instanceId, "nope"))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.INVALID_PHASE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("MigrationPhase state machine")
    class StateMachineInvariants {

        @Test
        @DisplayName("NONE cannot transition directly to MIGRATING")
        void illegalJump() {
            assertThat(MigrationPhase.NONE.canTransitionTo(MigrationPhase.MIGRATING)).isFalse();
        }

        @Test
        @DisplayName("NONE → INITIATED → PAYMENT_PENDING → PAYMENT_CAPTURED → MIGRATING → COMPLETED is legal")
        void happyPath() {
            assertThat(MigrationPhase.NONE.canTransitionTo(MigrationPhase.INITIATED)).isTrue();
            assertThat(MigrationPhase.INITIATED.canTransitionTo(MigrationPhase.PAYMENT_PENDING)).isTrue();
            assertThat(MigrationPhase.PAYMENT_PENDING.canTransitionTo(MigrationPhase.PAYMENT_CAPTURED)).isTrue();
            assertThat(MigrationPhase.PAYMENT_CAPTURED.canTransitionTo(MigrationPhase.MIGRATING)).isTrue();
            assertThat(MigrationPhase.MIGRATING.canTransitionTo(MigrationPhase.COMPLETED)).isTrue();
            assertThat(MigrationPhase.COMPLETED.canTransitionTo(MigrationPhase.NONE)).isTrue();
        }

        @Test
        @DisplayName("MIGRATION_FAILED is terminal for automatic transitions")
        void terminal() {
            assertThat(MigrationPhase.MIGRATION_FAILED.isTerminal()).isTrue();
            assertThat(MigrationPhase.MIGRATION_FAILED.allowedTransitions()).isEmpty();
        }

        @Test
        @DisplayName("COMPLETED can transition to REVERSED (rollback) or NONE (reset)")
        void completedTransitions() {
            assertThat(MigrationPhase.COMPLETED.canTransitionTo(MigrationPhase.REVERSED)).isTrue();
            assertThat(MigrationPhase.COMPLETED.canTransitionTo(MigrationPhase.NONE)).isTrue();
            assertThat(MigrationPhase.COMPLETED.canTransitionTo(MigrationPhase.MIGRATING)).isFalse();
        }

        @Test
        @DisplayName("isInFlight true for all non-terminal phases except NONE/COMPLETED")
        void inFlightSemantics() {
            assertThat(MigrationPhase.NONE.isInFlight()).isFalse();
            assertThat(MigrationPhase.COMPLETED.isInFlight()).isFalse();
            assertThat(MigrationPhase.INITIATED.isInFlight()).isTrue();
            assertThat(MigrationPhase.PAYMENT_PENDING.isInFlight()).isTrue();
            assertThat(MigrationPhase.PAYMENT_CAPTURED.isInFlight()).isTrue();
            assertThat(MigrationPhase.MIGRATING.isInFlight()).isTrue();
            assertThat(MigrationPhase.REVERSED.isInFlight()).isTrue();
            assertThat(MigrationPhase.MIGRATION_FAILED.isInFlight()).isTrue();
        }
    }
}
