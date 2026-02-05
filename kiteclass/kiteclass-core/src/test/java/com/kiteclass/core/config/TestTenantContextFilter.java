package com.kiteclass.core.config;

import com.kiteclass.core.common.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 *   <li>This filter handles multi-tenant context initialization</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestTenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_HEADER);

        try {
            if (tenantId != null && !tenantId.isEmpty()) {
                TenantContext.setCurrentTenant(UUID.fromString(tenantId));
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear context after request
            TenantContext.clear();
        }
    }
}
