package com.kitehub.subscription.onboarding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Onboarding progress per tenant (Wave 78 GAP-538).
 *
 * <p>One row per tenant. Step state stored as JSONB string on the DB so
 * additions to {@code OnboardingStepId} enum stay backward-compatible without
 * a schema migration — the application layer reconciles enum vs row state on
 * read (missing steps inserted with {@code completed=false}, unknown steps
 * ignored).</p>
 *
 * <p>Lazy-init: BE auto-creates a default row on first GET per tenant; FE never
 * needs to POST a "create" call.</p>
 *
 * @since Wave 78 — GAP-538
 */
@Entity
@Table(
        name = "onboarding_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_onboarding_progress_tenant", columnNames = "tenant_id")
        },
        indexes = {
                @Index(name = "idx_onboarding_progress_tenant", columnList = "tenant_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    /** JSONB array of {stepId, completed, completedAt}. Reconciled with {@code OnboardingStepId} enum on read. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps_json", nullable = false, columnDefinition = "jsonb")
    private String stepsJson;

    @Column(name = "completion_percent", nullable = false)
    private int completionPercent;

    @Column(name = "last_updated_at", nullable = false)
    private OffsetDateTime lastUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.lastUpdatedAt == null) {
            this.lastUpdatedAt = now;
        }
        if (this.stepsJson == null) {
            this.stepsJson = "[]";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.lastUpdatedAt = OffsetDateTime.now();
    }
}
