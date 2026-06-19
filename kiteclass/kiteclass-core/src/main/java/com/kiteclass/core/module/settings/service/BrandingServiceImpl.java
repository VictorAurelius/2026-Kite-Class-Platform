package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.events.BrandingEventPublisher;
import com.kiteclass.core.module.branding.events.BrandingUpdatedEvent;
import com.kiteclass.core.module.marketing.service.LandingPageContentSanitizer;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BannerUploadResponse;
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

    /**
     * Allowed image MIME types for logo/favicon/banner upload (GAP-804).
     *
     * <p><strong>GAP-1037</strong> — {@code image/svg+xml} removed. SVG is active
     * content (can embed {@code <script>} / event handlers) and was served inline
     * with the client-reported Content-Type → stored XSS when a tenant logo renders
     * on login / tenant pages. Branding assets are raster-only now (PNG / JPEG / WEBP
     * / ICO). The client-reported MIME is only a first gate — actual bytes are
     * magic-byte sniffed in {@link #isRasterImage(byte[])} so a script payload spoofed
     * with an {@code image/png} header is still rejected.
     */
    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp",
            "image/x-icon", "image/vnd.microsoft.icon");

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
     * {@inheritDoc}
     *
     * <p>GAP-1211 — stores each banner under a fresh {@code uuid}-named key (no
     * overwrite) and returns the renderable URL. Does NOT touch the {@code Branding}
     * row nor the {@code branding-by-tenant} cache (banners live on the landing
     * {@code heroImages} list, persisted separately by the landing admin PUT).
     */
    @Override
    public BannerUploadResponse uploadBanner(MultipartFile file) {
        UUID instanceId = TenantContext.getCurrentTenant();
        String url = storeBannerAsset(instanceId, file);
        log.info("Uploaded banner for instance {}", instanceId);
        return new BannerUploadResponse(url);
    }

    /**
     * Validate a banner multipart upload and persist it under a unique key
     * {@code static/{tenantId}/banner/{uuid}.{ext}} via {@link BrandingAssetStorage},
     * returning the renderable URL.
     *
     * <p>Distinct status codes from {@link #storeAsset} (which returns 400 for all):
     * banner upload returns HTTP 415 for an unsupported MIME and HTTP 413 when the
     * size cap is exceeded, per {@code pre-handoff-self-test-completeness.md} §2.5.
     *
     * @param instanceId tenant instance id
     * @param file       multipart banner upload
     * @return renderable URL of the stored banner
     */
    private String storeBannerAsset(UUID instanceId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("BRANDING_ASSET_EMPTY", new Object[0]);
        }
        if (file.getSize() > MAX_ASSET_BYTES) {
            throw new BusinessException("BRANDING_BANNER_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("BRANDING_BANNER_TYPE_UNSUPPORTED",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        if (brandingAssetStorage == null) {
            throw new BusinessException("BRANDING_ASSET_STORAGE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            log.warn("Banner upload IOException instance={} filename={}",
                    instanceId, file.getOriginalFilename(), ex);
            throw new BusinessException("BRANDING_ASSET_UPLOAD_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // GAP-1037: content-sniff the actual bytes — the client-reported MIME is
        // spoofable. A script/SVG payload sent with an image/png header passes the
        // allowlist above but fails the magic-byte check here.
        if (!isRasterImage(bytes)) {
            throw new BusinessException("BRANDING_BANNER_TYPE_UNSUPPORTED",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        // Unique object name per upload so banners accumulate (never overwrite a
        // slot). Extension preserved for correct Content-Type / browser handling.
        String uniqueName = UUID.randomUUID() + extensionFor(file.getOriginalFilename(), contentType);
        return brandingAssetStorage.store(instanceId, ResourceType.BANNER, uniqueName, contentType, bytes);
    }

    /**
     * Derive a lowercase file extension (including the leading dot) for a banner
     * object name: from the original filename when present, else mapped from the
     * MIME type. Empty string when neither yields one.
     */
    private static String extensionFor(String filename, String contentType) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                // Strip path-traversal / separators defensively; storage also sanitizes.
                return filename.substring(dot).toLowerCase()
                        .replace("..", "").replace("/", "").replace("\\", "");
            }
        }
        return switch (contentType == null ? "" : contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/x-icon", "image/vnd.microsoft.icon" -> ".ico";
            default -> "";
        };
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

        // GAP-1037: content-sniff the actual bytes — the client-reported MIME is
        // spoofable. A script/SVG payload sent with an image/png header passes the
        // allowlist above but fails the magic-byte check here.
        if (!isRasterImage(bytes)) {
            throw new ValidationException("BRANDING_ASSET_TYPE_UNSUPPORTED", new Object[0]);
        }

        return brandingAssetStorage.store(instanceId, type, filename, contentType, bytes);
    }

    /**
     * Magic-byte content sniff — accept only genuine raster images (PNG / JPEG /
     * WEBP / ICO). Closes <strong>GAP-1037</strong>: SVG/HTML/script payloads (active
     * content) never match a raster signature, so a {@code <script>}-bearing file
     * — even one spoofed with an {@code image/png} Content-Type header — is rejected
     * here regardless of what the client claimed.
     *
     * @param bytes raw uploaded bytes
     * @return {@code true} when the leading bytes match an accepted raster signature
     */
    static boolean isRasterImage(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return matchesPng(bytes) || matchesJpeg(bytes) || matchesWebp(bytes) || matchesIco(bytes);
    }

    /** PNG signature: 89 50 4E 47 0D 0A 1A 0A. */
    private static boolean matchesPng(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
    }

    /** JPEG SOI marker: FF D8 FF. */
    private static boolean matchesJpeg(byte[] b) {
        return b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    /** WEBP: "RIFF"....{size}...."WEBP" (RIFF container with WEBP fourcc at offset 8). */
    private static boolean matchesWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    /** ICO: 00 00 01 00 (reserved + image-type=1 icon, little-endian). */
    private static boolean matchesIco(byte[] b) {
        return b.length >= 4 && b[0] == 0x00 && b[1] == 0x00 && b[2] == 0x01 && b[3] == 0x00;
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
