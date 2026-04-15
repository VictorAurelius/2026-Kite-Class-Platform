package com.kiteclass.core.module.ai;

import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisResultTest {

    @Test
    void templateOnly_fallback_marker() {
        AnalysisResult r = AnalysisResult.templateOnly();

        assertThat(r.isTemplateOnly()).isTrue();
        assertThat(r.getPalette()).isEmpty();
        assertThat(r.getMoodTags()).isEmpty();
    }

    @Test
    void generation_templateFallback_marker() {
        GenerationResult r = GenerationResult.templateFallback();

        assertThat(r.isTemplateFallback()).isTrue();
        assertThat(r.getImageUrl()).isNull();
        assertThat(r.getImageBytes()).isNull();
    }
}
