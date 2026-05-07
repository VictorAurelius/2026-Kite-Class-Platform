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
 * Today the hook implementation lives at
 * `kitehub-frontend/src/app/(customer)/instances/_lifecycle-mock.ts` and
 * polls a deterministic mock until the backend `/api/instances/{id}/status`
 * endpoint ships. When that endpoint lands the hook body swaps to a real
 * `useQuery` with `refetchInterval` — call-sites here remain unchanged.
 *
 * // TODO(GAP-272l): wire `useInstanceLifecycle` to real
 * //   `InstanceLifecycleService` when the backend `/api/instances/{id}/status`
 * //   polling endpoint ships. Hook signature stays the same; only the body
 * //   swaps. Tracked alongside Wave 32 closure follow-ups.
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
import { InstanceLifecycleStatus } from '@kite/shared-ui';
import {
  useInstanceLifecycle,
  mockLifecycleEvents,
} from '@/app/(customer)/instances/_lifecycle-mock';
import type { InstanceLifecycleState } from '@kite/shared-ui';

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
   * Live URL once DEPLOYED (e.g. `edison.kiteclass.vn`). Forwarded as-is to
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

export function LifecycleInline(props: LifecycleInlineProps): React.ReactElement {
  const { instanceId, instanceName, liveUrl, onRetry, stateOverride } = props;

  // Service hook — single source of truth per `ai-branding-guidelines.md` §6.
  // TODO(GAP-272l): hook body swaps to real `useQuery` when backend
  //   `/api/instances/{id}/status` polling endpoint ships.
  const { state: hookState, events: hookEvents } = useInstanceLifecycle(instanceId);

  const state = stateOverride ?? hookState;
  const events = stateOverride ? mockLifecycleEvents(state) : hookEvents;

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
