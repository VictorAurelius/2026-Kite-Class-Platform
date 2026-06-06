import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type {
  AdminInstanceSummary,
  AdminPayment,
  DashboardStats,
  RevenueReport,
  ConfirmPaymentRequest,
  RejectPaymentRequest,
} from '@/types/admin';

/**
 * Get admin dashboard statistics.
 */
export function useAdminDashboard() {
  return useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: async () => {
      const { data } = await apiClient.get<DashboardStats>(
        endpoints.admin.dashboard
      );
      return data;
    },
    staleTime: 60000, // 1 minute
  });
}

/**
 * Spring Data `Page` response envelope — Wave 85 Bucket D.
 *
 * Backend returns `Page<T>` from `JpaRepository.findAll(Pageable)`; this matches
 * the on-wire shape so callers can read pagination metadata + content together.
 */
export interface PageEnvelope<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;     // current page index (0-based)
  size: number;
  first: boolean;
  last: boolean;
}

export interface AdminInstancesPageParams {
  page?: number;
  size?: number;
}

/**
 * Get a paginated page of instances for admin (Wave 85 Bucket D).
 *
 * Returns the full `Page<InstanceSummary>` envelope so the UI can render
 * "page N of M" + next/prev controls. Default size 50, max 200 (server-capped).
 */
export function useAdminInstances(params: AdminInstancesPageParams = {}) {
  const page = params.page ?? 0;
  const size = Math.min(params.size ?? 50, 200);
  return useQuery({
    queryKey: ['admin', 'instances', page, size],
    queryFn: async () => {
      const { data } = await apiClient.get<PageEnvelope<AdminInstanceSummary>>(
        endpoints.admin.instances,
        { params: { page, size } }
      );
      return data;
    },
  });
}

/**
 * Get single instance for admin.
 */
export function useAdminInstance(id: string | undefined) {
  return useQuery({
    queryKey: ['admin', 'instances', id],
    queryFn: async () => {
      const { data } = await apiClient.get<AdminInstanceSummary>(
        endpoints.admin.instanceById(id!)
      );
      return data;
    },
    enabled: !!id,
  });
}

/**
 * Suspend an instance (Admin).
 */
export function useSuspendInstance() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (instanceId: string) => {
      const { data } = await apiClient.patch<AdminInstanceSummary>(
        endpoints.admin.suspend(instanceId)
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'instances'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
    },
  });
}

/**
 * Activate an instance (Admin).
 */
export function useActivateInstance() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (instanceId: string) => {
      const { data } = await apiClient.patch<AdminInstanceSummary>(
        endpoints.admin.activate(instanceId)
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'instances'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
    },
  });
}

/**
 * Retry provisioning for a failed/stuck instance (Admin) — GAP-953, UC-PROV-05.
 *
 * Re-publishes tenant.created to re-drive the KiteClass provisioning saga.
 */
export function useRetryProvisioning() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ instanceId, reason }: { instanceId: string; reason?: string }) => {
      const { data } = await apiClient.post<AdminInstanceSummary>(
        endpoints.admin.retryProvisioning(instanceId),
        { reason }
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'instances'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
    },
  });
}

/**
 * Extend trial for an instance.
 */
export function useExtendTrial() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ instanceId, days }: { instanceId: string; days: number }) => {
      const { data } = await apiClient.post<AdminInstanceSummary>(
        endpoints.admin.extendTrial(instanceId),
        { days }
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'instances'] });
    },
  });
}

/**
 * Get pending payments for admin verification.
 */
export function useAdminPendingPayments() {
  return useQuery({
    queryKey: ['admin', 'payments', 'pending'],
    queryFn: async () => {
      const { data } = await apiClient.get<AdminPayment[]>(
        endpoints.admin.pendingPayments
      );
      return data;
    },
    // Auto-refresh every 30 seconds
    refetchInterval: 30000,
  });
}

/**
 * Confirm a payment (Admin).
 */
export function useConfirmPayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      paymentId,
      request,
    }: {
      paymentId: string;
      request: ConfirmPaymentRequest;
    }) => {
      const { data } = await apiClient.post<AdminPayment>(
        endpoints.admin.confirmPayment(paymentId),
        request
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'payments'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'instances'] });
    },
  });
}

/**
 * Reject a payment (Admin).
 */
export function useRejectPayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      paymentId,
      request,
    }: {
      paymentId: string;
      request: RejectPaymentRequest;
    }) => {
      const { data } = await apiClient.post<AdminPayment>(
        endpoints.admin.rejectPayment(paymentId),
        request
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'payments'] });
    },
  });
}

/**
 * Get revenue report.
 */
export function useAdminRevenue(
  period: 'DAILY' | 'MONTHLY' | 'YEARLY',
  startDate: string,
  endDate: string
) {
  return useQuery({
    queryKey: ['admin', 'revenue', period, startDate, endDate],
    queryFn: async () => {
      const params = new URLSearchParams({
        period,
        startDate,
        endDate,
      });
      const { data } = await apiClient.get<RevenueReport>(
        `${endpoints.admin.revenue}?${params}`
      );
      return data;
    },
    enabled: !!startDate && !!endDate,
  });
}
