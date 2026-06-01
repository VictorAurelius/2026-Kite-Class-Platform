package com.kiteclass.core.module.report.service;

import com.kiteclass.core.module.report.dto.AttendanceReportResponse;
import com.kiteclass.core.module.report.dto.MonthlyAttendancePoint;
import com.kiteclass.core.module.report.dto.MonthlyRevenuePoint;
import com.kiteclass.core.module.report.dto.RevenueReportResponse;
import com.kiteclass.core.module.report.repository.AttendanceReportRepository;
import com.kiteclass.core.module.report.repository.RevenueReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReportServiceImpl} aggregation logic — verifies the
 * dense zero-filled month series, totals, present-rate rounding, month bucketing
 * from raw DB rows, and window clamping (GAP-775).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportServiceImpl")
class ReportServiceImplTest {

    @Mock
    private RevenueReportRepository revenueReportRepository;

    @Mock
    private AttendanceReportRepository attendanceReportRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private static String currentMonthKey() {
        YearMonth now = YearMonth.now();
        return String.format("%04d-%02d", now.getYear(), now.getMonthValue());
    }

    private static String monthKeyAgo(int monthsAgo) {
        YearMonth ym = YearMonth.now().minusMonths(monthsAgo);
        return String.format("%04d-%02d", ym.getYear(), ym.getMonthValue());
    }

    @Test
    @DisplayName("revenue — zero-fills empty months + sums total + ordered oldest→newest")
    void revenue_zeroFillsAndSums() {
        YearMonth now = YearMonth.now();
        // DB has revenue only in the current month (1.500.000) — other 11 months empty.
        when(revenueReportRepository.sumCompletedRevenueByMonth(any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{now.getYear(), now.getMonthValue(), new BigDecimal("1500000")}
                ));

        RevenueReportResponse resp = reportService.getRevenueReport(12);

        assertThat(resp.getPeriod()).isEqualTo("month");
        assertThat(resp.getMonths()).isEqualTo(12);
        assertThat(resp.getPoints()).hasSize(12);
        assertThat(resp.getTotalRevenue()).isEqualByComparingTo("1500000");
        // newest (last) point = current month with the revenue
        MonthlyRevenuePoint last = resp.getPoints().get(11);
        assertThat(last.getMonth()).isEqualTo(currentMonthKey());
        assertThat(last.getAmount()).isEqualByComparingTo("1500000");
        // oldest (first) point = 11 months ago, zero-filled
        MonthlyRevenuePoint first = resp.getPoints().get(0);
        assertThat(first.getMonth()).isEqualTo(monthKeyAgo(11));
        assertThat(first.getAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("revenue — empty DB yields all-zero series + zero total")
    void revenue_emptyDb() {
        when(revenueReportRepository.sumCompletedRevenueByMonth(any(), any()))
                .thenReturn(Collections.emptyList());

        RevenueReportResponse resp = reportService.getRevenueReport(6);

        assertThat(resp.getPoints()).hasSize(6);
        assertThat(resp.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(resp.getPoints()).allSatisfy(p ->
                assertThat(p.getAmount()).isEqualByComparingTo("0"));
    }

    @Test
    @DisplayName("revenue — clamps months <1 to 1 and >36 to 36")
    void revenue_clampsWindow() {
        when(revenueReportRepository.sumCompletedRevenueByMonth(any(), any()))
                .thenReturn(Collections.emptyList());

        assertThat(reportService.getRevenueReport(0).getMonths()).isEqualTo(1);
        assertThat(reportService.getRevenueReport(-5).getMonths()).isEqualTo(1);
        assertThat(reportService.getRevenueReport(100).getMonths()).isEqualTo(36);
    }

    @Test
    @DisplayName("attendance — computes per-month present-rate + overall rate (HALF_UP 1 decimal)")
    void attendance_computesRates() {
        YearMonth now = YearMonth.now();
        // current month: 46 present / 50 total = 92.0%
        when(attendanceReportRepository.countAttendanceByMonth(any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{now.getYear(), now.getMonthValue(), 46L, 50L}
                ));

        AttendanceReportResponse resp = reportService.getAttendanceReport(3);

        assertThat(resp.getPeriod()).isEqualTo("month");
        assertThat(resp.getMonths()).isEqualTo(3);
        assertThat(resp.getPoints()).hasSize(3);

        MonthlyAttendancePoint last = resp.getPoints().get(2);
        assertThat(last.getMonth()).isEqualTo(currentMonthKey());
        assertThat(last.getPresentCount()).isEqualTo(46L);
        assertThat(last.getTotalCount()).isEqualTo(50L);
        assertThat(last.getPresentRate()).isEqualTo(92.0);
        // overall = 46/50 across whole window = 92.0
        assertThat(resp.getOverallPresentRate()).isEqualTo(92.0);
    }

    @Test
    @DisplayName("attendance — rounds 1/3 to 33.3 (HALF_UP) and empty month → rate 0")
    void attendance_roundingAndEmpty() {
        YearMonth now = YearMonth.now();
        when(attendanceReportRepository.countAttendanceByMonth(any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{now.getYear(), now.getMonthValue(), 1L, 3L}
                ));

        AttendanceReportResponse resp = reportService.getAttendanceReport(2);

        MonthlyAttendancePoint current = resp.getPoints().get(1);
        assertThat(current.getPresentRate()).isEqualTo(33.3);
        // older month had no records → rate 0, counts 0
        MonthlyAttendancePoint older = resp.getPoints().get(0);
        assertThat(older.getTotalCount()).isEqualTo(0L);
        assertThat(older.getPresentRate()).isEqualTo(0.0);
        // overall = 1/3 = 33.3
        assertThat(resp.getOverallPresentRate()).isEqualTo(33.3);
    }

    @Test
    @DisplayName("attendance — empty DB yields overall rate 0 (no divide-by-zero)")
    void attendance_emptyDbNoDivideByZero() {
        when(attendanceReportRepository.countAttendanceByMonth(any(), any()))
                .thenReturn(Collections.emptyList());

        AttendanceReportResponse resp = reportService.getAttendanceReport(12);

        assertThat(resp.getOverallPresentRate()).isEqualTo(0.0);
        assertThat(resp.getPoints()).hasSize(12);
    }
}
