package com.kitehub.branding.queue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AIJobPriority#fromTier(String)} tier mapping.
 */
class AIJobPriorityTest {

    @Test
    void fromTier_enterprise_returnsEnterprise() {
        assertThat(AIJobPriority.fromTier("ENTERPRISE")).isEqualTo(AIJobPriority.ENTERPRISE);
        assertThat(AIJobPriority.fromTier("enterprise")).isEqualTo(AIJobPriority.ENTERPRISE);
    }

    @Test
    void fromTier_premiumAndBasic_returnsPro() {
        assertThat(AIJobPriority.fromTier("PREMIUM")).isEqualTo(AIJobPriority.PRO);
        assertThat(AIJobPriority.fromTier("BASIC")).isEqualTo(AIJobPriority.PRO);
        assertThat(AIJobPriority.fromTier("pro")).isEqualTo(AIJobPriority.PRO);
    }

    @Test
    void fromTier_freeAndTrial_returnsFree() {
        assertThat(AIJobPriority.fromTier("FREE")).isEqualTo(AIJobPriority.FREE);
        assertThat(AIJobPriority.fromTier("TRIAL")).isEqualTo(AIJobPriority.FREE);
    }

    @Test
    void fromTier_null_returnsFreeAsSafeDefault() {
        assertThat(AIJobPriority.fromTier(null)).isEqualTo(AIJobPriority.FREE);
    }

    @Test
    void fromTier_unknownTier_returnsFree() {
        assertThat(AIJobPriority.fromTier("PLATINUM")).isEqualTo(AIJobPriority.FREE);
        assertThat(AIJobPriority.fromTier("")).isEqualTo(AIJobPriority.FREE);
    }

    @Test
    void weights_matchExpectedRatio() {
        assertThat(AIJobPriority.ENTERPRISE.getWeight()).isEqualTo(3);
        assertThat(AIJobPriority.PRO.getWeight()).isEqualTo(2);
        assertThat(AIJobPriority.FREE.getWeight()).isEqualTo(1);
    }
}
