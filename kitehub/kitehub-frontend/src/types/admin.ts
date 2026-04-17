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
 * Dashboard statistics.
 */
export interface DashboardStats {
  totalInstances: number;
  activeInstances: number;
  trialInstances: number;
  suspendedInstances: number;
  totalRevenue: number;
  monthlyRevenue: number;
  pendingPayments: number;
  newInstancesThisMonth: number;
}

/**
 * Revenue report item.
 */
export interface RevenueReportItem {
  period: string;                      // Date or month string
  revenue: number;
  paymentCount: number;
}

/**
 * Revenue report response.
 */
export interface RevenueReport {
  items: RevenueReportItem[];
  totalRevenue: number;
  period: 'DAILY' | 'MONTHLY' | 'YEARLY';
  startDate: string;
  endDate: string;
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
