package com.kitehub.branding.client;

import com.kitehub.branding.dto.LogoAnalysis;
import reactor.core.publisher.Mono;

/**
 * Abstraction for AI provider clients.
 * Implementations: OpenAI (cloud), Ollama (local).
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public interface AIClient {

    /**
     * Analyze logo and extract brand identity.
     *
     * @param imageUrl Logo image URL
     * @param organizationName Organization name for context
     * @return Logo analysis result
     */
    Mono<LogoAnalysis> analyzeLogo(String imageUrl, String organizationName);

    /**
     * Generate image from prompt.
     *
     * @param prompt Image generation prompt
     * @param size Image size (e.g., 1792x1024)
     * @return Generated image URL
     */
    Mono<String> generateImage(String prompt, String size);

    /**
     * Generate text from prompt.
     *
     * @param prompt Text generation prompt
     * @return Generated text
     */
    Mono<String> generateText(String prompt);

    /**
     * Get the provider name for logging.
     *
     * @return Provider name (e.g., "openai", "ollama", "mock")
     */
    String getProviderName();
}
