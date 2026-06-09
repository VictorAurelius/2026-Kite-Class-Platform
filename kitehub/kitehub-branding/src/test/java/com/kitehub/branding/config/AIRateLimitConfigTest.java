package com.kitehub.branding.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AIRateLimitConfig tier-based limit resolution.
 *
 * @since 1.0.0
 */
class AIRateLimitConfigTest {

    private AIRateLimitConfig config;

    @BeforeEach
    void setUp() {
        config = new AIRateLimitConfig();
        config.setFreePerDay(3);
        config.setBasicPerDay(10);
        config.setPremiumPerDay(50);
        config.setEnterprisePerDay(-1);
    }

    @Test
    void getLimitForTier_free_returnsFreeTierLimit() {
        assertThat(config.getLimitForTier("FREE")).isEqualTo(3);
    }

    @Test
    void getLimitForTier_trial_returnsFreeTierLimit() {
        assertThat(config.getLimitForTier("TRIAL")).isEqualTo(3);
    }

    @Test
    void getLimitForTier_basic_returnsBasicLimit() {
        assertThat(config.getLimitForTier("BASIC")).isEqualTo(10);
    }

    @Test
    void getLimitForTier_premium_returnsPremiumLimit() {
        assertThat(config.getLimitForTier("PREMIUM")).isEqualTo(50);
    }

    @Test
    void getLimitForTier_enterprise_returnsUnlimited() {
        assertThat(config.getLimitForTier("ENTERPRISE")).isEqualTo(-1);
    }

    @Test
    void getLimitForTier_caseInsensitive() {
        assertThat(config.getLimitForTier("free")).isEqualTo(3);
        assertThat(config.getLimitForTier("Basic")).isEqualTo(10);
        assertThat(config.getLimitForTier("premium")).isEqualTo(50);
        assertThat(config.getLimitForTier("enterprise")).isEqualTo(-1);
    }

    @Test
    void getLimitForTier_unknown_defaultsToFreeLimit() {
        assertThat(config.getLimitForTier("UNKNOWN")).isEqualTo(3);
        assertThat(config.getLimitForTier("")).isEqualTo(3);
    }

    @Test
    void defaultValues_areCorrect() {
        AIRateLimitConfig defaultConfig = new AIRateLimitConfig();
        assertThat(defaultConfig.getFreePerDay()).isEqualTo(3);
        assertThat(defaultConfig.getBasicPerDay()).isEqualTo(10);
        // GAP-1119: canonical SUB-22 PREMIUM regen = 30 (was 50).
        assertThat(defaultConfig.getPremiumPerDay()).isEqualTo(30);
        assertThat(defaultConfig.getEnterprisePerDay()).isEqualTo(-1);
    }

    @Test
    void getFullAiMonthlyQuotaForTier_returnsTierQuota() {
        // GAP-1119: FULL_AI eligible only PREMIUM (limited) + ENTERPRISE (unlimited).
        assertThat(config.getFullAiMonthlyQuotaForTier("PREMIUM")).isEqualTo(5);
        assertThat(config.getFullAiMonthlyQuotaForTier("ENTERPRISE")).isEqualTo(-1);
        assertThat(config.getFullAiMonthlyQuotaForTier("BASIC")).isZero();
        assertThat(config.getFullAiMonthlyQuotaForTier("FREE")).isZero();
        assertThat(config.getFullAiMonthlyQuotaForTier(null)).isZero();
    }
}
