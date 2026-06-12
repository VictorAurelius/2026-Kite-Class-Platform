'use client';

/**
 * DoneStep — terminal "Hoàn tất" screen for the AI Branding Wizard (GAP-1108 FE).
 *
 * Shown after the deploy SSE stream emits `complete`. Replaces the previous
 * silent `router.push('/branding')` dead-end with an explicit success surface
 * per the kit `done.html` design source:
 *   - success confirmation + summary recap of what shipped
 *   - PRIMARY CTA "Mở trang web của bạn" → the live tenant landing URL
 *     (`frontendUrl` carried by the `branding.deployed` event / approve 202
 *     response — Đợt 1 Bucket C). Opens in a new tab so the wizard tab stays.
 *   - SECONDARY action → back to the Branding management page.
 *
 * Spec source: `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/v3/done.html`.
 */

import { CheckCircle2, ExternalLink, LayoutDashboard, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

export interface DoneStepProps {
  /** Center name (for the summary recap). */
  tenantName: string;
  /** Live tenant landing URL — the post-deploy `frontendUrl`. May be empty if
   *  the backend did not carry it; the CTA falls back to a computed slug URL. */
  frontendUrl?: string | null;
  /** Slug fallback used to compute the landing URL when `frontendUrl` is empty. */
  slug?: string;
  /** Navigate back to /branding management. */
  onManage: () => void;
}

/** Compose a public landing URL from the slug when the backend URL is absent. */
function fallbackLandingUrl(slug?: string): string | null {
  if (!slug) return null;
  // G1 walk 2026-06-12: hardcode https://{slug}.kitehub.me là deadlink trên local
  // (GAP-803 class). Template env-driven — local default trỏ KC :3000 ?tenant=.
  const template =
    process.env.NEXT_PUBLIC_TENANT_LANDING_URL_TEMPLATE ??
    'http://localhost:3000/?tenant={slug}';
  return template.replace('{slug}', slug);
}

export function DoneStep({ tenantName, frontendUrl, slug, onManage }: DoneStepProps) {
  const landingUrl = (frontendUrl && frontendUrl.trim().length > 0)
    ? frontendUrl
    : fallbackLandingUrl(slug);

  return (
    <div className="max-w-2xl mx-auto space-y-6" data-testid="done-step">
      <Card className="p-8 text-center space-y-4">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400">
          <CheckCircle2 className="h-9 w-9" aria-hidden="true" />
        </div>
        <div>
          <p className="text-sm font-semibold uppercase tracking-wide text-emerald-600 flex items-center justify-center gap-2">
            <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
            Triển khai thành công
          </p>
          <h1 className="mt-1 text-2xl font-bold text-foreground">
            Trang web của bạn đã sẵn sàng!
          </h1>
          <p className="mt-2 text-muted-foreground">
            Bộ nhận diện thương hiệu của <strong>{tenantName || 'trung tâm'}</strong> đã
            được áp dụng lên trang chủ thật. Mở liên kết bên dưới để xem trang đã lên sóng.
          </p>
        </div>

        {landingUrl ? (
          <div className="space-y-3">
            <Button
              asChild
              size="lg"
              className="w-full sm:w-auto"
              data-testid="done-step-open-landing"
            >
              <a href={landingUrl} target="_blank" rel="noopener noreferrer">
                <ExternalLink className="mr-2 h-4 w-4" aria-hidden="true" />
                Mở trang web của bạn
              </a>
            </Button>
            <p className="font-mono text-xs text-muted-foreground break-all">
              {landingUrl}
            </p>
          </div>
        ) : (
          <p
            className="text-sm text-muted-foreground"
            data-testid="done-step-no-url"
          >
            Trang web đang được kích hoạt — liên kết sẽ xuất hiện trong trang Thương hiệu
            trong giây lát.
          </p>
        )}
      </Card>

      <div className="flex justify-center">
        <Button
          variant="outline"
          onClick={onManage}
          data-testid="done-step-manage"
        >
          <LayoutDashboard className="mr-2 h-4 w-4" aria-hidden="true" />
          Về trang quản lý Thương hiệu
        </Button>
      </div>
    </div>
  );
}
