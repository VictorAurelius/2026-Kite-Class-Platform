package com.kitehub.branding.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OpenAIClient implements AIClient {

    private final WebClient openAIWebClient;
    private final OpenAIConfig openAIConfig;
    private final ObjectMapper objectMapper;

    /**
     * Check if running in mock mode (API key starts with sk-mock or sk-placeholder).
     */
    private boolean isMockMode() {
        String apiKey = openAIConfig.getApi().getKey();
        return apiKey == null || apiKey.startsWith("sk-mock") || apiKey.startsWith("sk-placeholder");
    }

    /**
     * Analyze logo using GPT-4 Vision.
     * Extracts colors, theme, typography, target audience, and brand personality.
     *
     * @param imageUrl Logo image URL (must be publicly accessible)
     * @param organizationName Organization name for context
     * @return Logo analysis result
     */
    public Mono<LogoAnalysis> analyzeLogo(String imageUrl, String organizationName) {
        if (isMockMode()) {
            log.info("[MOCK] Returning sample logo analysis for: {}", organizationName);
            return Mono.just(LogoAnalysis.builder()
                .primaryColor("#2196F3")
                .secondaryColor("#FF5722")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .typography("Clean Sans-Serif")
                .targetAudience("Students and parents seeking quality education")
                .brandPersonality(List.of("Trustworthy", "Innovative", "Approachable"))
                .build());
        }

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
        if (isMockMode()) {
            log.info("[MOCK] Returning placeholder image URL");
            return Mono.just("https://placehold.co/1792x1024/2196F3/white?text=" +
                java.net.URLEncoder.encode("KiteClass Branding", java.nio.charset.StandardCharsets.UTF_8));
        }

        log.info("Generating image with DALL-E 3: {}", prompt);

        // G1 walk 2026-06-12: OpenAI deprecated dall-e-3 cho project keys mới
        // ("The model 'dall-e-3' does not exist") → default model = gpt-image-1.
        // gpt-image-1 KHÔNG nhận "quality":"standard" (chỉ low/medium/high/auto) và
        // trả b64_json thay vì url — request shape per-model, parse cả 2 dạng.
        String model = openAIConfig.getModels().getDalle();
        Map<String, Object> requestBody = model.startsWith("gpt-image")
            ? Map.of("model", model, "prompt", prompt, "n", 1, "size", size)
            : Map.of("model", model, "prompt", prompt, "n", 1, "size", size,
                     "quality", "standard");

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
        if (isMockMode()) {
            log.info("[MOCK] Returning sample marketing copy");
            return Mono.just("Chào mừng đến với trung tâm giáo dục hàng đầu. " +
                "Chúng tôi cung cấp chương trình học chất lượng cao với đội ngũ giảng viên giàu kinh nghiệm.");
        }

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
            1. primaryColor: The main brand color (single hex code, e.g., "#2196F3")
            2. secondaryColor: The secondary brand color (single hex code, e.g., "#FF5722")
            3. accentColor: The accent color for highlights/CTAs (single hex code, e.g., "#4CAF50")
            4. theme: Design theme enum - ONLY use one of: MODERN, CLASSIC, PLAYFUL, MINIMAL
            5. typography: Typography style description (e.g., "Modern Sans-serif")
            6. targetAudience: Target audience description
            7. brandPersonality: Array of brand personality traits (e.g., ["Professional", "Friendly"])

            Return ONLY valid JSON with these exact field names.
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

            // Parse JSON from content using Jackson ObjectMapper
            String jsonContent = extractJson(content);
            LogoAnalysis analysis = objectMapper.readValue(jsonContent, LogoAnalysis.class);

            // Set raw analysis for debugging/audit trail
            analysis.setRawAnalysis(content);

            log.info("Successfully parsed logo analysis: primaryColor={}, theme={}",
                analysis.getPrimaryColor(), analysis.getTheme());

            return analysis;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON from OpenAI response: {}", e.getMessage());

            // Fallback to mock data for development/testing
            log.warn("Using fallback mock data for logo analysis");
            return LogoAnalysis.builder()
                .primaryColor("#FF5733")
                .secondaryColor("#33FF57")
                .accentColor("#3357FF")
                .theme("MODERN")
                .typography("sans-serif, clean")
                .targetAudience("students and educators")
                .brandPersonality(List.of("innovative", "friendly", "professional"))
                .rawAnalysis("Fallback data (JSON parsing failed)")
                .build();
        } catch (Exception e) {
            log.error("Failed to parse logo analysis response", e);
            throw new RuntimeException("Failed to parse GPT-4 Vision response", e);
        }
    }

    /**
     * Extract JSON from content string.
     * OpenAI sometimes returns JSON with markdown code blocks (```json ... ```).
     *
     * @param content Content string potentially containing JSON
     * @return Extracted JSON string
     */
    private String extractJson(String content) {
        // Remove markdown code blocks if present
        String cleaned = content.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove ```json
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove ```
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove trailing ```
        }

        return cleaned.trim();
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
            // dall-e trả url; gpt-image-1 trả b64_json (không có url) → data-URI để caller
            // (BrandingJobV1Controller.generateFullAiBanner) decode + persist MinIO.
            String url = (String) data.get(0).get("url");
            if (url != null) {
                return url;
            }
            String b64 = (String) data.get(0).get("b64_json");
            return b64 == null ? null : "data:image/png;base64," + b64;
        } catch (Exception e) {
            log.error("Failed to parse image generation response", e);
            throw new RuntimeException("Failed to parse DALL-E 3 response", e);
        }
    }

    @Override
    public String getProviderName() {
        return isMockMode() ? "openai-mock" : "openai";
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
