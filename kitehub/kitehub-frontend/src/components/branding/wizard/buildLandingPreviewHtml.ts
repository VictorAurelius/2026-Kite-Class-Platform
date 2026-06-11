// ---------------------------------------------------------------------------
// buildLandingPreviewHtml — Step 7 preview composer (GAP-1144).
//
// Renders the wizard preview as the REAL KiteClass tenant landing page built by
// wave-landing-100 (see `kiteclass-frontend/src/app/(public)/page.tsx` +
// section components), themed by the tenant's brand colours + logo, with the
// freshly-generated banner as the hero visual.
//
// Replaces the previous ad-hoc per-template `buildPreviewHtml` (nav + template
// body + palette swatches) — the owner now sees what their landing page will
// actually look like, not a colour-chip demo.
//
// Output is a complete, SCRIPT-FREE `<!doctype html>` string for
// `<iframe srcDoc>` with `sandbox="allow-same-origin"`. Self-contained: no
// imports from Step6Preview, all helpers defined here.
//
// Sections mirrored from wave-landing-100 (plan
// `documents/03-planning/waves/wave-2026-06-09-landing-100.md`):
//   nav · hero (banner or gradient) · Problem · HowItWorks · TrustStrip ·
//   Floating Zalo CTA · footer
// ---------------------------------------------------------------------------

export interface LandingPreviewOptions {
  brand: {
    primary: string;
    secondary: string;
    background: string;
    foreground: string;
  };
  accent: string;
  orgName: string;
  slug: string;
  logoUrl: string | null;
  /** Live banner from the preview-banner endpoint; when present → hero shows it. */
  bannerUrl?: string | null;
  templateId?: string | null;
  templateName?: string | null;
  loading?: boolean;
}

// --- self-contained helpers (no Step6Preview import) -----------------------

const HEX_RE = /^#[0-9A-Fa-f]{3,8}$/;

/** Return `value` only if it is a safe hex colour, else `fallback` (CSS-injection guard). */
function safeColor(value: string | undefined | null, fallback: string): string {
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

const FALLBACK = {
  primary: '#1E40AF',
  secondary: '#F59E0B',
  background: '#FFFFFF',
  foreground: '#0F172A',
};

/**
 * Compose the wave-landing-100 standard tenant landing preview as a static,
 * script-free HTML document. Themed via `:root` brand CSS vars; the live banner
 * (when supplied) becomes the hero visual.
 */
export function buildLandingPreviewHtml(opts: LandingPreviewOptions): string {
  const primary = safeColor(opts.brand?.primary, FALLBACK.primary);
  const secondary = safeColor(opts.brand?.secondary, FALLBACK.secondary);
  const bg = safeColor(opts.brand?.background, FALLBACK.background);
  const fg = safeColor(opts.brand?.foreground, FALLBACK.foreground);
  const accent = safeColor(opts.accent, secondary);

  const safeOrg = escapeHtml((opts.orgName ?? '').trim() || 'Trung tâm của bạn');
  const safeSlug = escapeHtml((opts.slug ?? '').trim() || 'tenant-slug');

  const logoBlock = opts.logoUrl
    ? `<img class="logo-img" src="${escapeHtml(opts.logoUrl)}" alt="Logo ${safeOrg}" />`
    : `<div class="logo-monogram">${escapeHtml(initialsOf(opts.orgName ?? ''))}</div>`;

  const loadingNote = opts.loading
    ? '<div class="loading-note">Đang tạo bản xem trước…</div>'
    : '';

  // Hero: banner image when available, else themed gradient (landing-100 HeroSection).
  // The composed banner already bakes in the headline + subtitle, so the banner
  // hero shows the image cleanly (no overlaid h1/tagline — that doubled the text)
  // with just a CTA bar below it.
  const hero = opts.bannerUrl
    ? `
  <section class="hero hero--banner">
    <img class="hero-banner-img" src="${escapeHtml(opts.bannerUrl)}" alt="Banner ${safeOrg}" />
    <div class="hero-cta-bar"><span class="cta cta--primary">Đăng ký học thử</span></div>
  </section>`
    : `
  <section class="hero hero--gradient">
    <div class="hero-logo">${logoBlock}</div>
    <h1>${safeOrg}</h1>
    <p class="tagline">Nền tảng học tập trực tuyến — quản lý lớp, theo dõi tiến độ, kết nối phụ huynh.</p>
    <span class="cta cta--primary">Đăng ký học thử</span>
  </section>`;

  return `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<style>
  :root { --primary:${primary}; --secondary:${secondary}; --accent:${accent}; --bg:${bg}; --fg:${fg}; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: system-ui, -apple-system, 'Segoe UI', sans-serif; color: var(--fg); background: var(--bg); }

  /* nav */
  .nav { display:flex; align-items:center; gap:10px; padding:14px 28px; border-bottom:1px solid rgba(0,0,0,.07); position:sticky; top:0; background:var(--bg); z-index:5; }
  .nav .brand-name { font-weight:800; font-size:15px; }
  .nav .spacer { flex:1; }
  .nav .nav-link { font-size:13px; opacity:.72; margin-left:18px; }
  .nav .nav-cta { margin-left:18px; padding:8px 16px; border-radius:9999px; background:var(--primary); color:#fff; font-size:13px; font-weight:700; }
  .logo-img { height:30px; width:auto; border-radius:6px; object-fit:contain; }
  .logo-monogram { height:30px; width:30px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:13px; color:#fff; background:linear-gradient(135deg,var(--primary),var(--secondary)); }

  /* hero */
  .hero { text-align:center; color:#fff; }
  .hero--gradient { padding:60px 24px; background:linear-gradient(135deg,var(--primary),var(--secondary)); }
  .hero--gradient .hero-logo { display:flex; justify-content:center; margin-bottom:18px; }
  .hero--gradient .hero-logo .logo-img, .hero--gradient .hero-logo .logo-monogram { height:56px; width:auto; }
  .hero--gradient .hero-logo .logo-monogram { width:56px; font-size:22px; background:rgba(255,255,255,.2); }
  .hero--banner { text-align:center; }
  .hero-banner-img { display:block; width:100%; height:auto; max-height:380px; object-fit:contain; background:#0b1220; }
  .hero-cta-bar { padding:16px 24px; }
  .hero h1 { font-size:32px; line-height:1.2; margin-bottom:6px; text-shadow:0 1px 12px rgba(0,0,0,.25); }
  .hero .tagline { font-size:15px; opacity:.95; max-width:540px; margin:0 auto; }
  .cta { display:inline-block; padding:12px 28px; border-radius:9999px; font-weight:700; font-size:14px; }
  .cta--primary { background:var(--accent); color:#fff; margin-top:8px; }

  /* Problem section (landing-100) */
  .section { padding:44px 28px; }
  .section h2 { font-size:22px; font-weight:800; text-align:center; margin-bottom:8px; }
  .section .sub { font-size:14px; opacity:.65; text-align:center; max-width:520px; margin:0 auto 28px; }
  .cards { display:flex; gap:16px; flex-wrap:wrap; justify-content:center; }
  .card { flex:1 1 200px; max-width:260px; padding:22px; border-radius:14px; background:rgba(0,0,0,.03); border:1px solid rgba(0,0,0,.05); }
  .card .ic { width:40px; height:40px; border-radius:11px; margin-bottom:12px; background:linear-gradient(135deg,var(--primary),var(--accent)); opacity:.9; }
  .card h3 { font-size:15px; margin-bottom:6px; }
  .card p { font-size:13px; opacity:.7; line-height:1.5; }

  /* HowItWorks (landing-100) */
  .how { background:rgba(0,0,0,.02); }
  .steps { display:flex; gap:16px; flex-wrap:wrap; justify-content:center; counter-reset:step; }
  .step { flex:1 1 200px; max-width:260px; padding:22px; text-align:center; }
  .step .num { width:42px; height:42px; border-radius:50%; margin:0 auto 12px; display:flex; align-items:center; justify-content:center; font-weight:800; color:#fff; background:var(--primary); }
  .step h3 { font-size:15px; margin-bottom:6px; }
  .step p { font-size:13px; opacity:.7; }

  /* TrustStrip (landing-100) */
  .trust { background:linear-gradient(135deg,var(--primary),var(--secondary)); color:#fff; padding:36px 24px; }
  .trust-grid { display:flex; gap:24px; flex-wrap:wrap; justify-content:center; }
  .stat { text-align:center; min-width:120px; }
  .stat b { display:block; font-size:30px; font-weight:800; }
  .stat span { font-size:12px; opacity:.9; }

  /* Floating Zalo CTA (landing-100 FloatingCTA) */
  .zalo { position:fixed; right:18px; bottom:18px; display:flex; align-items:center; gap:8px; padding:11px 18px; border-radius:9999px; background:var(--accent); color:#fff; font-weight:700; font-size:13px; box-shadow:0 8px 24px rgba(0,0,0,.18); }
  .zalo .dot { width:9px; height:9px; border-radius:50%; background:#fff; }

  .loading-note { text-align:center; font-size:12px; opacity:.6; padding:8px; }
  footer { padding:22px 24px; text-align:center; font-size:11px; opacity:.55; border-top:1px solid rgba(0,0,0,.06); }
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
    <span class="nav-cta">Đăng nhập</span>
  </nav>

  ${hero}
  ${loadingNote}

  <section class="section problem">
    <h2>Vì sao chọn ${safeOrg}?</h2>
    <p class="sub">Giải pháp học tập trực tuyến toàn diện cho trung tâm của bạn.</p>
    <div class="cards">
      <div class="card"><div class="ic"></div><h3>Quản lý lớp dễ dàng</h3><p>Lịch học, điểm danh và bài tập gọn trong một nơi.</p></div>
      <div class="card"><div class="ic"></div><h3>Theo dõi tiến độ</h3><p>Bảng điểm và báo cáo học tập theo thời gian thực.</p></div>
      <div class="card"><div class="ic"></div><h3>Kết nối phụ huynh</h3><p>Thông báo và trao đổi với giáo viên qua Zalo, email.</p></div>
    </div>
  </section>

  <section class="section how">
    <h2>Cách hoạt động</h2>
    <p class="sub">Bắt đầu chỉ với 3 bước đơn giản.</p>
    <div class="steps">
      <div class="step"><div class="num">1</div><h3>Đăng ký lớp</h3><p>Chọn khóa học và lịch học phù hợp.</p></div>
      <div class="step"><div class="num">2</div><h3>Học & luyện tập</h3><p>Tham gia lớp, làm bài tập, nhận phản hồi.</p></div>
      <div class="step"><div class="num">3</div><h3>Theo dõi kết quả</h3><p>Xem tiến độ và báo cáo học tập.</p></div>
    </div>
  </section>

  <section class="trust">
    <div class="trust-grid">
      <div class="stat"><b>240+</b><span>học viên</span></div>
      <div class="stat"><b>98%</b><span>hài lòng</span></div>
      <div class="stat"><b>15</b><span>năm kinh nghiệm</span></div>
    </div>
  </section>

  <a class="zalo"><span class="dot"></span>Chat Zalo</a>

  <footer>${safeOrg} · ${safeSlug}.kiteclass.vn · Bản xem trước tạo bởi KiteHub AI Branding</footer>
</body>
</html>`;
}

export default buildLandingPreviewHtml;
