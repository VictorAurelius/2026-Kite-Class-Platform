package com.kiteclass.core.module.ai;

import com.kiteclass.core.module.ai.client.MockAIClient;
import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockAIClientTest {

    private final MockAIClient client = new MockAIClient();

    @Test
    void analyze_returns_deterministic_fixture() {
        AnalysisRequest req = AnalysisRequest.builder()
                .audience("K-12")
                .tone("friendly")
                .build();

        AnalysisResult r = client.analyze(req);

        assertThat(r.getPalette()).hasSize(3);
        assertThat(r.getPalette().get(0)).matches("^#[0-9A-Fa-f]{6}$");
        assertThat(r.getTypographyStyle()).isEqualTo("sans-serif");
        assertThat(r.getMoodTags()).contains("professional", "friendly");
        assertThat(r.isTemplateOnly()).isFalse();
    }

    @Test
    void generate_returns_mock_url() {
        GenerationRequest req = GenerationRequest.builder()
                .prompt("banner for K-12 school")
                .resourceType("BANNER")
                .width(1920).height(600)
                .build();

        GenerationResult r = client.generate(req);

        assertThat(r.getImageUrl()).startsWith("mock://ai-generated/banner-1920x600");
        assertThat(r.getMimeType()).isEqualTo("image/png");
        assertThat(r.isTemplateFallback()).isFalse();
    }
}
