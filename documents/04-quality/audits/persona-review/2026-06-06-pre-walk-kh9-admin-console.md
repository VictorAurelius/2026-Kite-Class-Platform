# Pre-Walk Persona Simulation — KH-9 Admin Console (instance / audit / beta-request management)

**Flow:** KH-9 — PLATFORM_ADMIN dùng admin console: dashboard → list/inspect instances → suspend/activate → audit logs → beta-requests list/approve/reject → impersonation.
**Date:** 2026-06-06
**Mandate:** `.claude/rules/pre-walk-persona-simulation-mandate.md` (prediction-only, KHÔNG fix).
**Stack:** gateway `:9000` (JWT HS512 → inject `X-User-Id` + `X-User-Roles`) → kitehub-admin (`/api/platform/admin/**` + `/api/v1/admin/**`) + kitehub-subscription (beta-requests + impersonate).

---

## Câu trả lời 2 câu hỏi headline

### (i) Gateway routing cho `/api/v1/admin/**` — KHÔNG phải walk-blocker ✅

Route TỒN TẠI và đúng thứ tự precedence (`kitehub-gateway/src/main/resources/application.yml`):
- L525 `kitehub-admin-beta-requests-v1` → `/api/v1/admin/beta-requests/**` → **kitehub-subscription** (đúng — BetaAccessController sống ở subscription)
- L539 `kitehub-admin-impersonate` → `/api/v1/admin/impersonate/**` → **kitehub-subscription** (đúng)
- L554 catch-all `/api/v1/admin/**` → **kitehub-admin** (đúng — AdminInstancesController + AdminAuditLogController)
- L284 `/api/platform/admin/**` → **kitehub-admin** (legacy AdminController suspend/activate)

Carve-outs precede catch-all → không misroute. **Routing không block walk.**

### (ii) Chuỗi role-literal PLATFORM_ADMIN — SOLID end-to-end ✅ (1 caveat)

```
TokenService.java:67  .claim("role", role)              role = user.getRole() = "PLATFORM_ADMIN"
  → JwtAuthGatewayFilter:182 claims.get("role")  :195 inject X-User-Roles: PLATFORM_ADMIN
    → XUserRolesHeaderFilter (admin + subscription) split(",") + prefix "ROLE_" → ROLE_PLATFORM_ADMIN
      → @PreAuthorize("hasRole('PLATFORM_ADMIN')")  ✅ MATCH
```

`@EnableMethodSecurity` xác nhận active ở CẢ kitehub-admin (`SecurityConfig:54`) và kitehub-subscription (`SecurityConfig:61`). Beta + impersonation endpoints THỰC SỰ guarded (không phải KH-8 hole). **Caveat (FM-4):** admin-service/beta/impersonation CHỈ chấp nhận `PLATFORM_ADMIN`, KHÔNG chấp nhận alias `ADMIN` (subscription's SubscriptionController/PaymentController/StaffInvitation DO accept `ADMIN`). Nếu seed user có role legacy `ADMIN` → 403 trên admin console (walk-blocker). Seed canonical (`seed-direct-sql.sh:21 SEED_ADMIN_ROLE=PLATFORM_ADMIN`) đúng, nhưng PHẢI verify.

---

## Ranked failure modes (confidence × impact)

### FM-1 🔴 Audit-log table drift: `admin_audit_log` (live) vs `admin_audit_logs` (immutable, có thể no-op)
- **(a) Where:** `audit/AdminAuditLog.java:24 @Table("admin_audit_log")` (singular) ← AdminAuditAspect WRITES + AdminAuditLogController READS qua `AdminAuditLogRepository`. NHƯNG `V50__rls_admin_bypass_null_force_fail_audit_logs.sql` tạo "immutable admin_audit_logs" (PLURAL). `BetaAccessRequestRepository.java:49` confirm "V50 only on admin_audit_logs". V36 tạo `admin_audit_log` (singular).
- **(b) Symptom:** Immutability/RLS hardening (V50) áp lên `admin_audit_logs` (plural) — table mà code KHÔNG đọc/ghi. Live audit table `admin_audit_log` (singular) có thể THIẾU immutability trigger → audit rows có thể UPDATE/DELETE (A09 tamper risk) + 1 table thừa gây nhầm lẫn.
- **(c) Pre-walk check:**
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c "\dt admin_audit_log*"
  docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT tablename FROM pg_tables WHERE tablename LIKE 'admin_audit_log%'; SELECT tgname,tgrelid::regclass FROM pg_trigger WHERE tgrelid::regclass::text LIKE 'admin_audit_log%';"
  ```

### FM-2 🔴 Suspend/activate KHÔNG ghi admin_audit_log (A09 audit-completeness gap)
- **(a) Where:** `AdminController.java:139-177` suspendInstance/activateInstance — chỉ `setStatus` + `publishEvent(SubscriptionDataChangedEvent)` cache-invalidation. KHÔNG có `@Auditable` (contrast: beta approve/reject CÓ `@Auditable` L187/L209; impersonation ghi audit cùng txn). `AdminInstancesController.java:36` confirm "Mutation operations (suspend/activate) remain on legacy".
- **(b) Symptom:** Admin suspend/activate 1 instance → KHÔNG có audit row → audit-log panel KHÔNG show được hành động privileged này. Compliance/forensics gap.
- **(c) Pre-walk check:** `grep -n "Auditable\|audit" AdminController.java` (expect 0 hits). Walk: suspend instance → `GET /api/v1/admin/audit-logs?action=INSTANCE_SUSPEND` → expect EMPTY (bug).

### FM-3 🟠 Suspend/activate chỉ ở legacy `/api/platform/admin/...` — FE có thể gọi sai path / không wire button → walk-blocker
- **(a) Where:** suspend/activate CHỈ tồn tại trên `AdminController` (`/api/platform/admin/instances/{id}/suspend|activate`). v1 `AdminInstancesController` = GET only. FE grep KHÔNG tìm thấy call-site action suspend/activate (`AdminInstancesTable.test.tsx` chỉ có "Tạm ngưng" như status FILTER, không phải action).
- **(b) Symptom:** Nếu FE gọi `/api/v1/admin/instances/{id}/suspend` → 404/405 (method không có trên v1 controller). Nếu UI không wire button → AC "admin suspends instance" UNREACHABLE từ console.
- **(c) Pre-walk check:**
  ```bash
  grep -rn "suspend\|activate\|platform/admin/instances" kitehub/kitehub-frontend/src/components/admin/ kitehub/kitehub-frontend/src/app/ | grep -iv test
  ```
  Walk: tìm nút suspend trong instances table; nếu có, DevTools Network → verify path = `/api/platform/admin/...` (PATCH).

### FM-4 🟠 Seed admin role phải đúng `PLATFORM_ADMIN` (alias `ADMIN` → 403 trên admin console)
- **(a) Where:** admin-service + beta + impersonation = `hasRole('PLATFORM_ADMIN')` literal only. `seed-sky-education-demo.sh:34` dùng `admin@kitehub.com`; `seed-direct-sql.sh` dùng `admin@kitehub.me` + role `PLATFORM_ADMIN`. Email/role mismatch giữa các seed scripts.
- **(b) Symptom:** Nếu local DB seed user role `ADMIN` (legacy) HOẶC walker login sai admin email → login OK nhưng MỌI admin console endpoint trả 403 (silent walk-blocker — login thành công làm tưởng nhầm).
- **(c) Pre-walk check:**
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT email, role FROM users WHERE role IN ('PLATFORM_ADMIN','ADMIN');"
  ```
  Verify đúng 1 row `PLATFORM_ADMIN` + biết email + password để login.

### FM-5 🟡 Suspend/activate state machine = no guard (double-suspend → 200 idempotent, không 409)
- **(a) Where:** `AdminController.java:146` `instance.setStatus(SUSPENDED)` vô điều kiện — không check current status.
- **(b) Symptom:** Suspend instance đã SUSPENDED → 200 (không 409 Conflict). Activate instance đang ACTIVE → 200. Không phải crash nhưng semantic lỏng; suspend KHÔNG cascade block tenant access — chỉ flip flag (verify gateway/middleware enforce SUSPENDED).
- **(c) Pre-walk check:** Walk: suspend 1 instance 2 lần liên tiếp → quan sát cùng 200. Sau suspend, thử login tenant đó → verify có bị block không (FE middleware `suspended.kitehub.me` → `/suspended`).

### FM-6 🟡 Suspend instance không tồn tại → 404 (OK) — verify không phải 500
- **(a) Where:** `AdminController:143` `.orElseThrow(EntityNotFoundException)` → `AdminExceptionHandler:34-37` maps → **404 NOT_FOUND** ✅ (đã verify mapping đúng). IllegalArgument → 400.
- **(b) Symptom:** Expect 404 cho id không tồn tại; nếu UUID malformed → 400 (MethodArgumentTypeMismatch — verify không leak 500).
- **(c) Pre-walk check:** `curl -X PATCH .../api/platform/admin/instances/00000000-0000-0000-0000-000000000000/suspend -H "Authorization: Bearer $ADMIN_JWT"` → expect 404. Thử id `abc` (malformed) → expect 400.

### FM-7 🟡 Schema drift cross-module: admin-service đọc Instance/Subscription của platform/subscription DB
- **(a) Where:** `AdminController.java:9-13` import `com.kitehub.platform.domain.entity.Instance` + `com.kitehub.subscription.repository.InstanceRepository` — admin service query trực tiếp entity của module khác. KH-6 đã trúng `branding_outbox.instance_id` đúng pattern này.
- **(b) Symptom:** Nếu Instance entity ↔ Flyway migration drift (cột thiếu/đổi tên) → GET /instances hoặc /dashboard 500 trên Postgres (H2/ddl-auto che ở IT — xem memory `kiteclass-core IT ddl-auto masks migration drift`).
- **(c) Pre-walk check:**
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c "\d instances" ; \
  grep -n "@Column\|private " kitehub/kitehub-platform/src/main/java/com/kitehub/platform/domain/entity/Instance.java
  ```
  Walk GET `/api/v1/admin/instances` + `/api/platform/admin/dashboard` → expect 200 không 500.

### FM-8 🟡 Beta-request state machine: double-approve / approve non-existent
- **(a) Where:** `BetaAccessController.java:192-227` approve/reject — IllegalArgument→404, IllegalState→409.
- **(b) Symptom:** Approve request đã APPROVED → expect 409 (qua IllegalStateException). Approve id không tồn tại → 404. Reject 1 request đã APPROVED → verify 409 không 500. Double-approve idempotency: token re-issue? (verify approveRequest không tạo duplicate invite token).
- **(c) Pre-walk check:** Walk: approve 1 PENDING request → 200; approve lại cùng id → expect 409. `GET /api/v1/admin/beta-requests?status=APPROVED` verify state flip.

### FM-9 🟡 Impersonation an toàn: 30s TTL + audit + non-admin reject
- **(a) Where:** `ImpersonationController.java:64/84/93` — 3 endpoint đều `@PreAuthorize('PLATFORM_ADMIN')`; javadoc claim "audit row cùng txn token mint — fail persist → token không trả". TTL 30s.
- **(b) Symptom:** Verify (1) non-admin gọi `/api/v1/admin/impersonate/{slug}` → 403; (2) start → audit row tạo + token TTL 30s thật; (3) impersonation_audit_log immutable (V48). Risk: scoped JWT có cho phép data access như tenant không? expired token sau 30s → 401.
- **(c) Pre-walk check:**
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c "\d impersonation_audit_log; SELECT count(*) FROM impersonation_audit_log;"
  ```
  Walk: start impersonate → check audit row + decode JWT exp (≈now+30s). Non-admin token → 403.

### FM-10 🟢 Dashboard/instance summary trả mock data (0 users/students/courses)
- **(a) Where:** `AdminController.java:261-263` convertToSummary hardcode `totalUsers(0L).totalStudents(0L).totalCourses(0L)` + `ownerEmail(null)` (comment "Mock data").
- **(b) Symptom:** Instance detail UI hiển thị 0 users/students/courses + owner email rỗng — misleading nhưng không crash. Walker đừng nhầm "0" là bug routing.
- **(c) Pre-walk check:** Walk GET instance detail → expect totals = 0 / ownerEmail null (documented, không phải lỗi).

### FM-11 🟢 RLS NOT FORCE'd trên kh-subscription — audit-log GET trả all-tenant (expected) nhưng không redaction
- **(a) Where:** `V50` comment: kh-subscription RLS KHÔNG flip `FORCE ROW LEVEL SECURITY` (HikariCP = table owner → bypass policy). Audit-log GET = platform-admin all-tenant (đúng scope).
- **(b) Symptom:** AdminAuditLogController trả mọi tenant audit (đúng cho platform admin) — verify không leak sensitive payload fields (token/password) trong `details` JSON column.
- **(c) Pre-walk check:** Walk `GET /api/v1/admin/audit-logs` → inspect response, verify không có raw secret trong details. `psql -c "SELECT details FROM admin_audit_log LIMIT 5"`.

---

## Tóm tắt cho walker

| # | Severity | 1-dòng | Loại |
|---|---|---|---|
| FM-1 | 🔴 | Audit table drift `admin_audit_log` (live) vs `admin_audit_logs` (immutable no-op?) | Verify psql trước |
| FM-2 | 🔴 | Suspend/activate KHÔNG audit (A09) | Walk + audit-log empty |
| FM-3 | 🟠 | Suspend/activate chỉ legacy path — FE wire? | Walk-blocker candidate |
| FM-4 | 🟠 | Seed phải `PLATFORM_ADMIN` (alias `ADMIN`→403) | Walk-blocker candidate |
| FM-5 | 🟡 | Double-suspend → 200 không 409 | Semantic |
| FM-6 | 🟡 | Suspend non-existent → 404 (verify không 500) | Validation |
| FM-7 | 🟡 | Cross-module schema drift Instance | 500 risk |
| FM-8 | 🟡 | Beta double-approve → 409 | State machine |
| FM-9 | 🟡 | Impersonation 30s TTL + audit + non-admin 403 | Security |
| FM-10 | 🟢 | Dashboard mock 0 totals | Cosmetic |
| FM-11 | 🟢 | RLS not-force + audit redaction | Info |

**Pre-walk MUST-run trước khi mở console:** FM-1 (table drift psql), FM-4 (seed role psql), FM-3 (FE suspend wire grep). 3 cái này quyết định walk có chạy được không.
