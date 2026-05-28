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
    ) {
        String tenantHeader = request.getHeader("X-Tenant-Id");

        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                UUID tenantId = UUID.fromString(tenantHeader);
                TenantContext.setCurrentTenant(tenantId);

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

        return true;
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
