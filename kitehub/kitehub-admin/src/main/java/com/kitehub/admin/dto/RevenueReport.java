package com.kitehub.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Revenue report DTO.
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReport {

    /**
     * Report period (DAILY, MONTHLY, YEARLY).
     */
    private String period;

    /**
     * Start date of the report.
     */
    private LocalDate startDate;

    /**
     * End date of the report.
     */
    private LocalDate endDate;

    /**
     * Total revenue for the period.
     */
    private BigDecimal totalRevenue;

    /**
     * Revenue by tier (FREE, BASIC, PREMIUM, ENTERPRISE).
     */
    private List<RevenueTierBreakdown> revenueByTier;

    /**
     * Daily revenue data points (for charts).
     */
    private List<DailyRevenue> dailyRevenue;

    /**
     * Monthly Recurring Revenue (MRR).
     */
    private BigDecimal mrr;

    /**
     * Projected Annual Revenue (MRR × 12).
     */
    private BigDecimal projectedArr;

    /**
     * Churn impact (revenue lost due to cancellations).
     */
    private BigDecimal churnImpact;

    /**
     * Revenue by tier breakdown.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueTierBreakdown {
        private String tier;
        private BigDecimal revenue;
        private Long subscriptionCount;
    }

    /**
     * Daily revenue data point.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private LocalDate date;
        private BigDecimal revenue;
    }
}
