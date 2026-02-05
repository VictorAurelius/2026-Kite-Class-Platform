package com.kiteclass.core.config;

import com.kiteclass.core.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor that enables Hibernate tenant filter for all requests.
 *
 * <p>For each request:
 * <ol>
 *   <li>Extracts tenant ID from X-Tenant-Id header</li>
 *   <li>Sets tenant ID in TenantContext (ThreadLocal)</li>
 *   <li>Enables Hibernate "tenantFilter" with tenant ID</li>
 *   <li>All queries automatically filtered to current tenant</li>
 *   <li>Clears context after request completion</li>
 * </ol>
 *
 * <p>This ensures multi-tenant data isolation at database query level.
 * Entities without instanceId matching current tenant will never be returned.
 *
 * @author KiteClass Team
 * @since 2.2.0
 * @see com.kiteclass.core.common.entity.BaseEntity
 * @see com.kiteclass.core.common.context.TenantContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(EntityManager.class)
public class TenantFilterInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

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

                // Enable Hibernate filter for this session
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantId);

                log.debug("Tenant filter enabled for tenant: {}", tenantId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid X-Tenant-Id header format: {}", tenantHeader);
                // Let request continue without tenant filter (will fail at service layer)
            }
        } else {
            log.debug("No X-Tenant-Id header found, tenant filter not enabled");
        }

        return true;
    }

    /**
     * After completion: Clear tenant context.
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
        log.debug("Tenant context cleared");
    }
}
