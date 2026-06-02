package com.kitehub.admin.controller;

import com.kitehub.admin.dto.RevenuePeriod;
import com.kitehub.admin.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Security tests for {@link AdminRevenueController} — verifies OWASP A01 Broken Access Control
 * is enforced at the controller boundary via {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")}.
 *
 * <p>Uses direct Spring Security method security via {@code @EnableMethodSecurity} +
 * {@link SpringExtension} to exercise the {@code @PreAuthorize} annotation at the class level.
 * The controller is registered as a Spring bean so Spring AOP creates a proxy that enforces
 * the {@code @PreAuthorize} check. Non-admin roles must receive {@link AccessDeniedException}.
 * This tests the annotation is present and wired, without requiring a full Spring MVC context.
 * Closes GAP-637 AC §3.</p>
 *
 * @since Wave 97 Bucket A — GAP-637
 */
@ExtendWith(SpringExtension.class)
@org.springframework.context.annotation.Import({
        AdminRevenueControllerSecurityTest.MethodSecurityTestConfig.class
})
@DisplayName("AdminRevenueController — security (GAP-637)")
class AdminRevenueControllerSecurityTest {

    /** Minimal Spring context: enables method security + registers controller as proxied bean. */
    @org.springframework.context.annotation.Configuration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
        @Bean
        public AdminRevenueController adminRevenueController() {
            return new AdminRevenueController(mock(AnalyticsService.class));
        }
    }

    @Autowired
    private AdminRevenueController controller;

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("getRevenue() — TENANT_USER role → AccessDeniedException (OWASP A01 / GAP-637)")
    void getRevenue_tenantUserRole_throwsAccessDenied() {
        assertThatThrownBy(() -> controller.getRevenue(RevenuePeriod.MONTHLY, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("getRevenue() — TEACHER role → AccessDeniedException (OWASP A01 / GAP-637)")
    void getRevenue_teacherRole_throwsAccessDenied() {
        assertThatThrownBy(() -> controller.getRevenue(RevenuePeriod.MONTHLY, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
