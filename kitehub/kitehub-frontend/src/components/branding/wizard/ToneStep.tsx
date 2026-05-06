'use client';

// ---------------------------------------------------------------------------
// ToneStep — Step 4 placeholder
// Owned by Wave 32 Bucket B.
// ---------------------------------------------------------------------------

import { Button } from '@/components/ui/button';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { WizardCard, WizardStepHeader } from './wizard-shared';
import type { WizardState } from './wizard-shared';

interface ToneStepProps {
  wizardState: WizardState;
  onNext: (tone: string) => void;
  onBack: () => void;
}

export function ToneStep({ wizardState: _wizardState, onNext, onBack }: ToneStepProps) {
  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow="Bước 4 / 6"
        title="Phong cách thương hiệu"
        subtitle="Chọn tông giọng điệu phù hợp với trung tâm của bạn."
      />
      <WizardCard>
        <p className="text-muted-foreground text-sm">
          Bước này đang được xây dựng (Wave 32 Bucket B). Nhấn &quot;Tiếp tục&quot; để bỏ qua tạm thời.
        </p>
      </WizardCard>
      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack}>
          <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
          Quay lại
        </Button>
        <Button onClick={() => onNext('professional')}>
          Tiếp tục
          <ArrowRight className="ml-2 w-4 h-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
