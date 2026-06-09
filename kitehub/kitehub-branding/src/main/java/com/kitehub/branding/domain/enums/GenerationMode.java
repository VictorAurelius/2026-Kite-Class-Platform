package com.kitehub.branding.domain.enums;

/**
 * Banner generation mode (per ADR-037 Amendment 2026-06-10).
 *
 * <ul>
 *   <li>{@link #TEMPLATE} — default FREE path: HTML template + Gemini-generated
 *       copy → (Playwright) WebP. Deterministic, crisp Vietnamese, $0.</li>
 *   <li>{@link #FULL_AI} — ENTERPRISE-only upgrade: GPT image-gen renders the
 *       full banner. Tier-gated per {@code ai-branding-guidelines.md} §2.4;
 *       CircuitBreaker / failure fallback → {@link #TEMPLATE}.</li>
 * </ul>
 *
 * @since GAP-1117 (real generation wiring)
 */
public enum GenerationMode {
    TEMPLATE,
    FULL_AI;

    /**
     * Resolve the generation mode for a subscription tier. FULL_AI is gated to
     * ENTERPRISE per {@code ai-branding-guidelines.md} §2.4; every other tier
     * (FREE / BASIC / PREMIUM / unknown) uses the TEMPLATE path.
     *
     * @param tier subscription tier string (nullable → TEMPLATE)
     * @return resolved generation mode
     */
    public static GenerationMode forTier(String tier) {
        return tier != null && "ENTERPRISE".equalsIgnoreCase(tier.trim())
                ? FULL_AI
                : TEMPLATE;
    }
}
