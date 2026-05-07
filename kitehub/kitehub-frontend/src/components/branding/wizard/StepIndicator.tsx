'use client';

/**
 * Wave 32 Bucket A — 6-step progress indicator for AI Branding Wizard v2.
 *
 * Spec source: documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step1-welcome-default.html
 * (`.wiz-stepper` markup with 6 `.wiz-step` items joined by `.wiz-step-bar` connectors).
 *
 * States per step:
 *   - completed (number < currentStep) — Check icon + filled primary background
 *   - current   (number === currentStep) — number + ring highlight + aria-current="step"
 *   - upcoming  (number > currentStep) — muted background + muted text
 */

import { Check } from 'lucide-react';

const WIZARD_STEPS: ReadonlyArray<{ number: 1 | 2 | 3 | 4 | 5 | 6; label: string }> = [
  { number: 1, label: 'Chào mừng' },
  { number: 2, label: 'Logo' },
  { number: 3, label: 'Đối tượng' },
  { number: 4, label: 'Phong cách' },
  { number: 5, label: 'Mẫu thiết kế' },
  { number: 6, label: 'Phê duyệt' },
];

export interface StepIndicatorProps {
  /** Active step (1-6). */
  currentStep: 1 | 2 | 3 | 4 | 5 | 6;
  /** Optional className override for outer wrapper. */
  className?: string;
}

export function StepIndicator({ currentStep, className = '' }: StepIndicatorProps) {
  return (
    <nav
      aria-label="Tiến trình"
      data-testid="wizard-step-indicator"
      className={`flex items-start justify-center flex-wrap gap-y-3 ${className}`}
    >
      {WIZARD_STEPS.map((step, idx) => {
        const isCompleted = step.number < currentStep;
        const isCurrent = step.number === currentStep;

        const circleClasses = [
          'w-9 h-9 rounded-full flex items-center justify-center text-sm font-medium transition-all',
          isCompleted
            ? 'bg-primary text-primary-foreground'
            : isCurrent
              ? 'bg-primary text-primary-foreground ring-4 ring-primary/20'
              : 'bg-muted text-muted-foreground',
        ].join(' ');

        const labelClasses = [
          'text-xs md:text-sm mt-2 font-medium text-center max-w-[6rem]',
          isCompleted || isCurrent ? 'text-foreground' : 'text-muted-foreground',
        ].join(' ');

        return (
          <div key={step.number} className="flex items-center" data-step={step.number}>
            <div className="flex flex-col items-center">
              <div
                className={circleClasses}
                aria-current={isCurrent ? 'step' : undefined}
                aria-label={`Bước ${step.number}: ${step.label}${
                  isCompleted ? ' (đã xong)' : isCurrent ? ' (đang làm)' : ''
                }`}
              >
                {isCompleted ? <Check className="w-4 h-4" /> : step.number}
              </div>
              <p className={labelClasses}>{step.label}</p>
            </div>

            {idx < WIZARD_STEPS.length - 1 && (
              <div
                className={[
                  'h-0.5 w-6 md:w-12 lg:w-16 mx-1 md:mx-2 mb-6 transition-colors',
                  isCompleted ? 'bg-primary' : 'bg-muted',
                ].join(' ')}
                aria-hidden="true"
              />
            )}
          </div>
        );
      })}
    </nav>
  );
}
