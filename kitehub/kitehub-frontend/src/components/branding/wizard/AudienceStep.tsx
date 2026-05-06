'use client';

// ---------------------------------------------------------------------------
// AudienceStep — Step 3 placeholder
// Owned by Wave 32 Bucket B (audience + tone steps).
// This stub satisfies TypeScript imports from the wizard shell.
// ---------------------------------------------------------------------------

import { Button } from '@/components/ui/button';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { WizardCard, WizardStepHeader } from './wizard-shared';
import type { WizardState } from './wizard-shared';

interface AudienceStepProps {
  wizardState: WizardState;
  onNext: (audience: string) => void;
  onBack: () => void;
}

export function AudienceStep({ wizardState: _wizardState, onNext, onBack }: AudienceStepProps) {
  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow="Bước 3 / 6"
        title="Đối tượng học viên"
        subtitle="Chọn nhóm học viên chính của trung tâm để AI tạo giao diện phù hợp."
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
        <Button onClick={() => onNext('mixed')}>
          Tiếp tục
          <ArrowRight className="ml-2 w-4 h-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
