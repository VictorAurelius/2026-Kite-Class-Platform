package com.kiteclass.core.module.branding;

import com.kiteclass.core.module.branding.classifier.AIFallbackClassifier;
import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.CustomAIRequestClassifier;
import com.kiteclass.core.module.branding.classifier.DefaultTemplateClassifier;
import com.kiteclass.core.module.branding.classifier.ResourceClassifier;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.classifier.StaticAssetClassifier;
import com.kiteclass.core.module.branding.classifier.TemplateMatchClassifier;
import com.kiteclass.core.module.branding.config.BrandingRoutingProperties;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.handler.FallbackHandler;
import com.kiteclass.core.module.branding.handler.HandlerResult;
import com.kiteclass.core.module.branding.handler.ResourceHandler;
import com.kiteclass.core.module.branding.service.ResourceRoutingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceRoutingServiceTest {

    private ResourceRoutingService service;

    @BeforeEach
    void setUp() {
        List<ResourceClassifier> chain = List.of(
                new DefaultTemplateClassifier(),  // intentionally out of order
                new StaticAssetClassifier(),
                new TemplateMatchClassifier(),
                new CustomAIRequestClassifier(),
                new AIFallbackClassifier()
        );
        service = new ResourceRoutingService(chain, java.util.List.of(), null);
    }

    private ResourceRequest request(boolean customRequested) {
        return ResourceRequest.builder()
                .type(ResourceType.BANNER)
                .customRequested(customRequested)
                .build();
    }

    private ClassificationContext ctx(boolean staticAsset, boolean templateMatch, boolean aiQuota) {
        return ClassificationContext.builder()
                .hasStaticAsset(staticAsset)
                .hasMatchingTemplate(templateMatch)
                .hasAIQuota(aiQuota)
                .build();
    }

    @Test
    void static_asset_wins_over_everything() {
        ResourceCategory result = service.classify(request(true),
                ctx(true, true, true));

        assertThat(result).isEqualTo(ResourceCategory.STATIC);
    }

    @Test
    void custom_request_with_quota_picks_ai() {
        ResourceCategory result = service.classify(request(true),
                ctx(false, true, true));

        assertThat(result).isEqualTo(ResourceCategory.FULL_AI);
    }

    @Test
    void custom_request_without_quota_falls_back_to_template() {
        ResourceCategory result = service.classify(request(true),
                ctx(false, true, false));

        assertThat(result).isEqualTo(ResourceCategory.TEMPLATE);
    }

    @Test
    void template_match_picked_when_not_custom() {
        ResourceCategory result = service.classify(request(false),
                ctx(false, true, true));

        assertThat(result).isEqualTo(ResourceCategory.TEMPLATE);
    }

    @Test
    void no_template_but_ai_quota_picks_ai() {
        ResourceCategory result = service.classify(request(false),
                ctx(false, false, true));

        assertThat(result).isEqualTo(ResourceCategory.FULL_AI);
    }

    @Test
    void nothing_available_defaults_to_template() {
        ResourceCategory result = service.classify(request(false),
                ctx(false, false, false));

        assertThat(result).isEqualTo(ResourceCategory.TEMPLATE);
    }

    @Test
    void chain_without_default_classifier_throws_when_nothing_matches() {
        ResourceRoutingService minimal = new ResourceRoutingService(
                List.of(new StaticAssetClassifier()),
                java.util.List.of(),
                null);

        assertThatThrownBy(() -> minimal.classify(request(false), ctx(false, false, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No classifier");
    }

    @Test
    void chain_respects_order_field() {
        ResourceClassifier fake = new ResourceClassifier() {
            @Override
            public java.util.Optional<ResourceCategory> classify(ResourceRequest r, ClassificationContext c) {
                return java.util.Optional.of(ResourceCategory.STATIC);
            }

            @Override
            public int order() {
                return 1;  // run before StaticAssetClassifier (order=10)
            }
        };
        ResourceRoutingService ordered = new ResourceRoutingService(
                List.of(new DefaultTemplateClassifier(), fake),
                java.util.List.of(),
                null);

        ResourceCategory result = ordered.classify(request(false), ctx(false, false, false));

        assertThat(result).isEqualTo(ResourceCategory.STATIC);
    }

    // ---- route() tests (Sub-PR 3.3) ------------------------------------------

    private ResourceHandler handler(ResourceCategory category, HandlerResult result) {
        ResourceHandler handler = mock(ResourceHandler.class);
        when(handler.supports()).thenReturn(category);
        when(handler.handle(any(), any())).thenReturn(result);
        return handler;
    }

    @Test
    void route_invokes_matching_handler_and_returns_result() {
        BrandingResource res = BrandingResource.builder().type(ResourceType.BANNER).build();
        ResourceHandler templateHandler = handler(
                ResourceCategory.TEMPLATE, HandlerResult.ready(ResourceCategory.TEMPLATE, res));
        FallbackHandler fallback = mock(FallbackHandler.class);

        ResourceRoutingService svc = new ResourceRoutingService(
                List.of(new DefaultTemplateClassifier()),
                List.of(templateHandler),
                fallback);

        HandlerResult r = svc.route(request(false), ctx(false, false, false));

        assertThat(r.getStatus()).isEqualTo(HandlerResult.Status.READY);
        assertThat(r.getResource()).isEqualTo(res);
    }

    @Test
    void route_escalates_to_fallback_when_handler_returns_FALLBACK() {
        ResourceHandler templateHandler = handler(
                ResourceCategory.TEMPLATE, HandlerResult.fallback("no template"));
        BrandingResource rescueRes = BrandingResource.builder().type(ResourceType.BANNER).build();
        FallbackHandler fallback = mock(FallbackHandler.class);
        when(fallback.rescue(any())).thenReturn(
                HandlerResult.ready(ResourceCategory.TEMPLATE, rescueRes));

        ResourceRoutingService svc = new ResourceRoutingService(
                List.of(new DefaultTemplateClassifier()),
                List.of(templateHandler),
                fallback);

        HandlerResult r = svc.route(request(false), ctx(false, false, false));

        assertThat(r.getResource()).isEqualTo(rescueRes);
    }

    @Test
    void route_uses_fallback_when_no_handler_registered_for_category() {
        BrandingResource rescueRes = BrandingResource.builder().type(ResourceType.BANNER).build();
        FallbackHandler fallback = mock(FallbackHandler.class);
        when(fallback.rescue(any())).thenReturn(
                HandlerResult.ready(ResourceCategory.TEMPLATE, rescueRes));

        ResourceRoutingService svc = new ResourceRoutingService(
                List.of(new DefaultTemplateClassifier()),
                List.of(),  // no handler for TEMPLATE
                fallback);

        HandlerResult r = svc.route(request(false), ctx(false, false, false));

        assertThat(r.getResource()).isEqualTo(rescueRes);
    }

    @Test
    void duplicate_handler_for_same_category_rejected_at_construction() {
        ResourceHandler a = mock(ResourceHandler.class);
        ResourceHandler b = mock(ResourceHandler.class);
        when(a.supports()).thenReturn(ResourceCategory.TEMPLATE);
        when(b.supports()).thenReturn(ResourceCategory.TEMPLATE);

        assertThatThrownBy(() -> new ResourceRoutingService(
                List.of(new DefaultTemplateClassifier()),
                List.of(a, b),
                null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate ResourceHandler");
    }

    // ---- GAP-106 metric wiring -----------------------------------------------

    @Test
    void classify_emits_classified_counter_tagged_by_category() {
        MeterRegistry registry = new SimpleMeterRegistry();
        BrandingRoutingProperties props = new BrandingRoutingProperties();
        ResourceRoutingService metered = new ResourceRoutingService(
                List.of(new DefaultTemplateClassifier(),
                        new StaticAssetClassifier(),
                        new TemplateMatchClassifier(),
                        new CustomAIRequestClassifier(),
                        new AIFallbackClassifier()),
                java.util.List.of(),
                null,
                props,
                registry);

        metered.classify(request(true), ctx(true, false, false));   // STATIC
        metered.classify(request(false), ctx(false, true, false));  // TEMPLATE
        metered.classify(request(false), ctx(false, false, true));  // FULL_AI

        assertThat(registry.get("branding.routing.classified")
                .tag("category", "static").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("branding.routing.classified")
                .tag("category", "template").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("branding.routing.classified")
                .tag("category", "full_ai").counter().count()).isEqualTo(1.0);
    }
}
