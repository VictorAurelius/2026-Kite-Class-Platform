'use client';

/**
 * GAP-1216 — output-first 5-step progress indicator for AI Branding Wizard v2.
 *
 * Spec source: documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/v3/index.html
 * (flow chip bar §2.5, hội tụ 3 audit 2026-06-11).
 *
 * Steps: 1 Bắt đầu · 2 Phong cách · 3 Hình ảnh · 4 Mẫu · 5 Xem & Tạo.
 * Mode-aware: in FULL_AI mode the Template step (4) is skipped, so it is hidden
 * from the indicator entirely (the FULL_AI route is Welcome → Personality →
 * Assets → Preview).
 *
 * States per step:
 *   - completed (step < currentStep) — Check icon + filled primary background
 *   - current   (step === currentStep) — number + ring highlight + aria-current="step"
 *   - upcoming  (step > currentStep) — muted background + muted text
 */

import { Check } from 'lucide-react';
import type { WizardStep } from './wizard-shared';
import type { GenerationMode } from './GenerationModeSelector';

const ALL_WIZARD_STEPS: ReadonlyArray<{ number: WizardStep; label: string }> = [
  { number: 1, label: 'Bắt đầu' },
  { number: 2, label: 'Phong cách' },
  { number: 3, label: 'Hình ảnh' },
  { number: 4, label: 'Mẫu thiết kế' },
  { number: 5, label: 'Xem & Tạo' },
];

export interface StepIndicatorProps {
  /** Active step (1-5). */
  currentStep: WizardStep;
  /**
   * Generation mode (GAP-1216) — FULL_AI hides the Template step (4) since that
   * route skips template selection. Defaults to TEMPLATE (all 5 steps shown).
   */
  mode?: GenerationMode;
  /** Optional className override for outer wrapper. */
  className?: string;
}

export function StepIndicator({ currentStep, mode = 'TEMPLATE', className = '' }: StepIndicatorProps) {
  const steps =
    mode === 'FULL_AI'
      ? ALL_WIZARD_STEPS.filter((s) => s.number !== 4)
      : ALL_WIZARD_STEPS;

  return (
    <nav
      aria-label="Tiến trình"
      data-testid="wizard-step-indicator"
      className={`flex items-start justify-center flex-wrap gap-y-3 ${className}`}
    >
      {steps.map((step, idx) => {
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

            {idx < steps.length - 1 && (
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
