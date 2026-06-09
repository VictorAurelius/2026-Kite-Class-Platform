# GAP-1116: LMS completeLesson không enforce enrollment cho bài trả phí (BR-LMS-019 chưa impl)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend (security/business — LMS)
**Found:** 2026-06-10 (outside-in audit 3-lens FE LMS wave — failure-mode F2)
**Affects:** `kiteclass-core` `LessonProgressServiceImpl.completeLesson` (dòng ~50-60)

## Problem

`completeLesson(lessonId, userId)` cho bài trả phí (`isTrial=false`) KHÔNG verify enrollment. Code cũ chỉ log `"For now, we'll allow progress tracking without strict enrollment check (can be added later)"` + `"ideally should verify enrollment"` — tức là no-op. BR-LMS-019 (paid lesson completion requires active enrollment) đã được document nhưng chưa implement → student CHƯA enroll vẫn mark được bài trả phí là "completed", làm sai progress tracking + có thể trigger `LessonCompletedEvent` downstream không hợp lệ.

`getLessonForStudent` (access content) đã enforce enrollment đúng, nhưng `completeLesson` (ghi progress) thì không — bất nhất giữa "đọc nội dung" và "ghi progress".

## Root Cause

`LessonProgressServiceImpl` không inject `EnrollmentRepository` + `ClassRepository` (khác `LmsServiceImpl` đã có) nên không thực hiện được enrollment check, để lại comment "can be added later".

## Proposed Fix

Inject `EnrollmentRepository` + `ClassRepository` vào `LessonProgressServiceImpl`; thêm `verifyStudentEnrollment(userId, courseId)` (mirror `LmsServiceImpl#getLessonForStudent`). Trong `completeLesson`: bài paid (`!isTrialLesson()`) → lookup module → `verifyStudentEnrollment` (throw `PermissionDenied("STUDENT_NOT_ENROLLED_IN_COURSE")` nếu chưa enroll); bài trial → cho phép (free preview).

## Acceptance Criteria
- [x] Complete bài paid khi CHƯA enroll → 403 `STUDENT_NOT_ENROLLED_IN_COURSE` (không ghi progress, không publish event)
- [x] Complete bài paid khi ĐÃ enroll → 200, ghi progress + publish event
- [x] Complete bài trial khi chưa enroll → 200 (không gọi enrollment repos)
- [x] Unit test phủ 3 case trên (`LessonProgressServiceTest`)
- [ ] Runtime-walk verify trên stack production-equivalent (gateway :9000) trước DONE flip

## Related
- Audit report: `documents/04-quality/audits/persona-review/2026-06-10-pre-wave-lms-fe-outside-in.md` (F2)
- Sister fix cùng PR: GAP-1115 (paywall structure), GAP-1117 (missing-header 500), GAP-1118 (tenant-context leak)
- Cùng enrollment-check pattern với `LmsServiceImpl#getLessonForStudent`

## Log

- **2026-06-10 (LMS BE security wave):** Fix shipped — inject `EnrollmentRepository` + `ClassRepository` vào `LessonProgressServiceImpl`; `completeLesson` cho bài paid giờ gọi `verifyStudentEnrollment(userId, module.getCourseId())` thay vì no-op log; bài trial vẫn allow. Unit test 3-case PASS (`LessonProgressServiceTest` deny-paid-not-enrolled + allow-paid-enrolled (3 happy paths cập nhật stub enrollment) + allow-trial-no-enrollment). Status 🟡 PARTIAL ~85% — code + test PASS; **residual:** runtime-walk per `feature-ship-runtime-walk-mandate.md` trước DONE flip.
