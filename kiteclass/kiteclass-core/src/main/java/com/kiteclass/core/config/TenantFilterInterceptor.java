package com.kiteclass.core.config;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.UUID;

/**
 * Interceptor that enables Hibernate tenant filter and sets user context for all requests.
 *
 * <p>For each request:
 * <ol>
 *   <li>Extracts tenant ID from X-Tenant-Id header</li>
 *   <li>Sets tenant ID in TenantContext (ThreadLocal)</li>
 *   <li>Enables Hibernate "tenantFilter" with tenant ID</li>
 *   <li>Extracts user ID from X-User-Id header (forwarded by Gateway)</li>
 *   <li>Sets user ID in UserContext (ThreadLocal)</li>
 *   <li>All queries automatically filtered to current tenant</li>
 *   <li>Clears both contexts after request completion</li>
 * </ol>
 *
 * <p>This ensures multi-tenant data isolation at database query level.
 * Entities without instanceId matching current tenant will never be returned.
 *
 * @author KiteClass Team
 * @since 2.2.0
 * @see com.kiteclass.core.common.entity.BaseEntity
 * @see com.kiteclass.core.common.context.TenantContext
 * @see com.kiteclass.core.common.context.UserContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilterInterceptor implements HandlerInterceptor {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    /**
     * Path prefixes whose handlers aggregate or expose tenant-scoped data and therefore
     * MUST receive a resolvable {@code X-Tenant-Id} (GAP-1039 fail-closed). A header-less
     * request to one of these paths is rejected with HTTP 400 instead of being allowed to
     * run unfiltered (which would leak cross-tenant aggregates).
     *
     * <p>Scoped deliberately narrow — a blanket fail-closed would break legitimately public
     * endpoints (auth, signup, marketing landing, DSAR). New tenant-scoped aggregate
     * endpoints should be added here.
     */
    private static final String[] TENANT_REQUIRED_PATH_PREFIXES = {
        "/api/v1/reports/"
    };

    /**
     * Initialization callback to log EntityManager availability status.
     * Called after bean construction to verify optional EntityManager dependency.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        EntityManager entityManager = entityManagerProvider.getIfAvailable();
        if (entityManager != null) {
            log.info("TenantFilterInterceptor initialized with EntityManager");
        } else {
            log.info("TenantFilterInterceptor initialized without EntityManager (test mode)");
        }
    }

    /**
     * Pre-handle: Extract tenant ID and enable filter.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler chosen handler to execute
     * @return true to continue execution chain
     */
    @Override
    public boolean preHandle(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler
    ) throws IOException {
        String tenantHeader = request.getHeader("X-Tenant-Id");
        boolean tenantResolved = false;

        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                UUID tenantId = UUID.fromString(tenantHeader);
                TenantContext.setCurrentTenant(tenantId);
                tenantResolved = true;

                // Enable Hibernate filter for this session (if EntityManager available)
                EntityManager entityManager = entityManagerProvider.getIfAvailable();
                if (entityManager != null) {
                    Session session = entityManager.unwrap(Session.class);
                    Filter filter = session.enableFilter("tenantFilter");
                    filter.setParameter("tenantId", tenantId);
                    log.debug("Tenant filter enabled for tenant: {}", tenantId);
                } else {
                    log.debug("EntityManager not available, tenant context set but filter not enabled");
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid X-Tenant-Id header format: {}", tenantHeader);
                // Let request continue without tenant filter (will fail at service layer)
            }
        } else {
            log.debug("No X-Tenant-Id header found, tenant filter not enabled");
        }

        // GAP-1039 fail-closed: tenant-scoped aggregate endpoints MUST have a resolvable
        // tenant. Reject early with 400 instead of running unfiltered (cross-tenant leak).
        // Scoped to TENANT_REQUIRED_PATH_PREFIXES so public endpoints are unaffected.
        if (!tenantResolved && requiresTenant(request)) {
            log.warn("Rejecting tenant-scoped request without resolvable X-Tenant-Id: {}",
                    request.getRequestURI());
            writeTenantRequiredError(request, response);
            return false;
        }

        // Set user context from X-User-Id header (for JPA auditing).
        // X-User-Id carries the JWT `sub` claim, a UUID (GAP-795) — not a numeric id.
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader);
                UserContext.setCurrentUser(userId);
                log.debug("User context set for user: {}", userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid X-User-Id header format: {}", userIdHeader);
                // Let request continue without user context (auditing will use null)
            }
        } else {
            log.debug("No X-User-Id header found, user context not set");
        }

        // Set numeric reference id from X-User-Reference-Id header (for ownership authz, GAP-798).
        // = users.reference_id = parents.id / teachers.id / students.id (V1 numeric convention).
        // Nullable: admin/owner are not domain entities. Audit still uses X-User-Id UUID above.
        String referenceIdHeader = request.getHeader("X-User-Reference-Id");
        if (referenceIdHeader != null && !referenceIdHeader.isBlank()) {
            try {
                UserContext.setCurrentReferenceId(Long.valueOf(referenceIdHeader));
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Reference-Id header format: {}", referenceIdHeader);
            }
        }

        return true;
    }

    /**
     * Returns true when the request targets a tenant-scoped path that must not run
     * without a resolved tenant (GAP-1039).
     *
     * @param request current HTTP request
     * @return true if the request URI matches a tenant-required prefix
     */
    private boolean requiresTenant(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        for (String prefix : TENANT_REQUIRED_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a 400 JSON error mirroring the {@code ErrorResponse} shape used by
     * {@code GlobalExceptionHandler.handleTenantNotSet} so clients get a consistent
     * {@code TENANT_NOT_SET} contract whether the request is rejected here or at the
     * service layer.
     *
     * @param request current HTTP request (for the path field)
     * @param response current HTTP response to write the error into
     * @throws IOException if the response writer cannot be obtained
     */
    private void writeTenantRequiredError(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String body = "{\"errorCode\":\"TENANT_NOT_SET\","
                + "\"message\":\"Tenant context not set for current thread. "
                + "Ensure X-Tenant-Id header is provided in request.\","
                + "\"path\":\"" + path + "\"}";
        response.getWriter().write(body);
    }

    /**
     * After completion: Clear tenant and user contexts.
     * Must be called to prevent memory leaks and cross-request data leakage.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler the handler that started async execution
     * @param ex any exception thrown on handler execution, if any
     */
    @Override
    public void afterCompletion(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler,
        Exception ex
    ) {
        TenantContext.clear();
        UserContext.clear();
        log.debug("Tenant and user contexts cleared");
    }
}
