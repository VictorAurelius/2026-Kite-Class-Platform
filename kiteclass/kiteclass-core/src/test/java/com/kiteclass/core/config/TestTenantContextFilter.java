package com.kiteclass.core.config;

import com.kiteclass.core.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Test filter to set up tenant context from X-Tenant-Id header.
 *
 * <p>This filter is specifically for integration tests where security is disabled
 * but multi-tenant support is still required. In production, TenantFilterInterceptor
 * handles this within the security chain.
 *
 * <p>Best Practice: Separation of Concerns
 * <ul>
 *   <li>Security configuration (TestSecurityConfig) handles authentication/authorization</li>
 *   <li>This filter handles multi-tenant context initialization AND Hibernate filter enablement</li>
 * </ul>
 *
 * <p>Note: EntityManager is optional (required=false). In @WebMvcTest slices:
 * <ul>
 *   <li>TenantContext is still set from header</li>
 *   <li>Hibernate filter is NOT enabled (no JPA/EntityManager)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestTenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired(required = false)
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_HEADER);

        try {
            if (tenantId != null && !tenantId.isEmpty()) {
                UUID tenantUuid = UUID.fromString(tenantId);
                TenantContext.setCurrentTenant(tenantUuid);

                // Enable Hibernate filter (critical for multi-tenant isolation)
                // Only if EntityManager is available (integration tests with JPA)
                if (entityManager != null) {
                    Session session = entityManager.unwrap(Session.class);
                    Filter filter = session.enableFilter("tenantFilter");
                    filter.setParameter("tenantId", tenantUuid);
                    log.debug("Tenant filter enabled for tenant: {}", tenantUuid);
                } else {
                    log.debug("TenantContext set but Hibernate filter not enabled (no EntityManager)");
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear context after request
            TenantContext.clear();
        }
    }
}
