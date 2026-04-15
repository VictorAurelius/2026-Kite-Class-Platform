package com.kiteclass.core.module.ai.client;

import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationRequest;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Resilient decorator (Decorator pattern) wrapping the active {@link AIClient} provider
 * with Circuit Breaker + Bulkhead + Retry + fallback, per ADR-008.
 *
 * <p>Config name {@code ai} (see {@code application.yml}). Fallback returns a
 * template-only signal so downstream {@code ResourceRoutingService} routes to the
 * TEMPLATE category — honoring the "template-first" philosophy.
 *
 * <p>Delegate injected by qualifier {@code baseAIClient} — both {@link MockAIClient}
 * and {@link OllamaAIClient} register under that name (gated by profile), so exactly
 * one is active at runtime and the decorator wraps it without self-cycle.
 *
 * <p>{@link Primary} so services auto-wire this instead of bare {@link AIClient} impls.
 *
 * @since 3.18.0 (Wave 3 Sub-PR 3.2, ADR-008)
 */
@Component("resilientAIClient")
@Primary
@Slf4j
public class ResilientAIClient implements AIClient {

    private final AIClient delegate;

    public ResilientAIClient(@Qualifier("baseAIClient") AIClient delegate) {
        this.delegate = delegate;
    }

    @CircuitBreaker(name = "ai", fallbackMethod = "analyzeFallback")
    @Bulkhead(name = "ai")
    @Retry(name = "ai")
    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        return delegate.analyze(request);
    }

    @CircuitBreaker(name = "ai", fallbackMethod = "generateFallback")
    @Bulkhead(name = "ai")
    @Retry(name = "ai")
    @Override
    public GenerationResult generate(GenerationRequest request) {
        return delegate.generate(request);
    }

    @SuppressWarnings("unused")
    private AnalysisResult analyzeFallback(AnalysisRequest request, Throwable cause) {
        log.warn("[resilience] AI analyze fallback → template-only. cause={}",
                cause.getClass().getSimpleName() + ": " + cause.getMessage());
        return AnalysisResult.templateOnly();
    }

    @SuppressWarnings("unused")
    private GenerationResult generateFallback(GenerationRequest request, Throwable cause) {
        log.warn("[resilience] AI generate fallback → template-only. cause={}",
                cause.getClass().getSimpleName() + ": " + cause.getMessage());
        return GenerationResult.templateFallback();
    }
}
