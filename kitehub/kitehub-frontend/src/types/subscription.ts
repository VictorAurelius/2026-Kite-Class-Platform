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
