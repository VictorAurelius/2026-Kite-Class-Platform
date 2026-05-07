# GAP-272f: Wave 32 visual regression baseline for AI Branding Wizard v2

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend tests (Playwright / visual regression)
**Found:** 2026-05-07 (Wave 32 REWORK closure)
**Affects:** AI Branding Wizard 28 screens — visual regression baseline tracking
**Related:** GAP-272 (parent), GAP-227 (real visual regression diff infra)

## Problem

Wave 32 REWORK shipped 4 buckets (PRs #887/889/888/890) implementing
Direction C 6-step wizard. Per Wave 32 v1 plan §7 closure protocol,
visual regression baseline phải established post-merge so future PRs
detect unintended visual drift.

State-check 2026-05-07:
- No `playwright.config.ts` visual regression suite for `(customer)/branding/wizard/*`
- No baseline screenshots in `kitehub-frontend/tests/visual/` or similar
- GAP-227 describes infra for real visual regression diff (Wave 8+); this
  gap is the Wave-32-specific BASELINE consumption

## Root Cause

Visual regression infra (GAP-227) is Wave 8+ scope; without it, Wave 32's
28 screens have no protection against future style drift. Bucket-level
unit tests verify component logic but not pixel-level rendering across
all 6 steps + sub-states.

## Proposed Fix

1. **Setup:** Playwright visual regression config for wizard route
2. **Baseline capture:** screenshots cho 6 steps + sub-states (slug
   default/validating/conflict, logo default/uploaded/skip/error, audience
   default/selected, tone default/selected, template grid/fullscreen,
   step6 preview/qgate-pass/qgate-fail/regenerate/quota-empty/deploying,
   advanced settings, disclaimer modal)
3. **CI integration:** run on every wizard-touching PR; fail if diff >5%
4. **Baseline refresh policy:** documented in `documents/05-guides/operations/visual-regression-runbook.md`

## Acceptance Criteria

- [ ] Playwright visual config covers wizard route
- [ ] Baseline screenshots committed cho 28 wizard screens
- [ ] CI gate active (pre-merge for wizard PRs)
- [ ] Refresh runbook documented

## Related

- GAP-272 (parent)
- GAP-227 (real visual regression infra — Wave 8+)
- Wave 32 v1 plan §7 (this letter pre-named there)
