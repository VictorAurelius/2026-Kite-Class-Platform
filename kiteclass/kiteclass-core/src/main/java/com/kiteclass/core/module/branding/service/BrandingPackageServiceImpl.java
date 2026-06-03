package com.kiteclass.core.module.branding.service;

import com.kiteclass.core.module.branding.dto.BrandingPackage;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Assembles the composite {@link BrandingPackage} from FrontendInstance + BrandingResource rows.
 *
 * <p>Exposed as {@code brandingPackageServiceImpl} bean. Wrapped by
 * {@link CachingBrandingPackageProxy} which is the {@code @Primary} bean — callers
 * autowire the interface and automatically hit the cache.
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4)
 */
@Service("brandingPackageServiceImpl")
@RequiredArgsConstructor
public class BrandingPackageServiceImpl implements BrandingPackageService {

    private final FrontendInstanceRepository instanceRepository;
    private final BrandingResourceRepository resourceRepository;

    @Override
    public BrandingPackage getByInstanceId(Long instanceId) {
        FrontendInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "FrontendInstance not found: id=" + instanceId));

        // GAP-129: tenant-scoped lookup using composite index (instance_id, deleted)
        // — replaces a cross-tenant findAll() that loaded ALL branding resources of
        // ALL tenants AND returned them in this tenant's package (multi-tenancy bug).
        List<BrandingPackage.AssetEntry> assets = resourceRepository
                .findByInstanceIdAndDeletedFalse(instance.getInstanceId()).stream()
                .map(this::toAsset)
                .toList();

        return new BrandingPackage(
                instance.getId(),
                instance.getTenantSlug(),
                instance.getSlug(),
                instance.getFrontendUrl(),
                instance.getBrandingVersion(),
                instance.getDeployedAt(),
                assets
        );
    }

    private BrandingPackage.AssetEntry toAsset(BrandingResource resource) {
        return new BrandingPackage.AssetEntry(
                resource.getType().name(),
                resource.getCategory().name(),
                resource.getStorageUrl(),
                null  // alt-text landed by GAP-074 in a later wave
        );
    }
}
