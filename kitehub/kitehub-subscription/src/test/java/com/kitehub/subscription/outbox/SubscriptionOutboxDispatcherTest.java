package com.kitehub.subscription.outbox;

import com.kitehub.subscription.config.EmailQueueConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionOutboxDispatcher — GAP-605 Phase 2 reliability net")
class SubscriptionOutboxDispatcherTest {

    @Mock
    private SubscriptionOutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MeterRegistry meterRegistry;
    private SubscriptionOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        dispatcher = new SubscriptionOutboxDispatcher(outboxRepository, rabbitTemplate, meterRegistry);
        ReflectionTestUtils.setField(dispatcher, "batchSize", 50);
        ReflectionTestUtils.setField(dispatcher, "backoffMinutes", 5L);
        dispatcher.registerMetrics();
    }

    private SubscriptionOutboxEvent pendingEvent(String topic, String eventType) {
        return SubscriptionOutboxEvent.builder()
            .id(UUID.randomUUID())
            .instanceId(UUID.randomUUID())
            .eventType(eventType)
            .topic(topic)
            .payload("{\"k\":\"v\"}")
            .createdAt(LocalDateTime.now().minusSeconds(30))
            .build();
    }

    @Test
    void dispatch_picks_undispatched_rows_publishes_to_rmq() {
        SubscriptionOutboxEvent event = pendingEvent("email.beta.invite", "beta.invite.sent");
        when(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc())
            .thenReturn(List.of(event));

        dispatcher.dispatch();

        verify(rabbitTemplate).convertAndSend(
            eq(EmailQueueConfig.EMAIL_EXCHANGE),
            eq("email.beta.invite"),
            (Object) eq("{\"k\":\"v\"}")
        );
    }

    @Test
    void dispatch_updates_dispatched_at_on_success() {
        SubscriptionOutboxEvent event = pendingEvent("email.test", "TEST");
        when(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc())
            .thenReturn(List.of(event));

        dispatcher.dispatch();

        ArgumentCaptor<SubscriptionOutboxEvent> captor = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getDispatchedAt()).isNotNull();
    }

    @Test
    void dispatch_handles_publish_failure_skips_save() {
        SubscriptionOutboxEvent event = pendingEvent("email.test", "TEST");
        when(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc())
            .thenReturn(List.of(event));
        doThrow(new AmqpException("broker down"))
            .when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

        dispatcher.dispatch();

        // Save NOT called because publish failed → dispatched_at stays NULL
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void dispatch_with_no_pending_rows_is_noop() {
        when(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc())
            .thenReturn(List.of());

        dispatcher.dispatch();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void dispatch_exposes_undispatched_count_metric() {
        when(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc())
            .thenReturn(List.of(
                pendingEvent("email.a", "A"),
                pendingEvent("email.b", "B"),
                pendingEvent("email.c", "C")
            ));

        dispatcher.dispatch();

        double undispatched = meterRegistry.get("outbox_undispatched_count").gauge().value();
        assertThat(undispatched).isEqualTo(3.0);
    }

    @Test
    void dispatch_skips_row_within_backoff_window() {
        SubscriptionOutboxEvent event = pendingEvent("email.test", "TEST");
        // First failure populates backoff map
        when(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc())
            .thenReturn(List.of(event));
        doThrow(new AmqpException("down"))
            .when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

        dispatcher.dispatch(); // first attempt fails
        dispatcher.dispatch(); // second attempt should skip due to backoff

        // RMQ convertAndSend called only once (second skipped by backoff)
        verify(rabbitTemplate, org.mockito.Mockito.times(1))
            .convertAndSend(anyString(), anyString(), (Object) any());
    }
}
