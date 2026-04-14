# GAP-050: Persona-Based Business Review Process

**Status:** 🟡 PLANNED (Wave 1 Sprint 0)
**Branch:** wave/01-foundation
**Priority:** 🔴 P0 (meta process — discovers other gaps)
**Domain:** Product / Business
**Detected:** 2026-04-14 (user raised with bulk import example)
**Related:**
- `.claude/skills/persona-based-business-review.md`
- `documents/00-brd/personas-catalog.md`
- GAP-049 (business correctness)

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

- [ ] Persona catalog published (✓ done)
- [ ] Skill published (✓ done)
- [ ] Initial gaps filed (GAP-051..064)
- [ ] Quarterly review meeting on calendar
- [ ] pre-flight-check project integrates persona review
- [ ] quality-audit adds persona coverage category
- [ ] First complete role-play report (documents/00-brd/persona-reviews/)

## Dependencies

- Product owner engagement
- Business stakeholder identification
- Access to real personas for validation (user interviews)

## Log

- 2026-04-14 — User raised: review phải theo persona. Initial scan found 14+ critical gaps.
