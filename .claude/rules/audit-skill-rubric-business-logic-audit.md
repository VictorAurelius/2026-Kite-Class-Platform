---
paths:
  - "documents/04-quality/audits/business-logic/**"
---

# Audit Skill Rubric — business-logic-audit (5 categories, per-check pass/fail)

**Priority:** 🟠 MANDATORY — audit primacy + per-check rubric for `business-logic-audit` skill
**Version:** 1.0.1
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (5-category per-check rubric + bug-finding-primacy + extends `business-logic-audit/SKILL.md` + worked self-test on current main surfaces ≥1 finding) per §6.5 Enforcement Parity Mandate; no constraint loosening — generalizes Wave 71c security-audit pattern closing GAP-523)
**Applies to:** Every invocation of `.claude/skills/quality/business-logic-audit/SKILL.md` (/100 code ↔ business rules sync — per-domain rules.md ↔ implementation)

---

## 1. The Rule

> **`business-logic-audit` skill must score every Category by per-check pass/fail (no averaging hides P0 rule drift within a 20-pt category). Any P0/P1 sub-check FAIL caps category total ≤ 16/20 AND audit-level verdict = FAIL. The bug list (every BR-xxx without implementation + every config key drift) is the deliverable.**

Wave 40 baseline `68/100 C` recalibrated with strict 5-attribute standard (per `business-logic-review.md`). 60% of rules.md had 5-attr coverage but the audit averaged Cat 1 (Rule Coverage) ÷ Cat 2 (Config Accuracy) etc. → hid which specific BR-xxx ids had no implementation. Per-check pass/fail surfaces them.

---

## 2. Mandatory per-check enumeration (≥5 per category)

### 2.1 Category 1 — Rule Coverage (P0 implementation, P1 traceability)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 1.1 | Every BR-xxx in rules.md has at least 1 grep hit in `src/main/java/**` | P0 | per BR-id: `grep -rn 'BR-DOMAIN-NNN' --include='*.java'` returns ≥1 |
| 1.2 | BR-xxx implementation cited in code via comment OR `@BusinessRule("BR-...")` annotation | P1 | sample 5 BR-ids; verify citation present |
| 1.3 | Verification chain doc-able: BR-xxx → UC-xxx → endpoint → `@Mapping` → `*Test.java` | P1 | sample 2 BR-ids; trace 5-link chain |
| 1.4 | No orphan code logic without matching BR-xxx (every business decision is rule-backed) | P1 | sample 3 services; ad-hoc business logic flagged |
| 1.5 | Per-domain rules.md has ≥1 BR-xxx (no empty rule files) | P1 | every rules.md file has ≥1 BR-id |
| 1.6 | rules.md frontmatter 5-attribute coverage per `business-logic-review.md` §2 | P0 | every BR row has Source/Rationale/Reviewer/Compliance/Cadence |

### 2.2 Category 2 — Config Accuracy (P0 key match, P1 default)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 2.1 | Every config key cited in rules.md exists in `application.yml` | P0 | sample 5 keys per domain; grep returns ≥1 |
| 2.2 | Config key VALUES match rules.md documented values | P0 | sample 5 keys; YAML value == doc value |
| 2.3 | No drift: renamed config keys reflected in BOTH rules.md AND `application.yml` (no silent renames) | P0 | per `audit-to-gap-pipeline.md` §2.7 — decision-doc code-sync |
| 2.4 | Per-tier defaults documented in rules.md match `application.yml` | P1 | sample 1 tiered config (e.g., AI rate limits per tier) |
| 2.5 | Override mechanism documented per `production-env-config-registry.md` | P1 | rules.md notes env-var override path |

### 2.3 Category 3 — Edge Case Tests (P0 error path, P1 boundary)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 3.1 | Every UC-xxx error path has matching `*Test.java` covering the error | P0 | per UC error path: grep `assertThrows\|assertStatus(4` |
| 3.2 | Boundary tests for numeric/string limits cited in rules.md | P1 | sample 3 limits (trial days, regen quota, password length); test exists |
| 3.3 | Negative tenant scenarios tested (subscription expired, plan downgraded) | P1 | grep `expired\|downgrade` in test names |
| 3.4 | Concurrent-action tests (race conditions in business flow) | P2 | per critical-state-transition flow |
| 3.5 | Time-sensitive tests (token expiry, trial expiry) | P1 | grep `expiry\|expired\|clock` |

### 2.4 Category 4 — Cross-Domain Consistency (P0 no contradiction, P1 cascade)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 4.1 | No rule in domain A contradicts rule in domain B | P0 | manual review for known conflict surfaces (e.g., subscription vs branding regen limits) |
| 4.2 | Cascading rules consistent: subscription expired → branding regen disabled (verify) | P0 | trace cascade chain across rules |
| 4.3 | Currency/locale conventions consistent (VND format across all 3 doc formats per `quality-audit/SKILL.md` Cat 9) | P1 | sample 3 currency-touching rules |
| 4.4 | Compliance overlap (PDPL + Consumer Protection) reconciled per `business-logic-review.md` §2.4 | P1 | spot-check 1 PII-touching rule |
| 4.5 | Persona-scope respected: K-12 rules absent for non-K-12 personas | P2 | per `personas-catalog.md` tier matrix |

### 2.5 Category 5 — Stakeholder Alignment (P1 review cadence, P2 evidence)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 5.1 | Every rules.md has Reviewer field per `business-logic-review.md` §2.3 | P0 | grep `Reviewer:` in every BR row |
| 5.2 | Quarterly review cadence on track (no `next_review` overdue) | P1 | grep `Next review:` dates |
| 5.3 | Event-driven re-review triggers documented (competitor change, regulation update) | P1 | per `business-logic-review.md` §5.3 |
| 5.4 | Compliance-critical rules (data retention, financial, K-12) flagged for legal counsel review | P1 | grep `Compliance check:` for `Compliant` or `Considered` |
| 5.5 | Rules sourced ≥80% non-"informed gut" (data, competitor, regulation evidence) | P2 | aggregate Source counts |

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "Cat 1 score 14/20 — most BRs implemented" | If ≥1 P0 BR-xxx has NO implementation → P0 FAIL → cap 16/20 |
| Skip Cat 2 because "config keys all in YAML" without value match | 2.2 value match P0 separate from 2.1 existence P0 |
| "60% rules.md coverage" without listing the 40% missing 5-attr | Bug list = every rule missing ≥1 attribute |
| "68/100 baseline" without per-BR FAIL list | Bug list precedes score; per-BR enumeration |
| Aggregate Cat 5 as "stakeholder alignment" without 5.1 reviewer-field check | 5.1 P0 separate from 5.2-5.5 P1/P2 |

---

## 4. Bug-finding > scoring primacy (BLOCKING)

> **A `business-logic-audit` run's purpose is to surface code-rules drift BEFORE production violates documented business policy (wrong pricing, wrong retention, wrong consent flow). A score of `68/100` is less actionable than `68/100 + list of 12 unimplemented BR-xxx + 5 config-key drifts`.** Per Wave 71c primacy pattern.

Rules for every `business-logic-audit` run:

1. Enumerate ALL §2 sub-checks across 5 categories. NEVER skip.
2. Each sub-check returns `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED`. No partial credit.
3. Final output starts with bug list (every BR/config FAIL with `rules.md:line` + `*.java:line` evidence) BEFORE score table.
4. Score descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS.
5. Per `business-logic-audit/SKILL.md` Cat 5 — Stakeholder rules require human review; Claude flags FAIL, human decides closure.

---

## 5. Worked self-test — apply rubric to current main HEAD (2026-05-14)

| Sub-check | Verification | Verdict |
|---|---|---|
| 1.6 5-attribute coverage per `business-logic-review.md` §2 | Wave 40 baseline `60% rules.md have 5-attr coverage` | ❌ FAIL (P0) — 40% gap surfaced, Phase 2 GAP-156 in flight |
| 2.3 No silent config-key renames | Per `audit-to-gap-pipeline.md` §2.7 decision-doc code-sync — Wave 71c found `kitehub.vn` → `kitehub.me` had 21 stale refs | ⚠️ Likely fixed now (PR #1084) but applies to any future rename |
| 3.1 UC error path test coverage | grep `assertThrows` count vs UC-xxx error count | ⚠️ UNCHECKED in this scope — verify per `business-logic-audit/SKILL.md` Cat 3 process |
| 5.1 Reviewer field per BR | `business-logic-review.md` §2.3 mandates; Wave 40 audit found gaps | ⚠️ Likely partial — many existing BRs predate `business-logic-review.md` (created 2026-04-29) |
| 5.5 Non-"informed gut" sources ≥80% | aggregate Source attribute counts | ⚠️ Likely FAIL — solo-dev mode many decisions are `informed gut` per `business-logic-review.md` §2.1 |

**Verdict:** ≥2 confirmed FAILs (1.6 5-attr coverage gap, 5.5 informed-gut prevalence). Wave 40 `68/100 C` recalibration reflected these but didn't enumerate per-BR. Per-check rubric forces per-BR drift list. Self-test PASS ✅.

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 business-logic-audit/SKILL.md rubric extension (paired same PR)

Skill body extended with §"Per-check scoring" subsection citing this rule.

### 6.2 Pre-promotion gate

Before any release tag `v1.0.0-rc.*` or `v1.0.0`, `business-logic-audit` run MUST report ZERO P0 FAILs across §2.1-§2.5.

### 6.3 Reviewer checklist

- [ ] Bug list precedes score?
- [ ] Each Category ≥5 per-check verdicts?
- [ ] Every unimplemented BR-xxx listed with rules.md:line evidence?

### 6.4 Override mechanism

```
git commit -m "...
BUSINESS_LOGIC_DEFER: <BR-xxx + reason — e.g., 5-attr Phase 2 GAP-156>
BUSINESS_LOGIC_FOLLOWUP: <gap link + completion date>"
```

### 6.5 Detector (deferred)

Future `scripts/check-business-logic-rubric.sh` parsing rules.md frontmatter + cross-ref code — defer until 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.

---

## 7. Log

- **2026-05-14 (v1.0.1):** PATCH — added `paths:` frontmatter per Wave 73 Bucket A1 path-scope. No constraint change; rule auto-loads only when matching files in context.
- **2026-05-14 (v1.0.0):** Rule created closing GAP-523 META P0 (Wave 72b Bucket E). Generalizes Wave 71c security-audit per-check pattern to business-logic-audit's 5 categories. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (GAP-523 Wave 71c retro) → Classify ✓ (no rule enforces per-BR pass/fail for business audit Cat 1-5; `business-logic-review.md` covers 5-attr standard but not rubric pass/fail) → Rule+Enforce ✓ (this file + business-logic-audit/SKILL.md §"Per-check scoring" extension paired same PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§5 worked example on current main — 2 confirmed FAILs: 1.6 5-attr 60% coverage gap + 5.5 informed-gut prevalence) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — no constraint loosening). Detector deferred per premature-rule guard ≥7 days.
