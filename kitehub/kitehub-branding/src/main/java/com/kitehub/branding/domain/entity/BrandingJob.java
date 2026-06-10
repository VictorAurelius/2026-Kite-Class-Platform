package com.kitehub.branding.domain.entity;

import com.kitehub.branding.domain.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Branding job entity for async AI branding generation.
 * <p>
 * Tracks progress of branding asset generation through RabbitMQ queue.
 *
 * @since 1.0
 */
@Entity
@Table(name = "branding_jobs")
@Data
public class BrandingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "organization_name", nullable = false, length = 200)
    private String organizationName;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    /**
     * Wizard user-type axis (GAP-1133): SOLO_TEACHER / SMALL_CENTER / LARGE_CENTER.
     * Stored as the enum string; nullable for backward-compat (pre-GAP-1133 jobs).
     * Migration: kitehub-subscription {@code V70__add_org_type_to_branding_jobs.sql}.
     */
    @Column(name = "org_type", length = 20)
    private String orgType;

    /**
     * Wizard tone selection (GAP-1146): {@code professional} / {@code friendly} /
     * {@code energetic} / {@code luxury}. Drives the deterministic preview palette
     * in {@link com.kitehub.branding.wizard.quality.BrandColoursDeriver} so the
     * brand colours reflect the chosen style (not just a hash of the org name).
     * Nullable for backward-compat (pre-GAP-1146 jobs).
     * Migration: kitehub-subscription {@code V71__add_tone_template_to_branding_jobs.sql}.
     */
    @Column(name = "tone", length = 20)
    private String tone;

    /**
     * Wizard template selection (GAP-1146): the {@code templateId} chosen at step 5.
     * Carried onto the job as a palette variant seed + future template-aware deploy.
     * Nullable for backward-compat.
     */
    @Column(name = "template_id", length = 50)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "assets_generated", columnDefinition = "TEXT")
    private String assetsGenerated; // JSON string of generated assets

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "queued_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime queuedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
