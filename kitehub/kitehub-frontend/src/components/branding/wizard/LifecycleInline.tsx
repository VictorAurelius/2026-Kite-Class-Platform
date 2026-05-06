'use client';

/**
 * LifecycleInline — Wave 32 Bucket D (GAP-272)
 *
 * Wraps G9 InstanceLifecycleStatus for inline Step 6 deploy context.
 * Renders all 5 states spec'd in the wave kit:
 *   NOT_STARTED | GENERATING | DEPLOYED | REGENERATING | FAILED
 *
 * Note: G9 supports 6 states (includes INITIALIZING) but the wizard spec
 * renders 5 post-wizard states (INITIALIZING is pre-wizard infrastructure).
 * LifecycleInline exposes only the 5 wizard-relevant states.
 */

import React from 'react';
import { InstanceLifecycleStatus } from '@kite/shared-ui';
import type { InstanceLifecycleState, LifecycleEvent } from '@kite/shared-ui';

/** The 5 lifecycle states relevant to Step 6 deploy flow */
export type WizardLifecycleState = Extract<
  InstanceLifecycleState,
  'NOT_STARTED' | 'GENERATING' | 'DEPLOYED' | 'REGENERATING' | 'FAILED'
>;

export interface LifecycleInlineProps {
  /** Current lifecycle state */
  status: WizardLifecycleState;
  /** Instance ID for display */
  instanceId: string;
  /** Instance display name */
  instanceName?: string;
  /** Live URL (only shown when DEPLOYED) */
  liveUrl?: string;
  /** Past lifecycle events for timeline */
  events?: readonly LifecycleEvent[];
  /** Called when user clicks retry (FAILED state) */
  onRetry?: () => void;
}

/** Mock events for states that follow a typical progression */
function buildMockEvents(status: WizardLifecycleState): LifecycleEvent[] {
  const base: LifecycleEvent[] = [
    {
      from: 'NOT_STARTED',
      to: 'GENERATING',
      timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
      actor: 'Hệ thống tự động',
    },
  ];

  if (status === 'DEPLOYED') {
    return [
      ...base,
      {
        from: 'GENERATING',
        to: 'DEPLOYED',
        timestamp: new Date(Date.now() - 1 * 60 * 1000).toISOString(),
        actor: 'Hệ thống tự động',
      },
    ];
  }

  if (status === 'REGENERATING') {
    return [
      ...base,
      {
        from: 'GENERATING',
        to: 'DEPLOYED',
        timestamp: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
      },
      {
        from: 'DEPLOYED',
        to: 'REGENERATING',
        timestamp: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
        actor: 'Chủ sở hữu',
      },
    ];
  }

  if (status === 'FAILED') {
    return [
      ...base,
      {
        from: 'GENERATING',
        to: 'FAILED',
        timestamp: new Date(Date.now() - 3 * 60 * 1000).toISOString(),
        reason: 'Quality gate điểm 62/100 — chưa đạt ngưỡng 70/100',
        actor: 'Hệ thống tự động',
      },
    ];
  }

  return base;
}

export function LifecycleInline({
  status,
  instanceId,
  instanceName,
  liveUrl,
  events,
  onRetry,
}: LifecycleInlineProps) {
  const resolvedEvents = events ?? buildMockEvents(status);

  return (
    <div className="rounded-xl border bg-white overflow-hidden">
      <InstanceLifecycleStatus
        state={status}
        instanceId={instanceId}
        instanceName={instanceName}
        liveUrl={status === 'DEPLOYED' ? liveUrl : undefined}
        events={resolvedEvents}
        onRetry={status === 'FAILED' ? onRetry : undefined}
        lang="vi"
      />
    </div>
  );
}
