package com.kiteclass.core.module.role;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-1274 — STAFF role {@code @PreAuthorize} coverage in kiteclass-core.
 *
 * <p>Before this wave the seeded STAFF template appeared in ZERO endpoint guards, so a
 * STAFF-authority user was denied everywhere. This wave adds STAFF to the staff permission
 * bundle (enrollment + attendance + invoice per GAP-1119) while keeping owner-only /
 * platform-only endpoints (payroll, branding, settings, role-assign) closed to STAFF.
 *
 * <p><strong>Why {@code @SpringBootTest} not {@code @WebMvcTest}:</strong> the sliced
 * {@code @WebMvcTest} + mock-{@code @Bean("authz")} pattern is broken on this repo —
 * the real {@code @Component("authz")} {@code AuthorizationBean} (now carrying a
 * {@code @PersistenceContext EntityManager} for {@code hasAccessToSession}, Wave 105/GAP-1165)
 * is instantiated in the slice and fails with "no EntityManagerFactory" (see the pre-existing
 * {@code AttendanceClassBatchControllerIT} failure). The full-context Testcontainers pattern
 * (mirrors {@code CrossUserAuthzTest}) gives a real JPA-backed authz bean, real method
 * security, and {@code @WithMockUser} role authorities — so the STAFF gates are enforced for real.
 *
 * <p>RLS / Hibernate {@code tenantFilter} is preserved on every query (X-Tenant-Id header set
 * by {@link TestTenantContextFilter}); STAFF gets tenant-wide back-office access to the bundle,
 * NOT class-ownership-scoped access (it is not a class teacher).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        TestContainersConfiguration.class,
        TestSecurityConfig.class,
        TestTenantContextFilter.class,
        StaffRolePreAuthorizeIT.MethodSecurityEnablerConfig.class
})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("GAP-1274 — STAFF @PreAuthorize coverage IT")
class StaffRolePreAuthorizeIT {

    /**
     * Activates {@code @PreAuthorize} evaluation — {@link TestSecurityConfig} enables
     * {@code @EnableWebSecurity} but NOT {@code @EnableMethodSecurity}, so without this the
     * guards are silently ignored (same trick as {@code CrossUserAuthzTest}).
     */
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityEnablerConfig {
        // annotation alone activates the AOP advisor
    }

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT = UUID.randomUUID().toString();

    // ── STAFF allowed on staff-bundle endpoints (invoice + enrollment) ──────

    @Test
    @DisplayName("STAFF CAN list invoices (invoice = staff bundle) — authz passes")
    @WithMockUser(roles = "STAFF")
    void staff_canListInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/invoices").header("X-Tenant-Id", TENANT))
                .andExpect(this::assertNotForbidden);
    }

    @Test
    @DisplayName("STAFF CAN list a class roster via role branch even when not class-owner")
    @WithMockUser(roles = "STAFF")
    void staff_canListEnrollmentsByClass_viaRoleBranch() throws Exception {
        // STAFF is not the class teacher → @authz.hasAccessToClass would deny; the
        // hasAnyRole('STAFF') OR-branch grants tenant-wide back-office access instead.
        mockMvc.perform(get("/api/v1/enrollments/class/999999").header("X-Tenant-Id", TENANT))
                .andExpect(this::assertNotForbidden);
    }

    // ── STAFF denied on owner-only / platform-only endpoints ────────────────

    @Test
    @DisplayName("STAFF CANNOT list payroll configs (owner-only) — 403")
    @WithMockUser(roles = "STAFF")
    void staff_cannotListPayroll() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payroll/configs").header("X-Tenant-Id", TENANT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STAFF CANNOT list role templates (role-assign = owner-only) — 403")
    @WithMockUser(roles = "STAFF")
    void staff_cannotAccessRoleManagement() throws Exception {
        mockMvc.perform(get("/api/v1/roles/templates").header("X-Tenant-Id", TENANT))
                .andExpect(status().isForbidden());
    }

    // ── Baseline: OWNER still reaches owner-only payroll (gate not over-tightened) ─

    @Test
    @DisplayName("OWNER CAN list payroll configs — baseline (no false-negative)")
    @WithMockUser(roles = "OWNER")
    void owner_canListPayroll() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payroll/configs").header("X-Tenant-Id", TENANT))
                .andExpect(this::assertNotForbidden);
    }

    /**
     * Allowed = method-security authz PASSED. We assert the response is NOT a Spring
     * Security 403 (any 2xx/4xx from the service layer proves the {@code @PreAuthorize}
     * guard let the request through). Mirrors {@code CrossUserAuthzTest}'s lenient style.
     */
    private void assertNotForbidden(org.springframework.test.web.servlet.MvcResult result) {
        int sc = result.getResponse().getStatus();
        if (sc == 403) {
            throw new AssertionError("Expected @PreAuthorize to PASS but got 403 Forbidden");
        }
    }
}
