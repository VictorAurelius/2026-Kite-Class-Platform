package com.kiteclass.core.module.ai.client;

import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stubbed AIClient for tests, dev, and sandbox tenants (GAP-075).
 *
 * <p>Returns deterministic fixture data — no network I/O, instant responses. Becomes
 * the primary when {@code ai-live} profile is NOT active.
 *
 * @since 3.18.0 (Wave 3 Sub-PR 3.2)
 */
@Component("baseAIClient")
@Profile("!ai-live")
@Slf4j
public class MockAIClient implements AIClient {

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        log.debug("[MockAI] analyze audience={} tone={}", request.getAudience(), request.getTone());
        return AnalysisResult.builder()
                .palette(List.of("#2563EB", "#1E40AF", "#EFF6FF"))
                .typographyStyle("sans-serif")
                .moodTags(List.of("professional", "friendly"))
                .build();
    }

    @Override
    public GenerationResult generate(GenerationRequest request) {
        log.debug("[MockAI] generate type={} size={}x{}",
                request.getResourceType(), request.getWidth(), request.getHeight());
        return GenerationResult.builder()
                .imageUrl("mock://ai-generated/" + request.getResourceType().toLowerCase()
                        + "-" + request.getWidth() + "x" + request.getHeight() + ".png")
                .mimeType("image/png")
                .build();
    }
}
