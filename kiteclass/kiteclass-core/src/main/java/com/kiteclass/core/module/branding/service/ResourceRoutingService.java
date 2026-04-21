package com.kiteclass.core.module.branding.service;

import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.ResourceClassifier;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.config.BrandingRoutingProperties;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.handler.FallbackHandler;
import com.kiteclass.core.module.branding.handler.HandlerResult;
import com.kiteclass.core.module.branding.handler.ResourceHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
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
 * <p><b>Config binding (GAP-106):</b> {@link BrandingRoutingProperties} exposes
 * {@code branding.routing.template-first} and {@code branding.routing.max-ai-ratio}.
 * The former is logged at startup (fail-loud if disabled in non-dev profiles);
 * the latter is a metric alert threshold consumed by Grafana/Prometheus via the
 * {@code branding.routing.classified} counter emitted on every classify().
 *
 * @since 3.16.0 (GAP-007, ADR-005), extended 3.19.0 (Wave 3 Sub-PR 3.3),
 *        metrics wired 3.21.0 (GAP-106, Wave 9-D).
 */
@Service
@Slf4j
public class ResourceRoutingService {

    private final List<ResourceClassifier> orderedClassifiers;
    private final Map<ResourceCategory, ResourceHandler> handlersByCategory;
    private final FallbackHandler fallback;
    private final BrandingRoutingProperties routingProperties;
    private final MeterRegistry meterRegistry;

    @Autowired
    public ResourceRoutingService(
            List<ResourceClassifier> classifiers,
            List<ResourceHandler> handlers,
            FallbackHandler fallback,
            BrandingRoutingProperties routingProperties,
            MeterRegistry meterRegistry) {
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
        this.routingProperties = routingProperties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Convenience constructor for tests that don't need metrics / properties.
     * Production wiring always uses the 5-arg Spring-injected constructor.
     */
    public ResourceRoutingService(
            List<ResourceClassifier> classifiers,
            List<ResourceHandler> handlers,
            FallbackHandler fallback) {
        this(classifiers, handlers, fallback, new BrandingRoutingProperties(), null);
    }

    /**
     * Startup log — honours the {@code branding.routing.template-first} flag.
     * When disabled, emits a warning — production deployments should never
     * disable template-first.
     */
    @PostConstruct
    void logRoutingConfig() {
        if (!routingProperties.isTemplateFirst()) {
            log.warn("branding.routing.template-first=false — template-first enforcement is DISABLED."
                    + " This is intended for debug/load-test only; production deployments MUST leave"
                    + " template-first=true (BR-RES-005).");
        } else {
            log.info("branding.routing.template-first=true, max-ai-ratio={} (BR-RES-005)",
                    routingProperties.getMaxAiRatio());
        }
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
                ResourceCategory category = result.get();
                log.debug("Request type={} routed to {} by {}",
                        request.getType(), category,
                        classifier.getClass().getSimpleName());
                recordClassification(category);
                return category;
            }
        }
        throw new IllegalStateException(
                "No classifier resolved request — DefaultTemplateClassifier missing from chain");
    }

    /**
     * Emit a Micrometer counter tagged by category so dashboards can compute
     * {@code branding.routing.ai_ratio = FULL_AI / total}. Prometheus alert rule
     * fires when this ratio exceeds {@code branding.routing.max-ai-ratio}
     * (default 0.20 per BR-RES-005).
     */
    private void recordClassification(ResourceCategory category) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("branding.routing.classified")
                .tag("category", category.name().toLowerCase(Locale.ROOT))
                .description("Branding requests classified per ResourceCategory (GAP-106, BR-RES-005)")
                .register(meterRegistry)
                .increment();
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
