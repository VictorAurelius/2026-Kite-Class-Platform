package com.kiteclass.core.module.ai.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration binding for external AI provider strategy (ADR-038).
 *
 * <p>Binds {@code ai.provider.*} + {@code ai.gemini.*} + {@code ai.openai.*} +
 * {@code ai.circuit-breaker.*} properties from {@code application.yml}. Provides
 * config-driven provider selection per ADR-038 §2.3.
 *
 * <p><b>Phase 1 scaffold (GAP-867 Phase 1)</b> — config binding + validation only.
 * The actual provider selection happens via {@code @ConditionalOnProperty} on
 * {@link com.kiteclass.core.module.ai.client.GeminiAIClient} and
 * {@link com.kiteclass.core.module.ai.client.OpenAIAIClient}; this class centralizes
 * type-safe access to provider settings for downstream consumers (cost cap
 * enforcement, observability metrics — both Phase 2/3 scope).
 *
 * <p>Active only under {@code ai-external} profile to avoid binding overhead in
 * test + dev profiles where {@code MockAIClient} suffices.
 *
 * <p>Out of scope this phase:
 * <ul>
 *   <li>Live property validation (e.g., reject empty {@code api-key} when primary
 *       is non-mock) — deferred Phase 2 với actual HTTP integration</li>
 *   <li>Failover logic (Gemini → OpenAI selection at runtime — handled by
 *       Resilience4j fallback method in {@code ResilientAIClient})</li>
 *   <li>Cost cap enforcement (Phase 3 observability wiring)</li>
 * </ul>
 *
 * @since Wave local-doable-9 (GAP-867 Phase 1 scaffold)
 * @see com.kiteclass.core.module.ai.client.AIClient
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
@Slf4j
public class AIClientConfig {

    private Provider provider = new Provider();
    private Gemini gemini = new Gemini();
    private Openai openai = new Openai();

    @PostConstruct
    public void logBinding() {
        log.info("AIClientConfig bound: provider.primary={} provider.fallback={} provider.failOnAllProviders={}",
                provider.getPrimary(), provider.getFallback(), provider.getFailOnAllProviders());
    }

    @Data
    public static class Provider {
        /** {@code gemini | openai | mock} — primary provider per ADR-038 §2.3. */
        private String primary = "gemini";

        /** Used when primary Circuit Breaker OPEN or rate-limit hit. */
        private String fallback = "openai";

        /** Final fallback when all providers fail — {@code template} routes to STATIC. */
        private String failOnAllProviders = "template";
    }

    @Data
    public static class Gemini {
        private String apiKey = "";
        private String modelText = "gemini-1.5-flash";
        private String modelQuality = "gemini-1.5-pro";
        private int timeoutMs = 30000;
        private int maxRetries = 2;
    }

    @Data
    public static class Openai {
        private String apiKey = "";
        private String modelText = "gpt-4o-mini";
        private String modelImage = "gpt-image-1";
        private int timeoutMs = 60000;
        private int maxRetries = 1;
    }
}
