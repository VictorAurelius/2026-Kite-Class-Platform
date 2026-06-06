package com.kitehub.branding.outbox;

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
 * Outbox record for branding-domain events (per ADR-021 — per-module domain outbox).
 *
 * <p>Written in the same JPA transaction as the {@code BrandingJob} mutation so the
 * event cannot be lost if the broker is down. The dispatcher (poll-and-publish loop)
 * is deferred to a follow-up gap; current consumers use Exception A pattern (direct
 * publish + outbox backup) per {@code design-patterns.md} §3.5.1.</p>
 *
 * <p>Schema: {@code V21__create_branding_outbox.sql} in kitehub-subscription
 * (which owns kitehub-schema migrations).</p>
 *
 * @since Wave 7 (GAP-222a Phase 2)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "branding_outbox", indexes = {
    @Index(name = "idx_branding_outbox_aggregate", columnList = "aggregate_id"),
    @Index(name = "idx_branding_outbox_dispatched", columnList = "dispatched_at")
})
public class BrandingOutboxEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    // V58 RLS sweep added branding_outbox.instance_id NOT NULL for tenant isolation;
    // entity/emitter were not updated → every insert failed the NOT NULL constraint.
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
