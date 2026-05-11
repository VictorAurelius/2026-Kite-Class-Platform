# GAP-050: Persona-Based Business Review Process

**Status:** 🟢 DONE 2026-05-11 — framework AC 100% shipped 2026-04-29 (PR #653); execution scope explicitly delegated to GAP-152 per `gap-done-discipline.md` §4 Option B (scope-split design 2026-04-20, NOT close-time deferral)
**Branch:** wave/01-foundation
**Priority:** 🔴 P0 (business-logic tier per `meta-gap-priority.md` §3)
**Domain:** Product / Business
**Detected:** 2026-04-14 (user raised with bulk import example)
**Scope clarification (2026-04-20):** This gap = **PROCESS framework** (catalog, skill, cadence). AC template + per-persona AC → **GAP-151**. Execute reviews + produce reports → **GAP-152**.
**Related:**
- `.claude/skills/quality/persona-based-business-review.md`
- `documents/00-brd/personas-catalog.md`
- `.claude/rules/meta-gap-priority.md` §3 — business-logic tier
- GAP-049 (business correctness — process scope)
- GAP-151 (persona AC template — framework scope)
- GAP-152 (persona review execution — execution scope)

## Problem

Review business logic hiện tại **không có persona perspective** → miss critical features vì "không có ai nhập vai sử dụng thật".

**User-raised example:**
> Trường cấp 3 đăng ký, 500 học sinh phải tự tạo tài khoản + gửi credentials cho giáo viên để assign class → vỡ vụn nghiệp vụ.

Này là **symptom** của gap lớn hơn: không có persona-based review process.

## Initial Persona Scan (from catalog)

Quick role-play identified **14+ missing features** cho Tier 1 personas:

| Persona | Coverage | Critical Gaps |
|---------|:--------:|---------------|
| P1 Solo Teacher | 🟡 60% | Simple workflows, schedule UI |
| P2 Small Center | 🟢 75% | Acceptable cho MVP |
| P3 Medium Center | 🟡 65% | Payroll, roles, advanced reports |
| P5 K-12 School | 🔴 **30%** | **Bulk import, parent portal, academic year, report card, homeroom, conduct, periods** |

K-12 School persona is **most under-served** — biggest market, biggest misses.

## Proposed Fix

### Process Establishment

1. **Quarterly persona review** meeting
   - Product owner + business stakeholder
   - Walk through each Tier 1 persona
   - Identify new gaps
   - Update catalog

2. **Role-play methodology** (skill persona-based-business-review.md)
   - Realistic scale (100, 500, 5000)
   - Full journey (discovery → termination)
   - Edge cases + peak moments

3. **Output: gap files** per discovered feature
   - Link to persona(s) blocked
   - Priority by market impact

### Immediate Actions (this gap)

- ✅ Create persona catalog (documents/00-brd/personas-catalog.md)
- ✅ Create skill (persona-based-business-review.md)
- 🆕 File critical gaps discovered in initial review:
  - GAP-051: Bulk import xlsx (CRITICAL — user's example)
  - GAP-052: Parent portal
  - GAP-053: Academic year structure
  - GAP-054: Multi-subject per student
  - GAP-055: Official report card VN format
  - GAP-056: Homeroom teacher (GVCN)
  - GAP-057: Payroll calculation
  - GAP-058: Role hierarchy / org chart
  - GAP-059: Student conduct tracking
  - GAP-060: Period-based attendance
  - GAP-061: Promotion/retention logic
  - GAP-062: Payroll integration
  - GAP-063: SMS/Zalo notifications
  - GAP-064: SCORM/xAPI (P7 corporate)

- 🆕 Schedule quarterly review
- 🆕 Integrate vào pre-flight-check project layer

## Acceptance Criteria

### Scope: PROCESS FRAMEWORK (this gap)

- [x] Persona catalog published (✓ done 2026-04-14)
- [x] Skill published (✓ done 2026-04-14)
- [x] Initial gaps filed (GAP-051..064 — done 2026-04-14)
- [x] Quarterly review cadence documented (rule or skill note — not calendar event) ✓ 2026-04-29 — `.claude/skills/quality/persona-based-business-review.md` §Quarterly Review Cadence (calendar-anchored EOQ dates Q1/Q2/Q3/Q4, off-cycle triggers, reviewer roles, `next_review` field tracking)
- [x] pre-flight-check project integrates persona review step ✓ 2026-04-29 — `.claude/skills/quality/pre-flight-check.md` Layer 4 (Persona impact check) added; auto-trigger conditions on user-facing PR diff; reviewer-side blocker semantics
- [x] quality-audit /100 adds persona coverage category (referencing GAP-152 output) ✓ 2026-04-29 — `.claude/skills/quality-audit/SKILL.md` Cat 11 Persona Coverage /10; total scale 100 → 110; GAP-152 referenced as data source; baseline 5/10 until first reports ship

### Delegated to sibling gaps (scope split 2026-04-20)

- ~~AC template + per-persona AC docs~~ → **GAP-151**
- ~~First complete role-play report~~ → **GAP-152** (ships 4 Tier 1 reports)
- ~~Persona-specific FAIL handling~~ → **GAP-152** (new gaps filed per finding)

## Dependencies

- Product owner engagement
- Business stakeholder identification
- Access to real personas for validation (user interviews)

## Log

- **2026-05-11:** PR# backfill + flip DONE (Wave 60 Bucket D-2). Verified shipped work cross-references:
  - PR #653 — `docs(skills): GAP-050 — persona review framework Phase 1 (cadence + pre-flight Layer 4 + audit Cat 11)` (merged 2026-04-29) — shipped 3 framework AC items: quarterly cadence in `persona-based-business-review.md`, pre-flight `Layer 4` Persona impact check, `quality-audit/SKILL.md` Cat 11 Persona Coverage /10.
  - PR #719 — `docs(wave): kick off Wave Persona-AC-Template — full ship GAP-151 (template + 4 Tier-1 AC docs + skill update)` (merged later) — GAP-151 ac-template delivered; cross-reference only.

  Code-verify: 5/5 framework AC verified shipped (catalog `.claude/skills/quality/persona-based-business-review.md` exists; cadence section present; pre-flight Layer 4 present; quality-audit Cat 11 present; initial gaps GAP-051..064 filed). Execution scope (per-persona reports) delegated to GAP-152 by 2026-04-20 scope-split design — this is `gap-done-discipline.md` §4 Option B (scope cut documented as out-of-scope at filing time, NOT a close-time deferral) → DONE valid.

  Verdict: 🟢 DONE (framework complete; execution = GAP-152).

- **2026-04-29 — Phase 1 framework AC shipped** (Wave Business Correctness, Cluster 5 Agent C, PR #653). All 3 remaining framework AC items checked: (1) Quarterly review cadence added to `persona-based-business-review.md` §Quarterly Review Cadence (calendar-anchored EOQ Q1-Q4 + off-cycle triggers + reviewer roles + `next_review` tracking); (2) `pre-flight-check.md` extended with Layer 4 (Persona impact check) — fires on user-facing diff, blocks merge on coverage degradation; (3) `quality-audit/SKILL.md` extended with Cat 11 Persona Coverage /10, total scale rebased 100 → 110 with proportional grade thresholds, references GAP-152 as data source. Status flipped 🟡 PLANNED → 🟡 PARTIAL: execution remains in GAP-152 (already-filed); scope was split 2026-04-20 with GAP-152 owning execution by design (NOT scope cut at close-time per `gap-done-discipline.md` §3 PARTIAL exit-ramp). Remaining work tracked in GAP-152 (first 4 Tier 1 reports) and GAP-151 (per-persona AC template).
- 2026-04-20 — Scope split: this gap = PROCESS framework. AC template + per-persona AC → GAP-151. Review execution + reports → GAP-152. 3 original AC items marked done (catalog, skill, initial gaps). Remaining AC narrowed to framework integration points.
- 2026-04-14 — User raised: review phải theo persona. Initial scan found 14+ critical gaps.
