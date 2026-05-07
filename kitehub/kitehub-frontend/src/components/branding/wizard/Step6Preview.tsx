'use client';

// ---------------------------------------------------------------------------
// Step6Preview — Step 6 (HEADLINE 122/128) preview-default state.
//
// Spec: `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step6-preview-default.html`
//
// What this component owns:
//   - Live iframe preview of the generated tenant site (mock URL until
//     backend `/branding/jobs/:id/preview` lands — TODO(GAP-272j)).
//   - 4 per-resource approve toggles (logo / colors / banner / hero) wired
//     to the WizardState reducer per `ai-branding-guidelines.md` §4.2.
//   - G11 ThemePreview integration with LIVE brand colours from the wizard
//     state (NOT MOCK_BRAND — v1 audit-caught violation).
//   - Footer summary (X/4 resources approved · quality score) + deploy CTA.
//
// What this component does NOT own (per rework plan §3 Bucket C boundaries):
//   - `<QualityGateWidget>` — Bucket D ships and slots in via state-driven
//     render branches (this file renders a SCAFFOLD placeholder marked with
//     TODO(GAP-272o) so the layout remains correct).
//   - `<RegenerateCounter>` — Bucket D, same scaffold pattern.
//   - QGate-fail / regenerate / deploying sub-states — Bucket D adds them
//     via the WizardState reducer's currentStep + jobId combos; THIS step
//     ships the preview-default state only.
// ---------------------------------------------------------------------------

import { useMemo } from 'react';
import { ArrowLeft, Rocket, Smartphone, Tablet, Monitor, Info } from 'lucide-react';
import { ThemePreview } from '@kite/shared-ui';
import { Button } from '@/components/ui/button';
import { ResourceToggle } from './ResourceToggle';
import { TEMPLATES } from './TemplateGrid';
import type { Step6PreviewProps, ApprovableResource } from './wizard-shared';

// ---------------------------------------------------------------------------
// Brand-colour derivation
// ---------------------------------------------------------------------------

interface BrandColours {
  primary: string;
  secondary: string;
  background: string;
  foreground: string;
}

const FALLBACK_BRAND: BrandColours = {
  primary: '#1E40AF',
  secondary: '#F59E0B',
  background: '#FFFFFF',
  foreground: '#0F172A',
};

// Per-template colour hints — derived from the SVG palettes in TemplateGrid.
// These ship as a TRANSITIONAL bridge until the backend generate endpoint
// returns real `theme.json` colours.
// TODO(GAP-272k): replace with real brand colours from the generate endpoint
// (the backend should return `theme.json` or its colour subset on
// /branding/jobs/:id when status === DEPLOYED or REGENERATING).
const TEMPLATE_COLOURS: Record<string, BrandColours> = {
  'template-t1-navy-focus': {
    primary: '#0F1E3D',
    secondary: '#F59E0B',
    background: '#FFFFFF',
    foreground: '#0F172A',
  },
  'template-t2-score-board': {
    primary: '#1E40AF',
    secondary: '#F59E0B',
    background: '#FFFFFF',
    foreground: '#0F172A',
  },
  'template-t3-coach-card': {
    primary: '#1E40AF',
    secondary: '#F59E0B',
    background: '#F8FAFC',
    foreground: '#0F172A',
  },
  'template-t4-result-stripes': {
    primary: '#0F1E3D',
    secondary: '#F59E0B',
    background: '#FFFFFF',
    foreground: '#0F172A',
  },
  'template-t5-schedule-grid': {
    primary: '#1E40AF',
    secondary: '#10B981',
    background: '#FFFFFF',
    foreground: '#0F172A',
  },
  'template-t6-roadmap-vertical': {
    primary: '#0F1E3D',
    secondary: '#F59E0B',
    background: '#0F1E3D',
    foreground: '#FFFFFF',
  },
};

// ---------------------------------------------------------------------------
// Mock preview URL (TODO replace with backend render endpoint)
// ---------------------------------------------------------------------------

/**
 * Build a `data:` URI showing a tiny mock tenant page so the iframe has
 * something visible until the real preview endpoint lands.
 *
 * TODO(GAP-272j): swap to `${endpoints.branding.jobAssets(jobId)}/preview`
 * (or equivalent backend render route) once the backend ships an HTML
 * preview endpoint we can iframe.
 */
function buildMockPreviewUrl(brand: BrandColours, tenantName: string) {
  const safeName = tenantName.replace(/</g, '&lt;') || 'Toán Master';
  const html = `<!doctype html>
<html lang="vi"><head><meta charset="utf-8"/><title>${safeName}</title>
<style>
  body { margin: 0; font-family: ui-sans-serif, system-ui, sans-serif; color: ${brand.foreground}; background: ${brand.background}; }
  .nav { padding: 12px 20px; background: ${brand.primary}; color: white; display: flex; justify-content: space-between; align-items: center; }
  .nav .cta { background: ${brand.secondary}; color: ${brand.primary}; padding: 6px 12px; border-radius: 4px; font-weight: 700; font-size: 12px; }
  .hero { padding: 24px 20px; }
  .hero h1 { font-size: 22px; margin: 0 0 8px; }
  .hero p { font-size: 13px; color: #475569; margin: 0; }
  .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 0 20px 20px; }
  .stat { background: #F1F5F9; padding: 12px; border-radius: 6px; text-align: center; }
  .stat .num { font-size: 22px; font-weight: 800; color: ${brand.primary}; }
  .stat .lbl { font-size: 11px; color: #475569; }
</style></head>
<body>
  <div class="nav"><strong>${safeName}</strong><span class="cta">Đăng ký test</span></div>
  <div class="hero"><h1>Vào trường chuyên cùng ${safeName}</h1><p>98% học viên đã đỗ năm 2025 · 240 thí sinh.</p></div>
  <div class="stats">
    <div class="stat"><div class="num">98%</div><div class="lbl">đỗ chuyên</div></div>
    <div class="stat"><div class="num">9.2</div><div class="lbl">điểm TB</div></div>
    <div class="stat"><div class="num">240</div><div class="lbl">học viên</div></div>
  </div>
</body></html>`;
  return `data:text/html;charset=utf-8,${encodeURIComponent(html)}`;
}

// ---------------------------------------------------------------------------
// Resource catalogue rendered in the approve stack
// ---------------------------------------------------------------------------

interface ResourceItem {
  id: ApprovableResource;
  title: string;
  description: (ctx: { templateCode: string | null; templateName: string | null; primary: string; secondary: string }) => string;
}

const RESOURCES: ReadonlyArray<ResourceItem> = [
  {
    id: 'logo',
    title: 'Logo',
    description: () => 'SVG monogram · ~12 KB',
  },
  {
    id: 'colors',
    title: 'Bảng màu',
    description: ({ primary, secondary }) =>
      `Chính ${primary} · Phụ ${secondary}`,
  },
  {
    id: 'banner',
    title: 'Banner',
    description: ({ templateCode, templateName }) =>
      templateName
        ? `"Vào trường chuyên" · template ${templateCode ?? '?'} ${templateName}`
        : '"Vào trường chuyên" · template chưa chọn',
  },
  {
    id: 'hero',
    title: 'Hero section',
    description: () => 'Score board 3 cột · responsive',
  },
] as const;

// ---------------------------------------------------------------------------
// Step6Preview
// ---------------------------------------------------------------------------

export function Step6Preview({
  wizardState,
  dispatch,
  brandColors: brandColorsOverride,
  previewUrl: previewUrlOverride,
  onDeploy,
  onBack,
}: Step6PreviewProps) {
  // Live brand colours derived from the selected template (or override for
  // tests / Storybook). NOT a hardcoded MOCK_BRAND constant — see
  // TODO(GAP-272k) above.
  const brandColors = useMemo(() => {
    if (brandColorsOverride) return brandColorsOverride;
    if (
      wizardState.templateId &&
      TEMPLATE_COLOURS[wizardState.templateId]
    ) {
      return TEMPLATE_COLOURS[wizardState.templateId];
    }
    return FALLBACK_BRAND;
  }, [brandColorsOverride, wizardState.templateId]);

  const selectedTemplate = useMemo(
    () => TEMPLATES.find((t) => t.id === wizardState.templateId) ?? null,
    [wizardState.templateId],
  );

  const previewUrl = useMemo(() => {
    if (previewUrlOverride) return previewUrlOverride;
    return buildMockPreviewUrl(
      brandColors ?? FALLBACK_BRAND,
      wizardState.tenantName,
    );
  }, [brandColors, previewUrlOverride, wizardState.tenantName]);

  const approvedCount = wizardState.approvedResources.length;
  const totalResources = RESOURCES.length;
  const allApproved = approvedCount === totalResources;

  return (
    <div className="space-y-6" data-testid="step6-preview">
      <div>
        <p className="text-sm font-semibold text-primary uppercase tracking-wide mb-1">
          Bước 6 / 6 — Cuối cùng!
        </p>
        <h1 className="text-2xl font-bold text-foreground mb-2">
          Xem trước trang web của bạn
        </h1>
        <p className="text-muted-foreground">
          Bật/tắt từng tài nguyên (logo, màu, banner, hero) để chọn cái bạn muốn
          deploy. AI đã chấm điểm chất lượng ở phải.
        </p>
      </div>

      <div className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        {/* Left column — iframe live preview */}
        <div className="space-y-3">
          <div className="rounded-lg border bg-white shadow-sm overflow-hidden">
            <div
              className="flex items-center gap-3 px-3 py-2 border-b bg-slate-50"
              data-testid="step6-preview-frame-head"
            >
              <div className="flex gap-1" aria-hidden="true">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-400" />
                <span className="w-2.5 h-2.5 rounded-full bg-amber-400" />
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400" />
              </div>
              <span className="font-mono text-xs text-muted-foreground flex-1 truncate">
                https://{wizardState.slug || 'tenant-slug'}.kiteclass.vn
              </span>
              <div className="flex gap-1" aria-label="Kích thước xem trước">
                <button
                  type="button"
                  className="p-1 rounded hover:bg-slate-200"
                  title="Mobile"
                >
                  <Smartphone className="w-3.5 h-3.5" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  className="p-1 rounded hover:bg-slate-200"
                  title="Tablet"
                >
                  <Tablet className="w-3.5 h-3.5" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  className="p-1 rounded bg-slate-200"
                  title="Desktop"
                  aria-pressed="true"
                >
                  <Monitor className="w-3.5 h-3.5" aria-hidden="true" />
                </button>
              </div>
            </div>

            {/* Iframe live preview — mock URL until GAP-272j wires real endpoint. */}
            <iframe
              src={previewUrl}
              title="Xem trước trang web"
              data-testid="step6-preview-iframe"
              className="w-full border-0 bg-white"
              style={{ aspectRatio: '16 / 10', minHeight: 320 }}
              sandbox="allow-same-origin"
            />
          </div>

          <p className="text-xs text-muted-foreground flex items-center gap-2">
            <Info className="w-3.5 h-3.5" aria-hidden="true" />
            Xem trước này render từ theme + assets đã chọn. Sẵn sàng deploy.
          </p>

          {/* G11 ThemePreview — wired with LIVE brand colours from wizard state. */}
          <div data-testid="step6-theme-preview">
            <ThemePreview
              brandColors={brandColors ?? FALLBACK_BRAND}
              initialMode="light"
            />
          </div>
        </div>

        {/* Right column — approve stack + qgate scaffold + regen scaffold */}
        <div className="space-y-4">
          {/* Quality gate scaffold — Bucket D fills in via state branch. */}
          <div
            className="rounded-lg border bg-emerald-50/40 p-3"
            data-testid="step6-quality-gate-scaffold"
          >
            <p className="text-xs uppercase tracking-wider text-muted-foreground font-semibold">
              Điểm chất lượng
            </p>
            <p className="text-sm text-muted-foreground mt-1">
              {/* TODO(GAP-272o): Bucket D's QualityGateWidget renders here. */}
              Đang chấm điểm...
            </p>
          </div>

          {/* Per-resource approve stack */}
          <div>
            <h3 className="font-bold text-sm mb-2">
              Phê duyệt từng tài nguyên ({approvedCount}/{totalResources})
            </h3>
            <div className="space-y-2">
              {RESOURCES.map((r) => {
                const approved = wizardState.approvedResources.includes(r.id);
                return (
                  <ResourceToggle
                    key={r.id}
                    resource={r.id}
                    title={r.title}
                    description={r.description({
                      templateCode: selectedTemplate?.code ?? null,
                      templateName: selectedTemplate?.name ?? null,
                      primary: brandColors?.primary ?? FALLBACK_BRAND.primary,
                      secondary: brandColors?.secondary ?? FALLBACK_BRAND.secondary,
                    })}
                    approved={approved}
                    dispatch={dispatch}
                  />
                );
              })}
            </div>
          </div>

          {/* Regenerate counter scaffold — Bucket D fills in. */}
          <div
            className="rounded-lg border bg-slate-50 p-3"
            data-testid="step6-regenerate-counter-scaffold"
          >
            <p className="text-xs text-muted-foreground">
              {/* TODO(GAP-272p): Bucket D's RegenerateCounter renders here. */}
              Tạo lại — quota theo tier (FREE 3 / PRO 10 / PREMIUM 30 / ENT ∞).
            </p>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between max-w-5xl mx-auto px-1">
        <Button variant="ghost" onClick={onBack} type="button">
          <ArrowLeft className="mr-2 w-4 h-4" aria-hidden="true" />
          Sửa các bước
        </Button>
        <p className="text-xs text-muted-foreground">
          {approvedCount}/{totalResources} tài nguyên đã phê duyệt
        </p>
        <Button
          type="button"
          onClick={onDeploy}
          disabled={!allApproved}
          data-testid="step6-deploy-button"
        >
          <Rocket className="mr-2 w-4 h-4" aria-hidden="true" />
          Triển khai trang web
        </Button>
      </div>
    </div>
  );
}
