# GAP-1297: LMS controllers đọc X-User-Id (UUID) as Long → 400 mọi LMS GET/progress cho authenticated user

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Phase:** phase-1-beta
**Found:** 2026-06-14 (G1 runtime walk RBAC+LMS — Flow 4/5)
**Affects:** `kiteclass-core` LMS — `LmsController` (3 GET) + `LessonProgressController` (3 endpoints)

## Problem

G1 gateway-walk (qua `:9000`, minted HS512 JWT) phát hiện: mọi LMS GET/progress endpoint trả **HTTP 400 `PARAM_TYPE_MISMATCH "Parameter 'X-User-Id' has invalid type"`** khi gọi bởi authenticated user (student/teacher):

```
GET /api/v1/lms/courses/13/modules (STUDENT ref=4) → 400
GET /api/v1/lms/lessons/1                            → 400
POST /api/v1/lms/progress/lessons/1/complete         → 400
GET /api/v1/lms/progress/courses/13                  → 400
```

Root cause: controller đọc `@RequestHeader("X-User-Id") Long userId`, nhưng **gateway inject `X-User-Id` = JWT `sub` (UUID)** (per `JwtAuthenticationGatewayFilter`), và cung cấp numeric reference qua header riêng `X-User-Reference-Id`. UUID không bind được vào `Long` → 400. Guest (không token) hoạt động vì header vắng (dual-mode null = guest).

Bằng chứng kiểu drift đúng nguồn: `lesson_progress.user_id` là `bigint` (= `students.id`), KHÔNG phải UUID. Nguồn đúng là `X-User-Reference-Id` (= entity numeric id), giống convention `EnrollmentController.getMyEnrollments()` đã dùng. Cùng class FE↔gateway↔BE contract drift với GAP-1069.

Blocking: LMS Increment B student consumption (GAP-1113) — lesson-player + mark-complete + progress hoàn toàn không gọi được qua gateway.

## Proposed Fix (SHIPPED inline)

Đổi `@RequestHeader("X-User-Id") Long userId` → `@RequestHeader("X-User-Reference-Id") Long userId` tại:
- `LmsController` `getCourseStructure` + `getLesson` (2 chỗ, `required=false` giữ guest-mode)
- `LessonProgressController` `completeLesson` + `getCourseProgress` + `getLessonProgress` (3 chỗ)

Authz hardening (actor-UUID ↔ numeric owner verify) tracked riêng GAP-798.

## Acceptance Criteria

- [x] LMS GET/progress trả 200 cho authenticated user (G1 re-walk PASS — progress lưu `userId:4` = students.id)
- [x] Guest-mode GET vẫn 200 (header vắng → guest)
- [ ] G2★ human browser-walk student-shell lesson-player + mark-complete (FE `:3000`) PASS
- [ ] (defer GAP-798) actor authz verify (không tin numeric id spoof)

## Related

- Discovered in: G1 walk `documents/04-quality/audits/rst-html/2026-06-14-g1-runtime-walk-rbac-lms.md`
- Cùng class: GAP-1069 (FE↔gateway↔BE contract drift), GAP-1117 (missing-header 500)
- Authz hardening umbrella: GAP-798 (actor-UUID ↔ numeric domain owner bridge)
- Flow: GAP-1113 (LMS Increment B), GAP-1285 (student enrollment)

## Addendum (G1 walk caller-sweep, 2026-06-14)

- **Caller-sweep test fix (cùng PR):** `LmsIntegrationTest` (4) + `LessonProgressIntegrationTest` (11) gửi `X-User-Id` → đổi sang `X-User-Reference-Id` khớp controller (per `api-contract-change-caller-sweep.md`). `GlobalExceptionHandlerTest` giữ nguyên (dùng "X-User-Id" làm string literal mẫu cho missing-header handler GAP-1117, không phải caller LMS).
- **End-to-end confirm:** KC tenant-auth login JWT có claim `referenceId = entityId` (`AuthTokenService.java:73`); gateway inject `X-User-Reference-Id` từ claim này (Wave auth-1 Bucket C). → fix BE đúng end-to-end (student login → JWT referenceId=students.id → gateway → X-User-Reference-Id → LMS controller).
- **FE dead-code (cleanup khuyến nghị, không blocking):** `kiteclass-frontend/src/lib/api/lms.ts:42` + `assignments.ts:131` set thủ công `X-User-Id` — gateway STRIP header này (anti-spoof GAP-814) rồi re-inject từ JWT → manual X-User-Id vô tác dụng. Có thể bỏ trong FE cleanup; không ảnh hưởng vì gateway cung cấp X-User-Reference-Id.
