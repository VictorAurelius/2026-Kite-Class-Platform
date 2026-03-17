/**
 * Unit tests for use-instances hooks.
 *
 * @since PR 5.10
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useOwnerInstances, useInstance, useTrialStatus } from '../use-instances';

// Mock apiClient
vi.mock('@/lib/api/client', () => ({
  default: {
    get: vi.fn(),
  },
}));

import apiClient from '@/lib/api/client';

const mockApiClient = apiClient as { get: ReturnType<typeof vi.fn> };

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  });

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    );
  };
}

describe('use-instances', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('useOwnerInstances', () => {
    it('fetches instances for owner when ownerId is provided', async () => {
      const mockInstances = [
        { id: 1, name: 'Test Instance 1' },
        { id: 2, name: 'Test Instance 2' },
      ];

      mockApiClient.get.mockResolvedValueOnce({
        data: { data: mockInstances },
      });

      const { result } = renderHook(() => useOwnerInstances(123), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(mockApiClient.get).toHaveBeenCalledWith('/api/platform/instances/owner/123');
      expect(result.current.data).toEqual(mockInstances);
    });

    it('does not fetch when ownerId is undefined', () => {
      const { result } = renderHook(() => useOwnerInstances(undefined), {
        wrapper: createWrapper(),
      });

      expect(result.current.isLoading).toBe(false);
      expect(result.current.fetchStatus).toBe('idle');
      expect(mockApiClient.get).not.toHaveBeenCalled();
    });

    it('handles error response', async () => {
      mockApiClient.get.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useOwnerInstances(123), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isError).toBe(true);
      });

      expect(result.current.error).toBeInstanceOf(Error);
    });
  });

  describe('useInstance', () => {
    it('fetches single instance when id is provided', async () => {
      const mockInstance = { id: 456, name: 'Single Instance' };

      mockApiClient.get.mockResolvedValueOnce({
        data: { data: mockInstance },
      });

      const { result } = renderHook(() => useInstance(456), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(mockApiClient.get).toHaveBeenCalledWith('/api/platform/instances/456');
      expect(result.current.data).toEqual(mockInstance);
    });

    it('does not fetch when id is undefined', () => {
      const { result } = renderHook(() => useInstance(undefined), {
        wrapper: createWrapper(),
      });

      expect(result.current.fetchStatus).toBe('idle');
      expect(mockApiClient.get).not.toHaveBeenCalled();
    });
  });

  describe('useTrialStatus', () => {
    it('fetches trial status when instanceId is provided', async () => {
      const mockTrialStatus = {
        daysRemaining: 7,
        isExpired: false,
      };

      mockApiClient.get.mockResolvedValueOnce({
        data: { data: mockTrialStatus },
      });

      const { result } = renderHook(() => useTrialStatus(789), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(mockApiClient.get).toHaveBeenCalledWith('/api/platform/instances/789/trial-status');
      expect(result.current.data).toEqual(mockTrialStatus);
    });

    it('does not fetch when instanceId is undefined', () => {
      const { result } = renderHook(() => useTrialStatus(undefined), {
        wrapper: createWrapper(),
      });

      expect(result.current.fetchStatus).toBe('idle');
      expect(mockApiClient.get).not.toHaveBeenCalled();
    });
  });
});
