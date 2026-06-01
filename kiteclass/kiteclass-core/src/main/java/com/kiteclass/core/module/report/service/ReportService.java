package com.kiteclass.core.module.report.service;

import com.kiteclass.core.module.report.dto.AttendanceReportResponse;
import com.kiteclass.core.module.report.dto.RevenueReportResponse;

/**
 * Analytics reporting service for the Owner dashboard (GAP-775 Mảng B11).
 *
 * <p>Produces tenant-scoped monthly aggregations. Tenant isolation is enforced by
 * the Hibernate tenant filter active on the underlying entities; callers must run
 * inside a request carrying {@code X-Tenant-Id} (per {@code TenantContext}).
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
public interface ReportService {

    /**
     * Builds the monthly revenue report over the trailing {@code months} window
     * (ending with the current month, inclusive).
     *
     * @param months number of trailing months to include (1..36)
     * @return revenue report with a zero-filled month series + total
     */
    RevenueReportResponse getRevenueReport(int months);

    /**
     * Builds the monthly attendance present-rate report over the trailing
     * {@code months} window (ending with the current month, inclusive).
     *
     * @param months number of trailing months to include (1..36)
     * @return attendance report with a zero-filled month series + overall rate
     */
    AttendanceReportResponse getAttendanceReport(int months);
}
