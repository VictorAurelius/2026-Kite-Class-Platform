package com.kitehub.branding.queue;

import com.kitehub.branding.config.AIQueueConfig;
import com.kitehub.branding.config.AIQueueProperties;
import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.dto.AIJobPayload;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AIQueueDispatcher} — routing by tier + feature flag.
 */
@ExtendWith(MockitoExtension.class)
class AIQueueDispatcherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private AIQueueProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private AIQueueDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        properties = new AIQueueProperties();
        meterRegistry = new SimpleMeterRegistry();
        dispatcher = new AIQueueDispatcher(rabbitTemplate, properties, meterRegistry);
    }

    @Test
    void dispatch_freeTier_routesToFreeQueue() {
        AIJobPayload payload = AIJobPayload.builder()
                .jobId(UUID.randomUUID())
                .instanceId(UUID.randomUUID())
                .tier("FREE")
                .jobType("landing-content")
                .build();

        dispatcher.dispatch(AIJobPriority.FREE, payload);

        verify(rabbitTemplate).convertAndSend(
                eq(AIQueueConfig.AI_EXCHANGE),
                eq(AIQueueConfig.ROUTING_KEY_FREE),
                eq((Object) payload));

        assertThat(meterRegistry.counter("ai.queue.dispatched", "tier", "free", "mode", "fair").count())
                .isEqualTo(1.0);
    }

    @Test
    void dispatch_proTier_routesToProQueue() {
        AIJobPayload payload = newPayload();

        dispatcher.dispatch(AIJobPriority.PRO, payload);

        verify(rabbitTemplate).convertAndSend(
                eq(AIQueueConfig.AI_EXCHANGE),
                eq(AIQueueConfig.ROUTING_KEY_PRO),
                eq((Object) payload));
    }

    @Test
    void dispatch_enterpriseTier_routesToEnterpriseQueue() {
        AIJobPayload payload = newPayload();

        dispatcher.dispatch(AIJobPriority.ENTERPRISE, payload);

        verify(rabbitTemplate).convertAndSend(
                eq(AIQueueConfig.AI_EXCHANGE),
                eq(AIQueueConfig.ROUTING_KEY_ENTERPRISE),
                eq((Object) payload));
    }

    @Test
    void dispatch_nullPriority_defaultsToFreeQueue() {
        AIJobPayload payload = newPayload();

        dispatcher.dispatch(null, payload);

        verify(rabbitTemplate).convertAndSend(
                eq(AIQueueConfig.AI_EXCHANGE),
                eq(AIQueueConfig.ROUTING_KEY_FREE),
                eq((Object) payload));
    }

    @Test
    void dispatch_featureFlagOff_routesToLegacyQueue() {
        // Given — feature flag disabled
        properties.setFairQueueEnabled(false);
        AIJobPayload payload = newPayload();

        // When
        dispatcher.dispatch(AIJobPriority.ENTERPRISE, payload);

        // Then — everything goes to legacy queue regardless of tier
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.BRANDING_EXCHANGE),
                eq(RabbitMQConfig.BRANDING_ROUTING_KEY),
                eq((Object) payload));

        assertThat(meterRegistry.counter("ai.queue.dispatched", "tier", "enterprise", "mode", "legacy").count())
                .isEqualTo(1.0);
    }

    @Test
    void dispatch_setsEnqueuedAtTimestamp() {
        AIJobPayload payload = newPayload();
        assertThat(payload.getEnqueuedAt()).isNull();

        dispatcher.dispatch(AIJobPriority.PRO, payload);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), captor.capture());
        AIJobPayload sent = (AIJobPayload) captor.getValue();
        assertThat(sent.getEnqueuedAt()).isNotNull();
    }

    private AIJobPayload newPayload() {
        return AIJobPayload.builder()
                .jobId(UUID.randomUUID())
                .instanceId(UUID.randomUUID())
                .jobType("test")
                .build();
    }
}
