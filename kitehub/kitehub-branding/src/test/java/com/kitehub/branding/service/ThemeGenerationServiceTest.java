package com.kitehub.branding.service;

import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.dto.ThemeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ThemeGenerationService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("ThemeGenerationService Tests")
class ThemeGenerationServiceTest {

    private ThemeGenerationService themeGenerationService;

    @BeforeEach
    void setUp() {
        themeGenerationService = new ThemeGenerationService();
    }

    @Test
    @DisplayName("generateThemeConfig should create complete theme from logo analysis")
    void generateThemeConfigShouldCreateCompleteTheme() {
        // Given
        LogoAnalysis analysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .typography("Clean Sans-serif")
                .targetAudience("Students and parents")
                .brandPersonality(List.of("Professional", "Friendly"))
                .build();

        // When
        ThemeConfig themeConfig = themeGenerationService.generateThemeConfig(analysis);

        // Then - Verify all major sections exist
        assertThat(themeConfig).isNotNull();
        assertThat(themeConfig.getColors()).isNotNull();
        assertThat(themeConfig.getTypography()).isNotNull();
        assertThat(themeConfig.getSpacing()).isNotNull();
        assertThat(themeConfig.getLayout()).isNotNull();
    }

    @Test
    @DisplayName("generateThemeConfig should generate color variants for primary color")
    void generateThemeConfigShouldGenerateColorVariants() {
        // Given
        LogoAnalysis analysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .build();

        // When
        ThemeConfig themeConfig = themeGenerationService.generateThemeConfig(analysis);

        // Then - Verify primary color variants (50-900)
        ThemeConfig.ColorVariants primary = themeConfig.getColors().getPrimary();
        assertThat(primary).isNotNull();
        assertThat(primary.getShade50()).isNotNull().startsWith("#");
        assertThat(primary.getShade100()).isNotNull().startsWith("#");
        assertThat(primary.getShade200()).isNotNull().startsWith("#");
        assertThat(primary.getShade300()).isNotNull().startsWith("#");
        assertThat(primary.getShade400()).isNotNull().startsWith("#");
        assertThat(primary.getShade500()).isNotNull().startsWith("#");  // Base color
        assertThat(primary.getShade600()).isNotNull().startsWith("#");
        assertThat(primary.getShade700()).isNotNull().startsWith("#");
        assertThat(primary.getShade800()).isNotNull().startsWith("#");
        assertThat(primary.getShade900()).isNotNull().startsWith("#");
    }

    @Test
    @DisplayName("generateThemeConfig should include semantic colors")
    void generateThemeConfigShouldIncludeSemanticColors() {
        // Given
        LogoAnalysis analysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .build();

        // When
        ThemeConfig themeConfig = themeGenerationService.generateThemeConfig(analysis);

        // Then - Verify semantic colors
        ThemeConfig.SemanticColors semantic = themeConfig.getColors().getSemantic();
        assertThat(semantic).isNotNull();
        assertThat(semantic.getSuccess()).isEqualTo("#10B981");  // Green
        assertThat(semantic.getWarning()).isEqualTo("#F59E0B");  // Amber
        assertThat(semantic.getError()).isEqualTo("#EF4444");    // Red
        assertThat(semantic.getInfo()).isEqualTo("#3B82F6");     // Blue
    }

    @Test
    @DisplayName("generateThemeConfig should configure typography based on theme")
    void generateThemeConfigShouldConfigureTypographyBasedOnTheme() {
        // Given - MODERN theme
        LogoAnalysis modernAnalysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .build();

        // When
        ThemeConfig modernTheme = themeGenerationService.generateThemeConfig(modernAnalysis);

        // Then - Modern should use sans-serif heading font
        assertThat(modernTheme.getTypography().getFontFamilyHeading())
                .contains("Inter");

        // Given - CLASSIC theme
        LogoAnalysis classicAnalysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("CLASSIC")
                .build();

        // When
        ThemeConfig classicTheme = themeGenerationService.generateThemeConfig(classicAnalysis);

        // Then - Classic should use serif heading font
        assertThat(classicTheme.getTypography().getFontFamilyHeading())
                .contains("Playfair Display");
    }

    @Test
    @DisplayName("generateThemeConfig should configure spacing based on theme")
    void generateThemeConfigShouldConfigureSpacingBasedOnTheme() {
        // Given - MINIMAL theme
        LogoAnalysis minimalAnalysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MINIMAL")
                .build();

        // When
        ThemeConfig minimalTheme = themeGenerationService.generateThemeConfig(minimalAnalysis);

        // Then - Minimal should use 8px unit
        assertThat(minimalTheme.getSpacing().getUnit()).isEqualTo(8);
        assertThat(minimalTheme.getSpacing().getSectionSpacing()).isEqualTo("6rem");

        // Given - MODERN theme
        LogoAnalysis modernAnalysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .build();

        // When
        ThemeConfig modernTheme = themeGenerationService.generateThemeConfig(modernAnalysis);

        // Then - Modern should use 4px unit
        assertThat(modernTheme.getSpacing().getUnit()).isEqualTo(4);
        assertThat(modernTheme.getSpacing().getSectionSpacing()).isEqualTo("4rem");
    }

    @Test
    @DisplayName("generateThemeConfig should configure layout based on theme")
    void generateThemeConfigShouldConfigureLayoutBasedOnTheme() {
        // Given - PLAYFUL theme
        LogoAnalysis playfulAnalysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("PLAYFUL")
                .build();

        // When
        ThemeConfig playfulTheme = themeGenerationService.generateThemeConfig(playfulAnalysis);

        // Then - Playful should use larger border radius
        assertThat(playfulTheme.getLayout().getBorderRadius().getSm()).isEqualTo("0.5rem");
        assertThat(playfulTheme.getLayout().getBorderRadius().getBase()).isEqualTo("0.75rem");
        assertThat(playfulTheme.getLayout().getBorderRadius().getLg()).isEqualTo("1rem");

        // Given - MODERN theme
        LogoAnalysis modernAnalysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .build();

        // When
        ThemeConfig modernTheme = themeGenerationService.generateThemeConfig(modernAnalysis);

        // Then - Modern should use smaller border radius
        assertThat(modernTheme.getLayout().getBorderRadius().getSm()).isEqualTo("0.25rem");
        assertThat(modernTheme.getLayout().getBorderRadius().getBase()).isEqualTo("0.5rem");
    }

    @Test
    @DisplayName("generateThemeConfig should include font sizes scale")
    void generateThemeConfigShouldIncludeFontSizesScale() {
        // Given
        LogoAnalysis analysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .build();

        // When
        ThemeConfig themeConfig = themeGenerationService.generateThemeConfig(analysis);

        // Then - Verify font size scale
        ThemeConfig.FontSizes fontSizes = themeConfig.getTypography().getFontSizes();
        assertThat(fontSizes).isNotNull();
        assertThat(fontSizes.getXs()).isEqualTo("0.75rem");
        assertThat(fontSizes.getSm()).isEqualTo("0.875rem");
        assertThat(fontSizes.getBase()).isEqualTo("1rem");
        assertThat(fontSizes.getLg()).isEqualTo("1.125rem");
        assertThat(fontSizes.getXl()).isEqualTo("1.25rem");
        assertThat(fontSizes.getXl2()).isEqualTo("1.5rem");
        assertThat(fontSizes.getXl3()).isEqualTo("1.875rem");
        assertThat(fontSizes.getXl4()).isEqualTo("2.25rem");
    }

    @Test
    @DisplayName("generateThemeConfig should include neutral color variants")
    void generateThemeConfigShouldIncludeNeutralColorVariants() {
        // Given
        LogoAnalysis analysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .build();

        // When
        ThemeConfig themeConfig = themeGenerationService.generateThemeConfig(analysis);

        // Then - Verify neutral grays
        ThemeConfig.ColorVariants neutral = themeConfig.getColors().getNeutral();
        assertThat(neutral).isNotNull();
        assertThat(neutral.getShade50()).isEqualTo("#FAFAFA");
        assertThat(neutral.getShade500()).isEqualTo("#737373");
        assertThat(neutral.getShade900()).isEqualTo("#171717");
    }
}
