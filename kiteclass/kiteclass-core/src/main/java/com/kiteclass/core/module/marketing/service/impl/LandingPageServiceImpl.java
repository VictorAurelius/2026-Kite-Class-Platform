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
import com.kiteclass.core.module.settings.storage.BrandingAssetUrlResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@org.springframework.validation.annotation.Validated
public class LandingPageServiceImpl implements LandingPageService {

    private final LandingPageRepository landingPageRepository;
    private final LandingPageMapper landingPageMapper;
    private final BrandingRepository brandingRepository;
    private final LandingPageContentSanitizer contentSanitizer;
    // GAP-1204: re-derive a fresh presigned URL for logo/hero on read so the public
    // landing never renders a broken (403) asset after the stored presigned URL's
    // 7-day TTL expires. Optional — null when no MinIO storage bean is configured.
    private final BrandingAssetUrlResolver assetUrlResolver;

    public LandingPageServiceImpl(
            LandingPageRepository landingPageRepository,
            LandingPageMapper landingPageMapper,
            BrandingRepository brandingRepository,
            LandingPageContentSanitizer contentSanitizer,
            @Autowired(required = false) BrandingAssetUrlResolver assetUrlResolver) {
        this.landingPageRepository = landingPageRepository;
        this.landingPageMapper = landingPageMapper;
        this.brandingRepository = brandingRepository;
        this.contentSanitizer = contentSanitizer;
        this.assetUrlResolver = assetUrlResolver;
    }

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

        LandingPageResponse response = withFreshAssetUrls(landingPageMapper.toResponse(landingPage));
        // GAP-1229: favicon đọc transient từ settings.Branding (không persist copy) —
        // tab browser per-tenant; null → FE fallback /icon.svg default KiteClass.
        brandingRepository.findByInstanceIdAndDeletedFalse(tenantId)
                .map(Branding::getFaviconUrl)
                .ifPresent(url -> response.setFaviconUrl(
                        assetUrlResolver != null ? assetUrlResolver.regenerate(url) : url));
        return response;
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
        return withFreshAssetUrls(landingPageMapper.toResponse(updated));
    }

    /**
     * Re-derive fresh presigned URLs for the response's asset fields (GAP-1204).
     *
     * <p>The branding pipeline can persist a presigned MinIO URL into
     * {@code landing_pages.logo_url} (inherited from the tenant's Branding row)
     * whose signature expires after the 7-day storage TTL → the public landing
     * renders a broken (403) logo. Mirroring the settings Branding surface
     * (GAP-1072), we regenerate from the stable object key on every read instead
     * of returning the stale stored value. {@code heroImageUrl} is swept too;
     * static {@code /demo-banners/...} paths are non-presigned → returned as-is.
     *
     * <p>Transient only — the regenerated URL is never written back to the DB.
     */
    private LandingPageResponse withFreshAssetUrls(LandingPageResponse response) {
        if (assetUrlResolver == null || response == null) {
            return response;
        }
        response.setLogoUrl(assetUrlResolver.regenerate(response.getLogoUrl()));
        response.setHeroImageUrl(assetUrlResolver.regenerate(response.getHeroImageUrl()));
        // GAP-826: regenerate each carousel banner too (mirror logo/hero single). Static
        // /demo-banners/... paths are non-presigned → returned as-is by the resolver.
        if (response.getHeroImages() != null) {
            response.setHeroImages(response.getHeroImages().stream()
                    .map(assetUrlResolver::regenerate)
                    .toList());
        }
        return response;
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
                            // GAP-1083: centerName = the center's own name (nav/footer/JsonLd
                            // prefer this over the heroTitle slogan).
                            newLandingPage.setCenterName(branding.getDisplayName());
                        }
                        if (branding.getTagline() != null) {
                            newLandingPage.setTagline(branding.getTagline());
                        }
                        if (branding.getZaloUrl() != null) {
                            // GAP-1083: surface the tenant's Zalo OA in the FloatingCTA.
                            newLandingPage.setZaloUrl(branding.getZaloUrl());
                        }
                    });

                    return landingPageRepository.save(newLandingPage);
                });
    }

    private static final java.util.regex.Pattern HEX = java.util.regex.Pattern.compile("^#[0-9A-Fa-f]{6}$");

    /**
     * GAP-1213 — apply a freshly-deployed AI-branding theme onto the tenant landing page so the
     * public landing changes after wizard deploy. Idempotent on {@code brandingVersion}; evicts
     * the {@code landingPages} cache (same key the public read uses) so the next visitor sees it.
     */
    @Override
    @Transactional
    @CacheEvict(value = "landingPages", key = "#tenantId")
    public boolean applyDeployedBranding(UUID tenantId, String primaryColor, String secondaryColor,
                                         String logoUrl, Integer brandingVersion) {
        LandingPage landingPage = getOrCreateDefault(tenantId);

        // Idempotency: ignore stale/duplicate deploy events (incoming version not newer).
        if (brandingVersion != null && landingPage.getBrandingVersion() != null
                && brandingVersion <= landingPage.getBrandingVersion()) {
            log.info("branding.deployed skipped for tenant {} — version {} <= applied {}",
                    tenantId, brandingVersion, landingPage.getBrandingVersion());
            return false;
        }

        boolean changed = false;
        if (primaryColor != null && HEX.matcher(primaryColor).matches()) {
            landingPage.setPrimaryColor(primaryColor);
            changed = true;
        }
        if (secondaryColor != null && HEX.matcher(secondaryColor).matches()) {
            landingPage.setSecondaryColor(secondaryColor);
            changed = true;
        }
        if (logoUrl != null && !logoUrl.isBlank()) {
            landingPage.setLogoUrl(logoUrl);
            changed = true;
        }
        if (brandingVersion != null) {
            landingPage.setBrandingVersion(brandingVersion);
        }
        landingPageRepository.save(landingPage);
        log.info("Applied branding.deployed to landing for tenant {} (version {}, changed={})",
                tenantId, brandingVersion, changed);
        return changed;
    }
}
