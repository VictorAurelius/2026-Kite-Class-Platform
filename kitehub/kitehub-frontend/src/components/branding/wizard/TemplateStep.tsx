'use client';

// ---------------------------------------------------------------------------
// TemplateStep — Step 5: template grid (6 presets) + generate trigger
// Owned by Wave 32 Bucket C.
// ---------------------------------------------------------------------------

import { Button } from '@/components/ui/button';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { WizardCard, WizardStepHeader } from './wizard-shared';
import type { WizardState } from './wizard-shared';

// 6 template presets — mirrors ai-branding-guidelines.md §2.2 requirement for
// ≥6 visual previews. Real thumbnails land once backend generate endpoint is
// wired; bg classes provide a distinct colour hint per template.
const TEMPLATES = [
  { id: 'template-edu-1', name: 'Mẫu 1', bg: 'bg-blue-50 border-blue-200' },
  { id: 'template-edu-2', name: 'Mẫu 2', bg: 'bg-purple-50 border-purple-200' },
  { id: 'template-edu-3', name: 'Mẫu 3', bg: 'bg-green-50 border-green-200' },
  { id: 'template-edu-4', name: 'Mẫu 4', bg: 'bg-orange-50 border-orange-200' },
  { id: 'template-edu-5', name: 'Mẫu 5', bg: 'bg-pink-50 border-pink-200' },
  { id: 'template-edu-6', name: 'Mẫu 6', bg: 'bg-indigo-50 border-indigo-200' },
] as const;

// Stub job ID — real value comes from the backend generate endpoint once wired.
const STUB_JOB_ID = 'job-stub-id';

interface TemplateStepProps {
  wizardState: WizardState;
  instanceId: string;
  onNext: (templateId: string, jobId: string) => void;
  onBack: () => void;
}

export function TemplateStep({
  wizardState,
  instanceId: _instanceId,
  onNext,
  onBack,
}: TemplateStepProps) {
  const selected = wizardState.templateId;

  function handleSelect(id: string) {
    onNext(id, STUB_JOB_ID);
  }

  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow="Bước 5 / 6"
        title="Chọn mẫu thiết kế"
        subtitle="AI sẽ tùy chỉnh mẫu theo thương hiệu của bạn."
      />

      <WizardCard>
        <div className="grid grid-cols-3 gap-3">
          {TEMPLATES.map((t) => (
            <button
              key={t.id}
              type="button"
              aria-label={t.name}
              aria-pressed={selected === t.id}
              onClick={() => handleSelect(t.id)}
              className={`rounded-lg border-2 p-3 h-24 transition-all text-xs font-medium text-center ${t.bg} ${
                selected === t.id
                  ? 'ring-2 ring-primary border-primary'
                  : 'hover:border-primary/50'
              }`}
            >
              {t.name}
            </button>
          ))}
        </div>
      </WizardCard>

      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack}>
          <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
          Quay lại
        </Button>
        <Button
          onClick={() => onNext(selected ?? 'template-edu-1', STUB_JOB_ID)}
          disabled={!selected}
        >
          Tiếp tục
          <ArrowRight className="ml-2 w-4 h-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
