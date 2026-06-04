package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.config.EmailQueueConfig;
import com.kitehub.subscription.outbox.SubscriptionOutboxEvent;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox-event composer for kitehub-subscription cross-service events.
 *
 * <p>Originally extracted from {@code TrialToPaidService} as {@code MigrationEventEmitter}
 * (Sub-PR 6.2 / GAP-192) and renamed in GAP-222c when the underlying outbox table
 * was generalized from migration-only to all subscription-module events. All
 * cross-service events flow through this class so the JSON-escape rule and
 * outbox-row shape live in exactly one place.</p>
 *
 * <p>Per {@code .claude/rules/design-patterns.md} §3.5 every cross-service event
 * flows through outbox; per §3.5.1 Exception A callers pair this write with a
 * best-effort {@code rabbitTemplate.convertAndSend} fast-path inside the same
 * transactional block — outbox is the reliability net.</p>
 *
 * <p>GAP-605 Wave 91 Bucket A: fast-path RMQ publish added directly in
 * {@link #emit(UUID, String, String, String)} so every cross-service event from
 * subscription module gets best-effort low-latency delivery. The
 * {@link com.kitehub.subscription.outbox.SubscriptionOutboxDispatcher} scheduled
 * job is the reliability net that catches any row not published successfully.</p>
 */
@Slf4j
@Component
public class SubscriptionEventEmitter {

    private final SubscriptionOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public SubscriptionEventEmitter(SubscriptionOutboxRepository outboxRepository,
                                    RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Constructor without RabbitTemplate — backward-compatible for unit tests
     * không cần RMQ wiring. Fast-path publish becomes no-op khi rabbitTemplate null.
     */
    public SubscriptionEventEmitter(SubscriptionOutboxRepository outboxRepository) {
        this(outboxRepository, null);
    }

    /**
     * Emit an event tied to an instance. Convenience overload for the common case
     * where an {@link Instance} is already in scope.
     */
    public void emit(Instance instance, String eventType, String topic, String payload) {
        emit(instance.getId(), eventType, topic, payload);
    }

    /**
     * Emit an event with an explicit (possibly null) instance id. Used for email
     * flows that may run before instance provisioning.
     *
     * <p>Two-step write per §3.5.1 Exception A:
     * 1. Persist outbox row (reliability net — dispatcher will retry if RMQ fails)
     * 2. Best-effort fast-path publish to RMQ — wrapped in try/catch nên publish
     *    failure không propagate cho caller; dispatcher poll sẽ catch up.</p>
     */
    public void emit(UUID instanceId, String eventType, String topic, String payload) {
        SubscriptionOutboxEvent event = SubscriptionOutboxEvent.builder()
            .id(UUID.randomUUID())
            .instanceId(instanceId)
            .eventType(eventType)
            .topic(topic)
            .payload(payload)
            .createdAt(LocalDateTime.now())
            .build();
        outboxRepository.save(event);
        log.debug("Outbox event queued: {} for instance {}", eventType, instanceId);

        // Best-effort fast-path — outbox is the reliability net.
        // Pattern lifted từ EmailServiceClient.publishToQueue — see design-patterns.md §3.5.1.
        //
        // GAP-925 (2026-06-04): `payload` arrives here as a pre-serialized JSON string
        // (`objectMapper.writeValueAsString(event)`). `convertAndSend(..., String)` runs
        // Jackson2JsonMessageConverter on the String → wraps it inside another JSON string,
        // so the consumer reads `"{...}"` (a quoted JSON-of-JSON) and Jackson rejects it
        // with `MismatchedInputException: Cannot construct EmailEvent ... from String`.
        // Build the AMQP Message manually with the raw UTF-8 bytes + Content-Type so the
        // converter passes through and consumers see the JSON object directly.
        if (rabbitTemplate != null) {
            try {
                MessageProperties props = new MessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setContentEncoding(StandardCharsets.UTF_8.name());
                Message msg = new Message(payload.getBytes(StandardCharsets.UTF_8), props);
                rabbitTemplate.send(EmailQueueConfig.EMAIL_EXCHANGE, topic, msg);
                log.debug("Fast-path publish OK: eventType={} topic={}", eventType, topic);
            } catch (Exception ex) {
                log.warn("Fast-path publish failed (eventType={} topic={}) — dispatcher will retry: {}",
                    eventType, topic, ex.getMessage());
            }
        }
    }

    /** JSON-escape helper for inline payload composition. */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
