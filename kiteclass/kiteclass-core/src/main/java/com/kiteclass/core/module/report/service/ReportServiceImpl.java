package com.kiteclass.core.module.report.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.report.dto.AttendanceReportResponse;
import com.kiteclass.core.module.report.dto.MonthlyAttendancePoint;
import com.kiteclass.core.module.report.dto.MonthlyRevenuePoint;
import com.kiteclass.core.module.report.dto.RevenueReportResponse;
import com.kiteclass.core.module.report.repository.AttendanceReportRepository;
import com.kiteclass.core.module.report.repository.RevenueReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default {@link ReportService} implementation.
 *
 * <p>Strategy per report:
 * <ol>
 *   <li>Clamp the requested window to {@code [1, MAX_MONTHS]}.</li>
 *   <li>Compute {@code [from, to)} as the first day of the oldest month → first
 *       day of next month (so "current month so far" is included).</li>
 *   <li>Query the aggregation repository (tenant-scoped automatically).</li>
 *   <li>Re-bucket DB rows into a dense, zero-filled month series.</li>
 * </ol>
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    /** Hard cap on the report window (3 years) — guards against unbounded scans. */
    static final int MAX_MONTHS = 36;

    private final RevenueReportRepository revenueReportRepository;
    private final AttendanceReportRepository attendanceReportRepository;

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(int months) {
        // GAP-1039: resolve the caller's tenant explicitly. Throws TenantNotSetException
        // (→ HTTP 400) when no tenant context is present, so a header-less request can
        // never aggregate revenue across all tenants.
        UUID tenantId = TenantContext.getCurrentTenant();
        int window = clampMonths(months);
        YearMonth oldest = YearMonth.now().minusMonths(window - 1L);
        LocalDateTime from = oldest.atDay(1).atStartOfDay();
        LocalDateTime to = YearMonth.now().plusMonths(1).atDay(1).atStartOfDay();

        Map<String, BigDecimal> byMonth = new HashMap<>();
        for (Object[] row : revenueReportRepository.sumCompletedRevenueByMonth(tenantId, from, to)) {
            String key = monthKey(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            byMonth.put(key, toBigDecimal(row[2]));
        }

        List<MonthlyRevenuePoint> points = new ArrayList<>(window);
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < window; i++) {
            YearMonth ym = oldest.plusMonths(i);
            String key = monthKey(ym.getYear(), ym.getMonthValue());
            BigDecimal amount = byMonth.getOrDefault(key, BigDecimal.ZERO);
            total = total.add(amount);
            points.add(MonthlyRevenuePoint.builder().month(key).amount(amount).build());
        }

        log.debug("Revenue report: window={} months, total={}", window, total);
        return RevenueReportResponse.builder()
                .period("month")
                .months(window)
                .totalRevenue(total)
                .points(points)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceReportResponse getAttendanceReport(int months) {
        // GAP-1039: resolve the caller's tenant explicitly. Throws TenantNotSetException
        // (→ HTTP 400) when no tenant context is present, so a header-less request can
        // never aggregate attendance across all tenants.
        UUID tenantId = TenantContext.getCurrentTenant();
        int window = clampMonths(months);
        YearMonth oldest = YearMonth.now().minusMonths(window - 1L);
        LocalDateTime from = oldest.atDay(1).atStartOfDay();
        LocalDateTime to = YearMonth.now().plusMonths(1).atDay(1).atStartOfDay();

        Map<String, long[]> byMonth = new HashMap<>();
        for (Object[] row : attendanceReportRepository.countAttendanceByMonth(tenantId, from, to)) {
            String key = monthKey(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            long present = ((Number) row[2]).longValue();
            long count = ((Number) row[3]).longValue();
            byMonth.put(key, new long[]{present, count});
        }

        List<MonthlyAttendancePoint> points = new ArrayList<>(window);
        long totalPresent = 0;
        long totalAll = 0;
        for (int i = 0; i < window; i++) {
            YearMonth ym = oldest.plusMonths(i);
            String key = monthKey(ym.getYear(), ym.getMonthValue());
            long[] counts = byMonth.getOrDefault(key, new long[]{0, 0});
            long present = counts[0];
            long count = counts[1];
            totalPresent += present;
            totalAll += count;
            points.add(MonthlyAttendancePoint.builder()
                    .month(key)
                    .presentCount(present)
                    .totalCount(count)
                    .presentRate(rate(present, count))
                    .build());
        }

        log.debug("Attendance report: window={} months, present={}/{}", window, totalPresent, totalAll);
        return AttendanceReportResponse.builder()
                .period("month")
                .months(window)
                .overallPresentRate(rate(totalPresent, totalAll))
                .points(points)
                .build();
    }

    /** Clamps a caller-supplied month window into {@code [1, MAX_MONTHS]}. */
    private int clampMonths(int months) {
        if (months < 1) {
            return 1;
        }
        return Math.min(months, MAX_MONTHS);
    }

    /** Formats {@code (year, month)} as ISO {@code YYYY-MM}. */
    private String monthKey(int year, int month) {
        return String.format("%04d-%02d", year, month);
    }

    /** PRESENT / total as a percentage in {@code [0,100]} rounded to 1 decimal; 0 when total=0. */
    private double rate(long present, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(present * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Null-safe conversion of a {@code SUM()} result into {@link BigDecimal}. */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }
}
