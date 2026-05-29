package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for Branding management.
 *
 * @since 2.9
 */
public interface BrandingService {

    /**
     * Get branding for current tenant.
     * Returns default branding if not customized.
     *
     * @return branding response
     */
    BrandingResponse getBranding();

    /**
     * Update branding for current tenant.
     * Creates new branding if not exists.
     *
     * @param request update request
     * @return updated branding response
     */
    BrandingResponse updateBranding(@Valid UpdateBrandingRequest request);

    /**
     * Upload a logo image for the current tenant.
     *
     * <p>Stores the raw file bytes in MinIO/S3 (bucket {@code kite-branding-assets},
     * key {@code static/{tenantId}/logo/{filename}}) and updates
     * {@code branding.logoUrl} with the renderable URL. The tenant is resolved
     * from {@code TenantContext} (OWNER-scoped admin path).
     *
     * @param file multipart logo image (PNG/JPEG/WEBP/SVG)
     * @return updated branding response (logoUrl populated)
     * @since GAP-804 — replaces the prior {@code uploadLogo(String fileUrl)} contract
     */
    BrandingResponse uploadLogo(MultipartFile file);

    /**
     * Upload a favicon image for the current tenant.
     *
     * <p>Stores the raw file bytes in MinIO/S3 (key
     * {@code static/{tenantId}/favicon/{filename}}) and updates
     * {@code branding.faviconUrl} with the renderable URL.
     *
     * @param file multipart favicon image (PNG/ICO/SVG)
     * @return updated branding response (faviconUrl populated)
     * @since GAP-804 — replaces the prior {@code uploadFavicon(String fileUrl)} contract
     */
    BrandingResponse uploadFavicon(MultipartFile file);

    /**
     * Get theme config JSON for current tenant.
     * Returns only the AI-generated theme configuration.
     *
     * @return theme config JSON string (or null if not set)
     */
    String getThemeConfig();
}
