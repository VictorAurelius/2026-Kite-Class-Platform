/**
 * Feature detection types for multi-tenant SaaS platform.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

export enum PricingTier {
  BASIC = 'BASIC',
  STANDARD = 'STANDARD',
  PREMIUM = 'PREMIUM',
}

export enum FeatureName {
  STUDENTS = 'STUDENTS',
  CLASSES = 'CLASSES',
  ATTENDANCE = 'ATTENDANCE',
  ENGAGEMENT = 'ENGAGEMENT',
  AI_BRANDING = 'AI_BRANDING',
  MEDIA = 'MEDIA',
  CUSTOM_DOMAIN = 'CUSTOM_DOMAIN',
}

export interface InstanceConfig {
  instanceId: string;
  tier: PricingTier;
  features: Record<FeatureName, boolean>;
  limitations: {
    maxStudents?: number;
    maxCourses?: number;
    maxStorage?: number; // in MB
  };
  status: InstanceStatus;
  trialDaysRemaining?: number;
  trialExpiresAt?: string;
  suspendedAt?: string;
}

export enum InstanceStatus {
  TRIAL = 'TRIAL',
  ACTIVE = 'ACTIVE',
  GRACE_PERIOD = 'GRACE_PERIOD',
  SUSPENDED = 'SUSPENDED',
}

export interface FeatureRequirement {
  feature: FeatureName;
  requiredTier: PricingTier;
  description: string;
}

// Tier definitions
export const TIER_FEATURES: Record<PricingTier, FeatureName[]> = {
  [PricingTier.BASIC]: [
    FeatureName.STUDENTS,
    FeatureName.CLASSES,
    FeatureName.ATTENDANCE,
  ],
  [PricingTier.STANDARD]: [
    FeatureName.STUDENTS,
    FeatureName.CLASSES,
    FeatureName.ATTENDANCE,
    FeatureName.ENGAGEMENT,
  ],
  [PricingTier.PREMIUM]: [
    FeatureName.STUDENTS,
    FeatureName.CLASSES,
    FeatureName.ATTENDANCE,
    FeatureName.ENGAGEMENT,
    FeatureName.AI_BRANDING,
    FeatureName.MEDIA,
    FeatureName.CUSTOM_DOMAIN,
  ],
};

export const TIER_LIMITS: Record<PricingTier, { maxStudents: number; maxCourses: number }> = {
  [PricingTier.BASIC]: { maxStudents: 50, maxCourses: 10 },
  [PricingTier.STANDARD]: { maxStudents: 200, maxCourses: 50 },
  [PricingTier.PREMIUM]: {
    maxStudents: Number.POSITIVE_INFINITY,
    maxCourses: Number.POSITIVE_INFINITY
  },
};
