package com.kitehub.subscription.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox record for trial-to-paid migration domain events.
 *
 * <p>Written in the same JPA transaction as the {@code Instance} mutation so that
 * the event cannot be lost if the broker is down. A separate dispatcher (deferred
 * to Phase 4b, tracked via follow-up gap) publishes records where
 * {@code dispatched_at IS NULL} to RabbitMQ.</p>
 *
 * <p>See {@code rules.md §5} for the full schema of each event type and the
 * consumer list per topic.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "migration_outbox", indexes = {
    @Index(name = "idx_migration_outbox_instance", columnList = "instance_id")
})
public class MigrationOutboxEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @NotBlank
    @Size(max = 64)
    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @NotBlank
    @Size(max = 64)
    @Column(name = "topic", length = 64, nullable = false)
    private String topic;

    @NotBlank
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Null until the broker dispatcher picks up and confirms publish. */
    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;
}
