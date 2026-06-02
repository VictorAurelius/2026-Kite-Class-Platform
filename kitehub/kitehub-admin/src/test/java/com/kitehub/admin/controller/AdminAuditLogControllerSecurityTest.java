package com.kitehub.admin.controller;

import com.kitehub.subscription.audit.AdminAuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Security tests for {@link AdminAuditLogController} — verifies OWASP A01 Broken Access Control
 * is enforced at the controller boundary via {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")}.
 *
 * <p>Mirrors {@link AdminInstancesControllerSecurityTest}: uses {@code @EnableMethodSecurity} +
 * {@link SpringExtension} so Spring AOP creates a proxy that enforces the {@code @PreAuthorize}
 * check. Non-admin roles must receive {@link AccessDeniedException}. The audit-log viewer
 * exposes a PDPL-sensitive trail — leaking it to non-admins would be a privacy + security
 * breach, so this guard is mandatory (GAP-774 AC §1 role-guard).</p>
 *
 * @since GAP-774
 */
@ExtendWith(SpringExtension.class)
@org.springframework.context.annotation.Import({
        AdminAuditLogControllerSecurityTest.MethodSecurityTestConfig.class
})
@DisplayName("AdminAuditLogController — security (GAP-774)")
class AdminAuditLogControllerSecurityTest {

    /** Minimal Spring context: enables method security + registers controller as proxied bean. */
    @org.springframework.context.annotation.Configuration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
        @Bean
        public AdminAuditLogController adminAuditLogController() {
            return new AdminAuditLogController(mock(AdminAuditLogRepository.class));
        }
    }

    @Autowired
    private AdminAuditLogController controller;

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("listAuditLogs() — TENANT_USER role → AccessDeniedException (OWASP A01 / GAP-774)")
    void listAuditLogs_tenantUserRole_throwsAccessDenied() {
        assertThatThrownBy(() -> controller.listAuditLogs(null, null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("listAuditLogs() — TEACHER role → AccessDeniedException (OWASP A01 / GAP-774)")
    void listAuditLogs_teacherRole_throwsAccessDenied() {
        assertThatThrownBy(() -> controller.listAuditLogs(null, null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("getAuditLog() — TENANT_USER role → AccessDeniedException (OWASP A01 / GAP-774)")
    void getAuditLog_tenantUserRole_throwsAccessDenied() {
        assertThatThrownBy(() -> controller.getAuditLog(1L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
