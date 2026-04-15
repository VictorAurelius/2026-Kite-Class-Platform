package com.kiteclass.core.module.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Input to {@code AIClient.analyze()} — extracts palette/tone/style signals
 * from tenant-supplied brand inputs.
 *
 * <p>Domain type only; no provider-specific fields (Adapter pattern per ADR-008).
 *
 * @since 3.18.0 (Wave 3 Sub-PR 3.2)
 */
@Value
@Builder
public class AnalysisRequest {

    /** Optional logo bytes (PNG/SVG). Null if tenant skipped upload. */
    byte[] logoBytes;

    String logoMimeType;

    /** Tenant-picked audience (e.g. "K-12", "center", "university"). */
    String audience;

    /** Tenant-picked tone (e.g. "friendly", "professional", "energetic"). */
    String tone;

    /** Additional user-selected brand context (segment presets, color hints, etc.). */
    @Builder.Default
    Map<String, String> context = Map.of();
}
