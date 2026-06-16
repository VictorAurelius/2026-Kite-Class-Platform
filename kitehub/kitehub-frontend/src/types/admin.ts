import type { InstanceStatus, SubscriptionTier } from './instance';
import type { PaymentStatus, PaymentMethod } from './payment';

/**
 * Admin instance summary for listing.
 */
export interface AdminInstanceSummary {
  id: string;                         // UUID
  organizationName: string;
  subdomain: string;
  status: InstanceStatus;
  tier: SubscriptionTier;
  ownerEmail: string | null;
  ownerPhone: string | null;
  trialEndDate: string | null;        // ISO datetime
  subscriptionEndDate: string | null;
  databaseUrl: string | null;
  totalUsers: number;
  totalStudents: number;
  totalCourses: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Admin payment for pending payments list.
 */
export interface AdminPayment {
  id: string;                          // UUID
  subscriptionId: string;              // UUID
  amountVnd: number;
  currency: string;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  qrCodeUrl: string | null;
  transactionId: string | null;
  bankCode: string | null;
  accountNumber: string | null;
  accountName: string | null;
  paymentContent: string | null;
  paidAt: string | null;
  createdAt: string;
  updatedAt: string;
  // Extended fields for admin listing
  instanceName?: string;               // Joined from instance
}

/**
 * Dashboard statistics — FE flat view model.
 *
 * GAP-1440: the backend (`GET /api/platform/admin/dashboard`,
 * {@code com.kitehub.admin.dto.DashboardStats}) returns a NESTED shape
 * (`instancesByStatus` map + `mrr`/`arr`). This flat type is the mapped view the
 * dashboard page consumes; mapping lives in `mapDashboardStats` (use-admin.ts).
 *
 * Note: `pendingPayments` is NOT part of the dashboard endpoint — the dashboard
 * page sources that count from the pending-payments list
 * (`useAdminPendingPayments`).
 */
export interface DashboardStats {
  totalInstances: number;
  activeInstances: number;
  trialInstances: number;
  suspendedInstances: number;
  totalRevenue: number;   // mapped from BE `arr` (annual recurring revenue)
  monthlyRevenue: number; // mapped from BE `mrr` (monthly recurring revenue)
  newInstancesThisMonth: number; // mapped from BE `newSignupsLast30Days`
}

/**
 * Backend dashboard stats response — the nested shape returned by
 * `GET /api/platform/admin/dashboard` (kitehub-admin
 * `AnalyticsService.getDashboardStats()`).
 *
 * Contract source of truth: {@code com.kitehub.admin.dto.DashboardStats}.
 * Keep this in sync with that DTO — `mapDashboardStats` + its contract test
 * (`use-admin-dashboard.test.tsx`) guard the FE side against drift.
 */
export interface DashboardStatsResponse {
  totalInstances: number;
  /** Keys: TRIAL / ACTIVE / SUSPENDED / EXPIRED / DELETED. */
  instancesByStatus: Record<string, number>;
  /** Keys: FREE / BASIC / PREMIUM / ENTERPRISE. */
  instancesByTier: Record<string, number>;
  mrr: number;
  arr: number;
  churnRate: number;
  conversionRate: number;
  newSignupsLast30Days: number;
  totalActiveUsers: number;
  revenueByTier: Record<string, number>;
  calculatedAt: string;
}

/**
 * Revenue-by-tier breakdown entry.
 *
 * Contract source: {@code com.kitehub.admin.dto.RevenueReport.RevenueTierBreakdown}.
 */
export interface RevenueTierBreakdown {
  tier: string;                        // FREE / BASIC / PREMIUM / ENTERPRISE
  revenue: number;
  subscriptionCount: number;
}

/**
 * Daily revenue data point (for charts).
 *
 * Contract source: {@code com.kitehub.admin.dto.RevenueReport.DailyRevenue}.
 */
export interface DailyRevenue {
  date: string;                        // ISO date (yyyy-MM-dd)
  revenue: number;
}

/**
 * Revenue report response.
 *
 * GAP-1441: aligned to the backend shape returned by
 * `GET /api/platform/admin/revenue` (kitehub-admin
 * `AnalyticsService.getRevenueReport()` → {@code com.kitehub.admin.dto.RevenueReport}).
 * The prior FE shape (`items: RevenueReportItem[]`) drifted from the BE DTO
 * which exposes `revenueByTier` + `dailyRevenue` + `mrr`/`projectedArr`/`churnImpact`.
 */
export interface RevenueReport {
  period: string;                      // DAILY / WEEKLY / MONTHLY / QUARTERLY / YEARLY
  startDate: string;
  endDate: string;
  totalRevenue: number;
  revenueByTier: RevenueTierBreakdown[];
  dailyRevenue: DailyRevenue[];
  mrr: number;
  projectedArr: number;
  churnImpact: number;
}

/**
 * Confirm payment request (Admin).
 */
export interface ConfirmPaymentRequest {
  transactionId: string;
}

/**
 * Reject payment request (Admin).
 */
export interface RejectPaymentRequest {
  reason: string;
}
