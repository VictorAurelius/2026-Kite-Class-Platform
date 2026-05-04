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
