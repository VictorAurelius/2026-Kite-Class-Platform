package com.kitehub.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RateLimitConfig} (GAP-259 tier multiplier additions).
 */
@DisplayName("RateLimitConfig")
class RateLimitConfigTest {

    @Test
    @DisplayName("default tier multipliers: FREE 1× / BASIC 1× / PREMIUM 3× / ENTERPRISE 10×")
    void defaultMultipliers() {
        RateLimitConfig config = new RateLimitConfig();
        assertThat(config.getMultiplierForTier("FREE")).isEqualTo(1.0);
        assertThat(config.getMultiplierForTier("BASIC")).isEqualTo(1.0);
        assertThat(config.getMultiplierForTier("PREMIUM")).isEqualTo(3.0);
        assertThat(config.getMultiplierForTier("ENTERPRISE")).isEqualTo(10.0);
    }

    @Test
    @DisplayName("getMultiplierForTier is case-insensitive")
    void caseInsensitive() {
        RateLimitConfig config = new RateLimitConfig();
        assertThat(config.getMultiplierForTier("premium")).isEqualTo(3.0);
        assertThat(config.getMultiplierForTier("Enterprise")).isEqualTo(10.0);
    }

    @Test
    @DisplayName("unknown tier defaults to 1.0×")
    void unknownTierDefaults() {
        RateLimitConfig config = new RateLimitConfig();
        assertThat(config.getMultiplierForTier("MYSTERY")).isEqualTo(1.0);
        assertThat(config.getMultiplierForTier(null)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("getLimitForTier returns map value or default")
    void limitForTier() {
        RateLimitConfig config = new RateLimitConfig();
        config.getLimits().put("FREE", 100);
        config.getLimits().put("PREMIUM", 2000);
        assertThat(config.getLimitForTier("FREE")).isEqualTo(100);
        assertThat(config.getLimitForTier("PREMIUM")).isEqualTo(2000);
        assertThat(config.getLimitForTier("UNKNOWN")).isEqualTo(config.getDefaultLimit());
    }
}
