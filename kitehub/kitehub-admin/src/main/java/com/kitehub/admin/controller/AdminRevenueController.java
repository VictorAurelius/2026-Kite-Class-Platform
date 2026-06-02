package com.kitehub.admin.controller;

import com.kitehub.admin.dto.RevenuePeriod;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.admin.dto.RevenueSummaryResponse;
import com.kitehub.admin.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Admin revenue v1 REST API — exposes revenue analytics at the canonical
 * {@code /api/v1/admin/revenue} path expected by frontend + integration consumers.
 *
 * <p>Fixes Wave 90 walkthrough sub-finding (404 at {@code /api/v1/admin/revenue}): legacy
 * {@link AdminController} mounts revenue API at {@code /api/platform/admin/revenue}; this
 * v1 controller provides the canonical path. Both prefixes coexist in Phase 1 BETA —
 * legacy path deprecation deferred to Phase 1.5+ when frontend consolidation complete.</p>
 *
 * <p>Per Wave 92 Bucket D, this controller provides revenue report + summary stats GET
 * endpoints. Delegates to {@link AnalyticsService} for cached analytics (5-min TTL).</p>
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/revenue")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin v1 - Revenue", description = "Admin revenue analytics + summary (Wave 92 Bucket D — Wave 90 404 fix)")
public class AdminRevenueController {

    private final AnalyticsService analyticsService;

    /**
     * Get revenue report for a period.
     *
     * <p>Accepts a typed {@link RevenuePeriod} (GAP-654) so Spring rejects invalid period values
     * with a 400 instead of forwarding arbitrary strings to {@link AnalyticsService}.</p>
     *
     * @param period    report period enum (DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY); defaults to MONTHLY
     * @param startDate start date (ISO yyyy-MM-dd); defaults to current month start
     * @param endDate   end date (ISO yyyy-MM-dd); defaults to today
     * @return revenue report
     */
    @GetMapping
    @Operation(summary = "Get revenue report", description = "Revenue analytics for a period (DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY)")
    public ResponseEntity<RevenueReport> getRevenue(
            @RequestParam(defaultValue = "MONTHLY") RevenuePeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        log.info("Admin v1 revenue report: {} from {} to {}", period, startDate, endDate);

        RevenueReport report = analyticsService.getRevenueReport(period.name(), startDate, endDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Get revenue summary stats (lightweight version of full report — current revenue snapshot).
     *
     * <p>Useful for dashboard widget khi không cần full daily breakdown. Returns a typed
     * {@link RevenueSummaryResponse} (GAP-654) — a lean projection of {@link RevenueReport} — instead
     * of the full report. Accepts a typed {@link RevenuePeriod} param (default MONTHLY); the current
     * snapshot range stays current-month-to-today, preserving the prior business semantics.</p>
     *
     * @param period aggregation period enum (default MONTHLY)
     * @return revenue summary stats
     */
    @GetMapping("/summary")
    @Operation(summary = "Revenue summary stats", description = "Current revenue snapshot for dashboard widget")
    public ResponseEntity<RevenueSummaryResponse> getRevenueSummary(
            @RequestParam(defaultValue = "MONTHLY") RevenuePeriod period
    ) {
        log.info("Admin v1 revenue summary (current month snapshot, period={})", period);

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();
        RevenueReport report = analyticsService.getRevenueReport(period.name(), startDate, endDate);
        return ResponseEntity.ok(RevenueSummaryResponse.fromReport(report));
    }
}
