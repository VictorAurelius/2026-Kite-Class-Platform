package com.kiteclass.core.module.tenantsettings.controller;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.tenantsettings.dto.request.UpdateTenantSettingsRequest;
import com.kiteclass.core.module.tenantsettings.dto.response.TenantSettingsResponse;
import com.kiteclass.core.module.tenantsettings.service.TenantSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for per-tenant settings.
 *
 * <p>Path {@code {id}} is the tenant (instance) UUID. Tenant isolation: the path id MUST
 * match the authenticated tenant (X-Tenant-Id via {@link TenantContext}); the actual data
 * scope is always derived from {@code TenantContext} in the service (defense in depth —
 * a caller cannot read/write another tenant's settings by swapping the path id).
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;

    /**
     * Get settings for a tenant (auto-creates defaults on first access).
     *
     * @param id tenant (instance) UUID from path
     * @return tenant settings
     * @throws PermissionDeniedException if path id != authenticated tenant
     */
    @GetMapping("/{id}/settings")
    public ResponseEntity<ApiResponse<TenantSettingsResponse>> getSettings(@PathVariable UUID id) {
        validateTenantAccess(id);
        return ResponseEntity.ok(ApiResponse.success(tenantSettingsService.getSettings()));
    }

    /**
     * Update settings for a tenant (upsert — provided-field-wins).
     *
     * @param id      tenant (instance) UUID from path
     * @param request fields to apply
     * @return updated tenant settings
     * @throws PermissionDeniedException if path id != authenticated tenant
     */
    @PutMapping("/{id}/settings")
    public ResponseEntity<ApiResponse<TenantSettingsResponse>> updateSettings(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantSettingsRequest request) {
        validateTenantAccess(id);
        return ResponseEntity.ok(ApiResponse.success(tenantSettingsService.updateSettings(request)));
    }

    /**
     * Verify the path tenant id matches the authenticated tenant context.
     *
     * <p>Prevents a caller authenticated for tenant A from reading/writing tenant B's
     * settings by passing B's id in the path (cross-tenant IDOR guard).
     *
     * @param pathTenantId tenant id from path
     * @throws PermissionDeniedException if it doesn't match the current tenant
     */
    private void validateTenantAccess(UUID pathTenantId) {
        UUID currentTenant = TenantContext.getCurrentTenant();
        if (!currentTenant.equals(pathTenantId)) {
            log.warn("TenantSettings.validateTenantAccess: deny — current tenant {} != path {}",
                    currentTenant, pathTenantId);
            throw new PermissionDeniedException("TENANT_ACCESS_DENIED");
        }
    }
}
