package com.kiteclass.core.module.quality.check;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import org.springframework.stereotype.Component;

/**
 * Check #2 — CSS variables actually applied to branding theme (no defaults left).
 *
 * <p>Scaffold: passes if instance has non-null frontendUrl (proxy for theme being
 * published). Real check needs to fetch rendered CSS + compare against tenant theme;
 * deferred to follow-up.
 *
 * @since 3.25.0 (Wave 4 Sub-PR 4.5)
 */
@Component
public class CssVarsQualityCheck implements QualityCheck {

    public static final String NAME = "css-vars-applied";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Result run(FrontendInstance instance) {
        if (instance.getFrontendUrl() == null || instance.getFrontendUrl().isBlank()) {
            return Result.fail(NAME, 50, "frontend_url not set — theme may not be published");
        }
        return Result.pass(NAME, 90);
    }
}
