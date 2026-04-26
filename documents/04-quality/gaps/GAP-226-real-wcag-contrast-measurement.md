# GAP-226: Real WCAG Contrast Measurement (replace scaffold)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 Feature (Quality / Accessibility) — Wave 8+ scope
**Domain:** Backend / Quality Gate / WCAG compliance
**Found:** 2026-04-26 (Sub-PR 223.1 baseline audit captured 8/20 for §3 §5 Quality Gate compatibility — scaffold returns pass without measuring)
**Affects:** Every AI Branding tenant deploy — current Quality Reviewer §5.1 contrast check is scaffold-pass; real WCAG AA requires contrast ratio ≥4.5:1 measurement against generated theme

## Problem

`InstanceQualityReviewer.review()` Step `ContrastCheck` (GAP-012 Wave 4) returns scaffold pass for all themes without computing actual contrast ratio. Tenant nhận theme với contrast <4.5:1 → vi phạm WCAG AA → accessibility lawsuit risk + tenant churn (low-vision users cannot read).

## Current State (verified 2026-04-26)

- `kitehub-branding/src/main/java/.../qualityreviewer/checks/ContrastCheck.java` — Strategy pattern landed, returns `CheckResult.pass()` regardless of input
- Baseline audit `2026-04-26-baseline.md` §3 = 8/20 reflecting this scaffold-only state
- Test `ContrastCheckTest.java` — exists but verifies scaffold behavior only (not real measurement)

## Proposed Fix

1. Implement `WCAGContrastCalculator` utility — compute relative luminance per WCAG 2.1 §1.4.3 formula
2. Update `ContrastCheck` to read theme JSON → extract `--color-primary` + `--color-bg` + `--color-text-on-primary` etc → compute pairs
3. Pass criteria: ALL critical pairs ≥4.5:1 (text on bg, button label on button bg, link on bg)
4. Pass criteria warning: pairs ≥3.0:1 < 4.5:1 → flag as warning (large text exception per WCAG §1.4.3)
5. Update test to use real fixture themes (5 known-good + 5 known-bad)

## Acceptance Criteria

- [ ] `WCAGContrastCalculator` matches WebAIM contrast calculator output for 10 sample colors
- [ ] `ContrastCheck` blocks deploy when any critical pair <4.5:1
- [ ] Test coverage: 5 PASS fixtures + 5 FAIL fixtures verified
- [ ] Baseline audit re-run after merge → §3 score moves from 8/20 toward ≥16/20

## Dependencies

- **Tracked under:** GAP-225 (umbrella) cluster C3, GAP-223 (governance scaffolding done Sub-PR 223.1)
- **Blocks:** §5 Quality Gate full automation; should run before scaling AI Branding tenant count
- **Blocked by:** none (theme JSON schema already stable post Wave 4)

## References

- `ai-branding-guidelines.md` §5.1 (WCAG contrast ratio ≥4.5:1)
- `ai-branding-quality-gate` skill §11.4.3
- GAP-012 (Quality Reviewer scaffold landed Wave 4)
- WCAG 2.1 SC §1.4.3 — Contrast (Minimum)
- WebAIM contrast calculator algorithm

## Log

- **2026-04-26** — Filed as Sub-PR 223.1 follow-up. Real implementation deferred to Wave 8+ when InstanceQualityReviewer infra capacity available.
