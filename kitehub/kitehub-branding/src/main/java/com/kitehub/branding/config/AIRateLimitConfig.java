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
    // GAP-1119: canonical SUB-22 PREMIUM regen = 30 (was 50 — drift fixed 2026-06-10).
    private int premiumPerDay = 30;
    private int enterprisePerDay = -1; // unlimited

    /**
     * FULL_AI (GPT image-gen) monthly cost quota per instance (GAP-1119).
     * FULL_AI is the paid, cost-bearing path (Gemini TEMPLATE = $0), so it carries
     * a tighter quota than the per-day regen limit above. PREMIUM = limited;
     * ENTERPRISE = unlimited (-1). FREE / BASIC are not FULL_AI-eligible at all
     * (see {@code GenerationMode.forTier}) so they have no FULL_AI quota.
     */
    private int fullaiPremiumPerMonth = 5;
    private int fullaiEnterprisePerMonth = -1; // unlimited

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

    /**
     * Get the monthly FULL_AI (paid image-gen) quota for a given tier (GAP-1119).
     * Only PREMIUM + ENTERPRISE are FULL_AI-eligible; any other tier returns 0
     * (no FULL_AI allowance).
     *
     * @param tier subscription tier name
     * @return monthly FULL_AI quota, -1 for unlimited, 0 if tier not eligible
     */
    public int getFullAiMonthlyQuotaForTier(String tier) {
        if (tier == null) {
            return 0;
        }
        return switch (tier.trim().toUpperCase()) {
            case "PREMIUM" -> fullaiPremiumPerMonth;
            case "ENTERPRISE" -> fullaiEnterprisePerMonth;
            default -> 0;
        };
    }
}
