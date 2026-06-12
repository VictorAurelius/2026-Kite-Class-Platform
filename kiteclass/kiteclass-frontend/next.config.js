/** @type {import('next').NextConfig} */
const path = require('path');
const withBundleAnalyzer = require('@next/bundle-analyzer')({
  enabled: process.env.ANALYZE === 'true',
});

const nextConfig = {
  output: 'standalone', // Enable standalone output for Docker builds
  // ADR-024 — pnpm workspace consumer. outputFileTracingRoot points at repo
  // root so Next standalone bundles workspace deps (e.g. @kite/shared-ui).
  outputFileTracingRoot: path.join(__dirname, '../..'),
  // ADR-024 — consume @kite/shared-ui as TypeScript source (no build step)
  transpilePackages: ['@kite/shared-ui'],
  images: {
    formats: ['image/avif', 'image/webp'],
    minimumCacheTTL: 86400,
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'cdn.kiteclass.com',
      },
    ],
  },
  // GAP-127 Wave 7-Perf — modular re-exports + barrel optimization to reduce
  // First Load JS. `optimizePackageImports` lets Next inline only used exports
  // from these libraries instead of the entire barrel index file.
  experimental: {
    optimizePackageImports: [
      'lucide-react',
      'date-fns',
      // GAP-127 wave-beta-readiness-9 — @tanstack/react-table is actively used
      // by DataTable + 4 column-config files + dashboard list pages but was
      // missing from this barrel-optimize list (KiteHub already had it). Adding
      // it lets Next inline only consumed exports instead of the whole barrel.
      '@tanstack/react-table',
      '@radix-ui/react-avatar',
      '@radix-ui/react-checkbox',
      '@radix-ui/react-dialog',
      '@radix-ui/react-dropdown-menu',
      '@radix-ui/react-label',
      '@radix-ui/react-popover',
      '@radix-ui/react-radio-group',
      '@radix-ui/react-select',
      '@radix-ui/react-separator',
      '@radix-ui/react-slot',
      '@radix-ui/react-switch',
      '@radix-ui/react-tabs',
      '@radix-ui/react-toast',
      '@radix-ui/react-tooltip',
    ],
  },
  // Wave 49 Bucket 0 — PWA infra + Wave 86 Bucket E Fix 3 CSP per OWASP A05.
  // Service worker scope header; manifest cache; CSP Report-Only Phase 1 BETA.
  async headers() {
    // Derive the configured API origin (+ ws variant) so connect-src allows the
    // actual gateway per environment: http://localhost:9000 in local dev,
    // the production API origin in prod. Avoids hardcoding + CSP connect-src
    // violations when the FE talks to NEXT_PUBLIC_API_URL.
    let apiOrigin = '';
    let apiWsOrigin = '';
    try {
      const u = new URL(process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000');
      apiOrigin = u.origin;
      apiWsOrigin = u.origin.replace(/^http/, 'ws');
    } catch (_) {
      /* leave empty if NEXT_PUBLIC_API_URL unset/invalid */
    }
    const connectSrc = [
      "'self'",
      apiOrigin,
      apiWsOrigin,
      'https://kiteclass.com',
      'https://*.kiteclass.com',
      'wss://*.kiteclass.com',
    ].filter(Boolean).join(' ');
    // Dev stack serves MinIO logo/branding assets over http://localhost:9100.
    // Allow that host in non-prod so logo previews don't trip CSP once it flips
    // to enforce. Production stays https-only (mirrors kitehub-frontend devImg
    // pattern, GAP-1112 kitehub side; GAP-1198 kiteclass side).
    const isDev = process.env.NODE_ENV !== 'production';
    const devImg = isDev ? ' http://localhost:9100' : '';
    const cspDirectives = [
      "default-src 'self'",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
      "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
      `img-src 'self' data: https: blob: https://cdn.kiteclass.com${devImg}`,
      "font-src 'self' https://fonts.gstatic.com data:",
      `connect-src ${connectSrc}`,
      "frame-ancestors 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "object-src 'none'",
      "worker-src 'self' blob:",
      // NOTE: 'upgrade-insecure-requests' intentionally omitted — it is ignored
      // when delivered via Content-Security-Policy-Report-Only (browser warning).
      // Re-add under enforcing CSP (not report-only) per Wave 86 Bucket E flip plan.
    ].join('; ');
    // GAP-1215 — the AI Branding wizard (KiteHub `:3001`) embeds the landing
    // `/preview` route in a cross-origin iframe so the owner sees the REAL render
    // path themed by their draft brand. Only `/preview` is framable, and only by
    // the KiteHub wizard origin (NOT wildcard); `/` keeps `X-Frame-Options: DENY`.
    // GAP-1238 (pre-walk sim #1, P0): local Docker chạy PRODUCTION build
    // (NODE_ENV=production → isDev=false) nhưng wizard vẫn ở http://localhost:3001
    // → gate bằng isDev làm iframe /preview bị CSP chặn trong walk local.
    // Origin wizard phải env-driven: default localhost:3001 (local Docker + dev);
    // production AWS set KITEHUB_WIZARD_ORIGIN='' để tắt (frame-ancestors chỉ còn
    // kitehub.me). Dùng ?? để empty-string disable được default.
    const wizardOrigin = process.env.KITEHUB_WIZARD_ORIGIN ?? 'http://localhost:3001';
    const khFrameAncestors = [
      "'self'",
      'https://kitehub.me',
      'https://*.kitehub.me',
      ...(wizardOrigin ? [wizardOrigin] : []),
    ].join(' ');
    const previewCsp = cspDirectives.replace(
      "frame-ancestors 'none'",
      `frame-ancestors ${khFrameAncestors}`,
    );
    const previewSecurityHeaders = [
      // Enforcing frame-ancestors → restricts framing to the KiteHub wizard origin
      // only (defense even before the full CSP flips off report-only). NOTE: no
      // `X-Frame-Options` here on purpose — its DENY can't allow a specific cross
      // origin, so we drop it for `/preview` and rely on frame-ancestors.
      { key: 'Content-Security-Policy', value: `frame-ancestors ${khFrameAncestors}` },
      { key: 'Content-Security-Policy-Report-Only', value: previewCsp },
      { key: 'X-Content-Type-Options', value: 'nosniff' },
      { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
      { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(), payment=()' },
      { key: 'Strict-Transport-Security', value: 'max-age=31536000; includeSubDomains' },
    ];
    return [
      {
        source: '/sw.js',
        headers: [
          { key: 'Service-Worker-Allowed', value: '/' },
          { key: 'Cache-Control', value: 'public, max-age=0, must-revalidate' },
          { key: 'Content-Type', value: 'application/javascript; charset=utf-8' },
        ],
      },
      {
        source: '/manifest.json',
        headers: [
          { key: 'Cache-Control', value: 'public, max-age=300, must-revalidate' },
          { key: 'Content-Type', value: 'application/manifest+json; charset=utf-8' },
        ],
      },
      // GAP-1215 — wizard preview surface: framable by the KiteHub origin only,
      // no X-Frame-Options DENY. Must precede the broad rule + the broad rule
      // excludes `/preview` (negative lookahead) so DENY is never sent here.
      { source: '/preview', headers: previewSecurityHeaders },
      { source: '/preview/:path*', headers: previewSecurityHeaders },
      // Wave 86 Bucket E Fix 3 — CSP + security headers per OWASP A05.
      // Phase 1 BETA Report-Only mode; flip to enforce after 1 week clean.
      // `/preview` excluded (negative lookahead) — it gets previewSecurityHeaders.
      {
        source: '/((?!preview).*)',
        headers: [
          { key: 'Content-Security-Policy-Report-Only', value: cspDirectives },
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
          { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(), payment=()' },
          { key: 'Strict-Transport-Security', value: 'max-age=31536000; includeSubDomains' },
        ],
      },
    ];
  },
};

module.exports = withBundleAnalyzer(nextConfig);
