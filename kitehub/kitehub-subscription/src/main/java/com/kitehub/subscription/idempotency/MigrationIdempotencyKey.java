package com.kitehub.subscription.idempotency;

import com.kitehub.platform.domain.enums.MigrationPhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
 * Persisted idempotency-key record for {@code POST /api/platform/instances/{id}/upgrade}
 * (GAP-192 Phase 4b-i).
 *
 * <p>Per {@code api-contract.md}: "Idempotency: {@code idempotencyKey} persisted; duplicate
 * request within 10 minutes returns original 202 response." This entity captures the
 * response envelope so a replayed request returns identically without starting a second
 * migration.</p>
 *
 * <p>Uniqueness is keyed on {@code (idempotency_key, instance_id)} — clients may reuse
 * the same UUID across different instances, but never within one instance in the TTL
 * window. Expired rows are purged by {@code MigrationScheduler}.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "migration_idempotency_key",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_idempotency_key",
        columnNames = {"idempotency_key", "instance_id"}
    ),
    indexes = {
        @Index(name = "idx_idempotency_key_expires", columnList = "expires_at"),
        @Index(name = "idx_idempotency_key_lookup", columnList = "idempotency_key,instance_id")
    }
)
public class MigrationIdempotencyKey {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "idempotency_key", length = 64, nullable = false)
    private String idempotencyKey;

    @NotNull
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "response_phase", length = 32, nullable = false)
    private MigrationPhase responsePhase;

    @NotNull
    @Column(name = "response_started_at", nullable = false)
    private LocalDateTime responseStartedAt;

    @NotBlank
    @Size(max = 255)
    @Column(name = "response_poll_url", length = 255, nullable = false)
    private String responsePollUrl;

    @Column(name = "response_estimated_completion_seconds", nullable = false)
    private int responseEstimatedCompletionSeconds;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
