# GAP-328: Mid+Final Exam Workflow with Approval Chain + Publish-Window

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-OPS-005

## Current State (verified 2026-05-04)

```bash
grep -rl "exam.workflow\|publish.window" kiteclass/ --include="*.java"
```
Result: no exam workflow distinct from regular grades; no approval chain; no publish-window control.

## Problem

K-12 mid/final exam workflow: Phó CM tạo lịch → admin in giấy mời PH + danh sách phòng thi → GV chấm trong window 7d → Tổ trưởng duyệt → HT ký thông báo → kết quả gửi PH. Currently: ad-hoc grade entry, leak risk before duyệt.

## Proposed Fix

1. **ExamSession entity:** distinct from regular grade (date range, room assignment, invigilation roster)
2. **State machine:** SCHEDULED → IN_PROGRESS → GRADING → DEPT_REVIEW → PRINCIPAL_APPROVAL → PUBLISHED
3. **Publish-window:** Grades hidden from PH until PUBLISHED; admin cannot bypass
4. **MOET giấy mời PH PDF:** auto-generate per exam session
5. **Phòng thi assignment:** auto-assign 30 HS / phòng theo SBD MOET-style

## Acceptance Criteria

- [ ] ExamSession entity + State Pattern transitions
- [ ] Publish-window enforced (PH portal returns 404 until PUBLISHED)
- [ ] MOET giấy mời PDF generation (use document-generation skill GAP-047)
- [ ] Approval chain notifications via in-app + email
- [ ] Test: mid-term exam workflow E2E from Phó CM → HT → PH visible
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** GAP-323 (gradebook), GAP-058 (role hierarchy), GAP-047 (PDF generation)
- **Cross-cuts:** GAP-331 (invigilation roster — uses ExamSession)
- **Wave plan:** Bucket D Stage 2

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
