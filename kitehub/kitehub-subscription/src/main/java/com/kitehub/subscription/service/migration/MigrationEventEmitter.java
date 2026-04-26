package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.outbox.MigrationOutboxEvent;
import com.kitehub.subscription.outbox.MigrationOutboxRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox-event composer extracted from {@code TrialToPaidService} (Sub-PR 6.2).
 *
 * <p>All migration-domain events are written through this class so the JSON-escape rule and
 * outbox-row shape live in exactly one place. Per {@code .claude/rules/design-patterns.md}
 * §3.5, every cross-service event flows through outbox — this collaborator IS that contract
 * for the trial-to-paid flow.</p>
 */
@Slf4j
public class MigrationEventEmitter {

    private final MigrationOutboxRepository outboxRepository;

    public MigrationEventEmitter(MigrationOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    public void emit(Instance instance, String eventType, String topic, String payload) {
        MigrationOutboxEvent event = MigrationOutboxEvent.builder()
            .id(UUID.randomUUID())
            .instanceId(instance.getId())
            .eventType(eventType)
            .topic(topic)
            .payload(payload)
            .createdAt(LocalDateTime.now())
            .build();
        outboxRepository.save(event);
        log.debug("Outbox event queued: {} for instance {}", eventType, instance.getId());
    }

    /** JSON-escape helper for inline payload composition. */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
