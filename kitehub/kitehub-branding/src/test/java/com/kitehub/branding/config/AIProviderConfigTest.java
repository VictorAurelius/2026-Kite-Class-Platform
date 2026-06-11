package com.kitehub.branding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.client.AIClient;
import com.kitehub.branding.client.OllamaClient;
import com.kitehub.branding.client.OpenAIClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for AIProviderConfig.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AIProviderConfigTest {

    @Mock
    private OpenAIClient openAIClient;

    @Test
    @DisplayName("provider=openai returns OpenAIClient")
    void openaiProvider() {
        when(openAIClient.getProviderName()).thenReturn("openai-mock");

        AIProviderConfig config = new AIProviderConfig();
        config.setProvider("openai");

        AIClient client = config.aiClient(openAIClient, new ObjectMapper());
        assertThat(client).isInstanceOf(OpenAIClient.class);
    }

    @Test
    @DisplayName("provider=ollama returns OllamaClient")
    void ollamaProvider() {
        AIProviderConfig config = new AIProviderConfig();
        config.setProvider("ollama");

        AIProviderConfig.Ollama ollamaConfig = new AIProviderConfig.Ollama();
        ollamaConfig.setBaseUrl("http://localhost:11434");
        ollamaConfig.setTextModel("llama3.1:8b");
        ollamaConfig.setVisionModel("llava:13b");
        ollamaConfig.setTimeoutSeconds(120);
        config.setOllama(ollamaConfig);

        AIClient client = config.aiClient(openAIClient, new ObjectMapper());
        assertThat(client).isInstanceOf(OllamaClient.class);
        assertThat(client.getProviderName()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("default provider is openai")
    void defaultProvider() {
        AIProviderConfig config = new AIProviderConfig();
        assertThat(config.getProvider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("provider=gemini returns GeminiClient (mock mode, no key)")
    void geminiProvider() {
        AIProviderConfig config = new AIProviderConfig();
        config.setProvider("gemini");

        // Default Gemini config has an empty api-key → GeminiClient MOCK mode.
        AIClient client = config.aiClient(openAIClient, new ObjectMapper());

        assertThat(client).isInstanceOf(com.kitehub.branding.client.GeminiClient.class);
        assertThat(client.getProviderName()).isEqualTo("gemini-mock");
    }
}
