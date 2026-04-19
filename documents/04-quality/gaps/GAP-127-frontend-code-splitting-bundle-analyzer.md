# GAP-127: Frontend has zero code-splitting across 64 pages — bundles likely >300 KB

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Frontend / Performance
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** `kiteclass-frontend` (40 pages), `kitehub-frontend` (24 pages)
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

Static analysis found:
- `grep 'dynamic(' / 'lazy(' / 'React.lazy'` in `kiteclass-frontend/src/**/*.tsx` → **1 hit (test utility)**.
- Same grep in `kitehub-frontend/src/**/*.tsx` → **0 hits**.
- `next.config.js` for both projects is 12 lines: `output: standalone` + `images.remotePatterns`. No bundle analyzer, no `modularizeImports`, no `experimental.optimizePackageImports`, no `images.formats`.
- Heavy deps all in initial bundle: framer-motion (~130 KB gz), recharts (~180 KB gz), @tanstack/react-table (~50 KB gz), 16-24 Radix UI primitives.

Every page module statically imports everything → marketing public landing ships admin bundles and vice versa. Expected First Load JS: marketing ~180-220 KB, admin dashboard ~400-550 KB (both exceed 150 KB rubric threshold).

## Context

- Next.js 15 supports RSC + `dynamic()`; zero usage today.
- `next build` output has never been committed or measured in CI.
- No bundle-size budget enforcement.

## Evidence

- `kiteclass/kiteclass-frontend/next.config.js` (12 lines, minimal)
- `kitehub/kitehub-frontend/next.config.js` (12 lines, minimal)
- `kiteclass/kiteclass-frontend/package.json` — 24 Radix packages
- `kitehub/kitehub-frontend/package.json` — framer-motion, recharts, remark, gray-matter
- Performance audit §3

## Proposed Fix

1. Install `@next/bundle-analyzer`; wrap `next.config.js`:
   ```js
   const withBundleAnalyzer = require('@next/bundle-analyzer')({ enabled: process.env.ANALYZE === 'true' });
   module.exports = withBundleAnalyzer({ ... });
   ```
2. Add `npm run analyze` script; commit one baseline report to `documents/04-quality/audits/performance/`.
3. Enable `experimental.optimizePackageImports: ['lucide-react', '@radix-ui/react-*', 'date-fns']`.
4. Add `modularizeImports` for `lucide-react` and `date-fns`.
5. Convert top-5 heaviest pages to `dynamic(() => import('...'), { ssr: false })` where not needed for SEO:
   - Admin charts (uses recharts)
   - Billing page
   - Parent portal dashboard
   - Branding wizard step 6 (preview)
   - Bulk import page (uses xlsx libraries if/when added)
6. Add `images.formats: ['image/avif', 'image/webp']` + `images.minimumCacheTTL: 86400`.
7. Establish CI guardrail: fail build if any route First Load JS > 250 KB (via `next-bundle-analyzer` threshold or custom script).

## Acceptance Criteria

- [ ] Bundle analyzer committed + baseline report attached
- [ ] Marketing public `/` First Load JS < 150 KB
- [ ] Admin dashboard First Load JS < 300 KB
- [ ] At least 5 routes use `dynamic()` for heavy components
- [ ] `modularizeImports` + `optimizePackageImports` configured
- [ ] CI check fails if any route exceeds 250 KB First Load JS

## Related

- Audit: performance-audit-2026-04-19.md §3

## Log

- 2026-04-19 — Gap created from performance baseline audit
