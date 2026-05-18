package com.kitehub.subscription.beta.scheduler;

import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho {@link BetaRequestAbortCleanupScheduler} — GAP-600 Wave 92 Bucket C.
 *
 * <p>Verifies scheduler logic: enabled gate, no-op when zero stale rows, bulk
 * UPDATE invocation when stale rows exist, manual trigger, và (GAP-644 Wave 97)
 * Micrometer drift counter emit khi {@code staleCount != aborted}.</p>
 *
 * <p>Repository contract (actual SQL semantics + index usage) verified in
 * PostgresIT separately. MeterRegistry dùng {@link SimpleMeterRegistry} (in-process
 * không cần external sink) để verify counter increment trong unit test scope.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BetaRequestAbortCleanupScheduler unit tests")
class BetaRequestAbortCleanupSchedulerTest {

    @Mock
    private BetaAccessRequestRepository repository;

    /**
     * SimpleMeterRegistry: in-process, no external sink.
     * Cho phép verify counter increment + query counter value trong unit test.
     */
    private MeterRegistry meterRegistry;

    private BetaRequestAbortCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduler = new BetaRequestAbortCleanupScheduler(repository, meterRegistry);
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

    // ─── GAP-644: Drift metric tests ─────────────────────────────────────────

    @Test
    @DisplayName("GAP-644: drift counter NOT emitted when staleCount == aborted (no drift)")
    void driftCounter_not_emitted_when_no_drift() {
        // Given — staleCount == aborted → no drift
        when(repository.countStalePending(any(OffsetDateTime.class))).thenReturn(3L);
        when(repository.markStaleAsAborted(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(3);

        // When
        scheduler.cleanupStalePendingRequests();

        // Then — drift counter phải không tồn tại trong MeterRegistry (chưa register)
        Counter driftCounter = meterRegistry.find(BetaRequestAbortCleanupScheduler.METRIC_DRIFT_COUNT)
                .counter();
        assertThat(driftCounter)
                .as("drift counter KHÔNG được emit khi staleCount == aborted")
                .isNull();
    }

    @Test
    @DisplayName("GAP-644: drift counter emitted khi staleCount != aborted (drift scenario)")
    void driftCounter_emitted_when_drift_detected() {
        // Given — staleCount=5 nhưng chỉ abort được 3 (race: 2 rows concurrent approve/reject)
        when(repository.countStalePending(any(OffsetDateTime.class))).thenReturn(5L);
        when(repository.markStaleAsAborted(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(3);

        // When
        scheduler.cleanupStalePendingRequests();

        // Then — drift counter PHẢI được emit với giá trị = 1 (1 drift event)
        Counter driftCounter = meterRegistry.find(BetaRequestAbortCleanupScheduler.METRIC_DRIFT_COUNT)
                .tag("expected_count", "5")
                .tag("actual_count", "3")
                .counter();
        assertThat(driftCounter)
                .as("drift counter PHẢI được register khi staleCount != aborted")
                .isNotNull();
        assertThat(driftCounter.count())
                .as("drift counter phải increment 1 lần cho 1 drift event")
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("GAP-644: drift counter accumulates across multiple scheduler runs")
    void driftCounter_accumulates_across_multiple_runs() {
        // Given — 2 lần chạy scheduler đều detect drift
        when(repository.countStalePending(any(OffsetDateTime.class))).thenReturn(5L);
        when(repository.markStaleAsAborted(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(4);  // drift delta = 1 mỗi lần

        // When — 2 scheduler runs
        scheduler.cleanupStalePendingRequests();
        scheduler.cleanupStalePendingRequests();

        // Then — counter accumulate → total = 2.0
        Counter driftCounter = meterRegistry.find(BetaRequestAbortCleanupScheduler.METRIC_DRIFT_COUNT)
                .tag("expected_count", "5")
                .tag("actual_count", "4")
                .counter();
        assertThat(driftCounter)
                .as("drift counter PHẢI tồn tại sau 2 drift events")
                .isNotNull();
        assertThat(driftCounter.count())
                .as("drift counter phải tích lũy — 2 runs × 1 increment = 2.0")
                .isEqualTo(2.0);
    }
}
