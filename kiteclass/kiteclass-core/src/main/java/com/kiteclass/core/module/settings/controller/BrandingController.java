package com.kiteclass.core.module.settings.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.service.BrandingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Branding management.
 *
 * @since 2.9
 */
@RestController
@RequestMapping("/api/v1/settings/branding")
@RequiredArgsConstructor
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
     * Upload logo for current tenant.
     * Requires admin role.
     *
     * @param fileUrl presigned S3 URL or file path
     * @return updated branding
     */
    @PostMapping("/logo")
    public ResponseEntity<ApiResponse<BrandingResponse>> uploadLogo(
            @RequestParam("fileUrl") String fileUrl) {
        BrandingResponse branding = brandingService.uploadLogo(fileUrl);
        return ResponseEntity.ok(ApiResponse.success(branding));
    }

    /**
     * Upload favicon for current tenant.
     * Requires admin role.
     *
     * @param fileUrl presigned S3 URL or file path
     * @return updated branding
     */
    @PostMapping("/favicon")
    public ResponseEntity<ApiResponse<BrandingResponse>> uploadFavicon(
            @RequestParam("fileUrl") String fileUrl) {
        BrandingResponse branding = brandingService.uploadFavicon(fileUrl);
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
