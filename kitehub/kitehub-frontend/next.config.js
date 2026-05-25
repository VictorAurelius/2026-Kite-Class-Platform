/** @type {import('next').NextConfig} */
const path = require('path');
const withBundleAnalyzer = require('@next/bundle-analyzer')({
  enabled: process.env.ANALYZE === 'true',
});

const nextConfig = {
  output: 'standalone',
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
        hostname: '*.amazonaws.com',
      },
    ],
  },
  // Defense-in-depth redirects for `/auth/*` paths.
  // Frontend routes use industry-standard top-level URLs (`/login`, `/register` etc.)
  // — `(auth)/` is a Next.js route group (parentheses = code organization only, NOT URL segment).
  // But external links (marketing, email templates, docs) may accidentally use `/auth/*`
  // form per folder-name intuition. These 301 redirects prevent 404s from those typos.
  async redirects() {
    return [
      { source: '/auth/login', destination: '/login', permanent: true },
      { source: '/auth/register', destination: '/register', permanent: true },
      { source: '/auth/beta-signup', destination: '/beta-signup', permanent: true },
      { source: '/auth/request-beta-access', destination: '/request-beta-access', permanent: true },
      { source: '/auth/verify-email', destination: '/verify-email', permanent: true },
    ];
  },
  // Wave 86 Bucket E Fix 3 — CSP + security headers per OWASP A05 + threat-model
  // 2026-05-16-auth-flow-magic-link §I3 (Referrer-Policy). Phase 1 BETA ships
  // Report-Only mode so we collect violations without breaking BETA users; flip
  // to enforce after 1 week zero P0 violations.
  async headers() {
    const cspDirectives = [
      "default-src 'self'",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
      "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
      "img-src 'self' data: https: blob:",
      "font-src 'self' https://fonts.gstatic.com data:",
      "connect-src 'self' https://kitehub.me https://*.kitehub.me wss://*.kitehub.me",
      "frame-ancestors 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "object-src 'none'",
      "upgrade-insecure-requests",
    ].join('; ');
    return [
      {
        source: '/(.*)',
        headers: [
          // Phase 1 BETA: Report-Only mode — log violations, don't block. Flip
          // header name to 'Content-Security-Policy' after 1 week clean Phase 1.5.
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
  // GAP-127 Wave 7-Perf — barrel optimization to shrink First Load JS.
  // `optimizePackageImports` enables Next.js modular re-exports for these
  // libraries so only consumed icons/components ship to the client.
  experimental: {
    optimizePackageImports: [
      'lucide-react',
      'date-fns',
      'recharts',
      '@tanstack/react-table',
      '@radix-ui/react-alert-dialog',
      '@radix-ui/react-avatar',
      '@radix-ui/react-checkbox',
      '@radix-ui/react-dialog',
      '@radix-ui/react-dropdown-menu',
      '@radix-ui/react-label',
      '@radix-ui/react-select',
      '@radix-ui/react-separator',
      '@radix-ui/react-slot',
      '@radix-ui/react-switch',
      '@radix-ui/react-tabs',
      '@radix-ui/react-toast',
      '@radix-ui/react-tooltip',
    ],
  },
};

module.exports = withBundleAnalyzer(nextConfig);
