package com.kiteclass.core.module.ai.client;

import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy Pattern + Adapter Pattern — OpenAI fallback provider for {@link AIClient}.
 *
 * <p>Per ADR-038 §2.1 (OpenAI as fallback when Gemini Circuit Breaker OPEN or
 * rate-limit hit) + ADR-037 (OpenAI {@code gpt-image-1} for banner image generation).
 * Activated under {@code ai-external} profile when {@code ai.provider.primary=openai}
 * is set.
 *
 * <p><b>Phase 1 scaffold (GAP-867 Phase 1)</b> — this class returns mock-shaped
 * domain results. Live HTTP integration with OpenAI API ({@code api.openai.com})
 * lands in Phase 2 follow-up gap per ADR-038 §5 Implementation Roadmap.
 *
 * <p>Registered with qualifier {@code baseAIClient} (same as {@link GeminiAIClient}
 * but gated by {@code primary=openai}) so {@link ResilientAIClient} decorator wraps
 * it transparently. Adheres to {@code design-patterns.md} §3.10 (no leaky abstraction
 * — domain types only, no {@code OpenAIResponse} leakage).
 *
 * <p><b>Out of scope this phase</b> (deferred to follow-up implementation gap):
 * <ul>
 *   <li>Live HTTP POST to OpenAI chat completions + image generation APIs</li>
 *   <li>Cost-cap enforcement per ADR-038 §3.2 (daily/per-tenant USD caps)</li>
 *   <li>Failover orchestration (Gemini → OpenAI on Circuit Breaker OPEN — handled by
 *       Resilience4j fallback method, not provider implementation)</li>
 *   <li>DPA signing for paid-tier usage (Phase 1.5+ trigger)</li>
 * </ul>
 *
 * @since Wave local-doable-9 (GAP-867 Phase 1 scaffold)
 * @see com.kiteclass.core.module.ai.client.AIClient
 * @see com.kiteclass.core.module.ai.config.AIClientConfig
 */
@Component("baseAIClient")
@Profile("ai-external")
@ConditionalOnProperty(prefix = "ai.provider", name = "primary", havingValue = "openai", matchIfMissing = false)
@Slf4j
public class OpenAIAIClient implements AIClient {

    private final String apiKey;
    private final String modelText;
    private final String modelImage;
    private final int timeoutMs;
    private final int maxRetries;

    public OpenAIAIClient(
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model-text:gpt-4o-mini}") String modelText,
            @Value("${ai.openai.model-image:gpt-image-1}") String modelImage,
            @Value("${ai.openai.timeout-ms:60000}") int timeoutMs,
            @Value("${ai.openai.max-retries:1}") int maxRetries) {
        this.apiKey = apiKey;
        this.modelText = modelText;
        this.modelImage = modelImage;
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;
        log.info("OpenAIAIClient wired modelText={} modelImage={} timeoutMs={} maxRetries={} apiKeyConfigured={}",
                modelText, modelImage, timeoutMs, maxRetries, !apiKey.isBlank());
    }

    /**
     * Phase 1 scaffold — returns deterministic mock-shaped result. Live integration
     * deferred to Phase 2 follow-up gap.
     */
    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        log.debug("[OpenAI scaffold] analyze audience={} tone={} (mock response — Phase 1)",
                request.getAudience(), request.getTone());
        return AnalysisResult.builder()
                .palette(List.of("#10A37F", "#0E8F70", "#E8F7F2"))
                .typographyStyle("sans-serif")
                .moodTags(List.of("clean", "approachable"))
                .build();
    }

    /**
     * Phase 1 scaffold — returns mock URL. Phase 2 will route image generation
     * through {@code modelImage} (gpt-image-1 per ADR-037).
     */
    @Override
    public GenerationResult generate(GenerationRequest request) {
        log.debug("[OpenAI scaffold] generate type={} size={}x{} (mock response — Phase 1)",
                request.getResourceType(), request.getWidth(), request.getHeight());
        return GenerationResult.builder()
                .imageUrl("openai-scaffold://mock-" + request.getResourceType().toLowerCase()
                        + "-" + request.getWidth() + "x" + request.getHeight() + ".png")
                .mimeType("image/png")
                .build();
    }

    public String modelText() {
        return modelText;
    }

    public String modelImage() {
        return modelImage;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public int maxRetries() {
        return maxRetries;
    }
}
