package com.kitehub.branding.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.dto.LogoAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client for Ollama local AI API.
 * Uses Ollama's OpenAI-compatible API format.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
public class OllamaClient implements AIClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String textModel;
    private final String visionModel;
    private final int timeoutSeconds;

    /**
     * Create OllamaClient.
     *
     * @param baseUrl Ollama API base URL (e.g., http://kitehub-ollama:11434)
     * @param textModel Text model name (e.g., llama3.1:8b)
     * @param visionModel Vision model name (e.g., llava:13b)
     * @param timeoutSeconds Request timeout
     * @param objectMapper Jackson ObjectMapper
     */
    public OllamaClient(String baseUrl, String textModel, String visionModel,
                         int timeoutSeconds, ObjectMapper objectMapper) {
        this.textModel = textModel;
        this.visionModel = visionModel;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
        log.info("OllamaClient initialized: baseUrl={}, textModel={}, visionModel={}",
                baseUrl, textModel, visionModel);
    }

    @Override
    public Mono<LogoAnalysis> analyzeLogo(String imageUrl, String organizationName) {
        log.info("[Ollama] Analyzing logo for: {} (model: {})", organizationName, visionModel);

        String prompt = buildLogoAnalysisPrompt(organizationName);

        // Ollama /api/chat with images
        Map<String, Object> requestBody = Map.of(
                "model", visionModel,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt,
                                "images", List.of(imageUrl)
                        )
                ),
                "stream", false,
                "format", "json"
        );

        return webClient.post()
                .uri("/api/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::parseOllamaChatResponse)
                .flatMap(content -> parseLogoAnalysis(content, organizationName))
                .doOnSuccess(a -> log.info("[Ollama] Logo analysis completed: theme={}", a.getTheme()))
                .doOnError(e -> log.error("[Ollama] Logo analysis failed: {}", e.getMessage()));
    }

    @Override
    public Mono<String> generateImage(String prompt, String size) {
        // Ollama does not support image generation.
        // Return placeholder - image generation uses DALL-E in prod or mock in dev.
        log.info("[Ollama] Image generation not supported, returning placeholder");
        return Mono.just("https://placehold.co/1792x1024/2196F3/white?text=" +
                java.net.URLEncoder.encode("KiteClass AI Branding", java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public Mono<String> generateText(String prompt) {
        log.info("[Ollama] Generating text (model: {})", textModel);

        Map<String, Object> requestBody = Map.of(
                "model", textModel,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a professional Vietnamese marketing copywriter for education centers."),
                        Map.of("role", "user", "content", prompt)
                ),
                "stream", false
        );

        return webClient.post()
                .uri("/api/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::parseOllamaChatResponse)
                .doOnSuccess(text -> log.info("[Ollama] Text generated: {} chars", text.length()))
                .doOnError(e -> log.error("[Ollama] Text generation failed: {}", e.getMessage()));
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    /**
     * Extract content from Ollama /api/chat response.
     */
    @SuppressWarnings("unchecked")
    private String parseOllamaChatResponse(Map<String, Object> response) {
        Map<String, Object> message = (Map<String, Object>) response.get("message");
        if (message == null) {
            throw new RuntimeException("Ollama response missing 'message' field");
        }
        return (String) message.get("content");
    }

    /**
     * Parse logo analysis JSON from Ollama text response.
     */
    private Mono<LogoAnalysis> parseLogoAnalysis(String content, String organizationName) {
        try {
            String jsonContent = extractJson(content);
            LogoAnalysis analysis = objectMapper.readValue(jsonContent, LogoAnalysis.class);
            analysis.setRawAnalysis(content);
            return Mono.just(analysis);
        } catch (JsonProcessingException e) {
            log.warn("[Ollama] Failed to parse logo analysis JSON, using fallback: {}", e.getMessage());
            return Mono.just(LogoAnalysis.builder()
                    .primaryColor("#2196F3")
                    .secondaryColor("#FF5722")
                    .accentColor("#4CAF50")
                    .theme("MODERN")
                    .typography("Clean Sans-Serif")
                    .targetAudience("Students and parents seeking quality education")
                    .brandPersonality(List.of("Trustworthy", "Innovative", "Approachable"))
                    .rawAnalysis("Ollama response (JSON parsing fallback): " + content)
                    .build());
        }
    }

    private String extractJson(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

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
}
