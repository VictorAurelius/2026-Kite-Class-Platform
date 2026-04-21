package com.kitehub.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
}
