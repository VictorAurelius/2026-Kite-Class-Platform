package com.kiteclass.core.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wave beta-readiness-2 Bucket A — JPA entity mirroring the {@code idempotency_keys}
 * table introduced V66 (GAP-730).
 *
 * <p>This entity exists primarily so Hibernate's {@code create-drop} schema
 * generation creates the table in tests (where Flyway is disabled). Runtime
 * lookups + inserts go through {@link IdempotencyService} via JdbcTemplate
 * for performance + clarity, matching the Wave 105 Bucket D
 * {@code PaymentIdempotencyService} precedent.
 *
 * @since 3.1.0 (Wave beta-readiness-2 Bucket A)
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @EmbeddedId
    private IdempotencyRecordId id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
