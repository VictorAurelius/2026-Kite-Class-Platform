package com.kitehub.branding.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

/**
 * Cross-tenant ownership guard for branding endpoints that scope by a client-supplied
 * {@code X-Instance-Id} header.
 *
 * <p><strong>GAP-1019 (Wave security-2 Bucket B):</strong> {@code /api/platform/branding/**}
 * endpoints scoped branding jobs + AI usage by an {@code X-Instance-Id} header that the
 * <em>client</em> sends. The gateway neither injects nor strips {@code X-Instance-Id}, so any
 * OWNER could send another tenant's instance id and read/create branding for them (OWASP A01
 * cross-tenant IDOR).</p>
 *
 * <p>The gateway {@code TenantHeaderGuardFilter} DOES inject a trusted {@code X-Tenant-Id} from
 * the verified JWT {@code tenantId} claim (= the caller's instance id for an OWNER) and strips
 * any client-sent {@code X-Tenant-Id}. This guard binds the client-supplied {@code X-Instance-Id}
 * to that trusted header — they must match unless the caller is a platform admin.</p>
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
     * Verify a client-supplied {@code X-Instance-Id} matches the gateway-trusted
     * {@code X-Tenant-Id}. Platform admins bypass (may target any instance).
     *
     * @param instanceIdHeader the client-supplied {@code X-Instance-Id} header value
     * @param tenantHeader     the gateway-trusted {@code X-Tenant-Id} header (caller's instance id)
     * @throws AccessDeniedException (→ HTTP 403) when not admin and the instance header is
     *                               missing, malformed, or does not match the trusted tenant
     */
    public static void requireInstanceOwnership(String instanceIdHeader, String tenantHeader) {
        if (isPlatformAdmin()) {
            return;
        }
        UUID instanceId = parse(instanceIdHeader, "Instance context missing", "Instance context malformed");
        UUID callerTenant = parse(tenantHeader, "Tenant context missing", "Tenant context malformed");
        if (!callerTenant.equals(instanceId)) {
            throw new AccessDeniedException("Cross-tenant access denied");
        }
    }

    /**
     * Bind only when the client supplied an {@code X-Instance-Id}. Used by AI-branding endpoints
     * where the header is optional ({@code required = false}): a non-admin caller MUST NOT scope
     * to another tenant's instance (cross-tenant IDOR — GAP-1019), but omitting the header is
     * allowed (no instance scoping happens → no cross-tenant data access; rate-limit accuracy is a
     * separate concern). Platform admins bypass.
     *
     * @param instanceIdHeader optional client-supplied {@code X-Instance-Id}
     * @param tenantHeader     gateway-trusted {@code X-Tenant-Id}
     */
    public static void requireInstanceOwnershipIfPresent(String instanceIdHeader, String tenantHeader) {
        if (instanceIdHeader == null || instanceIdHeader.isBlank()) {
            return;
        }
        requireInstanceOwnership(instanceIdHeader, tenantHeader);
    }

    /**
     * UUID overload — branding-job endpoints declare {@code @RequestHeader("X-Instance-Id") UUID}.
     */
    public static void requireInstanceOwnership(UUID instanceId, String tenantHeader) {
        if (isPlatformAdmin()) {
            return;
        }
        UUID callerTenant = parse(tenantHeader, "Tenant context missing", "Tenant context malformed");
        if (instanceId == null || !callerTenant.equals(instanceId)) {
            throw new AccessDeniedException("Cross-tenant access denied");
        }
    }

    private static UUID parse(String value, String missingMsg, String malformedMsg) {
        if (value == null || value.isBlank()) {
            throw new AccessDeniedException(missingMsg);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException(malformedMsg);
        }
    }
}
