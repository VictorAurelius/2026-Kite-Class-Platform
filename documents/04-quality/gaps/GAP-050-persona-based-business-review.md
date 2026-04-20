# GAP-050: Persona-Based Business Review Process

**Status:** 🟡 PLANNED (Wave 1 Sprint 0)
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
- [ ] Quarterly review cadence documented (rule or skill note — not calendar event)
- [ ] pre-flight-check project integrates persona review step
- [ ] quality-audit /100 adds persona coverage category (referencing GAP-152 output)

### Delegated to sibling gaps (scope split 2026-04-20)

- ~~AC template + per-persona AC docs~~ → **GAP-151**
- ~~First complete role-play report~~ → **GAP-152** (ships 4 Tier 1 reports)
- ~~Persona-specific FAIL handling~~ → **GAP-152** (new gaps filed per finding)

## Dependencies

- Product owner engagement
- Business stakeholder identification
- Access to real personas for validation (user interviews)

## Log

- 2026-04-20 — Scope split: this gap = PROCESS framework. AC template + per-persona AC → GAP-151. Review execution + reports → GAP-152. 3 original AC items marked done (catalog, skill, initial gaps). Remaining AC narrowed to framework integration points.
- 2026-04-14 — User raised: review phải theo persona. Initial scan found 14+ critical gaps.
