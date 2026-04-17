'use client';

import { ORDERED_STEPS, type StepName } from './types';

const LABELS: Record<StepName, string> = {
  welcome: 'Bắt đầu',
  logo: 'Logo',
  audience: 'Đối tượng',
  tone: 'Phong cách',
  template: 'Mẫu',
  preview: 'Xem trước',
};

export function WizardProgress({ current }: { current: StepName }) {
  const currentIdx = ORDERED_STEPS.indexOf(current);
  return (
    <ol className="flex items-center gap-1 text-xs" aria-label="Tiến trình wizard">
      {ORDERED_STEPS.map((step, idx) => {
        const state =
          idx < currentIdx ? 'done' : idx === currentIdx ? 'current' : 'pending';
        return (
          <li key={step} className="flex items-center">
            <span
              className={`flex h-7 min-w-7 items-center justify-center rounded-full px-2 font-medium ${
                state === 'current'
                  ? 'bg-primary text-primary-foreground'
                  : state === 'done'
                    ? 'bg-primary/20 text-primary'
                    : 'bg-muted text-muted-foreground'
              }`}
              aria-current={state === 'current' ? 'step' : undefined}
            >
              {idx + 1}. {LABELS[step]}
            </span>
            {idx < ORDERED_STEPS.length - 1 && (
              <span className="mx-1 h-px w-6 bg-border" aria-hidden />
            )}
          </li>
        );
      })}
    </ol>
  );
}
