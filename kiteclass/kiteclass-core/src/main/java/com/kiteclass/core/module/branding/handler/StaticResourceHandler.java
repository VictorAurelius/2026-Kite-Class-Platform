package com.kiteclass.core.module.branding.handler;

import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Returns the tenant-uploaded static asset for the requested resource type.
 *
 * <p>Assumes upload already happened (the classifier chain only returns STATIC when
 * {@link ClassificationContext#isHasStaticAsset()} is true). Actual uploads are handled
 * by the existing kiteclass-core storage module outside the branding pipeline.
 *
 * @since 3.19.0 (Wave 3 Sub-PR 3.3)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaticResourceHandler implements ResourceHandler {

    private final BrandingResourceRepository repository;

    @Override
    public ResourceCategory supports() {
        return ResourceCategory.STATIC;
    }

    @Override
    public HandlerResult handle(ResourceRequest request, ClassificationContext context) {
        return repository.findFirstByTypeAndCategoryAndDeletedFalse(
                        request.getType(), ResourceCategory.STATIC)
                .map(resource -> {
                    log.debug("[static] resolved type={} id={}", request.getType(), resource.getId());
                    return HandlerResult.ready(ResourceCategory.STATIC, resource);
                })
                .orElseGet(() -> {
                    log.warn("[static] classifier claimed static asset exists for type={} but "
                            + "repository has none — falling back", request.getType());
                    return HandlerResult.fallback("no static asset found despite classifier");
                });
    }
}
