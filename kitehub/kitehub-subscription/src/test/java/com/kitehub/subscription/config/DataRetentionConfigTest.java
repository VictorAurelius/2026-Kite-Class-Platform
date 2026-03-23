package com.kitehub.subscription.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DataRetentionConfig.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("DataRetentionConfig Unit Tests")
class DataRetentionConfigTest {

    @Test
    @DisplayName("Should have correct default values")
    void shouldHaveCorrectDefaultValues() {
        // Given
        DataRetentionConfig config = new DataRetentionConfig();

        // Then
        assertThat(config.getTrial()).isEqualTo(7);
        assertThat(config.getFree()).isEqualTo(7);
        assertThat(config.getBasic()).isEqualTo(30);
        assertThat(config.getPremium()).isEqualTo(60);
        assertThat(config.getEnterprise()).isEqualTo(90);
        assertThat(config.getWarningCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return correct retention days by tier")
    void shouldReturnCorrectRetentionDaysByTier() {
        // Given
        DataRetentionConfig config = new DataRetentionConfig();

        // Then
        assertThat(config.getRetentionDays("TRIAL")).isEqualTo(7);
        assertThat(config.getRetentionDays("FREE")).isEqualTo(7);
        assertThat(config.getRetentionDays("BASIC")).isEqualTo(30);
        assertThat(config.getRetentionDays("PREMIUM")).isEqualTo(60);
        assertThat(config.getRetentionDays("ENTERPRISE")).isEqualTo(90);
    }

    @Test
    @DisplayName("Should handle case-insensitive tier names")
    void shouldHandleCaseInsensitiveTierNames() {
        // Given
        DataRetentionConfig config = new DataRetentionConfig();

        // Then
        assertThat(config.getRetentionDays("trial")).isEqualTo(7);
        assertThat(config.getRetentionDays("basic")).isEqualTo(30);
        assertThat(config.getRetentionDays("Premium")).isEqualTo(60);
    }

    @Test
    @DisplayName("Should return free tier retention for unknown tier")
    void shouldReturnFreeTierRetentionForUnknownTier() {
        // Given
        DataRetentionConfig config = new DataRetentionConfig();

        // Then
        assertThat(config.getRetentionDays("UNKNOWN")).isEqualTo(7);
        assertThat(config.getRetentionDays("CUSTOM")).isEqualTo(7);
    }

    @Test
    @DisplayName("Should allow setting custom retention values")
    void shouldAllowSettingCustomRetentionValues() {
        // Given
        DataRetentionConfig config = new DataRetentionConfig();

        // When
        config.setTrial(14);
        config.setBasic(60);

        // Then
        assertThat(config.getRetentionDays("TRIAL")).isEqualTo(14);
        assertThat(config.getRetentionDays("BASIC")).isEqualTo(60);
    }
}
