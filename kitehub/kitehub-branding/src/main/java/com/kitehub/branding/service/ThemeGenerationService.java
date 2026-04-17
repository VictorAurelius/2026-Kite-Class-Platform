package com.kitehub.branding.service;

import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.dto.ThemeConfig;
import com.kitehub.branding.util.ColorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating theme config JSON from logo analysis.
 * Maps AI-extracted brand colors to complete theme configuration.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ThemeGenerationService {

    /**
     * Generate complete theme config from logo analysis.
     *
     * @param analysis Logo analysis from AI
     * @return Complete theme configuration
     */
    public ThemeConfig generateThemeConfig(LogoAnalysis analysis) {
        log.info("Generating theme config for theme: {}", analysis.getTheme());

        return ThemeConfig.builder()
                .colors(generateColorScheme(analysis))
                .typography(generateTypography(analysis))
                .spacing(generateSpacing(analysis))
                .layout(generateLayout(analysis))
                .build();
    }

    /**
     * Generate color scheme with variants from base colors.
     * Uses simple lightness adjustments for now - can be enhanced with proper color science later.
     */
    private ThemeConfig.ColorScheme generateColorScheme(LogoAnalysis analysis) {
        return ThemeConfig.ColorScheme.builder()
                .primary(generateColorVariants(analysis.getPrimaryColor()))
                .secondary(generateColorVariants(analysis.getSecondaryColor()))
                .accent(generateColorVariants(analysis.getAccentColor()))
                .neutral(generateNeutralVariants())
                .semantic(generateSemanticColors())
                .build();
    }

    /**
     * Generate color variants (50-900 shades) from base color.
     * Simple implementation: adjust lightness in HSL space.
     */
    private ThemeConfig.ColorVariants generateColorVariants(String baseColor) {
        return ThemeConfig.ColorVariants.builder()
                .shade50(lighten(baseColor, 0.5))
                .shade100(lighten(baseColor, 0.4))
                .shade200(lighten(baseColor, 0.3))
                .shade300(lighten(baseColor, 0.2))
                .shade400(lighten(baseColor, 0.1))
                .shade500(baseColor)  // Base color
                .shade600(darken(baseColor, 0.1))
                .shade700(darken(baseColor, 0.2))
                .shade800(darken(baseColor, 0.3))
                .shade900(darken(baseColor, 0.4))
                .build();
    }

    /**
     * Generate neutral gray variants for backgrounds/borders.
     */
    private ThemeConfig.ColorVariants generateNeutralVariants() {
        return ThemeConfig.ColorVariants.builder()
                .shade50("#FAFAFA")
                .shade100("#F5F5F5")
                .shade200("#E5E5E5")
                .shade300("#D4D4D4")
                .shade400("#A3A3A3")
                .shade500("#737373")
                .shade600("#525252")
                .shade700("#404040")
                .shade800("#262626")
                .shade900("#171717")
                .build();
    }

    /**
     * Generate semantic colors for UI states.
     */
    private ThemeConfig.SemanticColors generateSemanticColors() {
        return ThemeConfig.SemanticColors.builder()
                .success("#10B981")  // Green
                .warning("#F59E0B")  // Amber
                .error("#EF4444")    // Red
                .info("#3B82F6")     // Blue
                .build();
    }

    /**
     * Generate typography based on theme style.
     */
    private ThemeConfig.Typography generateTypography(LogoAnalysis analysis) {
        String theme = analysis.getTheme();
        boolean isModern = "MODERN".equals(theme) || "MINIMAL".equals(theme);

        return ThemeConfig.Typography.builder()
                .fontFamilyHeading(isModern ?
                    "'Inter', 'SF Pro Display', system-ui, sans-serif" :
                    "'Playfair Display', 'Georgia', serif")
                .fontFamilyBody("'Inter', 'SF Pro Text', system-ui, sans-serif")
                .fontSizeBase("16px")
                .fontSizes(ThemeConfig.FontSizes.builder()
                        .xs("0.75rem")
                        .sm("0.875rem")
                        .base("1rem")
                        .lg("1.125rem")
                        .xl("1.25rem")
                        .xl2("1.5rem")
                        .xl3("1.875rem")
                        .xl4("2.25rem")
                        .build())
                .fontWeights(ThemeConfig.FontWeights.builder()
                        .normal(400)
                        .medium(500)
                        .semibold(600)
                        .bold(700)
                        .build())
                .lineHeights(ThemeConfig.LineHeights.builder()
                        .tight("1.25")
                        .normal("1.5")
                        .relaxed("1.75")
                        .build())
                .build();
    }

    /**
     * Generate spacing based on theme style.
     */
    private ThemeConfig.Spacing generateSpacing(LogoAnalysis analysis) {
        String theme = analysis.getTheme();
        boolean isMinimal = "MINIMAL".equals(theme);

        return ThemeConfig.Spacing.builder()
                .unit(isMinimal ? 8 : 4)
                .sectionSpacing(isMinimal ? "6rem" : "4rem")
                .componentSpacing(isMinimal ? "2rem" : "1.5rem")
                .elementSpacing(isMinimal ? "1rem" : "0.75rem")
                .build();
    }

    /**
     * Generate layout based on theme style.
     */
    private ThemeConfig.Layout generateLayout(LogoAnalysis analysis) {
        String theme = analysis.getTheme();
        boolean isPlayful = "PLAYFUL".equals(theme);

        return ThemeConfig.Layout.builder()
                .maxWidth("1280px")
                .borderRadius(ThemeConfig.BorderRadius.builder()
                        .sm(isPlayful ? "0.5rem" : "0.25rem")
                        .base(isPlayful ? "0.75rem" : "0.5rem")
                        .lg(isPlayful ? "1rem" : "0.75rem")
                        .full("9999px")
                        .build())
                .shadow(ThemeConfig.Shadow.builder()
                        .sm("0 1px 2px 0 rgba(0, 0, 0, 0.05)")
                        .base("0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)")
                        .lg("0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)")
                        .build())
                .build();
    }

    /**
     * Lighten a hex color by percentage using HSL color space.
     */
    private String lighten(String hex, double percentage) {
        return ColorUtils.lighten(hex, percentage);
    }

    /**
     * Darken a hex color by percentage using HSL color space.
     */
    private String darken(String hex, double percentage) {
        return ColorUtils.darken(hex, percentage);
    }
}
