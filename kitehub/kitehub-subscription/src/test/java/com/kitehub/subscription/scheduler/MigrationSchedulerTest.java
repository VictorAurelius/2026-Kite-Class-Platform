package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.idempotency.MigrationIdempotencyKeyService;
import com.kitehub.subscription.service.TrialToPaidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MigrationScheduler} (GAP-192 Phase 4b-i).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationScheduler")
class MigrationSchedulerTest {

    @Mock
    private TrialToPaidService trialToPaidService;

    @Mock
    private MigrationIdempotencyKeyService idempotencyService;

    private MigrationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MigrationScheduler(trialToPaidService, idempotencyService);
    }

    private Instance stubInstance(UUID id) {
        Instance i = new Instance();
        i.setId(id);
        i.setMigrationPhase(MigrationPhase.PAYMENT_CAPTURED);
        return i;
    }

    @Test
    @DisplayName("tick() picks up PAYMENT_CAPTURED instances and calls retry wrapper")
    void picksUpReadyInstances() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(trialToPaidService.findInstancesReadyForMigration())
            .thenReturn(List.of(stubInstance(a), stubInstance(b)));

        scheduler.tick();

        verify(trialToPaidService).executeMigrationWithRetry(a);
        verify(trialToPaidService).executeMigrationWithRetry(b);
    }

    @Test
    @DisplayName("tick() is a no-op when no instances are ready")
    void emptyQueue() {
        when(trialToPaidService.findInstancesReadyForMigration()).thenReturn(List.of());

        scheduler.tick();

        verify(trialToPaidService, never()).executeMigrationWithRetry(any());
    }

    @Test
    @DisplayName("tick() continues on per-instance failure — doesn't abort batch")
    void continuesAfterPerInstanceFailure() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(trialToPaidService.findInstancesReadyForMigration())
            .thenReturn(List.of(stubInstance(a), stubInstance(b)));
        doThrow(new MigrationException(
            MigrationException.Code.INVALID_PHASE_TRANSITION, "boom"))
            .when(trialToPaidService).executeMigrationWithRetry(a);

        scheduler.tick();

        // b must still be processed even though a threw
        verify(trialToPaidService).executeMigrationWithRetry(b);
    }

    @Test
    @DisplayName("purgeExpiredIdempotencyKeys() delegates to idempotency service")
    void purge() {
        when(idempotencyService.purgeExpired()).thenReturn(2);
        scheduler.purgeExpiredIdempotencyKeys();
        verify(idempotencyService).purgeExpired();
    }

    @Test
    @DisplayName("purgeExpiredIdempotencyKeys() swallows exceptions")
    void purgeSwallowsException() {
        when(idempotencyService.purgeExpired())
            .thenThrow(new RuntimeException("db down"));
        // should not throw
        scheduler.purgeExpiredIdempotencyKeys();
    }
}
