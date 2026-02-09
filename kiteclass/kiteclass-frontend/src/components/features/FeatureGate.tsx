/**
 * Feature gate component for multi-tenant SaaS.
 *
 * Shows content only if feature is available on current pricing tier.
 * Shows upgrade prompt otherwise.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { useFeatureDetection } from '@/hooks/useFeatureDetection';
import type { FeatureName } from '@/types/feature-detection';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Lock } from 'lucide-react';
import type { ReactNode } from 'react';

interface FeatureGateProps {
  feature: FeatureName;
  children: ReactNode;
  fallback?: ReactNode;
}

export function FeatureGate({ feature, children, fallback }: FeatureGateProps) {
  const { isLoading, error, hasFeature, getRequiredTier } = useFeatureDetection();

  // Loading state
  if (isLoading) {
    return (
      <div role="status" aria-label="Loading feature availability">
        <Skeleton className="h-20 w-full" />
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Failed to load feature configuration</AlertTitle>
        <AlertDescription>
          Unable to verify feature availability. Please try again later.
        </AlertDescription>
      </Alert>
    );
  }

  // Feature available - show content
  if (hasFeature(feature)) {
    return <>{children}</>;
  }

  // Feature not available - show fallback or upgrade prompt
  if (fallback) {
    return <>{fallback}</>;
  }

  const requiredTier = getRequiredTier(feature);

  return (
    <Alert>
      <Lock className="h-4 w-4" />
      <AlertTitle>Feature Not Available</AlertTitle>
      <AlertDescription className="mt-2">
        <p className="mb-4">
          The <strong>{feature}</strong> feature is not available on your current plan
          {requiredTier && ` (requires ${requiredTier} tier or higher)`}.
        </p>
        <Button onClick={() => (window.location.href = '/pricing')}>
          Upgrade to {requiredTier || 'Premium'}
        </Button>
      </AlertDescription>
    </Alert>
  );
}
