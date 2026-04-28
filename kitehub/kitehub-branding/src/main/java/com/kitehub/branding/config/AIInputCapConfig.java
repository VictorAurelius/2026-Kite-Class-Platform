package com.kitehub.branding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Tier-aware input prompt size caps for AI provider calls (GAP-258).
 *
 * <p>Defends against cost-attack DDoS: an attacker sends a small number of
 * requests but each request carries a very long prompt, racking up provider
 * tokens. The per-day request count cap in {@link AIRateLimitConfig} alone
 * does not bound input cost.</p>
 *
 * <p>A value of {@code -1} means unlimited.</p>
 *
 * @since 1.4.0 (GAP-258)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.input")
public class AIInputCapConfig {

    /** Estimated tokens — FREE / TRIAL tier. */
    private int freeMaxTokens = 2000;

    /** Estimated tokens — BASIC tier. */
    private int basicMaxTokens = 4000;

    /** Estimated tokens — PREMIUM tier. */
    private int premiumMaxTokens = 8000;

    /** Estimated tokens — ENTERPRISE tier (-1 = unlimited). */
    private int enterpriseMaxTokens = 16000;

    /**
     * Resolve the maximum input token cap for a given subscription tier.
     *
     * @param tier subscription tier name (FREE, TRIAL, BASIC, PREMIUM, ENTERPRISE);
     *             unknown tiers default to FREE for fail-safe behaviour.
     * @return token cap, or {@code -1} for unlimited
     */
    public int getMaxTokensForTier(String tier) {
        if (tier == null) {
            return freeMaxTokens;
        }
        return switch (tier.toUpperCase()) {
            case "FREE", "TRIAL" -> freeMaxTokens;
            case "BASIC" -> basicMaxTokens;
            case "PREMIUM" -> premiumMaxTokens;
            case "ENTERPRISE" -> enterpriseMaxTokens;
            default -> freeMaxTokens;
        };
    }
}
