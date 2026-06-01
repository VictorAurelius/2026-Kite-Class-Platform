# GAP-127: Frontend has zero code-splitting across 64 pages — bundles likely >300 KB

**Status:** 🟡 PARTIAL (Wave 7-Perf + wave-beta-readiness-9: analyzer + extensive code-split + optimizePackageImports complete; only CI bundle-budget guardrail remains → GAP-236)
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

- [x] Bundle analyzer committed + baseline report attached (Wave 7-Perf; `@next/bundle-analyzer` gated `ANALYZE=true` both apps)
- [x] Marketing public `/` First Load JS < 150 KB (KiteHub `/` = 110 kB verified wave-beta-readiness-9 `next build`)
- [x] Admin dashboard First Load JS < 300 KB (KiteHub `/dashboard` 181 kB, `/admin/instances` 202 kB; KiteClass `/students`/`/teachers` 251 kB — all < 300 KB)
- [x] At least 5 routes use `dynamic()` for heavy components (20+ dynamic components/pages across both apps incl GAP-236 work)
- [x] `modularizeImports` + `optimizePackageImports` configured (both apps; wave-beta-readiness-9 added missing `@tanstack/react-table` to KiteClass list)
- [ ] CI check fails if any route exceeds 250 KB First Load JS  →  tracked GAP-236 (per-route bundle budget enforcement in CI)

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
- **2026-06-01 — wave-beta-readiness-9 Bucket C (PARTIAL → near-DONE):** State-check per
  `audit-to-gap-pipeline.md` §2.8 found the codebase already far ahead of the Wave 7-Perf
  Log description — both apps have analyzer + `optimizePackageImports` + `images.formats`,
  and ~20+ components/pages use `next/dynamic` (GAP-236 work landed). Single delta fixed:
  KiteClass `next.config.js` `optimizePackageImports` was **missing `@tanstack/react-table`**
  (actively used by DataTable + 4 column-config files + dashboard list pages; KiteHub already
  had it). Added it (mirrors KiteHub). Production builds verified clean both apps:
  - `pnpm --filter kiteclass-frontend build` → ✓ Compiled, 59/59 static pages, exit 0;
    shared First Load JS 103 kB; list pages `/students` 251 kB, `/teachers` 251 kB,
    `/billing` 233 kB, `/courses` 220 kB — all < 300 KB.
  - `pnpm --filter kitehub-frontend build` → ✓ Compiled, 90/90 static pages, exit 0;
    landing `/` = **110 kB** (< 150 KB marketing AC), `/dashboard` 181 kB, `/admin/instances`
    202 kB — all under cap.
  5/6 AC verified DONE; only CI per-route bundle-budget guardrail remains (deferred → GAP-236).
  Stays 🟡 PARTIAL.
- 2026-04-19 — Gap created from performance baseline audit
