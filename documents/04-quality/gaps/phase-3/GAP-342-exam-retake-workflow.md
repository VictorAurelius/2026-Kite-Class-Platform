# GAP-342: Exam Re-take Workflow for Sick Students with Admin Approval + Evidence

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-EDGE-004

## Current State (verified 2026-05-04)

No re-take workflow. HS ốm bị mark vắng + 0 → ảnh hưởng lên lớp.

## Problem

HS ốm có giấy bệnh viện → cần re-take, không penalty. Without: unfair grade impact.

## Proposed Fix

1. **ExamRetakeRequest entity:** student_id, exam_session_id, evidence_upload, status, approved_by
2. **Workflow:** PH submit → admin/Phó CM duyệt → schedule re-take session
3. **Grade tích hợp** vào ĐTBmHK normally

## Acceptance Criteria

- [ ] ExamRetakeRequest + workflow
- [ ] Evidence upload via MinIO (encrypted)
- [ ] Test: HS C ốm submit evidence → approve → re-take scheduled → điểm normal
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** GAP-321 (PH submission), GAP-328 (ExamSession), GAP-061 (promotion logic)
- **Wave plan:** Bucket D Stage 4

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
