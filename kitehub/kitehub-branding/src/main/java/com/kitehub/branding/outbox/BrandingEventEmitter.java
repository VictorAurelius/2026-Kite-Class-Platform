package com.kitehub.branding.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Writes branding-domain events to the outbox + best-effort fast-path publish.
 *
 * <p>Per design-patterns.md §3.5.1 Exception A: outbox-first (reliability net),
 * then direct {@link RabbitTemplate#convertAndSend} (fast-path); broker errors
 * are swallowed so the outbox row stays the source of truth for eventual delivery.</p>
 *
 * <p>Mirrors the {@code SubscriptionEventEmitter} precedent in kitehub-subscription
 * (originally {@code MigrationEventEmitter}, generalized in GAP-222c)
 * + the {@code BrandingEventPublisher} fast-path pattern in kiteclass-core.
 * Dispatcher (poll-and-publish loop) is deferred — events accumulate in the
 * {@code branding_outbox} table until the dispatcher gap lands.</p>
 *
 * @since Wave 7 (GAP-222a Phase 2)
 */
@Slf4j
@Component
public class BrandingEventEmitter {

    private final BrandingOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    public BrandingEventEmitter(BrandingOutboxRepository outboxRepository,
                                ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Persist an outbox row + best-effort direct publish.
     *
     * <p>Caller MUST be inside an existing {@code @Transactional} block so the
     * outbox row commits atomically with the domain change.</p>
     *
     * @param aggregateId the entity the event refers to (e.g. branding-job id)
     * @param instanceId  the tenant/instance the event belongs to (RLS scope —
     *                    branding_outbox.instance_id NOT NULL per V58)
     * @param eventType   short event name (e.g. {@code branding.job.queued})
     * @param exchange    RabbitMQ exchange for the fast-path publish
     * @param routingKey  RabbitMQ routing key
     * @param payloadObj  message body — serialized to JSON for outbox row;
     *                    same object handed to {@code rabbitTemplate}
     */
    public void emit(UUID aggregateId,
                     UUID instanceId,
                     String eventType,
                     String exchange,
                     String routingKey,
                     Object payloadObj) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadObj);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize {} payload: {}", eventType, ex.getMessage());
            return;
        }

        BrandingOutboxEvent event = BrandingOutboxEvent.builder()
            .id(UUID.randomUUID())
            .aggregateId(aggregateId)
            .instanceId(instanceId)
            .eventType(eventType)
            .topic(routingKey)
            .payload(payloadJson)
            .createdAt(LocalDateTime.now())
            .build();
        outboxRepository.save(event);

        if (rabbitTemplate != null) {
            try {
                // Best-effort fast-path; outbox is the reliability net.
                rabbitTemplate.convertAndSend(exchange, routingKey, payloadObj);
            } catch (Exception ex) {
                log.warn("Direct publish of {} failed — outbox will retry: {}",
                    eventType, ex.getMessage());
            }
        }
    }
}
