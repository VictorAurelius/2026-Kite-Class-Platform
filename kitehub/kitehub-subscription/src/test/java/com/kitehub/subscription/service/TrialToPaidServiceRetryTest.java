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
import com.kitehub.subscription.outbox.MigrationOutboxRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Retry + force-convert + webhook reversal coverage for
 * {@link TrialToPaidService} (GAP-192 Phase 4b-i).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrialToPaidService — retry + forceConvert + handlePaymentReversed")
class TrialToPaidServiceRetryTest {

    @Mock
    private InstanceRepository instanceRepository;
    @Mock
    private MigrationOutboxRepository outboxRepository;
    @Mock
    private TrialService trialService;
    @Mock
    private MigrationIdempotencyKeyService idempotencyService;

    private TrialToPaidConfig config;
    private TrialToPaidService service;

    private UUID instanceId;
    private Instance instance;

    @BeforeEach
    void setUp() {
        config = new TrialToPaidConfig();
        // Shrink backoff so tests don't sleep real seconds.
        config.setRetryAttempts(3);
        config.setRetryBackoffSeconds(List.of(0, 0, 0));
        service = new TrialToPaidService(
            instanceRepository, outboxRepository, config, trialService, idempotencyService);

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
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
            doAnswer(i -> { instance.setStatus(InstanceStatus.ACTIVE); return null; })
                .when(trialService).convertTrialToSubscription(instanceId);

            service.executeMigrationWithRetry(instanceId);

            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.COMPLETED);
            verify(trialService, atLeast(1)).convertTrialToSubscription(instanceId);
        }

        @Test
        @DisplayName("retry success mid-attempt: first 2 attempts fail, 3rd succeeds")
        void succeedsMidway() {
            AtomicInteger attempts = new AtomicInteger(0);
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));

            doAnswer(inv -> {
                int n = attempts.incrementAndGet();
                if (n < 3) {
                    throw new RuntimeException("transient failure #" + n);
                }
                instance.setStatus(InstanceStatus.ACTIVE);
                return null;
            }).when(trialService).convertTrialToSubscription(instanceId);

            service.executeMigrationWithRetry(instanceId);

            assertThat(attempts.get()).isEqualTo(3);
            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.COMPLETED);
        }

        @Test
        @DisplayName("retry exhausted: throws MigrationException, phase = MIGRATION_FAILED")
        void retryExhausted() {
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));
            doThrow(new RuntimeException("persistent failure"))
                .when(trialService).convertTrialToSubscription(instanceId);

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
            verify(trialService, org.mockito.Mockito.never()).convertTrialToSubscription(any());
        }
    }

    @Nested
    @DisplayName("forceConvert()")
    class ForceConvert {

        @Test
        @DisplayName("advances NONE → PAYMENT_CAPTURED with manual=true tag")
        void forceConvertHappyPath() {
            instance.setMigrationPhase(MigrationPhase.NONE);
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
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
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.handlePaymentReversed(instanceId, "chargeback");

            assertThat(result).isPresent();
            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.REVERSED);
        }

        @Test
        @DisplayName("idempotent when already REVERSED — no-op")
        void reversedIdempotent() {
            instance.setStatus(InstanceStatus.TRIAL);
            instance.setMigrationPhase(MigrationPhase.REVERSED);
            when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

            var result = service.handlePaymentReversed(instanceId, "duplicate webhook");

            assertThat(result).isEmpty();
            verify(instanceRepository, org.mockito.Mockito.never()).save(any());
        }
    }
}
