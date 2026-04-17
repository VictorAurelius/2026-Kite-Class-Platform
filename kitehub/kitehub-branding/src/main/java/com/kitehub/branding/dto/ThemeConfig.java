package com.kitehub.branding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complete theme configuration for KiteClass frontend.
 * Output format matches frontend ThemeConfig type expectations.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeConfig {

    /**
     * Color scheme configuration.
     */
    private ColorScheme colors;

    /**
     * Typography configuration.
     */
    private Typography typography;

    /**
     * Spacing configuration.
     */
    private Spacing spacing;

    /**
     * Layout configuration.
     */
    private Layout layout;

    /**
     * Color scheme with all color variants.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorScheme {
        /**
         * Primary color variants (50, 100, 200, ..., 900).
         */
        private ColorVariants primary;

        /**
         * Secondary color variants.
         */
        private ColorVariants secondary;

        /**
         * Accent color variants.
         */
        private ColorVariants accent;

        /**
         * Neutral gray variants for backgrounds/borders.
         */
        private ColorVariants neutral;

        /**
         * Semantic colors.
         */
        private SemanticColors semantic;
    }

    /**
     * Color variants for a single color family.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorVariants {
        private String shade50;
        private String shade100;
        private String shade200;
        private String shade300;
        private String shade400;
        private String shade500;  // Base color
        private String shade600;
        private String shade700;
        private String shade800;
        private String shade900;
    }

    /**
     * Semantic colors for UI states.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemanticColors {
        private String success;
        private String warning;
        private String error;
        private String info;
    }

    /**
     * Typography configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Typography {
        /**
         * Font family for headings.
         */
        private String fontFamilyHeading;

        /**
         * Font family for body text.
         */
        private String fontFamilyBody;

        /**
         * Base font size in rem.
         */
        private String fontSizeBase;

        /**
         * Font sizes for headings (h1-h6).
         */
        private FontSizes fontSizes;

        /**
         * Font weights.
         */
        private FontWeights fontWeights;

        /**
         * Line heights.
         */
        private LineHeights lineHeights;
    }

    /**
     * Font size scale.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FontSizes {
        private String xs;
        private String sm;
        private String base;
        private String lg;
        private String xl;
        private String xl2;
        private String xl3;
        private String xl4;
    }

    /**
     * Font weight scale.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FontWeights {
        private int normal;
        private int medium;
        private int semibold;
        private int bold;
    }

    /**
     * Line height scale.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineHeights {
        private String tight;
        private String normal;
        private String relaxed;
    }

    /**
     * Spacing configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Spacing {
        /**
         * Base spacing unit in px.
         */
        private int unit;

        /**
         * Section vertical spacing.
         */
        private String sectionSpacing;

        /**
         * Component spacing.
         */
        private String componentSpacing;

        /**
         * Element spacing.
         */
        private String elementSpacing;
    }

    /**
     * Layout configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Layout {
        /**
         * Maximum content width.
         */
        private String maxWidth;

        /**
         * Border radius scale.
         */
        private BorderRadius borderRadius;

        /**
         * Shadow scale.
         */
        private Shadow shadow;
    }

    /**
     * Border radius scale.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BorderRadius {
        private String sm;
        private String base;
        private String lg;
        private String full;
    }

    /**
     * Shadow scale.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Shadow {
        private String sm;
        private String base;
        private String lg;
    }
}
