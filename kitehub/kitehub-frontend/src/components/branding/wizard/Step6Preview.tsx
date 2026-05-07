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
// Wave 41 Bucket D (GAP-272o) — orchestrator wiring of `useDeployStream`
// + `useRegenerateQuota` into `DeployingStep` + `RegenerateCounter`. When
// the user clicks the Deploy CTA this component switches into a deploying
// sub-state and renders `<DeployingStep>` driven by the real SSE stream.
// On `complete` the parent's `onDeploy()` callback fires (router push).
//
// What this component does NOT own (per rework plan §3 Bucket C boundaries):
//   - `<QualityGateWidget>` — Bucket D ships and slots in via state-driven
//     render branches (this file renders a SCAFFOLD placeholder marked with
//     TODO(GAP-272o) so the layout remains correct).
// ---------------------------------------------------------------------------

import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Rocket, Smartphone, Tablet, Monitor, Info } from 'lucide-react';
import { ThemePreview } from '@kite/shared-ui';
import { Button } from '@/components/ui/button';
import { ResourceToggle, type ApprovableResource } from './ResourceToggle';
import { TEMPLATES } from './TemplateGrid';
import { DeployingStep, type DeployingLogEntry } from './DeployingStep';
import { RegenerateCounter } from './RegenerateCounter';
import type { Step6PreviewProps } from './wizard-shared';
import type { PricingTier } from '@/types/subscription';
import {
  usePreview,
  usePreviewBrandColors,
  useDeployStream,
  useRegenerateQuota,
  type DeployStreamEvent,
  type RegenerateQuotaResponse,
} from './hooks';

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

// ---------------------------------------------------------------------------
// Wave 41 Bucket D (GAP-272o) — orchestrator wiring helpers.
// ---------------------------------------------------------------------------

function mapHookTier(tier: RegenerateQuotaResponse['tier'] | undefined): PricingTier {
  if (!tier) return 'FREE';
  // Wave 34 contract still emits 'PRO' for legacy callers; map to BASIC
  // (the `RegenerateCounter` PricingTier vocabulary).
  if (tier === 'PRO') return 'BASIC';
  return tier;
}

function eventsToLogEntries(events: readonly DeployStreamEvent[]): DeployingLogEntry[] {
  const out: DeployingLogEntry[] = [];
  for (const ev of events) {
    if (ev.name === 'heartbeat') continue;
    const data = (ev.data ?? {}) as {
      message?: string;
      timestamp?: string;
      ts?: string;
      level?: DeployingLogEntry['level'];
      percent?: number;
      toState?: string;
      errorCode?: string;
    };
    const timestamp = data.timestamp ?? data.ts ?? new Date().toISOString();
    let message = '';
    let level: DeployingLogEntry['level'] = 'info';
    switch (ev.name) {
      case 'log':
        message = data.message ?? '';
        level = data.level ?? 'info';
        break;
      case 'progress':
        message = `Tiến trình ${data.percent ?? 0}%`;
        level = 'pending';
        break;
      case 'state-change':
        message = `Trạng thái: ${data.toState ?? '?'}`;
        level = 'info';
        break;
      case 'complete':
        message = data.message ?? 'Triển khai hoàn tất';
        level = 'success';
        break;
      case 'error':
        message = data.message ?? `Lỗi triển khai (${data.errorCode ?? 'UNKNOWN'})`;
        level = 'error';
        break;
      default:
        continue;
    }
    if (message) out.push({ timestamp, message, level });
  }
  return out;
}

// Wave 34 (GAP-272k): brand colours sourced from
// `BrandingJobResponse.brandColors` via `usePreviewBrandColors` hook.
// The legacy per-template hint map was removed — the v1
// `/api/v1/branding/jobs/{jobId}` endpoint is the single source of truth.

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

/**
 * Test/Storybook-friendly extension of Bucket A's canonical `Step6PreviewProps`.
 * Adds optional `brandColors` + `previewUrl` overrides used by tests and Bucket
 * D's QualityGate harness when wiring deterministic snapshots.
 *
 * `onBack` + `onDeploy` are mandatory in A's contract — this component keeps
 * them optional locally by `Partial`-ing them so the test file can render
 * without supplying them while production callers still satisfy the
 * orchestrator contract via destructure defaults below.
 */
export type Step6PreviewLocalProps = Omit<Step6PreviewProps, 'onBack' | 'onDeploy'> & {
  brandColors?: BrandColours;
  previewUrl?: string;
  onBack?: () => void;
  onDeploy?: () => void;
};

export function Step6Preview({
  wizardState,
  dispatch,
  brandColors: brandColorsOverride,
  previewUrl: previewUrlOverride,
  onDeploy = () => {},
  onBack = () => {},
}: Step6PreviewLocalProps) {
  // Wave 34 (GAP-272k): brand colours sourced from real backend job via
  // `usePreviewBrandColors`. Falls back to FALLBACK_BRAND while the v1
  // job is still loading or returns no colors.
  const { brandColors: jobBrandColors } = usePreviewBrandColors(
    wizardState.jobId ?? undefined,
  );

  const brandColors = useMemo<BrandColours>(() => {
    if (brandColorsOverride) return brandColorsOverride;
    if (jobBrandColors) {
      return {
        primary: jobBrandColors.primary,
        secondary: jobBrandColors.secondary,
        background: jobBrandColors.background ?? FALLBACK_BRAND.background,
        foreground: jobBrandColors.neutral ?? FALLBACK_BRAND.foreground,
      };
    }
    return FALLBACK_BRAND;
  }, [brandColorsOverride, jobBrandColors]);

  const selectedTemplate = useMemo(
    () => TEMPLATES.find((t) => t.id === wizardState.templateId) ?? null,
    [wizardState.templateId],
  );

  // Wave 34 (GAP-272j): real preview iframe URL sourced from
  // `usePreview(jobId)` → backend HTML render endpoint. Tests can pass
  // `previewUrl` override; absence yields undefined while jobId is null
  // so the iframe renders nothing rather than a stale data: URI.
  const hookedPreviewUrl = usePreview(wizardState.jobId ?? undefined);
  const previewUrl = previewUrlOverride ?? hookedPreviewUrl ?? '';

  const approvedCount = wizardState.approvedResources.length;
  const totalResources = RESOURCES.length;
  const allApproved = approvedCount === totalResources;

  // -------------------------------------------------------------------------
  // Wave 41 Bucket D (GAP-272o) — orchestrator wiring
  // -------------------------------------------------------------------------

  const [isDeploying, setIsDeploying] = useState(false);
  const [upsellModalOpen, setUpsellModalOpen] = useState(false);

  const { quota, regenerate } = useRegenerateQuota();
  const quotaTier = mapHookTier(quota.data?.tier);
  const quotaLimit = quota.data?.limit ?? 3;
  const quotaUsed = quota.data?.used ?? 0;
  const quotaExceeded =
    quota.data !== undefined &&
    quota.data.limit !== -1 &&
    quota.data.used >= quota.data.limit;

  const deployStream = useDeployStream(wizardState.jobId ?? undefined, {
    enabled: isDeploying && Boolean(wizardState.jobId),
  });

  const deployLogs = useMemo<DeployingLogEntry[]>(
    () => eventsToLogEntries(deployStream.events),
    [deployStream.events],
  );

  // Auto-trigger upsell modal when quota exhausted on non-ENTERPRISE tier
  // (per `ai-branding-guidelines.md` §4.3 + AC item 4).
  useEffect(() => {
    if (quotaExceeded && quotaTier !== 'ENTERPRISE' && !upsellModalOpen) {
      setUpsellModalOpen(true);
    }
  }, [quotaExceeded, quotaTier, upsellModalOpen]);

  // Forward SSE `complete` to parent — propagates wizard exit.
  useEffect(() => {
    if (!isDeploying) return;
    const latest = deployStream.latestEvent;
    if (latest?.name === 'complete') {
      onDeploy();
    }
  }, [isDeploying, deployStream.latestEvent, onDeploy]);

  const handleDeployClick = () => {
    if (!allApproved) return;
    setIsDeploying(true);
  };

  const handleRegenerateClick = () => {
    if (!wizardState.jobId) return;
    if (quotaExceeded && quotaTier !== 'ENTERPRISE') {
      setUpsellModalOpen(true);
      return;
    }
    regenerate.mutate({ jobId: wizardState.jobId });
  };

  const handleUpgradeClick = () => {
    // TODO(GAP-272r): route to /billing/upgrade when subscription page lands.
    setUpsellModalOpen(false);
  };

  // -------------------------------------------------------------------------
  // Deploying sub-state render branch
  // -------------------------------------------------------------------------

  if (isDeploying) {
    return (
      <DeployingStep
        logs={deployLogs}
        instanceId={
          typeof wizardState.jobId === 'string' ? wizardState.jobId : undefined
        }
      />
    );
  }

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

          {/* Wave 41 Bucket D (GAP-272o): real RegenerateCounter wired to
              `useRegenerateQuota` hook + tier mapping. */}
          <div data-testid="step6-regenerate-counter-wired">
            <RegenerateCounter
              tier={quotaTier}
              regenerateQuota={quotaLimit}
              regeneratesUsed={quotaUsed}
              upsellModalOpen={upsellModalOpen}
              onRegenerate={handleRegenerateClick}
              onUpgradeClick={handleUpgradeClick}
              onContinueWithCurrent={() => setUpsellModalOpen(false)}
              onUpsellModalOpenChange={setUpsellModalOpen}
            />
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
          onClick={handleDeployClick}
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
