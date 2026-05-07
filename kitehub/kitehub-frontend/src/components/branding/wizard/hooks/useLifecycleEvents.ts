/**
 * useLifecycleEvents — Wave 34 instance lifecycle event timeline (GAP-272l).
 *
 * Schema: GET `/api/v1/branding/instances/{instanceId}/lifecycle/events`
 * per `api-contract.md` Wave 34. Replaces Wave 32 inline `mockLifecycleEvents()`
 * fixture in `LifecycleInline` (the legacy `_lifecycle-mock.ts` stays in
 * place behind the hook so non-wizard call sites — instance list/detail —
 * can keep working until they migrate).
 *
 * v1 always returns `nextCursor: null`; pagination contract reserved.
 */

import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api/client';
import { endpoints } from '@/lib/api/endpoints';
import type { LifecycleEventsResponse } from './types';

export interface UseLifecycleEventsOptions {
  /** ISO-8601 lower bound; defaults server-side to last 30 days. */
  since?: string;
  /** Page size; default 50, server-clamped to 200. */
  limit?: number;
}

export function useLifecycleEvents(
  instanceId: string | undefined,
  options: UseLifecycleEventsOptions = {}
) {
  const { since, limit } = options;
  return useQuery<LifecycleEventsResponse>({
    queryKey: ['brandingV1', 'lifecycleEvents', instanceId, since, limit],
    queryFn: async () => {
      const { data } = await apiClient.get<LifecycleEventsResponse>(
        endpoints.brandingV1.instanceLifecycleEvents(instanceId!),
        { params: { since, limit } }
      );
      return data;
    },
    enabled: !!instanceId,
  });
}
