'use client';

/**
 * Settings → Branding → Advanced Mode — Wave 32 Bucket D (GAP-272)
 *
 * Enterprise-gated page for enabling AI Branding Advanced Mode.
 * Per ai-branding-guidelines.md §2.4:
 *   - Only ENTERPRISE tier may enable free-form prompt
 *   - Requires explicit opt-in via disclaimer modal
 *   - Non-ENTERPRISE users see upgrade prompt
 *
 * TODO(GAP-272): Wire real subscription tier via useActiveSubscription once
 *               instanceId flows through settings layout context.
 */

import React, { useState } from 'react';
import Link from 'next/link';
import { ChevronRight, Sparkles, Lock, ToggleLeft, ToggleRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useBrandingTier } from '@/hooks/use-branding-tier';
import { AdvancedModeDisclaimer } from '@/components/branding/wizard/AdvancedModeDisclaimer';

const TIER_LABELS = {
  FREE: 'FREE',
  BASIC: 'BASIC',
  PREMIUM: 'PREMIUM',
  ENTERPRISE: 'ENTERPRISE',
} as const;

function UpgradePrompt() {
  return (
    <div className="max-w-lg mx-auto py-12 text-center space-y-6">
      <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-amber-100">
        <Lock className="w-8 h-8 text-amber-600" aria-hidden />
      </div>
      <div className="space-y-2">
        <h1 className="text-2xl font-bold">Advanced Mode</h1>
        <p className="text-muted-foreground text-sm leading-relaxed">
          Tính năng này chỉ dành cho gói <strong>ENTERPRISE</strong>. Nâng cấp để mở khoá
          custom prompt tối đa 200 ký tự và toàn quyền kiểm soát AI Branding.
        </p>
      </div>
      <div className="rounded-xl border bg-amber-50 border-amber-200 p-5 text-left space-y-3 text-sm text-amber-900">
        <p className="font-semibold flex items-center gap-2">
          <Sparkles className="w-4 h-4 shrink-0" aria-hidden />
          Gói ENTERPRISE bao gồm:
        </p>
        <ul className="list-disc list-inside space-y-1 text-amber-800">
          <li>Advanced Mode — custom prompt AI không giới hạn</li>
          <li>Không giới hạn lượt tạo lại</li>
          <li>Input token cap 16,000 tokens</li>
          <li>Ưu tiên hàng đợi AI (queue priority: ENTERPRISE)</li>
          <li>Hỗ trợ kỹ thuật ưu tiên</li>
        </ul>
      </div>
      <div className="flex flex-col sm:flex-row gap-3 justify-center">
        <Button asChild>
          <Link href="/settings/billing">Nâng cấp lên ENTERPRISE</Link>
        </Button>
        <Button variant="outline" asChild>
          <Link href="/settings">Quay lại Settings</Link>
        </Button>
      </div>
    </div>
  );
}

export default function SettingsBrandingAdvancedPage() {
  const {
    tier,
    advancedModeEnabled,
    setAdvancedModeEnabled,
    canUseCustomPrompt,
  } = useBrandingTier();

  const [showDisclaimer, setShowDisclaimer] = useState(false);
  const [saved, setSaved] = useState(false);

  const isEnterprise = tier === 'ENTERPRISE';

  if (!isEnterprise) {
    return (
      <main className="container max-w-2xl py-8 px-4">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-1.5 text-xs text-muted-foreground mb-6" aria-label="Breadcrumb">
          <Link href="/settings" className="hover:text-foreground transition-colors">
            Settings
          </Link>
          <ChevronRight className="w-3 h-3" aria-hidden />
          <Link href="/settings" className="hover:text-foreground transition-colors">
            Branding
          </Link>
          <ChevronRight className="w-3 h-3" aria-hidden />
          <span className="text-foreground font-medium">Advanced</span>
        </nav>

        <UpgradePrompt />
      </main>
    );
  }

  function handleToggleClick() {
    if (!advancedModeEnabled) {
      // Turning ON — show disclaimer first
      setShowDisclaimer(true);
    } else {
      // Turning OFF — no disclaimer needed
      setAdvancedModeEnabled(false);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    }
  }

  function handleDisclaimerConfirm() {
    setAdvancedModeEnabled(true);
    setShowDisclaimer(false);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  }

  return (
    <main className="container max-w-2xl py-8 px-4 space-y-6">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-1.5 text-xs text-muted-foreground" aria-label="Breadcrumb">
        <Link href="/settings" className="hover:text-foreground transition-colors">
          Settings
        </Link>
        <ChevronRight className="w-3 h-3" aria-hidden />
        <Link href="/settings" className="hover:text-foreground transition-colors">
          Branding
        </Link>
        <ChevronRight className="w-3 h-3" aria-hidden />
        <span className="text-foreground font-medium">Advanced</span>
      </nav>

      {/* Page header */}
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-bold">Advanced Mode</h1>
          <p className="text-sm text-muted-foreground leading-relaxed">
            Mở khoá custom-prompt 200 ký tự cho ENTERPRISE. Mặc định:{' '}
            <strong>TẮT</strong> — chỉ wizard 6 bước được phép.
          </p>
        </div>
        <span className="inline-block px-2.5 py-1 rounded-full bg-amber-100 border border-amber-200 text-amber-800 text-xs font-bold shrink-0">
          {TIER_LABELS[tier]}
        </span>
      </div>

      {/* Main toggle card */}
      <div className="rounded-xl border bg-white shadow-sm divide-y">
        {/* Toggle row */}
        <div className="flex items-start justify-between gap-4 p-5">
          <div className="flex-1 space-y-1">
            <div className="flex items-center gap-2">
              <h3 className="font-bold text-base">Free-form prompt input</h3>
              <span className="px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 text-xs font-bold border border-amber-200">
                CHỈ ENTERPRISE
              </span>
            </div>
            <p className="text-sm text-muted-foreground">
              Khi bật, Step 6 của wizard hiển thị ô text 200 ký tự để nhập prompt tự do.
              AI sẽ dùng prompt này thay vì prompt tổng hợp từ 6 bước.
            </p>
          </div>

          <button
            type="button"
            role="switch"
            aria-checked={advancedModeEnabled}
            aria-label="Bật/tắt chế độ Advanced Mode"
            onClick={handleToggleClick}
            className={`shrink-0 transition-colors rounded-full ${
              advancedModeEnabled ? 'text-primary' : 'text-muted-foreground'
            }`}
          >
            {advancedModeEnabled ? (
              <ToggleRight className="w-10 h-10" aria-hidden />
            ) : (
              <ToggleLeft className="w-10 h-10" aria-hidden />
            )}
          </button>
        </div>

        {/* Status row */}
        <div className="px-5 py-3 bg-muted/30 flex items-center justify-between text-sm">
          <span className="text-muted-foreground">Trạng thái hiện tại:</span>
          <span
            className={`font-semibold ${
              advancedModeEnabled ? 'text-primary' : 'text-muted-foreground'
            }`}
          >
            {advancedModeEnabled ? '✅ Đang bật' : '⭕ Đang tắt'}
          </span>
        </div>

        {/* canUseCustomPrompt info */}
        {advancedModeEnabled && (
          <div className="px-5 py-3 flex items-center gap-2 text-sm text-emerald-800 bg-emerald-50">
            <Sparkles className="w-4 h-4 shrink-0 text-emerald-600" aria-hidden />
            <span>
              Custom prompt đang hoạt động.{' '}
              {canUseCustomPrompt
                ? 'Bạn có thể nhập prompt trong Bước 6 của wizard.'
                : 'Mở wizard để sử dụng.'}
            </span>
          </div>
        )}
      </div>

      {/* Behaviour notes */}
      <div className="rounded-xl border bg-slate-50 p-5 text-sm space-y-3">
        <h4 className="font-semibold text-foreground">Hành vi khi Advanced Mode BẬT:</h4>
        <ul className="space-y-2 text-muted-foreground">
          <li className="flex items-start gap-2">
            <span className="mt-0.5 w-1.5 h-1.5 rounded-full bg-muted-foreground shrink-0" aria-hidden />
            Bước 6 wizard hiển thị ô custom prompt 200 ký tự.
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 w-1.5 h-1.5 rounded-full bg-muted-foreground shrink-0" aria-hidden />
            Input vẫn qua kiểm tra token cap (16,000 tokens cho ENTERPRISE).
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 w-1.5 h-1.5 rounded-full bg-muted-foreground shrink-0" aria-hidden />
            Nếu AI thất bại quality gate (&lt;70/100), hệ thống tự fallback về template mặc định.
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 w-1.5 h-1.5 rounded-full bg-muted-foreground shrink-0" aria-hidden />
            Kết quả có thể không nhất quán — chỉ dành cho người dùng có kinh nghiệm.
          </li>
        </ul>
      </div>

      {/* Save feedback */}
      {saved && (
        <p
          role="status"
          aria-live="polite"
          className="text-sm text-emerald-700 font-medium"
        >
          ✓ Đã lưu thay đổi.
        </p>
      )}

      {/* Disclaimer modal */}
      <AdvancedModeDisclaimer
        open={showDisclaimer}
        onConfirm={handleDisclaimerConfirm}
        onCancel={() => setShowDisclaimer(false)}
      />
    </main>
  );
}
