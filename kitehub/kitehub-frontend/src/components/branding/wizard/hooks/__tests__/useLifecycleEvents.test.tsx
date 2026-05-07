/**
 * Wave 34 Bucket D — useLifecycleEvents + usePreview hook smoke tests.
 *
 * Validates lifecycle timeline (GAP-272l) and preview URL composer
 * (GAP-272j). Preview URL is purely computed (no fetch); the iframe
 * issues the actual HTTP request.
 */

import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useLifecycleEvents } from '../useLifecycleEvents';
import { usePreview } from '../usePreview';

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'TestQueryClientWrapper';
  return Wrapper;
}

describe('useLifecycleEvents', () => {
  it('fetches state-change events with nextCursor=null', async () => {
    const { result } = renderHook(() => useLifecycleEvents('inst-12345'), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.events.length).toBe(2);
    expect(result.current.data?.events[0]?.type).toBe('state-change');
    expect(result.current.data?.nextCursor).toBeNull();
  });

  it('returns 404 envelope as error for missing instance', async () => {
    const { result } = renderHook(() => useLifecycleEvents('inst-not-found'), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

describe('usePreview', () => {
  it('returns the v1 preview path for a given jobId', () => {
    const { result } = renderHook(() => usePreview('job-abc-123'));
    expect(result.current).toBe('/api/v1/branding/jobs/job-abc-123/preview');
  });

  it('returns undefined when jobId is missing', () => {
    const { result } = renderHook(() => usePreview(undefined));
    expect(result.current).toBeUndefined();
  });
});
