package com.kiteclass.core.module.ai.workflow;

import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.workflow.step.ExtractPaletteStep;
import com.kiteclass.core.module.ai.workflow.step.PickTemplateStep;
import com.kiteclass.core.module.ai.workflow.step.PublishPackageStep;
import com.kiteclass.core.module.branding.service.CachingBrandingPackageProxy;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StepsTest {

    private StepContext ctx() {
        return new StepContext(10L, "t-1");
    }

    @Test
    void extract_palette_reads_from_analysis() {
        var step = new ExtractPaletteStep();
        var context = ctx();
        context.setAnalysis(AnalysisResult.builder()
                .palette(List.of("#AA0000", "#00AA00", "#0000AA")).build());

        step.execute(context);

        assertThat((List<String>) context.get(ExtractPaletteStep.KEY_PALETTE))
                .containsExactly("#AA0000", "#00AA00", "#0000AA");
    }

    @Test
    void extract_palette_falls_back_to_neutral_when_analysis_empty() {
        var step = new ExtractPaletteStep();
        var context = ctx();

        step.fallback(context);

        List<String> palette = context.get(ExtractPaletteStep.KEY_PALETTE);
        assertThat(palette).isNotEmpty();
        assertThat(palette.get(0)).matches("^#[0-9A-Fa-f]{6}$");
    }

    @Test
    void pick_template_requires_palette_on_context() {
        var step = new PickTemplateStep();

        assertThatThrownBy(() -> step.execute(ctx()))
                .isInstanceOf(StepException.class)
                .hasMessageContaining("palette");
    }

    @Test
    void pick_template_writes_template_id_after_palette() {
        var context = ctx();
        context.put(ExtractPaletteStep.KEY_PALETTE, List.of("#2563EB"));
        var step = new PickTemplateStep();

        step.execute(context);

        assertThat((String) context.get(PickTemplateStep.KEY_TEMPLATE_ID))
                .isEqualTo("default-template-v1");
    }

    @Test
    void publish_package_marks_deployed_and_evicts_cache() {
        var lifecycle = mock(InstanceLifecycleService.class);
        var cache = mock(CachingBrandingPackageProxy.class);
        var step = new PublishPackageStep(lifecycle, cache);
        var context = ctx();
        context.put(PickTemplateStep.KEY_TEMPLATE_ID, "default-template-v1");

        step.execute(context);

        verify(lifecycle).markBrandingCompleted(10L, null);
        verify(cache).evict(10L);
    }

    @Test
    void publish_package_fails_when_template_id_missing() {
        var step = new PublishPackageStep(
                mock(InstanceLifecycleService.class),
                mock(CachingBrandingPackageProxy.class));

        assertThatThrownBy(() -> step.execute(ctx()))
                .isInstanceOf(StepException.class)
                .hasMessageContaining("template-id");
    }
}
