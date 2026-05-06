'use client';

// ---------------------------------------------------------------------------
// ApprovalStep — Step 6 placeholder
// Owned by Wave 32 Bucket D (per-resource approval + publish).
// ---------------------------------------------------------------------------

import { Button } from '@/components/ui/button';
import { ArrowLeft, Check } from 'lucide-react';
import { WizardCard, WizardStepHeader } from './wizard-shared';
import type { WizardState } from './wizard-shared';

interface ApprovalStepProps {
  wizardState: WizardState;
  jobId: string;
  onPublish: () => void;
  onBack: () => void;
}

export function ApprovalStep({
  wizardState: _wizardState,
  jobId: _jobId,
  onPublish,
  onBack,
}: ApprovalStepProps) {
  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow="Bước 6 / 6"
        title="Xem trước và phê duyệt"
        subtitle="Kiểm tra từng tài nguyên và phê duyệt trước khi triển khai."
      />
      <WizardCard>
        <p className="text-muted-foreground text-sm">
          Bước này đang được xây dựng (Wave 32 Bucket D). Nhấn &quot;Hoàn tất&quot; để kết thúc.
        </p>
      </WizardCard>
      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack}>
          <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
          Quay lại
        </Button>
        <Button onClick={onPublish}>
          <Check className="mr-2 w-4 h-4" aria-hidden="true" />
          Hoàn tất
        </Button>
      </div>
    </div>
  );
}
