package com.kiteclass.core.module.ai.workflow.step;

import com.kiteclass.core.module.ai.workflow.Step;
import com.kiteclass.core.module.ai.workflow.StepContext;
import com.kiteclass.core.module.ai.workflow.StepException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Picks a template id based on mood tags / segment preset. Scaffold only — a real
 * template-matcher service will be wired when GAP-011 template library lands.
 *
 * <p>Context keys consumed: {@code palette}, optional {@code segment-preset}
 * <p>Context keys written: {@code template-id} (String)
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5)
 */
@Component
@Slf4j
public class PickTemplateStep implements Step {

    public static final String KEY_TEMPLATE_ID = "template-id";
    private static final String DEFAULT_TEMPLATE = "default-template-v1";

    @Override
    public String name() {
        return "pick-template";
    }

    @Override
    public void execute(StepContext context) {
        Object palette = context.get(ExtractPaletteStep.KEY_PALETTE);
        if (!(palette instanceof List<?> list) || list.isEmpty()) {
            throw new StepException("pick-template requires palette set by extract-palette");
        }
        context.put(KEY_TEMPLATE_ID, DEFAULT_TEMPLATE);
        log.debug("[step:pick-template] id={} → {}", context.getInstanceId(), DEFAULT_TEMPLATE);
    }

    @Override
    public boolean hasFallback() {
        return true;
    }

    @Override
    public void fallback(StepContext context) {
        context.put(KEY_TEMPLATE_ID, DEFAULT_TEMPLATE);
        log.warn("[step:pick-template] fallback → default template");
    }
}
