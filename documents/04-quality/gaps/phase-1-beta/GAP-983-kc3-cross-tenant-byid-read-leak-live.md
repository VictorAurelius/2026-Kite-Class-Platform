# GAP-983: LIVE cross-tenant by-id read leak — course/class/session/teacher (KC-3 walk empirical proof)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (multi-tenant isolation — OWASP A01)
**Found:** 2026-06-05 (Wave flow-kc3 KC-3 G1 walk, production-equivalent local Docker stack)
**Affects:** `course`, `clazz`, `teacher` modules — mọi `findByIdAndDeletedFalse(id)` read path trong kiteclass-core dùng Hibernate `@Filter` cho tenant scope

## Problem

KC-3 G1 walk (course → class → schedule → sessions) trên stack production-equivalent **xác nhận LIVE cross-tenant data leak** trên các GET-by-id endpoint. Tenant `khanh-phapluat` (`126eaa8c`) đọc được data của tenant `sky-education` (`0edaee10`):

| Request (header X-Tenant-Id = khanh-phapluat) | Kết quả | Đúng phải là |
|---|---|---|
| `GET /api/v1/classes/14` (lớp của sky) | 🔴 **HTTP 200 + full data** "Lớp A1 - Ca tối T2-T4" | 404 |
| `GET /api/v1/classes/14/sessions` | 🔴 **HTTP 200 + 27 sessions** của sky | 404 |
| `GET /api/v1/courses/10/classes` | 🔴 **HTTP 200 + class 14** của sky | 404/empty |
| `GET /api/v1/teachers/10` | 🔴 **HTTP 200 + "Thầy Nguyễn Văn Minh"** của sky | 404 |
| `GET /api/v1/courses` (LIST, paginated) | ✅ chỉ trả course của chính khanh-phapluat | (isolated OK) |

**Root cause (empirical):**
- Isolation cơ chế = Hibernate `@FilterDef(name="tenantFilter")` trên `BaseEntity:43`, enable per-request tại `TenantFilterInterceptor:88` (`session.enableFilter("tenantFilter")`).
- Course/Class/Teacher `extends BaseEntity` NHƯNG by-id read dùng `repository.findByIdAndDeletedFalse(id)` (vd `CourseServiceImpl:153`) — Hibernate `@Filter` **không hiệu lực** trên path này → query trả entity của tenant khác.
- **LIST path an toàn** vì dùng explicit Specification predicate (`instance_id = currentTenant`), KHÔNG dựa `@Filter` → đó là lý do list isolated nhưng by-id leak.
- RLS trên `classes`/`courses`/`class_sessions` = **OFF** (`relrowsecurity=f` trong `kiteclass_shared`) → không có lớp phòng thủ thứ 2.

**Kiến trúc context:** kiteclass-core dùng **single shared DB** `kiteclass_shared` (`SPRING_DATASOURCE_URL`) + cột `instance_id` để isolate; per-tenant DB `kiteclass_0edaee10` (khai báo trong `instances.database_url`) được provision nhưng **KHÔNG dùng** (xem GAP-984). Mọi tenant pool chung 1 DB → isolation phụ thuộc HOÀN TOÀN vào app-layer filter → by-id leak = breach thật.

**Severity P0:** OWASP A01 IDOR + cross-tenant confidentiality breach. Mỗi tenant = 1 trường; tenant đọc được lớp/lịch/giáo viên của trường khác qua ID enumeration. PDPL personal data (tên giáo viên, lịch học sinh). Blocks beta launch.

## Quan hệ gap đã có (bug class đã track — đây là LIVE proof + escalation)

- **GAP-746** (OPEN P1): cùng bug class `findByIdAndDeletedFalse` thiếu tenant filter — đã document cho `EnrollmentRepository` + Invoice. GAP-983 mở rộng blast radius sang course/class/teacher/session + **escalate P1→P0** vì có LIVE empirical confirmation (không phải IT-hypothesis).
- **GAP-749**: 15-repo audit sweep cho missing tenant filter — GAP-983 là concrete evidence FOR sweep.
- **GAP-362** (OPEN P1): `TenantIsolationIT.shouldIsolateCourseDataBetweenTenants` flake — test đáng lẽ catch bug này đang disabled/flaky → giải thích vì sao leak lọt audit+test. Có thể test KHÔNG flaky mà đang **legit-fail**.
- **GAP-729** (DONE): A01 per-resource authz guard 11 controllers — guard `hasAccessTo*` cho owned mutating ops; KHÁC với tenant-scope trên read-by-id.

## Proposed Fix

Áp dụng systemic fix per GAP-746 Path A trên toàn bug class (course/class/session/teacher + sweep GAP-749):
- Đổi repository method → `findByIdAndInstanceIdAndDeletedFalse(id, tenantId)` HOẶC thêm `@Filter` applier (với `condition="instance_id = :tenantId"`) trên các entity + đảm bảo Hibernate filter hiệu lực trên findById path, HOẶC bật RLS FORCE trên các bảng như lớp phòng thủ thứ 2.
- Throw `EntityNotFoundException` (404) khi không thuộc tenant — KHÔNG 500/200.
- Re-enable + de-flake GAP-362 `TenantIsolationIT` → regression guard.

## Acceptance Criteria

- [ ] `GET /api/v1/classes/{id}`, `/sessions`, `/courses/{id}`, `/teachers/{id}` cross-tenant → **404** (not 200/500)
- [ ] Course/class/session/teacher by-id read scoped to caller tenant (verify với 2 tenant trên stack)
- [ ] `TenantIsolationIT.shouldIsolateCourseDataBetweenTenants` (GAP-362) re-enabled + PASS deterministic
- [ ] Cross-flow sweep per GAP-749 — mọi `findByIdAndDeletedFalse` repo audited + tenant-scoped

## Related

- Discovered in: Wave flow-kc3 KC-3 G1 walk (session 2026-06-05), evidence trong `documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc3-course-class-schedule.md`
- Parent bug class: [[GAP-746]], sweep [[GAP-749]], disabled guard test [[GAP-362]]
- Architecture discrepancy sibling: [[GAP-984]] (per-tenant DB provisioned but unused)
- Per `discovery-to-gap-inline-filing.md` §3 + `cross-flow-bug-class-sweep.md` (by-id leak bug class)
