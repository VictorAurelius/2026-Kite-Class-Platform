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

## Root-cause investigation (2026-06-05 — fix attempt + revert per release-fix-retry-budget §3.5)

Thử fix v1: thêm `@Filter(name="tenantFilter", condition="instance_id = :tenantId")` re-declared trên 4 entity (Course/Class/ClassSession/Teacher) matching pattern Lead/LandingPage. Rebuild + live re-test → **PARTIAL, reverted:**

| Endpoint | Service method | @Transactional? | Post-@Filter kết quả |
|---|---|---|---|
| `GET /teachers/{id}` | `getTeacherById` | ❌ none | 200 leak → **blocked** (nhưng 500 not 404) |
| `GET /courses/{id}` | `getCourseById` | ❌ none | filter applied (500 pre-existing user-U confound) |
| `GET /classes/{id}` | `getClass` | ✅ `@Transactional(readOnly=true)` | **STILL 200 leak** |
| List `/courses`, own-access | (Specification / non-txn) | — | ✅ no regression |

**ROOT CAUSE xác định:** `spring.jpa.open-in-view: false` (OSIV OFF) trong `application.yml:70`. `TenantFilterInterceptor.preHandle` enable filter qua `entityManagerProvider.getIfAvailable()` (OSIV/default session) + `filter.setParameter("tenantId", ...)`. Nhưng method `@Transactional` (vd `getClass`) mở **session riêng** mà filter chưa enable → leak. Method KHÔNG `@Transactional` (course/teacher/student) chạy trên session interceptor đã enable → filter áp dụng.

→ **`@Filter` trên entity là CẦN nhưng KHÔNG ĐỦ.** Phải đảm bảo filter enable trên transaction-bound session (mọi `@Transactional` read method platform-wide).

**Secondary bug:** khi filter trả empty, `.orElseThrow(EntityNotFoundException)` map ra **500 không phải 404** (leak existence + sai status) — cần verify exception→HTTP mapping + filter param-not-set trên non-interceptor session.

**Blast radius (sweep GAP-749):** 58 entity extends BaseEntity thiếu `@Filter` (chỉ 3 marketing entity Lead/LandingPage/ContactMessage có). Leak platform-wide: student/grade/payment/invoice/enrollment/attendance/... (Student by-id isolate được CHỈ vì `getStudentById` không `@Transactional`).

## Proposed Fix (dedicated security wave — không in-session, cần full IT validation)

3 layer, cần làm cùng + IT proof:
1. **Filter enablement reliable trên txn session** — chuyển enable từ MVC interceptor sang cơ chế bind vào actual Hibernate session của `@Transactional` (vd `TransactionSynchronization` / Hibernate `Integrator` / AOP `@Around` set filter trên `EntityManager` hiện hành). Đây là core fix cho `@Transactional` leak.
2. **`@Filter` applier trên 58 entity** thiếu (sweep GAP-749) — re-declare matching Lead pattern.
3. **Exception→404 mapping** — khi filter trả empty, `EntityNotFoundException` → 404 (verify handler), KHÔNG 500.
4. **Defense-in-depth:** cân nhắc RLS FORCE trên bảng nhạy cảm (hiện OFF trong kiteclass_shared).
- Re-enable + de-flake GAP-362 `TenantIsolationIT` + **mở rộng test coverage: thêm by-id isolation case** (hiện `shouldIsolateCourseDataBetweenTenants` CHỈ test LIST, không test findById — đó là coverage gap để lọt bug này). Regression guard cho cả `@Transactional` + non-txn read methods.

**Lý do defer (không fix in-session):** layer 1 (filter enablement) là infra change platform-wide, blast 58 entity + mọi `@Transactional` read, cần full kiteclass-core IT suite PASS (self-hosted runner OFFLINE) trước merge. Fix v1 attempt reverted để tránh ship half-fix (regress teacher→500 + getClass vẫn leak) per `release-fix-retry-budget.md` §3.

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
