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

    /**
     * Tracks previous tenant per request thread to detect cross-tenant context switches.
     * When tenant CHANGES between requests, persistence context must be cleared BEFORE
     * flush to avoid @PreUpdate firing on stale managed entities from prior tenant
     * (GAP-746 fix).
     */
    private static final ThreadLocal<UUID> PREVIOUS_TENANT = new ThreadLocal<>();

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
                //
                // Fixed: Now works with native SQL queries that use explicit CAST()
                // Previous issue was JPQL parameter type inference, resolved by using native SQL
                if (entityManager != null) {
                    try {
                        // GAP-746: when tenant CHANGES between requests, clear persistence
                        // context BEFORE flush to detach entities loaded under previous
                        // tenant. Otherwise flush() triggers @PreUpdate
                        // validateInstanceIdOnUpdate on stale managed entities, throwing
                        // InvalidDataAccessApiUsageException on cross-tenant test sequences.
                        // Same-tenant requests retain original flush-then-clear semantics
                        // to preserve mid-test direct repository.save() persistence.
                        //
                        // ⚠️ TEST-SIDE TRAP: the cross-tenant clear() WITHOUT prior flush
                        // discards pending UPDATEs queued in the persistence context by
                        // the test body (e.g. testAssignment.setStatus(PUBLISHED) +
                        // assignmentRepository.save() without intervening flush). The
                        // controller then reads STALE DB state. Tests that modify a
                        // managed entity in the test body BEFORE mockMvc.perform MUST
                        // explicitly call entityManager.flush() to push the UPDATE to DB.
                        // See .claude/skills/testing/testing-standards.md §2.8 for the
                        // required test pattern + symptoms (HTTP 400 unexpected, derived
                        // field assertion fail). Originating incidents: Wave gap-746
                        // closure 2026-05-25 + Wave rst-cleanup 2026-05-27
                        // (AssignmentIntegrationTest 2 sites + InvoiceFlow MT).
                        UUID previousTenant = PREVIOUS_TENANT.get();
                        if (previousTenant != null && !previousTenant.equals(tenantUuid)) {
                            entityManager.clear();
                        } else {
                            entityManager.flush();
                            entityManager.clear();
                        }
                        PREVIOUS_TENANT.set(tenantUuid);

                        Session session = entityManager.unwrap(Session.class);
                        Filter filter = session.enableFilter("tenantFilter");
                        filter.setParameter("tenantId", tenantUuid);
                        log.debug("Tenant filter enabled for tenant: {}", tenantUuid);
                    } catch (Exception e) {
                        log.warn("Failed to enable tenant filter: {}", e.getMessage());
                        // Continue without filter - will fail at service layer if tenant isolation required
                    }
                } else {
                    log.debug("TenantContext set, Hibernate filter not enabled (testing without filter)");
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear context after request
            TenantContext.clear();
            // Note: PREVIOUS_TENANT is intentionally NOT cleared here — it persists
            // across requests within the same test thread to detect cross-tenant
            // context switches (GAP-746).
        }
    }
}
