package com.kitehub.branding.client;

import com.kitehub.branding.config.OpenAIConfig;
import com.kitehub.branding.dto.LogoAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client for OpenAI API integration.
 * Supports GPT-4 Vision, DALL-E 3, and GPT-4 Turbo.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIClient {

    private final WebClient openAIWebClient;
    private final OpenAIConfig openAIConfig;

    /**
     * Analyze logo using GPT-4 Vision.
     * Extracts colors, theme, typography, target audience, and brand personality.
     *
     * @param imageUrl Logo image URL (must be publicly accessible)
     * @param organizationName Organization name for context
     * @return Logo analysis result
     */
    public Mono<LogoAnalysis> analyzeLogo(String imageUrl, String organizationName) {
        log.info("Analyzing logo for organization: {}", organizationName);

        String prompt = buildLogoAnalysisPrompt(organizationName);

        Map<String, Object> requestBody = Map.of(
            "model", openAIConfig.getModels().getVision(),
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
                    )
                )
            ),
            "max_tokens", 1000
        );

        return openAIWebClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(openAIConfig.getTimeout().getSeconds()))
            .map(this::parseLogoAnalysisResponse)
            .doOnSuccess(analysis -> log.info("Logo analysis completed: {}", analysis.getTheme()))
            .doOnError(error -> log.error("Failed to analyze logo: {}", error.getMessage()));
    }

    /**
     * Generate image using DALL-E 3.
     *
     * @param prompt Image generation prompt
     * @param size Image size (1024x1024, 1792x1024, 1024x1792)
     * @return Generated image URL
     */
    public Mono<String> generateImage(String prompt, String size) {
        log.info("Generating image with DALL-E 3: {}", prompt);

        Map<String, Object> requestBody = Map.of(
            "model", openAIConfig.getModels().getDalle(),
            "prompt", prompt,
            "n", 1,
            "size", size,
            "quality", "standard"
        );

        return openAIWebClient.post()
            .uri("/images/generations")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(openAIConfig.getTimeout().getSeconds()))
            .map(this::parseImageGenerationResponse)
            .doOnSuccess(imageUrl -> log.info("Image generated: {}", imageUrl))
            .doOnError(error -> log.error("Failed to generate image: {}", error.getMessage()));
    }

    /**
     * Generate marketing copy using GPT-4 Turbo.
     *
     * @param prompt Text generation prompt
     * @return Generated marketing copy
     */
    public Mono<String> generateText(String prompt) {
        log.info("Generating marketing copy with GPT-4 Turbo");

        Map<String, Object> requestBody = Map.of(
            "model", openAIConfig.getModels().getText(),
            "messages", List.of(
                Map.of("role", "system", "content", "You are a professional marketing copywriter."),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 500,
            "temperature", 0.7
        );

        return openAIWebClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(openAIConfig.getTimeout().getSeconds()))
            .map(this::parseTextGenerationResponse)
            .doOnSuccess(text -> log.info("Marketing copy generated: {} chars", text.length()))
            .doOnError(error -> log.error("Failed to generate text: {}", error.getMessage()));
    }

    /**
     * Build logo analysis prompt.
     *
     * @param organizationName Organization name
     * @return Formatted prompt
     */
    private String buildLogoAnalysisPrompt(String organizationName) {
        return String.format("""
            Analyze this logo for %s. Extract the following information in JSON format:
            1. primaryColors: List of primary colors (hex codes)
            2. secondaryColors: List of secondary colors (hex codes)
            3. theme: Design theme (modern/traditional/playful/professional)
            4. typography: Typography style description
            5. targetAudience: Target audience description
            6. brandPersonality: List of brand personality traits

            Return ONLY valid JSON with these fields.
            """, organizationName);
    }

    /**
     * Parse logo analysis response from GPT-4 Vision.
     *
     * @param response API response
     * @return Parsed LogoAnalysis
     */
    @SuppressWarnings("unchecked")
    private LogoAnalysis parseLogoAnalysisResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            log.debug("Raw GPT-4 Vision response: {}", content);

            // TODO: Parse JSON from content (implement JSON parsing logic)
            // For MVP: Return mock data
            return LogoAnalysis.builder()
                .primaryColors(List.of("#FF5733", "#33FF57"))
                .secondaryColors(List.of("#3357FF", "#F3FF33"))
                .theme("modern")
                .typography("sans-serif, clean")
                .targetAudience("students and educators")
                .brandPersonality(List.of("innovative", "friendly", "professional"))
                .rawAnalysis(content)
                .build();
        } catch (Exception e) {
            log.error("Failed to parse logo analysis response", e);
            throw new RuntimeException("Failed to parse GPT-4 Vision response", e);
        }
    }

    /**
     * Parse image generation response from DALL-E 3.
     *
     * @param response API response
     * @return Generated image URL
     */
    @SuppressWarnings("unchecked")
    private String parseImageGenerationResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            return (String) data.get(0).get("url");
        } catch (Exception e) {
            log.error("Failed to parse image generation response", e);
            throw new RuntimeException("Failed to parse DALL-E 3 response", e);
        }
    }

    /**
     * Parse text generation response from GPT-4 Turbo.
     *
     * @param response API response
     * @return Generated text
     */
    @SuppressWarnings("unchecked")
    private String parseTextGenerationResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("Failed to parse text generation response", e);
            throw new RuntimeException("Failed to parse GPT-4 Turbo response", e);
        }
    }
}
