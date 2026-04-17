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
      <div role="status" aria-label="Đang tải tính năng">
        <Skeleton className="h-20 w-full" />
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Không thể tải cấu hình tính năng</AlertTitle>
        <AlertDescription>
          Không thể xác minh tính năng. Vui lòng thử lại sau.
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
      <AlertTitle>Tính năng chưa khả dụng</AlertTitle>
      <AlertDescription className="mt-2">
        <p className="mb-4">
          Tính năng <strong>{feature}</strong> không khả dụng trong gói hiện tại
          {requiredTier && ` (yêu cầu gói ${requiredTier} trở lên)`}.
        </p>
        <Button onClick={() => (window.location.href = '/pricing')}>
          Nâng cấp lên {requiredTier || 'Premium'}
        </Button>
      </AlertDescription>
    </Alert>
  );
}
