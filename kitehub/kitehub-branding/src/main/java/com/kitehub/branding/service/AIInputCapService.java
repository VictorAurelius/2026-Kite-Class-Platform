package com.kitehub.branding.service;

import com.kitehub.branding.config.AIInputCapConfig;
import com.kitehub.branding.util.PromptTokenEstimator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Guards AI provider callsites against oversized input prompts (GAP-258).
 *
 * <p>The per-day request-count cap in {@link AIRateLimitService} bounds the
 * number of provider calls but not their input cost. A 100k-token prompt is
 * 50× more expensive than a 2k-token prompt yet consumes the same single
 * "request" slot. This service rejects oversized inputs before they reach
 * any provider, returning a structured 400 with the offending size + cap.</p>
 *
 * <p>Estimation uses {@link PromptTokenEstimator} (chars/4 heuristic). Real BPE
 * tokenization is out-of-scope (see GAP-258).</p>
 *
 * <p>Emits Micrometer counter {@code ai_input_token_rejection_total} tagged
 * with {@code tier} for Prometheus alerting (companion to GAP-122 alert
 * pattern).</p>
 *
 * @since 1.4.0 (GAP-258)
 */
@Slf4j
@Service
public class AIInputCapService {

    static final String COUNTER_NAME = "ai.input.token.rejection";
    static final String ERROR_CODE = "AI_INPUT_TOO_LONG";

    private final AIInputCapConfig config;
    private final MeterRegistry meterRegistry;

    public AIInputCapService(AIInputCapConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Validate the combined input size against the tier cap. Returns a 400
     * response when the input exceeds the cap, or {@code null} when it is
     * within budget (mirrors the {@code checkRateLimit} helper convention in
     * {@code AIBrandingController}).
     *
     * @param tier   subscription tier (resolved from gateway header)
     * @param inputs user-controlled string fields contributing to the prompt
     * @return rejection response or {@code null} when allowed
     */
    public ResponseEntity<Object> checkInputSize(String tier, String... inputs) {
        int cap = config.getMaxTokensForTier(tier);

        // -1 means unlimited (Enterprise default).
        if (cap < 0) {
            return null;
        }

        int estimated = PromptTokenEstimator.estimate(inputs);
        if (estimated <= cap) {
            return null;
        }

        Counter.builder(COUNTER_NAME)
                .description("AI input prompt token-cap rejections (GAP-258)")
                .tag("tier", tier == null ? "unknown" : tier.toUpperCase())
                .register(meterRegistry)
                .increment();

        log.warn("AI input cap exceeded (tier={}, estimated={}, cap={})",
                tier, estimated, cap);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", ERROR_CODE,
                        "message", "Input prompt exceeds the per-tier token budget",
                        "estimatedTokens", estimated,
                        "maxTokens", cap,
                        "tier", tier == null ? "FREE" : tier
                ));
    }
}
