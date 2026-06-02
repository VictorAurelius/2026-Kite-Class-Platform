package com.kitehub.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Typed lean response for the admin revenue summary endpoint (GAP-654).
 *
 * <p>A dashboard-widget-friendly projection of {@link RevenueReport}: it carries the current-month
 * revenue snapshot plus the year-over-period framing fields the frontend dashboard expects, without
 * the full daily/tier breakdown of {@code RevenueReport}.</p>
 *
 * <p>Fields the underlying {@code AnalyticsService} does not yet compute (year-to-date,
 * previous-month, growth %) are exposed as {@code null} rather than fabricated values — the
 * pre-existing summary endpoint only produced a current-month report, so this response preserves
 * those business semantics while still presenting a typed, springdoc-discoverable contract.
 * Populating the deferred fields is tracked for Phase 1.5+ when richer analytics land.</p>
 *
 * @param ytdRevenueVnd            year-to-date revenue (VND); {@code null} until analytics computes it
 * @param currentMonthRevenueVnd   current-month revenue (VND) — mapped from the report total
 * @param previousMonthRevenueVnd  previous-month revenue (VND); {@code null} until analytics computes it
 * @param growthPercentage         month-over-month growth %; {@code null} until analytics computes it
 * @param currency                 ISO currency code (always {@code VND} for Phase 1 BETA)
 * @param period                   aggregation period of the snapshot
 * @param asOfDate                 the date the snapshot reflects (report end date)
 * @since 1.0
 */
public record RevenueSummaryResponse(
        BigDecimal ytdRevenueVnd,
        BigDecimal currentMonthRevenueVnd,
        BigDecimal previousMonthRevenueVnd,
        BigDecimal growthPercentage,
        String currency,
        RevenuePeriod period,
        LocalDate asOfDate
) {

    /**
     * Map a current-month {@link RevenueReport} to the lean summary shape.
     *
     * <p>Adapter keeping the mapping in one place so the controller stays thin. Only fields the
     * report actually carries are populated; deferred fields stay {@code null} per the record javadoc.</p>
     *
     * @param report current-month revenue report (must not be {@code null})
     * @return typed summary projection
     */
    public static RevenueSummaryResponse fromReport(RevenueReport report) {
        return new RevenueSummaryResponse(
                null,
                report.getTotalRevenue(),
                null,
                null,
                "VND",
                RevenuePeriod.MONTHLY,
                report.getEndDate()
        );
    }
}
