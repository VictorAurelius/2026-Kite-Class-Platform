# GAP-236: FE code-splitting completion + CI bundle budget

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (perf already <250KB on covered routes; remaining 44+ pages = lower priority but still worth converting)
**Domain:** Frontend / Performance / DevOps
**Detected:** 2026-04-26 (Wave 7-Perf Agent B return finding)
**Related:** Parent GAP-127 (PARTIAL); audit `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

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

- [ ] CI fails if any route First Load JS > 250KB (default), with override env var documented
- [ ] ≥30 of remaining 44+ pages converted using `next/dynamic` for heavy children components
- [ ] Bundle analyzer baseline HTML reports committed (kc + kh)
- [ ] No regression on routes already covered by GAP-127

## Out-of-scope

- Dynamic imports for SEO-critical SSR pages (landing, marketing) — keep SSR
- React Server Components migration (separate gap if pursued)

## Related

- Parent: GAP-127 (PARTIAL — closed by Wave 7-Perf Agent B)
- Wave plan: `documents/03-planning/waves/wave-7-perf-cluster.md`
- Memory: `feedback_audit_calibration.md` (audit predicted 400-550KB worst-case; baseline already <250KB)

## Log

- **2026-04-26** — Filed during Wave 7-Perf consolidation. Agent B's return reported audit prediction was worst-case; baseline already healthier than expected. Real win was foundation (bundle analyzer + optimizePackageImports config) — page-level conversions are incremental polish, not blocker.
