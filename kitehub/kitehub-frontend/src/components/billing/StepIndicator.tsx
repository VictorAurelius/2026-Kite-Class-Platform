'use client';

import { Check } from 'lucide-react';

interface StepIndicatorProps {
  currentStep: number;
  totalSteps: number;
}

export function StepIndicator({ currentStep, totalSteps: _totalSteps }: StepIndicatorProps) {
  const steps = [
    { number: 1, label: 'Chọn gói' },
    { number: 2, label: 'Xác nhận' },
  ];

  return (
    <div className="flex items-center justify-center">
      {steps.map((step, idx) => (
        <div key={step.number} className="flex items-center">
          {/* Step Circle */}
          <div className="flex flex-col items-center">
            <div
              className={`
                w-10 h-10 rounded-full flex items-center justify-center text-sm font-medium
                ${step.number < currentStep
                  ? 'bg-primary text-primary-foreground'
                  : step.number === currentStep
                  ? 'bg-primary text-primary-foreground ring-4 ring-primary/20'
                  : 'bg-muted text-muted-foreground'}
              `}
            >
              {step.number < currentStep ? (
                <Check className="h-5 w-5" />
              ) : (
                step.number
              )}
            </div>
            <p className="text-sm mt-2 font-medium">{step.label}</p>
          </div>

          {/* Connector Line */}
          {idx < steps.length - 1 && (
            <div
              className={`
                h-0.5 w-24 mx-4 mb-6
                ${step.number < currentStep ? 'bg-primary' : 'bg-muted'}
              `}
            />
          )}
        </div>
      ))}
    </div>
  );
}
