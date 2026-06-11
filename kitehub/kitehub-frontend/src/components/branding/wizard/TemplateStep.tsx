'use client';

// ---------------------------------------------------------------------------
// TemplateStep — Step 5 of AI Branding Wizard v2.
//
// Spec: `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step5-template-{grid,fullscreen,with-custom-prompt}.html`
//
// Compliance:
//   - `ai-branding-guidelines.md` §2.1 — free-form prompt is BANNED for non-
//     Enterprise tiers. The custom-prompt textarea is GATED behind
//     `useBrandingTier().canUseCustomPrompt === true` (i.e. tier ENTERPRISE).
//   - §2.2 — ≥6 SVG previews filtered by audience + tone. Filtering is
//     handled by `<TemplateGrid>`; this component WIRES the live wizard
//     state into it (audit caught v1 reading the state but ignoring it).
//   - §2.5 — input prompt token cap. The textarea has a hard 200-char limit
//     mirroring the spec; backend `AIInputCapService` is the real gate.
//   - §4.2 — preview-before-commit. `<TemplateFullscreen>` is the in-step
//     preview surface.
//
// Cross-bucket:
//   - `useBrandingTier` is a stub exported from `wizard-shared.tsx` until
//     Bucket D's canonical hook lands. Caller can override the resolved tier
//     via the `tierOverride` prop (used by tests + Storybook).
//   - The chosen template ID + custom prompt land in `WizardState` via
//     `dispatch` — Step 6 reads them when wiring the iframe + brand colours.
// ---------------------------------------------------------------------------

import { useMemo, useState } from 'react';
import { ArrowLeft, ArrowRight, ShieldCheck, Zap, Info } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { TemplateGrid, TEMPLATES, type TemplateDescriptor } from './TemplateGrid';
import { TemplateFullscreen } from './TemplateFullscreen';
import {
  WizardCard,
  WizardStepHeader,
  type TemplateStepProps,
} from './wizard-shared';
import { useBrandingTier, type BrandingTierInfo } from '@/hooks/use-branding-tier';
import type { PricingTier } from '@/types/subscription';
import { TIER_CAP_LABEL, REGENERATE_QUOTA, estimateTokens } from '@/config/ai-input-cap';

// ---------------------------------------------------------------------------
// CustomPromptInput — Enterprise Advanced Mode opt-in.
// Visible IFF `useBrandingTier().canUseCustomPrompt === true`.
// Per `ai-branding-guidelines.md` §2.1 + §2.4 it is BANNED for FREE/PRO/PREMIUM.
// ---------------------------------------------------------------------------

interface CustomPromptInputProps {
  value: string;
  onChange: (next: string) => void;
  /** Tier label rendered in the helper line (FREE / PRO / PREMIUM / ENTERPRISE). */
  tierLabel: string;
  /** Per `ai-branding-guidelines.md` §2.5 the per-tier cap (estimated tokens). */
  capLabel: string;
}

function CustomPromptInput({
  value,
  onChange,
  tierLabel,
  capLabel,
}: CustomPromptInputProps) {
  const max = 200;
  // Heuristic chars/4 estimate (matches `PromptTokenEstimator` in §2.5)
  // via centralized helper from `@/config/ai-input-cap`.
  const estimatedTokens = estimateTokens(value);

  return (
    <div
      className="rounded-lg border-2 p-4 mb-6 max-w-3xl mx-auto"
      style={{ borderColor: 'hsl(var(--warning) / 0.4)' }}
      data-testid="custom-prompt-section"
    >
      <div className="flex items-center gap-2 mb-2">
        <Zap
          className="w-4 h-4"
          style={{ color: 'hsl(var(--warning))' }}
          aria-hidden="true"
        />
        <h3 className="font-bold">Custom prompt (tuỳ chọn)</h3>
        <span
          className="px-2 py-0.5 text-xs font-bold rounded"
          style={{
            background: 'hsl(var(--warning) / 0.15)',
            color: 'hsl(38 92% 30%)',
          }}
        >
          ADVANCED
        </span>
      </div>

      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value.slice(0, max))}
        rows={3}
        maxLength={max}
        placeholder="VD: Giọng văn ấm áp như trường mầm non, nhưng dùng bảng màu navy + cam của trung tâm luyện thi."
        className="w-full font-mono text-sm rounded-md border p-2 focus:ring-2 focus:ring-primary"
        aria-label="Custom prompt cho AI Branding"
        data-testid="custom-prompt-textarea"
      />

      <div className="flex items-center justify-between mt-2">
        <p className="text-xs text-muted-foreground">
          Tối đa {max} ký tự · 3-stage moderation pipeline sẽ chạy
        </p>
        <span className="text-xs font-mono text-muted-foreground">
          {value.length} / {max}
        </span>
      </div>

      <div
        className="mt-3 p-2 rounded-md text-xs flex gap-2"
        style={{
          background: 'hsl(var(--info) / 0.08)',
          border: '1px solid hsl(var(--info) / 0.3)',
        }}
      >
        <ShieldCheck
          className="w-4 h-4 shrink-0 mt-0.5"
          style={{ color: 'hsl(var(--info))' }}
          aria-hidden="true"
        />
        <div>
          Estimated tokens: <code className="font-mono">~{estimatedTokens}</code> ·
          Cap {tierLabel}: <code className="font-mono">{capLabel}</code> · An toàn.
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// TemplateStep
// ---------------------------------------------------------------------------

/**
 * Local props extension — Bucket A's canonical `TemplateStepProps` does not
 * include `tierOverride`, but tests + Storybook need to drive tier deterministically.
 * Production callers omit `tierOverride` and the component falls back to D's
 * `useBrandingTier()` hook (which subscribes to the active subscription).
 *
 * The legacy stub also accepted a sentinel "PRO" string; that label maps to
 * the canonical `BASIC` tier from `PricingTier`. We accept "PRO" only for
 * test compatibility and normalize it inline.
 */
type TierOverrideValue = PricingTier | 'PRO';

export interface TemplateStepLocalProps extends TemplateStepProps {
  tierOverride?: TierOverrideValue;
  /** Optional instance ID for `useBrandingTier`; tests omit it. */
  instanceId?: string;
}

/** Synthesize a {@link BrandingTierInfo} from a forced tier (test-only path). */
function synthesizeTierInfo(forced: PricingTier): BrandingTierInfo {
  const isEnterprise = forced === 'ENTERPRISE';
  return {
    tier: forced,
    regenerateQuota: REGENERATE_QUOTA[forced],
    advancedModeEnabled: isEnterprise,
    canUseCustomPrompt: isEnterprise,
    isLoading: false,
  };
}

/** Normalize the legacy "PRO" sentinel to canonical `BASIC`. */
function normalizeTierOverride(v: TierOverrideValue | undefined): PricingTier | undefined {
  if (v === undefined) return undefined;
  return v === 'PRO' ? 'BASIC' : v;
}

export function TemplateStep({
  wizardState,
  dispatch,
  tierOverride,
  instanceId,
  onNext,
  onBack,
}: TemplateStepLocalProps) {
  // When `tierOverride` is provided (tests / Storybook), short-circuit the
  // subscription hook so the component never touches React Query in tests.
  // Production path: `useBrandingTier(instanceId)` — Bucket D's canonical hook.
  const overrideTier = normalizeTierOverride(tierOverride);
  const realTierInfo = useBrandingTier(instanceId);
  const tierInfo: BrandingTierInfo =
    overrideTier !== undefined ? synthesizeTierInfo(overrideTier) : realTierInfo;

  const [fullscreen, setFullscreen] = useState<TemplateDescriptor | null>(null);
  // §2.4 Enterprise Advanced Mode — `customPrompt` is intentionally LOCAL
  // (not persisted in WizardState). It only matters for the active step;
  // navigating away clears it, which is fine because the prompt is a
  // generation hint, not a user-saved preference.
  const [customPrompt, setCustomPrompt] = useState('');

  // Derive the selected template descriptor from state (supports tests
  // re-rendering with a pre-populated state).
  const selected = useMemo(
    () => TEMPLATES.find((t) => t.id === wizardState.templateId) ?? null,
    [wizardState.templateId],
  );

  function handleSelect(t: TemplateDescriptor) {
    // A's reducer requires both `templateId` + `jobId` on SET_TEMPLATE.
    // Bucket C does not yet have a jobId at template-pick time — backend
    // returns one when generation kicks off in Step 6. Pass an empty string
    // here as a sentinel; Step 6's generate handler MUST overwrite this with
    // the real jobId via another SET_TEMPLATE dispatch (or a follow-up
    // `SET_JOB_ID` action when added in a later wave).
    dispatch({ type: 'SET_TEMPLATE', templateId: t.id, jobId: '' });
  }

  function handleFullscreenConfirm(t: TemplateDescriptor) {
    dispatch({ type: 'SET_TEMPLATE', templateId: t.id, jobId: '' });
    setFullscreen(null);
  }

  function handleCustomPromptChange(next: string) {
    setCustomPrompt(next);
  }

  const tierCapLabel = TIER_CAP_LABEL;

  return (
    <div className="space-y-6">
      <WizardStepHeader
        eyebrow={`Bước 6 / 7${tierInfo.canUseCustomPrompt ? ' · ENTERPRISE' : ''}`}
        title={
          tierInfo.canUseCustomPrompt
            ? 'Chọn mẫu hoặc tự định hướng'
            : 'Chọn mẫu thiết kế'
        }
        subtitle={
          tierInfo.canUseCustomPrompt
            ? 'Chọn 1 trong 6 mẫu, HOẶC nhập prompt riêng để AI tạo banner theo ý bạn.'
            : `6 mẫu phù hợp với ${
                wizardState.audience
                  ? `đối tượng "${wizardState.audience}"`
                  : 'lựa chọn của bạn'
              }${
                wizardState.tone ? ` · phong cách ${wizardState.tone}` : ''
              }. AI sẽ tuỳ chỉnh theo thương hiệu của bạn.`
        }
      />

      {/* §2.1 enforcement — render IFF Enterprise. */}
      {tierInfo.canUseCustomPrompt && (
        <CustomPromptInput
          value={customPrompt}
          onChange={handleCustomPromptChange}
          tierLabel={tierInfo.tier}
          capLabel={tierCapLabel[tierInfo.tier]}
        />
      )}

      <WizardCard className="!max-w-5xl">
        <h3 className="font-bold mb-3">
          {tierInfo.canUseCustomPrompt
            ? 'Hoặc chọn 1 trong 6 mẫu'
            : '6 mẫu được lọc theo lựa chọn của bạn'}
        </h3>
        <TemplateGrid
          audience={wizardState.audience}
          tone={wizardState.tone}
          selectedId={wizardState.templateId}
          onSelect={handleSelect}
          onOpenFullscreen={(t) => setFullscreen(t)}
        />

        <p className="text-xs text-muted-foreground mt-6 max-w-2xl flex items-start gap-2">
          <Info className="w-3.5 h-3.5 mt-0.5 shrink-0" aria-hidden="true" />
          <span>
            Tất cả mẫu pass WCAG AA contrast và responsive 320px → 3840px. Mỗi
            mẫu có thể đổi font, màu phụ ở Bước 6.
          </span>
        </p>
      </WizardCard>

      <TemplateFullscreen
        template={fullscreen}
        onClose={() => setFullscreen(null)}
        onConfirm={handleFullscreenConfirm}
      />

      <div className="flex items-center justify-between max-w-3xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack} type="button">
          <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
          Quay lại
        </Button>
        <Button
          type="button"
          onClick={onNext}
          disabled={!selected}
          data-testid="template-step-next"
        >
          Xem trước
          <ArrowRight className="ml-2 w-4 h-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
