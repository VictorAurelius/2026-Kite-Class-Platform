/**
 * React Query hooks for parent portal operations.
 *
 * Phase 1A (GAP-321 — Wave 18b1 Bucket D): transcript read-only.
 *
 * @author KiteClass Team
 * @since 2.18.0
 */

'use client';

import { useQuery } from '@tanstack/react-query';
import { parentApi } from '@/lib/api/parent';

const PARENT_KEY = 'parent';

/** Self profile (Wave 2). */
export function useParentMe() {
  return useQuery({
    queryKey: [PARENT_KEY, 'me'],
    queryFn: () => parentApi.getMe(),
  });
}

/** Linked-children list for the dashboard selector (Wave 2 endpoint reused). */
export function useMyChildren() {
  return useQuery({
    queryKey: [PARENT_KEY, 'me', 'children'],
    queryFn: () => parentApi.getMyChildren(),
  });
}

/**
 * Transcript list for a single linked child. Disabled until {@code childId}
 * is truthy so the query doesn't fire on first render with `NaN`.
 *
 * Server enforces BR-PARENT-PORTAL-001 scope guard — 403 PARENT_NOT_LINKED is
 * surfaced as React Query error; consumer renders a toast.
 */
export function useChildTranscript(childId: number | undefined) {
  return useQuery({
    queryKey: [PARENT_KEY, 'children', childId, 'transcript'],
    queryFn: () => parentApi.getChildTranscript(childId as number),
    enabled: typeof childId === 'number' && Number.isFinite(childId) && childId > 0,
  });
}
