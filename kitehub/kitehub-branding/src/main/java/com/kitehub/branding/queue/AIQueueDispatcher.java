package com.kitehub.branding.queue;

import com.kitehub.branding.config.AIQueueConfig;
import com.kitehub.branding.config.AIQueueProperties;
import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.dto.AIJobPayload;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes AI jobs to the correct tier queue based on priority (GAP-005a).
 *
 * <p>This is dedicated dispatcher infrastructure (per design-patterns.md §3.5.1
 * Exception D) — its single purpose is the broker handoff. Callers MUST persist
 * their {@code BrandingJob} row in their own transaction before invoking
 * {@link #dispatch}. The class contains no business logic — only routing,
 * metrics, and send.</p>
 *
 * <p>When {@code ai.queue.fair-queue-enabled=true} (default), routes to the
 * 3-tier priority topology (see {@link AIQueueConfig}). When disabled, all
 * jobs fall back to the legacy {@link RabbitMQConfig#BRANDING_QUEUE} to keep
 * backward compatibility.</p>
 *
 * <p>Metrics published per dispatch:
 * <ul>
 *   <li>{@code ai.queue.dispatched} (counter, tag=tier) — job enqueued</li>
 *   <li>{@code ai.queue.dispatch.failed} (counter, tag=tier) — publish failure</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Component
public class AIQueueDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final AIQueueProperties properties;
    private final MeterRegistry meterRegistry;

    public AIQueueDispatcher(RabbitTemplate rabbitTemplate,
                             AIQueueProperties properties,
                             MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Publish the given payload to the tier queue matching {@code priority}.
     *
     * <p>Null priority defaults to {@link AIJobPriority#FREE} — fail-safe for
     * unknown tiers.</p>
     *
     * @param priority target tier (nullable — defaults to FREE)
     * @param payload  job payload
     */
    public void dispatch(AIJobPriority priority, AIJobPayload payload) {
        AIJobPriority resolved = priority == null ? AIJobPriority.FREE : priority;
        payload.setEnqueuedAt(Instant.now());

        String tierTag = resolved.name().toLowerCase();

        try {
            if (properties.isFairQueueEnabled()) {
                String routingKey = AIQueueConfig.routingKeyFor(resolved);
                rabbitTemplate.convertAndSend(AIQueueConfig.AI_EXCHANGE, routingKey, payload);
                log.debug("Dispatched job {} to tier queue {}",
                        payload.getJobId(), routingKey);
            } else {
                // Legacy path — single queue, no priority differentiation.
                rabbitTemplate.convertAndSend(RabbitMQConfig.BRANDING_EXCHANGE,
                        RabbitMQConfig.BRANDING_ROUTING_KEY, payload);
                log.debug("Fair-queue disabled — dispatched job {} to legacy queue",
                        payload.getJobId());
            }

            Counter.builder("ai.queue.dispatched")
                    .tag("tier", tierTag)
                    .tag("mode", properties.isFairQueueEnabled() ? "fair" : "legacy")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ex) {
            log.error("Failed to dispatch AI job {} to tier {}: {}",
                    payload.getJobId(), tierTag, ex.getMessage(), ex);
            Counter.builder("ai.queue.dispatch.failed")
                    .tag("tier", tierTag)
                    .register(meterRegistry)
                    .increment();
            throw ex;
        }
    }
}
