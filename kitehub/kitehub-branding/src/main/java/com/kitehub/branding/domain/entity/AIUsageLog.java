package com.kitehub.branding.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tracks daily AI usage per instance for rate limiting.
 *
 * @since 1.0.0
 */
@Entity
@Table(
    name = "ai_usage_log",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ai_usage_per_day",
        columnNames = {"instance_id", "usage_date"}
    )
)
@Data
@NoArgsConstructor
public class AIUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "request_count", nullable = false)
    private int requestCount = 1;

    public AIUsageLog(UUID instanceId, LocalDate usageDate) {
        this.instanceId = instanceId;
        this.usageDate = usageDate;
        this.requestCount = 1;
    }
}
