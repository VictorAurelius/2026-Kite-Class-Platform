---
title: G1 Runtime Walk — RBAC + LMS + SSO (gateway BE-contract)
audience: dev
created: 2026-06-14
scope: Flow Verification Campaign G1 (Claude agent runtime walk) cho code RBAC role-shell + LMS + cross-product SSO shipped 2026-06-14
walk_type: gateway BE-contract walk (minted HS512 JWT → gateway :9000); browser-walk FE = G2★ human pending
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - .claude/rules/g1-browser-walk-before-flip.md
  - documents/05-guides/operations/2026-06-14-g2-recipe-rbac-role-shell.md
  - documents/05-guides/operations/2026-06-14-g2-recipe-lms-teacher-catalog.md
  - documents/05-guides/operations/2026-06-14-g2-recipe-lms-student.md
  - documents/05-guides/operations/2026-06-14-g2-recipe-sso-kh-kc.md
---

# G1 Runtime Walk — RBAC + LMS + SSO (2026-06-14)

## Phạm vi + phương pháp

- **Loại walk:** G1 agent runtime walk theo **G3/G1 gateway pattern** (memory `project_g3_walk_recipe`): mint HS512 JWT bằng gateway `JWT_SECRET`, curl qua gateway `:9000` (production-accurate auth chain), assert HTTP status + body. Bắt FE↔gateway↔BE contract drift mà unit/IT test che mất.
- **KHÔNG phải browser-walk.** FE containers (`kiteclass-frontend :3000`, `kitehub-frontend :3001`) chỉ reachable bằng **image cũ 2 ngày (pre-session)** — KHÔNG có các trang RBAC/LMS/student-shell/SSO mới của session này. Browser-real walk các trang FE mới = **G2★ human** (cần rebuild FE). Per `g1-browser-walk-before-flip.md` §1, **chưa flip** flow nào sang `walk-pass-pending-human` vì thiếu browser-walk evidence.
- **Stack:** kiteclass-core + kitehub-subscription **rebuild từ main HEAD `0d9167996`** (container cũ build trước commit session → bắt buộc rebuild). Postgres `kiteclass_shared` RLS, gateway HS512→header chain thật. Tenant A = `0edaee10-…` (sky-education): 86 students, 8 courses, 19 enrollments, 6 teachers, course 13 PUBLISHED.
- **Fixtures:** student 4 (2 enrollments: class 9 + 14), student 5 (class 6) cho isolation; teacher 3 dạy course 13; tokens minted với `referenceId` = entity numeric id (students.id / teachers.id).

## Tổng kết verdict

| Flow | G1 verdict | Ghi chú |
|---|---|---|
| 1. RBAC role-redirect / RoleGuard | ✅ PASS | Cross-role 403 đúng; role-home BE-area reachable đúng role |
| 2. RBAC Bucket D (assign/revoke) | ✅ PASS (sau fix GAP-1298) | Found+fixed inline: `LazyInitializationException` 500 |
| 3. STAFF coverage | ✅ PASS | STAFF allow enrollment/invoice/attendance; deny payroll/branding 403 |
| 4. LMS teacher authoring + guest catalog | ✅ PASS (sau fix GAP-1297) | create/reorder/upload-url OK; guest catalog OK; teacher GET fixed |
| 5. LMS student consumption | ✅ PASS (sau fix GAP-1297) | enrollments/me + isolation OK; lesson-player + progress fixed |
| 6. SSO KH→KC | ✅ FULL PASS | issue→exchange→no-relogin; replay 401; CSRF 415; no fix needed |

**Discoveries:** 2 blocking bug fixed inline (GAP-1297 + GAP-1298, G1 re-walk PASS) + 1 security PoC appended to existing **GAP-798** (X-Teacher-Id spoofable — không file gap mới, tránh duplicate).

**KHÔNG flip gap nào sang DONE** — G2★ human browser-walk bắt buộc (per task + `g1-browser-walk-before-flip.md`).

---

## Flow 1 — RBAC role-redirect / RoleGuard (GAP-1119/1277)

> Role-home redirect là FE (RoleGuard component `roles.ts ROLE_HOME`) — G1 verify BE authz backing mỗi role-area + cross-role 403.

| Bước | Command (qua gateway :9000) | Kết quả | Verdict |
|---|---|---|---|
| OWNER → owner-area | `GET /api/v1/roles/templates` | HTTP 200 (5 templates) | ✅ |
| STUDENT → student-area | `GET /api/v1/enrollments/me` | HTTP 200 | ✅ |
| TEACHER → teacher-area | `GET /api/v1/lms/courses/13/modules` | HTTP 200 (sau fix GAP-1297) | ✅ |
| TEACHER → owner-only (RoleGuard) | `GET /api/v1/roles/templates` | HTTP 403 ACCESS_DENIED | ✅ |
| STUDENT → owner-only | `GET /api/v1/roles/templates` | HTTP 403 | ✅ |
| PARENT → owner-only | `GET /api/v1/roles/templates` | HTTP 403 | ✅ |
| TEACHER → student-only | `GET /api/v1/enrollments/me` | HTTP 403 | ✅ |

**Verdict: ✅ PASS** — BE authz backing RoleGuard đúng (cross-role 403 cả 4 chiều). Role-home redirect FE-side cần G2★.

## Flow 2 — RBAC Bucket D assign/revoke (GAP-1119)

| Bước | Command | Trước fix | Sau fix (GAP-1298) | Verdict |
|---|---|---|---|---|
| List templates | `GET /api/v1/roles/templates` | 200 | 200 | ✅ |
| Seed templates | `POST /api/v1/roles/seed` | 200 (roleId 2-6) | 200 | ✅ |
| List assignments | `GET /api/v1/roles/assignments` | **500** | 200 | ✅ fixed |
| Assign user→role | `POST /api/v1/roles/assignments {userId:5,roleName:STAFF}` | **500** | 201 (`roles:["STAFF"]`) | ✅ fixed |
| List after assign | `GET /api/v1/roles/assignments` | **500** | 200 (`[{userId:5,roles:[STAFF]}]`) | ✅ fixed |
| Revoke | `DELETE /api/v1/roles/assignments?userId=5&roleName=STAFF` | 204 | 204 | ✅ |
| Assign invalid role | `POST … {roleName:NOPE}` | 400 INVALID_ROLE_NAME | 400 | ✅ |
| TEACHER assign (cross-role) | `POST …` | 403 | 403 | ✅ |

**Bug found (GAP-1298):** `RoleService.listAssignments()` + `getAssignmentForUser()` thiếu `@Transactional` → `org.hibernate.LazyInitializationException: Could not initialize proxy [Role#4] - no session` khi map `ur.getRole().getName()` ngoài session (OSIV off). 500 trên mọi assign/list khi có ≥1 assignment. **Fix inline:** thêm `@Transactional(readOnly = true)`. Re-walk PASS.

**Verdict: ✅ PASS (sau fix)**

## Flow 3 — STAFF coverage (GAP-1274/1275)

| Bước | Command | Kết quả | Verdict |
|---|---|---|---|
| STAFF enrollment | `GET /api/v1/enrollments/11` | 200 | ✅ allow |
| STAFF invoice | `GET /api/v1/invoices` | 200 | ✅ allow |
| STAFF attendance | `GET /api/v1/attendance/stats/class/9` | 200 (rate 95%) | ✅ allow |
| STAFF payroll | `GET /api/v1/admin/payroll/configs` | 403 | ✅ deny |
| STAFF branding | `PUT /api/v1/settings/branding` (valid body) | 403 ACCESS_DENIED | ✅ deny |
| OWNER branding (control) | `PUT /api/v1/settings/branding` (valid body) | 200 | ✅ control |

**Verdict: ✅ PASS** — `@PreAuthorize` STAFF coverage chính xác (allow enrollment/invoice/attendance; deny payroll/branding ADMIN/OWNER-only).

## Flow 4 — LMS teacher authoring + guest catalog (GAP-1113/1115)

| Bước | Command | Kết quả | Verdict |
|---|---|---|---|
| Teacher GET structure | `GET /api/v1/lms/courses/13/modules` (TEACHER) | 200 (sau fix GAP-1297; trước: 400) | ✅ fixed |
| Teacher create module | `POST /api/v1/lms/courses/13/modules` + `X-Teacher-Id:3` | 201 (module id) | ✅ |
| Teacher create lesson | `POST /api/v1/lms/modules/{id}/lessons` | 201 (lesson id, isTrial) | ✅ |
| Teacher reorder | `PUT /api/v1/lms/courses/13/modules/reorder {items:[…]}` | 200 | ✅ |
| Teacher upload-url | `POST /api/v1/lms/lessons/{id}/resources/upload-url` | 201 (presigned MinIO URL) | ✅ |
| Guest catalog | `GET /api/v1/courses?status=PUBLISHED` (no token + `X-Instance-Subdomain`) | 200 (course 13) | ✅ permitAll |
| Guest course structure | `GET /api/v1/lms/courses/13/modules` (no token = guest-mode) | 200 (trial) | ✅ |

**Verdict: ✅ PASS (sau fix GAP-1297)** — teacher authoring CRUD + guest catalog hoạt động. ⚠️ Bảo mật: teacher authoring tin `X-Teacher-Id` client-supplied (spoofable) — xem GAP-798 (PoC dưới).

## Flow 5 — LMS student consumption (GAP-1113/1285)

| Bước | Command | Kết quả | Verdict |
|---|---|---|---|
| Student my enrollments | `GET /api/v1/enrollments/me` (STUDENT ref=4) | 200 (class 14) | ✅ |
| Student2 isolation | `GET /api/v1/enrollments/me` (STUDENT ref=5) | 200 (class 6 — disjoint) | ✅ isolation |
| Student GET structure | `GET /api/v1/lms/courses/13/modules` | 200 (sau fix GAP-1297; trước: 400) | ✅ fixed |
| Student GET lesson | `GET /api/v1/lms/lessons/1` | 200 (full content) | ✅ fixed |
| Student mark-complete | `POST /api/v1/lms/progress/lessons/1/complete` | 200 (`userId:4 completed:true 100%`) | ✅ fixed |
| Student course progress | `GET /api/v1/lms/progress/courses/13` | 200 (1/1, 100%) | ✅ fixed |

**Bug found (GAP-1297):** `LmsController` (3 GET) + `LessonProgressController` (3) đọc `@RequestHeader("X-User-Id") Long userId`, nhưng gateway inject `X-User-Id` = JWT `sub` (UUID) + cung cấp numeric reference qua `X-User-Reference-Id`. Authenticated user → 400 `PARAM_TYPE_MISMATCH 'X-User-Id'` trên MỌI LMS GET/progress (guest OK vì header vắng). `lesson_progress.user_id` là `bigint` = students.id → nguồn đúng là `X-User-Reference-Id`. **Fix inline:** đổi 5 chỗ `X-User-Id` → `X-User-Reference-Id`. Re-walk: progress lưu `userId:4` đúng (= students.id). Isolation student 4 vs 5 = tập enrollment khác nhau ✅.

**Verdict: ✅ PASS (sau fix GAP-1297)** — enrollment-scope + isolation + lesson-player + progress hoạt động.

## Flow 6 — SSO KH→KC (GAP-1138, ADR-040 Option A)

| Bước | Command | Kết quả | Verdict |
|---|---|---|---|
| Issue code | `POST /api/v1/auth/sso/issue-code` (KH access token) | 200 `{code, expiresIn:60}` | ✅ |
| Issue no-token | `POST …/issue-code` (no Authorization) | 401 SSO_UNAUTHORIZED | ✅ |
| Exchange | `POST /api/v1/auth/sso/exchange {code}` | 200 (accessToken+refreshToken+user, no re-login) | ✅ |
| Replay (single-use) | `POST …/exchange {code}` lần 2 | 401 ("mã không hợp lệ/hết hạn") | ✅ |
| CSRF (non-JSON) | `POST …/exchange` `Content-Type: x-www-form-urlencoded` | 415 Unsupported Media Type | ✅ |

**Verdict: ✅ FULL PASS** — toàn bộ AC SSO PASS (TTL 60s, single-use GETDEL, CSRF JSON-guard, no-relogin). Không cần fix.

---

## FE reachability

| FE | Port | Kết quả | Ghi chú |
|---|---|---|---|
| KiteClass | `:3000` | HTTP 200 | ⚠️ image **stale 2 ngày** (pre-session) — KHÔNG có trang RBAC/LMS/student-shell mới |
| KiteHub | `:3001` | HTTP 200 | ⚠️ image **stale 2 ngày** — KHÔNG có nút SSO "Mở quản lý trường" mới |

→ FE server reachable nhưng **chưa build code session này**. Browser-walk các trang FE mới (RBAC assign UI, LMS authoring/player, student-shell, SSO button) = **G2★ human** (cần `bash kitehub/scripts/rebuild.sh kiteclass-frontend` + `kitehub-frontend`).

## Trạng thái flip (per g1-browser-walk-before-flip.md §1)

KHÔNG flip flow nào sang `walk-pass-pending-human`: G1 này là **gateway BE-contract walk**, chưa có browser-real walk (FE stale + down). BE-contract của cả 6 flow đã verify PASS — đủ để mở G2★ human browser-walk (theo 4 G2 recipe đã ship). Gap status giữ PARTIAL.

## Inline fixes shipped

1. **GAP-1297** — `LmsController` + `LessonProgressController`: `X-User-Id` → `X-User-Reference-Id` (5 chỗ). PARTIAL (BE fix + G1 PASS; FE G2 pending).
2. **GAP-1298** — `RoleService.listAssignments()` + `getAssignmentForUser()`: thêm `@Transactional(readOnly=true)`. PARTIAL (BE fix + G1 PASS; FE Bucket D assign UI G2 pending).

## Discovery không file mới (duplicate)

- **X-Teacher-Id spoofable** (PoC: STUDENT token + `X-Teacher-Id:3` → `POST /lms/courses/13/modules` → **201**) = instance của **GAP-798** (LMS/LessonProgress controller không bind actor-UUID ↔ numeric domain owner) + class GAP-1000. PoC empirical đã append vào GAP-798 thay vì file GAP-1299 (tránh duplicate per `audit-to-gap-pipeline.md` §2).
