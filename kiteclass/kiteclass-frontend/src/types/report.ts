/**
 * Analytics report types — Owner dashboard (revenue + attendance).
 *
 * Mirrors backend `RevenueReportResponse` + `AttendanceReportResponse`
 * (GAP-775 ReportController). Contract:
 * `documents/01-business/kiteclass/analytics-report/api-contract.md`.
 *
 * @since GAP-865 (KC reports FE page)
 */

/** One month of revenue (zero-filled when no completed payments). */
export interface RevenuePoint {
  /** ISO `YYYY-MM`, oldest → newest. */
  month: string;
  /** Revenue for that month in VND (0 if empty). */
  amount: number;
}

/** Monthly revenue report over a trailing window. */
export interface RevenueReport {
  /** Always `"month"` for Phase 1. */
  period: string;
  /** Actual window after server-side clamp [1, 36]. */
  months: number;
  /** Sum of `points[].amount` over the window (VND). */
  totalRevenue: number;
  points: RevenuePoint[];
}

/** One month of attendance present-rate. */
export interface AttendancePoint {
  /** ISO `YYYY-MM`, oldest → newest. */
  month: string;
  presentCount: number;
  totalCount: number;
  /** PRESENT/total × 100, 1 decimal; 0 when totalCount = 0. */
  presentRate: number;
}

/** Monthly attendance present-rate report over a trailing window. */
export interface AttendanceReport {
  /** Always `"month"` for Phase 1. */
  period: string;
  /** Actual window after server-side clamp [1, 36]. */
  months: number;
  /** Overall present-rate percent [0,100], 1 decimal, over the window. */
  overallPresentRate: number;
  points: AttendancePoint[];
}
