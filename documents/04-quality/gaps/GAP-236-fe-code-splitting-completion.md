# GAP-236: FE code-splitting completion + CI bundle budget

**Status:** 🟡 PARTIAL — Sub-PR A (CI bundle budget guardrail) shipped 2026-04-26; Sub-PRs B (page conversions) + C (analyzer baseline HTML) still OPEN
**Priority:** 🟡 P2 (perf already <250KB on covered routes; remaining 44+ pages = lower priority but still worth converting)
**Domain:** Frontend / Performance / DevOps
**Detected:** 2026-04-26 (Wave 7-Perf Agent B return finding)
**Related:** Parent GAP-127 (PARTIAL); audit `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`; guide `documents/05-guides/frontend-bundle-budget.md`

## Current State (verified 2026-04-26)

Wave 7-Perf Agent B (PR #570) shipped:
- ✅ `@next/bundle-analyzer` wired on both apps (env-gated `ANALYZE=true`)
- ✅ `experimental.optimizePackageImports` for radix + lucide + recharts + react-table + date-fns
- ✅ AVIF/WEBP image formats + 24h CDN cache
- ✅ 10 pages converted (1 KH landing + 5 KC dashboard + 5 column-config type-only imports)
- ✅ Baseline measurement: all routes <250KB First Load JS (target was <300KB)

Remaining: 44+ pages NOT converted to `next/dynamic` for heavy modals/forms/wizards.

## Problem

GAP-127 covered foundation + 10 highest-impact pages. Real-world growth + new features will reintroduce bundle bloat without:
1. Remaining page conversions (auth wizard, customer settings, admin payments, branding wizard sub-steps, react-day-picker, react-hook-form sub-trees)
2. **CI guardrail** — no automated check that prevents future regression beyond 250KB First Load JS threshold
3. Bundle analyzer baseline reports committed (currently HTML reports gitignored)

## Proposed Fix

### Sub-PR A: CI bundle budget guardrail
- Custom script reading `.next/build-manifest.json` after `pnpm build`
- Threshold: 250KB First Load JS per route (configurable via env)
- Fails CI on any route exceeding budget
- Add to `.github/workflows/frontend-ci.yml` for both apps

### Sub-PR B: Convert remaining heavy pages
Priority order (per heaviest deps):
- `/billing/*` — 4 pages, heavy date-pickers + forms
- `/branding/wizard/step-*` — wizard step components
- `/admin/payments` — react-table + filters
- `/customers/settings` — heavy forms
- Auth wizard — multi-step

### Sub-PR C: Commit bundle analyzer baseline
- Run `ANALYZE=true pnpm build` for both apps
- Commit HTML reports to `documents/04-quality/audits/performance/bundle-analyzer-baseline-{kc,kh}.html`
- Re-baseline quarterly per audit cadence

## Acceptance Criteria

- [x] CI fails if any route First Load JS > 250KB (default), with override env var documented (✅ shipped 2026-04-26 — Sub-PR A; default 250 KB, env `BUNDLE_BUDGET_KB`, per-route `bundle-budget.json`)
- [ ] ≥30 of remaining 44+ pages converted using `next/dynamic` for heavy children components
- [ ] Bundle analyzer baseline HTML reports committed (kc + kh)
- [x] No regression on routes already covered by GAP-127 (✅ verified 2026-04-26 — all 52 KC + 38 KH routes within 250 KB baseline)

## Out-of-scope

- Dynamic imports for SEO-critical SSR pages (landing, marketing) — keep SSR
- React Server Components migration (separate gap if pursued)

## Related

- Parent: GAP-127 (PARTIAL — closed by Wave 7-Perf Agent B)
- Wave plan: `documents/03-planning/waves/wave-7-perf-cluster.md`
- Memory: `feedback_audit_calibration.md` (audit predicted 400-550KB worst-case; baseline already <250KB)

## Log

- **2026-04-28** — Wave GAP-236 Sub-PR B Agent A shipped: KiteClass auth + public pages code-split (7 pages). Extracted heavy form bodies into `@/components/auth/{login,forgot-password,reset-password,student-register,parent-invite}-form.tsx` and `@/components/public/{contact-form,about-details}.tsx`; pages now use `next/dynamic` with skeleton loading state. SSR=false for auth pages (no SEO value), SSR=true for public pages (`/contact`, `/about` keep server-rendered HTML for crawlers). Bundle wins (First Load JS, gzipped, vs baseline 2026-04-28): `/login` 234.27 → 185.75 KB (−48.5 KB), `/forgot-password` 233.08 → 185.74 KB (−47.3 KB), `/reset-password` 233.04 → 209.68 KB (−23.4 KB). Smaller wins on `/register/student`, `/parent-invite/[token]` (chunks split but shared deps already optimized via `optimizePackageImports`). All 52 routes within 250 KB budget. Test-suite green (565 passed, 14 pre-existing skips); 2 tests updated to use `findByLabelText` for async lazy-form resolution.
- **2026-04-26** — Sub-PR A shipped (Wave P2 Cleanup Agent B). CI bundle budget guardrail added to both FE workflows: `scripts/check-bundle-budget.mjs` reads `.next/(app-)build-manifest.json` after `pnpm build`, gzips each chunk per route, fails CI on any route exceeding 250 KB First Load JS. Override via env `BUNDLE_BUDGET_KB` or per-route `bundle-budget.json`. 13 unit tests via `node --test`. Baseline captured: KiteClass top 5 routes = 236.09 / 235.80 / 235.51 / 234.80 / 234.21 KB; KiteHub top 5 = 194.15 / 173.66 / 170.37 / 169.65 / 169.00 KB. All routes within budget. Guide: `documents/05-guides/frontend-bundle-budget.md`. Status flipped 🔵 OPEN → 🟡 PARTIAL — Sub-PRs B (44+ page conversions) + C (analyzer baseline HTML reports) still open.
- **2026-04-26** — Filed during Wave 7-Perf consolidation. Agent B's return reported audit prediction was worst-case; baseline already healthier than expected. Real win was foundation (bundle analyzer + optimizePackageImports config) — page-level conversions are incremental polish, not blocker.
- **2026-04-28** — Wave GAP-236 Sub-PR B / Agent B (KiteClass admin + attendance + billing) shipped. 5 pages converted to `next/dynamic` with `ssr: false` lazy boundaries. New lazy wrappers: `DynamicAttendanceCalendar`, `DynamicAttendanceTrendsChart`, `DynamicClassStatsTable`, `DynamicActiveClassesTable` under `components/attendance/`; `DynamicPaymentForm`, `DynamicInvoiceDetailPanels` under `components/billing/`. Heavy children moved out of initial First Load JS: full SVG trends chart, calendar grid renderer, react-hook-form + zod resolver tree, payment-history + adjustments + invoice-items panels. Pages converted: `/attendance/reports` (417 LOC, AttendanceCalendar lazy), `/attendance` (210 LOC, ActiveClassesTable lazy), `/admin/attendance/stats` (273 LOC, trends chart + class stats table both lazy), `/billing/[id]` (233 LOC, detail panels lazy), `/billing/[id]/pay` (181 LOC, full payment form lazy with react-hook-form + zod). `/billing` already optimized via `dynamic-data-table` (no further change). `pnpm build` green; `pnpm check:budget` reports all 52 routes within 250KB budget — bucket pages: admin/attendance/stats 179.93 KB, attendance 198.33 KB, attendance/reports 202.97 KB, billing 195.59 KB, billing/[id] 189.20 KB, billing/[id]/pay 187.79 KB. `pnpm test --run` 565/565 passing (14 file-level skipped, no regression). Note: react-day-picker is currently only present in `ui/calendar.tsx` and not actually imported by any page in the bucket — calendar.tsx remains untouched as out-of-bucket, but the in-bucket attendance calendars (custom SVG-based, ~7K + ~10K) were the practical heavy-load targets and are now lazy.
