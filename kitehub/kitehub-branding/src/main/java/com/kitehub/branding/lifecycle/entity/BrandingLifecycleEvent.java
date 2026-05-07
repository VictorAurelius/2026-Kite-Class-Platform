package com.kitehub.branding.lifecycle.entity;

import com.kitehub.branding.lifecycle.LifecycleState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only history of every lifecycle transition + audit-relevant marker
 * (regenerate request, quality-score-computed, manual-override).
 *
 * <p>Doubles as the audit log for branding lifecycle operations
 * (option α + γ from the bucket spec — combined with the RabbitMQ
 * {@code branding.lifecycle.transition} event published by
 * {@link com.kitehub.branding.lifecycle.InstanceLifecycleService}).</p>
 *
 * @since Wave 34 (GAP-272l)
 */
@Entity
@Table(name = "branding_lifecycle_events", indexes = {
    @Index(name = "idx_branding_lifecycle_events_instance_ts",
        columnList = "instance_id, occurred_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandingLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 32)
    private LifecycleState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", length = 32)
    private LifecycleState toState;

    @Column(name = "actor_kind", nullable = false, length = 16)
    private String actorKind;

    @Column(name = "actor_id", length = 128)
    private String actorId;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
