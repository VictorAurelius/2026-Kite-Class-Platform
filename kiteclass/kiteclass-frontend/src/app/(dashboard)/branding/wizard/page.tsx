// shell-exempt: full-screen multi-step branding wizard, focused flow by design (no dashboard chrome)
'use client';

import { Suspense } from 'react';
import { BrandingWizard } from '@/components/branding/wizard/BrandingWizard';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { useAuthStore } from '@/stores/auth-store';
import { useTenantFromUrl } from '@/hooks/useTenantFromUrl';
import type { Tier } from '@/components/branding/wizard/types';

/**
 * Branding wizard page at {@code /branding/wizard}.
 *
 * Tier + tenant metadata are resolved from the authenticated session (auth-store
 * tenantId from JWT claim) + tenant slug (query param / subdomain / localStorage
 * via {@code useTenantFromUrl}). Previously these were hard-coded
 * ({@code tenantId="current-tenant" slug="my-school"}), which rendered the wizard
 * against a non-existent tenant and produced a blank page for seeded owners
 * (GAP-726).
 *
 * The inner component reads {@code useSearchParams} (via {@code useTenantFromUrl}),
 * so it is wrapped in {@code <Suspense>} to satisfy the Next.js production-build
 * prerender boundary requirement (per fe-build-local-verify rule).
 *
 * @since Wave 3 Sub-PR 3.7 (GAP-013 + GAP-031 + GAP-069)
 * @since GAP-726 — read tenantId/slug from session instead of hard-coded scaffold
 */
export default function BrandingWizardPage() {
  return (
    <Suspense fallback={<WizardLoading />}>
      <BrandingWizardResolved />
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

function BrandingWizardResolved() {
  const tenantId = useAuthStore((state) => state.tenantId);
  const tenantSlug = useTenantFromUrl();

  // tier follows the tenant subscription; default PRO scaffold until the
  // subscription tier is surfaced on the session (tracked separately).
  const tier: Tier = 'PRO';

  // Resolve slug from URL (query param / subdomain) with a localStorage fallback.
  // tenantSubdomain is written by useTenantFromUrl. (The tenantId UUID — stored
  // tenant-scoped per GAP-1074 — is not a slug, so it is not a slug fallback.)
  const slug =
    tenantSlug ||
    (typeof window !== 'undefined'
      ? localStorage.getItem('tenantSubdomain')
      : null);

  // tenantId comes from the JWT claim stored at login. If the session has not
  // hydrated yet (or the user is not authenticated), show a graceful message
  // instead of a blank page — the dashboard layout already guards /login redirect.
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

  return <BrandingWizard tier={tier} tenantId={tenantId} slug={slug ?? tenantId} />;
}
