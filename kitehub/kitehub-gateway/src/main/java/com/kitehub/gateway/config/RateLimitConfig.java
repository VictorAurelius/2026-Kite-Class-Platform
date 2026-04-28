package com.kitehub.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate limit configuration by pricing tier.
 *
 * <p>Two complementary settings:
 * <ul>
 *   <li>{@code limits} — base requests-per-minute per tier. Currently informational
 *       (Spring Cloud Gateway {@code RedisRateLimiter} uses per-route static config).</li>
 *   <li>{@code tierMultiplier} — multiplier applied to the per-route base burst capacity
 *       once tier-aware enforcement lands (GAP-260 follow-up). Shipped as data-only in
 *       this PR so route YAML can reference the keys; actual multiplier wiring requires a
 *       custom {@code RedisRateLimiter} extension that reads tier from the resolved key.</li>
 * </ul>
 *
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kitehub.rate-limit")
public class RateLimitConfig {

    /**
     * Rate limits by tier (requests per minute) — base.
     */
    private Map<String, Integer> limits = new HashMap<>();

    /**
     * Tier-aware burst capacity multiplier (GAP-259 §AC tier multiplier).
     *
     * <p>Defaults: FREE/BASIC 1×, PREMIUM 3×, ENTERPRISE 10×. Read by the future
     * tier-aware {@code RedisRateLimiter} extension (GAP-260). Available now as
     * config so ops can tune without a code change.</p>
     */
    private Map<String, Double> tierMultiplier = defaultTierMultiplier();

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

    /**
     * Get the tier-aware burst multiplier. Unknown tiers default to 1.0×.
     *
     * @param tier pricing tier
     * @return multiplier (1.0 means base burst, 3.0 means 3× base burst)
     */
    public double getMultiplierForTier(String tier) {
        if (tier == null) {
            return 1.0;
        }
        return tierMultiplier.getOrDefault(tier.toUpperCase(), 1.0);
    }

    private static Map<String, Double> defaultTierMultiplier() {
        Map<String, Double> m = new HashMap<>();
        m.put("FREE", 1.0);
        m.put("BASIC", 1.0);
        m.put("PREMIUM", 3.0);
        m.put("ENTERPRISE", 10.0);
        return m;
    }
}
