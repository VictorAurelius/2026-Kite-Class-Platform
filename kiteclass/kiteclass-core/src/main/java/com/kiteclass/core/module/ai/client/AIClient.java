package com.kiteclass.core.module.ai.client;

import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;

/**
 * Adapter for external AI providers (Ollama / OpenAI / Bedrock).
 *
 * <p>Wraps vendor SDKs behind domain types only — satisfies {@code design-patterns.md}
 * §3.4 (no direct external coupling) and §3.10 (no leaky abstraction). Domain code
 * MUST depend on this interface, not on a concrete provider.
 *
 * <p>Implementations bind to a Spring profile:
 * <ul>
 *   <li>{@code MockAIClient} — default (no profile), used in tests + sandbox tenants</li>
 *   <li>{@code OllamaAIClient} — profile {@code ai-live}, hits local Ollama daemon</li>
 *   <li>OpenAI / Bedrock — future, gated by enterprise tier</li>
 * </ul>
 *
 * <p>Resilience (Circuit Breaker + Bulkhead + Retry, per ADR-008) is applied by
 * {@code ResilientAIClient} decorator — concrete providers remain unaware.
 *
 * @since 3.18.0 (Wave 3 Sub-PR 3.2, ADR-008)
 */
public interface AIClient {

    AnalysisResult analyze(AnalysisRequest request) throws AIException;

    GenerationResult generate(GenerationRequest request) throws AIException;
}
