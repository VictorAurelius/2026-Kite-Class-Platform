# GAP-227: Real Visual Regression Diff (replace scaffold)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 Feature (Quality / Visual stability) — Wave 8+ scope
**Domain:** Backend / Quality Gate / Visual diff infrastructure
**Found:** 2026-04-26 (Sub-PR 223.1 baseline audit captured §3 = 8/20 — scaffold pass without visual diff)
**Affects:** Every AI Branding generation — current Quality Reviewer §5.4 visual regression check returns scaffold pass; real diff requires baseline screenshot store + generation screenshot capture + pixel-diff comparison

## Problem

`InstanceQualityReviewer.review()` Step `VisualRegressionCheck` (GAP-012 Wave 4) returns scaffold pass without comparing generated screenshots to tenant baseline. Tenant nhận theme drift (color shift, layout break) post-AI-regeneration mà KHÔNG được flag → silent quality regression.

## Current State (verified 2026-04-26)

- `kitehub-branding/src/main/java/.../qualityreviewer/checks/VisualRegressionCheck.java` — Strategy pattern landed, returns `CheckResult.pass()` regardless
- No screenshot service infrastructure — Playwright runs ad-hoc via `scripts/capture-screenshots.ts`, not service-callable
- No baseline screenshot store per tenant — baseline concept needs design

## Proposed Fix

1. **Screenshot service:** dedicated `kitehub-screenshot` microservice with Playwright headless + queue (RabbitMQ `screenshot.capture.{tenant}`)
2. **Baseline store:** MinIO bucket `branding-baselines/{tenant_id}/{version}/` storing reference screenshots per template + breakpoint
3. **Diff engine:** `pixelmatch` or `odiff` library wrapped as Spring service
4. **Diff threshold:** ≤20% pixel diff = pass, >20% + ≤40% = warning, >40% = fail (config tunable per tenant tier)
5. Update `VisualRegressionCheck` to call diff service + return real CheckResult

## Acceptance Criteria

- [ ] Screenshot service captures 4 breakpoints (mobile/tablet/desktop/wide) for any tenant URL on demand
- [ ] Baseline auto-saved on first DEPLOYED transition; updated on user-approved regenerate
- [ ] Diff engine returns numeric pixel-diff % + side-by-side annotated PNG
- [ ] `VisualRegressionCheck` blocks deploy when diff >40% on any breakpoint
- [ ] Baseline audit re-run after merge → §3 score moves from 8/20 toward ≥16/20

## Dependencies

- **Tracked under:** GAP-225 (umbrella) cluster C3, GAP-223 (governance scaffolding done Sub-PR 223.1)
- **Blocked by:** screenshot service infra (separate gap may be needed); MinIO bucket strategy
- **Aligned with:** existing `scripts/capture-screenshots.ts` Playwright pattern

## References

- `ai-branding-guidelines.md` §5.4 (Visual regression vs baseline ≤20% diff)
- `ai-branding-quality-gate` skill §11.4.3
- GAP-012 (Quality Reviewer scaffold landed Wave 4)
- `pixelmatch` / `odiff` libraries

## Log

- **2026-04-26** — Filed as Sub-PR 223.1 follow-up. Real implementation deferred to Wave 8+ when screenshot service + MinIO baseline store capacity available.
