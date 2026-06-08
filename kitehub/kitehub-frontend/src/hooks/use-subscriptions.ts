import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
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
      // BE trả bare SubscriptionResponse (per api-contract.md) — KHÔNG wrap ApiResponse.
      // 404 = no active subscription (TRIAL tenant chưa nâng cấp) → null, KHÔNG phải error
      // (billing/upgrade page treat null = tier FREE → show plan comparison). GAP-1079.
      try {
        const { data } = await apiClient.get<Subscription>(
          endpoints.subscriptions.active(instanceId!)
        );
        return data;
      } catch (err) {
        if (axios.isAxiosError(err) && err.response?.status === 404) return null;
        throw err;
      }
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
      const { data } = await apiClient.get<Subscription[]>(
        endpoints.subscriptions.byInstance(instanceId!)
      );
      return data;
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
      const { data } = await apiClient.patch<Subscription>(
        endpoints.subscriptions.upgrade(subscriptionId),
        { newTier }
      );
      return data;
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
      const { data } = await apiClient.patch<Subscription>(
        endpoints.subscriptions.downgrade(subscriptionId),
        { newTier }
      );
      return data;
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
      // BE trả bare SubscriptionResponse với pendingPaymentId top-level (per api-contract.md)
      const { data } = await apiClient.post<Subscription>(
        endpoints.subscriptions.create,
        { instanceId, tier, billingCycle, autoRenew: true }
      );
      return data;
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
