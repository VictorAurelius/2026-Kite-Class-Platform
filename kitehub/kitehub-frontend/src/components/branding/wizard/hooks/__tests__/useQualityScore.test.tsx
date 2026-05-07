/**
 * Wave 34 Bucket D — useQualityScore hook smoke test.
 *
 * Validates §5 Quality Gate consumption (GAP-272c).
 */

import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useQualityScore } from '../useQualityScore';

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

describe('useQualityScore', () => {
  it('fetches passing quality score with 5-dimension subscores', async () => {
    const { result } = renderHook(() => useQualityScore('job-abc-123'), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.score).toBe(82);
    expect(result.current.data?.passed).toBe(true);
    expect(result.current.data?.threshold).toBe(70);
    expect(result.current.data?.subscores.contrast).toBe(90);
    expect(result.current.data?.subscores.logoPlacement).toBe(80);
  });

  it('surfaces 404 errors without retrying', async () => {
    const { result } = renderHook(() => useQualityScore('job-not-found'), {
      wrapper: makeWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
