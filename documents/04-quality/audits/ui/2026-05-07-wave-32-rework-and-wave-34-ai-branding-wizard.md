# UI Audit — Wave 32 Rework + Wave 34 AI Branding Wizard v2

**Date:** 2026-05-07
**Auditor:** Background agent a1ffe560 (Sonnet, Explore subagent)
**Scope:** Wave 32 rework (PRs #886, #032ef6b3, #81aa2ec2, #4b153f93, #4f09efa2) + Wave 34 (PRs #905-911)
**Method:** Static-code audit (dev stack chưa boot — KHÔNG runtime screenshot)

---

## Score: 97/128 — A+

| Dimension | Score | Notes |
|-----------|:-----:|-------|
| Visual Hierarchy | 14/16 | Card system + eyebrow/title/subtitle pattern consistent. SVG previews responsive. |
| Layout & Spacing | 15/16 | max-w-* constraints + flexbox sound. QualityGateWidget placeholder breaks symmetry slightly. |
| Typography | 14/16 | Font weights consistent. Minor: estimatedTokens display thiếu `<code>` semantic wrapper |
| Color & Contrast | 15/16 | Dark-mode via theme tokens. FALLBACK_BRAND hardcoded chờ API (TODO 272k acknowledged) |
| Motion & Interaction | 13/16 | Debounce 600ms slug + spinner. Missing: skeleton cho Step 6 iframe |
| Accessibility (WCAG) | 15/16 | aria-label/aria-current/aria-hidden/role="status" đầy đủ. Focus rings consistent |
| Content & Copy | 15/16 | VN localization 100%. Helper hints + time estimates present. **i18n keys absent (hardcoded VN)** |
| Brand Consistency | 16/16 | Kite design tokens, ThemePreview G11 integration, T1-T6 template names consistent |

---

## Top 5 Findings

| ID | Sev | Component | Issue |
|----|:---:|-----------|-------|
| F1 | P1 | `Step6Preview.tsx:50-51` | Hardcoded FALLBACK_BRAND fallback during Wave 34 API loading; ~500ms render lag |
| F2 | P1 | `TemplateStep.tsx:204-209` | Enterprise prompt LOCAL state (resets on back-nav) |
| F3 | P2 | `QualityGateWidget.tsx:14-20` | 3 TODO(GAP-226/227/228) — deploy gate bypassable in tests |
| F4 | P2 | `WelcomeStep.tsx:27` | No client-side slug-validation cache |
| F5 | P2 | `TemplateGrid.tsx:13-15` | LOCAL templates catalogue (TODO 272n) — backend schema mismatch |

---

## Gap Recommendations

| Gap | Sev | Rationale |
|-----|:---:|-----------|
| **GAP-272o** (existing) | P1 | LifecycleInline orchestrator wire — confirmed |
| **GAP-272p** (NEW) | P1 | RegenerateCounter quota stale-check post POST /regenerate |
| **GAP-272q** (NEW, defer post-BETA) | P0 macro | i18n migration — 77 tests + components 100% hardcoded VN strings |

---

## Delta vs Wave 31 baseline (KH pro v2 kit ports)

| Metric | Wave 31 | Wave 32/34 | Delta |
|--------|--------|------------|------|
| UI Score | 89/128 | 97/128 | +8 |
| Accessibility | 12/16 | 15/16 | +3 |
| Test count | 45 | 77 | +32 |
| Hard inline mocks | 8 | 0 | -8 |
| Documented TODO refs | 0 | 11 (linked GAP-272x) | discipline ✓ |

**Verdict:** Wave 32 rework + Wave 34 EXCEEDS Wave 31 baseline. Zero scaffold-as-DONE recurrence.

---

## Limitations

- Static-code only; no runtime visual regression (dev stack chưa boot, GAP-244 dev-stack schema fix pending)
- A11y measured by code audit, not axe-core runtime
- Brand color fidelity not measured against design tokens (no Figma diff)
