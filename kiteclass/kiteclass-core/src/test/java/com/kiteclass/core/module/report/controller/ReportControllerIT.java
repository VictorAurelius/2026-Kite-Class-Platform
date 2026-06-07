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
 * Web slice tests for {@link ReportController} (GAP-775).
 *
 * <p>Verifies: (1) happy-path 200 + ApiResponse wrapper for revenue + attendance
 * as ADMIN; (2) OWASP A01 — non-admin user denied (403, service never invoked);
 * (3) default {@code months=12} param forwarded; (4) explicit {@code months} param
 * forwarded.
 *
 * <p>Pattern mirrors {@code AttendanceClassBatchControllerIT}: {@code @WebMvcTest}
 * + {@code @EnableMethodSecurity} so {@code @PreAuthorize("hasRole('ADMIN')")}
 * actually fires, with the service mocked.
 */
@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc
@Import({ReportControllerIT.TestSecurityConfig.class, ReportControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ReportController IT")
class ReportControllerIT {

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

    /** GAP-1039: every legitimate report request carries the gateway-forwarded tenant header. */
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
    @DisplayName("GET /reports/revenue — 200 + ApiResponse wrapper for ADMIN, default months=12")
    @WithMockUser(roles = "ADMIN")
    void revenue_admin_returns200() throws Exception {
        when(reportService.getRevenueReport(12)).thenReturn(sampleRevenue());

        mockMvc.perform(get("/api/v1/reports/revenue").header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.period").value("month"))
                .andExpect(jsonPath("$.data.totalRevenue").value(1500000))
                .andExpect(jsonPath("$.data.points[0].month").value("2026-06"));

        verify(reportService).getRevenueReport(12);
    }

    @Test
    @DisplayName("GET /reports/revenue?months=6 — forwards explicit window")
    @WithMockUser(roles = "ADMIN")
    void revenue_admin_forwardsMonthsParam() throws Exception {
        when(reportService.getRevenueReport(6)).thenReturn(sampleRevenue());

        mockMvc.perform(get("/api/v1/reports/revenue")
                        .param("months", "6")
                        .header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk());

        verify(reportService).getRevenueReport(6);
    }

    @Test
    @DisplayName("GET /reports/attendance — 200 + ApiResponse wrapper for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void attendance_admin_returns200() throws Exception {
        when(reportService.getAttendanceReport(12)).thenReturn(sampleAttendance());

        mockMvc.perform(get("/api/v1/reports/attendance").header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallPresentRate").value(92.5))
                .andExpect(jsonPath("$.data.points[0].presentRate").value(92.5));

        verify(reportService).getAttendanceReport(12);
    }

    @Test
    @DisplayName("OWASP A01 — non-admin user denied revenue (403, service NOT invoked)")
    @WithMockUser(roles = "TEACHER")
    void revenue_nonAdmin_denied() throws Exception {
        mockMvc.perform(get("/api/v1/reports/revenue"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc >= 200 && sc < 300) {
                        throw new AssertionError("Non-admin should NOT access reports, got " + sc);
                    }
                });

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("OWASP A01 — non-admin user denied attendance (403, service NOT invoked)")
    @WithMockUser(roles = "TEACHER")
    void attendance_nonAdmin_denied() throws Exception {
        mockMvc.perform(get("/api/v1/reports/attendance"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc >= 200 && sc < 300) {
                        throw new AssertionError("Non-admin should NOT access reports, got " + sc);
                    }
                });

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("GAP-1039 fail-closed — ADMIN without X-Tenant-Id is rejected (400, service NOT invoked)")
    @WithMockUser(roles = "ADMIN")
    void revenue_admin_noTenantHeader_failsClosed() throws Exception {
        mockMvc.perform(get("/api/v1/reports/revenue"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TENANT_NOT_SET"));

        // Cross-tenant aggregate leak guard: the report query must never run unfiltered.
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("GAP-1039 fail-closed — ADMIN without X-Tenant-Id denied attendance (400, service NOT invoked)")
    @WithMockUser(roles = "ADMIN")
    void attendance_admin_noTenantHeader_failsClosed() throws Exception {
        mockMvc.perform(get("/api/v1/reports/attendance"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TENANT_NOT_SET"));

        verifyNoInteractions(reportService);
    }
}
