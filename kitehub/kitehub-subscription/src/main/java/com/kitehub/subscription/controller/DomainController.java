package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.DomainSetupRequest;
import com.kitehub.subscription.dto.DomainVerifyResponse;
import com.kitehub.subscription.security.TenantOwnershipGuard;
import com.kitehub.subscription.service.DomainService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for custom domain management.
 *
 * Available to PREMIUM and ENTERPRISE tier instances only.
 * Tier check is performed inside DomainService (business rule enforcement).
 *
 * Endpoints:
 *   POST   /api/instances/{id}/domain          - initiate custom domain setup
 *   POST   /api/instances/{id}/domain/verify   - trigger DNS verification
 *   DELETE /api/instances/{id}/domain          - remove custom domain
 *   GET    /api/instances/{id}/domain          - get domain status
 *
 * <p>SLO Tier C (writes dominate; verify involves async DNS lookup).
 * See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/instances/{id}/domain")
@RequiredArgsConstructor
@Tag(name = "Custom Domain", description = "Custom domain setup and DNS verification (Premium/Enterprise only)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-c", "controller", "domain"})
public class DomainController {

    // KH-7 FM-1: DomainController had ZERO authz — any authenticated user (any role)
    // could read/set/delete any instance's domain. OWNER_AUTHZ is the role-level gate.
    // GAP-1023 (Wave security-2 Bucket B): cross-tenant ownership binding now enforced via
    // TenantOwnershipGuard — the path {id} (instanceId) is compared against the gateway-trusted
    // X-Tenant-Id (caller's instance). Owner A acting on Owner B's instance → 403.
    // Platform admins bypass (manage every instance).
    static final String OWNER_AUTHZ = "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    private final DomainService domainService;

    /**
     * Initiate custom domain setup.
     * Creates a DNS TXT verification token and sets status to PENDING_VERIFY.
     * Only available for PREMIUM and ENTERPRISE instances.
     *
     * @param id      instance UUID
     * @param request domain setup request containing the custom domain
     * @return DomainVerifyResponse with token and DNS instructions
     */
    @Operation(summary = "Initiate custom domain setup",
               description = "Generates a DNS TXT verification token. Premium/Enterprise only.")
    @PreAuthorize(OWNER_AUTHZ)
    @PostMapping
    public ResponseEntity<DomainVerifyResponse> initiateCustomDomain(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id,
        @Valid @RequestBody DomainSetupRequest request
    ) {
        TenantOwnershipGuard.requireOwnership(id, tenantHeader);
        DomainVerifyResponse response = domainService.initiateCustomDomain(id, request.getCustomDomain());
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger DNS verification for pending custom domain.
     * Checks if the TXT record has been added correctly.
     *
     * @param id instance UUID
     * @return DomainVerifyResponse with updated status
     */
    @Operation(summary = "Verify custom domain DNS",
               description = "Checks DNS TXT record. Returns VERIFIED if correct, PENDING if not found yet.")
    @PreAuthorize(OWNER_AUTHZ)
    @PostMapping("/verify")
    public ResponseEntity<DomainVerifyResponse> verifyCustomDomain(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id
    ) {
        TenantOwnershipGuard.requireOwnership(id, tenantHeader);
        DomainVerifyResponse response = domainService.verifyCustomDomain(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove custom domain from instance.
     * Clears all domain fields and resets status to NONE.
     *
     * @param id instance UUID
     * @return 204 No Content on success
     */
    @Operation(summary = "Remove custom domain",
               description = "Removes custom domain and clears all verification data.")
    @PreAuthorize(OWNER_AUTHZ)
    @DeleteMapping
    public ResponseEntity<Void> removeCustomDomain(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id
    ) {
        TenantOwnershipGuard.requireOwnership(id, tenantHeader);
        domainService.removeCustomDomain(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current custom domain status.
     *
     * @param id instance UUID
     * @return DomainVerifyResponse with current domain info and status
     */
    @Operation(summary = "Get custom domain status",
               description = "Returns current domain verification status and info.")
    @PreAuthorize(OWNER_AUTHZ)
    @GetMapping
    public ResponseEntity<DomainVerifyResponse> getDomainStatus(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id
    ) {
        TenantOwnershipGuard.requireOwnership(id, tenantHeader);
        DomainVerifyResponse response = domainService.getDomainStatus(id);
        return ResponseEntity.ok(response);
    }
}
