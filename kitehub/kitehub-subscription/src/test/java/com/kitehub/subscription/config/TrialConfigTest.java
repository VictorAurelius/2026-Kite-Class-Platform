package com.kitehub.subscription.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TrialConfig.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("TrialConfig Unit Tests")
class TrialConfigTest {

    @Test
    @DisplayName("Should have correct default values")
    void shouldHaveCorrectDefaultValues() {
        // Given
        TrialConfig config = new TrialConfig();

        // Then
        assertThat(config.getDurationDays()).isEqualTo(14);
        assertThat(config.getMaxPerOwner()).isEqualTo(1);
        // TR-08 / GAP-1270 — widened conversion cadence from [3, 1] to [10, 5, 3, 1].
        assertThat(config.getWarningDays()).isEqualTo(List.of(10, 5, 3, 1));
        assertThat(config.getMidpointDay()).isEqualTo(7);
        assertThat(config.getExtensionDays()).isEqualTo(7);
        assertThat(config.isAutoExtendOnExpiry()).isFalse();
    }

    @Test
    @DisplayName("Should allow setting custom values")
    void shouldAllowSettingCustomValues() {
        // Given
        TrialConfig config = new TrialConfig();

        // When
        config.setDurationDays(30);
        config.setMaxPerOwner(3);
        config.setWarningDays(List.of(7, 3, 1));
        config.setMidpointDay(15);

        // Then
        assertThat(config.getDurationDays()).isEqualTo(30);
        assertThat(config.getMaxPerOwner()).isEqualTo(3);
        assertThat(config.getWarningDays()).isEqualTo(List.of(7, 3, 1));
        assertThat(config.getMidpointDay()).isEqualTo(15);
    }
}
