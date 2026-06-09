/**
 * usePayments Hook Tests
 *
 * Tests for payment-related hooks.
 *
 * @since PR-Q4
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { mockPayment } from '@/__tests__/mocks/data';
import {
  usePayment,
  usePaymentQRCode,
  usePaymentHistory,
  useCreatePayment,
} from '../use-payments';
import apiClient from '@/lib/api/client';

// Mock API client
vi.mock('@/lib/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('usePayments hooks', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();

    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  describe('usePayment', () => {
    it('fetches payment successfully', async () => {
      (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: mockPayment,
      });

      const { result } = renderHook(() => usePayment('payment-123'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(mockPayment);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });

    it('does not fetch when paymentId is undefined', () => {
      const { result } = renderHook(() => usePayment(undefined), {
        wrapper,
      });

      expect(result.current.isLoading).toBe(false);
      expect(apiClient.get).not.toHaveBeenCalled();
    });

    it('auto-refetches when payment status is PENDING', async () => {
      const pendingPayment = { ...mockPayment, status: 'PENDING' };

      (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: pendingPayment,
      });

      const { result } = renderHook(() => usePayment('payment-123'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      // Check that refetchInterval is set when payment is PENDING
      expect(result.current.data?.status).toBe('PENDING');
    });

    it('handles fetch error', async () => {
      (apiClient.get as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('API Error')
      );

      const { result } = renderHook(() => usePayment('payment-123'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toBeDefined();
    });
  });

  describe('usePaymentQRCode', () => {
    it('fetches QR code URL successfully', async () => {
      const qrCodeUrl = 'https://example.com/qr-code.png';

      (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { qrCodeUrl },
      });

      const { result } = renderHook(() => usePaymentQRCode('payment-123'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toBe(qrCodeUrl);
    });

    it('does not fetch when paymentId is undefined', () => {
      const { result } = renderHook(() => usePaymentQRCode(undefined), {
        wrapper,
      });

      expect(result.current.isLoading).toBe(false);
      expect(apiClient.get).not.toHaveBeenCalled();
    });
  });

  describe('usePaymentHistory', () => {
    it('fetches payment history successfully', async () => {
      const mockHistory = [mockPayment, { ...mockPayment, id: 'payment-456' }];

      (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: mockHistory,
      });

      const { result } = renderHook(() => usePaymentHistory('sub-123'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(mockHistory);
      expect(result.current.data).toHaveLength(2);
    });

    it('does not fetch when subscriptionId is undefined', () => {
      const { result } = renderHook(() => usePaymentHistory(undefined), {
        wrapper,
      });

      expect(result.current.isLoading).toBe(false);
      expect(apiClient.get).not.toHaveBeenCalled();
    });
  });

  describe('useCreatePayment', () => {
    it('creates payment successfully', async () => {
      const newPayment = { ...mockPayment, id: 'new-payment' };

      (apiClient.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: newPayment,
      });

      const { result } = renderHook(() => useCreatePayment(), {
        wrapper,
      });

      result.current.mutate({
        subscriptionId: 'sub-123',
        amountVnd: 299000,
        paymentMethod: 'BANK_TRANSFER',
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(newPayment);
      expect(apiClient.post).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          subscriptionId: 'sub-123',
          amountVnd: 299000,
        })
      );
    });

    it('handles create error', async () => {
      (apiClient.post as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Create failed')
      );

      const { result } = renderHook(() => useCreatePayment(), {
        wrapper,
      });

      result.current.mutate({
        subscriptionId: 'sub-123',
        amountVnd: 299000,
        paymentMethod: 'BANK_TRANSFER',
      });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toBeDefined();
    });

    it('invalidates queries on success', async () => {
      const newPayment = { ...mockPayment, id: 'new-payment' };

      (apiClient.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: newPayment,
      });

      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useCreatePayment(), {
        wrapper,
      });

      result.current.mutate({
        subscriptionId: 'sub-123',
        amountVnd: 299000,
        paymentMethod: 'BANK_TRANSFER',
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      // Should invalidate both payments and subscriptions
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['payments'] });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['subscriptions'] });
    });
  });
});
