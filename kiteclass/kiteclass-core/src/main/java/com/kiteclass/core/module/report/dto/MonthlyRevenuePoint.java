package com.kiteclass.core.module.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One data point in the monthly revenue report — total completed-payment revenue
 * for a single calendar month.
 *
 * <p>{@code month} is ISO {@code YYYY-MM} (e.g. {@code "2026-05"}) so the FE can
 * sort + label chart axes without re-parsing locale-specific dates. {@code amount}
 * is VND (no minor unit) and is rendered FE-side as {@code 1.500.000đ} per
 * {@code vn-localization-audit-checklist.md} §1.
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenuePoint {

    /** Calendar month in ISO {@code YYYY-MM} format. */
    private String month;

    /** Total revenue (VND) from COMPLETED payments completed within this month. */
    private BigDecimal amount;
}
