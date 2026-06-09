'use client';

// ---------------------------------------------------------------------------
// Step6Preview — Step 6 (HEADLINE 122/128) preview-default state.
//
// Spec: `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/screens/step6-preview-default.html`
//
// What this component owns:
//   - Live iframe preview of the generated tenant site, rendered CLIENT-SIDE
//     via `<iframe srcDoc>` from brand colours + org name + uploaded logo
//     (see `buildPreviewHtml`). Replaces the previous `<iframe src={previewUrl}>`
//     which produced a blank frame (backend `/preview` not wired + iframe
//     cannot attach the auth header) — AC2 fix / TODO(GAP-272j)/(GAP-1021).
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
import { RegenerateCounter } from './RegenerateCounter';
import { toast } from 'sonner';
import {
  ORG_TYPE_OPTIONS,
  type Step6PreviewProps,
  type WizardStep,
} from './wizard-shared';
import { useAssets } from '@/hooks/use-branding';
import type { PricingTier } from '@/types/subscription';
import {
  usePreview,
  usePreviewBrandColors,
  useDeployStream,
  useRegenerateQuota,
  useCreateBrandingJobV1,
  useApproveBrandingJob,
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
// Client-side preview HTML composer (AC2 "AI branding preview được" fix).
//
// Why `srcDoc` instead of `<iframe src={previewUrl}>`:
//   The backend `/api/v1/branding/jobs/{id}/preview` endpoint is NOT wired yet
//   (TODO GAP-272j + GAP-1021 Part 1). Even once it lands, an `<iframe src>`
//   issues the request WITHOUT the `Authorization: Bearer` header (apiClient's
//   interceptor only covers fetch/XHR, not iframe navigations) → gateway
//   returns 401/404 → blank iframe (matches the "Blocked script execution in
//   about:blank ... sandboxed" console symptom).
//
//   So we render the preview CLIENT-SIDE from data the FE already holds with
//   authenticated access:
//     - brand colours  → usePreviewBrandColors (GET /api/v1/branding/jobs/{id})
//     - org name + slug → WizardState (Step 1)
//     - logo           → WizardState.logoUrl (uploaded Step 2 — the real asset)
//   The document is fully static (no <script>) so `sandbox="allow-same-origin"`
//   is sufficient. `usePreview` is kept (not removed) as a documented fallback
//   for when the backend HTML render endpoint eventually ships.
//
//   Note: we deliberately do NOT fetch GET /jobs/{id}/assets here — that
//   endpoint exists only at the legacy `/api/platform/branding/jobs/{id}/assets`
//   path keyed on the legacy job space, NOT the v1 wizard jobId, so it would
//   404. The uploaded logo (WizardState.logoUrl) already covers the asset need.
// ---------------------------------------------------------------------------

const HEX_RE = /^#[0-9A-Fa-f]{3,8}$/;

/** Return `value` only if it is a safe hex colour, else `fallback` (CSS-injection guard). */
function safeColor(value: string | undefined, fallback: string): string {
  return value && HEX_RE.test(value) ? value : fallback;
}

/** Minimal HTML-text escaper for user-supplied strings injected into srcDoc. */
function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** 1-2 letter monogram from an org name (logo fallback when no logoUrl). */
function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const letters = parts.slice(0, 2).map((p) => p[0] ?? '');
  return letters.join('').toUpperCase() || 'KC';
}

interface BuildPreviewHtmlOptions {
  brand: BrandColours;
  accent: string;
  orgName: string;
  slug: string;
  logoUrl: string | null;
  templateId: string | null;
  templateName: string | null;
  loading: boolean;
}

// ---------------------------------------------------------------------------
// Per-template body composer (GAP-272 §3a fix — "preview phải theo template").
//
// The previous version rendered ONE hard-coded hero+features layout for every
// template, so the only thing that changed when the user picked a different
// template was a text label in the palette section. The chosen template was
// effectively invisible in the preview. Each of the 6 wizard templates
// (TemplateGrid.TEMPLATES) is a distinct landing-page archetype (see the spec
// SVGs in `ui_kits/ai-branding-wizard-v2/screens/step5-template-grid.html`):
//   T1 Navy Focus     → centered exam-focus hero + single CTA
//   T2 Score Board    → data-first heading + horizontal score bars
//   T3 Coach Card     → teacher card (left) + benefit list (right)
//   T4 Result Stripes → 3 big-number stat columns
//   T5 Schedule Grid  → "Lịch khai giảng" rows + seats-left badges
//   T6 Roadmap        → vertical 12-month milestone timeline
// Each body is themed via the document's --primary/--secondary/--accent CSS
// vars (so brand colours flow through) — picking a different template now
// produces a visibly different preview. `null` template → generic fallback.
// ---------------------------------------------------------------------------

interface TemplateBodyCtx {
  /** Already HTML-escaped org name. */
  safeOrg: string;
  /** Pre-rendered <img>/<div> logo block (escaped). */
  logoBlock: string;
}

function renderTemplateBody(
  templateId: string | null,
  { safeOrg, logoBlock }: TemplateBodyCtx,
): string {
  switch (templateId) {
    case 'template-t1-navy-focus':
      return `
  <section class="hero t-centered">
    <div class="hero-logo">${logoBlock}</div>
    <h1>${safeOrg}</h1>
    <p class="tagline">98% học viên đạt điểm 9+ · luyện thi vào trường chuyên</p>
    <span class="cta">Đăng ký test miễn phí</span>
  </section>`;

    case 'template-t2-score-board':
      return `
  <section class="t-board">
    <div class="t-board-head"><span class="brand-name">${safeOrg}</span><span class="t-pill">Tham gia</span></div>
    <h2 class="t-h2">Bảng điểm 2025</h2>
    <div class="t-bars">
      <div class="t-bar"><span style="width:75%;background:var(--primary)"></span></div>
      <div class="t-bar"><span style="width:92%;background:var(--accent)"></span></div>
      <div class="t-bar"><span style="width:58%;background:var(--secondary)"></span></div>
    </div>
    <p class="t-link">Xem chi tiết →</p>
  </section>`;

    case 'template-t3-coach-card':
      return `
  <section class="t-coach">
    <div class="t-coach-card">
      <div class="t-avatar">${logoBlock}</div>
      <p class="t-coach-name">Th.S Nguyễn An</p>
      <p class="t-coach-sub">15 năm luyện thi</p>
      <span class="cta">Đặt buổi tư vấn</span>
    </div>
    <div class="t-coach-list">
      <div class="t-coach-item">Lộ trình cá nhân hóa</div>
      <div class="t-coach-item">Lớp ≤ 12 học viên</div>
      <div class="t-coach-item">Cam kết đầu ra</div>
    </div>
  </section>`;

    case 'template-t4-result-stripes':
      return `
  <section class="t-stats">
    <div class="t-stats-banner">98% đỗ chuyên năm 2025</div>
    <div class="t-stats-grid">
      <div class="t-stat" style="background:var(--primary)"><b>240</b><span>học viên đỗ</span></div>
      <div class="t-stat" style="background:var(--secondary)"><b>9.2</b><span>điểm trung bình</span></div>
      <div class="t-stat" style="background:var(--accent)"><b>15</b><span>năm kinh nghiệm</span></div>
    </div>
  </section>`;

    case 'template-t5-schedule-grid':
      return `
  <section class="t-schedule">
    <h2 class="t-h2">Lịch khai giảng</h2>
    <div class="t-sched-row"><span>15/08 · Lớp 12 · Khai giảng</span><span class="t-badge t-badge-ok">Còn 8 chỗ</span></div>
    <div class="t-sched-row"><span>22/08 · Lớp 9 · Vào chuyên</span><span class="t-badge t-badge-warn">Còn 3 chỗ</span></div>
    <div class="t-sched-row"><span>29/08 · Lớp 11 · Cơ bản</span><span class="t-badge t-badge-full">Hết chỗ</span></div>
  </section>`;

    case 'template-t6-roadmap-vertical':
      return `
  <section class="t-roadmap">
    <h2 class="t-h2 t-h2-light">Lộ trình 12 tháng</h2>
    <div class="t-timeline">
      <div class="t-milestone"><span class="t-dot"></span>Tháng 1-3 · Cơ bản</div>
      <div class="t-milestone"><span class="t-dot"></span>Tháng 4-6 · Nâng cao</div>
      <div class="t-milestone"><span class="t-dot"></span>Tháng 7-9 · Đề thi thật</div>
      <div class="t-milestone"><span class="t-dot"></span>Tháng 10-12 · Tổng ôn</div>
    </div>
  </section>`;

    default:
      // No template selected yet → generic hero + 3 features (legacy default).
      return `
  <section class="hero">
    <div class="hero-logo">${logoBlock}</div>
    <h1>${safeOrg}</h1>
    <p class="tagline">Nền tảng học tập trực tuyến hiện đại — quản lý lớp học, theo dõi tiến độ và kết nối phụ huynh.</p>
    <span class="cta">Đăng ký học thử</span>
  </section>
  <section class="features">
    <div class="feature"><div class="dot"></div><h3>Lớp học linh hoạt</h3><p>Lịch học, điểm danh và bài tập trong một nơi.</p></div>
    <div class="feature"><div class="dot"></div><h3>Theo dõi tiến độ</h3><p>Bảng điểm và báo cáo học tập theo thời gian thực.</p></div>
    <div class="feature"><div class="dot"></div><h3>Kết nối phụ huynh</h3><p>Thông báo và trao đổi với giáo viên dễ dàng.</p></div>
  </section>`;
  }
}

/**
 * Compose a standalone, script-free HTML landing preview reflecting the
 * generated branding (brand colours + org name + logo) AND the chosen template
 * layout (see `renderTemplateBody`). Rendered via `<iframe srcDoc>` so it needs
 * no authenticated network request.
 */
function buildPreviewHtml({
  brand,
  accent,
  orgName,
  slug,
  logoUrl,
  templateId,
  templateName,
  loading,
}: BuildPreviewHtmlOptions): string {
  const primary = safeColor(brand.primary, FALLBACK_BRAND.primary);
  const secondary = safeColor(brand.secondary, FALLBACK_BRAND.secondary);
  const bg = safeColor(brand.background, FALLBACK_BRAND.background);
  const fg = safeColor(brand.foreground, FALLBACK_BRAND.foreground);
  const acc = safeColor(accent, secondary);

  const safeOrg = escapeHtml(orgName.trim() || 'Trường của bạn');
  const safeSlug = escapeHtml(slug.trim() || 'tenant-slug');
  const safeTemplate = templateName ? escapeHtml(templateName) : '';
  const safeTemplateId = escapeHtml(templateId || 'default');

  const logoBlock = logoUrl
    ? `<img class="logo-img" src="${escapeHtml(logoUrl)}" alt="Logo ${safeOrg}" />`
    : `<div class="logo-monogram">${escapeHtml(initialsOf(orgName))}</div>`;

  const loadingNote = loading
    ? '<p class="loading-note-block">Đang tạo bản xem trước…</p>'
    : '';

  const templateBody = renderTemplateBody(templateId, { safeOrg, logoBlock });

  const swatches = (
    [
      ['Chính', primary],
      ['Phụ', secondary],
      ['Nhấn', acc],
      ['Nền', bg],
      ['Chữ', fg],
    ] as ReadonlyArray<readonly [string, string]>
  )
    .map(
      ([label, hex]) => `
        <div class="swatch">
          <span class="chip" style="background:${hex}"></span>
          <span class="chip-label">${label}</span>
          <span class="chip-hex">${hex}</span>
        </div>`,
    )
    .join('');

  return `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<style>
  :root { --primary:${primary}; --secondary:${secondary}; --accent:${acc}; --bg:${bg}; --fg:${fg}; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: system-ui, -apple-system, 'Segoe UI', sans-serif; color: var(--fg); background: var(--bg); }
  .nav { display:flex; align-items:center; gap:10px; padding:14px 24px; border-bottom:1px solid rgba(0,0,0,.08); }
  .nav .brand-name { font-weight:700; font-size:15px; }
  .nav .spacer { flex:1; }
  .nav .nav-link { font-size:13px; opacity:.7; margin-left:16px; }
  .logo-img { height:32px; width:auto; border-radius:6px; object-fit:contain; }
  .logo-monogram { height:32px; width:32px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:14px; color:#fff; background:linear-gradient(135deg,var(--primary),var(--secondary)); }
  .hero { padding:56px 24px; text-align:center; color:#fff; background:linear-gradient(135deg,var(--primary),var(--secondary)); }
  .hero .hero-logo { display:flex; justify-content:center; margin-bottom:18px; }
  .hero .hero-logo .logo-img, .hero .hero-logo .logo-monogram { height:56px; }
  .hero .hero-logo .logo-monogram { width:56px; font-size:22px; background:rgba(255,255,255,.2); }
  .hero h1 { font-size:30px; line-height:1.2; margin-bottom:10px; }
  .hero .tagline { font-size:15px; opacity:.92; max-width:520px; margin:0 auto 22px; }
  .hero .cta { display:inline-block; padding:11px 26px; border-radius:9999px; background:var(--accent); color:#fff; font-weight:700; font-size:14px; }
  .features { display:flex; gap:14px; flex-wrap:wrap; justify-content:center; padding:34px 24px; }
  .feature { flex:1 1 160px; max-width:220px; padding:18px; border-radius:12px; background:rgba(0,0,0,.03); text-align:center; }
  .feature .dot { width:34px; height:34px; border-radius:10px; margin:0 auto 10px; background:var(--primary); opacity:.85; }
  .feature h3 { font-size:14px; margin-bottom:6px; }
  .feature p { font-size:12px; opacity:.7; }
  /* per-template bodies (GAP-272 §3a) */
  .t-h2 { font-size:18px; font-weight:800; margin-bottom:14px; }
  .t-h2-light { color:#fff; text-align:center; }
  .t-board { padding:24px; }
  .t-board-head { display:flex; align-items:center; justify-content:space-between; padding:10px 14px; border-radius:8px; background:var(--primary); color:#fff; margin-bottom:16px; }
  .t-board-head .brand-name { color:#fff; font-weight:700; }
  .t-pill { background:#fff; color:var(--primary); font-size:11px; font-weight:700; padding:3px 12px; border-radius:9999px; }
  .t-bars { display:flex; flex-direction:column; gap:12px; background:rgba(0,0,0,.03); padding:18px; border-radius:10px; }
  .t-bar { height:10px; border-radius:5px; background:rgba(0,0,0,.06); overflow:hidden; }
  .t-bar span { display:block; height:100%; border-radius:5px; }
  .t-link { margin-top:14px; color:var(--primary); font-weight:700; font-size:13px; }
  .t-coach { display:flex; gap:16px; padding:24px; }
  .t-coach-card { flex:0 0 42%; background:var(--primary); color:#fff; border-radius:12px; padding:20px; text-align:center; }
  .t-coach-card .cta { display:inline-block; margin-top:6px; padding:9px 18px; border-radius:9999px; background:var(--accent); color:#fff; font-weight:700; font-size:13px; }
  .t-avatar { width:56px; height:56px; border-radius:50%; margin:0 auto 12px; background:rgba(255,255,255,.2); display:flex; align-items:center; justify-content:center; overflow:hidden; }
  .t-avatar .logo-img, .t-avatar .logo-monogram { height:56px; width:56px; }
  .t-coach-name { font-weight:800; font-size:15px; }
  .t-coach-sub { font-size:12px; opacity:.85; margin-bottom:14px; }
  .t-coach-list { flex:1; display:flex; flex-direction:column; gap:10px; justify-content:center; }
  .t-coach-item { border:1px solid rgba(0,0,0,.1); border-radius:8px; padding:14px; font-size:13px; font-weight:600; }
  .t-stats { padding:24px; }
  .t-stats-banner { background:var(--primary); color:#fff; text-align:center; font-weight:800; font-size:16px; padding:14px; border-radius:10px; margin-bottom:16px; }
  .t-stats-grid { display:flex; gap:12px; }
  .t-stat { flex:1; border-radius:12px; padding:20px 10px; text-align:center; color:#fff; }
  .t-stat b { display:block; font-size:28px; font-weight:800; }
  .t-stat span { font-size:11px; opacity:.9; }
  .t-schedule { padding:24px; }
  .t-sched-row { display:flex; align-items:center; justify-content:space-between; padding:14px; border-radius:8px; background:rgba(0,0,0,.03); margin-bottom:10px; font-size:13px; font-weight:600; }
  .t-badge { font-size:11px; font-weight:700; color:#fff; padding:4px 12px; border-radius:9999px; white-space:nowrap; }
  .t-badge-ok { background:#10B981; }
  .t-badge-warn { background:var(--accent); }
  .t-badge-full { background:#EF4444; }
  .t-roadmap { background:linear-gradient(135deg,var(--primary),var(--secondary)); padding:28px 24px; }
  .t-timeline { display:flex; flex-direction:column; gap:6px; max-width:420px; margin:0 auto; }
  .t-milestone { display:flex; align-items:center; gap:12px; color:#fff; font-size:13px; font-weight:600; padding:8px 0; }
  .t-dot { width:14px; height:14px; border-radius:50%; background:var(--accent); flex:0 0 14px; }
  .loading-note-block { text-align:center; font-size:12px; opacity:.7; padding:10px; }
  .palette { padding:28px 24px; border-top:1px solid rgba(0,0,0,.06); }
  .palette h2 { font-size:13px; text-transform:uppercase; letter-spacing:.05em; opacity:.6; margin-bottom:14px; text-align:center; }
  .swatches { display:flex; gap:14px; flex-wrap:wrap; justify-content:center; }
  .swatch { display:flex; flex-direction:column; align-items:center; gap:4px; }
  .chip { width:48px; height:48px; border-radius:10px; border:1px solid rgba(0,0,0,.08); }
  .chip-label { font-size:11px; font-weight:600; }
  .chip-hex { font-size:10px; opacity:.6; font-family:ui-monospace,monospace; }
  footer { padding:18px 24px; text-align:center; font-size:11px; opacity:.5; border-top:1px solid rgba(0,0,0,.06); }
</style>
</head>
<body>
  <nav class="nav">
    ${logoBlock}
    <span class="brand-name">${safeOrg}</span>
    <span class="spacer"></span>
    <span class="nav-link">Khóa học</span>
    <span class="nav-link">Lịch học</span>
    <span class="nav-link">Liên hệ</span>
  </nav>
  <div class="t-body" data-preview-template="${safeTemplateId}">${templateBody}
  </div>
  ${loadingNote}
  <section class="palette">
    <h2>Bảng màu thương hiệu${safeTemplate ? ` · ${safeTemplate}` : ''}</h2>
    <div class="swatches">${swatches}</div>
  </section>
  <footer>${safeOrg} · ${safeSlug}.kiteclass.vn · Bản xem trước tạo bởi KiteHub AI Branding</footer>
</body>
</html>`;
}

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
// Decision summary (GAP-1118) — recap of the prior wizard steps + jump-to-edit.
//
// Label maps are inlined (not imported from the Audience/Tone step modules) to
// keep this dynamically-loaded chunk free of those components' deps. Keep in
// sync with `AudienceStep.AUDIENCE_OPTIONS` / `ToneStep.TONE_OPTIONS` ids.
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

/** Side-panel summary of prior decisions with quick "edit → jump to step" (GAP-1118 §2). */
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
  /**
   * Instance id used to count PORTRAIT assets for the decision summary
   * (GAP-1118). Distinct from `wizardState.instanceId` (the deploy tenant claim,
   * null until job creation) — this is the asset-owning instance from Step 1-3.
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
  // becomes non-empty. Without this the deploy-stream stayed disabled (jobId='')
  // → "Đang chờ log…" forever. Guarded by a ref + the jobId check so it fires at
  // most once per wizard session.
  const { mutate: createJobMutate } = useCreateBrandingJobV1();
  const createStartedRef = useRef(false);

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
        // GAP-1115 — org-type axis flows into the generate request.
        orgType: wizardState.orgType,
      },
      {
        onSuccess: (job) => {
          if (job?.jobId) {
            // GAP-1105: capture the REAL instanceId (tenant claim) so the
            // lifecycle/events "Tiến trình" panel polls the right key — NOT jobId.
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
          // Allow a retry on the next render if creation failed.
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

  // Wave 34 (GAP-272k): brand colours sourced from real backend job via
  // `usePreviewBrandColors`. Falls back to FALLBACK_BRAND while the v1
  // job is still loading or returns no colors.
  const { brandColors: jobBrandColors, isLoading: brandColorsLoading } =
    usePreviewBrandColors(wizardState.jobId ?? undefined);

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

  // Accent colour for the preview CTA — only the wizard-level wire shape
  // carries `accent`; fall back to the secondary brand colour otherwise.
  const accentColor = jobBrandColors?.accent ?? brandColors.secondary;

  const selectedTemplate = useMemo(
    () => TEMPLATES.find((t) => t.id === wizardState.templateId) ?? null,
    [wizardState.templateId],
  );

  // Wave 34 (GAP-272j): backend HTML render endpoint URL via `usePreview`.
  // Kept as a documented FALLBACK only — the iframe renders client-side
  // `srcDoc` (see `buildPreviewHtml`) because the backend `/preview` endpoint
  // is not wired yet AND an `<iframe src>` cannot attach the auth header.
  const hookedPreviewUrl = usePreview(wizardState.jobId ?? undefined);
  const previewUrl = previewUrlOverride ?? hookedPreviewUrl ?? '';

  // AC2 "AI branding preview được": compose the live preview client-side from
  // the brand colours + org name + uploaded logo the FE already holds.
  const previewHtml = useMemo<string>(
    () =>
      buildPreviewHtml({
        brand: brandColors,
        accent: accentColor,
        orgName: wizardState.tenantName,
        slug: wizardState.slug,
        logoUrl: wizardState.logoUrl,
        templateId: wizardState.templateId ?? null,
        templateName: selectedTemplate?.name ?? null,
        loading: brandColorsLoading,
      }),
    [
      brandColors,
      accentColor,
      wizardState.tenantName,
      wizardState.slug,
      wizardState.logoUrl,
      selectedTemplate,
      brandColorsLoading,
    ],
  );

  const approvedCount = wizardState.approvedResources.length;
  const totalResources = RESOURCES.length;
  const allApproved = approvedCount === totalResources;

  // -------------------------------------------------------------------------
  // GAP-1118 — full-screen preview mode + prior-decisions summary panel
  // -------------------------------------------------------------------------

  const [fullscreenOpen, setFullscreenOpen] = useState(false);

  // Count PORTRAIT assets for the summary (GAP-1116 step feeds this).
  const { data: instanceAssets } = useAssets(assetInstanceId);
  const portraitCount = (instanceAssets ?? []).filter((a) => a.type === 'PORTRAIT').length;

  const logoValue = wizardState.logoUrl
    ? 'Đã tải lên'
    : wizardState.aiLogo
      ? 'AI tự tạo'
      : 'Chưa chọn';

  const decisionRows = useMemo<ReadonlyArray<DecisionRow>>(
    () => [
      {
        key: 'org-type',
        label: 'Loại tổ chức',
        value: ORG_TYPE_OPTIONS.find((o) => o.id === wizardState.orgType)?.label ?? 'Chưa chọn',
        step: 1,
      },
      { key: 'logo', label: 'Logo', value: logoValue, step: 2 },
      {
        key: 'portrait',
        label: 'Chân dung',
        value: portraitCount > 0 ? `${portraitCount} ảnh` : 'Chưa có',
        step: 3,
      },
      {
        key: 'audience',
        label: 'Đối tượng',
        value: wizardState.audience
          ? AUDIENCE_LABELS[wizardState.audience] ?? wizardState.audience
          : 'Chưa chọn',
        step: 4,
      },
      {
        key: 'tone',
        label: 'Phong cách',
        value: wizardState.tone ? TONE_LABELS[wizardState.tone] ?? wizardState.tone : 'Chưa chọn',
        step: 5,
      },
      {
        key: 'template',
        label: 'Mẫu thiết kế',
        value: selectedTemplate?.name ?? 'Chưa chọn',
        step: 6,
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

  // Auto-trigger upsell modal when quota exhausted on non-ENTERPRISE tier
  // (per `ai-branding-guidelines.md` §4.3 + AC item 4).
  useEffect(() => {
    if (quotaExceeded && quotaTier !== 'ENTERPRISE' && !upsellModalOpen) {
      setUpsellModalOpen(true);
    }
  }, [quotaExceeded, quotaTier, upsellModalOpen]);

  // Forward SSE `complete` to parent — propagates wizard exit (GAP-1108: surface
  // a success toast before redirect). Guarded so the toast + onDeploy fire once
  // even if the `complete` event lingers across renders before navigation.
  const deployCompletedRef = useRef(false);
  useEffect(() => {
    if (!isDeploying) return;
    const latest = deployStream.latestEvent;
    if (latest?.name === 'complete' && !deployCompletedRef.current) {
      deployCompletedRef.current = true;
      toast.success('Triển khai thành công — đang chuyển tới trang Thương hiệu');
      onDeploy();
    }
  }, [isDeploying, deployStream.latestEvent, onDeploy]);

  const handleDeployClick = () => {
    if (!allApproved || !wizardState.jobId) return;
    // GAP-1021: persist approved theme + trigger backend MOCK provisioning
    // (returns 202; async lifecycle drives the job to DEPLOYED). Fire-and-forget —
    // the SSE deploy-stream surfaces progress + the terminal `complete` event.
    approveMutate({
      jobId: wizardState.jobId,
      slug: wizardState.slug || undefined,
      templateId: wizardState.templateId,
      approvedResources: wizardState.approvedResources,
    });
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
          typeof wizardState.instanceId === 'string'
            ? wizardState.instanceId
            : undefined
        }
      />
    );
  }

  return (
    <div className="space-y-6" data-testid="step6-preview">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-primary uppercase tracking-wide mb-1">
            Bước 7 / 7 — Cuối cùng!
          </p>
          <h1 className="text-2xl font-bold text-foreground mb-2">
            Xem trước trang web của bạn
          </h1>
          <p className="text-muted-foreground">
            Bật/tắt từng tài nguyên (logo, màu, banner, hero) để chọn cái bạn muốn
            deploy. AI đã chấm điểm chất lượng ở phải.
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

            {/* Live preview rendered CLIENT-SIDE via srcDoc (brand colours +
                org name + uploaded logo). `src` is only used as a fallback if
                previewHtml is ever empty (it never is) — see buildPreviewHtml
                + usePreview comments above (AC2 fix / GAP-272j / GAP-1021). */}
            <iframe
              srcDoc={previewHtml}
              src={previewHtml ? undefined : previewUrl || undefined}
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

        {/* Right column — summary + approve stack + qgate scaffold + regen scaffold */}
        <div className="space-y-4">
          {/* GAP-1118 — prior-decisions summary with jump-to-edit links. */}
          <DecisionSummary rows={decisionRows} onJump={handleJumpToStep} />

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

      {/* GAP-1118 — full-screen live preview + side panel (summary + approve). */}
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
            {/* Left — large live iframe preview */}
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
                  srcDoc={previewHtml}
                  title="Xem trước toàn màn hình"
                  data-testid="step6-fullscreen-iframe"
                  className="w-full border-0 bg-white"
                  style={{ height: '70vh', minHeight: 360 }}
                  sandbox="allow-same-origin"
                />
              </div>
            </div>

            {/* Right — summary (jump-to-edit) + per-resource approve + deploy */}
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
