import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { Payment, CreatePaymentRequest, QRCodeResponse } from '@/types/payment';

/**
 * Get payment by ID with auto-polling for PENDING status
 * Auto-refreshes every 5s while payment is PENDING
 */
export function usePayment(paymentId: string | undefined) {
  return useQuery({
    queryKey: ['payments', paymentId],
    queryFn: async () => {
      // BE trả bare Payment (per api-contract.md) — KHÔNG wrap ApiResponse (GAP-1079 sweep)
      const { data } = await apiClient.get<Payment>(
        endpoints.payments.byId(paymentId!)
      );
      return data;
    },
    enabled: !!paymentId,
    // Auto-refetch every 5s if payment is PENDING
    refetchInterval: (query) => {
      const payment = query.state.data;
      return payment?.status === 'PENDING' ? 5000 : false;
    },
    // Stop refetching when tab is not visible (save resources)
    refetchIntervalInBackground: false,
  });
}

/**
 * Get QR code URL for payment
 */
export function usePaymentQRCode(paymentId: string | undefined) {
  return useQuery({
    queryKey: ['payments', paymentId, 'qr-code'],
    queryFn: async () => {
      const { data } = await apiClient.get<QRCodeResponse>(
        endpoints.payments.qrCode(paymentId!)
      );
      return data.qrCodeUrl;
    },
    enabled: !!paymentId,
    staleTime: 300000, // QR code doesn't change for 5 minutes
    retry: 2, // Retry 2 times on failure
  });
}

/**
 * Get payment history for a subscription
 */
export function usePaymentHistory(subscriptionId: string | undefined) {
  return useQuery({
    queryKey: ['payments', 'subscription', subscriptionId],
    queryFn: async () => {
      const { data } = await apiClient.get<Payment[]>(
        endpoints.payments.bySubscription(subscriptionId!)
      );
      return data;
    },
    enabled: !!subscriptionId,
  });
}

/**
 * Create a new payment
 * Used when upgrading subscription
 */
export function useCreatePayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: CreatePaymentRequest) => {
      const { data } = await apiClient.post<Payment>(
        endpoints.payments.create,
        request
      );
      return data;
    },
    onSuccess: () => {
      // Invalidate payment queries
      queryClient.invalidateQueries({ queryKey: ['payments'] });

      // Also invalidate subscriptions since payment affects subscription
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
    },
  });
}
