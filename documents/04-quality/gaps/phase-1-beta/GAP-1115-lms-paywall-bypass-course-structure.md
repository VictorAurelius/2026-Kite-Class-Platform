# GAP-1115: LMS paywall bypass — getCourseStructureForStudent trả full content bài trả phí cho student chưa enroll

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend (security — LMS)
**Found:** 2026-06-10 (outside-in audit 3-lens FE LMS wave — failure-mode F1)
**Affects:** `kiteclass-core` `LmsServiceImpl.getCourseStructureForStudent` (dòng ~122-138) + `buildModuleDetailResponse` + `LessonResponse`

## Problem

`getCourseStructureForStudent(courseId, userId)` fetch **TẤT CẢ lessons** (gồm cả bài `isTrial=false` trả phí) rồi map qua `lmsMapper.toLessonResponseList` → `LessonResponse` (chứa `content` + `videoUrl`). Comment trong code ghi "We do NOT verify enrollment here to allow students to see course structure before enrolling" — nhưng việc trả nguyên `content` + `videoUrl` của bài trả phí = lộ toàn bộ nội dung khoá học cho student CHƯA enroll (paywall bypass BR-LMS-002).

Endpoint detail `getLessonForStudent` đã có enrollment gate đúng (trial → cho phép; paid → `verifyStudentEnrollment`). Guest path `getCourseStructurePublic` cũng đúng (chỉ trả trial lessons). Riêng `getCourseStructureForStudent` bị hổng: outline preview tốt cho UX, nhưng BODY (content + videoUrl) của bài trả phí phải bị che cho non-enrolled.

## Root Cause

Thiết kế ưu tiên UX preview-trước-khi-enroll nhưng không tách "outline metadata" (được phép xem) khỏi "lesson body" (paywalled). Toàn bộ `LessonResponse` được trả nguyên thay vì strip `content`/`videoUrl` cho bài trả phí khi student chưa enroll.

## Proposed Fix

Trong `getCourseStructureForStudent`: tính `enrolled = isStudentEnrolledInCourse(userId, courseId)` (1 query, không throw — extract từ `verifyStudentEnrollment`). Với mỗi lesson: enrolled HOẶC `isTrialLesson()` → trả full body; còn lại (paid + chưa enroll) → strip `content` + `videoUrl` về null, GIỮ metadata (title/order/isTrial/estimatedDuration). Outline vẫn hiển thị đầy đủ cho UX, body bị paywall.

## Acceptance Criteria
- [x] Student chưa enroll → bài `isTrial=false` trả về với `content=null` + `videoUrl=null` (metadata giữ nguyên)
- [x] Student chưa enroll → bài trial trả full body
- [x] Student đã enroll → full body cho mọi bài
- [x] Unit test phủ 3 case trên (`LmsServiceTest`)
- [~] Runtime-walk verify trên stack production-equivalent — **G1 (agent) PASS 2026-06-19** via direct-core `:8088` (gateway-equivalent headers); G2 (human) pending

## Related
- Audit report: `documents/04-quality/audits/persona-review/2026-06-10-pre-wave-lms-fe-outside-in.md` (F1)
- Sister fix cùng PR: GAP-1116 (completeLesson enrollment), GAP-1117 (missing-header 500), GAP-1118 (tenant-context leak)
- Discovered in: outside-in audit FE LMS wave (GAP-1113 scope)

## Log

- **2026-06-10 (LMS BE security wave):** Fix shipped — `getCourseStructureForStudent` giờ tính `enrolled` 1 lần qua `isStudentEnrolledInCourse` (extract từ `verifyStudentEnrollment`, không throw) + `buildStudentModuleDetailResponse` strip `content`/`videoUrl` cho bài paid khi chưa enroll (giữ metadata). Unit test 3-case PASS (`LmsServiceTest` enrolled-full + notEnrolled-stripped + trial-intact). Status 🟡 PARTIAL ~85% — code + test PASS; **residual:** runtime-walk trên gateway :9000 với minted JWT (enrolled vs non-enrolled student) per `feature-ship-runtime-walk-mandate.md` trước DONE flip.
- **2026-06-14 (wave-2026-06-14-p0-closeout-1 Bucket A — branch `fix/wave-p0-a-lms-paywall`):** Re-verified fix vẫn còn trên main `cd89a3ddc` + **hardening + cross-flow sweep** (per `cross-flow-bug-class-sweep.md`). Tách logic enrollment-check bị duplicate (trước đây copy-paste trong `LmsServiceImpl` + `LessonProgressServiceImpl`) thành 1 component dùng chung `LessonAccessGuard` → read-path (gap này) + write-path (GAP-1116) giờ dùng CHUNG đúng 1 guard, không thể drift. **Sweep mọi endpoint trả lesson body cho student:** `getCourseStructureForStudent` + `getLessonForStudent` đều guarded (FIX); teacher `getModule`/`getLessonForTeacher`/`getCompletionRoster` gated ownership (EXEMPT); guest `getCourseStructurePublic`/`getLessonPublic` chỉ trả trial (EXEMPT); storage `GET /api/v1/storage/{fileId}/download-url` dùng access model visibility (PUBLIC/PRIVATE/TENANT), KHÔNG check enrollment → **DEFER** (cross-module, đề xuất coordinator file follow-up gap). Thêm `LessonAccessGuardTest` (5 case) + 3-case paywall tests `LmsServiceTest` PASS. Vẫn 🟡 PARTIAL — AC 5 (human runtime-walk gateway :9000 + RLS) chưa thực hiện được bởi agent → giữ PARTIAL trước DONE flip.
- **2026-06-19 (G1 runtime-walk — agent):** Unblocked walk data + G1-verified paywall live. Extended committed idempotent seed `kitehub/scripts/seed-walk-tenant.sh` (per `walk-data-committed-seed.md`): course ENG-A1 (id=30, paid 1.2M) + module "Chương 1" + 1 PAID lesson (`is_trial=false`, content+video) + 1 TRIAL lesson (`is_trial=true`, content+video) + non-enrolled student hv6 (id=180, "Đỗ Thị Lan", deliberately excluded from enroll loop). Re-run verified 0 duplicate. **G1 verify** `GET /api/v1/lms/courses/30/modules` direct-core `:8088` (`X-Tenant-Id` + `X-User-Reference-Id`; same headers the gateway forwards — `GatewayHeaderAuthenticationFilter`; RLS enforced via tenant context, no platform-admin bypass): (a) non-enrolled student 180 → paid lesson `content=null` + `videoUrl=null`, metadata (id/title/isTrial) kept ✅; (b) non-enrolled 180 → trial lesson full body ✅; (c) enrolled student 175 → paid lesson full body (content 57 chars + videoUrl) ✅. Paywall holds — **G1-PASS**. Production student-JWT path (gateway `:9000`) N/A: KC students have no self-login (parent-portal model); paywall is BE service logic (`LessonAccessGuard` + `stripPaidLessonBody`), fully exercised via gateway-equivalent header. Status stays 🟡 PARTIAL — human G2 walk pending before DONE flip.
