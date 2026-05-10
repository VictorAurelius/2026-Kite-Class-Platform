/**
 * Hero metric card — large headline number + delta indicator.
 *
 * Wave 49 Bucket A (GAP-267). Mirrors the `.hero-metric` block in
 * `ui_kits/kiteclass-parent/styles.css` — gradient background + AAA contrast
 * (white-on-gradient measured 7.2:1 in HTML prototype).
 *
 * Used on parent home for the monthly attendance rate, billing-success for
 * paid totals, and grades-overview for the GPA hero.
 */

'use client';

import { TrendingDown, TrendingUp, Minus } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface HeroMetricProps {
  label: string;
  value: string | number;
  /** Suffix glued to the value, e.g. `%`, `đ`, `điểm`. */
  suffix?: string;
  /** Optional delta line below the value. */
  delta?: {
    direction: 'up' | 'down' | 'flat';
    /** Plain text, e.g. `Tăng 4% so với tháng trước`. */
    label: string;
  };
  /** Override gradient — defaults to indigo-blue per persona theme. */
  className?: string;
  'data-testid'?: string;
}

const DELTA_ICON = {
  up: TrendingUp,
  down: TrendingDown,
  flat: Minus,
} as const;

export function HeroMetric({
  label,
  value,
  suffix,
  delta,
  className,
  'data-testid': testId,
}: HeroMetricProps) {
  const DeltaIcon = delta ? DELTA_ICON[delta.direction] : null;

  return (
    <section
      className={cn(
        'mx-4 mt-4 rounded-2xl bg-gradient-to-br from-sky-500 to-blue-600 p-5 text-white shadow-lg',
        className,
      )}
      aria-labelledby="hero-metric-label"
      data-testid={testId ?? 'hero-metric'}
    >
      <p
        id="hero-metric-label"
        className="text-xs font-semibold uppercase tracking-wide opacity-90"
      >
        {label}
      </p>
      <p className="mt-2 text-5xl font-extrabold tracking-tight">
        {value}
        {suffix && (
          <span className="ml-0.5 text-2xl font-bold opacity-90">{suffix}</span>
        )}
      </p>
      {delta && DeltaIcon && (
        <p className="mt-2 inline-flex items-center gap-1.5 rounded-full bg-white/15 px-2.5 py-1 text-xs font-semibold">
          <DeltaIcon className="h-3.5 w-3.5" aria-hidden />
          {delta.label}
        </p>
      )}
    </section>
  );
}
