package com.kitehub.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate limit configuration by pricing tier.
 * <p>
 * Defines request limits per minute for each tier:
 * - FREE: 100 requests/minute
 * - BASIC: 500 requests/minute
 * - PREMIUM: 2000 requests/minute
 * - ENTERPRISE: 10000 requests/minute
 *
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kitehub.rate-limit")
public class RateLimitConfig {

    /**
     * Rate limits by tier (requests per minute).
     */
    private Map<String, Integer> limits = new HashMap<>();

    /**
     * Default rate limit if tier not found.
     */
    private int defaultLimit = 100;

    /**
     * Get rate limit for tier.
     *
     * @param tier pricing tier (FREE, BASIC, PREMIUM, ENTERPRISE)
     * @return requests per minute allowed
     */
    public int getLimitForTier(String tier) {
        return limits.getOrDefault(tier, defaultLimit);
    }
}
