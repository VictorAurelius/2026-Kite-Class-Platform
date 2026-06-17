// SubscriptionTier is defined in ./instance.ts but we use PricingTier here
// import type { SubscriptionTier } from './instance';

export type BillingCycle = 'MONTHLY' | 'ANNUALLY';
export type SubscriptionStatus = 'ACTIVE' | 'CANCELLED' | 'EXPIRED' | 'PENDING';
export type PricingTier = 'FREE' | 'BASIC' | 'PREMIUM' | 'ENTERPRISE';

export interface Subscription {
  id: string;                    // UUID from backend
  instanceId: string;            // UUID
  tier: PricingTier;
  status: SubscriptionStatus;
  billingCycle: BillingCycle;
  priceVnd: number;              // Backend uses priceVnd
  startedAt: string;             // Backend uses startedAt/expiresAt
  expiresAt: string;
  autoRenew: boolean;
  pendingTier?: PricingTier;     // Tier waiting to be applied
  pendingPaymentId?: string;
  isActive: boolean;
  isExpired: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TierChangeRequest {
  newTier: PricingTier;
}

/**
 * GAP-1257-FE — "Đang chờ xác nhận" pending-payment status.
 *
 * Contract for `GET /api/platform/subscriptions/instance/{id}/pending-payment-status`
 * (BE-4 will add this endpoint). VietQR manual transfer flow (SUB-11) — after the
 * owner submits a transfer, the payment sits PENDING until a platform admin
 * reconciles the bank statement and confirms (SUB-19, UC-SUB-07). This shape lets
 * the FE show an "awaiting confirmation" screen with the admin-confirm SLA.
 */
export interface PendingPaymentStatus {
  /**
   * GAP-1471 — subscription owning the pending payment; used to cancel it via
   * DELETE /subscriptions/{subscriptionId}/pending-payment. BE populates this in
   * `OwnerBillingService.getPendingPaymentStatus` (`.subscriptionId(sub.getId())`).
   */
  subscriptionId: string;
  pendingPaymentId: string;
  /** GAP-1473 — BE serializes this as `amount` (PendingPaymentStatusResponse.amount). */
  amount: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';
  expiresAt: string | null;
  /**
   * GAP-1473 — admin-confirm SLA in hours (BE sends `adminConfirmSlaHours: long`,
   * NOT a pre-formatted string). Render as "trong vòng {n} giờ làm việc".
   */
  adminConfirmSlaHours: number;
}

export interface PlanDetails {
  tier: PricingTier;
  name: string;
  monthlyPrice: number;
  yearlyPrice: number;
  features: string[];
  limits: string[];
  maxStudents: number;
  maxTeachers: number;
  storageMB: number;
}
