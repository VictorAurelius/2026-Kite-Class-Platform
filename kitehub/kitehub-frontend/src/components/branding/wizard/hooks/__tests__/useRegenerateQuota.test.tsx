/**
 * Wave 34 Bucket D — useRegenerateQuota hook smoke tests (GAP-272d).
 *
 * Verifies:
 *  - Quota query parses the tier-aware response shape
 *  - Mutation auto-injects `Idempotency-Key` (UUID v4) — MSW handler
 *    asserts presence + format
 *  - 403 quota-exceeded surfaces as mutation error without retry
 */

import { describe, it, expect } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useRegenerateQuota } from '../useRegenerateQuota';

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false } },
  });
  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'TestQueryClientWrapper';
  return Wrapper;
}

describe('useRegenerateQuota', () => {
  it('fetches the regenerate quota with FREE-tier defaults', async () => {
    const { result } = renderHook(() => useRegenerateQuota(), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.quota.isSuccess).toBe(true));

    expect(result.current.quota.data?.tier).toBe('FREE');
    expect(result.current.quota.data?.limit).toBe(3);
    expect(result.current.quota.data?.used).toBe(0);
  });

  it('regenerate mutation returns updated job in REGENERATING state', async () => {
    const { result } = renderHook(() => useRegenerateQuota(), {
      wrapper: makeWrapper(),
    });

    await act(async () => {
      await result.current.regenerate.mutateAsync({ jobId: 'job-abc-123' });
    });

    await waitFor(() => expect(result.current.regenerate.isSuccess).toBe(true));
    expect(result.current.regenerate.data?.jobId).toBe('job-abc-123');
    expect(result.current.regenerate.data?.status).toBe('REGENERATING');
  });

  it('quota-exceeded job id surfaces 403 as mutation error', async () => {
    const { result } = renderHook(() => useRegenerateQuota(), {
      wrapper: makeWrapper(),
    });

    let rejected = false;
    await act(async () => {
      try {
        await result.current.regenerate.mutateAsync({ jobId: 'job-quota-exceeded' });
      } catch {
        rejected = true;
      }
    });

    // mutateAsync REJECTS on 403 — that is the contract surface for
    // callers (UI shows quota-exceeded modal). isError is also flipped
    // by react-query, but rejection alone is sufficient signal here.
    expect(rejected).toBe(true);
    await waitFor(() => expect(result.current.regenerate.isError).toBe(true));
  });
});
