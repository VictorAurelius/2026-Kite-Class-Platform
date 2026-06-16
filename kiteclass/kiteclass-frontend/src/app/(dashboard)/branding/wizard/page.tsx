// shell-exempt: focused hand-off surface to the canonical KiteHub branding wizard
'use client';

import { Suspense } from 'react';
import { ExternalLink, Info } from 'lucide-react';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { useAuthStore } from '@/stores/auth-store';

/**
 * Branding wizard route at {@code /branding/wizard} (KiteClass `:3000`).
 *
 * GAP-1214 — UNIFY: the AI Branding generation wizard is a KiteHub platform
 * capability (KH-6). KiteClass previously shipped a SECOND, divergent XState
 * wizard ({@code BrandingWizard} + {@code wizard-machine.ts}) whose preview
 * rendered {@code about:blank} — a maintenance-drift orphan vs the canonical
 * KiteHub 7-step wizard. This route now hands off to the canonical KiteHub
 * wizard ({@code :3001 (customer)/branding/wizard}) instead of rendering the
 * orphan FSM.
 *
 * GAP-1447 — NO AUTO-BOUNCE: cross-product SSO between KiteClass (`:3000`) and
 * KiteHub (`:3001`) does NOT exist yet, so a silent {@code window.location.assign}
 * lands the owner on the KiteHub login form with no KiteClass session — a
 * dead-end. Instead we render an explicit hand-off card: the owner clicks
 * intentionally and the link opens in a NEW TAB ({@code target="_blank"}) so the
 * KiteClass session is preserved. The card states up front that a separate
 * KiteHub login may be required. Full shared-session SSO is deferred (tracked in
 * GAP-1447) — when it lands, this card can resume auto-redirect.
 *
 * Per `.claude/rules/kitehub-kiteclass-boundary.md` §2: KiteHub FE = `:3001`,
 * resolved from {@code NEXT_PUBLIC_KITEHUB_URL} (local default `:3001`).
 *
 * @since GAP-1214 (Wave branding-100 Đợt 3) — supersedes GAP-726 route scaffold.
 */
export default function BrandingWizardPage() {
  return (
    <Suspense fallback={<WizardLoading />}>
      <BrandingWizardHandoff />
    </Suspense>
  );
}

function WizardLoading() {
  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <LoadingSpinner size="lg" />
    </div>
  );
}

/** Canonical KiteHub AI Branding wizard URL. */
function kitehubWizardUrl(): string {
  const base = process.env.NEXT_PUBLIC_KITEHUB_URL || 'http://localhost:3001';
  return `${base.replace(/\/$/, '')}/branding/wizard`;
}

function BrandingWizardHandoff() {
  const tenantId = useAuthStore((state) => state.tenantId);
  const target = kitehubWizardUrl();

  if (!tenantId) {
    return (
      <div className="mx-auto max-w-3xl p-6">
        <div className="rounded-xl border border-muted bg-muted/30 p-10 text-center">
          <p className="font-medium">Đang tải thông tin trung tâm…</p>
          <p className="mt-1 text-sm text-muted-foreground">
            Nếu màn hình này không tự cập nhật, vui lòng đăng nhập lại để tiếp tục
            trợ lý cài đặt thương hiệu.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl p-6" data-testid="kc-wizard-redirect">
      <div className="rounded-xl border border-muted bg-muted/30 p-8">
        <h1 className="text-lg font-semibold">Trình hướng dẫn AI Branding</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Trình tạo thương hiệu bằng AI nay nằm trên nền tảng KiteHub. Nhấn nút bên dưới
          để mở trong tab mới.
        </p>

        {/* GAP-1447: nêu rõ cần đăng nhập KiteHub riêng — tránh dead-end "bị đá ra login" */}
        <div className="mt-4 flex items-start gap-2 rounded-md border border-amber-300/60 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-500/40 dark:bg-amber-950/30 dark:text-amber-200">
          <Info className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <span>
            Hiện chưa có đăng nhập dùng chung giữa KiteClass và KiteHub. Bạn có thể cần
            đăng nhập lại bằng tài khoản KiteHub ở tab mới. Phiên làm việc KiteClass
            hiện tại vẫn được giữ nguyên.
          </span>
        </div>

        <a
          href={target}
          target="_blank"
          rel="noopener noreferrer"
          data-testid="kc-wizard-redirect-link"
          className="mt-5 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90"
        >
          <ExternalLink className="h-4 w-4" aria-hidden="true" />
          Mở Trình hướng dẫn AI Branding trên KiteHub
        </a>
      </div>
    </div>
  );
}
