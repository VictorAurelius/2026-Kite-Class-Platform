package com.kiteclass.core.module.marketing.service.impl;

import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.dto.response.LandingPageResponse;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.mapper.LandingPageMapper;
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import com.kiteclass.core.module.marketing.service.LandingPageContentSanitizer;
import com.kiteclass.core.module.marketing.service.LandingPageService;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of LandingPageService interface.
 *
 * <p>Business Rule: BR-MKT-001 - Each tenant has ONE landing page.
 * Landing pages are auto-created with default values if not exists.
 *
 * @since 2.10
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class LandingPageServiceImpl implements LandingPageService {

    private final LandingPageRepository landingPageRepository;
    private final LandingPageMapper landingPageMapper;
    private final BrandingRepository brandingRepository;
    private final LandingPageContentSanitizer contentSanitizer;

    /**
     * Gets landing page for tenant, creates default if not exists.
     *
     * <p>Implements BR-MKT-001: Each tenant has ONE landing page.
     * Result is cached with key "landingPage::{tenantId}".
     *
     * @param tenantId the tenant ID
     * @return LandingPageResponse with landing page content
     */
    @Override
    // NOT readOnly: getOrCreateDefault() INSERTs a default row on first visit (lazy
    // create per BR-MKT-001). readOnly=true threw "cannot execute INSERT in a
    // read-only transaction" for every tenant's first homepage load (GAP-809 walk fix).
    // @Cacheable means the create happens once; subsequent visits hit the cache.
    @Transactional
    // GAP-043 (Wave 9.5-D) — sync=true critical here: landing pages are PUBLIC-facing
    // (anonymous visitor traffic), so a cache expiry can be hit by hundreds of
    // concurrent visitors simultaneously. Request-coalescing keeps the DB protected.
    @Cacheable(value = "landingPages", key = "#tenantId")
    public LandingPageResponse getLandingPage(UUID tenantId) {
        log.debug("Fetching landing page for tenant: {}", tenantId);

        LandingPage landingPage = getOrCreateDefault(tenantId);

        return landingPageMapper.toResponse(landingPage);
    }

    /**
     * Updates landing page content for tenant.
     *
     * <p>Creates landing page with defaults if not exists.
     * Only updates non-null fields from request.
     *
     * @param tenantId the tenant ID
     * @param request  the update request
     * @return LandingPageResponse with updated content
     */
    @Override
    @Transactional
    @CacheEvict(value = "landingPages", key = "#tenantId")
    public LandingPageResponse updateLandingPage(UUID tenantId, UpdateLandingPageRequest request) {
        log.info("Updating landing page for tenant: {}", tenantId);

        LandingPage landingPage = getOrCreateDefault(tenantId);

        landingPageMapper.updateEntity(landingPage, request);
        // GAP-827: sanitize-on-write (defense-in-depth) AFTER MapStruct copy, BEFORE persist.
        // Strips XSS from text + JSONB sections, NFC-preserves VN diacritics, validates image
        // URL scheme/host allowlist (throws ValidationException → HTTP 400 on malicious URL).
        contentSanitizer.sanitize(landingPage);
        LandingPage updated = landingPageRepository.save(landingPage);

        log.info("Updated landing page for tenant: {}", tenantId);
        return landingPageMapper.toResponse(updated);
    }

    /**
     * Gets existing landing page or creates default for tenant.
     *
     * <p>Private helper method implementing BR-MKT-001.
     * Default values are set in LandingPage entity fields.
     *
     * @param tenantId the tenant ID
     * @return landing page (existing or newly created)
     */
    private LandingPage getOrCreateDefault(UUID tenantId) {
        return landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)
                .orElseGet(() -> {
                    log.info("Creating default landing page for tenant: {}", tenantId);

                    LandingPage newLandingPage = new LandingPage();
                    newLandingPage.setInstanceId(tenantId);
                    // Inherit tenant branding (settings.Branding) so the public homepage
                    // reflects the owner's customised theme/logo/name instead of the
                    // generic KiteClass default. Falls back to entity @Column defaults
                    // when the tenant has no branding row. (GAP-809 demo-trio walk fix.)
                    brandingRepository.findByInstanceIdAndDeletedFalse(tenantId).ifPresent(b -> {
                        Branding branding = b;
                        if (branding.getPrimaryColor() != null) {
                            newLandingPage.setPrimaryColor(branding.getPrimaryColor());
                        }
                        if (branding.getSecondaryColor() != null) {
                            newLandingPage.setSecondaryColor(branding.getSecondaryColor());
                        }
                        if (branding.getLogoUrl() != null) {
                            newLandingPage.setLogoUrl(branding.getLogoUrl());
                        }
                        if (branding.getDisplayName() != null) {
                            newLandingPage.setHeroTitle(branding.getDisplayName());
                        }
                        if (branding.getTagline() != null) {
                            newLandingPage.setTagline(branding.getTagline());
                        }
                    });

                    return landingPageRepository.save(newLandingPage);
                });
    }
}
