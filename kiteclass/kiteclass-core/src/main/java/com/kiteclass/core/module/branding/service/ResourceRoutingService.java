package com.kiteclass.core.module.branding.service;

import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.ResourceClassifier;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.handler.FallbackHandler;
import com.kiteclass.core.module.branding.handler.HandlerResult;
import com.kiteclass.core.module.branding.handler.ResourceHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ResourceRoutingService — orchestrates two pipelines:
 *
 * <ol>
 *   <li><b>Classification</b> (Chain of Responsibility, GAP-007): walk ordered classifiers
 *       until one returns a {@link ResourceCategory}.</li>
 *   <li><b>Handling</b> (Strategy, Wave 3 Sub-PR 3.3): dispatch to the
 *       {@link ResourceHandler} whose {@link ResourceHandler#supports()} matches the
 *       category. If the handler returns {@link HandlerResult.Status#FALLBACK}, invoke
 *       {@link FallbackHandler#rescue} as a last-resort rescue.</li>
 * </ol>
 *
 * <p>Per ADR-005:
 * <ul>
 *   <li>Template-first routing (~80% of traffic should NOT hit FULL_AI)</li>
 *   <li>AI used only when explicitly requested or no template matches</li>
 * </ul>
 *
 * @since 3.16.0 (GAP-007, ADR-005), extended 3.19.0 (Wave 3 Sub-PR 3.3)
 */
@Service
@Slf4j
public class ResourceRoutingService {

    private final List<ResourceClassifier> orderedClassifiers;
    private final Map<ResourceCategory, ResourceHandler> handlersByCategory;
    private final FallbackHandler fallback;

    public ResourceRoutingService(
            List<ResourceClassifier> classifiers,
            List<ResourceHandler> handlers,
            FallbackHandler fallback) {
        this.orderedClassifiers = classifiers.stream()
                .sorted(Comparator.comparingInt(ResourceClassifier::order))
                .toList();
        this.handlersByCategory = new EnumMap<>(ResourceCategory.class);
        for (ResourceHandler handler : handlers) {
            ResourceCategory category = handler.supports();
            ResourceHandler previous = this.handlersByCategory.put(category, handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate ResourceHandler for category " + category
                                + ": " + previous.getClass().getSimpleName()
                                + " and " + handler.getClass().getSimpleName());
            }
        }
        this.fallback = fallback;
    }

    /**
     * Walk the classifier chain until one classifier returns a category.
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

    /**
     * Full pipeline — classify + dispatch + fallback-rescue.
     */
    public HandlerResult route(ResourceRequest request, ClassificationContext context) {
        ResourceCategory category = classify(request, context);
        ResourceHandler handler = handlersByCategory.get(category);
        if (handler == null) {
            log.warn("No handler registered for category {}, rescuing via fallback", category);
            return fallback.rescue(request);
        }
        HandlerResult result = handler.handle(request, context);
        if (result.getStatus() == HandlerResult.Status.FALLBACK) {
            log.info("Handler {} returned FALLBACK, rescuing", handler.getClass().getSimpleName());
            return fallback.rescue(request);
        }
        return result;
    }
}
