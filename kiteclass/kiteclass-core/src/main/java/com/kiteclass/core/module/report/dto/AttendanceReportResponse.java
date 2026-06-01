package com.kiteclass.core.module.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Attendance report response — monthly present-rate series plus an overall rate.
 *
 * <p>Powers the Owner dashboard "Tỷ lệ điểm danh" KPI card + 12-month chart
 * (GAP-775 Mảng B11). Tenant-scoped automatically via the Hibernate tenant
 * filter on {@code attendance.instance_id}.
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportResponse {

    /** Aggregation period granularity — currently always {@code "month"}. */
    private String period;

    /** Number of trailing months covered by {@link #points}. */
    private int months;

    /** Present-rate across the whole window ({@code sum present / sum total} × 100, 1 decimal). */
    private double overallPresentRate;

    /** One {@link MonthlyAttendancePoint} per covered month, oldest → newest. */
    private List<MonthlyAttendancePoint> points;
}
