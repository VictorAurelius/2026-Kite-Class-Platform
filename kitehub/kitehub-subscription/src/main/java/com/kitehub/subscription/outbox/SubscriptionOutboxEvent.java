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
 * Outbox record for kitehub-subscription cross-service events.
 *
 * <p>Originally introduced as {@code MigrationOutboxEvent} (GAP-192) for the
 * trial-to-paid flow; generalized in GAP-222c to cover purge + email events
 * after ADR-021 elected the per-MODULE domain outbox pattern. Written in the
 * same JPA transaction as the originating mutation so the event cannot be
 * lost if the broker is down.</p>
 *
 * <p>{@code instance_id} is nullable because some email flows (admin
 * notifications, sign-up confirmations sent before instance provisioning)
 * have no instance binding. Migration + purge events always populate it.</p>
 *
 * <p>Per {@code design-patterns.md §3.5.1 Exception A}, callers write this
 * row + best-effort fast-path publish in the same transactional block.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192) — generalized in GAP-222c
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscription_outbox", indexes = {
    @Index(name = "idx_subscription_outbox_instance", columnList = "instance_id")
})
public class SubscriptionOutboxEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "instance_id")
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
