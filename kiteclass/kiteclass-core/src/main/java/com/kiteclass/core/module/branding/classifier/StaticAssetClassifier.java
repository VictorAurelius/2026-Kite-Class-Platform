package com.kiteclass.core.module.branding.classifier;

import com.kiteclass.core.module.branding.entity.ResourceCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * If tenant has uploaded a static asset for this type, reuse it.
 *
 * @since 3.16.0
 */
@Component
public class StaticAssetClassifier implements ResourceClassifier {

    @Override
    public Optional<ResourceCategory> classify(ResourceRequest request, ClassificationContext context) {
        if (context.isHasStaticAsset()) {
            return Optional.of(ResourceCategory.STATIC);
        }
        return Optional.empty();
    }

    @Override
    public int order() {
        return 10;
    }
}
