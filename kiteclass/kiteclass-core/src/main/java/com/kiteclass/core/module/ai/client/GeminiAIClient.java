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
 * Strategy Pattern + Adapter Pattern — Gemini Free Tier provider for {@link AIClient}.
 *
 * <p>Per ADR-038 §2.1 (provider selection: Gemini Free Tier primary) and §2.2
 * (provider-agnostic AIClient interface). Activated under {@code ai-external} profile
 * when {@code ai.provider.primary=gemini} is set.
 *
 * <p><b>Phase 1 scaffold (GAP-867 Phase 1)</b> — this class returns mock-shaped
 * domain results so the Spring wiring, config binding, and Resilience4j decorator
 * can be exercised end-to-end without burning real Gemini quota or requiring an
 * API key in CI. The live HTTP integration (WebClient → {@code generativelanguage.googleapis.com})
 * lands in Phase 2 follow-up gap per ADR-038 §5 Implementation Roadmap.
 *
 * <p>Registered with qualifier {@code baseAIClient} so {@link ResilientAIClient}
 * (Decorator, {@code @Primary}) wraps it transparently — domain code stays unaware
 * of which provider is active. Adheres to {@code design-patterns.md} §3.10
 * (no leaky abstraction — neutral {@link AnalysisResult} / {@link GenerationResult}
 * domain types, no {@code GeminiResponse} leakage).
 *
 * <p><b>Out of scope this phase</b> (deferred to follow-up implementation gap):
 * <ul>
 *   <li>Live HTTP POST to Gemini generativelanguage API</li>
 *   <li>Real prompt composition + response parsing</li>
 *   <li>Circuit Breaker tuning beyond the inherited {@code ai} instance</li>
 *   <li>Rate-limit handling (free-tier 15 RPM) + retry-after backoff</li>
 *   <li>Cost cap enforcement per {@code ai.cost.cap.daily-usd}</li>
 *   <li>Prompt PII sanitization (regex strip email/phone) per ADR-038 §2.4</li>
 * </ul>
 *
 * @since Wave local-doable-9 (GAP-867 Phase 1 scaffold)
 * @see com.kiteclass.core.module.ai.client.AIClient
 * @see com.kiteclass.core.module.ai.config.AIClientConfig
 */
@Component("baseAIClient")
@Profile("ai-external")
@ConditionalOnProperty(prefix = "ai.provider", name = "primary", havingValue = "gemini", matchIfMissing = false)
@Slf4j
public class GeminiAIClient implements AIClient {

    private final String apiKey;
    private final String modelText;
    private final String modelQuality;
    private final int timeoutMs;
    private final int maxRetries;

    public GeminiAIClient(
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model-text:gemini-1.5-flash}") String modelText,
            @Value("${ai.gemini.model-quality:gemini-1.5-pro}") String modelQuality,
            @Value("${ai.gemini.timeout-ms:30000}") int timeoutMs,
            @Value("${ai.gemini.max-retries:2}") int maxRetries) {
        this.apiKey = apiKey;
        this.modelText = modelText;
        this.modelQuality = modelQuality;
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;
        log.info("GeminiAIClient wired modelText={} modelQuality={} timeoutMs={} maxRetries={} apiKeyConfigured={}",
                modelText, modelQuality, timeoutMs, maxRetries, !apiKey.isBlank());
    }

    /**
     * Phase 1 scaffold — returns deterministic mock-shaped result. Live integration
     * deferred to Phase 2 follow-up gap.
     */
    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        log.debug("[GeminiAI scaffold] analyze audience={} tone={} (mock response — Phase 1)",
                request.getAudience(), request.getTone());
        return AnalysisResult.builder()
                .palette(List.of("#0F62FE", "#0043CE", "#EDF5FF"))
                .typographyStyle("sans-serif")
                .moodTags(List.of("modern", "trustworthy"))
                .build();
    }

    /**
     * Phase 1 scaffold — returns mock URL pointing to gemini-provider-shaped path.
     * Note: Gemini Free Tier does not return image bytes directly (text models only);
     * banner image generation routes to OpenAI {@code gpt-image-1} per ADR-037 + ADR-038.
     * This stub exists để satisfy interface contract; Phase 2 will throw
     * {@link NonRetryableAIException} cho image gen on Gemini provider.
     */
    @Override
    public GenerationResult generate(GenerationRequest request) {
        log.debug("[GeminiAI scaffold] generate type={} size={}x{} (mock response — Phase 1)",
                request.getResourceType(), request.getWidth(), request.getHeight());
        return GenerationResult.builder()
                .imageUrl("gemini-scaffold://mock-" + request.getResourceType().toLowerCase()
                        + "-" + request.getWidth() + "x" + request.getHeight() + ".png")
                .mimeType("image/png")
                .build();
    }

    public String apiKey() {
        return apiKey;
    }

    public String modelText() {
        return modelText;
    }

    public String modelQuality() {
        return modelQuality;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public int maxRetries() {
        return maxRetries;
    }
}
