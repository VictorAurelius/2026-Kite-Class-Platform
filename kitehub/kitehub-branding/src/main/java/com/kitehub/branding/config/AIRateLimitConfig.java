package com.kitehub.branding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI rate limiting per subscription tier.
 * Limits are expressed as maximum AI requests per instance per day.
 * A value of -1 means unlimited.
 *
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.rate-limit")
public class AIRateLimitConfig {

    private int freePerDay = 3;
    private int basicPerDay = 10;
    private int premiumPerDay = 50;
    private int enterprisePerDay = -1; // unlimited

    /**
     * Get the daily AI request limit for a given subscription tier.
     *
     * @param tier subscription tier name (FREE, TRIAL, BASIC, PREMIUM, ENTERPRISE)
     * @return daily limit, or -1 for unlimited
     */
    public int getLimitForTier(String tier) {
        return switch (tier.toUpperCase()) {
            case "FREE", "TRIAL" -> freePerDay;
            case "BASIC" -> basicPerDay;
            case "PREMIUM" -> premiumPerDay;
            case "ENTERPRISE" -> enterprisePerDay;
            default -> freePerDay;
        };
    }
}
