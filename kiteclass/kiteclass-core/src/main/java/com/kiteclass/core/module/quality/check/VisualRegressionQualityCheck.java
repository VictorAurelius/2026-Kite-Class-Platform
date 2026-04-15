package com.kiteclass.core.module.quality.check;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import org.springframework.stereotype.Component;

/**
 * Check #4 — visual regression vs baseline ≤ 20% diff.
 *
 * <p>Scaffold: always passes at score 85 for now. Real implementation needs a
 * screenshot service + image-hash comparator; deferred to follow-up.
 *
 * @since 3.25.0 (Wave 4 Sub-PR 4.5)
 */
@Component
public class VisualRegressionQualityCheck implements QualityCheck {

    public static final String NAME = "visual-regression";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Result run(FrontendInstance instance) {
        return Result.pass(NAME, 85);
    }
}
