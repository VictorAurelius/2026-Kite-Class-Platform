package com.kiteclass.core.module.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One data point in the monthly attendance report — present-rate for a single
 * calendar month.
 *
 * <p>{@code month} is ISO {@code YYYY-MM}. {@code presentRate} is a percentage in
 * the closed range {@code [0, 100]} rounded to 1 decimal place (e.g. {@code 92.5}).
 * {@code presentCount} / {@code totalCount} are surfaced so the FE can show the
 * raw fraction ("46/50 buổi") alongside the percentage.
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAttendancePoint {

    /** Calendar month in ISO {@code YYYY-MM} format. */
    private String month;

    /** Count of PRESENT attendance records marked within this month. */
    private long presentCount;

    /** Count of ALL non-deleted attendance records marked within this month. */
    private long totalCount;

    /** PRESENT / total as a percentage in {@code [0, 100]}, 1 decimal; 0 when totalCount=0. */
    private double presentRate;
}
