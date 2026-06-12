// shell-exempt: focused redirect surface to the canonical KiteHub branding wizard
'use client';

import { Suspense, useEffect } from 'react';
import { ExternalLink } from 'lucide-react';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { useAuthStore } from '@/stores/auth-store';

/**
 * Branding wizard route at {@code /branding/wizard} (KiteClass `:3000`).
 *
 * GAP-1214 — UNIFY: the AI Branding generation wizard is a KiteHub platform
 * capability (KH-6). KiteClass previously shipped a SECOND, divergent XState
 * wizard ({@code BrandingWizard} + {@code wizard-machine.ts}) whose preview
 * rendered {@code about:blank} — a maintenance-drift orphan vs the canonical
 * KiteHub 7-step wizard. This route now REDIRECTS to the canonical KiteHub
 * wizard ({@code :3001 (customer)/branding/wizard}) instead of rendering the
 * orphan FSM. The orphan component is retired from the live flow (marked
 * deprecated; kept only for its standalone tests until removed in a follow-up).
 *
 * Per `.claude/rules/kitehub-kiteclass-boundary.md` §2: KiteHub FE = `:3001`,
 * resolved from {@code NEXT_PUBLIC_KITEHUB_URL} (local default `:3001`).
 *
 * @since GAP-1214 (Wave branding-100 Đợt 3) — supersedes GAP-726 route scaffold.
 */
export default function BrandingWizardPage() {
  return (
    <Suspense fallback={<WizardLoading />}>
      <BrandingWizardRedirect />
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

function BrandingWizardRedirect() {
  const tenantId = useAuthStore((state) => state.tenantId);
  const target = kitehubWizardUrl();

  // Auto-redirect once authenticated; the manual link is the no-JS / fallback path.
  useEffect(() => {
    if (!tenantId) return;
    if (typeof window !== 'undefined') {
      window.location.assign(target);
    }
  }, [tenantId, target]);

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
      <div className="rounded-xl border border-muted bg-muted/30 p-10 text-center">
        <p className="font-medium">Đang chuyển tới Trình hướng dẫn AI Branding…</p>
        <p className="mt-1 text-sm text-muted-foreground">
          Trình tạo thương hiệu nay nằm trên KiteHub. Nếu trình duyệt không tự chuyển,
          hãy nhấn nút bên dưới.
        </p>
        <a
          href={target}
          data-testid="kc-wizard-redirect-link"
          className="mt-4 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90"
        >
          <ExternalLink className="h-4 w-4" aria-hidden="true" />
          Mở Trình hướng dẫn AI Branding
        </a>
      </div>
    </div>
  );
}
