/**
 * useSlugAvailability — wizard Step 1 slug check (GAP-272i).
 *
 * Wave 34 Bucket D — replaces Wave 32 inline `SLUG_STUB_TAKEN` constant.
 *
 * Schema: `documents/01-business/kitehub/ai-branding/api-contract.md`
 * GET `/api/v1/branding/slug-availability?slug=...`.
 *
 * The hook exposes an imperative `checkSlug(slug)` function (NOT a
 * useQuery) because the WelcomeStep already drives a custom debounce +
 * stale-response guard. Migration to react-query later is mechanical.
 */

import { useCallback } from 'react';
import { AxiosError } from 'axios';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { SlugAvailabilityResponse } from './types';

export interface UseSlugAvailability {
  checkSlug(slug: string): Promise<SlugAvailabilityResponse>;
}

export function useSlugAvailability(): UseSlugAvailability {
  const checkSlug = useCallback(async (slug: string): Promise<SlugAvailabilityResponse> => {
    try {
      const { data } = await apiClient.get<SlugAvailabilityResponse>(
        endpoints.brandingV1.slugAvailability,
        { params: { slug } }
      );
      return data;
    } catch (err) {
      if (err instanceof AxiosError && err.response?.status === 400) {
        // INVALID_SLUG_FORMAT — surface as not-available with empty suggestions
        // so the WelcomeStep keeps the user in the syntax-hint state without
        // crashing. The 400-error envelope itself is logged by the interceptor.
        return { available: false, suggestions: [] };
      }
      throw err;
    }
  }, []);

  return { checkSlug };
}
