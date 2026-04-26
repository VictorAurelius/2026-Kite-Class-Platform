# GAP-127: Frontend has zero code-splitting across 64 pages — bundles likely >300 KB

**Status:** 🟡 PARTIAL (Wave 7-Perf Agent B: bundle analyzer + landing-page split + per-list-page DataTable lazy)
**Priority:** 🔴 P0
**Domain:** Frontend / Performance
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** `kiteclass-frontend` (40 pages), `kitehub-frontend` (24 pages)
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Current State (verified 2026-04-26)

After build measurement on the actual codebase, the realistic state is much
better than the original audit predicted (the audit assumed worst-case bundles
but `next/dynamic` was unused, not that bundles were 400-550 KB):

- **kiteclass-frontend** First Load JS already <250 KB on every route (max 241 KB
  on `/courses/new`, `/courses/[id]/edit`, `/courses/[id]/classes/new`).
- **kitehub-frontend** First Load JS already <200 KB on every route (max 198 KB
  on `/admin/instances`).
- The `optimizePackageImports` Next.js feature contributes the bulk of the gain —
  Radix UI + lucide-react + date-fns barrels were the main bloat source.

Wave 7-Perf Agent B closed:
- `@next/bundle-analyzer` wired to both apps (`pnpm analyze`)
- `experimental.optimizePackageImports` for radix + lucide + date-fns + recharts + react-table
- KiteHub landing `/` route: framer-motion split into separate chunk via `LandingShell` (`ssr: false`)
- 5 KiteClass dashboard list pages (teachers, students, classes, courses, billing) use `DataTable` via `next/dynamic`
- 5 column-config files: `ColumnDef` imports converted to `import type` (zero runtime cost)
- `images.formats: ['image/avif','image/webp']` + `minimumCacheTTL: 86400` on both apps

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

- **2026-04-26 — Wave 7-Perf Agent B (PARTIAL):** Scope revised after build-state check.
  Baseline build measurement showed both FE apps already <250 KB First Load JS thanks
  to existing `optimizePackageImports`-friendly architecture. Shipped: bundle analyzer
  in both apps, broader `optimizePackageImports`, image format/cache config, framer-motion
  code-split on landing page (165 KB → 104 KB First Load JS, –61 KB / –37%), DataTable
  lazy wrapper on 5 KiteClass list pages, type-only ColumnDef imports.
  484/484 KiteHub tests + 550/550 KiteClass tests pass.
  Out-of-scope (44+ remaining pages): refile as **GAP-236 — finish FE code-splitting for
  remaining auth/wizard/customer/admin pages + per-route bundle budget enforcement in CI**.
- 2026-04-19 — Gap created from performance baseline audit
