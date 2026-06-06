package com.kitehub.subscription.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

/**
 * Cross-tenant ownership guard for platform routes that role-gate but never bind a
 * path/body instance id to the caller's tenant.
 *
 * <p><strong>GAP-1015 / GAP-1023 (Wave security-2 Bucket B):</strong> platform endpoints
 * ({@code /api/platform/subscriptions/**}, {@code /api/instances/{id}/domain}) carry an
 * {@code @PreAuthorize} role gate but the controllers accepted a subscription/instance id
 * without ever verifying it belongs to the caller. Any OWNER could read / cancel / downgrade
 * another tenant's subscription, or read / delete another tenant's domain, by guessing the id
 * (OWASP A01 cross-tenant IDOR).</p>
 *
 * <p>The gateway {@code TenantHeaderGuardFilter} already injects a trusted {@code X-Tenant-Id}
 * header from the verified JWT {@code tenantId} claim (= the caller's instance id for an OWNER)
 * and the global {@code RemoveRequestHeader=X-Tenant-Id} default-filter strips any client-sent
 * value first — so this header is authoritative, not spoofable. This guard compares the
 * resource's instance id against that trusted header, mirroring the
 * {@code StaffInvitationController} {@code existing.getTenantId().equals(tenantId)} precedent.</p>
 *
 * <p>Platform admins ({@code PLATFORM_ADMIN} / {@code ADMIN}) manage every instance and carry no
 * single owning tenant, so they bypass the check.</p>
 */
public final class TenantOwnershipGuard {

    private static final Set<String> ADMIN_AUTHORITIES =
            Set.of("ROLE_PLATFORM_ADMIN", "ROLE_ADMIN");

    private TenantOwnershipGuard() {
    }

    /**
     * @return {@code true} when the current authentication carries a platform-admin authority
     *         ({@code ROLE_PLATFORM_ADMIN} / {@code ROLE_ADMIN}) — cross-tenant bypass.
     */
    public static boolean isPlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_AUTHORITIES::contains);
    }

    /**
     * Verify the caller owns {@code resourceInstanceId}. Platform admins bypass.
     *
     * @param resourceInstanceId the instance the resource belongs to (subscription.instanceId,
     *                           domain path {@code {id}}, create-request instanceId, ...)
     * @param tenantHeader       the gateway-trusted {@code X-Tenant-Id} header (caller's instance id)
     * @throws AccessDeniedException (→ HTTP 403) when not admin and the tenant header is
     *                               missing, malformed, or does not match {@code resourceInstanceId}
     */
    public static void requireOwnership(UUID resourceInstanceId, String tenantHeader) {
        if (isPlatformAdmin()) {
            return;
        }
        UUID callerTenant = parseTenant(tenantHeader);
        if (!callerTenant.equals(resourceInstanceId)) {
            throw new AccessDeniedException("Cross-tenant access denied");
        }
    }

    /**
     * Verify the caller is the owner-user {@code resourceOwnerId} themselves. Platform admins bypass.
     *
     * <p><strong>GAP-1050 (Wave security-3, residual of GAP-1025):</strong> owner-enumeration routes
     * such as {@code GET /api/platform/instances/owner/{ownerId}} took an arbitrary {@code ownerId}
     * path variable and listed every instance for that user — any authenticated caller could
     * enumerate another user's instances by guessing their id (OWASP A01 cross-user IDOR). This
     * binds the path {@code ownerId} to the gateway-trusted {@code X-User-Id} header (caller's own
     * user id, injected from the verified JWT {@code sub}/user claim by the gateway
     * {@code TenantHeaderGuardFilter}). The axis differs from {@link #requireOwnership}: this compares
     * the caller's USER id (X-User-Id), not the caller's instance/tenant id (X-Tenant-Id).</p>
     *
     * @param resourceOwnerId the owner-user id the resource is scoped to (path {@code {ownerId}})
     * @param userHeader      the gateway-trusted {@code X-User-Id} header (caller's own user id)
     * @throws AccessDeniedException (→ HTTP 403) when not admin and the user header is
     *                               missing, malformed, or does not match {@code resourceOwnerId}
     */
    public static void requireSelfOrAdmin(UUID resourceOwnerId, String userHeader) {
        if (isPlatformAdmin()) {
            return;
        }
        UUID callerUser = parseUser(userHeader);
        if (!callerUser.equals(resourceOwnerId)) {
            throw new AccessDeniedException("Cross-user access denied");
        }
    }

    private static UUID parseTenant(String tenantHeader) {
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new AccessDeniedException("Tenant context missing");
        }
        try {
            return UUID.fromString(tenantHeader);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Tenant context malformed");
        }
    }

    private static UUID parseUser(String userHeader) {
        if (userHeader == null || userHeader.isBlank()) {
            throw new AccessDeniedException("User context missing");
        }
        try {
            return UUID.fromString(userHeader);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("User context malformed");
        }
    }
}
