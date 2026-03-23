export type InstanceStatus = 'TRIAL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED';

// SAAS-16: Custom domain types
export type DomainStatus = 'NONE' | 'PENDING_VERIFY' | 'VERIFIED' | 'FAILED';

export interface DomainVerifyResponse {
  customDomain: string | null;
  verifyToken: string | null;
  verifyRecord: string | null;
  status: DomainStatus;
  verifiedAt: string | null;
  backupUrl: string;
}

export interface Instance {
  id: string;  // UUID from backend
  organizationName: string;
  subdomain: string;
  ownerId: string;  // UUID
  contactEmail: string | null;
  status: InstanceStatus;
  tier: SubscriptionTier;
  trialStartedAt: string | null;
  trialExpiresAt: string | null;
  trialDaysLeft: number | null;
  subscriptionId: string | null;
  subscriptionExpiresAt: string | null;
  isActive: boolean;
  isOnTrial: boolean;
  createdAt: string;
  updatedAt: string;
  // Custom domain settings
  customDomain?: string | null;
  customDomainVerified?: boolean;
}

export type SubscriptionTier = 'FREE' | 'BASIC' | 'PREMIUM' | 'ENTERPRISE';

export interface TrialStatus {
  instanceId: string;
  trialEndDate: string;
  daysRemaining: number;
  warningLevel: 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'EXPIRED';
  expired: boolean;
}

export interface CreateInstanceRequest {
  organizationName: string;
  subdomain: string;
  ownerEmail: string;
  ownerPassword: string;
}
