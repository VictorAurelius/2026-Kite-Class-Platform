# GAP-405: Visual Regression Baseline (Playwright Screenshot Diff)

**Status:** 🔵 OPEN
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

- [ ] `playwright.config.ts` enables `screenshot` baseline mode
- [ ] Baseline `*.png` committed under `e2e/__screenshots__/<browser>/<screen>.png`
- [ ] CI step `pnpm test:e2e --update-snapshots` available (manual review)
- [ ] Diff report uploaded artifact on fail (HTML viewer)
- [ ] README documents baseline update workflow

## Related

- GAP-403 (parent E2E gate)
- GAP-404 (beta funnel coverage)
- UI audit /128 rubric (manual review continues alongside)
