package com.kiteclass.core.module.report.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.report.dto.AttendanceReportResponse;
import com.kiteclass.core.module.report.dto.RevenueReportResponse;
import com.kiteclass.core.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Owner-dashboard analytics reports (GAP-775 Mảng B11).
 *
 * <p>Two tenant-wide aggregations:
 * <ul>
 *   <li>{@code GET /api/v1/reports/revenue} — monthly completed-payment revenue</li>
 *   <li>{@code GET /api/v1/reports/attendance} — monthly attendance present-rate</li>
 * </ul>
 *
 * <p><strong>OWASP A01 authorization (per {@code pre-launch-owasp-rest-hardening-checklist.md}
 * §2.1):</strong> reports expose tenant-WIDE aggregated financials + operations, so they are
 * Owner/admin-only. Guarded with {@code @PreAuthorize("hasRole('ADMIN')")} — a role gate, not a
 * per-resource gate, because the resource IS the whole tenant (no per-class/per-student scope to
 * narrow). Tenant isolation itself is enforced by the Hibernate {@code tenantFilter} on the
 * underlying {@code payments} / {@code attendance} tables, so an ADMIN of tenant A never sees
 * tenant B's figures.
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Owner dashboard analytics reports (revenue + attendance)")
public class ReportController {

    private final ReportService reportService;

    /**
     * Monthly revenue report over a trailing window.
     *
     * @param months trailing months to include (default 12, clamped to 1..36 server-side)
     * @return revenue report (zero-filled month series + total)
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Monthly revenue report",
            description = "Tenant-wide SUM of COMPLETED payment amounts grouped by month. "
                    + "Owner/admin only (OWASP A01 role gate).")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @Parameter(description = "Trailing months (default 12, clamped 1..36)")
            @RequestParam(name = "months", defaultValue = "12") int months) {
        log.debug("GET /api/v1/reports/revenue?months={}", months);
        RevenueReportResponse response = reportService.getRevenueReport(months);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Monthly attendance present-rate report over a trailing window.
     *
     * @param months trailing months to include (default 12, clamped to 1..36 server-side)
     * @return attendance report (zero-filled month series + overall rate)
     */
    @GetMapping("/attendance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Monthly attendance present-rate report",
            description = "Tenant-wide PRESENT/total attendance ratio grouped by month. "
                    + "Owner/admin only (OWASP A01 role gate).")
    public ResponseEntity<ApiResponse<AttendanceReportResponse>> getAttendanceReport(
            @Parameter(description = "Trailing months (default 12, clamped 1..36)")
            @RequestParam(name = "months", defaultValue = "12") int months) {
        log.debug("GET /api/v1/reports/attendance?months={}", months);
        AttendanceReportResponse response = reportService.getAttendanceReport(months);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
