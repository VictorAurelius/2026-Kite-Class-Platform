package com.kitehub.admin.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CrossServiceCacheInvalidationListener}.
 *
 * <p>Pure unit tests — no broker, no Spring context. The Spring AMQP listener
 * machinery is exercised by the framework; here we verify the adapter logic
 * (parse payload → republish as in-process Spring event so existing
 * {@link AdminCacheInvalidationListener} evicts caches).</p>
 */
@ExtendWith(MockitoExtension.class)
class CrossServiceCacheInvalidationListenerTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private CrossServiceCacheInvalidationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CrossServiceCacheInvalidationListener(applicationEventPublisher);
    }

    @Test
    void onSubscriptionEvent_shouldPublishInProcessEventWithRoutingKeyAndAggregateId() {
        UUID aggregateId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "subscriptionId", aggregateId.toString(),
                "tier", "PREMIUM"
        );

        listener.onSubscriptionEvent(payload, "subscription.upgraded");

        ArgumentCaptor<SubscriptionDataChangedEvent> captor =
                ArgumentCaptor.forClass(SubscriptionDataChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        SubscriptionDataChangedEvent event = captor.getValue();
        assertThat(event.getChangeType()).isEqualTo("subscription.upgraded");
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
    }

    @Test
    void onInstanceEvent_shouldPublishInProcessEventWithInstanceId() {
        UUID instanceId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "instanceId", instanceId.toString(),
                "subdomain", "demo"
        );

        listener.onInstanceEvent(payload, "instance.suspended");

        ArgumentCaptor<SubscriptionDataChangedEvent> captor =
                ArgumentCaptor.forClass(SubscriptionDataChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        SubscriptionDataChangedEvent event = captor.getValue();
        assertThat(event.getChangeType()).isEqualTo("instance.suspended");
        assertThat(event.getAggregateId()).isEqualTo(instanceId);
    }

    @Test
    void onSubscriptionEvent_withoutAggregateId_shouldStillPublishWithNullId() {
        Map<String, Object> payload = Map.of("note", "bulk-import-no-id");

        listener.onSubscriptionEvent(payload, "subscription.bulk_import");

        ArgumentCaptor<SubscriptionDataChangedEvent> captor =
                ArgumentCaptor.forClass(SubscriptionDataChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        SubscriptionDataChangedEvent event = captor.getValue();
        assertThat(event.getChangeType()).isEqualTo("subscription.bulk_import");
        assertThat(event.getAggregateId()).isNull();
    }

    @Test
    void onSubscriptionEvent_withMalformedAggregateId_shouldPublishWithNullId() {
        Map<String, Object> payload = Map.of("subscriptionId", "not-a-uuid");

        listener.onSubscriptionEvent(payload, "subscription.created");

        ArgumentCaptor<SubscriptionDataChangedEvent> captor =
                ArgumentCaptor.forClass(SubscriptionDataChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        SubscriptionDataChangedEvent event = captor.getValue();
        assertThat(event.getChangeType()).isEqualTo("subscription.created");
        assertThat(event.getAggregateId()).isNull();
    }

    @Test
    void onSubscriptionEvent_withNullPayload_shouldStillPublish() {
        listener.onSubscriptionEvent(null, "subscription.cancelled");

        ArgumentCaptor<SubscriptionDataChangedEvent> captor =
                ArgumentCaptor.forClass(SubscriptionDataChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        SubscriptionDataChangedEvent event = captor.getValue();
        assertThat(event.getChangeType()).isEqualTo("subscription.cancelled");
        assertThat(event.getAggregateId()).isNull();
    }

    @Test
    void onSubscriptionEvent_withMissingRoutingKey_shouldNotPublish() {
        Map<String, Object> payload = Map.of("subscriptionId", UUID.randomUUID().toString());

        listener.onSubscriptionEvent(payload, null);

        verify(applicationEventPublisher, never()).publishEvent(any(SubscriptionDataChangedEvent.class));
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }
}
