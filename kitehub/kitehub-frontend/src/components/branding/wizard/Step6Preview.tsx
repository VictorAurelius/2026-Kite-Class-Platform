'use client';

// ---------------------------------------------------------------------------
// Step6Preview — Step 7 (final) preview + approve + deploy.
//
// Spec: `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step6-preview-default.html`
//
// What this component owns:
//   - Live iframe preview that points at the REAL KiteClass landing render path
//     (GAP-1215): `<iframe src>` → kiteclass `/preview` route (TemplateRenderer +
//     section components) themed by the selected palette variant + draft org /
//     logo / banner via query params. ONE render path → preview == deploy
//     source; new sections/themes auto-appear with no hand-sync. Replaces the
//     drift-prone `buildLandingPreviewHtml` srcDoc composer.
//   - Multi-variant pick (GAP-1212 kit v3): 3 palette variants derived from the
//     BE-resolved base; picking one re-themes the live preview + banner.
//   - Live banner preview (GAP-1143): `useBannerPreview` POSTs the FE palette to
//     the backend `preview-banner` endpoint → TEMPLATE-mode WebP (no FULL_AI
//     quota burned while exploring); the URL feeds the landing hero + the
//     `BannerLivePreview` tile.
//   - Generation-mode selector (GAP-1142): tier-gated TEMPLATE vs FULL_AI per
//     SUB-22 / ADR-037. Preview always uses TEMPLATE; FULL_AI commits on Deploy.
//   - Asset reuse (GAP-1143): pick a banner from previously-uploaded assets.
//   - 4 per-resource approve toggles + G11 ThemePreview + deploy CTA + SSE
//     deploy-stream (Wave 41 Bucket D / GAP-272o) + RegenerateCounter.
// ---------------------------------------------------------------------------

import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  Rocket,
  Smartphone,
  Tablet,
  Monitor,
  Info,
  Maximize2,
  Pencil,
} from 'lucide-react';
import { ThemePreview } from '@kite/shared-ui';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { ResourceToggle, type ApprovableResource } from './ResourceToggle';
import { TEMPLATES } from './TemplateGrid';
import { DeployingStep, type DeployingLogEntry } from './DeployingStep';
import { DoneStep } from './DoneStep';
import { RegenerateCounter } from './RegenerateCounter';
import {
  GenerationModeSelector,
  type GenerationMode,
} from './GenerationModeSelector';
import { AssetReusePicker } from './AssetReusePicker';
import { buildPaletteVariants } from './paletteVariants';
import { useLandingPreviewUrl } from './hooks/useLandingPreviewUrl';
import { toast } from 'sonner';
import {
  ORG_TYPE_OPTIONS,
  type Step6PreviewProps,
  type WizardStep,
} from './wizard-shared';
import { useAssets } from '@/hooks/use-branding';
import { useBrandingTier } from '@/hooks/use-branding-tier';
import type { PricingTier } from '@/types/subscription';
import {
  usePreviewBrandColors,
  useDeployStream,
  useRegenerateQuota,
  useCreateBrandingJobV1,
  useApproveBrandingJob,
  type DeployStreamEvent,
  type RegenerateQuotaResponse,
} from './hooks';
import {
  useBannerPreview,
  type PreviewBannerRequest,
} from './hooks/useBannerPreview';

// ---------------------------------------------------------------------------
// Brand-colour shape
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
// Decision summary (GAP-1136) — recap of the prior wizard steps + jump-to-edit.
// ---------------------------------------------------------------------------

const AUDIENCE_LABELS: Record<string, string> = {
  preschool: 'Trường mầm non',
  secondary: 'Trường THCS / THPT',
  'english-center': 'Trung tâm tiếng Anh',
  'exam-prep': 'Lớp luyện thi',
};

const TONE_LABELS: Record<string, string> = {
  professional: 'Chuyên nghiệp',
  friendly: 'Thân thiện',
  energetic: 'Năng động',
  luxury: 'Sang trọng',
};

interface DecisionRow {
  key: string;
  label: string;
  value: string;
  /** Wizard step the user jumps to when clicking "Sửa". */
  step: WizardStep;
}

interface DecisionSummaryProps {
  rows: ReadonlyArray<DecisionRow>;
  /** Jump to a prior step (orchestrator dispatches GO_TO_STEP). */
  onJump: (step: WizardStep) => void;
}

/** Side-panel summary of prior decisions with quick "edit → jump to step" (GAP-1136 §2). */
function DecisionSummary({ rows, onJump }: DecisionSummaryProps) {
  return (
    <div data-testid="step6-decision-summary" className="rounded-lg border bg-card p-3">
      <h3 className="mb-2 text-sm font-bold">Các bước đã chọn</h3>
      <ul className="space-y-1.5">
        {rows.map((row) => (
          <li
            key={row.key}
            data-testid={`step6-summary-${row.key}`}
            className="flex items-center justify-between gap-2 text-sm"
          >
            <span className="min-w-0">
              <span className="text-muted-foreground">{row.label}: </span>
              <span className="font-medium">{row.value}</span>
            </span>
            <button
              type="button"
              onClick={() => onJump(row.step)}
              data-testid={`step6-summary-edit-${row.key}`}
              className="inline-flex shrink-0 items-center gap-1 rounded px-1.5 py-0.5 text-xs text-primary hover:bg-primary/10"
            >
              <Pencil className="h-3 w-3" aria-hidden="true" />
              Sửa
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Step6Preview
// ---------------------------------------------------------------------------

export type Step6PreviewLocalProps = Omit<Step6PreviewProps, 'onBack' | 'onDeploy'> & {
  brandColors?: BrandColours;
  previewUrl?: string;
  /**
   * Instance id used to count PORTRAIT assets for the decision summary
   * (GAP-1136) + tier lookup + asset reuse. Distinct from
   * `wizardState.instanceId` (the deploy tenant claim, null until job creation).
   */
  assetInstanceId?: string;
  onBack?: () => void;
  onDeploy?: () => void;
};

export function Step6Preview({
  wizardState,
  dispatch,
  brandColors: brandColorsOverride,
  previewUrl: previewUrlOverride,
  assetInstanceId,
  onDeploy = () => {},
  onBack = () => {},
}: Step6PreviewLocalProps) {
  // GAP-1021: create a REAL BrandingJob on entering Step 6 so `wizardState.jobId`
  // becomes non-empty (enables deploy-stream).
  const { mutate: createJobMutate } = useCreateBrandingJobV1();
  const createStartedRef = useRef(false);

  // GAP-1142 / GAP-1216: generation mode now lives in WizardState (picked in
  // Step 1). Read from there + write via dispatch so the choice stays consistent
  // across the Welcome selector + this preview selector. GAP-1143: optional reused
  // banner from library.
  const generationMode: GenerationMode = wizardState.mode;
  const setGenerationMode = (m: GenerationMode) => dispatch({ type: 'SET_MODE', mode: m });
  const [reusedBannerUrl, setReusedBannerUrl] = useState<string | null>(null);

  useEffect(() => {
    if (wizardState.jobId || createStartedRef.current) return;
    createStartedRef.current = true;
    createJobMutate(
      {
        slug: wizardState.slug || undefined,
        organizationName: wizardState.tenantName || undefined,
        language: 'vi',
        audience: wizardState.audience,
        tone: wizardState.tone,
        templateId: wizardState.templateId,
        logoUrl: wizardState.logoUrl,
        aiLogo: wizardState.aiLogo,
        // GAP-1133 — org-type axis flows into the generate request.
        orgType: wizardState.orgType,
      },
      {
        onSuccess: (job) => {
          if (job?.jobId) {
            dispatch({
              type: 'SET_JOB_ID',
              jobId: String(job.jobId),
              instanceId:
                job.tenantId ??
                (job.instanceId != null ? String(job.instanceId) : undefined),
            });
          }
        },
        onError: () => {
          createStartedRef.current = false;
        },
      },
    );
  }, [
    wizardState.jobId,
    wizardState.slug,
    wizardState.tenantName,
    wizardState.audience,
    wizardState.tone,
    wizardState.templateId,
    wizardState.logoUrl,
    wizardState.aiLogo,
    wizardState.orgType,
    createJobMutate,
    dispatch,
  ]);

  // Wave 34 (GAP-272k): brand colours sourced from real backend job.
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

  const accentColor = jobBrandColors?.accent ?? brandColors.secondary;

  const selectedTemplate = useMemo(
    () => TEMPLATES.find((t) => t.id === wizardState.templateId) ?? null,
    [wizardState.templateId],
  );

  // GAP-1212: derive 3 palette variants from the BE-resolved base. Variant A =
  // base (deploy-faithful); B/C are preview-only alternatives. Picking one
  // re-themes the live preview iframe + regenerates the preview banner.
  const paletteVariants = useMemo(
    () =>
      buildPaletteVariants({
        primary: brandColors.primary,
        secondary: brandColors.secondary,
        accent: accentColor,
      }),
    [brandColors.primary, brandColors.secondary, accentColor],
  );
  const [selectedVariantId, setSelectedVariantId] = useState<string>('variant-a');
  const selectedVariant =
    paletteVariants.find((v) => v.id === selectedVariantId) ?? paletteVariants[0];

  // -------------------------------------------------------------------------
  // GAP-1143 — live banner preview (TEMPLATE, no quota). Triggered on mount +
  // when the palette / org / logo changes. The reused-asset URL (if picked)
  // overrides the generated banner.
  // -------------------------------------------------------------------------
  const bannerPreview = useBannerPreview();

  // Portraits uploaded in Step 3 (GAP-1134) feed the banner compose layer so the
  // featured teacher headshot appears in the generated banner (fixes "banner
  // thiếu ảnh chân dung").
  const { data: instanceAssets } = useAssets(assetInstanceId);
  const portraitUrls = useMemo(
    () => (instanceAssets ?? []).filter((a) => a.type === 'PORTRAIT').map((a) => a.url),
    [instanceAssets],
  );

  const previewBannerReq = useMemo<PreviewBannerRequest>(
    () => ({
      organizationName: wizardState.tenantName || 'Trung tâm giáo dục',
      copy: undefined,
      logoUrl: wizardState.logoUrl,
      portraitUrls,
      themeIcon: undefined,
      colours: {
        // GAP-1212: banner reflects the SELECTED palette variant.
        primary: selectedVariant.primary,
        secondary: selectedVariant.secondary,
        accent: selectedVariant.accent,
        neutral: brandColors.foreground,
        background: brandColors.background,
      },
    }),
    [wizardState.tenantName, wizardState.logoUrl, portraitUrls, selectedVariant, brandColors.foreground, brandColors.background],
  );

  const { generate: generateBannerPreview } = bannerPreview;
  useEffect(() => {
    // Always TEMPLATE for preview — never burns FULL_AI quota while exploring.
    generateBannerPreview(previewBannerReq).catch(() => {
      // Swallow — BannerLivePreview surfaces the error/fallback state.
    });
  }, [previewBannerReq, generateBannerPreview]);

  // Reused library asset wins over the freshly-composed preview.
  const effectiveBannerUrl = reusedBannerUrl ?? bannerPreview.bannerUrl;

  // GAP-1215: WYSIWYG preview = the REAL landing render path. The iframe points
  // at the kiteclass `/preview` route themed by the selected variant + draft
  // org/logo/banner — one render path, preview == deploy source. Sections/themes
  // that land on the real landing auto-appear here with no hand-sync.
  const landingPreviewSrc = useLandingPreviewUrl({
    primary: selectedVariant.primary,
    secondary: selectedVariant.secondary,
    accent: selectedVariant.accent,
    templateType: wizardState.templateId ?? undefined,
    orgName: wizardState.tenantName,
    logoUrl: wizardState.logoUrl,
    heroImage: effectiveBannerUrl,
    tenant:
      typeof wizardState.instanceId === 'string' ? wizardState.instanceId : undefined,
  });
  // Test hook: an explicit `previewUrl` prop overrides the composed landing URL.
  const previewSrc = previewUrlOverride ?? landingPreviewSrc;

  const approvedCount = wizardState.approvedResources.length;
  const totalResources = RESOURCES.length;
  const allApproved = approvedCount === totalResources;

  // -------------------------------------------------------------------------
  // GAP-1136 — full-screen preview mode + prior-decisions summary panel
  // -------------------------------------------------------------------------

  const [fullscreenOpen, setFullscreenOpen] = useState(false);

  const portraitCount = portraitUrls.length;

  // GAP-1142: tier drives FULL_AI eligibility in the mode selector.
  const { tier } = useBrandingTier(
    (typeof wizardState.instanceId === 'string' ? wizardState.instanceId : undefined) ??
      assetInstanceId,
  );

  const logoValue = wizardState.logoUrl
    ? 'Đã tải lên'
    : wizardState.aiLogo
      ? 'AI tự tạo'
      : 'Chưa chọn';

  const decisionRows = useMemo<ReadonlyArray<DecisionRow>>(
    () => [
      // GAP-1216 — jump-to-edit steps remapped to the output-first 5-step flow:
      // org-type → Welcome (1); audience/tone → Brand personality (2);
      // logo/portrait → Assets (3); template → Template (4).
      {
        key: 'org-type',
        label: 'Loại tổ chức',
        value: ORG_TYPE_OPTIONS.find((o) => o.id === wizardState.orgType)?.label ?? 'Chưa chọn',
        step: 1,
      },
      {
        key: 'audience',
        label: 'Đối tượng',
        value: wizardState.audience
          ? AUDIENCE_LABELS[wizardState.audience] ?? wizardState.audience
          : 'Chưa chọn',
        step: 2,
      },
      {
        key: 'tone',
        label: 'Phong cách',
        value: wizardState.tone ? TONE_LABELS[wizardState.tone] ?? wizardState.tone : 'Chưa chọn',
        step: 2,
      },
      { key: 'logo', label: 'Logo', value: logoValue, step: 3 },
      {
        key: 'portrait',
        label: 'Chân dung',
        value: portraitCount > 0 ? `${portraitCount} ảnh` : 'Chưa có',
        step: 3,
      },
      {
        key: 'template',
        label: 'Mẫu thiết kế',
        value: selectedTemplate?.name ?? 'Chưa chọn',
        step: 4,
      },
    ],
    [
      wizardState.orgType,
      logoValue,
      portraitCount,
      wizardState.audience,
      wizardState.tone,
      selectedTemplate,
    ],
  );

  const handleJumpToStep = (step: WizardStep) => {
    setFullscreenOpen(false);
    dispatch({ type: 'GO_TO_STEP', step });
  };

  // -------------------------------------------------------------------------
  // Wave 41 Bucket D (GAP-272o) — orchestrator wiring
  // -------------------------------------------------------------------------

  const [isDeploying, setIsDeploying] = useState(false);
  const [upsellModalOpen, setUpsellModalOpen] = useState(false);
  // GAP-1108 FE — terminal "done" screen + live landing URL (frontendUrl from
  // the approve 202 / `complete` SSE event). GAP-1216 — FAILED recovery state.
  const [deployDone, setDeployDone] = useState(false);
  const [deployFrontendUrl, setDeployFrontendUrl] = useState<string | null>(null);
  const [deployError, setDeployError] = useState<
    { message: string; code?: string; retryable: boolean } | null
  >(null);

  const { mutate: approveMutate } = useApproveBrandingJob();
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

  useEffect(() => {
    if (quotaExceeded && quotaTier !== 'ENTERPRISE' && !upsellModalOpen) {
      setUpsellModalOpen(true);
    }
  }, [quotaExceeded, quotaTier, upsellModalOpen]);

  // GAP-1108 FE — on a terminal `complete` SSE event, surface the DONE screen
  // (with the live landing URL) instead of silently routing away. GAP-1216 — on
  // a terminal `error` event, surface the FAILED panel (retry/back).
  const deployCompletedRef = useRef(false);
  useEffect(() => {
    if (!isDeploying || deployCompletedRef.current) return;
    const latest = deployStream.latestEvent;
    if (latest?.name === 'complete') {
      deployCompletedRef.current = true;
      const data = (latest.data ?? {}) as { frontendUrl?: string };
      if (data.frontendUrl) setDeployFrontendUrl(data.frontendUrl);
      toast.success('Triển khai thành công!');
      setDeployDone(true);
    } else if (latest?.name === 'error') {
      deployCompletedRef.current = true;
      const data = (latest.data ?? {}) as {
        message?: string;
        errorCode?: string;
        retryable?: boolean;
      };
      setDeployError({
        message:
          data.message ??
          'Quá trình triển khai gặp sự cố. Bạn có thể thử lại hoặc quay lại chỉnh sửa.',
        code: data.errorCode,
        retryable: data.retryable ?? true,
      });
    }
  }, [isDeploying, deployStream.latestEvent]);

  const startDeploy = () => {
    if (!wizardState.jobId) return;
    deployCompletedRef.current = false;
    setDeployError(null);
    setDeployDone(false);
    approveMutate(
      {
        jobId: wizardState.jobId,
        slug: wizardState.slug || undefined,
        templateId: wizardState.templateId,
        approvedResources: wizardState.approvedResources,
      },
      {
        onSuccess: (res) => {
          if (res?.frontendUrl) setDeployFrontendUrl(res.frontendUrl);
        },
        onError: (err) => {
          const e = err as {
            response?: { status?: number; data?: { errorCode?: string; qualityScore?: number } };
          };
          const status = e?.response?.status;
          const code = e?.response?.data?.errorCode;
          // GAP-1217 — server-side quality gate rejected the approve (422).
          // Keep the user on the preview so they can edit + retry; don't dead-end.
          if (status === 422 || code === 'QUALITY_GATE_FAILED') {
            const score = e?.response?.data?.qualityScore;
            toast.error(
              `Chưa đạt chuẩn chất lượng${
                score != null ? ` (điểm ${score}/100)` : ''
              } — hãy điều chỉnh rồi triển khai lại.`,
            );
            setIsDeploying(false);
            deployCompletedRef.current = true;
            return;
          }
          // Other approve failures → FAILED panel (retryable).
          setDeployError({
            message: 'Không gửi được yêu cầu triển khai. Vui lòng thử lại.',
            code,
            retryable: true,
          });
          deployCompletedRef.current = true;
        },
      },
    );
    setIsDeploying(true);
  };

  const handleDeployClick = () => {
    if (!allApproved || !wizardState.jobId) return;
    startDeploy();
  };

  const handleRetryDeploy = () => {
    startDeploy();
  };

  const handleBackFromDeploy = () => {
    setIsDeploying(false);
    setDeployError(null);
    deployCompletedRef.current = false;
  };

  const handleRegenerateClick = () => {
    if (!wizardState.jobId) return;
    if (quotaExceeded && quotaTier !== 'ENTERPRISE') {
      setUpsellModalOpen(true);
      return;
    }
    // GAP-1145: regenerate REQUIRES X-Instance-Id (the job's tenant/instance claim).
    // The gateway maps tenantId→X-Tenant-Id but never sets X-Instance-Id, so the FE
    // must pass it. wizardState.instanceId is the deploy tenant claim set on job
    // creation. Without it the server returns 400 MISSING_INSTANCE_ID.
    const instanceId =
      typeof wizardState.instanceId === 'string' ? wizardState.instanceId : undefined;
    if (!instanceId) {
      toast.info(
        'Bản xem trước trực tiếp đã tự cập nhật khi bạn đổi lựa chọn — "Tạo lại" khả dụng sau khi triển khai.',
      );
      return;
    }
    regenerate.mutate(
      { jobId: wizardState.jobId, instanceId },
      {
        // Mid-wizard the job is a mock (QUEUED/INITIALIZING, never DEPLOYED) so the
        // server returns 409 INVALID_JOB_STATE. Surface a friendly note instead of a
        // raw error — the live banner preview already re-generates on every input change.
        onError: () =>
          toast.info(
            'Bản xem trước trực tiếp tự cập nhật khi bạn đổi lựa chọn. "Tạo lại" áp dụng sau khi triển khai.',
          ),
      },
    );
  };

  const handleUpgradeClick = () => {
    // TODO(GAP-272r): route to /billing/upgrade when subscription page lands.
    setUpsellModalOpen(false);
  };

  // GAP-1147: on-demand FULL_AI banner. The backend tier-gates + meters the
  // PREMIUM/ENTERPRISE quota and downgrades to TEMPLATE when ineligible/exhausted,
  // so the FE just reflects the resolved mode + fallbackReason in a toast.
  const fullAiEligible = tier === 'PREMIUM' || tier === 'ENTERPRISE';
  const handleGenerateFullAi = () => {
    generateBannerPreview({ ...previewBannerReq, mode: 'FULL_AI' }, tier)
      .then((res) => {
        if (res.mode === 'FULL_AI') {
          toast.success('Đã tạo banner bằng AI cao cấp — đã trừ 1 lượt.');
        } else if (res.fallbackReason === 'NOT_AVAILABLE') {
          // GAP-1218: image-gen chưa wire (GAP-1135) — server KHÔNG trừ lượt,
          // toast phải nói thật thay vì claim "AI cao cấp".
          toast.info(
            'AI vẽ banner đang hoàn thiện — dùng bản Mẫu chất lượng cao, KHÔNG trừ lượt của bạn.',
          );
        } else if (res.fallbackReason === 'QUOTA_EXHAUSTED') {
          toast.info(
            'Đã hết lượt AI cao cấp tháng này — đang dùng bản Mẫu. Nâng cấp để có thêm lượt.',
          );
        } else {
          toast.info('Gói hiện tại chưa hỗ trợ AI cao cấp — đang dùng bản Mẫu.');
        }
      })
      .catch(() => toast.error('Không tạo được banner AI cao cấp, vui lòng thử lại.'));
  };

  // -------------------------------------------------------------------------
  // Deploying sub-state render branch
  // -------------------------------------------------------------------------

  // GAP-1108 FE — terminal success screen with the live landing link.
  if (deployDone) {
    return (
      <DoneStep
        tenantName={wizardState.tenantName}
        frontendUrl={deployFrontendUrl}
        slug={wizardState.slug}
        onManage={onDeploy}
      />
    );
  }

  if (isDeploying) {
    return (
      <DeployingStep
        logs={deployLogs}
        instanceId={
          typeof wizardState.instanceId === 'string'
            ? wizardState.instanceId
            : undefined
        }
        errorMessage={deployError?.message}
        errorCode={deployError?.code}
        errorRetryable={deployError?.retryable}
        onRetry={handleRetryDeploy}
        onBack={handleBackFromDeploy}
      />
    );
  }

  return (
    <div className="space-y-6" data-testid="step6-preview">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-primary uppercase tracking-wide mb-1">
            Bước 5 / 5 — Xem & Tạo
          </p>
          <h1 className="text-2xl font-bold text-foreground mb-2">
            Xem trước trang web của bạn
          </h1>
          <p className="text-muted-foreground">
            Đây là trang chủ thật của trung tâm bạn. Bật/tắt từng tài nguyên và
            chọn loại AI để tạo banner.
          </p>
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setFullscreenOpen(true)}
          data-testid="step6-fullscreen-open"
          className="shrink-0"
        >
          <Maximize2 className="mr-2 h-4 w-4" aria-hidden="true" />
          Toàn màn hình
        </Button>
      </div>

      <div className="grid lg:grid-cols-[1.4fr_1fr] gap-6">
        {/* Left column — iframe live preview (wave-landing-100 standard) */}
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
                <button type="button" className="p-1 rounded hover:bg-slate-200" title="Mobile">
                  <Smartphone className="w-3.5 h-3.5" aria-hidden="true" />
                </button>
                <button type="button" className="p-1 rounded hover:bg-slate-200" title="Tablet">
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

            <iframe
              src={previewSrc}
              title="Xem trước trang web"
              data-testid="step6-preview-iframe"
              className="w-full border-0 bg-white"
              style={{ aspectRatio: '16 / 10', minHeight: 320 }}
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
            />
          </div>

          <p className="text-xs text-muted-foreground flex items-center gap-2">
            <Info className="w-3.5 h-3.5" aria-hidden="true" />
            Đây là trang chủ THẬT (render path KiteClass) — đúng bản sẽ lên sóng khi triển khai.
          </p>

          {/* G11 ThemePreview — wired with LIVE brand colours from wizard state. */}
          <div data-testid="step6-theme-preview">
            <ThemePreview brandColors={brandColors ?? FALLBACK_BRAND} initialMode="light" />
          </div>
        </div>

        {/* Right column — mode selector + banner + summary + approve + regen */}
        <div className="space-y-4">
          {/* GAP-1142 — generation mode (tier-gated) */}
          <div data-testid="step6-generation-mode" className="rounded-lg border bg-card p-3">
            <h3 className="mb-2 text-sm font-bold">Loại tạo banner</h3>
            <GenerationModeSelector
              tier={tier}
              value={generationMode}
              onChange={setGenerationMode}
              onUpgradeClick={handleUpgradeClick}
            />
            {/* GAP-1147: explicit FULL_AI generate action — only for eligible tiers
                when FULL_AI is selected. Each click consumes one PREMIUM quota slot. */}
            {generationMode === 'FULL_AI' && fullAiEligible && (
              <Button
                type="button"
                size="sm"
                className="mt-3 w-full"
                disabled={bannerPreview.isLoading}
                onClick={handleGenerateFullAi}
                data-testid="generate-full-ai-banner"
              >
                {bannerPreview.isLoading
                  ? 'Đang tạo banner AI…'
                  : 'Tạo bằng AI cao cấp (tốn 1 lượt)'}
              </Button>
            )}
          </div>

          {/* GAP-1212 — multi-variant pick (kit v3 "AI tạo biến thể — chọn 1").
              Picking a variant re-themes the live preview + banner instantly.
              Variant A = the deploy-faithful base palette. */}
          <div data-testid="step6-variant-picker" className="rounded-lg border bg-card p-3 space-y-2">
            <h3 className="text-sm font-bold">Biến thể bảng màu — chọn 1</h3>
            <p className="text-xs text-muted-foreground">
              Chọn 1 biến thể; bản xem trước bên trái đổi theo ngay.
            </p>
            <div className="grid grid-cols-3 gap-2">
              {paletteVariants.map((v) => {
                const isSelected = v.id === selectedVariantId;
                return (
                  <button
                    key={v.id}
                    type="button"
                    onClick={() => setSelectedVariantId(v.id)}
                    aria-pressed={isSelected}
                    data-testid={`step6-${v.id}`}
                    data-selected={isSelected ? 'true' : 'false'}
                    className={`overflow-hidden rounded-md border text-left transition ${
                      isSelected
                        ? 'border-primary ring-2 ring-primary/30'
                        : 'border-border hover:border-primary/60'
                    }`}
                  >
                    <span
                      className="block h-9"
                      style={{ background: `linear-gradient(120deg, ${v.primary}, ${v.accent})` }}
                      aria-hidden="true"
                    />
                    <span className="block px-2 py-1 text-[11px] font-medium">{v.label}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* GAP-1143 — reuse banner from library. The banner itself is the hero
              of the landing preview on the left (not duplicated here). */}
          <div data-testid="step6-banner-block" className="rounded-lg border bg-card p-3 space-y-2">
            <h3 className="text-sm font-bold">Banner</h3>
            <p className="text-xs text-muted-foreground">
              Banner hiển thị ở đầu trang xem trước bên trái. Chọn để dùng lại từ thư viện:
            </p>
            <AssetReusePicker
              instanceId={assetInstanceId}
              type="BANNER"
              selectedUrl={reusedBannerUrl}
              onSelect={setReusedBannerUrl}
            />
          </div>

          {/* GAP-1136 — prior-decisions summary with jump-to-edit links. */}
          <DecisionSummary rows={decisionRows} onJump={handleJumpToStep} />

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

          {/* Wave 41 Bucket D (GAP-272o): RegenerateCounter wired to quota hook. */}
          <div data-testid="step6-regenerate-counter-wired">
            <RegenerateCounter
              tier={quotaTier}
              regenerateQuota={quotaLimit}
              regeneratesUsed={quotaUsed}
              // GAP-1219(a): "Tạo lại" chỉ có nghĩa sau khi job đầu tiên tồn tại
              // (jobId + instanceId set khi create job ở bước này thành công).
              hasGenerated={Boolean(wizardState.jobId && wizardState.instanceId)}
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

      {/* GAP-1136 — full-screen live preview + side panel (summary + approve). */}
      <Dialog
        open={fullscreenOpen}
        onOpenChange={(next) => {
          if (!next) setFullscreenOpen(false);
        }}
      >
        <DialogContent
          className="max-w-6xl w-[96vw] p-4 md:p-6"
          data-testid="step6-fullscreen-dialog"
        >
          <DialogTitle className="sr-only">Xem trước toàn màn hình</DialogTitle>
          <div className="grid gap-4 lg:grid-cols-[1.5fr_1fr]">
            <div className="space-y-2">
              <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
                <div className="flex items-center gap-3 border-b bg-slate-50 px-3 py-2">
                  <div className="flex gap-1" aria-hidden="true">
                    <span className="h-2.5 w-2.5 rounded-full bg-rose-400" />
                    <span className="h-2.5 w-2.5 rounded-full bg-amber-400" />
                    <span className="h-2.5 w-2.5 rounded-full bg-emerald-400" />
                  </div>
                  <span className="flex-1 truncate font-mono text-xs text-muted-foreground">
                    https://{wizardState.slug || 'tenant-slug'}.kiteclass.vn
                  </span>
                </div>
                <iframe
                  src={previewSrc}
                  title="Xem trước toàn màn hình"
                  data-testid="step6-fullscreen-iframe"
                  className="w-full border-0 bg-white"
                  style={{ height: '70vh', minHeight: 360 }}
                  sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
                />
              </div>
            </div>

            <div className="max-h-[78vh] space-y-4 overflow-y-auto">
              <DecisionSummary rows={decisionRows} onJump={handleJumpToStep} />

              <div>
                <h3 className="mb-2 text-sm font-bold">
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

              <Button
                type="button"
                className="w-full"
                onClick={() => {
                  setFullscreenOpen(false);
                  handleDeployClick();
                }}
                disabled={!allApproved}
                data-testid="step6-fullscreen-deploy"
              >
                <Rocket className="mr-2 h-4 w-4" aria-hidden="true" />
                Triển khai trang web
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
