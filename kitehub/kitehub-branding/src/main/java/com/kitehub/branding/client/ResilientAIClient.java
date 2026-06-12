package com.kitehub.branding.client;

import com.kitehub.branding.dto.LogoAnalysis;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Resilient decorator (Decorator pattern) wrapping the configured {@link AIClient}
 * with Resilience4j circuit-breaker protection — GAP-148 / BR-QUEUE-015..018.
 *
 * <p>Without this wrapper, the {@code resilience4j.circuitbreaker.instances.ai-provider}
 * block in {@code application.yml} was dead config: loaded but unreferenced.
 * Now every AI call in {@code kitehub-branding} flows through this primary bean,
 * honouring the thresholds documented in
 * {@code documents/01-business/kiteclass/ai-agent-workflow/rules.md} §BR-QUEUE-015..018:
 *
 * <ul>
 *   <li>BR-QUEUE-015 — failureRateThreshold = 50% (opens CB)</li>
 *   <li>BR-QUEUE-016 — waitDurationInOpenState = 30s</li>
 *   <li>BR-QUEUE-017 — slidingWindowSize = 20 calls</li>
 *   <li>BR-QUEUE-018 — minimumNumberOfCalls = 10</li>
 * </ul>
 *
 * <p><b>Fallbacks</b> return domain-safe values so downstream pipelines can continue
 * via the template path (aligning with the "template-first" philosophy in
 * {@code ai-branding-guidelines.md} §1). Fallback signatures take the same parameters
 * as the wrapped method plus a trailing {@link Throwable} — Resilience4j selects the
 * method by signature.
 *
 * <p><b>Primary bean:</b> {@code @Primary} so {@code AIBrandingService} and
 * {@code ContentGenerationService} auto-wire this instead of the bare provider.
 * The delegate is injected via qualifier {@code aiClient} — the @Bean defined in
 * {@link com.kitehub.branding.config.AIProviderConfig#aiClient} — avoiding a
 * self-cycle.
 *
 * @since 3.21.0 (GAP-148, Wave 9-D)
 */
@Slf4j
@Component("resilientAIClient")
@Primary
public class ResilientAIClient implements AIClient {

    /** Circuit breaker name — must match the instance in application.yml. */
    public static final String CB_NAME = "ai-provider";

    private final AIClient delegate;

    public ResilientAIClient(@Qualifier("aiClient") AIClient delegate) {
        this.delegate = delegate;
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "analyzeLogoFallback")
    @Override
    public Mono<LogoAnalysis> analyzeLogo(String imageUrl, String organizationName) {
        return delegate.analyzeLogo(imageUrl, organizationName);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "generateImageFallback")
    @Override
    public Mono<String> generateImage(String prompt, String size) {
        return delegate.generateImage(prompt, size);
    }

    /**
     * GAP-1218 / G1 walk 2026-06-12 — STRICT image-gen cho FULL_AI path: KHÔNG fallback
     * placeholder. Fallback nuốt lỗi làm controller tưởng generation thành công →
     * trừ quota + toast "đã trừ 1 lượt" trên banner placehold.co (consumer-trust violation).
     * CB vẫn đếm lỗi (cùng CB_NAME); caller tự catch → fallbackReason GENERATION_FAILED,
     * KHÔNG charge.
     */
    @CircuitBreaker(name = CB_NAME)
    public Mono<String> generateImageStrict(String prompt, String size) {
        return delegate.generateImage(prompt, size);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "generateTextFallback")
    @Override
    public Mono<String> generateText(String prompt) {
        return delegate.generateText(prompt);
    }

    @Override
    public String getProviderName() {
        return "resilient(" + delegate.getProviderName() + ")";
    }

    // ---- Fallbacks -----------------------------------------------------------

    /**
     * Circuit breaker open / wrapped call failed → return a neutral template-only
     * analysis so the caller can continue via the template-first path.
     */
    @SuppressWarnings("unused")
    private Mono<LogoAnalysis> analyzeLogoFallback(String imageUrl, String organizationName,
                                                   Throwable cause) {
        log.warn("[resilience][{}] analyzeLogo fallback → template defaults for org={}, cause={}:{}",
                CB_NAME, organizationName, cause.getClass().getSimpleName(), cause.getMessage());
        return Mono.just(LogoAnalysis.builder()
                .primaryColor("#2563EB")
                .secondaryColor("#64748B")
                .accentColor("#F59E0B")
                .theme("MODERN")
                .typography("Sans-serif")
                .targetAudience("Students and educators")
                .brandPersonality(List.of("Trustworthy", "Approachable"))
                .rawAnalysis("Fallback (circuit breaker open or upstream failure)")
                .build());
    }

    /**
     * Image generation failed / CB open → return a deterministic placeholder URL
     * that the upstream service can replace with a template asset.
     */
    @SuppressWarnings("unused")
    private Mono<String> generateImageFallback(String prompt, String size, Throwable cause) {
        log.warn("[resilience][{}] generateImage fallback → placeholder for size={}, cause={}:{}",
                CB_NAME, size, cause.getClass().getSimpleName(), cause.getMessage());
        return Mono.just("https://placehold.co/" + size + "/2563EB/white?text=Template");
    }

    /**
     * Text generation failed / CB open → return a short Vietnamese default copy
     * so pipelines can finalise without a cascading 5xx.
     */
    @SuppressWarnings("unused")
    private Mono<String> generateTextFallback(String prompt, Throwable cause) {
        log.warn("[resilience][{}] generateText fallback → default copy, cause={}:{}",
                CB_NAME, cause.getClass().getSimpleName(), cause.getMessage());
        return Mono.just("Chào mừng đến với trung tâm giáo dục. Chúng tôi cung cấp chương trình "
                + "học chất lượng cao với đội ngũ giảng viên giàu kinh nghiệm.");
    }
}
