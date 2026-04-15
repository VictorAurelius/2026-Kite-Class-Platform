package com.kiteclass.core.module.ai.workflow.step;

import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.workflow.Step;
import com.kiteclass.core.module.ai.workflow.StepContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reads the palette from {@link AnalysisResult} and stashes it in the context under
 * key {@code palette} for downstream Steps.
 *
 * <p>Fallback: if no analysis palette is available, seed with a neutral default
 * (#2563EB primary). Downstream Steps see a palette regardless.
 *
 * <p>Context keys written: {@code palette} (List&lt;String&gt;).
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5)
 */
@Component
@Slf4j
public class ExtractPaletteStep implements Step {

    public static final String KEY_PALETTE = "palette";
    private static final List<String> NEUTRAL_PALETTE = List.of("#2563EB", "#1E40AF", "#EFF6FF");

    @Override
    public String name() {
        return "extract-palette";
    }

    @Override
    public void execute(StepContext context) {
        AnalysisResult analysis = context.getAnalysis();
        List<String> palette = analysis != null && !analysis.getPalette().isEmpty()
                ? analysis.getPalette()
                : NEUTRAL_PALETTE;
        context.put(KEY_PALETTE, palette);
        log.debug("[step:extract-palette] palette={}", palette);
    }

    @Override
    public boolean hasFallback() {
        return true;
    }

    @Override
    public void fallback(StepContext context) {
        context.put(KEY_PALETTE, NEUTRAL_PALETTE);
        log.warn("[step:extract-palette] fallback to neutral palette");
    }
}
