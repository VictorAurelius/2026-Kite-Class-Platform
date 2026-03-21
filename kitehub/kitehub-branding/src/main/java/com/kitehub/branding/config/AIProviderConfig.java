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
import org.springframework.context.annotation.Primary;

/**
 * AI provider configuration.
 * Selects between OpenAI (cloud) and Ollama (local) based on ai.provider property.
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

    @Data
    public static class Ollama {
        private String baseUrl = "http://kitehub-ollama:11434";
        private String textModel = "llama3.1:8b";
        private String visionModel = "llava:13b";
        private int timeoutSeconds = 120;
    }

    /**
     * Create the primary AIClient bean based on configured provider.
     *
     * @param openAIClient OpenAI client (always available as fallback)
     * @param objectMapper Jackson ObjectMapper
     * @return Selected AIClient implementation
     */
    @Bean
    @Primary
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

        log.info("AI Provider: OpenAI (cloud) [provider={}]", openAIClient.getProviderName());
        return openAIClient;
    }
}
