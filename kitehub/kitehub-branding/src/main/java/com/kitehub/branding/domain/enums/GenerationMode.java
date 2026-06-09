package com.kitehub.branding.domain.enums;

/**
 * Banner generation mode (per ADR-037 Amendment 2026-06-10).
 *
 * <ul>
 *   <li>{@link #TEMPLATE} — default FREE path: HTML template + Gemini-generated
 *       copy → (Playwright) WebP. Deterministic, crisp Vietnamese, $0.</li>
 *   <li>{@link #FULL_AI} — paid upgrade for PREMIUM (limited monthly quota) +
 *       ENTERPRISE (unlimited): GPT image-gen renders the full banner. Tier-gated
 *       per {@code ai-branding-guidelines.md} §2.4 + SUB-22 matrix (GAP-1137);
 *       CircuitBreaker / failure / quota-exceeded fallback → {@link #TEMPLATE}.</li>
 * </ul>
 *
 * @since GAP-1135 (real generation wiring); GAP-1137 (PREMIUM+ENTERPRISE gate)
 */
public enum GenerationMode {
    TEMPLATE,
    FULL_AI;

    /**
     * Resolve the generation mode for a subscription tier. FULL_AI is gated to
     * PREMIUM + ENTERPRISE per {@code ai-branding-guidelines.md} §2.4 + SUB-22
     * matrix (GAP-1137 — user decision 2026-06-10: PREMIUM limited quota,
     * ENTERPRISE unlimited); FREE / BASIC / unknown use the TEMPLATE path.
     *
     * <p>NOTE: tier eligibility (this method) is distinct from the PREMIUM
     * monthly cost quota — an eligible PREMIUM job still falls back to TEMPLATE
     * when its quota is exhausted (enforced by {@code FullAiQuotaService}).</p>
     *
     * @param tier subscription tier string (nullable → TEMPLATE)
     * @return resolved generation mode
     */
    public static GenerationMode forTier(String tier) {
        if (tier == null) {
            return TEMPLATE;
        }
        return switch (tier.trim().toUpperCase()) {
            case "PREMIUM", "ENTERPRISE" -> FULL_AI;
            default -> TEMPLATE;
        };
    }
}
