/**
 * useSubscriptions Hook Tests
 *
 * Tests for subscription-related hooks.
 *
 * @since PR-Q4
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { mockSubscription } from '@/__tests__/mocks/data';
import {
  useActiveSubscription,
  useSubscriptionHistory,
  useUpgradeSubscription,
  useDowngradeSubscription,
  useCancelSubscription,
} from '../use-subscriptions';
import apiClient from '@/lib/api/client';

// Mock API client
vi.mock('@/lib/api/client', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('useSubscriptions hooks', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();

    // Create fresh QueryClient for each test
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

  describe('useActiveSubscription', () => {
    it('fetches active subscription successfully', async () => {
      (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: mockSubscription,
      });

      const { result } = renderHook(() => useActiveSubscription('instance-1'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(mockSubscription);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });

    it('does not fetch when instanceId is undefined', () => {
      const { result } = renderHook(() => useActiveSubscription(undefined), {
        wrapper,
      });

      expect(result.current.isLoading).toBe(false);
      expect(apiClient.get).not.toHaveBeenCalled();
    });

    it('handles fetch error', async () => {
      (apiClient.get as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('API Error')
      );

      const { result } = renderHook(() => useActiveSubscription('instance-1'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toBeDefined();
    });
  });

  describe('useSubscriptionHistory', () => {
    it('fetches subscription history successfully', async () => {
      const mockHistory = [mockSubscription, { ...mockSubscription, id: 'sub-456' }];

      (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: mockHistory,
      });

      const { result } = renderHook(() => useSubscriptionHistory('instance-1'), {
        wrapper,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(mockHistory);
      expect(result.current.data).toHaveLength(2);
    });

    it('does not fetch when instanceId is undefined', () => {
      const { result } = renderHook(() => useSubscriptionHistory(undefined), {
        wrapper,
      });

      expect(result.current.isLoading).toBe(false);
      expect(apiClient.get).not.toHaveBeenCalled();
    });
  });

  describe('useUpgradeSubscription', () => {
    it('upgrades subscription successfully', async () => {
      const upgradedSub = { ...mockSubscription, tier: 'PREMIUM' };

      (apiClient.patch as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: upgradedSub,
      });

      const { result } = renderHook(() => useUpgradeSubscription(), {
        wrapper,
      });

      result.current.mutate({
        subscriptionId: 'sub-123',
        newTier: 'PREMIUM',
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(upgradedSub);
      expect(apiClient.patch).toHaveBeenCalledWith(
        expect.stringContaining('sub-123'),
        { newTier: 'PREMIUM' }
      );
    });

    it('handles upgrade error', async () => {
      (apiClient.patch as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Upgrade failed')
      );

      const { result } = renderHook(() => useUpgradeSubscription(), {
        wrapper,
      });

      result.current.mutate({
        subscriptionId: 'sub-123',
        newTier: 'PREMIUM',
      });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toBeDefined();
    });
  });

  describe('useDowngradeSubscription', () => {
    it('downgrades subscription successfully', async () => {
      const downgradedSub = { ...mockSubscription, tier: 'BASIC' };

      (apiClient.patch as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: downgradedSub,
      });

      const { result } = renderHook(() => useDowngradeSubscription(), {
        wrapper,
      });

      result.current.mutate({
        subscriptionId: 'sub-123',
        newTier: 'BASIC',
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(downgradedSub);
    });
  });

  describe('useCancelSubscription', () => {
    it('cancels subscription successfully', async () => {
      (apiClient.delete as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true },
      });

      const { result } = renderHook(() => useCancelSubscription(), {
        wrapper,
      });

      result.current.mutate('sub-123');

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(apiClient.delete).toHaveBeenCalledWith(
        expect.stringContaining('sub-123')
      );
    });

    it('handles cancel error', async () => {
      (apiClient.delete as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Cancel failed')
      );

      const { result } = renderHook(() => useCancelSubscription(), {
        wrapper,
      });

      result.current.mutate('sub-123');

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toBeDefined();
    });
  });
});
