package com.kiteclass.core.module.branding.classifier;

import com.kiteclass.core.module.branding.entity.ResourceCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Terminal classifier — always returns TEMPLATE (default generic template library).
 *
 * <p>Guarantees the chain always resolves to a category.
 *
 * @since 3.16.0
 */
@Component
public class DefaultTemplateClassifier implements ResourceClassifier {

    @Override
    public Optional<ResourceCategory> classify(ResourceRequest request, ClassificationContext context) {
        return Optional.of(ResourceCategory.TEMPLATE);
    }

    @Override
    public int order() {
        return 100;
    }
}
