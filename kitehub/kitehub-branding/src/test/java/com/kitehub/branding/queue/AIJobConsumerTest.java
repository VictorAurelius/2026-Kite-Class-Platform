package com.kitehub.branding.queue;

import com.kitehub.branding.config.AIQueueProperties;
import com.kitehub.branding.dto.AIJobPayload;
import com.kitehub.branding.service.DistributedRateLimiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AIJobConsumer} — concurrency gate, backpressure, metrics.
 */
@ExtendWith(MockitoExtension.class)
class AIJobConsumerTest {

    @Mock
    private DistributedRateLimiter rateLimiter;

    @Mock
    private BacklogInspector backlogInspector;

    private AIQueueProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private AIJobConsumer consumer;
    private AtomicInteger processedCount;

    @BeforeEach
    void setUp() {
        properties = new AIQueueProperties();
        meterRegistry = new SimpleMeterRegistry();
        processedCount = new AtomicInteger(0);

        // Test subclass so we can observe process() invocations.
        consumer = new AIJobConsumer(rateLimiter, properties, meterRegistry, backlogInspector) {
            @Override
            protected void process(AIJobPriority priority, AIJobPayload payload) {
                processedCount.incrementAndGet();
                super.process(priority, payload);
            }
        };
    }

    @Test
    void consumeEnterprise_happyPath_acquiresAndReleasesSlot() {
        UUID instanceId = UUID.randomUUID();
        when(rateLimiter.tryAcquireConcurrencySlot(eq(instanceId), anyInt())).thenReturn(true);

        AIJobPayload payload = AIJobPayload.builder()
                .jobId(UUID.randomUUID())
                .instanceId(instanceId)
                .tier("ENTERPRISE")
                .enqueuedAt(Instant.now().minusSeconds(1))
                .build();

        consumer.consumeEnterprise(payload);

        assertThat(processedCount).hasValue(1);
        verify(rateLimiter).tryAcquireConcurrencySlot(eq(instanceId), eq(properties.getConcurrency().getEnterprise()));
        verify(rateLimiter).releaseConcurrencySlot(instanceId);
        assertThat(meterRegistry.counter("ai.job.outcome", "tier", "enterprise", "outcome", "success").count())
                .isEqualTo(1.0);
    }

    @Test
    void consume_concurrencyCapReached_throwsAndIncrementsMetric() {
        UUID instanceId = UUID.randomUUID();
        when(rateLimiter.tryAcquireConcurrencySlot(eq(instanceId), anyInt())).thenReturn(false);

        AIJobPayload payload = AIJobPayload.builder()
                .jobId(UUID.randomUUID())
                .instanceId(instanceId)
                .tier("PRO")
                .enqueuedAt(Instant.now())
                .build();

        assertThatThrownBy(() -> consumer.consumePro(payload))
                .isInstanceOf(AIJobConsumer.ConcurrencyLimitedException.class);

        assertThat(processedCount).hasValue(0);
        assertThat(meterRegistry.counter("ai.job.outcome", "tier", "pro", "outcome", "concurrency_limited").count())
                .isEqualTo(1.0);
    }

    @Test
    void consumeFree_enterpriseBacklogOverThreshold_degradesWithoutProcessing() {
        // Given — enterprise backlog exceeds threshold (50)
        when(backlogInspector.enterpriseBacklog()).thenReturn(100L);

        AIJobPayload payload = AIJobPayload.builder()
                .jobId(UUID.randomUUID())
                .instanceId(UUID.randomUUID())
                .tier("FREE")
                .enqueuedAt(Instant.now())
                .build();

        // When
        consumer.consumeFree(payload);

        // Then — no processing, no slot acquisition, degraded counter +1
        assertThat(processedCount).hasValue(0);
        verify(rateLimiter, org.mockito.Mockito.never())
                .tryAcquireConcurrencySlot(org.mockito.ArgumentMatchers.any(), anyInt());
        assertThat(meterRegistry.counter("ai.job.outcome", "tier", "free", "outcome", "degraded").count())
                .isEqualTo(1.0);
    }

    @Test
    void consumeFree_enterpriseBacklogUnderThreshold_processesNormally() {
        // Given
        when(backlogInspector.enterpriseBacklog()).thenReturn(10L);
        UUID instanceId = UUID.randomUUID();
        when(rateLimiter.tryAcquireConcurrencySlot(eq(instanceId), anyInt())).thenReturn(true);

        AIJobPayload payload = AIJobPayload.builder()
                .jobId(UUID.randomUUID())
                .instanceId(instanceId)
                .tier("FREE")
                .enqueuedAt(Instant.now())
                .build();

        // When
        consumer.consumeFree(payload);

        // Then
        assertThat(processedCount).hasValue(1);
        verify(rateLimiter).releaseConcurrencySlot(instanceId);
    }

    @Test
    void consume_recordsWaitTimeTimer() {
        UUID instanceId = UUID.randomUUID();
        when(rateLimiter.tryAcquireConcurrencySlot(eq(instanceId), anyInt())).thenReturn(true);

        AIJobPayload payload = AIJobPayload.builder()
                .jobId(UUID.randomUUID())
                .instanceId(instanceId)
                .tier("PRO")
                .enqueuedAt(Instant.now().minusSeconds(2))
                .build();

        consumer.consumePro(payload);

        assertThat(meterRegistry.timer("ai.job.wait.time", "tier", "pro").count()).isEqualTo(1);
        assertThat(meterRegistry.timer("ai.job.duration", "tier", "pro").count()).isEqualTo(1);
    }
}
