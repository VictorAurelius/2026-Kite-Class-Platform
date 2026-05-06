'use client';

import { Check } from 'lucide-react';
import type { WizardStep } from './wizard-shared';

// ---------------------------------------------------------------------------
// Step metadata — labels match the spec HTML stepper (ai-branding-guidelines §4.1)
// ---------------------------------------------------------------------------

const STEPS: { number: WizardStep; label: string }[] = [
  { number: 1, label: 'Chào mừng' },
  { number: 2, label: 'Logo' },
  { number: 3, label: 'Đối tượng' },
  { number: 4, label: 'Phong cách' },
  { number: 5, label: 'Mẫu thiết kế' },
  { number: 6, label: 'Phê duyệt' },
];

// ---------------------------------------------------------------------------
// StepIndicator
// ---------------------------------------------------------------------------

interface StepIndicatorProps {
  /** Currently active step (1–6). */
  currentStep: WizardStep;
}

/**
 * 6-step progress indicator for the AI Branding Wizard v2.
 *
 * Visual states:
 * - Completed (step < currentStep): filled circle with checkmark
 * - Current (step === currentStep): filled circle with ring halo + number
 * - Upcoming (step > currentStep): muted circle with number
 *
 * Matches spec: `ai-branding-wizard-v2/screens/step1-welcome-default.html` stepper.
 */
export function StepIndicator({ currentStep }: StepIndicatorProps) {
  return (
    <nav
      aria-label="Tiến trình"
      className="flex items-center justify-center py-4"
    >
      {STEPS.map((step, idx) => {
        const isCompleted = step.number < currentStep;
        const isCurrent = step.number === currentStep;
        const isUpcoming = step.number > currentStep;

        return (
          <div key={step.number} className="flex items-center">
            {/* Step node */}
            <div className="flex flex-col items-center gap-1.5">
              <div
                aria-current={isCurrent ? 'step' : undefined}
                className={[
                  'w-9 h-9 rounded-full flex items-center justify-center text-sm font-semibold transition-all',
                  isCompleted
                    ? 'bg-primary text-primary-foreground'
                    : isCurrent
                    ? 'bg-primary text-primary-foreground ring-4 ring-primary/20'
                    : 'bg-muted text-muted-foreground',
                ].join(' ')}
              >
                {isCompleted ? (
                  <Check className="w-4 h-4" aria-hidden="true" />
                ) : (
                  <span>{step.number}</span>
                )}
              </div>

              <span
                className={[
                  'text-xs font-medium hidden sm:block',
                  isUpcoming ? 'text-muted-foreground' : 'text-foreground',
                ].join(' ')}
              >
                {step.label}
              </span>
            </div>

            {/* Connector bar — not rendered after the last step */}
            {idx < STEPS.length - 1 && (
              <div
                aria-hidden="true"
                className={[
                  'h-px w-10 md:w-16 mx-1 md:mx-2 mb-5 transition-colors',
                  isCompleted ? 'bg-primary' : 'bg-muted',
                ].join(' ')}
              />
            )}
          </div>
        );
      })}
    </nav>
  );
}
