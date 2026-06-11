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
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';
import { useRegenerateQuota } from '../useRegenerateQuota';
import { server } from '@/test/msw/server';

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
      await result.current.regenerate.mutateAsync({
        jobId: 'job-abc-123',
        instanceId: 'inst-001',
      });
    });

    await waitFor(() => expect(result.current.regenerate.isSuccess).toBe(true));
    expect(result.current.regenerate.data?.jobId).toBe('job-abc-123');
    expect(result.current.regenerate.data?.status).toBe('REGENERATING');
  });

  // GAP-1145: the FE MUST send X-Instance-Id (the job's tenant/instance claim);
  // the gateway never injects it, so its absence makes the server reject with
  // 400 MISSING_INSTANCE_ID for every caller.
  it('sends the X-Instance-Id header on regenerate (GAP-1145)', async () => {
    let sentInstanceId: string | null = 'NOT_SENT';
    server.use(
      http.post('*/api/v1/branding/jobs/:jobId/regenerate', ({ request, params }) => {
        sentInstanceId = request.headers.get('X-Instance-Id');
        const { jobId } = params as { jobId: string };
        return HttpResponse.json({
          jobId,
          instanceId: 1,
          status: 'REGENERATING',
          createdAt: '2026-06-10T09:00:00Z',
          updatedAt: new Date().toISOString(),
        });
      })
    );

    const { result } = renderHook(() => useRegenerateQuota(), {
      wrapper: makeWrapper(),
    });

    await act(async () => {
      await result.current.regenerate.mutateAsync({
        jobId: 'job-hdr',
        instanceId: 'inst-xyz',
      });
    });

    expect(sentInstanceId).toBe('inst-xyz');
  });

  // GAP-391-A: post-regenerate cache invalidation triggers refetch with
  // updated quota. Without invalidation the FE would show stale "3/3"
  // remaining for ~1-2 seconds before server reject (UX miss).
  it('invalidates regenerate-quota cache on successful regenerate (GAP-391-A)', async () => {
    let usedCount = 0;
    server.use(
      http.get('*/api/v1/branding/regenerate-quota', () =>
        HttpResponse.json({
          tier: 'FREE',
          used: usedCount,
          limit: 3,
          resetAt: '2026-05-08T00:00:00Z',
        })
      ),
      http.post('*/api/v1/branding/jobs/:jobId/regenerate', ({ params }) => {
        usedCount += 1;
        const { jobId } = params as { jobId: string };
        return HttpResponse.json({
          jobId,
          instanceId: 1,
          status: 'REGENERATING',
          regenerateCount: usedCount,
          brandingVersion: usedCount,
          createdAt: '2026-05-07T09:00:00Z',
          updatedAt: new Date().toISOString(),
        });
      })
    );

    const { result } = renderHook(() => useRegenerateQuota(), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.quota.isSuccess).toBe(true));
    expect(result.current.quota.data?.used).toBe(0);

    await act(async () => {
      await result.current.regenerate.mutateAsync({
        jobId: 'job-fresh',
        instanceId: 'inst-001',
      });
    });

    // After mutation success, hook invalidates ['brandingV1', 'regenerateQuota']
    // → react-query refetches → fresh value (used=1) appears.
    await waitFor(() => expect(result.current.quota.data?.used).toBe(1));
    expect(result.current.quota.data?.limit).toBe(3);
  });

  it('quota-exceeded job id surfaces 403 as mutation error', async () => {
    const { result } = renderHook(() => useRegenerateQuota(), {
      wrapper: makeWrapper(),
    });

    let rejected = false;
    await act(async () => {
      try {
        await result.current.regenerate.mutateAsync({
          jobId: 'job-quota-exceeded',
          instanceId: 'inst-001',
        });
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
