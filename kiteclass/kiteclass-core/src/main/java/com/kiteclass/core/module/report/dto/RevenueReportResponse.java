package com.kiteclass.core.module.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Revenue report response — monthly revenue series plus a roll-up total.
 *
 * <p>Powers the Owner dashboard "Doanh thu tháng" KPI card + 12-month chart
 * (GAP-775 Mảng B11). Tenant-scoped automatically via the Hibernate tenant
 * filter on {@code payments.instance_id}.
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {

    /** Aggregation period granularity — currently always {@code "month"}. */
    private String period;

    /** Number of trailing months covered by {@link #points}. */
    private int months;

    /** Sum of {@link #points} amounts — the KPI headline figure (VND). */
    private BigDecimal totalRevenue;

    /** One {@link MonthlyRevenuePoint} per covered month, oldest → newest. */
    private List<MonthlyRevenuePoint> points;
}
