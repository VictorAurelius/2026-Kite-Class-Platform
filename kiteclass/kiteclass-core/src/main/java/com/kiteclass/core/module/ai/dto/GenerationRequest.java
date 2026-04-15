package com.kiteclass.core.module.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Input to {@code AIClient.generate()} — produces a branded image asset.
 *
 * <p>Prompts are backend-composed (per ai-branding-guidelines.md §2.3) — the
 * wizard doesn't expose a free-form prompt field except in Enterprise Advanced
 * Mode (tracked separately). This DTO reflects that composition result.
 *
 * @since 3.18.0 (Wave 3 Sub-PR 3.2)
 */
@Value
@Builder
public class GenerationRequest {

    /** Composed prompt (subject, style, constraints). */
    String prompt;

    /** Desired asset type (LOGO / BANNER / HERO / …). Stringly-typed to avoid circular dep. */
    String resourceType;

    /** Output width/height in pixels. */
    int width;
    int height;

    /** Optional seed for deterministic regeneration. */
    Long seed;

    /** Additional model-specific knobs (e.g. guidance_scale). */
    @Builder.Default
    Map<String, String> providerHints = Map.of();
}
