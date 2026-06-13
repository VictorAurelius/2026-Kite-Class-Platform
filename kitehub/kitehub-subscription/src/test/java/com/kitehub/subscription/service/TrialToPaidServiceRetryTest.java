package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.idempotency.MigrationIdempotencyKeyService;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.migration.MigrationRetryRunner;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Retry + force-convert + webhook reversal coverage for
 * {@link TrialToPaidService} (GAP-192 Phase 4b-i).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrialToPaidService — retry + forceConvert + handlePaymentReversed")
@SuppressWarnings("deprecation")  // tests delegation to legacy TrialService.convertTrialToSubscription during Phase 4b-i transition
class TrialToPaidServiceRetryTest {

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
    private TrialToPaidService service;
    private MigrationRetryRunner retryRunner;

    private UUID instanceId;
    private Instance instance;

    @BeforeEach
    void setUp() {
        config = new TrialToPaidConfig();
        // Shrink backoff so tests don't sleep real seconds.
        config.setRetryAttempts(3);
        config.setRetryBackoffSeconds(List.of(0, 0, 0));
        SubscriptionEventEmitter eventEmitter = new SubscriptionEventEmitter(outboxRepository);
        InstanceTierSyncService tierSyncService = new InstanceTierSyncService();
        retryRunner = new MigrationRetryRunner(
            instanceRepository, trialService, config, eventEmitter, retryRunnerSelfProvider);
        // Self-reference returns the raw runner (no Spring proxy in a unit test). Lenient
        // because not every test enters the retry loop (e.g. precondition fail-fast).
        lenient().when(retryRunnerSelfProvider.getObject()).thenReturn(retryRunner);
        service = new TrialToPaidService(
            instanceRepository, eventEmitter, config, trialService, idempotencyService,
            tierSyncService, retryRunner);

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
        instance.setMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
    }

    @Nested
    @DisplayName("executeMigrationWithRetry()")
    class Retry {

        @Test
        @DisplayName("first-attempt success: no retries, migration COMPLETED")
        void happyFirstAttempt() {
            // preview load (findById) + per-attempt mutating loads (findByIdForUpdate)
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
            doAnswer(i -> { instance.setStatus(InstanceStatus.ACTIVE); return null; })
                .when(trialService).convertTrialToSubscription(eq(instanceId), any());

            service.executeMigrationWithRetry(instanceId);

            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.COMPLETED);
            verify(trialService, atLeast(1)).convertTrialToSubscription(eq(instanceId), any());
        }

        @Test
        @DisplayName("retry success mid-attempt: first 2 attempts fail, 3rd succeeds")
        void succeedsMidway() {
            AtomicInteger attempts = new AtomicInteger(0);
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));

            doAnswer(inv -> {
                int n = attempts.incrementAndGet();
                if (n < 3) {
                    throw new RuntimeException("transient failure #" + n);
                }
                instance.setStatus(InstanceStatus.ACTIVE);
                return null;
            }).when(trialService).convertTrialToSubscription(eq(instanceId), any());

            service.executeMigrationWithRetry(instanceId);

            assertThat(attempts.get()).isEqualTo(3);
            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.COMPLETED);
        }

        @Test
        @DisplayName("retry exhausted: throws MigrationException, phase = MIGRATION_FAILED")
        void retryExhausted() {
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
            doThrow(new RuntimeException("persistent failure"))
                .when(trialService).convertTrialToSubscription(eq(instanceId), any());

            assertThatThrownBy(() -> service.executeMigrationWithRetry(instanceId))
                .isInstanceOf(MigrationException.class);

            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.MIGRATION_FAILED);
        }

        @Test
        @DisplayName("no-retry on non-transient INVALID_PHASE_TRANSITION (wrong phase)")
        void noRetryOnInvalidPhase() {
            instance.setMigrationPhase(MigrationPhase.INITIATED); // can't execute from here
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

            assertThatThrownBy(() -> service.executeMigrationWithRetry(instanceId))
                .isInstanceOf(MigrationException.class)
                .extracting(e -> ((MigrationException) e).getCode())
                .isEqualTo(MigrationException.Code.INVALID_PHASE_TRANSITION);

            // trialService must never have been invoked
            verify(trialService, org.mockito.Mockito.never()).convertTrialToSubscription(any(), any());
        }
    }

    @Nested
    @DisplayName("forceConvert()")
    class ForceConvert {

        @Test
        @DisplayName("advances NONE → PAYMENT_CAPTURED with manual=true tag")
        void forceConvertHappyPath() {
            instance.setMigrationPhase(MigrationPhase.NONE);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
            when(idempotencyService.findExisting(any(), any())).thenReturn(Optional.empty());

            UpgradeRequest req = UpgradeRequest.builder()
                .tier(PricingTier.PREMIUM)
                .billingCycle("ANNUAL")
                .paymentMethodId("admin-force-convert:INV-1")
                .idempotencyKey("admin:INV-1")
                .build();

            UpgradeResponse resp = service.forceConvert(instanceId, req, "INV-1", "Bank verified");

            assertThat(resp.getMigrationPhase()).isEqualTo(MigrationPhase.PAYMENT_CAPTURED);
            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.PAYMENT_CAPTURED);
            // GAP-1095 — requested tier persisted at initiate (carried into force-convert).
            assertThat(instance.getTier()).isEqualTo(PricingTier.PREMIUM);
        }
    }

    @Nested
    @DisplayName("handlePaymentReversed()")
    class HandleReversed {

        @Test
        @DisplayName("runs rollback when within window")
        void reversedWithinWindow() {
            instance.setStatus(InstanceStatus.ACTIVE);
            instance.setMigrationPhase(MigrationPhase.COMPLETED);
            instance.setMigrationCompletedAt(LocalDateTime.now().minusHours(2));
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.handlePaymentReversed(instanceId, "chargeback");

            assertThat(result).isPresent();
            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.REVERSED);
            // GAP-1256 — tier reset to FREE on rollback.
            assertThat(instance.getTier()).isEqualTo(PricingTier.FREE);
        }

        @Test
        @DisplayName("idempotent when already REVERSED — no-op")
        void reversedIdempotent() {
            instance.setStatus(InstanceStatus.TRIAL);
            instance.setMigrationPhase(MigrationPhase.REVERSED);
            when(instanceRepository.findByIdForUpdate(instanceId)).thenReturn(Optional.of(instance));

            var result = service.handlePaymentReversed(instanceId, "duplicate webhook");

            assertThat(result).isEmpty();
            verify(instanceRepository, org.mockito.Mockito.never()).save(any());
        }
    }
}
