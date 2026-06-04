import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { Subscription, PricingTier, BillingCycle } from '@/types/subscription';
import type { ApiResponse } from '@/types/api';

/**
 * Get active subscription for an instance
 */
export function useActiveSubscription(instanceId: string | undefined) {
  return useQuery({
    queryKey: ['subscriptions', 'active', instanceId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<Subscription>>(
        endpoints.subscriptions.active(instanceId!)
      );
      return data.data;
    },
    enabled: !!instanceId,
    staleTime: 30000, // 30s - subscription thay đổi không thường xuyên
  });
}

/**
 * Get subscription history for an instance
 */
export function useSubscriptionHistory(instanceId: string | undefined) {
  return useQuery({
    queryKey: ['subscriptions', 'history', instanceId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<Subscription[]>>(
        endpoints.subscriptions.byInstance(instanceId!)
      );
      return data.data;
    },
    enabled: !!instanceId,
  });
}

/**
 * Upgrade subscription to higher tier
 * Creates pending payment that needs to be completed
 */
export function useUpgradeSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      subscriptionId,
      newTier
    }: {
      subscriptionId: string;
      newTier: PricingTier;
    }) => {
      const { data } = await apiClient.patch<ApiResponse<Subscription>>(
        endpoints.subscriptions.upgrade(subscriptionId),
        { newTier }
      );
      return data.data;
    },
    onSuccess: () => {
      // Invalidate all subscription queries
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
    },
  });
}

/**
 * Downgrade subscription to lower tier
 * Takes effect at end of current billing cycle
 */
export function useDowngradeSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      subscriptionId,
      newTier
    }: {
      subscriptionId: string;
      newTier: PricingTier;
    }) => {
      const { data } = await apiClient.patch<ApiResponse<Subscription>>(
        endpoints.subscriptions.downgrade(subscriptionId),
        { newTier }
      );
      return data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
    },
  });
}

/**
 * Create a new subscription (UC-SUB-01)
 *
 * Use case: Owner trên TRIAL/FREE chưa có subscription row → tạo subscription mới
 * cùng pending payment để chuyển sang gói trả phí (BASIC/PREMIUM/ENTERPRISE).
 *
 * BE endpoint: POST /api/platform/subscriptions
 * Response shape: SubscriptionResponse với pendingPaymentId (UUID dùng để
 * redirect sang `/billing/payment/{pendingPaymentId}` cho VietQR chuyển khoản).
 */
export function useCreateSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      instanceId,
      tier,
      billingCycle,
    }: {
      instanceId: string;
      tier: PricingTier;
      billingCycle: BillingCycle;
    }) => {
      const { data } = await apiClient.post<ApiResponse<Subscription>>(
        endpoints.subscriptions.create,
        { instanceId, tier, billingCycle, autoRenew: true }
      );
      return data.data;
    },
    onSuccess: () => {
      // Invalidate active subscription query so dashboard cập nhật ngay
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
    },
  });
}

/**
 * Cancel subscription
 * Cancels at end of current billing cycle
 */
export function useCancelSubscription() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (subscriptionId: string) => {
      const { data } = await apiClient.delete<ApiResponse<void>>(
        endpoints.subscriptions.cancel(subscriptionId)
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
    },
  });
}
