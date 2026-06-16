---
title: Pre-Walk Persona Simulation — 5 priority KH/KC flows
audience: dev
created: 2026-06-15
scope: Predict failure modes BEFORE human G2★ walk cho 5 flow ưu tiên (SSO / LMS / RBAC / Attendance / Owner reports)
mode: PREDICT-ONLY (no source edit — main stack rebuilding)
references:
  - .claude/rules/pre-walk-persona-simulation-mandate.md
  - .claude/rules/g1-browser-walk-before-flip.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1138-cross-product-sso-kh-kc-impl.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1115-lms-paywall-bypass-course-structure.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1119-kc-role-based-login-routing-rbac-shell.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1066-v87-attendance-status-normalize-crashloop.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1139-kc-owner-not-tenant-admin-403.md
---

# Pre-Walk Persona Simulation — 5 priority flows (2026-06-15)

> **Mục đích:** dự đoán failure mode TRƯỚC khi human walk G2★ qua browser, để coordinator batch-fix các bug HIGH-confidence trước. Mỗi failure mode có: (a) where, (b) symptom, (c) pre-walk check (chạy được qua gateway `:9000` / psql / grep), (d) confidence + fix đề xuất.
>
> **Lưu ý chung — biến môi trường (điều chỉnh theo seed thật):**
> ```
> GW=http://localhost:9000          # shared gateway
> SKY=0edaee10-2d13-44be-9151-12b78b7c5fd4   # tenant sky-education (attendance recipe)
> AAAABBBB=aaaabbbb-0000-0000-0000-000000000001  # tenant skytest (SSO recipe)
> ```
> **Stack vừa rebuild → chạy `bash kitehub/scripts/status.sh` đầu tiên; nếu `kiteclass-core` KHÔNG healthy → đọc Flow 4 §FM-1 NGAY (V87 crash-loop là P0 chặn TOÀN BỘ KC).**

---

## Cross-flow risk #0 — stale Docker image (áp dụng cả 5 flow)

Đa số fix của 5 flow nằm trên `main` nhưng gap status PARTIAL "human re-walk pending **sau rebuild**". Nếu image cũ hơn commit fix → walk gặp lại bug đã fix (false-fail). **Pre-walk check (chạy TRƯỚC mọi flow):**

```bash
bash scripts/check-stale-images.sh    # liệt kê service có image cũ hơn source commit
# HOẶC thủ công so image-created-time vs git log:
docker images --format '{{.Repository}}\t{{.CreatedAt}}' | grep -E 'kiteclass-core|kitehub-subscription|kitehub-frontend|kiteclass-frontend'
git -C . log -1 --format='%ci %h' -- kiteclass/kiteclass-core kitehub/kitehub-subscription
```

Service phải rebuild trước walk: **kiteclass-core** (Flow 2/3/4/5 — chứa fix GAP-1115/1116/1139/1297/1298/1299 + V87), **kitehub-subscription** + **kitehub-frontend** + **kiteclass-frontend** (Flow 1 SSO chain). HIGH confidence — đây là nguyên nhân #1 của false-fail walk.

---

## Flow 1 — SSO KiteHub → KiteClass (GAP-1138 70% / GAP-1305 85%)

**Persona:** Owner KiteHub login `:3001` → click "Mở quản lý trường" → vào KC `:3000` owner-shell không re-login.
**Chuỗi:** kitehub-frontend `:3001` → kitehub-subscription (issue-code) → redirect KC `:3000/sso/callback` → exchange → KC session.

| # | (a) Where | (b) Symptom human thấy | (c) Pre-walk check | (d) Confidence + fix |
|---|---|---|---|---|
| **FM1-1** | KH owner credential chưa seed (`seed-kh-owner-sso.sql`) | `:3001/dashboard` đá thẳng `/login`, nút "Mở quản lý trường" không render → walk dừng bước 1 | `curl -s -X POST $GW/api/auth/login -H 'Content-Type: application/json' -d '{"email":"sso.owner@skytest.test","password":"Test@1234"}'` → kỳ vọng 200 + accessToken | **HIGH** — chạy `docker exec -i kite-postgres psql -U kitehub -d kitehub < kitehub/scripts/seed-kh-owner-sso.sql` trước walk |
| **FM1-2** | `AuthService.resolveTenantIdForRole` `findFirst()` không ORDER BY (GAP-1306) | SSO land tenant RỖNG (không có KC data) hoặc lúc đúng lúc sai (non-deterministic) nếu dùng owner đa-instance | Decode JWT `tenantId` claim từ login response, kỳ vọng `=aaaabbbb...`; chạy 2-3 lần xem có đổi không | **HIGH** — BẮT BUỘC dùng `sso.owner@skytest.test` (single-instance), KHÔNG `owner.test@test.vn` (2 instance). Caveat trong recipe §2.1 |
| **FM1-3** | Stale FE images (`kitehub-frontend` / `kiteclass-frontend`) | Nút render nhưng click không redirect / callback page trắng / ERR_EMPTY_RESPONSE | `curl -s -o /dev/null -w '%{http_code}' http://localhost:3001` + `http://localhost:3000` → kỳ vọng 200 (không phải empty) | **HIGH** — rebuild 2 FE container (GAP-1067 class) |
| **FM1-4** | `JWT_SECRET` không chung KH/KC (ADR-039) | Exchange trả token 200 NHƯNG call KC nghiệp vụ sau đó 401/400 (gateway `TenantHeaderGuardFilter` reject token KH-minted) | `TOK=$(curl -s -X POST $GW/api/auth/login ...|jq -r .accessToken); curl -s -X POST $GW/api/v1/auth/sso/issue-code -H "Authorization: Bearer $TOK"` → 200 + code; rồi exchange → 200 + token; rồi dùng token đó gọi 1 endpoint KC `/api/v1/...` → kỳ vọng KHÔNG 401 | **MED** — verify env `JWT_SECRET` giống nhau giữa kitehub-subscription + kiteclass-core + gateway |
| **FM1-5** | Opaque code single-use + TTL ≤60s (đúng design) | User F5 trang callback HOẶC mở lại link → "Đăng nhập SSO thất bại / mã hết hạn" → user tưởng bug | exchange code lần 2 → kỳ vọng 401 (replay reject, ĐÚNG); chỉ cần đảm bảo recipe nói rõ "F5 callback = phải click nút lại" | **MED** — không phải bug; recipe §3 Bước 4 đã cover. Đảm bảo error message tiếng Việt rõ |
| **FM1-6** | Token-in-URL leak | URL redirect mang JWT 3-phần thay vì opaque code ngắn | Sau issue-code, kiểm URL redirect (recipe Bước 2): `?code=<opaque>` KHÔNG phải `eyJ...` | **MED** (security) — opaque code đã verified BE-contract; browser walk confirm |
| **FM1-7** | Cross-origin `:3001`→`:3000` hard-navigate | Callback không nhận code / CORS block / localStorage 2 origin lẫn | DevTools: sau click, URL bar = `localhost:3000/sso/callback?code=...`; Local Storage `localhost:3000` có `kc:<tenantId>:accessToken` | **MED** — browser-walk-only (curl không bắt được); G2★ human |

**Tóm tắt Flow 1:** 3 HIGH (credential seed / non-determinism / stale FE). Đây là flow rủi ro nhất vì chuỗi 3 service + cross-origin. KHÔNG flip DONE qua curl (per `g1-browser-walk-before-flip.md` §3.2 access-mode parity — SSO redirect là access-mode).

---

## Flow 2 — LMS paywall + teacher/student (GAP-1115/1116 85%, GAP-1113 60%)

**Personas:** teacher authoring (X-Teacher-Id), guest catalog (paywall CTA), enrolled student (lesson player + mark-complete).

| # | (a) Where | (b) Symptom | (c) Pre-walk check | (d) Confidence + fix |
|---|---|---|---|---|
| **FM2-1** | FE↔BE contract drift (GAP-1069 class) — dashboard gọi endpoint BE không expose | Trang dashboard/catalog 404 vài request, list rỗng | `bash scripts/check-fe-be-api-contract.sh` (nếu có); HOẶC grep FE call sites: `grep -rnE "/api/v1/(lms|courses|classes|invoices)" kiteclass/kiteclass-frontend/src/lib` rồi đối chiếu với `LmsController`/`LessonProgressController` @*Mapping | **HIGH** — FE `lib/api/lms.ts` gọi `/api/v1/lms/modules,lessons,resources`; verify mỗi path có @Mapping BE |
| **FM2-2** | Student mark-complete header `X-User-Id`→`X-User-Reference-Id` (GAP-1297) | Student click "Hoàn thành bài" → 403 / progress ghi sai user | `curl -s -X POST "$GW/api/v1/lms/lessons/<id>/complete" -H "Authorization: Bearer <student-jwt>"` → kỳ vọng 200 (gateway tự inject X-User-Reference-Id từ JWT). Nếu FE còn gửi header tay → drift | **HIGH** — verify gateway `JwtAuthenticationGatewayFilter` inject `X-User-Reference-Id` (đã confirm dòng 237) + FE KHÔNG override |
| **FM2-3** | Paywall strip `content=null`/`videoUrl=null` cho bài paid chưa enroll (GAP-1115) | FE lesson viewer crash / blank khi content=null (không guard null) | Guest gọi `GET $GW/api/v1/lms/courses/<courseId>/modules` → bài `isTrial=false` trả `content:null`; verify FE render "Cần ghi danh" thay vì crash | **MED** — fix BE đã ship; rủi ro là FE chưa handle null gracefully. Browser-walk guest → bài paid |
| **FM2-4** | Tenant resolution browser gửi `X-Tenant-Id` → gateway strip (GAP-1068 class) | Mọi call LMS qua browser → 400 | Browser DevTools Network: request LMS có `X-Instance-Subdomain` (hoặc JWT tenantId), KHÔNG phải `X-Tenant-Id` gắn tay | **HIGH** (browser-only) — curl gắn header che mất; phải browser-walk per g1-browser-walk §3 |
| **FM2-5** | Student-auth KC-9 chưa provision credential (GAP-1277) | Student login `:3000/login` → 401 → không vào được lesson player (Increment B) | `docker exec kite-postgres psql -U kite -d kiteclass_shared -c "SELECT email,entity_type FROM auth_credentials WHERE entity_type='STUDENT' LIMIT 3;"` → nếu rỗng phải provision | **HIGH** — provision qua `POST /api/v1/students/{id}/credentials` (recipe RBAC §2.2) trước walk student |
| **FM2-6** | Teacher authoring spoof `X-Teacher-Id` (GAP-798) | Teacher A sửa được module của teacher B (IDOR) | `curl` create module với `X-Teacher-Id` của teacher KHÔNG sở hữu course → kỳ vọng 403 | **MED** — authz check; verify ownership guard active |
| **FM2-7** | completeLesson bài paid chưa enroll (GAP-1116) | Student chưa ghi danh vẫn mark-complete bài paid → progress sai | `curl POST .../lessons/<paid-lesson>/complete` với student chưa enroll → kỳ vọng 403 `STUDENT_NOT_ENROLLED_IN_COURSE` | **MED** — fix shipped (LessonAccessGuard); verify |

**Tóm tắt Flow 2:** 4 HIGH (contract drift / header rename / tenant browser / student credential). 2 phụ thuộc browser thật (FM2-4) + provisioning (FM2-5).

---

## Flow 3 — RBAC role-shell (GAP-1119 85%)

**Personas:** ADMIN/OWNER/TEACHER/STUDENT/PARENT login → role-home đúng + cross-role guard.

| # | (a) Where | (b) Symptom | (c) Pre-walk check | (d) Confidence + fix |
|---|---|---|---|---|
| **FM3-1** | JWT `role` claim vocabulary mismatch vs `roles.ts normalizeRole` | Login OK nhưng redirect SAI role-home (vd teacher về `/dashboard` thay vì `/teacher`) vì `normalizeRole` trả null → fallback `/dashboard` | Decode JWT `role` claim sau login mỗi role; đối chiếu với `roles.ts` CANONICAL (OWNER/ADMIN/STAFF/TEACHER/PARENT/STUDENT) + ALIASES (TENANT_OWNER/PRINCIPAL/...). Claim phải map được | **HIGH** — nếu BE emit `TENANT_OWNER` → FE map OWNER OK; nếu emit token lạ không trong map → null → wrong home |
| **FM3-2** | TEACHER/STUDENT credential chưa provision | Login 401 → walk bước 2/3 dừng | `psql ... "SELECT email,entity_type FROM auth_credentials ORDER BY created_at DESC LIMIT 5;"` → có TEACHER + STUDENT row? | **HIGH** — provision per recipe §2.2 trước walk |
| **FM3-3** | RoleGuard leak — `(dashboard)/admin/layout.tsx` | Teacher gõ URL `/admin/roles` VÀO được + thấy nút gán role → authz leak | `curl -s -o /dev/null -w '%{http_code}' "$GW/api/v1/roles/templates" -H "Authorization: Bearer <teacher-jwt>"` → kỳ vọng 403 (BE gate); browser: teacher → `/admin/roles` bị chặn | **HIGH** (BE 403 confirmed G1) — browser RoleGuard FE chưa walk; G2★ |
| **FM3-4** | Assign/revoke role mutation LazyInit (GAP-1298) | Owner gán role → 500 (LazyInitializationException) | `curl -s -X POST "$GW/api/v1/roles/assignments" -H "Authorization: Bearer <owner-jwt>" -H 'Content-Type: application/json' -d '{"userId":<n>,"roleTemplate":"TEACHER"}'` → kỳ vọng 200/201, KHÔNG 500 | **HIGH** — fix shipped GAP-1298; verify image rebuilt (nếu kc-core stale → 500 lại) |
| **FM3-5** | Role-home redirect 5 role (`ROLE_HOME` map) | STUDENT `/student` 404 / redirect loop (student-shell gated KC-9) | Browser login student → URL `/student` render? Nếu 404 → student-shell chưa build đúng | **MED** — student-shell scaffold gated KC-9; có thể defer |
| **FM3-6** | STAFF session phụ thuộc SSO (Flow 1) | Không test được STAFF scope nếu SSO chưa thông | STAFF vào KC qua SSO owner/staff → Flow 1 phải PASS trước | **MED** — chain dependency; STAFF walk sau Flow 1 |
| **FM3-7** | Seed role templates chưa có | `/admin/roles` render rỗng / "chưa seed templates" | `psql ... "SELECT name FROM roles WHERE deleted=false;"` → 5 template (OWNER/STAFF/TEACHER/PARENT/STUDENT)? | **MED** — seed templates nếu thiếu (RoleSeederService) |

**Tóm tắt Flow 3:** 4 HIGH (role-claim mapping / credential / RoleGuard / assign-500). FM3-4 phụ thuộc kc-core image rebuilt.

---

## Flow 4 — Attendance (GAP-1066 V87, P0)

**Persona:** teacher mark single + bulk + period stats. **SPECIAL:** V87 normalize lowercase data để kc-core boot.

| # | (a) Where | (b) Symptom | (c) Pre-walk check | (d) Confidence + fix |
|---|---|---|---|---|
| **FM4-1** ⚠️P0 | V87 `chk_attendance_status` UPPERCASE + normalize (GAP-1066) | **kiteclass-core boot crash-loop** (1230 restarts) → MỌI flow KC chết, không chỉ attendance | `bash kitehub/scripts/status.sh` → kc-core healthy? + `psql -U kitehub -d kiteclass_shared -tA -c "SELECT version,success FROM flyway_schema_history WHERE version='87';"` → kỳ vọng `87|t` + `SELECT count(*) FROM attendance WHERE status <> UPPER(status);` → kỳ vọng 0 | **HIGH** — V87 đã có normalize UPDATE (lines 27-28); rủi ro = kc-core image cũ chưa apply V87. Nếu crash-loop → rebuild kc-core, KHÔNG walk được gì |
| **FM4-2** | Contract `enrollmentId+sessionId` (KHÔNG studentId+classId) | FE gửi sai body → 400/500 | `curl -s -X POST $GW/api/v1/attendance -H "Authorization: Bearer <teacher-jwt>" -H 'Content-Type: application/json' -d '{"enrollmentId":32,"sessionId":1,"status":"PRESENT"}'` → 201 | **MED** — recipe §⚠️ đã cảnh báo; verify FE gửi đúng shape |
| **FM4-3** | Authz: `X-User-Id` = teacher của lớp / bulk cần `X-Teacher-Id` | Mark → 403 (user không phải teacher lớp); bulk → 403 thiếu X-Teacher-Id | Browser: teacher login → mark; gateway inject X-User-Id + X-User-Reference-Id từ JWT (confirmed). Verify teacher JWT là teacher của class 14 | **MED** — gateway auto-inject; rủi ro nếu FE bulk grid không gửi đủ context |
| **FM4-4** | MAKEUP status (pre-V87 bị CHECK chặn) | Mark MAKEUP → 500 nếu V87 chưa apply | `curl ... -d '{"enrollmentId":32,"sessionId":7,"status":"MAKEUP"}'` → 201 | **MED** — phụ thuộc FM4-1 (V87 applied) |
| **FM4-5** | EXCUSED cần note (GAP-993) + session COMPLETED guard (GAP-992) | EXCUSED không note → 400; session đã đóng → 400 — user có thể tưởng bug | `-d '{"enrollmentId":32,"sessionId":8,"status":"EXCUSED"}'` → 400 `EXCUSED_REQUIRES_NOTE` (đúng) | **LOW** — đúng design; recipe đã cover, đảm bảo FE message rõ tiếng Việt |
| **FM4-6** | attendanceRate = (PRESENT+LATE)/tổng (GAP-994) | Stats tính LATE là vắng → tỉ lệ sai | `curl -s "$GW/api/v1/attendance/stats/student/4" -H "Authorization: Bearer <teacher-jwt>"` → field `attendanceRate` tính cả LATE | **LOW** — fix shipped; verify số |

**Tóm tắt Flow 4:** 1 P0-HIGH (V87 crash-loop = gate sống/chết cho TOÀN BỘ KC). Nếu kc-core không healthy sau rebuild → đây là blocker đầu tiên phải xử.

---

## Flow 5 — Owner reports/enrollments/payroll (GAP-1139 95%, P0)

**Persona:** tenant OWNER mở reports/enrollments/payroll — KHÔNG được 403.

| # | (a) Where | (b) Symptom | (c) Pre-walk check | (d) Confidence + fix |
|---|---|---|---|---|
| **FM5-1** | Stale kc-core image (pre-#2296) | OWNER vẫn 403 trên reports/enrollments/payroll dù code đã fix trên main | `git -C . log --oneline -- kiteclass/kiteclass-core | grep -i "1139\|owner.*admin"` rồi so image-created-time; HOẶC sau rebuild verify trực tiếp FM5-2 | **HIGH** — fix `isAdmin()` += ROLE_OWNER + 2 controller hasAnyRole đã trên main (đã confirm code). #1 risk = image chưa rebuild |
| **FM5-2** | Gateway inject `X-User-Roles` từ JWT role claim ≠ "OWNER" | OWNER 403 vì `GatewayHeaderAuthenticationFilter` prefix `ROLE_` thô → nếu claim=`TENANT_OWNER` → `ROLE_TENANT_OWNER` → `isAdmin()` chỉ nhận ROLE_OWNER/ADMIN/PLATFORM_ADMIN → false → 403 | Decode owner JWT `role` claim sau tenant-auth login → kỳ vọng đúng `"OWNER"`. Nếu `"TENANT_OWNER"` → cross-layer drift (FE roles.ts map nhưng BE GatewayHeaderAuthenticationFilter KHÔNG map alias) | **MED-HIGH** — cross-layer drift thật: FE `roles.ts` ALIASES có TENANT_OWNER→OWNER nhưng BE filter chỉ prefix ROLE_. Verify claim = OWNER literal |
| **FM5-3** | `X-Tenant-Id` không set → owner reports 400 TENANT_NOT_SET | OWNER mở /reports → 400 (không phải 403) vì owner JWT thiếu tenantId claim → gateway không inject X-Tenant-Id | `curl -s -o /dev/null -w '%{http_code}' "$GW/api/v1/reports/revenue?months=12" -H "Authorization: Bearer <owner-jwt>"` → kỳ vọng 200, KHÔNG 400/403 | **MED** — `ReportControllerAuthzTest` confirm owner-no-tenant→400; verify owner JWT có tenantId |
| **FM5-4** | enrollments/class/{id}?status=ACTIVE (load roster điểm danh) | OWNER 403 load roster | `curl -s -o /dev/null -w '%{http_code}' "$GW/api/v1/enrollments/class/14?status=ACTIVE" -H "Authorization: Bearer <owner-jwt>"` → 200 | **HIGH** — `hasAccessToEnrollment` có `isAdmin()` bypass (OWNER); verify image rebuilt |
| **FM5-5** | PayrollController 3 endpoint hasAnyRole('ADMIN','OWNER') | OWNER mở bảng lương → 403 | `curl -s -o /dev/null -w '%{http_code}' "$GW/api/v1/payroll/periods" -H "Authorization: Bearer <owner-jwt>"` → 200 | **MED** — fix confirmed (controller line 89 hasAnyRole); verify image |
| **FM5-6** | Marketing controllers (GAP-1150 DEFER) | OWNER 403 trên marketing/landing-page/lead nếu chưa include OWNER | grep `@PreAuthorize` marketing controllers; gap note "đã include OWNER as of #2376" | **LOW** — out-of-scope reports; chỉ check nếu walk chạm marketing |

**Tóm tắt Flow 5:** 3 HIGH/MED-HIGH (stale image / role-claim literal / enrollment roster). FM5-2 là cross-layer drift đáng verify nhất (decode owner JWT role claim).

---

## Recommended batch-fix order (coordinator)

| Ưu tiên | Hành động pre-walk | Bao phủ flow |
|---|---|---|
| **1** | Rebuild + verify healthy: kiteclass-core, kitehub-subscription, 2 FE (cross-flow #0) | TẤT CẢ |
| **2** | Verify kc-core boot (V87 applied, 0 lowercase row) — FM4-1 | 4 + chặn 2/3/5 |
| **3** | Seed KH owner SSO (`seed-kh-owner-sso.sql`) + provision TEACHER/STUDENT credentials | 1, 2, 3 |
| **4** | Decode owner JWT role claim = "OWNER" + tenantId present (FM5-2/5-3) | 5 |
| **5** | curl-probe 4 endpoint owner (reports/enrollments/payroll) → 200 (FM5-1/4/5) | 5 |
| **6** | check-fe-be-api-contract.sh (FM2-1) + verify gateway X-User-Reference-Id inject (FM2-2) | 2 |

---

## Discoveries filed (per discovery-to-gap-inline-filing.md §3)

KHÔNG file gap mới session này (PREDICT-ONLY, no source edit). Failure mode FM5-2 (cross-layer drift FE alias TENANT_OWNER→OWNER vs BE filter prefix-only) + FM1-2 (GAP-1306 đã filed) là 2 candidate đáng theo dõi; coordinator quyết file nếu walk confirm.
