package com.kitehub.branding.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-1137 — FULL_AI tier gate: PREMIUM + ENTERPRISE → FULL_AI; everything else → TEMPLATE.
 */
@DisplayName("GenerationMode.forTier (GAP-1137 tier gate)")
class GenerationModeTest {

    @ParameterizedTest
    @ValueSource(strings = {"PREMIUM", "ENTERPRISE", "premium", "  enterprise  "})
    @DisplayName("PREMIUM + ENTERPRISE (case/space-insensitive) → FULL_AI")
    void premiumAndEnterpriseGetFullAi(String tier) {
        assertThat(GenerationMode.forTier(tier)).isEqualTo(GenerationMode.FULL_AI);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FREE", "TRIAL", "BASIC", "free", "basic", "GOLD", "unknown"})
    @DisplayName("FREE / BASIC / unknown → TEMPLATE")
    void otherTiersGetTemplate(String tier) {
        assertThat(GenerationMode.forTier(tier)).isEqualTo(GenerationMode.TEMPLATE);
    }

    @Test
    @DisplayName("null tier → TEMPLATE (no crash)")
    void nullTierGetsTemplate() {
        assertThat(GenerationMode.forTier(null)).isEqualTo(GenerationMode.TEMPLATE);
    }
}
