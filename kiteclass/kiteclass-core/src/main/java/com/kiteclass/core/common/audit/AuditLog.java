package com.kiteclass.core.common.audit;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.retention.Retention;
import com.kiteclass.core.module.retention.RetentionBucket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Append-only audit row — one per security-sensitive action.
 *
 * <p>Callers MUST go through {@link AuditLogWriter} so foundation rules (propagation,
 * truncation, serialization) stay in one place. Direct repository.save is discouraged.
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.0)
 */
@Entity
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_log_action_type", columnList = "action_type"),
                @Index(name = "idx_audit_log_aggregate",
                        columnList = "aggregate_type,aggregate_id"),
                @Index(name = "idx_audit_log_actor", columnList = "actor_user_id"),
                @Index(name = "idx_audit_log_instance_id", columnList = "instance_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Retention(value = RetentionBucket.RETAIN_WITH_PSEUDO,
        pseudonymizeFields = {"actor_user_id"})
public class AuditLog extends BaseEntity {

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "reason", length = 500)
    private String reason;
}
