package com.kiteclass.core.module.instance.entity;

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

/**
 * Frontend instance entity — represents a provisioned KiteClass tenant frontend.
 *
 * <p>State transitions delegated to {@link FrontendInstanceStatus} state machine.
 * All mutations go through {@code InstanceLifecycleService}, never set status directly
 * from controllers.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-INST-001: slug unique per tenant</li>
 *   <li>BR-INST-002: status transitions enforced by state machine (no direct sets)</li>
 *   <li>BR-INST-003: retry count capped at MAX_RETRIES (abandon after)</li>
 *   <li>BR-INST-004: brandingVersion increments on every successful deploy</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-009, ADR-004)
 */
@Entity
@Table(
        name = "frontend_instances",
        indexes = {
                @Index(name = "idx_frontend_instance_slug", columnList = "instance_id,slug", unique = true),
                @Index(name = "idx_frontend_instance_status", columnList = "status"),
                @Index(name = "idx_frontend_instance_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrontendInstance extends BaseEntity {

    /**
     * Human-readable tenant slug (cross-service ref to KiteHub {@code instances.slug}),
     * used for FE deploy lookup. Distinct from {@code instanceId} (UUID, the RLS
     * tenant-isolation filter inherited from {@link BaseEntity}). Renamed from
     * {@code tenantId} → {@code tenantSlug} per GAP-891 to remove naming confusion
     * between the two tenant-prefixed identifiers (DB column {@code tenant_slug}, V82).
     */
    @Column(name = "tenant_slug", nullable = false, length = 100)
    private String tenantSlug;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "frontend_url", length = 300)
    private String frontendUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FrontendInstanceStatus status = FrontendInstanceStatus.NOT_STARTED;

    @Column(name = "initializing_at")
    private Instant initializingAt;

    @Column(name = "generating_at")
    private Instant generatingAt;

    @Column(name = "deployed_at")
    private Instant deployedAt;

    @Column(name = "last_regenerate_at")
    private Instant lastRegenerateAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    /**
     * Timestamp tenant was SUSPENDED (subscription expired / payment failed). GAP-954.
     */
    @Column(name = "suspended_at")
    private Instant suspendedAt;

    /**
     * Timestamp tenant was soft-DELETED. Starts the 30-day PDPL Art 23 retention grace before the
     * cross-service hard purge runs in kitehub-subscription. GAP-954.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "branding_version", nullable = false)
    @Builder.Default
    private Integer brandingVersion = 0;

    /**
     * Transition to a new status, enforcing state machine rules.
     *
     * @throws IllegalStateException if transition not allowed from current status
     */
    public void transitionTo(FrontendInstanceStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid transition: " + status + " -> " + target
                            + " (allowed from " + status + ": " + status.allowedTransitions() + ")"
            );
        }
        FrontendInstanceStatus previous = this.status;
        this.status = target;
        Instant now = Instant.now();
        if (target == FrontendInstanceStatus.INITIALIZING) {
            this.initializingAt = now;
            this.failureReason = null;
        } else if (target == FrontendInstanceStatus.GENERATING) {
            this.generatingAt = now;
        } else if (target == FrontendInstanceStatus.DEPLOYED) {
            this.deployedAt = now;
            // Bump branding version only on a genuine (re)generation deploy — NOT on a
            // SUSPENDED → DEPLOYED reactivation, which restores the same branding (GAP-954).
            if (previous == FrontendInstanceStatus.GENERATING
                    || previous == FrontendInstanceStatus.REGENERATING) {
                this.brandingVersion = this.brandingVersion + 1;
            }
        } else if (target == FrontendInstanceStatus.REGENERATING) {
            this.lastRegenerateAt = now;
        } else if (target == FrontendInstanceStatus.FAILED) {
            this.failedAt = now;
            this.retryCount = this.retryCount + 1;
        } else if (target == FrontendInstanceStatus.SUSPENDED) {
            this.suspendedAt = now;
        } else if (target == FrontendInstanceStatus.DELETED) {
            this.deletedAt = now;
        }
    }
}
