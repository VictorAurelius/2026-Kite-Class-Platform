package com.kiteclass.core.module.marketing.service;

import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.dto.response.LandingPageResponse;
import jakarta.validation.Valid;

import java.util.UUID;

/**
 * Service interface for LandingPage business logic.
 *
 * <p>Business Rule: BR-MKT-001 - Each tenant has ONE landing page.
 * Landing pages are auto-created with default values if not exists.
 *
 * @since 2.10
 */
public interface LandingPageService {

    /**
     * Gets landing page for tenant, creates default if not exists.
     *
     * <p>Implements BR-MKT-001: Each tenant has ONE landing page.
     * If landing page doesn't exist, auto-creates with default values.
     *
     * @param tenantId the tenant ID (instance ID) for multi-tenant isolation
     * @return LandingPageResponse with landing page content
     */
    LandingPageResponse getLandingPage(UUID tenantId);

    /**
     * Updates landing page content for tenant.
     *
     * <p>Only updates non-null fields from request (partial update).
     * Creates landing page with defaults if not exists.
     *
     * @param tenantId the tenant ID (instance ID) for multi-tenant isolation
     * @param request  the update request with new values
     * @return LandingPageResponse with updated landing page content
     */
    LandingPageResponse updateLandingPage(UUID tenantId, @Valid UpdateLandingPageRequest request);

    /**
     * Apply a deployed AI-branding theme onto the tenant's landing page (GAP-1213).
     *
     * <p>Invoked by the {@code branding.deployed} cross-service consumer when kitehub-branding
     * finishes a wizard deploy, so the PUBLIC per-tenant landing actually changes (theme colours
     * + logo) — the broken last mile where "Deploy thành công" never reached the landing.
     * Idempotent: skips when {@code brandingVersion} is not newer than the version already
     * applied (stale/duplicate event). Evicts the {@code landingPages} cache so the next
     * anonymous visitor sees the new theme.</p>
     *
     * @param tenantId        instance UUID (RLS tenant)
     * @param primaryColor    new primary theme colour ({@code #RRGGBB}), nullable = unchanged
     * @param secondaryColor  new secondary theme colour, nullable = unchanged
     * @param logoUrl         new logo URL/object-key, nullable = unchanged
     * @param brandingVersion deploy version for idempotency (nullable = always apply)
     * @return {@code true} if the landing was updated, {@code false} if skipped (stale version)
     */
    boolean applyDeployedBranding(UUID tenantId, String primaryColor, String secondaryColor,
                                  String logoUrl, Integer brandingVersion);
}
