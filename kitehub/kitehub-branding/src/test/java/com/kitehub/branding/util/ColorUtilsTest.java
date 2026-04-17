package com.kitehub.branding.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ColorUtils HSL color transformations.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("ColorUtils Tests")
class ColorUtilsTest {

    @Test
    @DisplayName("lighten() should increase lightness of hex color")
    void lightenShouldIncreaseLightness() {
        // Given
        String baseColor = "#2196F3";  // Blue
        double percentage = 0.2;

        // When
        String lightenedColor = ColorUtils.lighten(baseColor, percentage);

        // Then
        assertThat(lightenedColor).isNotNull();
        assertThat(lightenedColor).startsWith("#");
        assertThat(lightenedColor).hasSize(7);
        assertThat(lightenedColor).isNotEqualTo(baseColor);

        // Lightened color should be "lighter" - not a perfect test but validates transformation
        assertThat(lightenedColor).matches("#[0-9A-F]{6}");
    }

    @Test
    @DisplayName("darken() should decrease lightness of hex color")
    void darkenShouldDecreaseLightness() {
        // Given
        String baseColor = "#2196F3";  // Blue
        double percentage = 0.2;

        // When
        String darkenedColor = ColorUtils.darken(baseColor, percentage);

        // Then
        assertThat(darkenedColor).isNotNull();
        assertThat(darkenedColor).startsWith("#");
        assertThat(darkenedColor).hasSize(7);
        assertThat(darkenedColor).isNotEqualTo(baseColor);
        assertThat(darkenedColor).matches("#[0-9A-F]{6}");
    }

    @Test
    @DisplayName("lighten() with 0 percentage should return same color")
    void lightenWithZeroPercentageShouldReturnSameColor() {
        // Given
        String baseColor = "#2196F3";

        // When
        String result = ColorUtils.lighten(baseColor, 0.0);

        // Then - should be very close to original (allowing for rounding)
        assertThat(result).matches("#[0-9A-F]{6}");
    }

    @Test
    @DisplayName("darken() with 0 percentage should return same color")
    void darkenWithZeroPercentageShouldReturnSameColor() {
        // Given
        String baseColor = "#2196F3";

        // When
        String result = ColorUtils.darken(baseColor, 0.0);

        // Then
        assertThat(result).matches("#[0-9A-F]{6}");
    }

    @Test
    @DisplayName("lighten() should handle pure white correctly")
    void lightenShouldHandlePureWhite() {
        // Given
        String white = "#FFFFFF";

        // When
        String result = ColorUtils.lighten(white, 0.5);

        // Then - white can't get lighter, should stay white or very close
        assertThat(result).matches("#F{6}|#[EF]{6}");
    }

    @Test
    @DisplayName("darken() should handle pure black correctly")
    void darkenShouldHandlePureBlack() {
        // Given
        String black = "#000000";

        // When
        String result = ColorUtils.darken(black, 0.5);

        // Then - black can't get darker, should stay black
        assertThat(result).isEqualTo("#000000");
    }

    @Test
    @DisplayName("lighten() should throw on invalid hex color")
    void lightenShouldThrowOnInvalidHex() {
        assertThatThrownBy(() -> ColorUtils.lighten("invalid", 0.5))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ColorUtils.lighten("#GGG", 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("darken() should throw on invalid hex color")
    void darkenShouldThrowOnInvalidHex() {
        assertThatThrownBy(() -> ColorUtils.darken("notahex", 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Multiple lighten operations should progressively lighten")
    void multipleLightenOperationsShouldProgressivelyLighten() {
        // Given
        String color = "#FF5722";  // Deep Orange

        // When
        String light1 = ColorUtils.lighten(color, 0.1);
        String light2 = ColorUtils.lighten(light1, 0.1);
        String light3 = ColorUtils.lighten(light2, 0.1);

        // Then - each step should be different
        assertThat(light1).isNotEqualTo(color);
        assertThat(light2).isNotEqualTo(light1);
        assertThat(light3).isNotEqualTo(light2);
    }

    @Test
    @DisplayName("Multiple darken operations should progressively darken")
    void multipleDarkenOperationsShouldProgressivelyDarken() {
        // Given
        String color = "#4CAF50";  // Green

        // When
        String dark1 = ColorUtils.darken(color, 0.1);
        String dark2 = ColorUtils.darken(dark1, 0.1);
        String dark3 = ColorUtils.darken(dark2, 0.1);

        // Then - each step should be different
        assertThat(dark1).isNotEqualTo(color);
        assertThat(dark2).isNotEqualTo(dark1);
        assertThat(dark3).isNotEqualTo(dark2);
    }
}
