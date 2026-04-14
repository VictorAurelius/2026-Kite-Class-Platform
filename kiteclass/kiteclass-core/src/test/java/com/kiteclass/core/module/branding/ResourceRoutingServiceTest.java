package com.kiteclass.core.module.branding;

import com.kiteclass.core.module.branding.classifier.AIFallbackClassifier;
import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.CustomAIRequestClassifier;
import com.kiteclass.core.module.branding.classifier.DefaultTemplateClassifier;
import com.kiteclass.core.module.branding.classifier.ResourceClassifier;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.classifier.StaticAssetClassifier;
import com.kiteclass.core.module.branding.classifier.TemplateMatchClassifier;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.service.ResourceRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        service = new ResourceRoutingService(chain);
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
        ResourceRoutingService minimal = new ResourceRoutingService(List.of(
                new StaticAssetClassifier()
        ));

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
        ResourceRoutingService ordered = new ResourceRoutingService(List.of(
                new DefaultTemplateClassifier(),
                fake
        ));

        ResourceCategory result = ordered.classify(request(false), ctx(false, false, false));

        assertThat(result).isEqualTo(ResourceCategory.STATIC);
    }
}
