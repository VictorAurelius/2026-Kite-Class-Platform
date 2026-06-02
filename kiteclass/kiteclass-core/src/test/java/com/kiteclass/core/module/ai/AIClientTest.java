package com.kiteclass.core.module.ai;

import com.kiteclass.core.module.ai.client.GeminiAIClient;
import com.kiteclass.core.module.ai.client.OpenAIAIClient;
import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Phase 1 external AI provider scaffolds (GAP-867 Phase 1).
 *
 * <p>Verifies that {@link GeminiAIClient} + {@link OpenAIAIClient} satisfy the
 * {@code AIClient} interface contract per ADR-038 §2.2:
 * <ul>
 *   <li>Domain types neutral (no provider-specific leakage)</li>
 *   <li>{@code analyze()} returns {@link AnalysisResult} with palette + typography + moodTags</li>
 *   <li>{@code generate()} returns {@link GenerationResult} with imageUrl + mimeType</li>
 *   <li>Config-driven model + timeout fields exposed via accessors</li>
 * </ul>
 *
 * <p>Note: these are Phase 1 SCAFFOLD tests — no live API call. Live integration
 * tests (Testcontainers + WireMock vendor stub) deferred to Phase 2 follow-up gap
 * per ADR-038 §5 Implementation Roadmap.
 *
 * @since Wave local-doable-9 (GAP-867 Phase 1 scaffold)
 */
class AIClientTest {

    private static final String DEFAULT_GEMINI_KEY = "";
    private static final String DEFAULT_OPENAI_KEY = "";

    @Test
    void gemini_analyze_returns_neutral_domain_result() {
        GeminiAIClient client = new GeminiAIClient(
                DEFAULT_GEMINI_KEY, "gemini-1.5-flash", "gemini-1.5-pro", 30000, 2);

        AnalysisRequest req = AnalysisRequest.builder()
                .audience("K-12")
                .tone("professional")
                .build();

        AnalysisResult r = client.analyze(req);

        assertThat(r).isNotNull();
        assertThat(r.getPalette()).isNotEmpty();
        assertThat(r.getPalette().get(0)).matches("^#[0-9A-Fa-f]{6}$");
        assertThat(r.getTypographyStyle()).isNotBlank();
        assertThat(r.getMoodTags()).isNotEmpty();
        assertThat(r.isTemplateOnly()).isFalse();
    }

    @Test
    void gemini_generate_returns_neutral_domain_result() {
        GeminiAIClient client = new GeminiAIClient(
                DEFAULT_GEMINI_KEY, "gemini-1.5-flash", "gemini-1.5-pro", 30000, 2);

        GenerationRequest req = GenerationRequest.builder()
                .prompt("modern banner for tech center")
                .resourceType("BANNER")
                .width(1920).height(600)
                .build();

        GenerationResult r = client.generate(req);

        assertThat(r).isNotNull();
        assertThat(r.getImageUrl()).contains("banner").contains("1920x600");
        assertThat(r.getMimeType()).isEqualTo("image/png");
        assertThat(r.isTemplateFallback()).isFalse();
    }

    @Test
    void gemini_exposes_config_accessors() {
        GeminiAIClient client = new GeminiAIClient(
                DEFAULT_GEMINI_KEY, "gemini-1.5-flash", "gemini-1.5-pro", 25000, 3);

        assertThat(client.modelText()).isEqualTo("gemini-1.5-flash");
        assertThat(client.modelQuality()).isEqualTo("gemini-1.5-pro");
        assertThat(client.timeoutMs()).isEqualTo(25000);
        assertThat(client.maxRetries()).isEqualTo(3);
    }

    @Test
    void openai_analyze_returns_neutral_domain_result() {
        OpenAIAIClient client = new OpenAIAIClient(
                DEFAULT_OPENAI_KEY, "gpt-4o-mini", "gpt-image-1", 60000, 1);

        AnalysisRequest req = AnalysisRequest.builder()
                .audience("university")
                .tone("energetic")
                .build();

        AnalysisResult r = client.analyze(req);

        assertThat(r).isNotNull();
        assertThat(r.getPalette()).isNotEmpty();
        assertThat(r.getPalette().get(0)).matches("^#[0-9A-Fa-f]{6}$");
        assertThat(r.getTypographyStyle()).isNotBlank();
        assertThat(r.getMoodTags()).isNotEmpty();
        assertThat(r.isTemplateOnly()).isFalse();
    }

    @Test
    void openai_generate_returns_neutral_domain_result() {
        OpenAIAIClient client = new OpenAIAIClient(
                DEFAULT_OPENAI_KEY, "gpt-4o-mini", "gpt-image-1", 60000, 1);

        GenerationRequest req = GenerationRequest.builder()
                .prompt("logo for K-12 school")
                .resourceType("LOGO")
                .width(512).height(512)
                .build();

        GenerationResult r = client.generate(req);

        assertThat(r).isNotNull();
        assertThat(r.getImageUrl()).contains("logo").contains("512x512");
        assertThat(r.getMimeType()).isEqualTo("image/png");
        assertThat(r.isTemplateFallback()).isFalse();
    }

    @Test
    void openai_exposes_config_accessors() {
        OpenAIAIClient client = new OpenAIAIClient(
                DEFAULT_OPENAI_KEY, "gpt-4o-mini", "gpt-image-1", 45000, 2);

        assertThat(client.modelText()).isEqualTo("gpt-4o-mini");
        assertThat(client.modelImage()).isEqualTo("gpt-image-1");
        assertThat(client.timeoutMs()).isEqualTo(45000);
        assertThat(client.maxRetries()).isEqualTo(2);
    }
}
