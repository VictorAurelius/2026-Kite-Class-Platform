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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for branding flow.
 * Tests: logo analysis → theme generation pipeline.
 *
 * <h3>GAP-1044 — auth-migrated + actually executed in CI</h3>
 * <p>The {@code AIBrandingController} AI endpoints ({@code /analyze-logo}, {@code /generate-image},
 * {@code /generate-text}, {@code /generate-theme}) gained {@code @PreAuthorize(OWNER_AUTHZ)} in
 * Wave 101 (GAP-562); these older flow tests hit them anonymously → 403. {@code @WithMockUser(roles
 * = "OWNER")} restores access (no {@code X-Instance-Id} sent, so the ownership guard early-returns;
 * the {@code /templates} list endpoint is unauthenticated and unaffected). Renamed from {@code *IT}
 * → {@code *IntegrationTest} so Spring Boot's default Surefire {@code <includes>} runs it in CI's
 * {@code ./mvnw clean test} (the project ships no maven-failsafe plugin, so {@code *IT} classes were
 * silently never executed).</p>
 *
 * @since 1.1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OWNER")
@DisplayName("Branding Flow IT")
class BrandingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ThemeGenerationService themeGenerationService;

    @Test
    @DisplayName("Generate theme from logo analysis")
    void generateThemeFromLogoAnalysis() throws Exception {
        LogoAnalysis analysis = LogoAnalysis.builder()
            .primaryColor("#2563EB")
            .secondaryColor("#1E40AF")
            .accentColor("#F59E0B")
            .theme("MODERN")
            .typography("Modern Sans-serif")
            .targetAudience("students and teachers")
            .build();

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
        assertThat(theme.getColors().getPrimary()).isNotNull();
    }

    @Test
    @DisplayName("Analyze logo in mock AI mode returns brand colors")
    void analyzeLogoWithMockAI() throws Exception {
        String requestBody = """
            {
                "logoUrl": "https://example.com/logo.png",
                "organizationName": "Test School"
            }
            """;

        MvcResult asyncResult = mockMvc.perform(post("/api/platform/branding/ai/analyze-logo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.primaryColor").exists());
    }

    @Test
    @DisplayName("Generate hero image in mock AI mode returns image URL")
    void generateImageWithMockAI() throws Exception {
        String requestBody = """
            {
                "organizationName": "Test School",
                "theme": "MODERN",
                "colors": "#2563EB,#1E40AF"
            }
            """;

        MvcResult asyncResult = mockMvc.perform(post("/api/platform/branding/ai/generate-image")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imageUrl").exists());
    }

    @Test
    @DisplayName("Generate marketing text in mock AI mode returns non-empty text")
    void generateTextWithMockAI() throws Exception {
        String requestBody = """
            {
                "organizationName": "Test School",
                "theme": "MODERN",
                "targetAudience": "students and teachers"
            }
            """;

        MvcResult asyncResult = mockMvc.perform(post("/api/platform/branding/ai/generate-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").exists());
    }

    @Test
    @DisplayName("List branding templates returns 200")
    void listBrandingTemplatesReturnsOk() throws Exception {
        mockMvc.perform(get("/api/platform/branding/templates"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Theme generation service produces valid config")
    void themeGenerationServiceProducesValidConfig() {
        LogoAnalysis analysis = LogoAnalysis.builder()
            .primaryColor("#059669")
            .secondaryColor("#047857")
            .accentColor("#D97706")
            .theme("CLASSIC")
            .typography("Classic Serif")
            .targetAudience("professionals")
            .build();

        ThemeConfig config = themeGenerationService.generateThemeConfig(analysis);

        assertThat(config).isNotNull();
        assertThat(config.getColors()).isNotNull();
        assertThat(config.getColors().getPrimary()).isNotNull();
        assertThat(config.getTypography()).isNotNull();
    }
}
