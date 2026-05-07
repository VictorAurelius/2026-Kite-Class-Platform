package com.kitehub.branding.lifecycle.entity;

import com.kitehub.branding.lifecycle.LifecycleState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Current lifecycle state for a branding instance.
 *
 * <p>One row per {@code instanceId}. Updated only via
 * {@link com.kitehub.branding.lifecycle.InstanceLifecycleService}.</p>
 *
 * @since Wave 34 (GAP-272l)
 */
@Entity
@Table(name = "branding_instance_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandingInstanceState {

    @Id
    @Column(name = "instance_id")
    private UUID instanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private LifecycleState state;

    @Column(name = "branding_version", nullable = false)
    private Integer brandingVersion;

    @Column(name = "regenerate_count", nullable = false)
    private Integer regenerateCount;

    @Column(name = "last_failure_reason", length = 1000)
    private String lastFailureReason;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
