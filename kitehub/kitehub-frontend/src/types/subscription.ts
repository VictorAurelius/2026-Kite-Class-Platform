import type { SubscriptionTier } from './instance';

export type BillingCycle = 'MONTHLY' | 'ANNUALLY';
export type SubscriptionStatus = 'ACTIVE' | 'CANCELLED' | 'EXPIRED' | 'PENDING';

export interface Subscription {
  id: number;
  instanceId: number;
  tier: SubscriptionTier;
  status: SubscriptionStatus;
  billingCycle: BillingCycle;
  pricePerMonth: number;
  startDate: string;
  endDate: string;
  autoRenew: boolean;
  createdAt: string;
}
