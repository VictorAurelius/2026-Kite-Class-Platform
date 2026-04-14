package com.kiteclass.core.module.branding.classifier;

import com.kiteclass.core.module.branding.entity.ResourceCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * No template matched but AI quota available → use AI.
 *
 * @since 3.16.0
 */
@Component
public class AIFallbackClassifier implements ResourceClassifier {

    @Override
    public Optional<ResourceCategory> classify(ResourceRequest request, ClassificationContext context) {
        if (context.isHasAIQuota()) {
            return Optional.of(ResourceCategory.FULL_AI);
        }
        return Optional.empty();
    }

    @Override
    public int order() {
        return 40;
    }
}
