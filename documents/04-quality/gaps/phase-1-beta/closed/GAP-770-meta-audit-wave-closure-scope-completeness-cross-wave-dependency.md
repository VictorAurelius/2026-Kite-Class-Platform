---
audience: dev
---

# GAP-770 — META audit Wave beta-prep-1 + Wave 79 closure scope-completeness retroactive

**Status:** 🟢 DONE 2026-05-27 (Wave meta-6 Bucket B)
**Priority:** 🟡 P2 (META audit — retroactive enforcement check)
**Domain:** Meta governance
**Found:** 2026-05-27 (Wave 106 RST Mảng A findings F4 + F6 surfacing cross-wave dependency miss pattern)
**Affects:** Trust of `wave-closure-scope-completeness.md` v1.0.0 enforcement — 2 closures shipped status:complete có thể missed Scope-Completeness Reconciliation table cho 2 specific items
**Phase:** phase-1-beta

## Problem

Wave 106 RST Mảng A walk surfaced 2 findings cùng class "cross-wave dependency miss":

| Finding | Source Wave | Pattern |
|---|---|---|
| **F4 (GAP-765)** Beta request POST 201 không có confirmation email | Wave beta-prep-1 | Plan ship beta-request endpoint, không wire confirmation email path |
| **F6 (GAP-767)** /faq route 404 dù `documents/05-guides/user-manual/anonymous/faq.md` đã có | Wave 79 Bucket F1 | Plan ship docs source, không wire FE MDX route consumption |

Pattern: wave plan scope nói "X + Y", actual ship chỉ "X" — without Scope-Completeness Reconciliation table flag NOT-IMPLEMENTED items.

`wave-closure-scope-completeness.md` v1.0.0 (2026-05-18) đã EXIST mandate every closure PR include reconciliation table mapping mọi plan §3 Scope item → ✅DONE / 🟡PARTIAL / ❌NOT-IMPLEMENTED. Nếu rule được enforce strict, 2 closure PRs này should have flagged item missing với follow-up gap.

## Hypothesis

**H1 (most likely):** Closure PRs predate `wave-closure-scope-completeness.md` v1.0.0 (2026-05-18):
- Wave 79 closure: 2026-05-14 (před rule)
- Wave beta-prep-1 closure: 2026-05-25 (post rule) — should have applied
- → H1 explains F6 (grandfathered) but NOT F4

**H2:** Rule shipped but author/reviewer của Wave beta-prep-1 closure không apply strict — missed item OR didn't categorize NOT-IMPLEMENTED.

**H3:** Item was scope-design (intentional defer) but không file follow-up gap → orphan trong scope.

## Proposed audit (defer Đợt 107)

Spawn audit agent (outside-in: read 2 closure PRs + plan files + apply rule v1.0.0 §3 mandate retroactively):

1. **Wave 79 closure PR** — find PR, check whether reconciliation table existed. If grandfathered pre-rule → exempt; else audit retroactive.
2. **Wave beta-prep-1 closure PR** — find PR, check reconciliation table.
   - F4 confirmation email path mentioned trong table?
   - Marked ✅DONE / 🟡PARTIAL / ❌NOT-IMPLEMENTED?
   - Nếu NOT-IMPLEMENTED → follow-up gap filed?
3. **Verdict matrix:**
   - Rule applied correctly → no enforcement gap; F4+F6 are legitimate scope decisions
   - Rule NOT applied → enforcement gap; consider stricter detector

## Action paths

| Audit verdict | Action |
|---|---|
| Rule applied + items flagged with follow-up | Close GAP-770 → cross-wave dependency working as designed |
| Rule applied but items missed flag | File enforcement gap — strengthen rule §5 detector |
| Rule NOT applied (closure PR predates rule OR author miss) | Backfill reconciliation tables retroactive |
| Recurrence ≥3 same class | Consider stricter rule v1.1.0 + automated detector |

## Acceptance Criteria

- [x] Audit agent run trên 2 closure PRs
- [x] Verdict documented `documents/04-quality/audits/meta/2026-05-27-wave-92-79-closure-retroactive.md`
- [x] Decision: enforcement gap OR scope decision OR rule extension needed → **HYBRID: grandfather both closures (predate rule landing) + SHIP detector NOW per recurrence #2 (GAP-774) per `incident-to-rule-pipeline.md` §3.1 condition 2 failure**
- [x] If rule extension → ~~file follow-up gap (rule v1.1.0)~~ — Rule bumped v1.0.0 → v1.0.1 PATCH same PR (Wave meta-6 Bucket B) per `rule-change-process.md` §5 PATCH self-approve

## Log

- **2026-05-27 (Wave meta-6 Bucket B):** Closed DONE per AC all 4 satisfied. Audit `documents/04-quality/audits/meta/2026-05-27-wave-92-79-closure-retroactive.md` shipped với verdict: Wave 79 + Wave 92 closure PRs grandfathered legitimately (predate rule v1.0.0 landing 2026-05-18; Wave 79 = 3 days before; Wave 92 = same-day-as-rule với prospective-only intent). Recurrence #2 confirmed via GAP-774 D4 admin audit log Wave 92 escape (V62/V63 schema + 3 BETA_REQUEST_APPROVE rows populated but NO Controller + NO FE page = orphan class same as Wave 87/88 CF DNS cutover recurrence #1). Per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions check, recurrence-count ≥2 confirmed → condition 2 fails → SHIP detector NOW (Stage 3 hard requirement). Detector shipped same PR: `scripts/check-wave-closure-completeness.sh` (95 LOC bash + 2 self-test fixtures PASS) + CI job `wave-closure-completeness` trong `quality-docs.yml` WARN-mode 30-day grace through 2026-06-26. Rule bumped v1.0.0 → v1.0.1 PATCH per `rule-change-process.md` §5 same PR (additive Log entry + §5.3 detector ship). File moved `phase-1-beta/` → `phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0 §3.3.

## Related

- META rule: `.claude/rules/wave-closure-scope-completeness.md` v1.0.0
- Parent findings: GAP-765 (F4 email), GAP-767 (F6 FAQ route)
- Wave plans: `wave-beta-prep-1-*.md`, `wave-2026-05-14-79-*.md` (anonymous user-manual F1)
- Rule audit framework: `.claude/rules/incident-to-rule-pipeline.md` §3.1 (recurrence count threshold for rule extension)
