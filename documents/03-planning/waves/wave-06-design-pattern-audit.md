---
title: Wave 6 — Design Pattern Audit + Hotspot Refactor
status: draft
created: 2026-04-25
updated: 2026-04-25
waves: [6]
gaps: [GAP-046]
---

# Wave 6 — Design Pattern Audit + Hotspot Refactor

**Owner:** solo-dev
**Status:** 🟡 PLANNED (Sub-PR 6.0a is this planning doc)
**Closes:** GAP-046 (currently 🟡 PARTIAL)
**Predecessor:** Wave 5 (DONE 2026-04-25, document generation)
**Methodology:** Superpowers per `CLAUDE.md` (Brainstorm → Task Breakdown → TDD → Implementation → Code Review)

---

## 1. Brainstorm (per `core/brainstorming-methodology.md`)

### 1.1 Question Assumptions

| Question | Answer |
|----------|--------|
| **What problem?** | GAP-046 has rules + Outbox infra DONE but (a) no audit skill enforces design-patterns.md §3 anti-pattern list, (b) ≥1 service over 500-LOC God-Service threshold (TrialToPaidService 546 LOC), (c) ~95 status-switch sites untriaged for State Pattern candidates |
| **Why now?** | After Wave 5 closure, GAP-046 is #1 in Block-GA queue per `meta-gap-priority.md` (meta beats feature). Force-multiplier: every future PR review picks up better hygiene if skill ships |
| **Who uses?** | (a) PR review checklist (per `design-patterns.md` §4 — currently manual); (b) periodic quality refresh; (c) future Wave 7+ refactor planning |
| **Success criteria** | (i) Skill produces actionable hotspot list (file path + LOC + violated pattern); (ii) baseline report saved + 1-3 hotspot gaps filed; (iii) ≥1 refactor PR using skill output drops a service below 500-LOC threshold |
| **Constraints** | Solo-dev; respect `feedback_audit_grep_scope.md` (multi-module greps); respect `audit-to-gap-pipeline.md` Step 2.5 (state-check before each new gap); skill body <100 lines per `skill-conventions.md` §2 |

### 1.2 Trade-offs — 3 options for the audit skill

| Criterion (weight) | A) Standalone skill | B) Extend quality-audit | C) Hybrid (light + standalone) |
|--------------------|:-------------------:|:----------------------:|:------------------------------:|
| Maintainability (3x) | ⭐⭐⭐ — focused, one place | ⭐⭐ — quality-audit already 10 categories | ⭐ — two places to update on rule change |
| Discoverability (2x) | ⭐⭐ — separate invocation | ⭐⭐⭐ — folded into existing audit | ⭐⭐⭐ — light hint surfaces in quality, deep on demand |
| Focus depth (3x) | ⭐⭐⭐ — 17 patterns × 9 services warrants depth | ⭐⭐ — would dilute quality-audit | ⭐⭐⭐ |
| Audit time (2x) | ⭐⭐⭐ — only when invoked | ⭐⭐ — always runs | ⭐⭐ |
| **Weighted total** | **31** | **23** | **25** |

### 1.3 Decision

**Option A — standalone `quality/design-pattern-audit/SKILL.md`.**

Rationale:
- Mirrors existing `business-logic-audit` precedent (separate skill, not folded into quality-audit)
- `quality-audit/SKILL.md` already 10 categories; adding 11th would push past `skill-conventions.md` §2 "<100 lines" guideline
- Pattern check is its own discipline (17 patterns) — depth > breadth here

**Rejected:**
- B (extend quality-audit) — bloat risk; 11th category dilutes 10 existing
- C (hybrid) — maintenance overhead doubles for marginal discoverability gain

---

## 2. Task Breakdown (per `core/task-breakdown-guide.md`)

### 2.1 Sub-PR ledger

| # | Sub-PR | Scope | Effort | TDD strategy |
|---|--------|-------|:------:|--------------|
| 1 | **6.0a** (this PR) | Wave plan + brainstorm + breakdown | S (30 min) | N/A — planning artifact |
| 2 | 6.0b | Skill scaffolding (SKILL.md + 2 reference docs + skills index update) | S-M (1-2h) | Dry-run on known hotspot (TrialToPaidService = should FLAG; small clean POJO = should PASS) |
| 3 | 6.1 | Baseline audit run → report + 1-3 hotspot gap files | S (30-60 min) | Audit output reviewed against hand-verified TrialToPaidService LOC |
| 4 | 6.2 | Refactor hotspot #1 — TrialToPaidService Facade extract | M (2-4h) | Classical Java TDD per `tdd-enforcement.md` — write tests for extracted services FIRST |
| 5 | 6.3 | Refactor hotspot #2 — InstanceService (496 LOC at threshold) | M (2-4h) | Same TDD pattern |
| 6 | 6.4 *(optional)* | Status-switch → State Pattern for 1 critical entity (instance lifecycle) | M-L (3-5h) | TDD per state transition |
| 7 | 6.5 | Wave closure: GAP-046 → 🟢 DONE if all AC checked, ROADMAP updates, post-wave audit suite refresh per `post-wave-audit-mandate.md` | S (30-60 min) | Verify hotspot count delta; re-run skill, check rules.md adherence |

### 2.2 Sub-PR 6.0b detailed task list (5-element per `task-breakdown-guide.md`)

> **State-check first** (per `task-breakdown-guide.md` §"State-Check First" + memory `feedback_gap_state_check_required.md`): GAP-046 already has `Current State` table from Sub-PR 6.0a doc-sync (`b2c37909`). Re-verify on entry to 6.0b — if any of the 4 ✅ items regressed (catalog, rule, Outbox infra, code review checklist), update gap before continuing.

| # | File path | Change | Code sample reference | Verify | Est. |
|---|-----------|--------|----------------------|--------|:----:|
| T1 | `.claude/skills/quality/design-pattern-audit/SKILL.md` | New skill body (<100 lines per `skill-conventions.md` §2) | Adapt `business-logic-audit/SKILL.md` template — frontmatter trigger phrases, 5-step Process, Grep-Scope warning, Context Management | `wc -l` ≤ 100 | 30 min |
| T2 | `.claude/skills/quality/design-pattern-audit/reference/scoring-guide.md` | Detailed rubric per anti-pattern category from `design-patterns.md` §3 | Mirror `business-logic-audit/reference/scoring-guide.md` shape — table of categories with point deductions | Each of §3 ten anti-patterns has scoring entry | 45 min |
| T3 | `.claude/skills/quality/design-pattern-audit/reference/anti-pattern-detectors.md` | Grep + AST hints for hotspot detection (God-Service LOC threshold, status-switch density, Ollama-type leak in domain layer) | Bash one-liners with `--include="*.java"` + multi-module patterns | Manual run on TrialToPaidService FLAGS; clean POJO PASSES | 30 min |
| T4 | `.claude/skills/_README-skills-index.md` | Add `quality/design-pattern-audit` row under "Check & Audit" section | Match shape of existing `quality/business-logic-audit` row | `grep -c design-pattern-audit` returns ≥1 | 5 min |
| T5 | (verification) | Dry-run skill on TrialToPaidService.java + simple POJO | N/A | TrialToPaidService → flagged with God-Service rule + LOC; POJO → no flags | 15 min |

### 2.3 Sub-PR 6.2/6.3 TDD pattern (refactor sub-PRs)

Per `tdd-enforcement.md` + `design-patterns.md` §3.1 (God Service refactor):

```
For each extracted sub-service S:
  RED:    Write failing test asserting S exists with method M, called by Facade
  GREEN:  Implement S minimally to pass
  REFACT: Move logic out of God Service into S; verify integration tests still pass
  REPEAT: Until God Service drops below 500 LOC threshold
```

Acceptance per refactor PR: original service `wc -l` < 500; all existing tests still pass; new sub-service has its own test file with ≥1 isolated test.

---

## 3. Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Skill flags false-positives (over-eager God-Service trigger on legitimately-large legacy services) | Calibrate threshold via manual review of audit baseline before filing gaps; document calibration in `reference/scoring-guide.md` |
| Refactor breaks behavior (TrialToPaidService is subscription-critical) | TDD strict — refactor only with green test suite; integration tests must stay green at every commit |
| GitNexus pilot (GAP-221) lands mid-wave and changes audit approach | Wave 6 stays grep-based; if GitNexus ADOPTed mid-wave, file follow-up gap to migrate skill, do NOT block wave |
| Wave scope creep (start refactoring patterns 6.4 turns into Big Bang) | Sub-PR 6.4 marked OPTIONAL — close wave at 6.3 if effort budget exhausted |
| Status-switch refactor breaks lifecycle invariants | Test each transition explicitly; State Pattern adoption only for entity with clear ≥3 states + invariants worth enforcing |

---

## 4. Out of Scope

- Pattern catalog expansion (already DONE in `ai-branding-design-patterns.md`)
- Rules doc rewrite (already DONE — `.claude/rules/design-patterns.md`)
- DDD Aggregate refactor (Phase 3 in GAP-046 §Phase 3 — Enterprise; defer to post-GA)
- Saga pattern adoption (same — Phase 3, post-GA)
- XState frontend wizard (Phase 4 — defer until wizard refactor justifies cost)
- Strangler Fig migration (Phase 5 — only on v1→v2 ramp, not currently active)

---

## 5. Audit Mandate (per `post-wave-audit-mandate.md`)

Wave 6 changes touch:
- `.claude/skills/**` — meta change, no audit triggered by file pattern
- `kiteclass/kiteclass-core/**` `kitehub/*/src/main/java/**` (refactor sub-PRs 6.2/6.3) — triggers per `audit-gate.py` AUDIT_RULES:
  - **Quality refresh** (mandatory post-wave per §2.3)
  - **Performance audit** if any refactor touches request handler / DB query path

Sub-PR 6.5 wave closure runs the audit suite. 3-day window enforces via `audit-gate.py` block.

---

## 6. Wave 6 vs PowerPoint trade-off (alternative wave candidate)

Per Sub-PR 5.6b (#532) closure note, Wave 6 has 2 candidates:
- **A — Design Pattern Audit + Refactor** (this plan) — Meta-priority, force multiplier
- **B — PowerPoint format** — Feature, scope-locked from Wave 5

`meta-gap-priority.md` §3 priority matrix puts Meta-P0 above Feature-P0; this wave (A) wins on priority. PowerPoint deferred again — viable Canva/Slides alternative reduces urgency.

---

## 7. Log

- 2026-04-25 — Sub-PR 6.0a (this doc) drafted via Superpowers methodology after user confirmed Path B in session continuation. Brainstorm done in session step C earlier; this doc consolidates + adds Trade-off matrix + Task Breakdown per templates.
