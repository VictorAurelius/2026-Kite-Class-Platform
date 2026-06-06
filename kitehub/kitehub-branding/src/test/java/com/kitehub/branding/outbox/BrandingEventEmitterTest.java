package com.kitehub.branding.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BrandingEventEmitterTest {

    @Mock
    private BrandingOutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BrandingEventEmitter emitter;

    @BeforeEach
    void setUp() {
        emitter = new BrandingEventEmitter(outboxRepository, objectMapper);
        ReflectionTestUtils.setField(emitter, "rabbitTemplate", rabbitTemplate);
    }

    @Test
    void emit_persistsOutboxRowWithCorrectFields() {
        UUID aggregateId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        var payload = new TestPayload("hello");

        emitter.emit(aggregateId, instanceId, "branding.test.fired", "branding.exchange", "branding.test", payload);

        ArgumentCaptor<BrandingOutboxEvent> captor = ArgumentCaptor.forClass(BrandingOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        BrandingOutboxEvent saved = captor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateId()).isEqualTo(aggregateId);
        assertThat(saved.getInstanceId()).isEqualTo(instanceId);
        assertThat(saved.getEventType()).isEqualTo("branding.test.fired");
        assertThat(saved.getTopic()).isEqualTo("branding.test");
        assertThat(saved.getPayload()).contains("\"value\":\"hello\"");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getDispatchedAt()).isNull();
    }

    @Test
    void emit_alsoFiresFastPathPublishWhenRabbitTemplatePresent() {
        UUID aggregateId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        var payload = new TestPayload("hello");

        emitter.emit(aggregateId, instanceId, "branding.test.fired", "branding.exchange", "branding.test", payload);

        verify(rabbitTemplate).convertAndSend(eq("branding.exchange"), eq("branding.test"), eq(payload));
    }

    @Test
    void emit_swallowsBrokerErrorSoOutboxStaysSourceOfTruth() {
        UUID aggregateId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        var payload = new TestPayload("hello");
        doThrow(new AmqpException("broker down")).when(rabbitTemplate)
            .convertAndSend(any(String.class), any(String.class), any(Object.class));

        // Must NOT propagate — outbox row already saved acts as the reliability net
        emitter.emit(aggregateId, instanceId, "branding.test.fired", "branding.exchange", "branding.test", payload);

        verify(outboxRepository).save(any(BrandingOutboxEvent.class));
    }

    @Test
    void emit_skipsFastPathWhenRabbitTemplateNotWired() {
        UUID aggregateId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        var payload = new TestPayload("hello");
        ReflectionTestUtils.setField(emitter, "rabbitTemplate", null);

        emitter.emit(aggregateId, instanceId, "branding.test.fired", "branding.exchange", "branding.test", payload);

        verify(outboxRepository).save(any(BrandingOutboxEvent.class));
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    record TestPayload(String value) {}
}
