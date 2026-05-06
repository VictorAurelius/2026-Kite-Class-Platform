'use client';

// ---------------------------------------------------------------------------
// ApprovalStep — Step 6: ThemePreview (G11) + per-resource approval + deploy gate
// Owned by Wave 32 Bucket C.
// G11 ThemePreview imported from @kite/shared-ui (Wave 29 Bucket C output).
// ---------------------------------------------------------------------------

import { useState } from 'react';
import { ThemePreview } from '@kite/shared-ui';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Check } from 'lucide-react';
import { WizardCard, WizardStepHeader } from './wizard-shared';
import type { WizardState } from './wizard-shared';
import { ResourceToggle } from './ResourceToggle';

// Mock brand colours — live values come from the wizard state once the
// generate endpoint is wired. Mirrors MOCK_THEME in the branding hub page.
const MOCK_BRAND = {
  primary: '#2563eb',
  secondary: '#7c3aed',
  background: '#ffffff',
  foreground: '#1e293b',
} as const;

// Four resources requiring individual approval per ai-branding-guidelines.md §4.2.
const RESOURCES = [
  { resource: 'logo', label: 'Logo', icon: '🖼️' },
  { resource: 'colors', label: 'Bảng màu', icon: '🎨' },
  { resource: 'banner', label: 'Banner', icon: '🖼️' },
  { resource: 'hero', label: 'Hero', icon: '🌟' },
] as const;

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
  const [approvedResources, setApprovedResources] = useState<string[]>([]);

  const allApproved = RESOURCES.every((r) => approvedResources.includes(r.resource));

  function handleApprove(resource: string) {
    setApprovedResources((prev) =>
      prev.includes(resource) ? prev : [...prev, resource],
    );
  }

  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow="Bước 6 / 6"
        title="Xem trước và phê duyệt"
        subtitle="Kiểm tra từng tài nguyên và phê duyệt trước khi triển khai."
      />

      {/* G11 ThemePreview — live colour preview per ai-branding-guidelines.md §4.2 */}
      <WizardCard>
        <ThemePreview brandColors={MOCK_BRAND} initialMode="light" lang="vi" />
      </WizardCard>

      {/* Per-resource approval list */}
      <WizardCard>
        <p className="text-sm font-medium text-foreground mb-3">
          Phê duyệt từng thành phần
        </p>
        <div className="space-y-2">
          {RESOURCES.map((r) => (
            <ResourceToggle
              key={r.resource}
              resource={r.resource}
              label={r.label}
              icon={r.icon}
              approved={approvedResources.includes(r.resource)}
              onApprove={() => handleApprove(r.resource)}
            />
          ))}
        </div>
      </WizardCard>

      <div className="flex items-center justify-between max-w-2xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack}>
          <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
          Quay lại
        </Button>
        <Button
          disabled={!allApproved}
          onClick={onPublish}
          aria-label="Triển khai"
        >
          <Check className="mr-2 w-4 h-4" aria-hidden="true" />
          Triển khai
        </Button>
      </div>
    </div>
  );
}
