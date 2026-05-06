/**
 * Wave 31 Bucket D — client-side mock for AI Branding instance lifecycle.
 *
 * Backend `/api/instances/{id}/status` polling endpoint does NOT yet exist
 * (state-checked 2026-05-06: only domain/upgrade/admin endpoints under
 * `/api/instances/{id}/*`). When that endpoint lands, replace the
 * `useInstanceLifecycle` hook here with a real `useQuery` polling at 5-10s
 * intervals. Until then, this file ships deterministic mock data so the
 * G9 InstanceLifecycleStatus integration can be demoed end-to-end.
 *
 * Follow-up: file gap "wire G9 to real /api/instances/{id}/status endpoint"
 * once kitehub-branding ships the controller.
 */

import { useEffect, useState } from 'react';
import type {
  InstanceLifecycleState,
  LifecycleEvent,
} from '@kite/shared-ui';

/**
 * Deterministic per-instance fixture states for the list page. Spans the
 * full set of 6 lifecycle states so the list visibly demonstrates the
 * compact G9 pill across the lifecycle.
 */
export const MOCK_INSTANCE_LIFECYCLE: Record<string, InstanceLifecycleState> = {
  default: 'DEPLOYED',
};

const STATE_ORDER: readonly InstanceLifecycleState[] = [
  'NOT_STARTED',
  'INITIALIZING',
  'GENERATING',
  'DEPLOYED',
  'REGENERATING',
  'FAILED',
] as const;

/**
 * Hash-based deterministic state picker — same id always yields same state
 * so the list view stays stable across renders.
 */
export function mockLifecycleStateFor(instanceId: string | undefined): InstanceLifecycleState {
  if (!instanceId) return 'NOT_STARTED';
  let hash = 0;
  for (let i = 0; i < instanceId.length; i++) {
    hash = (hash * 31 + instanceId.charCodeAt(i)) | 0;
  }
  const idx = Math.abs(hash) % STATE_ORDER.length;
  return STATE_ORDER[idx]!;
}

/**
 * Mock timeline of lifecycle events leading up to the given current state.
 * Used on the instance detail page where the full G9 timeline renders.
 */
export function mockLifecycleEvents(
  currentState: InstanceLifecycleState,
  baseTimestamp = '2026-04-01T08:00:00.000Z'
): LifecycleEvent[] {
  const baseMs = new Date(baseTimestamp).getTime();
  const step = 6 * 60 * 60 * 1000; // 6h between transitions
  const t = (i: number) => new Date(baseMs + i * step).toISOString();

  switch (currentState) {
    case 'NOT_STARTED':
      return [];
    case 'INITIALIZING':
      return [{ from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: t(1) }];
    case 'GENERATING':
      return [
        { from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: t(1) },
        { from: 'INITIALIZING', to: 'GENERATING', timestamp: t(2) },
      ];
    case 'DEPLOYED':
      return [
        { from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: t(1) },
        { from: 'INITIALIZING', to: 'GENERATING', timestamp: t(2) },
        { from: 'GENERATING', to: 'DEPLOYED', timestamp: t(3) },
      ];
    case 'REGENERATING':
      return [
        { from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: t(1) },
        { from: 'INITIALIZING', to: 'GENERATING', timestamp: t(2) },
        { from: 'GENERATING', to: 'DEPLOYED', timestamp: t(3) },
        {
          from: 'DEPLOYED',
          to: 'REGENERATING',
          timestamp: t(20),
          reason: 'Owner yêu cầu tái tạo template Tươi mát',
        },
      ];
    case 'FAILED':
      return [
        { from: 'NOT_STARTED', to: 'INITIALIZING', timestamp: t(1) },
        { from: 'INITIALIZING', to: 'GENERATING', timestamp: t(2) },
        {
          from: 'GENERATING',
          to: 'FAILED',
          timestamp: t(3),
          reason:
            'Quality gate điểm 62/100 — chưa đạt ngưỡng 70/100. Vui lòng thử lại.',
        },
      ];
  }
}

/**
 * Mock hook approximating a polling client. Returns a stable lifecycle state
 * keyed off the instanceId and the matching timeline. When the real backend
 * endpoint ships, swap the body for `useQuery` with `refetchInterval`.
 *
 * @param instanceId  optional instance id; when missing returns NOT_STARTED.
 * @param cycle       when true, cycles through STATE_ORDER every 8s (used on
 *                    detail page only to demo transitions). The list view
 *                    keeps a stable hash-derived state.
 */
export function useInstanceLifecycle(
  instanceId: string | undefined,
  options: { cycle?: boolean } = {}
): { state: InstanceLifecycleState; events: LifecycleEvent[] } {
  const initial = mockLifecycleStateFor(instanceId);
  const [state, setState] = useState<InstanceLifecycleState>(initial);

  useEffect(() => {
    if (!options.cycle) return;
    const id = setInterval(() => {
      setState((prev) => {
        const idx = STATE_ORDER.indexOf(prev);
        return STATE_ORDER[(idx + 1) % STATE_ORDER.length]!;
      });
    }, 8000);
    return () => clearInterval(id);
  }, [options.cycle]);

  return {
    state,
    events: mockLifecycleEvents(state),
  };
}
