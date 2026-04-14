package com.kiteclass.core.module.branding.classifier;

import com.kiteclass.core.module.branding.entity.ResourceCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Honor explicit "custom" request when AI quota is available.
 *
 * @since 3.16.0
 */
@Component
public class CustomAIRequestClassifier implements ResourceClassifier {

    @Override
    public Optional<ResourceCategory> classify(ResourceRequest request, ClassificationContext context) {
        if (request.isCustomRequested() && context.isHasAIQuota()) {
            return Optional.of(ResourceCategory.FULL_AI);
        }
        return Optional.empty();
    }

    @Override
    public int order() {
        return 20;
    }
}
