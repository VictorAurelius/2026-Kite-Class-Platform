/**
 * KPICard — KPI tile with optional sparkline + delta indicator.
 *
 * Source spec: kiteclass-pro-v2/screens/dashboard-default.html § "Stat cards row".
 *
 * Layout:
 *   ┌──────────────────────────────┐
 *   │ <label>           <icon> │
 *   │ <value>                       │
 *   │ <delta%>      <sparkline> │
 *   └──────────────────────────────┘
 *
 * Tones drive the delta color + sparkline accent. Defaults to 'neutral'.
 */

'use client';

import { cn } from '@/lib/utils';
import { Sparkline } from './Sparkline';
import type { KPIData } from './types';

export interface KPICardProps extends KPIData {
  /** Optional className override on the outer card. */
  className?: string;
}

const TONE_STYLES: Record<NonNullable<KPIData['tone']>, { delta: string; spark: string }> = {
  positive: {
    delta: 'text-emerald-600 dark:text-emerald-400',
    spark: 'text-emerald-500 dark:text-emerald-400',
  },
  negative: {
    delta: 'text-rose-600 dark:text-rose-400',
    spark: 'text-rose-500 dark:text-rose-400',
  },
  warning: {
    delta: 'text-amber-600 dark:text-amber-400',
    spark: 'text-amber-500 dark:text-amber-400',
  },
  neutral: {
    delta: 'text-muted-foreground',
    spark: 'text-indigo-500 dark:text-indigo-400',
  },
};

function formatDelta(delta: number): string {
  const sign = delta > 0 ? '+' : '';
  return `${sign}${delta.toFixed(1)}%`;
}

export function KPICard({
  label,
  value,
  delta,
  sparkline,
  tone = 'neutral',
  icon,
  className,
}: KPICardProps) {
  const tones = TONE_STYLES[tone];

  return (
    <div
      className={cn(
        'rounded-xl border border-border bg-card p-4 shadow-sm',
        'transition-colors hover:bg-accent/40',
        className,
      )}
      data-testid="kpi-card"
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
        {icon ? <span className="text-muted-foreground">{icon}</span> : null}
      </div>
      <div className="mt-2 text-2xl font-semibold text-foreground tabular-nums">
        {value}
      </div>
      <div className="mt-3 flex items-end justify-between gap-3">
        {typeof delta === 'number' ? (
          <span
            className={cn('text-xs font-medium tabular-nums', tones.delta)}
            data-testid="kpi-delta"
          >
            {formatDelta(delta)}
          </span>
        ) : (
          <span aria-hidden="true" />
        )}
        {sparkline !== undefined ? (
          <span className={tones.spark} aria-hidden="true">
            <Sparkline
              data={sparkline}
              width={88}
              height={24}
              stroke="currentColor"
              ariaLabel={`${label} trend`}
            />
          </span>
        ) : null}
      </div>
    </div>
  );
}
