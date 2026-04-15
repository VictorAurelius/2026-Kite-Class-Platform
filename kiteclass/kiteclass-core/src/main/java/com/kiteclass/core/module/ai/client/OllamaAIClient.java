package com.kiteclass.core.module.ai.client;

import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adapter for local Ollama daemon. Activated only under {@code ai-live} profile.
 *
 * <p>This is the thin-wrapper scaffold — actual HTTP integration (WebClient calls to
 * {@code /api/generate} endpoint, model routing, image multipart handling) lands in
 * Sub-PR 3.5 (AI Agent workflow) where the concrete prompts live. Keeping this class
 * here lets the Resilience4j decorator + Spring wiring be exercised now.
 *
 * <p>Designed to throw {@link AIException} (retryable) on network / 5xx, and
 * {@link NonRetryableAIException} on 4xx / schema violations.
 *
 * @since 3.18.0 (Wave 3 Sub-PR 3.2)
 */
@Component("baseAIClient")
@Profile("ai-live")
@Slf4j
public class OllamaAIClient implements AIClient {

    private final String baseUrl;
    private final String defaultModel;

    public OllamaAIClient(
            @Value("${ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ai.ollama.default-model:gemma2}") String defaultModel) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        log.info("OllamaAIClient wired url={} model={}", baseUrl, defaultModel);
    }

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        throw new AIException(
                "OllamaAIClient.analyze not implemented yet — "
                        + "full integration in Wave 3 Sub-PR 3.5 (AI Agent workflow).");
    }

    @Override
    public GenerationResult generate(GenerationRequest request) {
        throw new AIException(
                "OllamaAIClient.generate not implemented yet — "
                        + "full integration in Wave 3 Sub-PR 3.5 (AI Agent workflow).");
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String defaultModel() {
        return defaultModel;
    }
}
