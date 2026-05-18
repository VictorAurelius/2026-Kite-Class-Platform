package com.kitehub.subscription.beta.scheduler;

import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BetaRequestAbortCleanupScheduler} — GAP-600 Wave 92 Bucket C.
 *
 * <p>Verifies scheduler logic: enabled gate, no-op when zero stale rows, bulk
 * UPDATE invocation when stale rows exist, manual trigger. Repository contract
 * (actual SQL semantics + index usage) verified in PostgresIT separately.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BetaRequestAbortCleanupScheduler unit tests")
class BetaRequestAbortCleanupSchedulerTest {

    @Mock
    private BetaAccessRequestRepository repository;

    @InjectMocks
    private BetaRequestAbortCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Default config: 24h threshold, enabled
        ReflectionTestUtils.setField(scheduler, "staleThresholdHours", 24);
        ReflectionTestUtils.setField(scheduler, "enabled", true);
    }

    @Test
    @DisplayName("cleanup skipped when scheduler disabled via config")
    void cleanup_skipped_when_disabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.cleanupStalePendingRequests();

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("cleanup no-op when zero stale PENDING rows")
    void cleanup_no_op_when_zero_stale() {
        when(repository.countStalePending(any(OffsetDateTime.class))).thenReturn(0L);

        scheduler.cleanupStalePendingRequests();

        verify(repository, times(1)).countStalePending(any(OffsetDateTime.class));
        verify(repository, never()).markStaleAsAborted(any(), any());
    }

    @Test
    @DisplayName("cleanup invokes bulk UPDATE when stale rows exist")
    void cleanup_invokes_bulk_update_when_stale_rows_exist() {
        when(repository.countStalePending(any(OffsetDateTime.class))).thenReturn(3L);
        when(repository.markStaleAsAborted(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(3);

        scheduler.cleanupStalePendingRequests();

        verify(repository).countStalePending(any(OffsetDateTime.class));
        verify(repository).markStaleAsAborted(any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("threshold cutoff = now - staleThresholdHours")
    void threshold_cutoff_uses_configured_hours() {
        ReflectionTestUtils.setField(scheduler, "staleThresholdHours", 12);
        when(repository.countStalePending(any(OffsetDateTime.class))).thenReturn(1L);
        when(repository.markStaleAsAborted(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(1);

        OffsetDateTime before = OffsetDateTime.now();
        scheduler.cleanupStalePendingRequests();
        OffsetDateTime after = OffsetDateTime.now();

        // Verify threshold passed to repository is within (before - 12h, after - 12h) window
        verify(repository).countStalePending(org.mockito.ArgumentMatchers.argThat(threshold -> {
            return !threshold.isBefore(before.minusHours(12).minusSeconds(1))
                    && !threshold.isAfter(after.minusHours(12).plusSeconds(1));
        }));
    }

    @Test
    @DisplayName("manual trigger returns rows aborted when enabled")
    void manual_trigger_returns_aborted_count() {
        when(repository.markStaleAsAborted(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(5);

        int aborted = scheduler.triggerManualCleanup();

        assertThat(aborted).isEqualTo(5);
        verify(repository).markStaleAsAborted(any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("manual trigger returns 0 when disabled (no DB call)")
    void manual_trigger_returns_zero_when_disabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        int aborted = scheduler.triggerManualCleanup();

        assertThat(aborted).isZero();
        verifyNoInteractions(repository);
    }
}
