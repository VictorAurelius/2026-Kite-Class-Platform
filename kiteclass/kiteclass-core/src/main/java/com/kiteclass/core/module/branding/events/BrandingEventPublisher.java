package com.kiteclass.core.module.branding.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link BrandingUpdatedEvent} through both the Outbox (for reliable
 * durable delivery) and directly to RabbitMQ (for low-latency cache eviction).
 *
 * <p>Design-patterns.md §3.5 requires events go through the Outbox — we do.
 * The direct publish is a best-effort fast-path; if the broker is unavailable
 * the outbox row still guarantees eventual delivery once the broker returns.
 * If {@link RabbitTemplate} isn't wired (tests, offline dev) we log-and-skip.
 *
 * @since Wave 4 (GAP-021)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrandingEventPublisher {

    private static final String AGGREGATE_TYPE = "Branding";

    private final OutboxEventWriter outbox;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /**
     * Emit {@code branding.updated}. Must be called from inside an existing
     * {@code @Transactional} block so the outbox row commits atomically with
     * the domain change.
     */
    public void publishUpdated(BrandingUpdatedEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize branding.updated event: {}", ex.getMessage());
            return;
        }

        outbox.enqueue(
                BrandingEventsConfig.ROUTING_KEY_UPDATED,
                AGGREGATE_TYPE,
                String.valueOf(event.instanceId()),
                payload);

        // Best-effort fast-path for cache eviction; outbox is the reliability net.
        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend(
                        BrandingEventsConfig.BRANDING_EVENTS_EXCHANGE,
                        BrandingEventsConfig.ROUTING_KEY_UPDATED,
                        event);
            } catch (Exception ex) {
                log.warn("Direct publish of branding.updated failed — outbox will retry: {}",
                        ex.getMessage());
            }
        }
    }
}
