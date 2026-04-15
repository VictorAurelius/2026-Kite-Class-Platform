package com.kiteclass.core.module.retention;

import com.kiteclass.core.common.entity.BaseEntity;
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

import java.time.Instant;
import java.util.UUID;

/**
 * GDPR Art. 17 deletion request (ADR-013, GAP-073).
 *
 * <p>Lifecycle owned by {@link DeletionStatus}: PENDING → GRACE_PERIOD → PROCESSING →
 * COMPLETED (happy path) or PENDING/GRACE_PERIOD → CANCELLED (user reverses).
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-RET-001: 7-day grace window after request; reversible via cancel()</li>
 *   <li>BR-RET-002: State transitions enforced by {@link DeletionStatus} machine;
 *       attempts from terminal states throw {@link IllegalStateException}</li>
 *   <li>BR-RET-003: {@code dataExportUrl} populated when GDPR Art. 20 ZIP is ready</li>
 *   <li>BR-RET-004: Exactly one non-terminal DeletionRequest per (userId, tenantId)</li>
 * </ul>
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.4)
 */
@Entity
@Table(
        name = "deletion_requests",
        indexes = {
                @Index(name = "idx_deletion_request_user", columnList = "user_id"),
                @Index(name = "idx_deletion_request_tenant", columnList = "tenant_id"),
                @Index(name = "idx_deletion_request_status", columnList = "status"),
                @Index(name = "idx_deletion_request_grace_ends", columnList = "grace_ends_at"),
                @Index(name = "idx_deletion_request_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletionRequest extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private DeletionStatus status = DeletionStatus.PENDING;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "grace_starts_at")
    private Instant graceStartsAt;

    @Column(name = "grace_ends_at")
    private Instant graceEndsAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /** GDPR Art. 20 ZIP signed URL, populated when export is ready. Nullable. */
    @Column(name = "data_export_url", length = 1024)
    private String dataExportUrl;

    /**
     * Transition to a target state, enforcing the {@link DeletionStatus} machine.
     * Stamps the corresponding timestamp on success.
     *
     * @throws IllegalStateException when the target state is not reachable from current
     */
    public void transitionTo(DeletionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid deletion transition: " + status + " -> " + target);
        }
        this.status = target;
        Instant now = Instant.now();
        switch (target) {
            case PROCESSING -> this.processingStartedAt = now;
            case COMPLETED -> this.completedAt = now;
            case CANCELLED -> this.cancelledAt = now;
            default -> {
                // no timestamp for GRACE_PERIOD (graceStartsAt/graceEndsAt set at request time)
            }
        }
    }
}
