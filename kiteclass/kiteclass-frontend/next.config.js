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
      // Wave 86 Bucket E Fix 3 — CSP + security headers per OWASP A05.
      // Phase 1 BETA Report-Only mode; flip to enforce after 1 week clean.
      {
        source: '/(.*)',
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
