package com.kiteclass.core.module.branding.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies default values for {@link BrandingRoutingProperties}. Defaults must
 * match BR-RES-005 (template-first) and the documented FULL_AI alert threshold.
 *
 * @since 3.21.0 (GAP-106)
 */
class BrandingRoutingPropertiesTest {

    @Test
    void defaults_match_br_res_005() {
        BrandingRoutingProperties props = new BrandingRoutingProperties();

        assertThat(props.isTemplateFirst())
                .as("template-first must default true (BR-RES-005)")
                .isTrue();
        assertThat(props.getMaxAiRatio())
                .as("max-ai-ratio default must be 0.20 per rules.md")
                .isEqualTo(0.20);
    }

    @Test
    void template_first_can_be_disabled_for_debug() {
        BrandingRoutingProperties props = new BrandingRoutingProperties();
        props.setTemplateFirst(false);
        assertThat(props.isTemplateFirst()).isFalse();
    }

    @Test
    void max_ai_ratio_respects_override() {
        BrandingRoutingProperties props = new BrandingRoutingProperties();
        props.setMaxAiRatio(0.35);
        assertThat(props.getMaxAiRatio()).isEqualTo(0.35);
    }
}
