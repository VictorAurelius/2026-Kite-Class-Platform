package com.kiteclass.core.module.quality.check;

import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Check #5 — logo present and not obviously broken.
 *
 * <p>Scaffold: passes if at least one LOGO resource exists and has a storage_url. Real
 * bounding-box check needs image introspection; deferred.
 *
 * @since 3.25.0 (Wave 4 Sub-PR 4.5)
 */
@Component
@RequiredArgsConstructor
public class LogoPlacementQualityCheck implements QualityCheck {

    public static final String NAME = "logo-placement";

    private final BrandingResourceRepository repository;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Result run(FrontendInstance instance) {
        List<BrandingResource> logos = repository.findByTypeAndDeletedFalse(ResourceType.LOGO);
        if (logos.isEmpty()) {
            return Result.fail(NAME, 40, "no LOGO resource present for this tenant");
        }
        boolean anyWithUrl = logos.stream()
                .anyMatch(r -> r.getStorageUrl() != null && !r.getStorageUrl().isBlank());
        if (!anyWithUrl) {
            return Result.fail(NAME, 55, "LOGO exists but no storage_url set");
        }
        return Result.pass(NAME, 95);
    }
}
