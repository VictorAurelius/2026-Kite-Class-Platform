# GAP-152: Execute Persona Review Round 1 — Role-Play 4 Tier 1 Personas

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — validates whether current system meets Tier 1 needs)
**Domain:** Business / Persona / Review
**Found:** 2026-04-20 (user raised: "phải nhập vai đúng các đối tượng này để thực hiện review")
**Affects:** GA readiness, backlog prioritization, feature roadmap calibration
**Blocked by:** GAP-151 (tenant AC) + GAP-153 (secondary persona AC for Student/Parent in P5 review)

## Problem

GAP-050 (persona-based review process) có framework + skill, nhưng **CHƯA execute any actual review report**. `documents/00-brd/persona-reviews/` directory chưa tồn tại.

Result:
- Coverage % trong catalog (60% / 75% / 65% / 30%) là **estimate**, không phải measurement
- 14+ "Critical Missing Features" (GAP-051..064) derived từ **quick scan 2026-04-14**, không phải deep role-play
- **Chưa biết còn case nào tương tự "xlsx import broken for K-12"** mà chưa surface

User's requirement:
> "phải nhập vai đúng các đối tượng này để thực hiện review ... bộ tiêu chí của họ và họ sẽ review nghiệp vụ của hệ thống xem có đúng tiêu chí chưa"

## Root Cause

GAP-050 AC liệt kê "Initial gaps filed" as deliverable but:
- Filing gaps ≠ reviewing system against persona
- Quick scan 2026-04-14 found 14 gaps but didn't formalize scoring per persona
- No scheduled follow-up after Wave 1 deferred — review debt accumulated

## Proposed Fix

### Scope: 4 Tier 1 personas × 1 review report each

For each of P1/P2/P3/P5:

1. **Load AC doc** (from GAP-151 deliverable)
2. **Role-play** real-world scale:
   - P1: 1 teacher, 15 students, 3 courses (realistic solo tutor)
   - P2: 2 teachers + 1 owner, 60 students, 8 classes
   - P3: 12 teachers, 300 students, 30 classes, 2 admins
   - P5: 45 teachers, 1200 students, 40 classes, principal + 2 VPs + 3 dept heads + 5 staff + 1200 parents
3. **Walk through journey** — discovery → signup → provisioning → daily ops → financial → communication → edge case → termination
4. **Score each AC** — PASS / PARTIAL / FAIL with evidence (UI screenshot / API call / missing endpoint)
5. **File NEW gaps** for FAIL cases not already tracked (follow `audit-to-gap-pipeline.md`)
6. **Output review report** to `documents/00-brd/persona-reviews/P<N>-<name>-round-1-2026-MM-DD.md`

### Report template

```markdown
---
title: Persona Review — P<N> <Name> — Round 1
status: draft | approved
created: 2026-MM-DD
reviewer: <name/role>
persona: P<N>
scale: <numbers>
ac_doc_version: <hash/date of persona-criteria/P<N>-*.md used>
---

# Review — P<N> <Name>

## Summary

- Total ACs: <N>
- PASS: <X> (<X%>)
- PARTIAL: <Y> (<Y%>)
- FAIL: <Z> (<Z%>)
- Overall coverage: <score>/100
- Gaps filed: <list of NEW gap IDs>

## Detailed Results

### 1. Onboarding

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 | PASS | Screenshot: onboarding-wizard.png | — |
| AC-ONBOARD-002 | FAIL | No bulk import UI; tested at /settings/users | GAP-051 |
| ... |

### 2. Daily Operations

...

## Critical Findings

<Top 3-5 most damaging gaps for this persona>

## Recommendations

<Priority reordering suggestion based on findings>

## Log
- 2026-MM-DD — Review completed by <reviewer>
```

### Parallel strategy

4 agents could work in parallel (1 per persona) — but each agent needs:
- Pre-assigned GAP number range (for new gap files discovered)
- Read access to AC doc (output of GAP-151)
- Write access to own persona report file

**Sequencing decision:** execute **sequentially** (1 persona at a time) for this gap because:
- First review calibrates methodology
- Patterns may emerge that refine AC for subsequent reviews
- 1 session = 1-2 personas feasible manually

If performance critical → split into sub-gaps GAP-152a/b/c/d after first persona's calibration.

## Acceptance Criteria

- [ ] `documents/00-brd/persona-reviews/` directory created with README
- [ ] P1 Solo Teacher review report (round 1) shipped
- [ ] P2 Small Center review report shipped
- [ ] P3 Medium Center review report shipped
- [ ] P5 K-12 School review report shipped (user's priority — expect most findings)
- [ ] Each report: scored per AC, evidence captured, gaps filed for NEW failures
- [ ] New gaps (if any beyond GAP-051..064) created per `audit-to-gap-pipeline.md`
- [ ] Personas catalog "Coverage Review Status" table updated with measured scores (replace estimates)
- [ ] At least 1 finding NOT in current 14 P0/P1 gaps documented (proves review adds value)
- [ ] ROADMAP updated with new gaps from review
- [ ] GAP-050 AC updated to reference this gap's output

## Dependencies

- **Blocked by GAP-151** — tenant AC (P1/P2/P3/P5) template + docs must exist first
- **Blocked by GAP-153** — secondary persona AC (Student-in-P5, Parent-in-P5 critical) must exist for P5 review to be meaningful
- `persona-based-business-review.md` skill (already exists)
- Access to current system (can be local dev stack — screenshots OK as evidence)

## Out of Scope

- **Fixing discovered gaps** — each FAIL goes into backlog per priority order (meta → biz-logic → feature)
- **Tier 2/3 persona reviews** — future gap (GAP-155+)
- **Round 2 reviews** — quarterly cadence, future gaps

## Related

- GAP-050 — parent (review process definition)
- GAP-151 — provides AC docs consumed by this gap
- GAP-051..064 — expected to be confirmed or recalibrated by review findings
- GAP-149 — audit grep scope meta fix (sister meta-gap fixed 2026-04-20, prevents silent false-positives during review)
- Rule: `.claude/rules/meta-gap-priority.md` — business-logic tier

## Log

- 2026-04-20 — Created. User raised: persona reviews chưa execute, xlsx import case show framework works but unused. Scoped to Tier 1 only to bound wave effort.
