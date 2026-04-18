/**
 * Hook for fetching public branding (anonymous auth pages).
 *
 * Uses React Query with 5-minute stale-time so the auth flow doesn't hammer
 * the branding service. Falls back to defaults on any error — the caller
 * does not need to handle {error} for rendering purposes.
 *
 * @since Wave 4 (GAP-037)
 */

'use client';

import { useQuery } from '@tanstack/react-query';
import {
  DEFAULT_PUBLIC_BRANDING,
  publicBrandingApi,
  type PublicBranding,
} from '@/lib/api/public-branding';

const PUBLIC_BRANDING_KEY = 'public-branding';

export function usePublicBranding(tenantId: string | null) {
  const query = useQuery<PublicBranding>({
    queryKey: [PUBLIC_BRANDING_KEY, tenantId ?? 'anon'],
    queryFn: () => publicBrandingApi.get(tenantId),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    retry: 1,
  });

  return {
    branding: query.data ?? DEFAULT_PUBLIC_BRANDING,
    isLoading: query.isLoading,
    error: query.error,
  };
}
