'use client';

/**
 * RegenerateCounter — Wave 32 Bucket D (GAP-272)
 *
 * Shows remaining regenerate quota per tier (ai-branding-guidelines.md §4.3):
 *   FREE: 3 | BASIC: 10 | PREMIUM: 30 | ENTERPRISE: unlimited
 *
 * Quota-empty state triggers upsell modal.
 * Counter is always visible (§4.3 mandate: "Counter PHẢI visible").
 */

import React, { useState } from 'react';
import { RefreshCw, ArrowUp, Infinity } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { PricingTier } from '@/types/subscription';

const TIER_LABELS: Record<PricingTier, string> = {
  FREE: 'FREE',
  BASIC: 'BASIC',
  PREMIUM: 'PREMIUM',
  ENTERPRISE: 'ENTERPRISE',
};

const TIER_BADGE_CLASS: Record<PricingTier, string> = {
  FREE: 'bg-gray-100 text-gray-700 border-gray-200',
  BASIC: 'bg-blue-100 text-blue-700 border-blue-200',
  PREMIUM: 'bg-purple-100 text-purple-700 border-purple-200',
  ENTERPRISE: 'bg-amber-100 text-amber-800 border-amber-200',
};

/** Next upgrade tier for upsell — null when at top tier */
const NEXT_TIER: Partial<Record<PricingTier, PricingTier>> = {
  FREE: 'BASIC',
  BASIC: 'PREMIUM',
  PREMIUM: 'ENTERPRISE',
};

export interface RegenerateCounterProps {
  tier: PricingTier;
  /** Total quota (-1 = unlimited). */
  regenerateQuota: number;
  /** Used count. */
  regenerateUsed: number;
  /** Called when user clicks "Tạo lại". Caller handles actual regen. */
  onRegenerate?: () => void;
  /** Called when user clicks upgrade in upsell modal. */
  onUpgrade?: (targetTier: PricingTier) => void;
  /** Whether a regenerate is in progress. */
  isRegenerating?: boolean;
}

function UpsellModal({
  currentTier,
  onUpgrade,
  onClose,
}: {
  currentTier: PricingTier;
  onUpgrade?: (t: PricingTier) => void;
  onClose: () => void;
}) {
  const nextTier = NEXT_TIER[currentTier];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      role="dialog"
      aria-modal="true"
      aria-label="Nâng cấp gói để tạo lại thêm"
    >
      <div className="bg-white rounded-2xl shadow-2xl p-6 max-w-sm w-full mx-4 space-y-4">
        <div className="text-center space-y-1">
          <p className="text-2xl">🚀</p>
          <h2 className="text-lg font-bold">Hết lượt tạo lại</h2>
          <p className="text-sm text-muted-foreground">
            Gói {TIER_LABELS[currentTier]} cho phép tạo lại{' '}
            {currentTier === 'FREE' ? '3' : currentTier === 'BASIC' ? '10' : '30'} lần/phiên.
            Nâng cấp để tạo thêm.
          </p>
        </div>

        {nextTier && (
          <Button
            className="w-full"
            onClick={() => {
              onUpgrade?.(nextTier);
              onClose();
            }}
          >
            <ArrowUp className="w-4 h-4 mr-2" aria-hidden />
            Nâng lên {TIER_LABELS[nextTier]}
          </Button>
        )}

        <Button variant="outline" className="w-full" onClick={onClose}>
          Đóng
        </Button>
      </div>
    </div>
  );
}

export function RegenerateCounter({
  tier,
  regenerateQuota,
  regenerateUsed,
  onRegenerate,
  onUpgrade,
  isRegenerating = false,
}: RegenerateCounterProps) {
  const [showUpsell, setShowUpsell] = useState(false);

  const isUnlimited = regenerateQuota === -1;
  const remaining = isUnlimited ? -1 : Math.max(0, regenerateQuota - regenerateUsed);
  const isExhausted = !isUnlimited && remaining <= 0;

  function handleRegenerate() {
    if (isExhausted) {
      setShowUpsell(true);
      return;
    }
    onRegenerate?.();
  }

  // Bar fill percentage
  const fillPct = isUnlimited
    ? 100
    : regenerateQuota > 0
      ? Math.round((remaining / regenerateQuota) * 100)
      : 0;

  return (
    <>
      <div
        className="rounded-xl border bg-white p-4 space-y-3"
        aria-label={`Regenerate counter: ${isUnlimited ? 'không giới hạn' : `${remaining}/${regenerateQuota} lượt còn`}`}
      >
        {/* Tier badge + counter label */}
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <span
              className={`inline-block px-2 py-0.5 rounded-full border text-xs font-bold ${TIER_BADGE_CLASS[tier]}`}
            >
              {TIER_LABELS[tier]}
            </span>
            <span className="text-sm font-semibold text-foreground">
              {isUnlimited ? (
                <span className="flex items-center gap-1">
                  <Infinity className="w-4 h-4" aria-hidden /> Không giới hạn
                </span>
              ) : (
                `${remaining}/${regenerateQuota} lượt còn`
              )}
            </span>
          </div>

          {isExhausted && (
            <span className="text-xs text-destructive font-semibold">Hết quota</span>
          )}
        </div>

        {/* Progress bar (hidden for unlimited) */}
        {!isUnlimited && (
          <div className="w-full h-1.5 rounded-full bg-muted overflow-hidden" aria-hidden>
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                isExhausted
                  ? 'bg-destructive'
                  : remaining <= 1
                    ? 'bg-amber-500'
                    : 'bg-primary'
              }`}
              style={{ width: `${fillPct}%` }}
            />
          </div>
        )}

        {/* Regen button */}
        <Button
          variant={isExhausted ? 'outline' : 'secondary'}
          size="sm"
          disabled={isRegenerating}
          onClick={handleRegenerate}
          className={isExhausted ? 'border-destructive text-destructive hover:bg-destructive/5' : ''}
          aria-disabled={isExhausted && !onUpgrade}
        >
          <RefreshCw
            className={`w-4 h-4 mr-2 ${isRegenerating ? 'animate-spin' : ''}`}
            aria-hidden
          />
          {isRegenerating
            ? 'Đang tạo lại…'
            : isExhausted
              ? 'Nâng cấp để tạo lại thêm'
              : 'Tạo lại'}
        </Button>
      </div>

      {showUpsell && (
        <UpsellModal
          currentTier={tier}
          onUpgrade={onUpgrade}
          onClose={() => setShowUpsell(false)}
        />
      )}
    </>
  );
}
