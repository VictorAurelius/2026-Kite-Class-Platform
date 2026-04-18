package com.kitehub.branding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Job payload enqueued onto tier-based AI queues (GAP-005a).
 *
 * <p>Carries the minimum data needed for a worker to pick up the job, apply
 * rate-limiting, call the AI provider, and publish results. Latency metrics
 * use {@link #enqueuedAt} to compute wait time.</p>
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIJobPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Unique job id for tracing, rate-limit, and result correlation. */
    private UUID jobId;

    /** Instance (tenant) id — used for per-tenant rate-limit + concurrency cap. */
    private UUID instanceId;

    /** Pricing tier string (ENTERPRISE / PREMIUM / BASIC / FREE / TRIAL). */
    private String tier;

    /** Free-form job type — e.g. {@code hero-image}, {@code landing-content}. */
    private String jobType;

    /** Optional organization name for logging / prompt composition. */
    private String organizationName;

    /** Optional language code (vi / en). */
    private String language;

    /** Optional logo URL for image-generation jobs. */
    private String logoUrl;

    /** When the dispatcher published this message — used for wait-time metrics. */
    private Instant enqueuedAt;
}
