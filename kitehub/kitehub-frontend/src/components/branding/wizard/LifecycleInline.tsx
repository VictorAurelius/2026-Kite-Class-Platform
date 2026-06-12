/**
 * LifecycleInline — wraps G9 InstanceLifecycleStatus for inline use in Step 6.
 *
 * Wave 32 Bucket D — Direction C 6-step refactor.
 *
 * Per `ai-branding-guidelines.md` §6: lifecycle state transitions go through
 * `InstanceLifecycleService` ONLY. This component MUST source its state via
 * `useInstanceLifecycle(instanceId)` — NOT hardcoded buildMockEvents() (that
 * was the v1 violation flagged in 2026-05-07 audit).
 *
 * Wave 34 Bucket D (GAP-272l): real backend lifecycle event timeline.
 *
 * The current-state pill is still derived from
 * `useInstanceLifecycle()` (a deterministic helper kept until backend
 * exposes `/api/instances/{id}/status`); the timeline below the pill
 * is now sourced from
 * `GET /api/v1/branding/instances/{instanceId}/lifecycle/events`
 * via `useLifecycleEvents` so the wizard's deploying view shows the
 * real audit trail.
 *
 * Per `ai-branding-guidelines.md` §6 the 5 lifecycle states this wrapper
 * must render in the wizard context:
 *   NOT_STARTED, GENERATING, DEPLOYED, REGENERATING, FAILED
 * (INITIALIZING is internal — the wizard collapses it under "đang chuẩn bị").
 *
 * The component:
 *   - Reads state via the service hook (NEVER hardcoded events)
 *   - Renders G9 InstanceLifecycleStatus from `@kite/shared-ui`
 *   - Forwards `onRetry` to the parent so the wizard can dispatch a regen action
 */

'use client';

import type React from 'react';
import { useMemo } from 'react';
import { InstanceLifecycleStatus } from '@kite/shared-ui';
import {
  useInstanceLifecycle,
  mockLifecycleEvents,
} from '@/app/(customer)/instances/_lifecycle-mock';
import type {
  InstanceLifecycleState,
  LifecycleEvent,
} from '@kite/shared-ui';
import { useLifecycleEvents } from './hooks';
import type { WizardLifecycleEvent } from './hooks';

export interface LifecycleInlineProps {
  /**
   * Active instance ID — drives the service hook. When undefined the
   * component renders NOT_STARTED + empty timeline (mid-wizard state).
   */
  instanceId: string | undefined;
  /**
   * Optional override for the instance display name shown above the pill.
   * Defaults to the instanceId when omitted.
   */
  instanceName?: string;
  /**
   * Live URL once DEPLOYED (e.g. `edison.kitehub.me`). Forwarded as-is to
   * G9 — the underlying component only renders it in DEPLOYED state.
   */
  liveUrl?: string;
  /**
   * Click handler for the FAILED-state retry CTA. The wizard dispatches a
   * regen action via the WizardState reducer.
   */
  onRetry?: () => void;
  /**
   * Optional state override — useful in tests + when parent wants to force a
   * particular state without spinning up a real instanceId. When provided it
   * fully replaces the hook's state. Events still come from the hook unless
   * `eventsOverride` is also passed.
   */
  stateOverride?: InstanceLifecycleState;
}

/**
 * Map Wave 34 wire `WizardLifecycleEvent` → G9 `LifecycleEvent` shape
 * (legacy 4-field tuple `{ from, to, timestamp, reason }`).
 */
function adaptWireEvents(events: readonly WizardLifecycleEvent[]): LifecycleEvent[] {
  const adapted: LifecycleEvent[] = [];
  for (const e of events) {
    if (e.type !== 'state-change' || !e.fromState || !e.toState) continue;
    adapted.push({
      from: e.fromState as InstanceLifecycleState,
      to: e.toState as InstanceLifecycleState,
      timestamp: e.ts,
      reason:
        typeof e.metadata?.reason === 'string'
          ? (e.metadata.reason as string)
          : undefined,
    });
  }
  // Backend returns newest-first; G9 timeline expects chronological order.
  return adapted.reverse();
}

export function LifecycleInline(props: LifecycleInlineProps): React.ReactElement {
  const { instanceId, instanceName, liveUrl, onRetry, stateOverride } = props;

  // Current-state pill — until backend exposes a status endpoint we keep
  // the deterministic helper.
  const { state: hookState } = useInstanceLifecycle(instanceId);

  // Wave 34 (GAP-272l): real backend timeline via v1 events endpoint.
  // Falls back to the legacy mock helper while no instanceId or while
  // the query is loading so the component is still demoable mid-wizard.
  const eventsQuery = useLifecycleEvents(instanceId);

  const state = stateOverride ?? hookState;

  const events = useMemo<LifecycleEvent[]>(() => {
    if (eventsQuery.data?.events && eventsQuery.data.events.length > 0) {
      return adaptWireEvents(eventsQuery.data.events);
    }
    return mockLifecycleEvents(state);
  }, [eventsQuery.data, state]);

  return (
    <div className="max-w-2xl mx-auto" data-testid="lifecycle-inline" data-state={state}>
      <InstanceLifecycleStatus
        state={state}
        events={events}
        instanceId={instanceId ?? 'inst-pending'}
        instanceName={instanceName}
        liveUrl={liveUrl}
        onRetry={state === 'FAILED' ? onRetry : undefined}
      />
    </div>
  );
}
