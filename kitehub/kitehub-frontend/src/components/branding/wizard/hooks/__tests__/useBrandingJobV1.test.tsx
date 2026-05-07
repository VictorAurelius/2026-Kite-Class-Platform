/**
 * Wave 34 Bucket D — useBrandingJobV1 + usePreviewBrandColors smoke tests.
 *
 * Validates the BrandingJobResponse wrapper (Bucket B contract) is
 * fetched + that brandColors derivation (GAP-272k) returns the wire
 * shape verbatim.
 */

import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useBrandingJobV1 } from '../useBrandingJobV1';
import { usePreviewBrandColors } from '../usePreviewBrandColors';
import type { ReactNode } from 'react';

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

describe('useBrandingJobV1', () => {
  it('fetches the BrandingJobResponse wrapper with brandColors', async () => {
    const { result } = renderHook(() => useBrandingJobV1('job-abc-123'), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.jobId).toBe('job-abc-123');
    expect(result.current.data?.status).toBe('DEPLOYED');
    expect(result.current.data?.brandColors?.primary).toBe('#1a73e8');
  });

  it('returns 404 envelope as an error for missing job', async () => {
    const { result } = renderHook(() => useBrandingJobV1('job-not-found'), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

describe('usePreviewBrandColors', () => {
  it('extracts brandColors from the underlying job', async () => {
    const { result } = renderHook(() => usePreviewBrandColors('job-abc-123'), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.brandColors).toBeDefined());

    expect(result.current.brandColors?.primary).toBe('#1a73e8');
    expect(result.current.brandColors?.secondary).toBe('#fbbc04');
    expect(result.current.brandColors?.source).toBe('TEMPLATE');
  });
});
