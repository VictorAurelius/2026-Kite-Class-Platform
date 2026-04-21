package com.kitehub.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for trial-to-paid migration behavior.
 *
 * <p>Maps to {@code kitehub.trial-to-paid} properties — see rules.md §4 for the
 * canonical YAML.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@Configuration
@ConfigurationProperties(prefix = "kitehub.trial-to-paid")
@Data
public class TrialToPaidConfig {

    /** "flip-in-place" (default) vs future "shadow-provision". */
    private String strategy = "flip-in-place";

    /** Rule T2P-04 — auto-rollback window after PAYMENT_CAPTURED / COMPLETED. */
    private int reversalWindowHours = 24;

    /** Rule T2P-05 — grace period after trial expiry to still upgrade without re-provision. */
    private int rescueWindowHours = 24;

    /** Rule T2P-06 — allow shadow provisioning for cross-tier upgrades. */
    private boolean shadowCrossTier = false;

    /** Rule T2P-03 — p95 SLA surfaced to FE for poll hint. */
    private int backendP95Seconds = 5;

    /** Rule T2P-09 — max retry attempts on MIGRATING failure. */
    private int retryAttempts = 3;

    /** Rule T2P-09 — retry backoff schedule (seconds). Size ≥ {@code retryAttempts - 1}. */
    private List<Integer> retryBackoffSeconds = List.of(1, 3, 9);

    /** Phase 4b-i — scheduler fixed-delay between MIGRATING pickups. */
    private long schedulerFixedDelayMs = 5_000L;

    /** Phase 4b-i — shared HMAC secret for the migration webhook. */
    private String webhookSecret = "";
}
