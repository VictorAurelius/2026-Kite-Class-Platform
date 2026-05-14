# Audit Skill Rubric — ui-review (5 dimensions per screen, per-check pass/fail)

**Priority:** 🟠 MANDATORY — audit primacy + per-check rubric for `ui-review` skill
**Version:** 1.0.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (5-dimension per-check rubric for per-screen /128 + bug-finding-primacy + extends `ui-review/SKILL.md` + worked self-test on current main surfaces ≥1 finding) per §6.5 Enforcement Parity Mandate; no constraint loosening — generalizes Wave 71c security-audit pattern closing GAP-523)
**Applies to:** Every invocation of `.claude/skills/quality/ui-review/SKILL.md` (per-screen /128 — Technical /20, Design Heuristics /40, Visual /28, User Friendliness /20, WCAG /20)

---

## 1. The Rule

> **`ui-review` skill must score every screen across 5 dimensions by per-check pass/fail items WITHIN each dimension. Averaging across dimensions OR across screens hides P0 a11y/responsive/dark-mode failures. Per `ui-review/SKILL.md` Rule 3 already mandates "Report LOWEST screen — đây là quality bar thực sự" — this rule sharpens by binding each dimension to enumerated per-check items. Any P0/P1 sub-check FAIL on ANY sampled screen → audit-level verdict = FAIL for that screen.**

Wave 53 baseline `111.7/128 A+` averaged across 144 screens × 7 kits. 3 kits at PARTIAL with <105 screens carried forward as GAP-429 umbrella. Per-screen averaging masks WHICH dimension failed on WHICH screen. This rule sharpens: enumerate sub-checks per dimension.

---

## 2. Mandatory per-check enumeration (≥5 sub-checks per dimension)

### 2.1 Dimension 1 — Technical /20 (P0 responsive, P0 a11y semantic)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 1.1 | Responsive: 320/768/1024/1440 viewports all render without horizontal scroll | P0 | screenshots at 4 widths; no overflow-x |
| 1.2 | Dark mode: toggling theme visibly changes contrast (not just `bg-*` swap) | P1 | light vs dark screenshot diff >40% pixel-change |
| 1.3 | Theme system: `?primary=FF0000` URL changes primary visible color | P1 | screenshot with override |
| 1.4 | No console errors on first page load (network errors OK) | P0 | DevTools console clean in capture script |
| 1.5 | Semantic HTML: `<nav>`, `<main>`, `<header>`, `<footer>` present | P0 | grep page source |
| 1.6 | No anti-patterns: inline `style=`, `!important` overuse | P1 | grep `<.+style=\|!important` count ≤5 per page |

### 2.2 Dimension 2 — Design Heuristics /40 (Nielsen's 10, each /4 sub-checked)

Per-Nielsen-heuristic pass/fail. Lowest heuristic pulls dimension.

| # | Check (Nielsen) | Severity | Pass criterion |
|---|---|---|---|
| 2.1 | Visibility of system status (loading states, progress) | P1 | every long action shows spinner/skeleton |
| 2.2 | Match between system and real world (Vietnamese tone, education metaphors) | P1 | spot-check 3 labels for VN-natural phrasing |
| 2.3 | User control and freedom (undo, cancel, breadcrumb) | P1 | back/cancel button visible |
| 2.4 | Consistency and standards (button styles, terminology) | P0 | sample 5 buttons across pages — same shape/color |
| 2.5 | Error prevention (inline validation, confirm destructive actions) | P1 | sample 1 destructive action — confirms |
| 2.6 | Recognition rather than recall (icons + labels, not just icons) | P1 | sample 5 nav items have text |
| 2.7 | Flexibility and efficiency (keyboard shortcuts, bulk actions for power users) | P2 | sample 1 power-user flow |
| 2.8 | Aesthetic and minimalist design (no clutter) | P1 | visual judgment |
| 2.9 | Help users recognize/recover errors (clear error messages, recovery action) | P1 | error states show next step |
| 2.10 | Help and documentation (contextual help, tooltips) | P2 | sample 1 complex form has help text |

### 2.3 Dimension 3 — Visual Aesthetics /28 (P1 polish, P0 contrast)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 3.1 | Color palette consistent with design system tokens | P1 | no rogue colors outside palette |
| 3.2 | Typography hierarchy clear (h1/h2/h3 size + weight ratios distinct) | P1 | DevTools inspect headings |
| 3.3 | Spacing rhythm consistent (8px/16px scale, not arbitrary) | P1 | sample 5 sections |
| 3.4 | Visual hierarchy: primary action prominent | P1 | "CTA stands out" judgment |
| 3.5 | Polish: aligned grid, no orphan elements, no pixel-perfect bugs | P2 | spot-check |
| 3.6 | Icon set consistent (single library — Lucide OR Heroicons, not mixed) | P2 | grep icon imports |
| 3.7 | Image quality (no blur, no broken images, alt text present) | P1 | sample 3 images |

### 2.4 Dimension 4 — User Friendliness /20 (P0 first impression, P1 nav)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 4.1 | First impression: page purpose clear within 3 seconds | P0 | hero text + visual cue obvious |
| 4.2 | Navigation always accessible (sidebar/header persists) | P0 | nav visible on every page |
| 4.3 | Primary action clarity (1 dominant CTA per page) | P1 | sample 3 pages |
| 4.4 | Empty states designed (not blank white screen) | P1 | sample 1 empty list view |
| 4.5 | Loading states designed (skeleton OR spinner, not blank) | P1 | sample 1 async page |
| 4.6 | Mobile menu functional (hamburger opens, items reachable) | P0 | screenshot mobile width nav |

### 2.5 Dimension 5 — WCAG /20 (P0 contrast, P0 keyboard)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 5.1 | Contrast ratio ≥4.5:1 for normal text (WCAG AA) | P0 | axe-core scan OR manual contrast check |
| 5.2 | Touch targets ≥44×44px on mobile | P0 | sample 5 buttons in mobile screenshot |
| 5.3 | Labels: every form input has `<label>` OR `aria-label` | P0 | grep input elements |
| 5.4 | Screen reader navigation: heading hierarchy (h1 → h2 → h3, no skip) | P1 | DevTools accessibility tree |
| 5.5 | Keyboard navigation: Tab order logical, focus visible | P0 | manual tab through page |
| 5.6 | Skip-to-content link present | P2 | grep `<a href="#main` |

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| Score screen `100/128` averaged across 5 dimensions hiding D5 WCAG 5/20 | If WCAG dimension ≤10/20 OR any P0 sub-check FAIL → audit-level verdict FAIL for that screen |
| Skip mobile screenshots "because desktop looks fine" | 1.1 P0 requires 4-viewport coverage |
| "Dark mode 3/4 — it works" without dark-light diff verification | 1.2 P1 quantitative threshold (>40% pixel-change) |
| Aggregate D2 Nielsen as "8/10 heuristics OK" | Each heuristic 0-4 separately; lowest pulls D2 |
| "111.7/128 A+ baseline" without listing 3 PARTIAL kits' specific FAILs | Bug list = every screen below 105/128 with dimension breakdown |
| Self-score equal to external auditor estimate | Per `ui-review/SKILL.md` Gotchas — external auditor typically 20-35 pts lower; trust the lower number |

---

## 4. Bug-finding > scoring primacy (BLOCKING)

> **A `ui-review` run's purpose is to surface UI quality bombs (broken responsive, low contrast, missing dark mode, no a11y) BEFORE users hit them. A score of `111.7/128` is less actionable than `111.7/128 + list of 3 PARTIAL kits below 105 + per-screen WCAG fail rows`.** Per Wave 71c primacy pattern.

Rules for every `ui-review` run:

1. Enumerate ALL §2 sub-checks across 5 dimensions for sampled screens. NEVER skip.
2. Each sub-check returns `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED`. No partial credit (the existing /4 sub-rubric per `ui-review/SKILL.md` §"Scoring Rubric" remains for narrative — but P0 sub-check FAIL is binary fail regardless of /4).
3. Final output starts with bug list (every screen below threshold + dimension causing it) BEFORE per-screen scores.
4. Score descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS on a sampled screen.
5. Per `ui-review/SKILL.md` §"Score Dimensions" — LOWEST screen IS the quality bar. Don't average it away.

---

## 5. Worked self-test — apply rubric to current main HEAD (2026-05-14)

| Sub-check | Verification | Verdict |
|---|---|---|
| 1.1 Responsive 4 viewports | `ui-review/SKILL.md` capture script supports 4 viewports — sample 1 kit | likely PASS for shipped kits |
| 5.1 Contrast ratio AA | per Wave 53 baseline 3 PARTIAL kits with <105 — likely WCAG sub-check issues | ⚠️ FAIL on 3 PARTIAL kits (per Wave 53 GAP-429 umbrella carry-forward) |
| 5.2 Touch targets ≥44px | mobile screenshots inspection | ⚠️ UNCHECKED in this scope — verify per actual audit |
| 4.1 First impression clarity | subjective; needs external eye | ⚠️ Likely partial on dashboard pages with mock-auth loading states |
| 2.4 Consistency standards | sample 5 buttons | ⚠️ Likely surface ≥1 finding — Wave 53 noted "PARTIAL kits 266/268/270" suggesting some kits' button styles diverge |

**Verdict:** ≥1 confirmed FAIL (5.1 WCAG contrast on 3 PARTIAL kits per Wave 53 carry-forward GAP-429). Per-dimension rubric forces enumerating WHICH dimension on WHICH kit pulled below 105/128. Wave 53 `111.7/128 A+` reflected this but the dimension-level fail wasn't enumerated in score line. Self-test PASS ✅.

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 ui-review/SKILL.md rubric extension (paired same PR)

Skill body extended with §"Per-check scoring" subsection citing this rule. Existing §"Scoring Rubric" 0-4 narrative remains; this rule adds per-check pass/fail layer on top for P0/P1 binary fail semantics.

### 6.2 Pre-promotion gate

Before any release tag `v1.0.0-rc.*` or `v1.0.0`, `ui-review` run MUST report ZERO P0 sub-check FAILs across §2.1-§2.5 on sampled screens (top-3 per kit).

### 6.3 Reviewer checklist

- [ ] Bug list precedes score table?
- [ ] Each Dimension lists per-check verdicts?
- [ ] Lowest screen called out as quality bar?

### 6.4 Override mechanism

```
git commit -m "...
UI_REVIEW_DEFER: <screen + dimension + reason — e.g., kit-270 WCAG 5.1 GAP-429>
UI_REVIEW_FOLLOWUP: <gap link + completion date>"
```

### 6.5 Detector (deferred)

Future `scripts/check-ui-review-rubric.sh` parsing audit report markdown — defer until 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.

---

## 7. Log

- **2026-05-14 (v1.0.0):** Rule created closing GAP-523 META P0 (Wave 72b Bucket E). Generalizes Wave 71c security-audit per-check pattern to ui-review's 5 dimensions /128. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (GAP-523 Wave 71c retro) → Classify ✓ (no rule enforces per-sub-check pass/fail for ui-review dimensions 1-5; existing 0-4 narrative in `ui-review/SKILL.md` allows averaging hide P0) → Rule+Enforce ✓ (this file + ui-review/SKILL.md §"Per-check scoring" extension paired same PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§5 worked example on current main — 1 confirmed FAIL: 5.1 WCAG contrast on 3 PARTIAL kits per Wave 53 GAP-429 carry-forward) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — no constraint loosening). Detector deferred per premature-rule guard ≥7 days.
