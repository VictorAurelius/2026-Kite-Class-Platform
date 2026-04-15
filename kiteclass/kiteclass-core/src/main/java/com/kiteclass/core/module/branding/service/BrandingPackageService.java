package com.kiteclass.core.module.branding.service;

import com.kiteclass.core.module.branding.dto.BrandingPackage;

/**
 * Builds the composite {@link BrandingPackage} for a given instance id.
 *
 * <p>Split as interface so {@code CachingBrandingPackageProxy} can decorate it (Proxy
 * pattern per ADR-009).
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4)
 */
public interface BrandingPackageService {

    BrandingPackage getByInstanceId(Long instanceId);
}
