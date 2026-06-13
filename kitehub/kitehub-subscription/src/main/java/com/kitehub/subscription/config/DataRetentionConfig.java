package com.kitehub.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for data retention periods per pricing tier.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "kitehub.data-retention")
@Data
public class DataRetentionConfig {

    /**
     * Retention days for TRIAL tier after suspension.
     */
    private int trial = 7;

    /**
     * Retention days for FREE tier after suspension.
     */
    private int free = 7;

    /**
     * Retention days for BASIC tier after suspension.
     */
    private int basic = 30;

    /**
     * Retention days for PREMIUM tier after suspension.
     */
    private int premium = 60;

    /**
     * Retention days for ENTERPRISE tier after suspension.
     */
    private int enterprise = 90;

    /**
     * Number of warning notifications before data deletion.
     */
    private int warningCount = 2;

    /**
     * Final-warning lead window in days before deletion (GAP-1026).
     *
     * <p>DataRetentionService fires the final "data will be deleted" warning when the
     * instance is within this many days of its retention expiry. Range-based (not an
     * exact == 1 day check) so a cron-downtime day no longer skips the warning forever.
     * De-dup across the window is handled by EmailServiceClient per-day idempotency.</p>
     */
    private int finalWarningLeadDays = 1;

    /**
     * Get retention days by tier name.
     *
     * @param tier pricing tier name
     * @return retention days for the given tier
     */
    public int getRetentionDays(String tier) {
        return switch (tier.toUpperCase()) {
            case "TRIAL", "FREE" -> trial;
            case "BASIC" -> basic;
            case "PREMIUM" -> premium;
            case "ENTERPRISE" -> enterprise;
            default -> free;
        };
    }
}
