package com.kitehub.branding.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OllamaClient.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
class OllamaClientTest {

    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        ollamaClient = new OllamaClient(
                "http://localhost:11434",
                "llama3.1:8b",
                "llava:13b",
                120,
                new ObjectMapper()
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
}
