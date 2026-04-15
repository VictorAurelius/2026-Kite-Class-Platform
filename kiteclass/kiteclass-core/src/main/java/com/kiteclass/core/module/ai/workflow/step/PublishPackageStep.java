package com.kiteclass.core.module.ai.workflow.step;

import com.kiteclass.core.module.ai.workflow.Step;
import com.kiteclass.core.module.ai.workflow.StepContext;
import com.kiteclass.core.module.ai.workflow.StepException;
import com.kiteclass.core.module.branding.service.CachingBrandingPackageProxy;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Terminal step: marks the frontend instance as DEPLOYED and evicts the branding package
 * cache so FE fetches the fresh composite on next call.
 *
 * <p>Context consumed: {@code template-id} (sanity check, proves planner ran steps in order).
 * <p>Context written: none — side effect only (lifecycle transition + cache evict).
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublishPackageStep implements Step {

    private final InstanceLifecycleService lifecycle;
    private final CachingBrandingPackageProxy packageCache;

    @Override
    public String name() {
        return "publish-package";
    }

    @Override
    public void execute(StepContext context) {
        Object templateId = context.get(PickTemplateStep.KEY_TEMPLATE_ID);
        if (templateId == null) {
            throw new StepException("publish-package requires template-id from pick-template");
        }
        lifecycle.markBrandingCompleted(context.getInstanceId(), null);
        packageCache.evict(context.getInstanceId());
        log.info("[step:publish-package] id={} → DEPLOYED + cache evicted",
                context.getInstanceId());
    }
}
