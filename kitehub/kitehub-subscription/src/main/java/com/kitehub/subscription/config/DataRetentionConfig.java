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
