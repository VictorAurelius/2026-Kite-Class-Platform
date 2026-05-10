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
  // Wave 49 Bucket 0 — PWA infra. Service worker must be served with the
  // proper scope header; manifest must not be aggressively cached so
  // updates propagate quickly. Offline fallback can cache forever.
  async headers() {
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
    ];
  },
};

module.exports = withBundleAnalyzer(nextConfig);
