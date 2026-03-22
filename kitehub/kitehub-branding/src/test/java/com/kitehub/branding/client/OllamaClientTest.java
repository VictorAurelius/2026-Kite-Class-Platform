package com.kitehub.branding.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.dto.LogoAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OllamaClient.
 * Tests verify that color types are single strings (not arrays) to match frontend expectations.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
class OllamaClientTest {

    private OllamaClient ollamaClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ollamaClient = new OllamaClient(
                "http://localhost:11434",
                "llama3.1:8b",
                "llava:13b",
                120,
                objectMapper
        );
    }

    @Test
    @DisplayName("getProviderName returns ollama")
    void getProviderName() {
        assertThat(ollamaClient.getProviderName()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("generateImage returns placeholder (Ollama does not support image gen)")
    void generateImageReturnsPlaceholder() {
        String result = ollamaClient.generateImage("test prompt", "1792x1024").block();
        assertThat(result).contains("placehold.co");
    }

    @Test
    @DisplayName("LogoAnalysis DTO uses single color strings, not arrays")
    void logoAnalysisDtoUsesStringColors() {
        LogoAnalysis analysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .typography("Clean Sans-Serif")
                .targetAudience("Students and parents")
                .build();

        // Verify fields are String type, not List<String>
        assertThat(analysis.getPrimaryColor()).isInstanceOf(String.class);
        assertThat(analysis.getSecondaryColor()).isInstanceOf(String.class);
        assertThat(analysis.getAccentColor()).isInstanceOf(String.class);

        // Verify values are hex codes
        assertThat(analysis.getPrimaryColor()).startsWith("#");
        assertThat(analysis.getSecondaryColor()).startsWith("#");
        assertThat(analysis.getAccentColor()).startsWith("#");

        // Verify theme is uppercase enum value
        assertThat(analysis.getTheme()).isIn("MODERN", "CLASSIC", "PLAYFUL", "MINIMAL");
    }

    @Test
    @DisplayName("LogoAnalysis can be serialized to JSON with correct field names")
    void logoAnalysisSerializesToJson() throws Exception {
        LogoAnalysis analysis = LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .build();

        String json = objectMapper.writeValueAsString(analysis);

        // Verify JSON contains correct field names (not primaryColors, secondaryColors)
        assertThat(json).contains("\"primaryColor\"");
        assertThat(json).contains("\"secondaryColor\"");
        assertThat(json).contains("\"accentColor\"");
        assertThat(json).doesNotContain("\"primaryColors\"");
        assertThat(json).doesNotContain("\"secondaryColors\"");
    }
}
