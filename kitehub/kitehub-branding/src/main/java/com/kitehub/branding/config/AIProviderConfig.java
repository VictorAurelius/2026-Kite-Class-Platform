package com.kitehub.branding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.client.AIClient;
import com.kitehub.branding.client.OllamaClient;
import com.kitehub.branding.client.OpenAIClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI provider configuration.
 * Selects between OpenAI (cloud) and Ollama (local) based on ai.provider property.
 *
 * <p>GAP-148 / Wave 9-D: the bean is named {@code aiClient} and NO longer
 * {@code @Primary}. The primary {@link AIClient} bean is now
 * {@link com.kitehub.branding.client.ResilientAIClient} which wraps this
 * provider with Resilience4j circuit-breaker protection (BR-QUEUE-015..018).
 * The resilient wrapper injects this bean via {@code @Qualifier("aiClient")}.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIProviderConfig {

    private String provider = "openai";

    private Ollama ollama = new Ollama();

    private Gemini gemini = new Gemini();

    @Data
    public static class Ollama {
        private String baseUrl = "http://kitehub-ollama:11434";
        private String textModel = "llama3.1:8b";
        private String visionModel = "llava:13b";
        private int timeoutSeconds = 120;
    }

    /**
     * Google Gemini free-tier provider config (GAP-1135 / ADR-037 Amendment).
     * {@code apiKey} blank/absent → {@code GeminiClient} MOCK mode (graceful no-key).
     */
    @Data
    public static class Gemini {
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private String apiKey = "";
        private String textModel = "gemini-flash-latest";
        private String visionModel = "gemini-flash-latest";
        private int timeoutSeconds = 60;
    }

    /**
     * Create the underlying AIClient bean based on configured provider.
     * Wrapped by {@code ResilientAIClient} (the actual {@code @Primary} bean) —
     * callers should NOT inject this directly; use {@link AIClient} and Spring
     * will supply the resilient wrapper.
     *
     * @param openAIClient OpenAI client (always available as fallback)
     * @param objectMapper Jackson ObjectMapper
     * @return Selected AIClient implementation
     */
    @Bean(name = "aiClient")
    public AIClient aiClient(OpenAIClient openAIClient, ObjectMapper objectMapper) {
        if ("ollama".equalsIgnoreCase(provider)) {
            log.info("AI Provider: Ollama (local) at {}", ollama.getBaseUrl());
            log.info("  Text model: {}", ollama.getTextModel());
            log.info("  Vision model: {}", ollama.getVisionModel());
            return new OllamaClient(
                    ollama.getBaseUrl(),
                    ollama.getTextModel(),
                    ollama.getVisionModel(),
                    ollama.getTimeoutSeconds(),
                    objectMapper
            );
        }

        if ("gemini".equalsIgnoreCase(provider)) {
            // GAP-1135 / ADR-037: Gemini free-tier text/HTML copy provider.
            com.kitehub.branding.client.GeminiClient geminiClient =
                    new com.kitehub.branding.client.GeminiClient(
                            gemini.getBaseUrl(),
                            gemini.getApiKey(),
                            gemini.getTextModel(),
                            gemini.getVisionModel(),
                            gemini.getTimeoutSeconds(),
                            objectMapper);
            log.info("AI Provider: Gemini (free-tier) [provider={}]", geminiClient.getProviderName());
            return geminiClient;
        }

        log.info("AI Provider: OpenAI (cloud) [provider={}]", openAIClient.getProviderName());
        return openAIClient;
    }
}
