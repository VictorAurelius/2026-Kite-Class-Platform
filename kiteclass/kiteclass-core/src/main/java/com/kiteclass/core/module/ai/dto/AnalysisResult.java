package com.kiteclass.core.module.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Output of {@code AIClient.analyze()} — describes brand signals in domain terms.
 *
 * <p>Includes {@code templateOnly} flag set by the resilience fallback path — signals
 * {@code ResourceRoutingService} to force TEMPLATE category (skip FULL_AI) for this
 * session.
 *
 * @since 3.18.0 (Wave 3 Sub-PR 3.2)
 */
@Value
@Builder
public class AnalysisResult {

    /** Dominant colors, hex strings (no alpha). */
    @Builder.Default
    List<String> palette = List.of();

    /** Inferred typography style (e.g. "serif", "sans-serif", "rounded"). */
    String typographyStyle;

    /** Inferred brand mood in 1-3 tags (e.g. "professional", "warm"). */
    @Builder.Default
    List<String> moodTags = List.of();

    /**
     * True when analysis couldn't run (AI down / quota exceeded / fallback).
     * Downstream should route via TEMPLATE category only.
     */
    boolean templateOnly;

    /** Fallback marker — safe default result for downstream code. */
    public static AnalysisResult templateOnly() {
        return AnalysisResult.builder().templateOnly(true).build();
    }
}
