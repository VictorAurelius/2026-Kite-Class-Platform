package com.kiteclass.core.module.branding.handler;

import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Last-resort resolver — returns a generic default TEMPLATE resource bundled with the
 * platform. Invoked by {@link com.kiteclass.core.module.branding.service.ResourceRoutingService}
 * when the primary handler returned {@link HandlerResult.Status#FALLBACK} or errored.
 *
 * <p>Deliberately <strong>not</strong> a {@link ResourceHandler} — keeping it out of the
 * category→handler map so the router invokes it explicitly, not by classification.
 *
 * @since 3.19.0 (Wave 3 Sub-PR 3.3)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FallbackHandler {

    private final BrandingResourceRepository repository;

    public HandlerResult rescue(ResourceRequest request) {
        BrandingResource fallback = repository
                .findFirstByTypeAndCategoryAndDeletedFalse(request.getType(), ResourceCategory.TEMPLATE)
                .orElse(null);
        if (fallback != null) {
            log.info("[fallback] serving default template type={} id={}",
                    request.getType(), fallback.getId());
            return HandlerResult.ready(ResourceCategory.TEMPLATE, fallback);
        }
        log.warn("[fallback] no default template type={}; seeding required", request.getType());
        return HandlerResult.pending(ResourceCategory.TEMPLATE, "seed-default-template-pending");
    }
}
