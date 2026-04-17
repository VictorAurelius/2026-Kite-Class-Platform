/**
 * Feature detection hook for multi-tenant SaaS.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api-client';
import type { InstanceConfig, FeatureName, PricingTier } from '@/types/feature-detection';
import { TIER_FEATURES } from '@/types/feature-detection';

export function useFeatureDetection() {
  const { data: config, isLoading, error } = useQuery<InstanceConfig>({
    queryKey: ['instance', 'config'],
    queryFn: async () => {
      const response = await apiClient.get('/api/v1/instance/config');
      return response.data.data;
    },
    staleTime: 60 * 60 * 1000, // 1 hour
    retry: 2,
  });

  const hasFeature = (feature: FeatureName): boolean => {
    if (!config) return false;
    return config.features[feature] === true;
  };

  const requireFeature = (feature: FeatureName): void => {
    if (!hasFeature(feature)) {
      throw new Error(
        `Tính năng "${feature}" không khả dụng trong gói hiện tại. ` +
        `Vui lòng nâng cấp để sử dụng tính năng này.`
      );
    }
  };

  const getRequiredTier = (feature: FeatureName): PricingTier | null => {
    // Find the lowest tier that provides this feature
    for (const [tier, features] of Object.entries(TIER_FEATURES)) {
      if (features.includes(feature)) {
        return tier as PricingTier;
      }
    }
    return null;
  };

  return {
    config,
    isLoading,
    error,
    hasFeature,
    requireFeature,
    getRequiredTier,
  };
}
