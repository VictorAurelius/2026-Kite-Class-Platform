package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import jakarta.validation.Valid;

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
     * Upload logo for current tenant.
     * Uploads to S3 and updates branding.logoUrl.
     *
     * @param fileUrl presigned S3 URL or file path
     * @return updated branding response
     */
    BrandingResponse uploadLogo(String fileUrl);

    /**
     * Upload favicon for current tenant.
     * Uploads to S3 and updates branding.faviconUrl.
     *
     * @param fileUrl presigned S3 URL or file path
     * @return updated branding response
     */
    BrandingResponse uploadFavicon(String fileUrl);

    /**
     * Get theme config JSON for current tenant.
     * Returns only the AI-generated theme configuration.
     *
     * @return theme config JSON string (or null if not set)
     */
    String getThemeConfig();
}
