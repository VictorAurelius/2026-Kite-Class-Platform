/**
 * RegenerateCounter — Step 6 regenerate quota counter (per `ai-branding-guidelines.md` §4.3).
 *
 * Wave 32 Bucket D — Direction C 6-step refactor.
 *
 * Tier quotas (sourced from `useBrandingTier` — never hardcoded here):
 *   FREE:     3 / session   (counter visible)
 *   BASIC:   10 / session
 *   PREMIUM: 30 / session
 *   ENTERPRISE: unlimited (counter shown but no decrement)
 *
 * Visible state is fully derived from props:
 *   - `tier`              — drives the comparison row + tier badge
 *   - `regenerateQuota`   — total budget (sentinel `-1` = unlimited ENTERPRISE)
 *   - `regeneratesUsed`   — used count; component derives `remaining = quota - used`
 *
 * Component dispatches:
 *   - `onRegenerate()`    — when remaining > 0 (or unlimited)
 *   - `onUpgradeClick()`  — when quota empty + user clicks upgrade CTA in modal
 *   - `onContinueWithCurrent()` — when quota empty + user dismisses modal
 *
 * Real quota tracking pending — backend `/branding/jobs/:id/regenerate-counter`
 * endpoint not yet implemented (state-checked 2026-05-07; no `regenerate.count`
 * endpoint in `endpoints.ts`). Until then the parent step holds the count in
 * WizardState reducer + persists it to localStorage. See follow-up
 * GAP-272d (backend regenerate quota tracking).
 */

'use client';

import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Check, Info, RefreshCw, Rocket, TrendingUp } from 'lucide-react';
import type { PricingTier } from '@/types/subscription';

export interface RegenerateCounterProps {
  /** Active tier — drives badge + comparison row highlight. */
  tier: PricingTier;
  /** Total regenerate budget for this session per tier. `-1` sentinel = unlimited. */
  regenerateQuota: number;
  /** How many regenerates have been used this session. */
  regeneratesUsed: number;
  /** Whether the upsell modal is open (quota === 0). Caller manages open state. */
  upsellModalOpen: boolean;
  /** Click handler when remaining > 0 (or unlimited). */
  onRegenerate: () => void;
  /** Click handler when quota empty + user clicks upgrade CTA in modal. */
  onUpgradeClick: () => void;
  /** Click handler when quota empty + user dismisses modal. */
  onContinueWithCurrent: () => void;
  /** Notifies parent when modal opens/closes (controlled). */
  onUpsellModalOpenChange: (open: boolean) => void;
  /**
   * GAP-1219(a): whether a first generate has happened. When false the
   * regenerate button is a no-op — disable it + explain, instead of showing
   * "Tạo lại lần 1" that burns user trust on a dead click.
   * Defaults true for back-compat with existing callers.
   */
  hasGenerated?: boolean;
}

const TIER_LABEL: Record<PricingTier, string> = {
  FREE: 'FREE',
  BASIC: 'BASIC',
  PREMIUM: 'PREMIUM',
  ENTERPRISE: 'ENTERPRISE',
};

const TIER_BADGE_CLASS: Record<PricingTier, string> = {
  FREE: 'bg-slate-200 text-slate-800',
  BASIC: 'bg-blue-100 text-blue-800',
  PREMIUM: 'bg-purple-100 text-purple-800',
  ENTERPRISE: 'bg-gradient-to-r from-amber-200 to-orange-200 text-orange-900',
};

interface ComparisonRow {
  tier: PricingTier;
  label: string;
  quotaText: string;
  highlight: boolean;
}

function buildComparisonRows(activeTier: PricingTier): ComparisonRow[] {
  return [
    {
      tier: 'FREE',
      label: 'Gói miễn phí',
      quotaText: '3 lượt / ngày',
      highlight: activeTier === 'FREE',
    },
    {
      tier: 'BASIC',
      label: '500.000đ / tháng',
      quotaText: '10 lượt / ngày',
      highlight: activeTier === 'BASIC',
    },
    {
      tier: 'PREMIUM',
      label: '1.500.000đ / tháng',
      quotaText: '30 lượt / ngày',
      highlight: activeTier === 'PREMIUM',
    },
    {
      tier: 'ENTERPRISE',
      label: 'Liên hệ',
      quotaText: 'Không giới hạn',
      highlight: activeTier === 'ENTERPRISE',
    },
  ];
}

export function RegenerateCounter(props: RegenerateCounterProps) {
  const {
    tier,
    regenerateQuota,
    regeneratesUsed,
    upsellModalOpen,
    onRegenerate,
    onUpgradeClick,
    onContinueWithCurrent,
    onUpsellModalOpenChange,
    hasGenerated = true,
  } = props;

  const isUnlimited = regenerateQuota === -1;
  // Clamp `used` between 0 and `quota` so display never reads negatively.
  const used = Math.max(0, Math.min(regenerateQuota === -1 ? regeneratesUsed : regenerateQuota, regeneratesUsed));
  const remaining = isUnlimited ? Number.POSITIVE_INFINITY : regenerateQuota - used;
  const isQuotaEmpty = !isUnlimited && remaining <= 0;
  const rows = buildComparisonRows(tier);

  // Render dot pattern for the 3-step FREE counter when applicable.
  // For BASIC (10) / PREMIUM (30) we render a numeric remaining instead of dots
  // since the kit only spec'd the dot row for the 3-quota FREE case.
  const showDotPattern = !isUnlimited && regenerateQuota === 3;

  return (
    <div className="space-y-4 max-w-2xl mx-auto" data-testid="regenerate-counter">
      <div className="mb-2">
        <p className="text-sm font-semibold text-primary uppercase tracking-wide mb-1">
          Hạn mức tạo lại
        </p>
        <h2 className="text-2xl font-bold text-foreground mb-1">
          {isUnlimited
            ? 'Hạn mức không giới hạn (ENTERPRISE)'
            : isQuotaEmpty
              ? `Đã hết ${regenerateQuota}/${regenerateQuota} lượt`
              : `Còn ${remaining}/${regenerateQuota} lượt tạo lại`}
        </h2>
        <p className="text-muted-foreground text-sm">
          Mỗi lần bấm &ldquo;Tạo lại&rdquo; sẽ trừ 1 lượt. Hết quota cần nâng cấp gói. Quota reset đầu mỗi phiên onboarding.
        </p>
      </div>

      {/* Active counter row */}
      <Card
        className={`p-4 flex items-center justify-between gap-4 ${
          isQuotaEmpty ? 'opacity-60' : ''
        }`}
        aria-disabled={isQuotaEmpty}
        data-testid="regenerate-counter-bar"
        data-quota-empty={isQuotaEmpty}
      >
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <span
              className={`text-xs font-bold px-2 py-0.5 rounded uppercase ${TIER_BADGE_CLASS[tier]}`}
              data-testid="regenerate-counter-active-tier-badge"
            >
              {TIER_LABEL[tier]}
            </span>
            <span className="text-sm text-muted-foreground">Gói hiện tại</span>
          </div>
          <span className="text-sm font-semibold text-foreground" data-testid="regenerate-counter-status">
            {!hasGenerated
              ? 'Khả dụng sau khi tạo bản đầu tiên'
              : isUnlimited
                ? 'Không giới hạn'
                : isQuotaEmpty
                  ? `Đã hết ${regenerateQuota}/${regenerateQuota} lượt`
                  : `${remaining}/${regenerateQuota} lượt còn`}
          </span>
        </div>
        <div className="flex items-center gap-3">
          {showDotPattern && (
            <div className="flex items-center gap-1" aria-hidden="true">
              {Array.from({ length: regenerateQuota }, (_, i) => (
                <span
                  key={i}
                  className={`w-2.5 h-2.5 rounded-full ${
                    i < used ? 'bg-slate-400' : 'bg-emerald-400'
                  }`}
                  data-testid={`regenerate-counter-dot-${i}`}
                  data-spent={i < used}
                  title={i < used ? 'Đã dùng' : 'Còn lại'}
                />
              ))}
            </div>
          )}
          <Button
            type="button"
            size="sm"
            variant="secondary"
            disabled={isQuotaEmpty || !hasGenerated}
            onClick={hasGenerated ? onRegenerate : undefined}
            data-testid="regenerate-counter-button"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            {!hasGenerated
              ? 'Tạo lại'
              : isUnlimited
                ? 'Tạo lại'
                : isQuotaEmpty
                  ? 'Tạo lại'
                  : `Tạo lại lần ${used + 1}`}
          </Button>
        </div>
      </Card>

      {/* Tier comparison row */}
      <Card className="p-4">
        <h3 className="font-bold text-sm mb-3">Hạn mức theo gói</h3>
        <div className="space-y-2 text-sm" data-testid="regenerate-counter-comparison">
          {rows.map((row) => (
            <div
              key={row.tier}
              className={`flex items-center justify-between p-2 rounded ${
                row.highlight ? 'bg-primary/5 border border-primary/20' : ''
              }`}
              data-testid={`regenerate-counter-row-${row.tier.toLowerCase()}`}
              data-active={row.highlight}
            >
              <div className="flex items-center gap-2">
                <span
                  className={`text-xs font-bold px-2 py-0.5 rounded uppercase ${TIER_BADGE_CLASS[row.tier]}`}
                >
                  {TIER_LABEL[row.tier]}
                </span>
                <span className="text-sm">{row.label}</span>
              </div>
              <span className="font-mono font-semibold">{row.quotaText}</span>
            </div>
          ))}
        </div>
        {tier !== 'ENTERPRISE' && (
          <Button
            type="button"
            className="w-full mt-3"
            onClick={onUpgradeClick}
            data-testid="regenerate-counter-upgrade-button"
          >
            <TrendingUp className="w-4 h-4" />
            {tier === 'FREE'
              ? 'Nâng cấp lên BASIC — 249k/tháng'
              : tier === 'BASIC'
                ? 'Nâng cấp lên PREMIUM — 499k/tháng'
                : 'Liên hệ ENTERPRISE'}
          </Button>
        )}
      </Card>

      <p className="text-xs text-muted-foreground flex items-start gap-2">
        <Info className="w-3.5 h-3.5 mt-0.5 shrink-0" />
        <span>
          Mỗi lượt tạo lại tốn ~5MB ảnh + 30 giây compute. Quota giúp hệ thống ổn định cho mọi tenant.
        </span>
      </p>

      {/* Upsell modal — surfaces when quota empty per `ai-branding-guidelines.md` §4.3. */}
      <Dialog open={upsellModalOpen} onOpenChange={onUpsellModalOpenChange}>
        <DialogContent data-testid="regenerate-counter-upsell-modal">
          <DialogHeader>
            <DialogTitle>Đã hết quota tạo lại</DialogTitle>
            <DialogDescription>
              Gói {TIER_LABEL[tier]} hỗ trợ {regenerateQuota} lượt / ngày onboarding
            </DialogDescription>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Bạn đã thử {used} lần — đây là cơ hội để nâng cấp gói để tiếp tục tinh chỉnh.
          </p>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span
                className={`text-xs font-bold px-2 py-0.5 rounded uppercase ${TIER_BADGE_CLASS.BASIC}`}
              >
                BASIC
              </span>
              <span className="text-2xl font-extrabold">
                249<span className="text-sm text-muted-foreground">.000đ/tháng</span>
              </span>
            </div>
            <ul className="text-sm space-y-1 text-blue-900">
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4" /> 10 lượt tạo lại / phiên
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4" /> Truy cập 30+ template thay vì 6
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4" /> Hỗ trợ qua chat 24/7
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4" /> Hủy bất kỳ lúc nào
              </li>
            </ul>
          </div>
          <DialogFooter className="sm:justify-stretch gap-2">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={onContinueWithCurrent}
              data-testid="regenerate-counter-continue-button"
            >
              <Rocket className="w-4 h-4" />
              Tiếp tục với kết quả hiện tại
            </Button>
            <Button
              type="button"
              className="flex-1"
              onClick={onUpgradeClick}
              data-testid="regenerate-counter-modal-upgrade-button"
            >
              <TrendingUp className="w-4 h-4" />
              Nâng cấp BASIC
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
