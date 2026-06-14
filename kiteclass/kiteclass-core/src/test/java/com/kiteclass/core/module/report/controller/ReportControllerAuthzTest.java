package com.kiteclass.core.module.report.controller;

import com.kiteclass.core.module.report.dto.AttendanceReportResponse;
import com.kiteclass.core.module.report.dto.MonthlyAttendancePoint;
import com.kiteclass.core.module.report.dto.MonthlyRevenuePoint;
import com.kiteclass.core.module.report.dto.RevenueReportResponse;
import com.kiteclass.core.module.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CI-bound web-slice authorization tests for {@link ReportController} (GAP-1139).
 *
 * <p>The sibling {@code ReportControllerIT} covers ADMIN happy-path + non-admin
 * deny + tenant fail-closed, but (a) it predates the GAP-1139 OWNER fix so it
 * never exercises the {@code OWNER} role, and (b) as a {@code *IT} it is NOT
 * bound to the surefire CI gate (no failsafe configured in this module). This
 * {@code *Test} therefore acts as the CI regression guard for the
 * {@code @PreAuthorize("hasAnyRole('ADMIN','OWNER')")} role gate added for the
 * school OWNER (tenant-admin):
 *
 * <ul>
 *   <li>OWNER → 200 on /reports/revenue + /reports/attendance (the GAP-1139 fix);</li>
 *   <li>non-owner / non-admin (STUDENT) → 403, service never invoked;</li>
 *   <li>OWNER without {@code X-Tenant-Id} → 400 {@code TENANT_NOT_SET}: an OWNER
 *       still cannot run an UNSCOPED cross-tenant aggregate (GAP-1039 fail-closed).
 *       Full per-row tenant data isolation (tenant A's OWNER only sees tenant A
 *       figures) is enforced at the persistence layer by the Hibernate
 *       {@code tenantFilter} and is exercised by the tenant-filter integration
 *       tests, not by this controller slice (service is mocked here).</li>
 * </ul>
 *
 * <p>Mirrors the proven {@code ReportControllerIT} slice config: {@code @WebMvcTest}
 * + {@code @EnableMethodSecurity} so {@code @PreAuthorize} actually fires, with the
 * {@link ReportService} mocked.
 */
@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc
@Import({ReportControllerAuthzTest.TestSecurityConfig.class, ReportControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ReportController OWNER authz (GAP-1139)")
class ReportControllerAuthzTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        ReportService reportService() {
            return Mockito.mock(ReportService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportService reportService;

    /** Gateway-forwarded tenant header carried on every legitimate request. */
    private static final String TENANT_HEADER = UUID.randomUUID().toString();

    @BeforeEach
    void resetMocks() {
        Mockito.reset(reportService);
    }

    private RevenueReportResponse sampleRevenue() {
        return RevenueReportResponse.builder()
                .period("month")
                .months(12)
                .totalRevenue(new BigDecimal("1500000"))
                .points(List.of(MonthlyRevenuePoint.builder()
                        .month("2026-06").amount(new BigDecimal("1500000")).build()))
                .build();
    }

    private AttendanceReportResponse sampleAttendance() {
        return AttendanceReportResponse.builder()
                .period("month")
                .months(12)
                .overallPresentRate(92.5)
                .points(List.of(MonthlyAttendancePoint.builder()
                        .month("2026-06").presentCount(37L).totalCount(40L).presentRate(92.5).build()))
                .build();
    }

    @Test
    @DisplayName("GAP-1139: OWNER → 200 on /reports/revenue (was 403 before tenant-admin fix)")
    @WithMockUser(roles = "OWNER")
    void revenue_owner_returns200() throws Exception {
        when(reportService.getRevenueReport(12)).thenReturn(sampleRevenue());

        mockMvc.perform(get("/api/v1/reports/revenue").header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").value(1500000));

        verify(reportService).getRevenueReport(12);
    }

    @Test
    @DisplayName("GAP-1139: OWNER → 200 on /reports/attendance (was 403 before tenant-admin fix)")
    @WithMockUser(roles = "OWNER")
    void attendance_owner_returns200() throws Exception {
        when(reportService.getAttendanceReport(12)).thenReturn(sampleAttendance());

        mockMvc.perform(get("/api/v1/reports/attendance").header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallPresentRate").value(92.5));

        verify(reportService).getAttendanceReport(12);
    }

    @Test
    @DisplayName("OWASP A01: non-owner / non-admin (STUDENT) → denied revenue (non-2xx, service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void revenue_nonOwner_denied() throws Exception {
        mockMvc.perform(get("/api/v1/reports/revenue").header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc >= 200 && sc < 300) {
                        throw new AssertionError("Non-owner/non-admin must NOT access reports, got " + sc);
                    }
                });

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("OWASP A01: non-owner / non-admin (STUDENT) → denied attendance (non-2xx, service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void attendance_nonOwner_denied() throws Exception {
        mockMvc.perform(get("/api/v1/reports/attendance").header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc >= 200 && sc < 300) {
                        throw new AssertionError("Non-owner/non-admin must NOT access reports, got " + sc);
                    }
                });

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("Tenant isolation: OWNER without X-Tenant-Id → 400 TENANT_NOT_SET (no unscoped cross-tenant aggregate)")
    @WithMockUser(roles = "OWNER")
    void revenue_owner_noTenantHeader_failsClosed() throws Exception {
        mockMvc.perform(get("/api/v1/reports/revenue"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TENANT_NOT_SET"));

        // OWNER is tenant-admin but still bound to its own tenant scope: the
        // aggregate query must never run unfiltered across tenants.
        verifyNoInteractions(reportService);
    }
}
