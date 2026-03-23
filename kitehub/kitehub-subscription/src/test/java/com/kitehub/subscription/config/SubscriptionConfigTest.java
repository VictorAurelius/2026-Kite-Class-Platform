package com.kitehub.subscription.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SubscriptionConfig.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("SubscriptionConfig Unit Tests")
class SubscriptionConfigTest {

    @Test
    @DisplayName("Should have correct default values")
    void shouldHaveCorrectDefaultValues() {
        // Given
        SubscriptionConfig config = new SubscriptionConfig();

        // Then
        assertThat(config.getGracePeriodDays()).isEqualTo(3);
        assertThat(config.getWarningDays()).isEqualTo(List.of(7, 3, 1));
    }

    @Test
    @DisplayName("Should allow setting custom values")
    void shouldAllowSettingCustomValues() {
        // Given
        SubscriptionConfig config = new SubscriptionConfig();

        // When
        config.setGracePeriodDays(7);
        config.setWarningDays(List.of(14, 7, 3, 1));

        // Then
        assertThat(config.getGracePeriodDays()).isEqualTo(7);
        assertThat(config.getWarningDays()).isEqualTo(List.of(14, 7, 3, 1));
    }
}
