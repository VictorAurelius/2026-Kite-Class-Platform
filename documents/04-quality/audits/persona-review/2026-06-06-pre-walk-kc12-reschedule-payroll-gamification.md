# Pre-Walk Persona Simulation — KC-12 Reschedule + Payroll + Gamification + Analytics

**Flow:** KC-12 — flow MỎNG (thin). 3 surface user-facing thật: (1) **Payroll read-only** `GET /api/v1/admin/payroll/{configs,periods,periods/{id}}` (@PreAuthorize ADMIN, `PayrollController` + `PayrollServiceImpl`); (2) **Reschedule** `POST /api/v1/classes/{classId}/reschedule` (@authz.hasAccessToClass, mutate dates + audit + Outbox `class.rescheduled`, `ClassServiceImpl:497`); (3) **Gamification** `PointServiceImpl` (internal — KHÔNG có controller, award qua attendance event KC-5) + **Analytics** (KHÔNG có controller riêng — đã cover ở KC-11 ReportController). Side-effect: reschedule ghi `class_rescheduled` audit cols + Outbox row; payroll read-only (write = Phase 2 GAP-057b chưa ship).
**Date:** 2026-06-06
**Mandate:** `.claude/rules/pre-walk-persona-simulation-mandate.md` (prediction-only, KHÔNG fix).
**Stack:** gateway `:9000` → `kiteclass-core:8080`. Auth: OWNER token kitehub `/api/auth/login` (owner.test@test.vn). `TenantFilterInterceptor` bật Hibernate `tenantFilter` (`instance_id = :tenantId`) từ `X-Tenant-Id` header; `GatewayHeaderAuthenticationFilter` map `X-User-Roles` → `ROLE_*` (KC-11 confirmed ADMIN bridge works).

---

## ⚠️ Walk-blocker note đọc TRƯỚC: OWNER token KHÔNG vào được payroll

Cả 3 payroll endpoint là `@PreAuthorize("hasRole('ADMIN')")` (`PayrollController:62,89,120`) — yêu cầu **`ROLE_ADMIN`** chính xác. OWNER test token (`owner.test@test.vn`) carry role `OWNER` → `ROLE_OWNER`, **KHÔNG phải** `ROLE_ADMIN` → sẽ nhận **403** trên payroll. Walker PHẢI **mint ADMIN HS512 token** (`X-User-Roles: ADMIN`) qua gateway `JWT_SECRET` để walk payroll. OWNER token vẫn dùng được cho reschedule (`@authz.hasAccessToClass` admin-bypass nhận `ROLE_PLATFORM_ADMIN`/`ROLE_ADMIN` — verify OWNER có map admin-bypass không, xem FM-5).

---

## Câu trả lời 2 câu hỏi headline

### (i) Payroll có rò cross-tenant như KC-11 reports (GAP-1039) không? → **CÓ — cùng class y hệt** 🟠 (RECURRENCE)

`PayrollPeriod` + `PayrollConfig` đều `extends BaseEntity` (`PayrollPeriod.java:52`, `PayrollConfig.java:51`) → có `@Filter(name="tenantFilter", condition="instance_id = :tenantId")` (`BaseEntity.java:43-44`). NHƯNG cả 2 repository **dựa HOÀN TOÀN vào Hibernate `tenantFilter`** — KHÔNG có explicit `instance_id` predicate:
- `PayrollPeriodRepository.findByFilters` JPQL chỉ `WHERE p.deleted=false AND (teacherId) AND (startDate) AND (endDate)` — KHÔNG có `instance_id`.
- `findByIdAndDeletedFalse` + `findAllByDeletedFalse` = derived query, KHÔNG có `instance_id`.

Y hệt KC-11 FM-1: `TenantFilterInterceptor:79` CHỈ `enableFilter` khi `tenantHeader != null && !isBlank`. Thiếu header → `else` branch (`:98-100`) chỉ log "tenant filter not enabled", **KHÔNG reject**. Filter off → payroll query trả **TẤT CẢ tenant**. Payroll = lương giáo viên (`grossAmount`, `netAmount`, `hourlyRate`) — **high-sensitivity hơn cả reports**. Blast radius = phụ thuộc deployment shared `kiteclass_shared` vs per-tenant physical DB (cùng câu hỏi KC-11 — verify lúc walk).

### (ii) Reschedule `classId` có tenant-bound không (IDOR)? → **CÓ — qua filter** ✅ nhưng **admin-bypass + filter-off = lỗ tiềm ẩn** ⚠️

`POST /reschedule` = `@PreAuthorize("@authz.hasAccessToClass(#classId)")`. `findClassOrThrow` (`ClassServiceImpl:575`) dùng `classRepository.findByIdAndDeletedFalse(classId)` → `Class` entity có `@Filter` tenantFilter. Khi header có mặt: load class bị filter scope về tenant caller → tenant khác → 404. TEACHER còn double-gate (`hasAccessToClass` so `teacher_id` UUID == actor UUID). **NHƯNG** `AuthorizationBean.hasAccessToClass` có **admin-bypass**: `ROLE_PLATFORM_ADMIN`/`ROLE_ADMIN` → return `true` cho MỌI tenant resource — tenant binding của admin chỉ còn dựa vào `@Filter`. Filter-off (curl thẳng `:8080` không header, hoặc TenantResolver fallback null) → admin reschedule lớp tenant khác. Cùng root cause FM-1/(i): defense chỉ-1-lớp Hibernate filter.

---

## Ranked failure modes

### FM-1 🟠 Payroll cross-tenant leak khi `X-Tenant-Id` absent — KC-11 GAP-1039 RECURRENCE (highest value)
- **(a) Where:** `module/payroll/repository/PayrollPeriodRepository.java` (`findByFilters` JPQL KHÔNG có `instance_id` predicate) + `PayrollConfigRepository.java` (`findAllByDeletedFalse` + `findByTeacherIdAndDeletedFalse` derived, KHÔNG có `instance_id`). `PayrollServiceImpl.listConfigs():180` + `listPeriods():189` dựa hoàn toàn vào `@Filter` từ `BaseEntity:43-44`. `TenantFilterInterceptor:79,98-100` — thiếu header → filter off, KHÔNG reject.
- **(b) Symptom walker thấy:** Qua gateway `instance-apis` áp `TenantResolver` set `X-Tenant-Id` → scoped đúng (bình thường). NHƯNG: (a) curl THẲNG `kiteclass-core:8080/api/v1/admin/payroll/periods` (bypass gateway, không `X-Tenant-Id`, chỉ `X-User-Roles: ADMIN`) → trả periods/configs của **TẤT CẢ tenant** (lương GV mọi trường); (b) nếu `TenantResolver` fallback null (localhost/apex không subdomain, JWT thiếu tenantId claim) → cùng leak. ADMIN role-gate (`hasRole('ADMIN')`) chỉ chặn non-admin, KHÔNG chặn cross-tenant.
- **(c) Pre-walk check:**
  ```bash
  # 1) Payroll query KHÔNG có instance_id predicate (dựa Hibernate filter)
  grep -rn "instance_id\|instanceId\|WHERE\|@Query" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payroll/repository/*.java
  # 2) Interceptor: thiếu X-Tenant-Id → filter off, KHÔNG reject (đã verify :79,:98-100)
  sed -n '77,100p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/config/TenantFilterInterceptor.java
  # 3) Walk: curl payroll periods KHÔNG có X-Tenant-Id (direct :8080 nếu expose) → expect leak vs reject
  curl -s -i -H "X-User-Roles: ADMIN" -H "X-User-Id: <uuid>" \
    "http://localhost:8080/api/v1/admin/payroll/periods" | head -25
  # 4) Verify deployment: payroll_periods nhiều tenant cùng 1 DB?
  docker exec kite-postgres psql -U <user> -d <db> -c "SELECT DISTINCT instance_id FROM payroll_periods LIMIT 5"
  ```

### FM-2 🟠 Payroll `periods/{id}` IDOR — `getPeriodById` filter-gated only (cùng root FM-1)
- **(a) Where:** `PayrollController.getPeriod():119-128` → `PayrollServiceImpl.getPeriodById():173` → `payrollPeriodRepository.findByIdAndDeletedFalse(id)` (derived query, KHÔNG `instance_id`). Tenant binding của `{id}` chỉ dựa `@Filter` từ `BaseEntity`.
- **(b) Symptom:** Header có mặt → filter scope entity load → ADMIN tenant A đọc period id của tenant B → filter loại → `EntityNotFoundException PAYROLL_PERIOD_NOT_FOUND` 404 (ĐÚNG). Header absent (curl thẳng :8080) → ADMIN đọc period id BẤT KỲ tenant nào → leak lương 1 GV cụ thể. Khác FM-1 (list leak) ở chỗ đây là targeted single-record IDOR. Cùng defense gap (1-lớp filter).
- **(c) Pre-walk check:**
  ```bash
  sed -n '171,176p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payroll/service/impl/PayrollServiceImpl.java
  # Walk: với X-Tenant-Id của tenant A, GET periods/{id_của_tenant_B} → expect 404 (filter gates)
  # Walk: KHÔNG header, GET periods/{any_id} → expect leak (filter off)
  curl -s -i -H "X-User-Roles: ADMIN" "http://localhost:8080/api/v1/admin/payroll/periods/1" | head -15
  ```

### FM-3 🟡 Gamification `StudentPoint` KHÔNG có `@Filter` tenantFilter → cross-tenant SUM via studentId collision (internal, no controller)
- **(a) Where:** `module/gamification/entity/StudentPoint.java:38` — `public class StudentPoint {` **KHÔNG `extends BaseEntity`** (plain `@Entity`, có `instance_id NOT NULL` col `:47-48` nhưng KHÔNG có `@FilterDef`/`@Filter`). `StudentPointRepository.getTotalPointsByStudentId` JPQL = `SELECT SUM(sp.points) FROM StudentPoint sp WHERE sp.studentId = :studentId` — CHỈ filter `studentId`, **KHÔNG `instance_id`**. `PointServiceImpl.awardAttendancePoints():34` set `instanceId(TenantContext.getCurrentTenant())` lúc ghi, nhưng read KHÔNG filter tenant.
- **(b) Symptom:** KHÔNG có controller → KHÔNG walk trực tiếp được. Award trigger từ `AttendanceServiceImpl:116` (KC-5 attendance flow). `studentId` là numeric `Long` per-tenant (mỗi tenant đánh số student 1,2,3...). `getTotalPointsByStudentId(1)` → SUM points của student id=1 **ở MỌI tenant** (collision numbering) → điểm gamification của student tenant A cộng nhầm điểm student id=1 tenant B. Latent integrity bug — chỉ lộ nếu Wave sau thêm endpoint `GET /students/{id}/points` đọc qua method này. Thêm: nếu attendance award khi `TenantContext.getCurrentTenant()` null → insert `instance_id = null` vào NOT NULL col → fail (hoặc rollback attendance — verify KC-5 không null context).
- **(c) Pre-walk check:**
  ```bash
  # Confirm StudentPoint KHÔNG extends BaseEntity + repo KHÔNG instance_id
  grep -n "class StudentPoint\|extends\|@Filter" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/gamification/entity/StudentPoint.java
  grep -n "instance_id\|instanceId\|WHERE" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/gamification/repository/StudentPointRepository.java
  # Confirm NO controller (internal only)
  find kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/gamification -name "*Controller.java"  # expect empty
  ```

### FM-4 🟡 Reschedule Outbox `tenantId` null khi `TenantContext` null → Wave 106 dispatcher mis-attribution (latent, như KC-11 FM-4)
- **(a) Where:** `ClassServiceImpl.rescheduleClass():534` `UUID tenantId = TenantContext.getCurrentTenant();` → `:537` `tenantId != null ? tenantId.toString() : null` đẩy vào `ClassRescheduledEvent`. Outbox enqueue (`:554`) ghi event với `tenant = null` nếu header absent.
- **(b) Symptom:** Reschedule thành công (200 + audit cols ghi) NHƯNG Outbox row `class.rescheduled` có `tenant=null` khi không có X-Tenant-Id. Wave 106 dispatcher đọc outbox + resolve recipient → KHÔNG biết tenant → mis-attribution / cross-tenant notification mix. Latent (consumer side Phase 1.5+, recipient lists ship EMPTY v1.0.0 per `:548-549,:492-493`). Walker hiện chỉ thấy 1 row outbox + log "Class rescheduled". KHÔNG email/MailHog trong scope.
- **(c) Pre-walk check:**
  ```bash
  sed -n '530,560p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/service/impl/ClassServiceImpl.java
  # Sau walk reschedule: kiểm outbox row tenant null không
  docker exec kite-postgres psql -U <user> -d <tenant-db> \
    -c "SELECT aggregate_id, event_type, payload->>'tenantId' FROM outbox_event WHERE event_type='class.rescheduled' ORDER BY id DESC LIMIT 3"
  ```

### FM-5 🟡 Reschedule admin-bypass authz — `hasAccessToClass` cho ADMIN/OWNER return true mọi tenant; tenant binding chỉ còn `@Filter`
- **(a) Where:** `common/security/AuthorizationBean.hasAccessToClass(#classId)` — admin-bypass: `ROLE_PLATFORM_ADMIN`/`ROLE_ADMIN` → `true` cho MỌI tenant resource (javadoc "admin can read/write any tenant resource"). Non-admin → so `classes.teacher_id` UUID == `UserContext.getCurrentUser()` UUID (GAP-795). `findClassOrThrow:576` `findByIdAndDeletedFalse` → `Class` `@Filter` gates tenant.
- **(b) Symptom:** Header có mặt → admin reschedule chỉ tenant mình (Class load filter scope đúng) → OK. Header absent (curl thẳng :8080) → admin-bypass + filter-off → admin reschedule lớp BẤT KỲ tenant. TEACHER an toàn hơn (teacher_id UUID match). **Câu hỏi walk quan trọng:** OWNER test token có role `OWNER` → `ROLE_OWNER` — KHÔNG khớp admin-bypass (`ROLE_ADMIN`/`ROLE_PLATFORM_ADMIN`) NÊN OWNER reschedule lớp KHÔNG phải của mình → deny (`hasAccessToClass` false, vì OWNER không là teacher_id). Verify lúc walk: OWNER có reschedule được lớp seed không (có thể 403 nếu OWNER ≠ teacher của lớp + không phải admin-bypass).
- **(c) Pre-walk check:**
  ```bash
  sed -n '/boolean hasAccessToClass/,/^    }/p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/security/AuthorizationBean.java
  # Walk: ADMIN token reschedule lớp seed → 200; OWNER (không teacher_id) → có thể 403; cross-tenant (no header) → leak
  ```

### FM-6 🟡 Reschedule date validation YẾU — `validateDates` chỉ check `end > start`; KHÔNG check past-date dù DTO javadoc claim "≥ today"; KHÔNG conflict detection
- **(a) Where:** `ClassServiceImpl.validateDates():580-584` chỉ `if (!endDate.isAfter(startDate)) throw CLASS_INVALID_DATES`. `RescheduleClassRequest` javadoc claim "`newStartDate` required, must be ≥ today" NHƯNG annotation chỉ `@NotNull` — KHÔNG có `@FutureOrPresent`. Reschedule chỉ allow khi `clazz.canEditSchedule()` (status SCHEDULED, BR-CLASS-006 `:505`).
- **(b) Symptom:** TEACHER/ADMIN POST reschedule với `newStartDate` trong QUÁ KHỨ (vd `2020-01-01`) + `newEndDate > newStartDate` → PASS validation → lớp dời về quá khứ (javadoc nói cấm nhưng code không enforce). KHÔNG có session/room conflict detection (dời trùng lịch lớp khác). Severity moderate — data-quality bug, không security. Walker: thử reschedule `newStartDate` past → expect 400 (theo javadoc) nhưng thực tế 200 (bug).
- **(c) Pre-walk check:**
  ```bash
  grep -n "FutureOrPresent\|@NotNull\|isBefore.*now\|LocalDate.now" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/dto/RescheduleClassRequest.java \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/service/impl/ClassServiceImpl.java
  # Walk: POST reschedule newStartDate=2020-01-01 → expect 400 (javadoc) vs 200 (bug)
  ```

### FM-7 🟢 Reschedule + Outbox atomic — serialization fail roll back reschedule (PASS, KHÔNG inconsistent state)
- **(a) Where:** `rescheduleClass()` `@Transactional` (`:496`). `classRepository.save(clazz)` (`:530`) + `outboxEventWriter.enqueue` (`:554`) CÙNG txn ("outbox is the reliability net" `:532`). Nếu `objectMapper.writeValueAsString` fail → `throw ValidationException RESCHEDULE_EVENT_SERIALIZATION_FAILED` (`:562`) → toàn txn rollback → reschedule REVERT.
- **(b) Symptom:** Outbox publish fail KHÔNG để lại state lệch (date đã đổi nhưng event mất) — cả hai roll back cùng nhau. Đây là note "ĐÃ AN TOÀN" để walker đừng chase. Đúng Outbox pattern (`design-patterns.md` §3.5.1). Contrast: notification fail KHÔNG block reschedule vì recipient lookup ở consumer side (empty list v1.0.0).
- **(c) Pre-walk check:**
  ```bash
  grep -n "@Transactional\|outboxEventWriter.enqueue\|reliability net\|RESCHEDULE_EVENT_SERIALIZATION" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/service/impl/ClassServiceImpl.java
  ```

### FM-8 🟢 Payroll write + Analytics + Gamification = KHÔNG walkable surface (thin-flow scope confirm)
- **(a) Where:** (1) Payroll **write** (`POST /run`, `/approve`, `/pay`, payslip PDF, bank export) = Phase 2 GAP-057b **CHƯA SHIP** — `PayrollController` chỉ 3 GET read-only (`:61,88,119`). `PayrollServiceImpl.calculate()` tồn tại nhưng KHÔNG có controller endpoint (internal/test only). (2) **Analytics** — KHÔNG có controller riêng; chỉ `ReportController` (revenue/attendance, đã walk KC-11) + `StudentPortalController`. (3) **Gamification** — KHÔNG có controller (xác minh `find .../gamification -name "*Controller.java"` = 0 hit).
- **(b) Symptom:** Walker KHÔNG nên tìm payroll write button / analytics dashboard / gamification points page — chúng KHÔNG tồn tại ở KC-12 scope. KC-12 walk thật = (a) payroll 3 GET read-only (cần ADMIN token), (b) reschedule POST. Đừng coi "không có payroll approve" / "không có points endpoint" là regression — là Phase 2 / internal scope.
- **(c) Pre-walk check:**
  ```bash
  grep -rn "@PostMapping\|@PutMapping\|/run\|/approve\|/pay" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payroll/controller/PayrollController.java || echo "payroll = GET read-only only (Phase 1)"
  find kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/gamification -name "*Controller.java" || echo "no gamification controller — internal"
  ```

---

## Tóm tắt cho walker

| # | Severity | 1-dòng | Loại |
|---|---|---|---|
| FM-1 | 🟠 | Payroll `tenantFilter` không bật khi thiếu `X-Tenant-Id` → list lương cross-tenant (KC-11 GAP-1039 recurrence) | Tenant-isolation (high-value real) |
| FM-2 | 🟠 | `getPeriodById` filter-gated only → targeted IDOR lương 1 GV khi filter off | Tenant-isolation / IDOR |
| FM-3 | 🟡 | `StudentPoint` KHÔNG `extends BaseEntity` / KHÔNG `@Filter` + SUM by studentId → cross-tenant point collision (internal, no controller) | Data-integrity (latent) |
| FM-4 | 🟡 | Reschedule Outbox `tenantId=null` khi context null → Wave 106 dispatcher mis-attribution | Data-integrity (latent) |
| FM-5 | 🟡 | `hasAccessToClass` admin-bypass mọi tenant; OWNER token có thể 403 reschedule (không teacher_id, không admin) | Authz / walk-blocker |
| FM-6 | 🟡 | `validateDates` chỉ check end>start; past-date reschedule PASS dù javadoc cấm; không conflict detect | Validation gap |
| FM-7 | 🟢 | Reschedule + Outbox cùng txn → serialization fail roll back reschedule (PASS — đừng chase) | Outbox ✅ |
| FM-8 | 🟢 | Payroll write (Phase 2) + Analytics + Gamification = KHÔNG walkable; chỉ 3 payroll GET + reschedule POST | Scope-setting ✅ |

**Pre-walk MUST-run trước khi mở flow (4 check quyết định walk + risk thật):**

1. **FM-1 payroll tenant scoping (cao nhất, KC-11 recurrence)** — `grep -rn "instance_id\|@Query" .../payroll/repository/*.java` (xác nhận dựa Hibernate filter, KHÔNG explicit predicate) + `sed -n '77,100p' TenantFilterInterceptor.java` (thiếu header → filter off, không reject) + verify deployment `docker exec kite-postgres psql -c "SELECT DISTINCT instance_id FROM payroll_periods"`. **Quyết định: payroll list lương có rò cross-tenant khi thiếu X-Tenant-Id không, blast radius = shared DB hay per-tenant DB.** File: `module/payroll/repository/PayrollPeriodRepository.java` + `PayrollConfigRepository.java` + `config/TenantFilterInterceptor.java:79,98-100`.
2. **Walk-blocker ADMIN token** — payroll = `hasRole('ADMIN')` (`PayrollController:62,89,120`); OWNER test token = `ROLE_OWNER` ≠ `ROLE_ADMIN` → 403. **MINT ADMIN HS512 token trước khi walk payroll.** File: `module/payroll/controller/PayrollController.java:62`.
3. **FM-5 reschedule classId tenant-bound + authz** — `sed -n '/boolean hasAccessToClass/,/^    }/p' AuthorizationBean.java` + walk ADMIN reschedule lớp seed → 200; OWNER (không teacher_id) → có thể 403; no-header cross-tenant → leak. **Quyết định: reschedule classId có tenant-bound qua filter không, admin-bypass có lỗ filter-off không, OWNER token walk được reschedule không.** File: `common/security/AuthorizationBean.java` + `ClassServiceImpl.java:575-578` + `ClassController.java:223-232`.
4. **FM-6 reschedule validation** — `grep -n "FutureOrPresent\|isBefore.*now" RescheduleClassRequest.java ClassServiceImpl.java` + walk POST reschedule `newStartDate=2020-01-01` → expect 400 (javadoc) vs 200 (bug). **Quyết định: reschedule có cho dời về quá khứ không (validation gap).** File: `module/clazz/dto/RescheduleClassRequest.java` + `ClassServiceImpl.java:580-584`.

**Headline:** Payroll **CÓ tái phát KC-11 cross-tenant leak class** (FM-1/FM-2 — repo dựa 100% Hibernate `tenantFilter`, không explicit `instance_id`, lương GV high-sensitivity) — finding cao giá trị nhất. Reschedule `classId` **tenant-bound qua filter** (✅) nhưng admin-bypass + filter-off để lại lỗ tiềm ẩn (FM-5). Gamification + Analytics = **KHÔNG có walkable surface** (FM-8 — gamification internal no-controller, analytics = ReportController KC-11, payroll write = Phase 2). Thin-flow xác nhận đúng: walk thật chỉ = 3 payroll GET (cần ADMIN token) + 1 reschedule POST.
