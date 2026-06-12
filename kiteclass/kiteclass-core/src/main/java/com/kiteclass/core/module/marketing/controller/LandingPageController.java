package com.kiteclass.core.module.marketing.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.dto.response.LandingPageResponse;
import com.kiteclass.core.module.marketing.service.LandingPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Landing Page operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>GET /api/v1/tenants/{id}/landing - Get landing page (public)</li>
 *   <li>PUT /api/v1/tenants/{id}/landing - Update landing page (admin/teacher)</li>
 * </ul>
 *
 * @since 2.10
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/landing")
@RequiredArgsConstructor
@Tag(name = "Marketing - Landing Page", description = "Landing page management APIs")
public class LandingPageController {

    private final LandingPageService landingPageService;

    /**
     * Gets landing page for tenant.
     * Public endpoint - no authentication required.
     *
     * @param tenantId the tenant ID
     * @return ApiResponse with landing page content and HTTP 200
     */
    @GetMapping
    @Operation(summary = "Get landing page", description = "Retrieves landing page content for a tenant (public access)")
    public ApiResponse<LandingPageResponse> getLandingPage(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {
        log.debug("REST request to get landing page for tenant: {}", tenantId);
        LandingPageResponse response = landingPageService.getLandingPage(tenantId);
        return ApiResponse.success(response);
    }

    /**
     * Updates landing page content for tenant.
     * Requires ADMIN or TEACHER role.
     *
     * @param tenantId the tenant ID
     * @param request  the update request with new values
     * @return ApiResponse with updated landing page content and HTTP 200
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Update landing page", description = "Updates landing page content for a tenant (requires ADMIN or TEACHER role)")
    public ApiResponse<LandingPageResponse> updateLandingPage(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateLandingPageRequest request) {
        log.info("REST request to update landing page for tenant: {}", tenantId);
        LandingPageResponse response = landingPageService.updateLandingPage(tenantId, request);
        return ApiResponse.success(response, "Landing page updated successfully");
    }
}
