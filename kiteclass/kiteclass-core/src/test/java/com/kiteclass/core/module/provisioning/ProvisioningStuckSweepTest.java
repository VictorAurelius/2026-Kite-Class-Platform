package com.kiteclass.core.module.provisioning;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProvisioningStuckSweepTest {

    private static final long THRESHOLD_MINUTES = 10;

    @Mock
    private FrontendInstanceRepository repository;

    @Mock
    private InstanceLifecycleService lifecycle;

    private MeterRegistry meterRegistry;
    private ProvisioningStuckSweep sweep;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        sweep = new ProvisioningStuckSweep(repository, lifecycle, meterRegistry, THRESHOLD_MINUTES);
    }

    private FrontendInstance instance(long id, FrontendInstanceStatus status, Instant enteredAt) {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-" + id).slug("slug-" + id)
                .status(status).retryCount(0).brandingVersion(0)
                .build();
        i.setId(id);
        if (status == FrontendInstanceStatus.INITIALIZING) {
            i.setInitializingAt(enteredAt);
        } else if (status == FrontendInstanceStatus.GENERATING) {
            i.setGeneratingAt(enteredAt);
        }
        return i;
    }

    private double counter(String result) {
        var c = meterRegistry.find(ProvisioningStuckSweep.METRIC_STUCK)
                .tag("result", result).counter();
        return c == null ? 0d : c.count();
    }

    private Instant minutesAgo(long m) {
        return Instant.now().minus(m, ChronoUnit.MINUTES);
    }

    @Test
    void marks_initializing_instance_stuck_beyond_threshold() {
        FrontendInstance stuck = instance(1L, FrontendInstanceStatus.INITIALIZING, minutesAgo(15));
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.INITIALIZING))
                .thenReturn(List.of(stuck));
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.GENERATING))
                .thenReturn(List.of());

        int swept = sweep.sweepStuckInstances();

        assertThat(swept).isEqualTo(1);
        verify(lifecycle).markFailed(eq(1L), contains("stuck in INITIALIZING"));
        assertThat(counter("swept")).isEqualTo(1d);
    }

    @Test
    void marks_generating_instance_stuck_beyond_threshold() {
        FrontendInstance stuck = instance(2L, FrontendInstanceStatus.GENERATING, minutesAgo(30));
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.INITIALIZING))
                .thenReturn(List.of());
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.GENERATING))
                .thenReturn(List.of(stuck));

        int swept = sweep.sweepStuckInstances();

        assertThat(swept).isEqualTo(1);
        verify(lifecycle).markFailed(eq(2L), contains("stuck in GENERATING"));
    }

    @Test
    void leaves_recent_instance_untouched() {
        FrontendInstance fresh = instance(3L, FrontendInstanceStatus.INITIALIZING, minutesAgo(2));
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.INITIALIZING))
                .thenReturn(List.of(fresh));
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.GENERATING))
                .thenReturn(List.of());

        int swept = sweep.sweepStuckInstances();

        assertThat(swept).isEqualTo(0);
        verify(lifecycle, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void skips_instance_with_null_entered_timestamp() {
        FrontendInstance anomalous = instance(4L, FrontendInstanceStatus.INITIALIZING, null);
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.INITIALIZING))
                .thenReturn(List.of(anomalous));
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.GENERATING))
                .thenReturn(List.of());

        int swept = sweep.sweepStuckInstances();

        assertThat(swept).isEqualTo(0);
        verify(lifecycle, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void markFailed_failure_increments_sweep_failed_counter_and_continues() {
        FrontendInstance stuck1 = instance(5L, FrontendInstanceStatus.INITIALIZING, minutesAgo(20));
        FrontendInstance stuck2 = instance(6L, FrontendInstanceStatus.INITIALIZING, minutesAgo(20));
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.INITIALIZING))
                .thenReturn(List.of(stuck1, stuck2));
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.GENERATING))
                .thenReturn(List.of());
        doThrow(new RuntimeException("db down")).when(lifecycle).markFailed(eq(5L), anyString());

        int swept = sweep.sweepStuckInstances();

        // stuck1 fails markFailed (not counted as swept) but loop continues to stuck2.
        assertThat(swept).isEqualTo(1);
        verify(lifecycle).markFailed(eq(5L), anyString());
        verify(lifecycle).markFailed(eq(6L), anyString());
        assertThat(counter("sweep_failed")).isEqualTo(1d);
        assertThat(counter("swept")).isEqualTo(1d);
    }

    @Test
    void scheduled_sweep_never_propagates_exceptions() {
        when(repository.findByStatusAndDeletedFalse(FrontendInstanceStatus.INITIALIZING))
                .thenThrow(new RuntimeException("repo exploded"));

        // Must not throw — cron entry point swallows + logs.
        sweep.scheduledSweep();
    }
}
