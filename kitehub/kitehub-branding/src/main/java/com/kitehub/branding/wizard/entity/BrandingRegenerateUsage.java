package com.kitehub.branding.wizard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks regenerate-quota consumption per user per daily window (Wave 34 sub-GAP-272d).
 *
 * <p>One row per (user_id, window_start) pair holds the running counter for that
 * day's regenerate consumption. Tier caps are not stored here — they live in
 * config keys {@code kitehub.regenerate.*} (FREE=3, PRO=10, PREMIUM=30,
 * ENTERPRISE=-1) per {@code .claude/rules/ai-branding-guidelines.md} §4.3.
 *
 * <p>Idempotency: POST /api/v1/branding/jobs/{jobId}/regenerate accepts
 * Idempotency-Key header; replays within the day return the same {@code jobId}
 * without re-consuming quota.
 *
 * @since 1.0
 */
@Entity
@Table(name = "branding_regenerate_usage")
@Data
public class BrandingRegenerateUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "instance_id")
    private UUID instanceId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(nullable = false, length = 20)
    private String tier;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(name = "last_regenerate_at")
    private LocalDateTime lastRegenerateAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
