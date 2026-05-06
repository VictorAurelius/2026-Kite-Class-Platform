'use client';

// ---------------------------------------------------------------------------
// TemplateStep — Step 5 placeholder
// Owned by Wave 32 Bucket C (template picker + generate trigger).
// ---------------------------------------------------------------------------

import { Button } from '@/components/ui/button';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { WizardCard, WizardStepHeader } from './wizard-shared';
import type { WizardState } from './wizard-shared';

interface TemplateStepProps {
  wizardState: WizardState;
  instanceId: string;
  onNext: (templateId: string, jobId: string) => void;
  onBack: () => void;
}

export function TemplateStep({
  wizardState: _wizardState,
  instanceId: _instanceId,
  onNext,
  onBack,
}: TemplateStepProps) {
  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow="Bước 5 / 6"
        title="Chọn mẫu thiết kế"
        subtitle="Chọn 1 trong 6 mẫu thiết kế phù hợp với phong cách của bạn."
      />
      <WizardCard>
        <p className="text-muted-foreground text-sm">
          Bước này đang được xây dựng (Wave 32 Bucket C). Nhấn &quot;Tiếp tục&quot; để bỏ qua tạm thời.
        </p>
      </WizardCard>
      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack}>
          <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
          Quay lại
        </Button>
        <Button onClick={() => onNext('template-default', 'job-stub-id')}>
          Tiếp tục
          <ArrowRight className="ml-2 w-4 h-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
