package com.kiteclass.core.module.settings.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.service.BrandingService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for Branding management.
 *
 * @since 2.9
 */
@RestController
@RequestMapping("/api/v1/settings/branding")
@RequiredArgsConstructor
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-a", "controller", "branding-settings"})
public class BrandingController {

    private final BrandingService brandingService;

    /**
     * Get branding for current tenant.
     * Public endpoint (no authentication required).
     *
     * @return branding response
     */
    @GetMapping
    public ResponseEntity<ApiResponse<BrandingResponse>> getBranding() {
        BrandingResponse branding = brandingService.getBranding();
        return ResponseEntity.ok(ApiResponse.success(branding));
    }

    /**
     * Update branding for current tenant.
     * Requires admin role.
     *
     * @param request update request
     * @return updated branding
     */
    @PutMapping
    public ResponseEntity<ApiResponse<BrandingResponse>> updateBranding(
            @Valid @RequestBody UpdateBrandingRequest request) {
        BrandingResponse branding = brandingService.updateBranding(request);
        return ResponseEntity.ok(ApiResponse.success(branding));
    }

    /**
     * Upload a logo image for the current tenant (multipart).
     * Requires admin role.
     *
     * <p>GAP-804: accepts the raw image as {@code multipart/form-data} field
     * {@code logo}; the service stores it in MinIO and returns the branding
     * with the renderable {@code logoUrl}. Replaces the prior
     * {@code @RequestParam("fileUrl") String} contract that mismatched the FE
     * multipart upload.
     *
     * @param logo multipart logo image (field name {@code logo})
     * @return updated branding
     */
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BrandingResponse>> uploadLogo(
            @RequestPart("logo") MultipartFile logo) {
        BrandingResponse branding = brandingService.uploadLogo(logo);
        return ResponseEntity.ok(ApiResponse.success(branding));
    }

    /**
     * Upload a favicon image for the current tenant (multipart).
     * Requires admin role.
     *
     * <p>GAP-804: accepts the raw image as {@code multipart/form-data} field
     * {@code favicon}; the service stores it in MinIO and returns the branding
     * with the renderable {@code faviconUrl}.
     *
     * @param favicon multipart favicon image (field name {@code favicon})
     * @return updated branding
     */
    @PostMapping(value = "/favicon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BrandingResponse>> uploadFavicon(
            @RequestPart("favicon") MultipartFile favicon) {
        BrandingResponse branding = brandingService.uploadFavicon(favicon);
        return ResponseEntity.ok(ApiResponse.success(branding));
    }

    /**
     * Get theme config JSON for current tenant.
     * Lightweight endpoint that returns only the AI-generated theme configuration.
     * Public endpoint (no authentication required).
     *
     * @return theme config JSON string
     */
    @GetMapping("/theme")
    public ResponseEntity<ApiResponse<String>> getThemeConfig() {
        String themeConfig = brandingService.getThemeConfig();
        return ResponseEntity.ok(ApiResponse.success(themeConfig));
    }
}
