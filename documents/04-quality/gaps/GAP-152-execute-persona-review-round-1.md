# GAP-152: Execute Persona Review Round 1 — Role-Play 4 Tier 1 Personas

**Status:** 🟢 DONE 2026-05-04 — Wave 17 SHIPPED (4 review reports + 57 new gaps + ROADMAP + catalog sync)
**Priority:** 🔴 P0 (business-logic tier — validates whether current system meets Tier 1 needs)
**Domain:** Business / Persona / Review
**Found:** 2026-04-20 (user raised: "phải nhập vai đúng các đối tượng này để thực hiện review")
**Affects:** GA readiness, backlog prioritization, feature roadmap calibration
**Closed by:** Wave 17 — 4 review PRs (#745 P2, #747 P3, #748 P5, #749 P1) + closure PR (this)
**Blocked by:** ~~GAP-151~~ (DONE Wave 15, 2026-04-30) + ~~GAP-153~~ (DONE Wave 16, 2026-04-30) — **UNBLOCKED 2026-04-30**, executed Wave 17 2026-05-04

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

- [x] `documents/00-brd/persona-reviews/` directory created with README (Phase 1 PR #739)
- [x] P1 Solo Teacher review report (round 1) shipped — PR #749 (score 36.2/100, 10 gaps GAP-286..295)
- [x] P2 Small Center review report shipped — PR #745 (score 36.8/100, 8 gaps GAP-296..303)
- [x] P3 Medium Center review report shipped — PR #747 (score 9.6/100, 15 gaps GAP-306..320 — full reserved range)
- [x] P5 K-12 School review report shipped (user's priority — expect most findings) — PR #748 (score 8.3/100, 24 gaps GAP-321..344)
- [x] Each report: scored per AC, evidence captured (file path + line + grep results), gaps filed for NEW failures
- [x] New gaps created per `audit-to-gap-pipeline.md` — 57 total NEW gaps in reserved ranges (P1=10, P2=8, P3=15, P5=24)
- [x] Personas catalog "Coverage Review Status" table updated with measured scores (closure PR — this)
- [x] At least 1 finding NOT in current 14 P0/P1 gaps documented — many: parent portal LEGAL mandate (GAP-321), child protection criminal liability (GAP-322), period-attendance K-12 model (GAP-323), 5-tier role hierarchy (GAP-324), MoET license verification (GAP-326), etc.
- [x] ROADMAP updated with new gaps from review (closure PR — this)
- [x] GAP-050 AC updated to reference this gap's output — Wave 17 execution closes GAP-050 §"Initial gaps filed" deliverable expectation

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

- **2026-05-04** — **🟢 DONE** — Wave 17 SHIPPED in single day. Phase 1 (PR #739) created plan + foundation. Phase 2 attempted parallel background → 3/4 agents killed silently (mobile SSH SIGHUP cascade — root cause documented in `feedback_agent_kill_root_cause.md`, fix landed in PR #746 mobile-resilient stack). Phase 2 re-attempted with commit-after-each-file mandate → all 4 agents shipped clean (PRs #745 P2, #747 P3, #748 P5, #749 P1). Closure PR (this) syncs ROADMAP §Status Snapshot + personas-catalog.md "Coverage Review Status" measured scores + flips this gap → DONE per `gap-done-discipline.md`. **Outcomes:**
  - **288 ACs scored** across 4 personas + secondary docs (29 P1, 38 P2, 82 P3, 134 P5)
  - **Coverage scores (measured vs estimated):** P1 36.2/100 (was 60% est), P2 36.8/100 (was 75% est), P3 9.6/100 (was 65% est), P5 8.3/100 (was 30% est) — **all 4 personas significantly LOWER than estimates** — proves estimates were optimistic; review surfaces real readiness state
  - **57 NEW gaps filed** across reserved ranges (P1=10 GAP-286..295, P2=8 GAP-296..303, P3=15 GAP-306..320, P5=24 GAP-321..344) — vs ~25 expected; deeper review surfaced more cases
  - **Cross-persona keystone gaps surfaced:** GAP-063 (Zalo/SMS notification) blocks ALL 4 personas — recommend bump P1 → P0; GAP-057 (commission/payroll) blocks P2+P3+P5 — recommend bump P1 → P0; recurring-class generator (GAP-290) blocks all 4
  - **K-12 LEGAL findings:** parent portal (Luật GD 2019 Đ.83 — GAP-321) + child protection (Luật Trẻ Em 2016 — GAP-322) + period attendance K-12 model (GAP-323) — K-12 GA blocked until Stage 1 lands (~6 weeks)
  - **Verdict:** all 4 Tier-1 personas NOT ready for GA at current state. Backlog re-prioritization needed: GAP-063 + GAP-057 → P0; Wave 18-19 should focus Stage 1 LEGAL + foundation per P5 review §Recommendations
- **2026-04-30** — **UNBLOCKED** — both dependencies shipped same day: GAP-151 (Wave 15 Persona-AC-Template, 121 ACs across 4 Tier-1 personas) + GAP-153 (Wave 16 Secondary-Persona-AC, 167 ACs across 8 secondary persona docs). Path B execution (GAP-153 first → GAP-152 next) avoided PARTIAL closure-loop. Total ACs available for review: **288 ACs across 12 docs** (4 tenant + 8 secondary). Wave 17 (next wave-pack candidate) ready to execute Round 1 review with 4 parallel agents (one per Tier-1 persona, consuming both tenant + relevant secondary AC docs).
- 2026-04-20 — Created. User raised: persona reviews chưa execute, xlsx import case show framework works but unused. Scoped to Tier 1 only to bound wave effort.
