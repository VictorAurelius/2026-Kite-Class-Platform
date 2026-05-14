package com.kitehub.subscription.idempotency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Generic per-endpoint idempotency cache entry (GAP-536 Wave 77 Bucket D).
 *
 * <p>Backs the {@code idempotency_keys} table introduced by V41. A row is
 * inserted on first successful invocation of an idempotent endpoint and
 * replayed verbatim on duplicates received within the 24h TTL. Distinct from
 * {@code MigrationIdempotencyKey} which is scoped to trial-to-paid upgrade
 * (V20, 10-min TTL).</p>
 *
 * <p>Stripe semantics:
 * <ul>
 *   <li>Same key + same request hash → replay {@code response_status} +
 *       {@code response_body}</li>
 *   <li>Same key + different request hash → 422 idempotency conflict</li>
 *   <li>Key not seen before → handler runs, then this row is written</li>
 * </ul></p>
 *
 * @since Wave 77 — GAP-536
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @Column(name = "key", length = 128, nullable = false, updatable = false)
    private String key;

    @Column(name = "endpoint", length = 64, nullable = false, updatable = false)
    private String endpoint;

    @Column(name = "request_hash", length = 64, nullable = false, updatable = false)
    private String requestHash;

    @Column(name = "response_status", nullable = false, updatable = false)
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;
}
