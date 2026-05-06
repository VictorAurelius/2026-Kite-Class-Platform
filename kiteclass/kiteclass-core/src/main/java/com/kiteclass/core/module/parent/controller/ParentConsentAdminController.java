package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.parent.dto.BulkBumpConsentRequest;
import com.kiteclass.core.module.parent.dto.BulkBumpConsentResponse;
import com.kiteclass.core.module.parent.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin surface for PDPL re-consent flow (Wave 24 — GAP-361 Phase 1C v1.5).
 *
 * <p>{@code POST /api/v1/admin/parent/consent/bulk-bump} — bulk-bumps the
 * {@code parental_consent.version} field on every non-deleted
 * {@link com.kiteclass.core.module.parent.entity.ParentStudentLink} row in
 * the calling tenant whose stored version is strictly less than the
 * requested {@code newVersion}.
 *
 * <p>Used when the privacy policy is amended (e.g., a new facet is added
 * → all existing parental consents are stale and require re-confirmation
 * before facet APIs return data per BR-PARENT-PORTAL-015).
 *
 * <p>RBAC per BR-PARENT-PORTAL-016: only PRINCIPAL or ADMIN roles may
 * invoke. Tenant scope is enforced via {@link TenantContext} — the bump
 * always targets the active tenant, never cross-tenant.
 *
 * @author KiteClass Team
 * @since 2.24.0 (Wave 24 — GAP-361 Phase 1C v1.5)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/parent/consent")
@RequiredArgsConstructor
@Tag(name = "Parent Consent Admin",
        description = "PDPL re-consent admin tooling (GAP-361 Phase 1C v1.5)")
public class ParentConsentAdminController {

    private final ConsentService consentService;

    @PostMapping("/bulk-bump")
    @Operation(summary = "Bulk-bump parental-consent version for the active tenant",
            description = "BR-PARENT-PORTAL-015 + BR-PARENT-PORTAL-016: PDPL "
                    + "Decree 13/2023 Art 16 K2 d — when policy changes, all "
                    + "parents must re-confirm consent. This endpoint stamps the "
                    + "new policy version onto every non-deleted "
                    + "ParentStudentLink in the active tenant whose stored "
                    + "version is strictly below the new version. Idempotent — "
                    + "links already at or above the new version are left "
                    + "untouched. RBAC: PRINCIPAL or ADMIN role required.")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','OWNER')")
    public ResponseEntity<ApiResponse<BulkBumpConsentResponse>> bulkBump(
            @RequestBody @Valid BulkBumpConsentRequest body) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Instant now = Instant.now();
        int bumped = consentService.bulkBumpVersion(
                tenantId, body.newVersion(), body.reason());
        log.info("Admin bulk-bump: tenant={} bumpedCount={} newVersion={} reason='{}'",
                tenantId, bumped, body.newVersion(), body.reason());
        return ResponseEntity.ok(ApiResponse.success(
                new BulkBumpConsentResponse(bumped, body.newVersion(), now)));
    }
}
