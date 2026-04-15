package com.kiteclass.core.module.branding.handler;

import com.kiteclass.core.module.ai.client.AIClient;
import com.kiteclass.core.module.ai.dto.GenerationResult;
import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceHandlersTest {

    @Mock
    private BrandingResourceRepository repository;

    @Mock
    private AIClient aiClient;

    @InjectMocks
    private StaticResourceHandler staticHandler;

    @InjectMocks
    private TemplateResourceHandler templateHandler;

    @InjectMocks
    private AIResourceHandler aiHandler;

    @InjectMocks
    private FallbackHandler fallback;

    private ResourceRequest request(ResourceType type, boolean custom) {
        return ResourceRequest.builder().type(type).customRequested(custom).build();
    }

    private ClassificationContext ctx(boolean aiQuota) {
        return ClassificationContext.builder().hasAIQuota(aiQuota).build();
    }

    // ---- StaticResourceHandler ------------------------------------------------

    @Test
    void static_handler_returns_ready_when_asset_exists() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.LOGO).category(ResourceCategory.STATIC).build();
        when(repository.findFirstByTypeAndCategoryAndDeletedFalse(
                eq(ResourceType.LOGO), eq(ResourceCategory.STATIC)))
                .thenReturn(Optional.of(res));

        HandlerResult result = staticHandler.handle(request(ResourceType.LOGO, false), ctx(false));

        assertThat(result.getStatus()).isEqualTo(HandlerResult.Status.READY);
        assertThat(result.getCategory()).isEqualTo(ResourceCategory.STATIC);
        assertThat(result.getResource()).isEqualTo(res);
    }

    @Test
    void static_handler_falls_back_when_asset_missing() {
        when(repository.findFirstByTypeAndCategoryAndDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());

        HandlerResult result = staticHandler.handle(request(ResourceType.LOGO, false), ctx(false));

        assertThat(result.getStatus()).isEqualTo(HandlerResult.Status.FALLBACK);
        assertThat(result.getMessage()).contains("no static asset");
    }

    // ---- TemplateResourceHandler ----------------------------------------------

    @Test
    void template_handler_reuses_existing_row() {
        BrandingResource res = BrandingResource.builder()
                .type(ResourceType.BANNER).category(ResourceCategory.TEMPLATE).templateId(1L).build();
        when(repository.findFirstByTypeAndCategoryAndDeletedFalse(
                eq(ResourceType.BANNER), eq(ResourceCategory.TEMPLATE)))
                .thenReturn(Optional.of(res));

        HandlerResult result = templateHandler.handle(request(ResourceType.BANNER, false), ctx(false));

        assertThat(result.getStatus()).isEqualTo(HandlerResult.Status.READY);
        assertThat(result.getResource()).isEqualTo(res);
    }

    @Test
    void template_handler_pending_when_no_existing_row() {
        when(repository.findFirstByTypeAndCategoryAndDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());

        HandlerResult result = templateHandler.handle(request(ResourceType.BANNER, false), ctx(false));

        assertThat(result.getStatus()).isEqualTo(HandlerResult.Status.PENDING);
        assertThat(result.getJobId()).isEqualTo("template-compose-pending");
    }

    // ---- AIResourceHandler ----------------------------------------------------

    @Test
    void ai_handler_pending_on_happy_path() {
        when(aiClient.generate(any())).thenReturn(GenerationResult.builder()
                .imageUrl("https://ai/hero.png").mimeType("image/png").build());

        HandlerResult result = aiHandler.handle(request(ResourceType.HERO, true), ctx(true));

        assertThat(result.getStatus()).isEqualTo(HandlerResult.Status.PENDING);
        assertThat(result.getCategory()).isEqualTo(ResourceCategory.FULL_AI);
    }

    @Test
    void ai_handler_fallback_when_client_marks_fallback() {
        when(aiClient.generate(any())).thenReturn(GenerationResult.templateFallback());

        HandlerResult result = aiHandler.handle(request(ResourceType.HERO, true), ctx(true));

        assertThat(result.getStatus()).isEqualTo(HandlerResult.Status.FALLBACK);
    }

    // ---- FallbackHandler ------------------------------------------------------

    @Test
    void fallback_rescue_returns_ready_when_default_template_exists() {
        BrandingResource tmpl = BrandingResource.builder()
                .type(ResourceType.HERO).category(ResourceCategory.TEMPLATE).templateId(99L).build();
        when(repository.findFirstByTypeAndCategoryAndDeletedFalse(any(), any()))
                .thenReturn(Optional.of(tmpl));

        HandlerResult result = fallback.rescue(request(ResourceType.HERO, false));

        assertThat(result.getStatus()).isEqualTo(HandlerResult.Status.READY);
        assertThat(result.getResource()).isEqualTo(tmpl);
    }

    @Test
    void fallback_rescue_pending_when_no_seed_template() {
        when(repository.findFirstByTypeAndCategoryAndDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());

        HandlerResult result = fallback.rescue(request(ResourceType.HERO, false));

        assertThat(result.getStatus()).isEqualTo(HandlerResult.Status.PENDING);
    }
}
