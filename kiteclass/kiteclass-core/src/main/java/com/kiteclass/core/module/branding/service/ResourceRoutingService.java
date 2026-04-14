package com.kiteclass.core.module.branding.service;

import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.ResourceClassifier;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * ResourceRoutingService — runs the classification chain (Chain of Responsibility)
 * to decide which {@link ResourceCategory} handles a resource request.
 *
 * <p>Per ADR-005:
 * <ul>
 *   <li>Template-first routing (~80% of traffic should NOT hit FULL_AI)</li>
 *   <li>AI used only when explicitly requested or no template matches</li>
 * </ul>
 *
 * <p>The actual handlers (static fetch, template compose, AI generate) live in a
 * downstream service — this class is responsible solely for classification.
 *
 * @since 3.16.0 (GAP-007, ADR-005)
 */
@Service
@Slf4j
public class ResourceRoutingService {

    private final List<ResourceClassifier> orderedClassifiers;

    public ResourceRoutingService(List<ResourceClassifier> classifiers) {
        this.orderedClassifiers = classifiers.stream()
                .sorted(Comparator.comparingInt(ResourceClassifier::order))
                .toList();
    }

    /**
     * Walk the chain until one classifier returns a category.
     *
     * <p>{@code DefaultTemplateClassifier} (order=100) guarantees a terminal answer.
     */
    public ResourceCategory classify(ResourceRequest request, ClassificationContext context) {
        for (ResourceClassifier classifier : orderedClassifiers) {
            var result = classifier.classify(request, context);
            if (result.isPresent()) {
                log.debug("Request type={} routed to {} by {}",
                        request.getType(), result.get(),
                        classifier.getClass().getSimpleName());
                return result.get();
            }
        }
        throw new IllegalStateException(
                "No classifier resolved request — DefaultTemplateClassifier missing from chain");
    }
}
