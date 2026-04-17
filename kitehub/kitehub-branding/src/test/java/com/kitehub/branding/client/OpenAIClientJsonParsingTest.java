package com.kitehub.branding.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.config.OpenAIConfig;
import com.kitehub.branding.dto.LogoAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OpenAIClient JSON parsing functionality.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAIClient JSON Parsing Tests")
class OpenAIClientJsonParsingTest {

    @Mock
    private WebClient webClient;

    @Mock
    private OpenAIConfig openAIConfig;

    private OpenAIClient openAIClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        openAIClient = new OpenAIClient(webClient, openAIConfig, objectMapper);
    }

    @Test
    @DisplayName("Should parse valid JSON from OpenAI response")
    void shouldParseValidJsonFromOpenAIResponse() throws Exception {
        // Given
        String validJson = """
            {
              "primaryColor": "#FF5733",
              "secondaryColor": "#33FF57",
              "accentColor": "#3357FF",
              "theme": "MODERN",
              "typography": "sans-serif",
              "targetAudience": "students",
              "brandPersonality": ["innovative", "friendly"]
            }
            """;

        Map<String, Object> openAIResponse = Map.of(
            "choices", List.of(
                Map.of("message", Map.of("content", validJson))
            )
        );

        // When
        LogoAnalysis result = invokeParseLogoAnalysisResponse(openAIResponse);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryColor()).isEqualTo("#FF5733");
        assertThat(result.getSecondaryColor()).isEqualTo("#33FF57");
        assertThat(result.getAccentColor()).isEqualTo("#3357FF");
        assertThat(result.getTheme()).isEqualTo("MODERN");
        assertThat(result.getTypography()).isEqualTo("sans-serif");
        assertThat(result.getTargetAudience()).isEqualTo("students");
        assertThat(result.getBrandPersonality()).containsExactly("innovative", "friendly");
    }

    @Test
    @DisplayName("Should parse JSON with markdown code blocks")
    void shouldParseJsonWithMarkdownCodeBlocks() throws Exception {
        // Given
        String jsonWithMarkdown = """
            ```json
            {
              "primaryColor": "#FF0000",
              "secondaryColor": "#00FF00",
              "accentColor": "#0000FF",
              "theme": "CLASSIC",
              "typography": "serif",
              "targetAudience": "adults",
              "brandPersonality": ["professional"]
            }
            ```
            """;

        Map<String, Object> openAIResponse = Map.of(
            "choices", List.of(
                Map.of("message", Map.of("content", jsonWithMarkdown))
            )
        );

        // When
        LogoAnalysis result = invokeParseLogoAnalysisResponse(openAIResponse);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryColor()).isEqualTo("#FF0000");
        assertThat(result.getSecondaryColor()).isEqualTo("#00FF00");
        assertThat(result.getTheme()).isEqualTo("CLASSIC");
    }

    @Test
    @DisplayName("Should fallback to mock data on invalid JSON")
    void shouldFallbackToMockDataOnInvalidJson() throws Exception {
        // Given
        String invalidJson = "This is not valid JSON at all!";

        Map<String, Object> openAIResponse = Map.of(
            "choices", List.of(
                Map.of("message", Map.of("content", invalidJson))
            )
        );

        // When
        LogoAnalysis result = invokeParseLogoAnalysisResponse(openAIResponse);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRawAnalysis()).contains("Fallback data");
        assertThat(result.getTheme()).isEqualTo("MODERN");
        assertThat(result.getPrimaryColor()).isEqualTo("#FF5733");
        assertThat(result.getSecondaryColor()).isEqualTo("#33FF57");
    }

    @Test
    @DisplayName("Should extract JSON without markdown blocks")
    void shouldExtractJsonWithoutMarkdownBlocks() throws Exception {
        // Given
        String cleanJson = "{\"test\": \"value\"}";

        // When
        String extracted = invokeExtractJson(cleanJson);

        // Then
        assertThat(extracted).isEqualTo("{\"test\": \"value\"}");
    }

    @Test
    @DisplayName("Should extract JSON from markdown with json prefix")
    void shouldExtractJsonFromMarkdownWithJsonPrefix() throws Exception {
        // Given
        String jsonWithPrefix = "```json\n{\"test\": \"value\"}\n```";

        // When
        String extracted = invokeExtractJson(jsonWithPrefix);

        // Then
        assertThat(extracted).isEqualTo("{\"test\": \"value\"}");
    }

    @Test
    @DisplayName("Should extract JSON from markdown without json prefix")
    void shouldExtractJsonFromMarkdownWithoutJsonPrefix() throws Exception {
        // Given
        String jsonWithoutPrefix = "```\n{\"test\": \"value\"}\n```";

        // When
        String extracted = invokeExtractJson(jsonWithoutPrefix);

        // Then
        assertThat(extracted).isEqualTo("{\"test\": \"value\"}");
    }

    /**
     * Helper to invoke private parseLogoAnalysisResponse method via reflection.
     */
    private LogoAnalysis invokeParseLogoAnalysisResponse(Map<String, Object> response) throws Exception {
        Method method = OpenAIClient.class.getDeclaredMethod("parseLogoAnalysisResponse", Map.class);
        method.setAccessible(true);
        return (LogoAnalysis) method.invoke(openAIClient, response);
    }

    /**
     * Helper to invoke private extractJson method via reflection.
     */
    private String invokeExtractJson(String content) throws Exception {
        Method method = OpenAIClient.class.getDeclaredMethod("extractJson", String.class);
        method.setAccessible(true);
        return (String) method.invoke(openAIClient, content);
    }
}
