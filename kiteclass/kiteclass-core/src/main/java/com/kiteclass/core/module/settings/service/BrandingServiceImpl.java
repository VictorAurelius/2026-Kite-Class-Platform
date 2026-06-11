package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.events.BrandingEventPublisher;
import com.kiteclass.core.module.branding.events.BrandingUpdatedEvent;
import com.kiteclass.core.module.marketing.service.LandingPageContentSanitizer;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.mapper.BrandingMapper;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import com.kiteclass.core.module.settings.storage.BrandingAssetStorage;
import com.kiteclass.core.module.settings.storage.BrandingAssetUrlResolver;
import com.kiteclass.core.module.settings.versioning.BrandingVersionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Service implementation for Branding management.
 *
 * @since 2.9
 */
@Service
@Validated
@Slf4j
public class BrandingServiceImpl implements BrandingService {

    /** Per-file upload cap (5 MB). Logos/favicons are small; cap protects MinIO + memory. */
    static final long MAX_ASSET_BYTES = 5L * 1024 * 1024;

    /** Allowed image MIME types for logo/favicon upload (GAP-804). */
    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp",
            "image/svg+xml", "image/x-icon", "image/vnd.microsoft.icon");

    private final BrandingRepository brandingRepository;
    private final BrandingMapper brandingMapper;
    private final BrandingEventPublisher brandingEventPublisher;
    private final BrandingVersionService brandingVersionService;
    private final BrandingAssetStorage brandingAssetStorage;
    private final LandingPageContentSanitizer contentSanitizer;
    private final BrandingAssetUrlResolver assetUrlResolver;

    public BrandingServiceImpl(
            BrandingRepository brandingRepository,
            BrandingMapper brandingMapper,
            @Autowired(required = false) BrandingEventPublisher brandingEventPublisher,
            @Autowired(required = false) BrandingVersionService brandingVersionService,
            @Autowired(required = false) BrandingAssetStorage brandingAssetStorage,
            @Autowired(required = false) LandingPageContentSanitizer contentSanitizer,
            @Autowired(required = false) BrandingAssetUrlResolver assetUrlResolver) {
        this.brandingRepository = brandingRepository;
        this.brandingMapper = brandingMapper;
        this.brandingEventPublisher = brandingEventPublisher;
        this.brandingVersionService = brandingVersionService;
        this.brandingAssetStorage = brandingAssetStorage;
        this.contentSanitizer = contentSanitizer;
        this.assetUrlResolver = assetUrlResolver;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cached per tenant in {@code branding-by-tenant} (default 1h TTL via {@link
     * com.kiteclass.core.common.config.CacheConfig}; eviction on
     * {@link #updateBranding(UpdateBrandingRequest)} / {@link #uploadLogo(MultipartFile)} /
     * {@link #uploadFavicon(MultipartFile)}). Closes <strong>GAP-215</strong> — Wave 5 audit found this
     * was uncached, hitting PostgreSQL on every document render
     * (see {@code documents/04-quality/audits/performance/performance-audit-2026-04-25-wave5.md}
     * finding P0-1).
     *
     * <p>{@code sync = true} coalesces concurrent cache misses for the same tenant — same
     * stampede-protection pattern as {@link
     * com.kiteclass.core.module.branding.service.CachingBrandingPackageProxy} (GAP-043).
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "branding-by-tenant",
            key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()",
            sync = true)
    public BrandingResponse getBranding() {
        UUID instanceId = TenantContext.getCurrentTenant();

        Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> createDefaultBranding(instanceId));

        BrandingResponse response = brandingMapper.toResponse(branding);
        // GAP-1072: the persisted logo/favicon URLs are presigned and expire after
        // the storage TTL (7 days). Re-derive a fresh presigned URL on every read so
        // the FE never renders a broken (403) asset. Transient only — not written back.
        if (assetUrlResolver != null) {
            response.setLogoUrl(assetUrlResolver.regenerate(response.getLogoUrl()));
            response.setFaviconUrl(assetUrlResolver.regenerate(response.getFaviconUrl()));
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(
            value = "branding-by-tenant",
            key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()")
    public BrandingResponse updateBranding(@Valid UpdateBrandingRequest request) {
        UUID instanceId = TenantContext.getCurrentTenant();

        Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> {
                    Branding newBranding = new Branding();
                    newBranding.setInstanceId(instanceId);
                    newBranding.setDisplayName(request.getDisplayName());
                    newBranding.setPrimaryColor("#3B82F6");
                    newBranding.setSecondaryColor("#8B5CF6");
                    newBranding.setAccentColor("#10B981");
                    return brandingRepository.save(newBranding);
                });

        // Update fields from request (PATCH semantics)
        brandingMapper.updateFromRequest(request, branding);

        // GAP-829: sanitize tenant free-text on write (defense-in-depth). Branding
        // displayName/tagline get copied into LandingPage hero on seed-from-branding,
        // then reused on non-auto-escape surfaces (JsonLd, email, OG meta). Strip markup
        // at source; sanitizeText preserves VN diacritics (NFC) per vn-localization §5.
        // Email/phone/social URLs are already @Email/@Pattern/@Size-constrained → no free-text.
        if (contentSanitizer != null) {
            branding.setDisplayName(contentSanitizer.sanitizeText(branding.getDisplayName()));
            branding.setTagline(contentSanitizer.sanitizeText(branding.getTagline()));
            branding.setAddress(contentSanitizer.sanitizeText(branding.getAddress()));
        }

        branding = brandingRepository.save(branding);

        // Wave 4 (GAP-033p): snapshot the new state into version history.
        if (brandingVersionService != null) {
            brandingVersionService.snapshot(branding, /*rollbackOf*/ null);
        }

        // Wave 4 (GAP-021): publish branding.updated so downstream caches evict.
        if (brandingEventPublisher != null) {
            brandingEventPublisher.publishUpdated(new BrandingUpdatedEvent(
                    branding.getId(),
                    instanceId.toString(),
                    branding.getVersion() == null ? 0 : branding.getVersion().intValue(),
                    Instant.now()));
        }

        log.info("Updated branding for instance {}", instanceId);

        return brandingMapper.toResponse(branding);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(
            value = "branding-by-tenant",
            key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()")
    public BrandingResponse uploadLogo(MultipartFile file) {
        UUID instanceId = TenantContext.getCurrentTenant();
        String url = storeAsset(instanceId, ResourceType.LOGO, file);

        Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> createDefaultBranding(instanceId));

        branding.setLogoUrl(url);
        branding = brandingRepository.save(branding);

        log.info("Uploaded logo for instance {}", instanceId);

        return brandingMapper.toResponse(branding);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(
            value = "branding-by-tenant",
            key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()")
    public BrandingResponse uploadFavicon(MultipartFile file) {
        UUID instanceId = TenantContext.getCurrentTenant();
        String url = storeAsset(instanceId, ResourceType.FAVICON, file);

        Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseGet(() -> createDefaultBranding(instanceId));

        branding.setFaviconUrl(url);
        branding = brandingRepository.save(branding);

        log.info("Uploaded favicon for instance {}", instanceId);

        return brandingMapper.toResponse(branding);
    }

    /**
     * Validate a multipart asset upload and persist it to MinIO via
     * {@link BrandingAssetStorage}, returning the renderable URL.
     *
     * @param instanceId tenant instance id
     * @param type       asset type (LOGO / FAVICON)
     * @param file       multipart upload
     * @return renderable URL of the stored asset
     */
    private String storeAsset(UUID instanceId, ResourceType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("BRANDING_ASSET_EMPTY", new Object[0]);
        }
        if (file.getSize() > MAX_ASSET_BYTES) {
            throw new ValidationException("BRANDING_ASSET_TOO_LARGE", new Object[0]);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("BRANDING_ASSET_TYPE_UNSUPPORTED", new Object[0]);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new ValidationException("BRANDING_ASSET_FILENAME_REQUIRED", new Object[0]);
        }
        if (brandingAssetStorage == null) {
            throw new BusinessException("BRANDING_ASSET_STORAGE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            log.warn("Branding asset upload IOException instance={} type={} filename={}",
                    instanceId, type, filename, ex);
            throw new BusinessException("BRANDING_ASSET_UPLOAD_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return brandingAssetStorage.store(instanceId, type, filename, contentType, bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public String getThemeConfig() {
        UUID instanceId = TenantContext.getCurrentTenant();

        return brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .map(Branding::getThemeConfigJson)
                .orElse(null);
    }

    /**
     * Create default branding for tenant.
     *
     * @param instanceId tenant instance ID
     * @return default branding (not persisted)
     */
    private Branding createDefaultBranding(UUID instanceId) {
        Branding branding = new Branding();
        branding.setInstanceId(instanceId);
        branding.setDisplayName("KiteClass");
        branding.setTagline("Nền tảng quản lý trung tâm đào tạo");
        branding.setPrimaryColor("#3B82F6");
        branding.setSecondaryColor("#8B5CF6");
        branding.setAccentColor("#10B981");
        return branding;
    }
}
