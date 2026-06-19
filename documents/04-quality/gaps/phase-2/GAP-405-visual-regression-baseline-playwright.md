# GAP-405: Visual Regression Baseline (Playwright Screenshot Diff)

**Status:** 🟡 PARTIAL 2026-05-07 (Wave 37 Bucket C — spec scaffold ready; baseline PNGs sinh first run on staging)
**Priority:** 🟡 P2
**Domain:** Testing / Visual QA
**Found:** 2026-05-07 (Wave 37 — Layer 3)
**Affects:** Critical screen pixel-level regression detection

## Problem

UI score /128 (97 → 99 Wave 36) là human review. Pixel-level regression (e.g. ConsentBanner dark-pattern compliance, branding wizard step layout) khó detect manual sau mỗi PR.

## Proposed Fix

Playwright `expect.toHaveScreenshot()` baseline cho 8-12 critical screens:
- ConsentBanner (PDPL benchmark, Wave 35)
- Beta-signup form (Wave 36)
- Branding wizard 6 steps (Wave 32 rework + Wave 34)
- Dashboard home (KH + KC)
- Admin/beta-requests queue

Threshold: 0.1% pixel diff acceptable (font rendering variance), >2% fail.

## Acceptance Criteria

- [x] Playwright `expect.toHaveScreenshot()` enabled — `playwright.config.ts` already supports built-in snapshot mode (no config change needed; `maxDiffPixelRatio` set per-test)
- [x] 8 critical screens covered trong `kitehub/kitehub-frontend/e2e/visual-regression/critical-screens.spec.ts` (home, pricing, request-beta-access, beta-signup, admin/beta-requests, login, signup, dashboard)
- [ ] Baseline `*.png` committed — **deferred to first staging run** per `gap-done-discipline.md` §3 PARTIAL exit ramp; baselines must be generated on actual staging environment, not local WSL2 (font rendering variance). Tracked: when staging deploy lands, run `pnpm -F kitehub-frontend exec playwright test visual-regression --update-snapshots` then commit PNGs in follow-up PR.
- [x] Diff report uploaded artifact on fail — covered by GAP-403 workflow `if: failure()` upload step (`playwright-report` + `test-results` artifacts include `*-diff.png`, `*-actual.png`, `*-expected.png`)
- [x] Baseline update workflow documented inline trong spec file header (`pnpm -F kitehub-frontend exec playwright test visual-regression --update-snapshots`)

## Log

- **2026-05-07** Wave 37 Bucket C: visual regression spec scaffold shipped (`e2e/visual-regression/critical-screens.spec.ts` — 8 tests). Per `gap-done-discipline.md` §3 PARTIAL exit ramp: baseline PNG generation deferred to staging-first run (`pnpm exec playwright test --list` confirms 8 specs parse cleanly). Status PARTIAL until staging baseline committed in follow-up PR; tracked here, not in new gap because work is direct continuation of this gap's AC.

## Related

- GAP-403 (parent E2E gate)
- GAP-404 (beta funnel coverage)
- UI audit /128 rubric (manual review continues alongside)
