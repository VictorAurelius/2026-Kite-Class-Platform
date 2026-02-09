package com.kiteclass.core.config;

import com.kiteclass.core.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Filter;
import org.hibernate.Session;
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
 * @author KiteClass Team
 * @since 2.3.1
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TestTenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private final EntityManager entityManager;

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
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantUuid);
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear context after request
            TenantContext.clear();
        }
    }
}
