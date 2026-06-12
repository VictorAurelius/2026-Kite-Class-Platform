'use client';

// ---------------------------------------------------------------------------
// GenerationModeSelector — Step 7 generation-mode picker (Wave wizard-step7 / GAP-1142)
//
// Tier-gated TEMPLATE vs FULL_AI selection per SUB-22 / ADR-037: FULL_AI is
// eligible ONLY for PREMIUM + ENTERPRISE. FREE/BASIC see a disabled FULL_AI
// card with an upgrade CTA. PREMIUM shows the monthly FULL_AI quota; once the
// quota is spent the option is disabled. ENTERPRISE is unlimited.
//
// Pure presentational + tier logic: parent passes tier + quota, no fetching.
// Visual style mirrors RegenerateCounter (tier badge) + ToneCard (radio cards).
// ---------------------------------------------------------------------------

import { Lock, Sparkles, LayoutTemplate, CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { PricingTier } from '@/types/subscription';

export type GenerationMode = 'TEMPLATE' | 'FULL_AI';

export interface GenerationModeSelectorProps {
  /** Active subscription tier — drives FULL_AI eligibility gate. */
  tier: PricingTier;
  /** Currently selected mode (controlled). */
  value: GenerationMode;
  /** Dispatched when the user picks a selectable mode. */
  onChange: (mode: GenerationMode) => void;
  /**
   * Remaining FULL_AI generations this month.
   * `null` = unlimited (ENTERPRISE); `number` = remaining quota (PREMIUM).
   */
  fullAiRemaining?: number | null;
  /** PREMIUM monthly FULL_AI quota (e.g. 5). */
  fullAiLimit?: number;
  /** CTA handler for FREE/BASIC upgrade prompt. */
  onUpgradeClick?: () => void;
  /** Disables the whole control (e.g. while a job is running). */
  disabled?: boolean;
}

/** Tier badge palette — mirrors RegenerateCounter TIER_BADGE_CLASS. */
const PREMIUM_BADGE_CLASS = 'bg-purple-100 text-purple-800';

export function GenerationModeSelector({
  tier,
  value,
  onChange,
  fullAiRemaining,
  fullAiLimit,
  onUpgradeClick,
  disabled = false,
}: GenerationModeSelectorProps) {
  // FULL_AI is only available for PREMIUM + ENTERPRISE (SUB-22 / ADR-037).
  const isFullAiTier = tier === 'PREMIUM' || tier === 'ENTERPRISE';
  const hasNumericRemaining = typeof fullAiRemaining === 'number';
  const premiumQuotaExhausted =
    tier === 'PREMIUM' && hasNumericRemaining && fullAiRemaining === 0;

  // FREE/BASIC: FULL_AI gated → show upgrade CTA instead.
  const showUpgradeCta = !isFullAiTier;
  // FULL_AI clickable only when eligible tier + quota remaining + not disabled.
  const fullAiSelectable = !disabled && isFullAiTier && !premiumQuotaExhausted;
  const fullAiDisabled = !fullAiSelectable;

  const templateSelected = value === 'TEMPLATE';
  const fullAiSelected = value === 'FULL_AI';

  function handleSelect(mode: GenerationMode) {
    if (disabled) return;
    if (mode === 'FULL_AI' && !fullAiSelectable) return;
    onChange(mode);
  }

  /** FULL_AI sub-label varies by tier + quota state. */
  function fullAiSubLabel(): string {
    if (!isFullAiTier) {
      return 'Chỉ dành cho gói PREMIUM trở lên';
    }
    if (tier === 'ENTERPRISE') {
      return 'Không giới hạn';
    }
    // PREMIUM
    if (premiumQuotaExhausted) {
      return 'Đã hết lượt AI cao cấp tháng này — dùng Mẫu hoặc nâng cấp';
    }
    if (hasNumericRemaining) {
      return `Còn ${fullAiRemaining}/${fullAiLimit ?? fullAiRemaining} lượt tháng này`;
    }
    return 'Khả dụng tháng này';
  }

  return (
    <div
      role="radiogroup"
      aria-label="Chế độ tạo thương hiệu"
      aria-disabled={disabled}
      data-testid="generation-mode-selector"
      className="grid gap-3 sm:grid-cols-2"
    >
      {/* ---------------- TEMPLATE — always selectable ---------------- */}
      <button
        type="button"
        role="radio"
        aria-checked={templateSelected}
        aria-label="Mẫu"
        disabled={disabled}
        data-testid="mode-option-template"
        data-selected={templateSelected}
        onClick={() => handleSelect('TEMPLATE')}
        className={cn(
          'group relative flex flex-col gap-2 rounded-xl border p-5 text-left transition',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2',
          disabled && 'cursor-not-allowed opacity-60',
          templateSelected
            ? 'border-primary shadow-md ring-2 ring-primary/40'
            : 'border-input hover:shadow-sm',
        )}
      >
        <div className="flex items-start justify-between">
          <LayoutTemplate
            className="h-6 w-6 text-foreground"
            aria-hidden="true"
          />
          <div className="flex items-center gap-1.5">
            <span
              data-testid="mode-template-recommended"
              className="rounded bg-emerald-100 px-2 py-0.5 text-[10px] font-bold uppercase text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300"
            >
              Khuyến nghị
            </span>
            {templateSelected && (
              <CheckCircle2
                className="h-5 w-5 text-emerald-600"
                aria-hidden="true"
              />
            )}
          </div>
        </div>
        <div className="text-base font-semibold text-foreground">Mẫu dựng sẵn</div>
        <p className="text-sm text-muted-foreground">
          AI điền nội dung trung tâm vào mẫu rồi chụp thành ảnh. Chữ tiếng Việt sắc nét, nhanh, ổn định.
        </p>
        <div className="mt-1 flex flex-wrap gap-1.5">
          <ModeChip>Miễn phí</ModeChip>
          <ModeChip>~10 giây</ModeChip>
          <ModeChip>Chữ Việt nét</ModeChip>
        </div>
      </button>

      {/* ---------------- FULL_AI — tier-gated ---------------- */}
      <div className="flex flex-col gap-2">
        <button
          type="button"
          role="radio"
          aria-checked={fullAiSelected}
          aria-label="AI cao cấp"
          aria-disabled={fullAiDisabled}
          disabled={fullAiDisabled}
          data-testid="mode-option-full-ai"
          data-selected={fullAiSelected}
          data-locked={!isFullAiTier}
          data-quota-exhausted={premiumQuotaExhausted}
          onClick={() => handleSelect('FULL_AI')}
          className={cn(
            'group relative flex flex-1 flex-col gap-2 rounded-xl border p-5 text-left transition',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2',
            fullAiDisabled && 'cursor-not-allowed opacity-60',
            fullAiSelected
              ? 'border-primary shadow-md ring-2 ring-primary/40'
              : 'border-input hover:shadow-sm',
          )}
        >
          <div className="flex items-start justify-between">
            <Sparkles className="h-6 w-6 text-purple-600" aria-hidden="true" />
            <div className="flex items-center gap-1.5">
              {!isFullAiTier && (
                <Lock
                  className="h-4 w-4 text-muted-foreground"
                  aria-hidden="true"
                  data-testid="mode-full-ai-lock"
                />
              )}
              <span
                className={cn(
                  'rounded px-2 py-0.5 text-[10px] font-bold uppercase',
                  PREMIUM_BADGE_CLASS,
                )}
              >
                Premium
              </span>
              {fullAiSelected && (
                <CheckCircle2
                  className="h-5 w-5 text-emerald-600"
                  aria-hidden="true"
                />
              )}
            </div>
          </div>
          <div className="text-base font-semibold text-foreground">
            AI vẽ toàn bộ
          </div>
          <p className="text-sm text-muted-foreground">
            AI vẽ cả ảnh banner theo phong cách riêng. Phù hợp khi muốn banner độc đáo.
          </p>
          <div className="mt-1 flex flex-wrap gap-1.5">
            <ModeChip>PREMIUM / ENTERPRISE</ModeChip>
            <ModeChip>Ảnh AI gốc</ModeChip>
          </div>
          <p
            className="mt-1 text-xs text-muted-foreground"
            data-testid="mode-full-ai-sublabel"
          >
            {fullAiSubLabel()}
          </p>
        </button>

        {/* Upgrade CTA — only FREE/BASIC, where FULL_AI is gated. */}
        {showUpgradeCta && (
          <Button
            type="button"
            size="sm"
            variant="secondary"
            disabled={disabled}
            onClick={onUpgradeClick}
            data-testid="mode-upgrade-cta"
            className="w-full"
          >
            <Sparkles className="h-3.5 w-3.5" />
            Nâng cấp lên PREMIUM để dùng AI cao cấp
          </Button>
        )}
      </div>
    </div>
  );
}

/** Small pill used for the mode-card affordance chips (kit v3). */
function ModeChip({ children }: { children: React.ReactNode }) {
  return (
    <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] font-medium text-muted-foreground">
      {children}
    </span>
  );
}
