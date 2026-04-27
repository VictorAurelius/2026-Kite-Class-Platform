package com.kitehub.admin.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * AMQP adapter that bridges cross-service Outbox events into the in-process
 * {@link SubscriptionDataChangedEvent} flow already wired up by
 * {@link AdminCacheInvalidationListener}.
 *
 * <p>Closes <strong>GAP-237</strong>. {@link AdminCacheInvalidationListener}
 * (GAP-126) handles same-JVM mutations via Spring {@code ApplicationEvent};
 * this listener subscribes to the cross-service {@code kitehub.events.exchange}
 * (TopicExchange, see {@link RabbitListenerConfig}) and republishes each
 * received message as the same in-process event so cache eviction logic stays
 * in one place.</p>
 *
 * <h3>Routing keys</h3>
 * <ul>
 *   <li>{@code subscription.*} — tier upgrades, renewals, cancellations</li>
 *   <li>{@code instance.*} — provisioning, suspend, activate, delete</li>
 * </ul>
 *
 * <h3>Producer side (currently deferred)</h3>
 * <p>The kitehub-subscription module persists rows in {@code subscription_outbox}
 * but the dispatcher that pushes those rows to {@code kitehub.events.exchange}
 * is deferred (memory: {@code project_outbox_per_module_pattern.md}). When that
 * dispatcher lands, this listener begins receiving events automatically — no
 * admin-side change required.</p>
 *
 * <h3>Resilience</h3>
 * <p>The listener swallows malformed payloads (logged at WARN) so a single bad
 * event cannot poison the queue. UUID parsing failures fall back to a {@code null}
 * aggregate id which still triggers full cache eviction (the cache eviction is
 * total, not per-aggregate, so the missing id is informational only).</p>
 *
 * @since GAP-237
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kitehub.admin.cross-service-cache-invalidation.enabled",
        havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class CrossServiceCacheInvalidationListener {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Receives subscription lifecycle events ({@code subscription.*}).
     *
     * <p>Bound by {@link RabbitListenerConfig} to
     * {@code kitehub.admin.subscription-events} queue, which is bound to
     * {@code kitehub.events.exchange} with routing key {@code subscription.*}.</p>
     */
    @RabbitListener(queues = RabbitListenerConfig.ADMIN_SUBSCRIPTION_EVENTS_QUEUE)
    public void onSubscriptionEvent(@Payload(required = false) Map<String, Object> payload,
                                    @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false)
                                    String routingKey) {
        handle(payload, routingKey, "subscriptionId");
    }

    /**
     * Receives instance lifecycle events ({@code instance.*}).
     *
     * <p>Bound by {@link RabbitListenerConfig} to
     * {@code kitehub.admin.instance-events} queue, which is bound to
     * {@code kitehub.events.exchange} with routing key {@code instance.*}.</p>
     */
    @RabbitListener(queues = RabbitListenerConfig.ADMIN_INSTANCE_EVENTS_QUEUE)
    public void onInstanceEvent(@Payload(required = false) Map<String, Object> payload,
                                @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false)
                                String routingKey) {
        handle(payload, routingKey, "instanceId");
    }

    private void handle(Map<String, Object> payload, String routingKey, String idField) {
        if (routingKey == null || routingKey.isBlank()) {
            log.warn("Dropping cross-service event without routing key (payload keys={})",
                    payload == null ? "null" : payload.keySet());
            return;
        }
        UUID aggregateId = extractUuid(payload, idField);
        log.debug("Cross-service cache invalidation triggered: routingKey={}, {}={}",
                routingKey, idField, aggregateId);
        applicationEventPublisher.publishEvent(
                new SubscriptionDataChangedEvent(this, routingKey, aggregateId));
    }

    private UUID extractUuid(Map<String, Object> payload, String fieldName) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ex) {
            log.warn("Cross-service event {} not a valid UUID: '{}'", fieldName, value);
            return null;
        }
    }
}
