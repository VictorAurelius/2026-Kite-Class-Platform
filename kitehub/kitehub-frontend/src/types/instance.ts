export type InstanceStatus = 'TRIAL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED';

export interface Instance {
  id: number;
  organizationName: string;
  subdomain: string;
  ownerEmail: string;
  contactEmail: string | null;
  status: InstanceStatus;
  tier: SubscriptionTier;
  trialEndDate: string | null;
  databaseName: string;
  createdAt: string;
  updatedAt: string;
}

export type SubscriptionTier = 'FREE' | 'BASIC' | 'PREMIUM' | 'ENTERPRISE';

export interface TrialStatus {
  instanceId: number;
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
