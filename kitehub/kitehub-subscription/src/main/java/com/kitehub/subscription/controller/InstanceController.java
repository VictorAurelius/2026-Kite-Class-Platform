package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.CursorPage;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.PurgeResult;
import com.kitehub.subscription.dto.RegisterInstanceRequest;
import com.kitehub.subscription.dto.RegisterInstanceResponse;
import com.kitehub.subscription.dto.TrialStatusResponse;
import com.kitehub.subscription.dto.UpdateInstanceRequest;
import com.kitehub.subscription.security.TenantOwnershipGuard;
import com.kitehub.subscription.service.InstancePurgeService;
import com.kitehub.subscription.service.InstanceService;
import com.kitehub.subscription.service.TrialService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for instance management.
 *
 * <p>SLO Tier B (mixed list/detail reads + writes; class-level tag uses Tier B
 * as the dominant read pattern). Per-method tag overrides are acceptable.
 * See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/platform/instances")
@RequiredArgsConstructor
@Tag(name = "Instances", description = "Instance provisioning, trial management, and CRUD operations")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-b", "controller", "instance"})
public class InstanceController {

    private final InstanceService instanceService;
    private final TrialService trialService;
    private final InstancePurgeService instancePurgeService;

    // GAP-1050 (Wave security-3, residual of GAP-1025): the mutation + owner-enumeration
    // endpoints below previously had ZERO authz — any authenticated caller could update
    // another tenant's instance (cross-tenant write) or enumerate another user's instances.
    // OWNER_AUTHZ is the role-level gate; cross-tenant/cross-user binding is enforced via
    // TenantOwnershipGuard against the gateway-trusted X-Tenant-Id / X-User-Id headers
    // (GAP-814 TenantHeaderGuardFilter). Platform admins bypass (manage every instance).
    static final String OWNER_AUTHZ = "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /**
     * Create a new trial instance.
     *
     * @param request create instance request
     * @return created instance response
     */
    /** Default page size for the instance admin list (GAP-432). */
    private static final int DEFAULT_PAGE_SIZE = 50;
    /** Hard cap on page size to prevent re-introducing unbounded scans. */
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * List instances (non-deleted), paginated.
     *
     * <p><strong>GAP-432:</strong> previously returned a flat
     * {@code List<InstanceResponse>} backed by {@code findAll()}. Now returns
     * a {@link Page} envelope with explicit {@code page} + {@code size} bounds
     * (default 50, capped at 200).</p>
     *
     * @param page Zero-based page index (default 0)
     * @param size Page size (default 50, capped at 200)
     * @return page of instance responses
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
    public ResponseEntity<Page<InstanceResponse>> listInstances(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InstanceResponse> responses = instanceService.listAllInstances(pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * Cursor (keyset) variant for very large instance lists — Wave 85 Bucket D D-AC1.
     *
     * <p>When the platform exceeds ~1M instance rows, OFFSET pagination incurs
     * a linear skip cost. This endpoint accepts an opaque base64 cursor (decoded
     * into the last-seen {@code id}) and returns the next page via keyset query.
     * Order is fixed {@code id ASC}.</p>
     *
     * @param cursor opaque cursor token from prior response (null/blank for first page)
     * @param size   page size (defaults 50, capped at 200)
     * @return CursorPage envelope of {@link InstanceResponse}
     */
    @GetMapping(params = "cursor")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
    public ResponseEntity<CursorPage<InstanceResponse>> listInstancesByCursor(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        UUID cursorId = CursorPage.decodeCursor(cursor);
        CursorPage<InstanceResponse> page = instanceService.listInstancesByCursor(cursorId, safeSize);
        return ResponseEntity.ok(page);
    }

    // GAP-1525 (OWASP A01): platform-admin-only instance creation, matching the sibling
    // list/delete/extend-trial/purge endpoints. Self-service signup is POST /register (below).
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
    public ResponseEntity<InstanceResponse> createInstance(@Valid @RequestBody CreateInstanceRequest request) {
        InstanceResponse response = instanceService.createTrialInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Register a new trial instance (self-service registration).
     * Creates owner and instance in one step.
     *
     * @param request registration request
     * @return registration response with user info and tokens
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterInstanceResponse> registerInstance(@Valid @RequestBody RegisterInstanceRequest request) {
        RegisterInstanceResponse response = instanceService.registerInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get instance by ID.
     *
     * @param id instance UUID
     * @return instance response
     */
    @GetMapping("/{id}")
    public ResponseEntity<InstanceResponse> getInstanceById(@PathVariable UUID id) {
        InstanceResponse response = instanceService.getInstanceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get instance by subdomain.
     *
     * @param subdomain subdomain
     * @return instance response
     */
    @GetMapping("/subdomain/{subdomain}")
    public ResponseEntity<InstanceResponse> getInstanceBySubdomain(@PathVariable String subdomain) {
        InstanceResponse response = instanceService.getInstanceBySubdomain(subdomain);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all instances for owner.
     *
     * <p><strong>GAP-1050:</strong> a caller may only enumerate their OWN instances — the
     * {@code ownerId} path variable is bound to the gateway-trusted {@code X-User-Id} header
     * via {@link TenantOwnershipGuard#requireSelfOrAdmin}. Platform admins bypass.</p>
     *
     * @param userHeader gateway-injected {@code X-User-Id} (caller's own user id)
     * @param ownerId    owner UUID
     * @return list of instance responses
     */
    @GetMapping("/owner/{ownerId}")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<List<InstanceResponse>> getInstancesByOwner(
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @PathVariable UUID ownerId
    ) {
        TenantOwnershipGuard.requireSelfOrAdmin(ownerId, userHeader);
        List<InstanceResponse> responses = instanceService.getInstancesByOwner(ownerId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Update instance (owner self-service or platform admin).
     *
     * <p><strong>GAP-1050:</strong> the path {@code {id}} (instance id) is bound to the
     * gateway-trusted {@code X-Tenant-Id} header (caller's own instance id) via
     * {@link TenantOwnershipGuard#requireOwnership} — an OWNER acting on another tenant's
     * instance → 403. Platform admins bypass (manage every instance).</p>
     *
     * @param tenantHeader gateway-injected {@code X-Tenant-Id} (caller's own instance id)
     * @param id           instance UUID
     * @param request      update request
     * @return updated instance response
     */
    @PutMapping("/{id}")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<InstanceResponse> updateInstancePut(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInstanceRequest request
    ) {
        TenantOwnershipGuard.requireOwnership(id, tenantHeader);
        InstanceResponse response = instanceService.updateInstance(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<InstanceResponse> updateInstance(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInstanceRequest request
    ) {
        TenantOwnershipGuard.requireOwnership(id, tenantHeader);
        InstanceResponse response = instanceService.updateInstance(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete instance (soft delete).
     *
     * @param id instance UUID
     * @return no content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteInstance(@PathVariable UUID id) {
        instanceService.deleteInstance(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get trial status for instance.
     *
     * @param id instance UUID
     * @return trial status response
     */
    @GetMapping("/{id}/trial-status")
    public ResponseEntity<TrialStatusResponse> getTrialStatus(@PathVariable UUID id) {
        TrialStatusResponse response = trialService.getTrialStatus(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Extend trial period (admin only).
     *
     * @param id instance UUID
     * @param days number of days to extend
     * @return no content
     */
    @PostMapping("/{id}/extend-trial")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
    public ResponseEntity<Void> extendTrial(
        @PathVariable UUID id,
        @RequestParam int days
    ) {
        trialService.extendTrial(id, days);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently purge a deleted instance (admin only).
     * Removes database, backups, email logs, and publishes cross-service cleanup event.
     * Instance must be in DELETED status. Requires at least one COMPLETED backup.
     *
     * <p>The acting admin's id is forwarded from the gateway-trusted {@code X-User-Id}
     * header to {@link InstancePurgeService#adminPurge(UUID, UUID)} so the
     * {@code TENANT_DELETED} audit row (PDPL Art 23, GAP-954) records a real actor —
     * the {@code admin_audit_log.admin_user_id} column is NOT NULL + FK to {@code users},
     * so a missing actor would FK-violate the audit insert (GAP-954 closure-walk bug).</p>
     *
     * @param id              instance UUID
     * @param adminUserIdHeader acting PLATFORM_ADMIN user id (gateway {@code X-User-Id}; nullable)
     * @return purge result with details
     */
    @DeleteMapping("/{id}/purge")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")
    @Operation(summary = "Permanently purge a deleted instance (admin only)")
    public ResponseEntity<PurgeResult> purgeInstance(
        @PathVariable UUID id,
        @RequestHeader(value = "X-User-Id", required = false) String adminUserIdHeader
    ) {
        UUID adminUserId = parseUuidOrNull(adminUserIdHeader);
        PurgeResult result = instancePurgeService.adminPurge(id, adminUserId);
        return ResponseEntity.ok(result);
    }

    /** Parse a gateway-forwarded {@code X-User-Id} header to UUID, or {@code null} if absent/malformed. */
    private static UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
