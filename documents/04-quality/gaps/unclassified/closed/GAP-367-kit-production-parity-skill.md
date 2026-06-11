# GAP-367: Skill `quality/kit-production-parity` — extend `ui-review/SKILL.md` with 4-layer parity check

**Status:** 🟢 DONE
**Priority:** 🟠 P1 Meta (skill gap; force-multiplies every Track 2 port review)
**Domain:** Meta / Skills / Frontend review
**Found:** 2026-05-06 (Wave 22 follow-up audit — coordinator review of FE review standards)
**Affects:** Every Track 2 port PR (GAP-266..280) and every future production FE component matching a kit screen

## Problem

User asked at Wave 22: "Việc rebuild lại code frontend theo UI kits thì sẽ được review theo tiêu chuẩn như thế nào?"

Audit found:

| Layer review | Skill exists? |
|---|---|
| /128 visual rubric (production standalone) | ✅ `quality/ui-review/SKILL.md` |
| /128 visual rubric (HTML kit prototype) | ✅ `quality/ui-review-prototype/SKILL.md` |
| Visual parity (pixel-diff prod vs kit) | ❌ |
| AC traceability (kit dossier AC → production E2E test) | ❌ |
| WCAG real measurement (axe-core / lighthouse-ci) | ❌ — separate gap GAP-352 |
| Component-spec parity (production component matches `dossier/04-component-gaps.md` G* spec) | ❌ |

Track 2 port AC currently uses generic "All N screens ≥110/128" — but /128 is holistic, doesn't enforce kit-parity. Production could score 110 with completely different UX vs kit. Without parity skill, port reviewers manually eyeball — inconsistent + slow.

## Current State (verified 2026-05-06)

```bash
ls .claude/skills/quality/ | grep -i "ui\|parity\|kit"
# → ui-review/, ui-review-prototype/
# → no kit-production-parity, no kit-component-parity
```

GAP-355 (visual regression drift policy) tracks one slice (visual-diff baseline tooling). This gap covers the skill layer that wraps multiple parity dimensions into a reusable review.

## Proposed Fix

Two options — discuss with user before pick:

**Option A — Extend existing `quality/ui-review/SKILL.md`** with `--mode=kit-parity` flag:
- Pros: single skill, one mental model, less skill-folder sprawl
- Cons: ui-review already has 14 screens / 4 dimensions / /128 rubric — adding parity makes it a 600-line skill

**Option B — NEW skill `quality/kit-production-parity/SKILL.md`** (recommended):
- Pros: clear separation; parity is a different question than score
- Cons: 2 skills to maintain

Recommend **Option B**. Skill scope:

**Layer 1 — Visual parity** (semi-automated):
- Capture production screen screenshot (Playwright)
- Compare against kit HTML rendered screenshot (also Playwright)
- Pixel-diff via Resemblejs / Pixelmatch — output diff %
- Threshold: ≤15% diff = pass, 15-30% = warn, >30% = fail (configurable)
- Tooling: Percy / Chromatic / Playwright + custom diff script

**Layer 2 — AC traceability** (manual + grep):
- Read kit `dossier/10-acceptance-criteria.md` for the screen
- For each AC item, grep production E2E tests for matching reference (AC-* ID or descriptive text)
- Output: AC-coverage-% (target: 100% wired, missing items listed)

**Layer 3 — Component-spec parity** (manual):
- Read kit `dossier/04-component-gaps.md` G* spec for components used in screen
- Compare production component (e.g., `<G2-Card>`) interface (props, types, slots) to kit spec
- Output: PASS / WARN per component (free text issue summary)

**Layer 4 — WCAG measurement** (delegate to GAP-352):
- Run axe-core / lighthouse-ci on production screen
- Output: contrast ratios, ARIA issues, keyboard nav, etc.
- This layer reuses GAP-352 axe-core CI gate; skill calls it via tooling boundary

**Output report format** (markdown to `documents/04-quality/audits/parity/YYYY-MM-DD-<screen>.md`):
- Per-layer score / verdict
- Issues itemized
- Diff screenshots embedded
- Recommendation: PASS / NEEDS POLISH / NEEDS REWORK

## Acceptance Criteria

- [x] `.claude/skills/quality/kit-production-parity/SKILL.md` created (Option B — new skill, recommended)
- [x] 4-layer review process documented — mapped to V-model (要件定義 / 基本設計-visual+WCAG / 詳細設計-AC / コンポーネント設計-component) per `design-layer-coverage.md` §2
- [x] Reference doc `reference/parity-rubric.md` with per-layer thresholds (visual diff % / AC coverage % / component-spec match) + worked example + report template
- [x] Skill activates on: Track 2 port PR, kit-derived component PR, port retro audit, production→kit back-port — §When to use
- [x] Self-test: applied to existing kit-port pair (`kiteclass-public` ↔ production public pages, back-port direction #2326) — §Self-test
- [x] `output-review-mandate.md` §3 row "Production FE port from kit" cites this skill as Process
- [x] PR template integration: FE kit-parity checkbox references this skill for parity audit
- [x] Bidirectional support (kit→prod port + prod→kit back-port) — covers production-led case (Wave 78 public, #2326)

## Out-of-scope (tracked separately)

| Item | Where |
| Playwright screenshot capture + Pixelmatch diff executable script | Layer-2 visual procedure documented in skill + `reference/parity-rubric.md` (manual browser-walk fallback when no harness); automated pixel-diff tooling pairs with GAP-355 (visual regression baseline tooling) when Playwright harness lands |
| WCAG axe-core / lighthouse-ci real measurement | Delegated to GAP-352 (Layer 2 contrast = self-measure pointer until that lands) |

## Related

- Sister gap: **GAP-366** (frontend-standards.md kit-as-source-of-truth — manual standard layer)
- Visual regression: GAP-355 (drift policy — Phase 1 baseline tooling)
- WCAG audit: GAP-352 (axe-core / lighthouse-ci — delegated Layer 4)
- Component gaps: kit `dossier/04-component-gaps.md` (Layer 3 input)
- AC source: kit `dossier/10-acceptance-criteria.md` (Layer 2 input)
- Existing skills: `quality/ui-review/SKILL.md`, `quality/ui-review-prototype/SKILL.md`

## Why Meta-P1

- Meta gap; force-multiplies every Track 2 port + future kit-derived component
- P1 because Track 2 Phase 2 hasn't started — closing this before Phase 2 = ship right
- Per `meta-gap-priority.md` §3 — Meta-P1 above Feature-P0

## Effort estimate

~16-24h (skill design + Playwright tooling + pixelmatch wiring + reference doc + self-test). Pair with GAP-366 (manual standard) as 2-bucket meta wave-pack.

## Log

- **2026-05-06:** Filed at Wave 22 closure per user question "rebuild FE theo UI kits review theo tiêu chuẩn nào?". Coordinator audit confirmed gap. Sister to GAP-366 (manual standard companion). Recommend close before Track 2 Phase 2 wave-pack kickoff.
- **2026-06-11 (Wave ui-kits-100 Bucket F):** Status → 🟢 DONE. Shipped new skill `.claude/skills/quality/kit-production-parity/SKILL.md` (Option B) + `reference/parity-rubric.md`. 4 parity dimensions mapped onto the V-model 4 layers per `design-layer-coverage.md` §2 (要件定義 requirements / 基本設計 visual+token+WCAG / 詳細設計 AC traceability+state-machine / コンポーネント設計 component-spec). **Bidirectional** per lesson `kiteclass-public` #2326: kit→production port parity AND production→kit back-port parity (production can lead, e.g. Wave 78 public pages). GAP-1223 lesson encoded — read `_shared/colors_and_type.css` tokens before Layer-2 visual verdict. Index row added to `_README-skills-index.md`; `check-skill-conventions.sh` PASS (FAIL 0; folder name not -audit/-review so no eval-fixtures requirement). Enforcement: `output-review-mandate.md` §3 row "Production FE port from kit" cites this skill as Process; PR template FE kit-parity checkbox references it. Playwright/Pixelmatch executable tooling + WCAG axe-core deferred out-of-scope (→ GAP-355 / GAP-352). Sister GAP-366 (manual standard §3.1) DONE same wave. git mv → `unclassified/closed/`.
