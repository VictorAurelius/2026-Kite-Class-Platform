package com.kiteclass.core.module.branding.classifier;

import com.kiteclass.core.module.branding.entity.ResourceCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default happy path — if a template matches, compose via template (fast & cheap).
 *
 * @since 3.16.0
 */
@Component
public class TemplateMatchClassifier implements ResourceClassifier {

    @Override
    public Optional<ResourceCategory> classify(ResourceRequest request, ClassificationContext context) {
        if (context.isHasMatchingTemplate()) {
            return Optional.of(ResourceCategory.TEMPLATE);
        }
        return Optional.empty();
    }

    @Override
    public int order() {
        return 30;
    }
}
