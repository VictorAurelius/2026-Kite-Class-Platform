package com.kiteclass.core.module.tenantsettings.service;

import com.kiteclass.core.module.tenantsettings.dto.request.UpdateTenantSettingsRequest;
import com.kiteclass.core.module.tenantsettings.dto.response.TenantSettingsResponse;
import jakarta.validation.Valid;

/**
 * Service for per-tenant settings (timezone / locale / Năm học / ...).
 *
 * <p>Tenant scope always derived from {@code TenantContext} (X-Tenant-Id) — never from a
 * caller-supplied path param — for defense-in-depth tenant isolation.
 *
 * @since Wave provisioning-1 (GAP-947)
 */
public interface TenantSettingsService {

    /**
     * Get the current tenant's settings, auto-creating a default row (with Năm học
     * auto-filled) on first access.
     *
     * @return tenant settings
     */
    TenantSettingsResponse getSettings();

    /**
     * Upsert the current tenant's settings (PUT — provided-field-wins merge).
     *
     * @param request fields to apply (null fields keep existing)
     * @return updated tenant settings
     */
    TenantSettingsResponse updateSettings(@Valid UpdateTenantSettingsRequest request);
}
