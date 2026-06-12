'use client';

/**
 * AssetsStep — Step 3 of the output-first AI Branding Wizard (GAP-1216 / GAP-1229).
 *
 * Merges the former Logo (Step 2) + Portrait (Step 3) screens onto ONE optional
 * "Hình ảnh" page. Both sub-sections are skippable — the AI composes a banner
 * (monogram logo + portrait-less hero) when nothing is uploaded.
 *
 * "Components giữ": the heavy upload UI + hooks stay in `LogoStep` / `PortraitStep`;
 * they render here in `embedded` mode (own footers suppressed) so this composite
 * owns a single shared footer + step header.
 *
 * Branch (GAP-1134): the Portrait sub-section only appears in FULL_AI mode, where
 * the generated banner uses a featured teacher headshot. In TEMPLATE mode the
 * template supplies the hero imagery, so portraits are not collected here.
 */

import { ArrowLeft, ArrowRight, Image as ImageIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { LogoStep } from './LogoStep';
import { PortraitStep } from './PortraitStep';
import type { WizardState, WizardAction } from './wizard-shared';

export interface AssetsStepProps {
  wizardState: WizardState;
  dispatch: React.Dispatch<WizardAction>;
  /** Tenant instance — required for the upload endpoint paths. */
  instanceId: string;
  onNext: () => void;
  onBack: () => void;
}

export function AssetsStep({
  wizardState,
  dispatch,
  instanceId,
  onNext,
  onBack,
}: AssetsStepProps) {
  const showPortrait = wizardState.mode === 'FULL_AI';

  return (
    <div className="space-y-6" data-testid="assets-step">
      <div className="mx-auto max-w-2xl space-y-2">
        <p className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-primary">
          <ImageIcon className="h-3.5 w-3.5" aria-hidden="true" />
          Bước 3 / 5 · Tuỳ chọn
        </p>
        <h1 className="text-2xl font-bold text-foreground">Hình ảnh thương hiệu</h1>
        <p className="text-muted-foreground">
          Thêm logo{showPortrait ? ' và ảnh chân dung giáo viên' : ''} nếu bạn đã có — hoặc bỏ qua,
          AI vẫn tạo được trang web hoàn chỉnh.
        </p>
      </div>

      {/* Logo sub-section (heavy upload UI reused, footer suppressed). */}
      <LogoStep
        wizardState={wizardState}
        dispatch={dispatch}
        instanceId={instanceId}
        embedded
      />

      {/* Portrait sub-section — FULL_AI only (GAP-1134). */}
      {showPortrait && (
        <PortraitStep
          wizardState={wizardState}
          dispatch={dispatch}
          instanceId={instanceId}
          embedded
        />
      )}

      {/* Single shared footer. Both sub-sections are optional → always allow
          continue (the user can skip all uploads). */}
      <div className="mx-auto flex max-w-2xl items-center justify-between px-1">
        <Button variant="ghost" onClick={onBack} data-testid="assets-back">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
        <p className="text-xs text-muted-foreground">
          Bước 3 / 5 · Tuỳ chọn — bỏ qua nếu chưa có
        </p>
        <Button onClick={onNext} data-testid="assets-continue">
          Tiếp tục
          <ArrowRight className="ml-2 h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
