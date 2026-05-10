/**
 * Activity row — icon + title + sub + trailing timestamp.
 *
 * Wave 49 Bucket A (GAP-267). Ports `.activity-row` from kc-parent kit.
 */

'use client';

import { cn } from '@/lib/utils';

export type ActivityVariant = 'green' | 'blue' | 'amber' | 'red' | 'muted';

const VARIANT_CLASS: Record<ActivityVariant, string> = {
  green: 'bg-emerald-100 text-emerald-700',
  blue: 'bg-sky-100 text-sky-700',
  amber: 'bg-amber-100 text-amber-700',
  red: 'bg-red-100 text-red-700',
  muted: 'bg-muted text-muted-foreground',
};

export interface ActivityRowProps {
  icon: React.ReactNode;
  title: string;
  sub?: string;
  trailing?: string;
  variant?: ActivityVariant;
}

export function ActivityRow({
  icon,
  title,
  sub,
  trailing,
  variant = 'muted',
}: ActivityRowProps) {
  return (
    <li className="flex items-start gap-3 border-b py-3 last:border-b-0">
      <span
        className={cn(
          'flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full',
          VARIANT_CLASS[variant],
        )}
        aria-hidden
      >
        {icon}
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium leading-tight">{title}</p>
        {sub && (
          <p className="mt-0.5 text-xs text-muted-foreground">{sub}</p>
        )}
      </div>
      {trailing && (
        <span className="flex-shrink-0 text-[11px] text-muted-foreground">
          {trailing}
        </span>
      )}
    </li>
  );
}
