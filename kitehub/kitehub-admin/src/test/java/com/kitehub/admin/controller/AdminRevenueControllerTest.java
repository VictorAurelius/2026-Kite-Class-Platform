package com.kitehub.admin.controller;

import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.admin.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Smoke tests for {@link AdminRevenueController} — verifies HTTP 200 + JSON shape
 * for the canonical {@code /api/v1/admin/revenue} path (Wave 92 Bucket D — fixes Wave 90
 * walkthrough 404 sub-finding).
 *
 * <p>Pure unit-level test using Mockito stubs — avoids full Spring context overhead per
 * existing {@link AdminControllerPaginationTest} pattern.</p>
 */
class AdminRevenueControllerTest {

    private AnalyticsService analyticsService;
    private AdminRevenueController controller;

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        controller = new AdminRevenueController(analyticsService);
    }

    @Test
    void getRevenue_returnsHttp200AndReport() {
        RevenueReport report = RevenueReport.builder()
                .period("MONTHLY")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 18))
                .totalRevenue(new BigDecimal("5000000"))
                .mrr(new BigDecimal("500000"))
                .projectedArr(new BigDecimal("6000000"))
                .churnImpact(BigDecimal.ZERO)
                .revenueByTier(Collections.emptyList())
                .dailyRevenue(Collections.emptyList())
                .build();
        when(analyticsService.getRevenueReport(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(report);

        ResponseEntity<RevenueReport> response = controller.getRevenue(
                "MONTHLY", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPeriod()).isEqualTo("MONTHLY");
        assertThat(response.getBody().getTotalRevenue()).isEqualTo(new BigDecimal("5000000"));
        assertThat(response.getBody().getMrr()).isEqualTo(new BigDecimal("500000"));
    }

    @Test
    void getRevenue_nullDates_defaultsToCurrentMonth() {
        RevenueReport report = RevenueReport.builder()
                .period("MONTHLY")
                .totalRevenue(BigDecimal.ZERO)
                .mrr(BigDecimal.ZERO)
                .projectedArr(BigDecimal.ZERO)
                .churnImpact(BigDecimal.ZERO)
                .revenueByTier(Collections.emptyList())
                .dailyRevenue(Collections.emptyList())
                .build();
        when(analyticsService.getRevenueReport(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(report);

        ResponseEntity<RevenueReport> response = controller.getRevenue("MONTHLY", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Verify default dates applied (startOfMonth + today)
        LocalDate expectedStart = LocalDate.now().withDayOfMonth(1);
        LocalDate expectedEnd = LocalDate.now();
        verify(analyticsService).getRevenueReport("MONTHLY", expectedStart, expectedEnd);
    }

    @Test
    void getRevenueSummary_returnsHttp200AndCurrentMonthReport() {
        RevenueReport report = RevenueReport.builder()
                .period("MONTHLY")
                .totalRevenue(new BigDecimal("3000000"))
                .mrr(new BigDecimal("300000"))
                .projectedArr(new BigDecimal("3600000"))
                .churnImpact(BigDecimal.ZERO)
                .revenueByTier(Collections.emptyList())
                .dailyRevenue(Collections.emptyList())
                .build();
        when(analyticsService.getRevenueReport(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(report);

        ResponseEntity<RevenueReport> response = controller.getRevenueSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMrr()).isEqualTo(new BigDecimal("300000"));
        // Always uses MONTHLY + current month range
        LocalDate expectedStart = LocalDate.now().withDayOfMonth(1);
        LocalDate expectedEnd = LocalDate.now();
        verify(analyticsService).getRevenueReport("MONTHLY", expectedStart, expectedEnd);
    }
}
