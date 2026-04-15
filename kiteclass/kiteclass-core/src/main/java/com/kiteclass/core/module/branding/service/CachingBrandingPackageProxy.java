package com.kiteclass.core.module.branding.service;

import com.kiteclass.core.module.branding.dto.BrandingPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Proxy (Proxy pattern, ADR-009) caching {@link BrandingPackage} by instanceId.
 *
 * <p>Cache {@code branding-package} backed by Redis (see {@code CacheConfig}). TTL 1h
 * by default; eviction driven by branding lifecycle events — when
 * {@code InstanceLifecycleService} emits {@code instance.deployed} or
 * {@code instance.regenerating}, the outbox publisher / event listeners (wired in
 * Sub-PR 3.6) will call {@link #evict(Long)}.
 *
 * <p>Until that event-driven wiring lands, callers can invoke {@link #evict(Long)}
 * directly after state transitions if immediate consistency is needed.
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4, ADR-009)
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class CachingBrandingPackageProxy implements BrandingPackageService {

    public static final String CACHE_NAME = "branding-package";

    @Qualifier("brandingPackageServiceImpl")
    private final BrandingPackageService delegate;

    @Override
    @Cacheable(value = CACHE_NAME, key = "#instanceId")
    public BrandingPackage getByInstanceId(Long instanceId) {
        log.debug("[branding-package] cache miss — loading instanceId={}", instanceId);
        return delegate.getByInstanceId(instanceId);
    }

    @CacheEvict(value = CACHE_NAME, key = "#instanceId")
    public void evict(Long instanceId) {
        log.info("[branding-package] evicting cache instanceId={}", instanceId);
    }
}
