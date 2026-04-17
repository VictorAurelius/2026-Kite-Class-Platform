'use client';

import { cn } from '@/lib/utils';
import type { TrialStatus } from '@/types/instance';

const warningColors: Record<string, string> = {
  NONE: 'bg-blue-500',
  LOW: 'bg-blue-500',
  MEDIUM: 'bg-yellow-500',
  HIGH: 'bg-orange-500',
  EXPIRED: 'bg-red-500',
};

interface TrialCountdownProps {
  trial: TrialStatus;
}

export function TrialCountdown({ trial }: TrialCountdownProps) {
  const maxDays = 14;
  const progress = Math.max(0, Math.min(100, (trial.daysRemaining / maxDays) * 100));
  const barColor = warningColors[trial.warningLevel] ?? 'bg-blue-500';

  return (
    <div className="rounded-lg border p-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium">Trial</span>
        <span
          className={cn(
            'text-sm font-semibold',
            trial.expired ? 'text-destructive' : 'text-foreground'
          )}
        >
          {trial.expired ? 'Hết hạn' : `Còn ${trial.daysRemaining} ngày`}
        </span>
      </div>
      <div className="mt-2 h-2 w-full rounded-full bg-muted">
        <div
          className={cn('h-2 rounded-full transition-all', barColor)}
          style={{ width: `${progress}%` }}
        />
      </div>
    </div>
  );
}
