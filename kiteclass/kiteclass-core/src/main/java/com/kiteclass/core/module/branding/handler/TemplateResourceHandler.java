package com.kiteclass.core.module.branding.handler;

import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Composes a TEMPLATE asset: picks matching SVG template + brand params, renders PNG,
 * persists a {@link BrandingResource} row.
 *
 * <p>Scaffold only in this Sub-PR — actual template engine (SVG transform + PNG render)
 * lands in a follow-up inside Wave 3 / 6. Here the handler returns an existing TEMPLATE
 * row if present, or {@code fallback} to nudge routing to the fallback link. Persistence
 * of freshly-composed resources is delegated to the upstream service once template
 * engine is wired.
 *
 * @since 3.19.0 (Wave 3 Sub-PR 3.3)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateResourceHandler implements ResourceHandler {

    private final BrandingResourceRepository repository;

    @Override
    public ResourceCategory supports() {
        return ResourceCategory.TEMPLATE;
    }

    @Override
    public HandlerResult handle(ResourceRequest request, ClassificationContext context) {
        return repository.findFirstByTypeAndCategoryAndDeletedFalse(
                        request.getType(), ResourceCategory.TEMPLATE)
                .map(resource -> {
                    log.debug("[template] reusing type={} id={}", request.getType(), resource.getId());
                    return HandlerResult.ready(ResourceCategory.TEMPLATE, resource);
                })
                .orElseGet(() -> {
                    log.info("[template] no existing template resource for type={}; compose pending",
                            request.getType());
                    return HandlerResult.pending(ResourceCategory.TEMPLATE, "template-compose-pending");
                });
    }
}
