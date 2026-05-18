# GAP-329: Substitute Teacher Workflow with Time-bound RBAC

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-OPS-006, AC-EDGE-003; GAP-058

## Current State (verified 2026-05-04)

```bash
grep -rl "substitute\|time.bound.RBAC" kiteclass/ --include="*.java"
```
Result: zero. No substitute workflow; no time-bound class access.

## Problem

GV bộ môn báo nghỉ → Phó CM phân công GV thay; GV thay nhận access **chỉ tiết đó** then auto-revoke. AC-OPS-006 + AC-EDGE-003 FAIL.

## Proposed Fix

1. **Substitution entity:** `Substitution (id, original_teacher_id, substitute_teacher_id, class_id, subject_id, date, period_no, expires_at, lesson_plan_url, audit)`
2. **Time-bound RBAC:** dynamic permission grant valid only `[period_start, period_end]`
3. **Mobile workflow:** Phó CM dashboard "Emergency substitution" → list available GV → assign
4. **Push notification** GV thay với link giáo án + roster

## Acceptance Criteria

- [ ] Substitution entity + RBAC integration
- [ ] Time-bound permission expires automatically (test by querying after period_end)
- [ ] ≤30 min SLA Phó CM mobile workflow
- [ ] Audit log substitution
- [ ] Test: GV báo nghỉ → Phó CM assign → GV thay access lớp → tiết kết thúc → access revoke
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** GAP-324 (role hierarchy), GAP-323 (period schema)
- **Wave plan:** Bucket D Stage 3

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
