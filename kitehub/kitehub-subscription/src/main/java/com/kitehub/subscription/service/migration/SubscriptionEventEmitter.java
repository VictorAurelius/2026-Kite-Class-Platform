package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.outbox.SubscriptionOutboxEvent;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
 * transactional block.</p>
 */
@Slf4j
@Component
public class SubscriptionEventEmitter {

    private final SubscriptionOutboxRepository outboxRepository;

    public SubscriptionEventEmitter(SubscriptionOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
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
    }

    /** JSON-escape helper for inline payload composition. */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
