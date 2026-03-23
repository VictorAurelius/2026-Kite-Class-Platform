package com.kitehub.branding.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.dto.ThemeConfig;
import com.kitehub.branding.service.ThemeGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for branding flow.
 * Tests: logo analysis → theme generation pipeline.
 *
 * @since 1.1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Branding Flow IT")
class BrandingFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ThemeGenerationService themeGenerationService;

    @Test
    @DisplayName("Generate theme from logo analysis")
    void generateThemeFromLogoAnalysis() throws Exception {
        LogoAnalysis analysis = new LogoAnalysis();
        analysis.setOrganizationName("Test School");
        analysis.setIndustry("education");
        analysis.setStyle("modern");
        analysis.setPrimaryColor("#2563EB");
        analysis.setSecondaryColor("#1E40AF");
        analysis.setAccentColor("#F59E0B");

        MvcResult result = mockMvc.perform(post("/api/platform/branding/ai/generate-theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(analysis)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.colors").exists())
            .andExpect(jsonPath("$.colors.primary").exists())
            .andReturn();

        ThemeConfig theme = objectMapper.readValue(
            result.getResponse().getContentAsString(), ThemeConfig.class);
        assertThat(theme.getColors()).isNotNull();
        assertThat(theme.getColors().getPrimary()).isNotBlank();
    }

    @Test
    @DisplayName("Theme generation service produces valid config")
    void themeGenerationServiceProducesValidConfig() {
        LogoAnalysis analysis = new LogoAnalysis();
        analysis.setOrganizationName("Academy Pro");
        analysis.setIndustry("education");
        analysis.setStyle("professional");
        analysis.setPrimaryColor("#059669");
        analysis.setSecondaryColor("#047857");
        analysis.setAccentColor("#D97706");

        ThemeConfig config = themeGenerationService.generateThemeConfig(analysis);

        assertThat(config).isNotNull();
        assertThat(config.getColors()).isNotNull();
        assertThat(config.getColors().getPrimary()).isNotBlank();
        assertThat(config.getTypography()).isNotNull();
    }
}
