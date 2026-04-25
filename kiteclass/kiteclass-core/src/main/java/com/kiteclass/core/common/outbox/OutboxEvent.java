package com.kiteclass.core.common.outbox;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.retention.Retention;
import com.kiteclass.core.module.retention.RetentionBucket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Outbox event row (Transactional Outbox Pattern per ADR-007).
 *
 * <p>Services write a row inside their domain `@Transactional` block — same commit
 * as the aggregate change. {@code OutboxEventPublisher} later picks it up and
 * dispatches to the broker.
 *
 * <p>Lifecycle (see {@link OutboxStatus}): PENDING → PUBLISHED or PENDING → FAILED.
 *
 * @since 3.17.0 (Wave 3 Sub-PR 3.1, ADR-007)
 */
@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type,aggregate_id"),
                @Index(name = "idx_outbox_event_type", columnList = "event_type"),
                @Index(name = "idx_outbox_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Retention(RetentionBucket.PURGE_DELAYED)
public class OutboxEvent extends BaseEntity {

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)  // GAP-220: bind String → JSON (jsonb), not VARCHAR.
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "next_attempt_at", nullable = false)
    @Builder.Default
    private Instant nextAttemptAt = Instant.now();

    /**
     * Mark row as published (happy path).
     */
    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    /**
     * Record a dispatch failure and schedule the next retry.
     * After {@code maxRetries}, transitions to FAILED (terminal).
     */
    public void markFailureAndScheduleRetry(String errorMessage, int maxRetries, long backoffSeconds) {
        this.retryCount = this.retryCount + 1;
        this.lastError = errorMessage;
        if (this.retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
            return;
        }
        long factor = 1L << Math.min(this.retryCount, 6);
        this.nextAttemptAt = Instant.now().plusSeconds(backoffSeconds * factor);
    }
}
