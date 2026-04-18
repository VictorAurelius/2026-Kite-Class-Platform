package com.kitehub.branding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the fair-queueing AI pipeline (GAP-005a).
 *
 * <p>Binds to the {@code ai.queue} prefix in {@code application.yml} and
 * exposes:</p>
 * <ul>
 *   <li>{@code fairQueueEnabled} — global feature flag; disable to revert
 *   to the legacy single-queue behaviour.</li>
 *   <li>{@code tierWeights} — weighted-round-robin weights per tier
 *   (ENTERPRISE:PRO:FREE = 3:2:1 by default).</li>
 *   <li>{@code concurrency} — max in-flight jobs per tenant per tier.</li>
 *   <li>{@code sla} — target p95 wait time in seconds per tier.</li>
 *   <li>{@code backpressure} — thresholds for automatic degradation.</li>
 * </ul>
 *
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.queue")
public class AIQueueProperties {

    /** Global feature flag — when false, legacy single-queue is used. */
    private boolean fairQueueEnabled = true;

    /** Weighted round-robin weights per tier. */
    private TierWeights tierWeights = new TierWeights();

    /** Max concurrent jobs per instance per tier (Redis semaphore cap). */
    private Concurrency concurrency = new Concurrency();

    /** SLA targets (p95 wait in seconds). */
    private Sla sla = new Sla();

    /** Backpressure thresholds. */
    private Backpressure backpressure = new Backpressure();

    @Data
    public static class TierWeights {
        private int enterprise = 3;
        private int pro = 2;
        private int free = 1;
    }

    @Data
    public static class Concurrency {
        private int free = 1;
        private int pro = 3;
        private int enterprise = 10;
    }

    @Data
    public static class Sla {
        /** Free tier p95 wait target. */
        private int freeP95Seconds = 180;
        /** Pro tier p95 wait target. */
        private int proP95Seconds = 60;
        /** Enterprise tier p95 wait target. */
        private int enterpriseP95Seconds = 30;
    }

    @Data
    public static class Backpressure {
        /** When enterprise queue depth exceeds this, free tier degrades to template fallback. */
        private int enterpriseBacklogThreshold = 50;
    }
}
