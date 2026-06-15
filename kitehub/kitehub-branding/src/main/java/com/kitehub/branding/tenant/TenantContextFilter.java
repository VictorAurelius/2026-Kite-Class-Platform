package com.kitehub.branding.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Binds {@link TenantContext} per HTTP request from gateway-trusted headers so the
 * {@link TenantAwareDataSourceInterceptor} can set the Postgres RLS GUC at the {@code @Transactional}
 * boundary (GAP-1020 Part 1).
 *
 * <p>Tenant source = {@code X-Tenant-Id} ONLY. The gateway strips any client-supplied
 * {@code X-Tenant-Id} and re-injects it from the verified JWT {@code tenantId} claim
 * (GAP-814 {@code TenantHeaderGuardFilter}). The client-controlled {@code X-Instance-Id} header is
 * deliberately NOT used for the GUC — it is validated against {@code X-Tenant-Id} by
 * {@code TenantOwnershipGuard} in the controller, but the data scope must derive solely from the
 * trusted value. For a non-admin caller {@code X-Instance-Id == X-Tenant-Id}; for a platform admin
 * the {@code app.is_platform_admin} GUC bypasses RLS so cross-instance reads/writes are allowed.</p>
 *
 * <p>Platform-admin detection reuses the gateway-trusted {@code X-User-Roles} header (same header
 * {@code XUserRolesHeaderFilter} builds Spring authorities from — stripped + re-injected by the
 * gateway, never client-supplied).</p>
 *
 * <p>Context is cleared in a {@code finally} so a pooled request thread never leaks tenant state to
 * the next request. {@code shouldNotFilter*Dispatch} are overridden so the binding is re-applied on
 * async (reactive {@code Mono}/{@code Flux}) + error re-dispatch.</p>
 */
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    static final String HEADER_TENANT_ID = "X-Tenant-Id";
    static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            bindTenant(request.getHeader(HEADER_TENANT_ID));
            if (isPlatformAdmin(request.getHeader(HEADER_USER_ROLES))) {
                TenantContext.setPlatformAdmin(true);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void bindTenant(String tenantHeader) {
        if (tenantHeader == null || tenantHeader.isBlank()) {
            return;
        }
        try {
            TenantContext.setCurrentTenant(UUID.fromString(tenantHeader.trim()));
        } catch (IllegalArgumentException ex) {
            // Malformed gateway header — leave unset → default-deny rather than trust a bad value.
            log.warn("Ignoring malformed X-Tenant-Id header (not a UUID)");
        }
    }

    private boolean isPlatformAdmin(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        for (String role : rolesHeader.split(",")) {
            String normalized = role.trim().toUpperCase();
            if (normalized.equals("PLATFORM_ADMIN") || normalized.equals("ROLE_PLATFORM_ADMIN")
                    || normalized.equals("ADMIN") || normalized.equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
