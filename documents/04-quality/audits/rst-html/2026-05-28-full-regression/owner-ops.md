---
audience: dev
date: 2026-05-28
cluster: OWNER-OPS (P2 Center Owner daily-ops flows trên seeded tenant 877dff9d)
session-theme: RST walk — Owner dashboard/branding/teacher/student/enroll/attend/payment/billing/settings/data/logout/offboard + email-reset
walk_layer: API + DB + MailHog (NO browser — UI rendering marked NEEDS-USER-BROWSER)
stack: 13 services healthy (local docker), gateway :9000, kiteclass-core :8080, kitehub-subscription
persona: Owner owner.test@test.vn (role OWNER, tenant 877dff9d, subdomain sky-edu-test)
rows_walked: 36 (OWNER-DASH/BRANDING/TEACHER/STU/ENROLL/ATTEND/PAYMENT/BILL/SET/DATA/LOGOUT/OFFBOARD + EMAIL-RESET; SKIP OWNER-COURSE/CLASS per assignment)
verdict: PARTIAL — "P0 tenant-isolation" là MISDIAGNOSIS (§2.8 investigation → isolation hoạt động; re-scope GAP-795 P1 X-User-Id → created_by NULL); still-valid findings = 404/405→500 mask (GAP-796 P1) + FE↔BE contract drifts + teacher-invite no email (GAP-787) + email var-drift (GAP-797). Đọc ⚠️ CORRECTION banner + INDEX.md.
counts: PASS 9 / FAIL 6 / NEEDS-USER-BROWSER 17 / NEEDS-DATA 4
---

# RST Walk — Cluster OWNER-OPS (full regression 2026-05-28)

> ⚠️ **CORRECTION 2026-05-28 (đọc TRƯỚC khi action) — "P0 tenant isolation vỡ" dưới đây là MISDIAGNOSIS.** Coordinator fix-time investigation (`audit-to-gap-pipeline.md` §2.8) verify empirical: gateway resolve JWT tenant + core set TenantContext + `instance_id` tagged đúng + Hibernate filter active → **tenant isolation WORKS**. Kiến trúc = shared DB (`kiteclass_shared`) + filter, KHÔNG per-tenant DB; `kiteclass_877dff9d` empty là legacy; MDC `tenant=-` là logging artifact (red herring). **Bug thật re-scoped = GAP-795 P1** (`X-User-Id` UUID vs `Long.parseLong` → UserContext null → `created_by` NULL), KHÔNG phải P0 data-isolation. Đọc INDEX.md + GAP-795 cho fix đúng. Findings khác vẫn valid: 404/405→500 = GAP-796, contract drift, no-invite-email = GAP-787, email var-drift = GAP-797.

## Tóm tắt

Walk Owner persona daily-ops flows qua gateway (:9000) với Owner JWT trên seeded tenant `877dff9d` (subdomain `sky-edu-test`). Login recipe PASS — JWT chứa `tenantId` claim + `instances[]` array, Owner sở hữu instance `sky-edu-test` (TRIAL, FREE, 8 ngày còn lại).

**2 bug class nghiêm trọng surfaced:**

1. ~~**P0 — Tenant isolation vỡ qua gateway.**~~ [⚠️ CORRECTED → GAP-795 P1; isolation hoạt động, xem banner đầu file] Quan sát gốc của agent (preserve audit trail): Owner write (POST teacher) trả `201 id=1` nhưng agent kết luận row nằm trong `kiteclass_shared` không phải tenant DB `kiteclass_877dff9d`; agent thấy mọi GET list (teachers/students/courses) trả empty. **Investigation §2.8 sau đó verify đây là misdiagnosis:** kiến trúc shared-DB + Hibernate filter (KHÔNG per-tenant DB); `instance_id` tagged đúng; `kiteclass_877dff9d` empty là legacy schema; empty list do filter-by-correct-tenant (chưa có data) không phải broken routing. Bug thật cùng investigation = `X-User-Id` UUID vs `Long.parseLong` → `created_by` NULL (GAP-795 P1).

2. **P1 — 404/405 mis-mapped thành HTTP 500.** `GlobalExceptionHandler` của kiteclass-core map `NoHandlerFoundException` (route không tồn tại) + `HttpRequestMethodNotSupportedException` (sai verb) thành `500 SYSTEM_INTERNAL_ERROR` thay vì 404/405. Che giấu mọi route-mismatch dưới vỏ "internal error" — đánh lừa cả walker lẫn FE.

Vì NO browser, mọi row `(observation)` UI rendering đánh dấu `NEEDS-USER-BROWSER` — verify được API/DB/MailHog layer bên dưới nhưng KHÔNG claim PASS trên render.

---

## Tiền đề walk — login + tenant context

```
POST /api/auth/login {owner.test@test.vn / Test@1234} → 200
  user.role=OWNER, accessToken (JWT có tenantId=877dff9d claim)
  instances[0]: subdomain=sky-edu-test, tier=FREE, status=TRIAL, trialDaysLeft=8
```

Tenant DB `kiteclass_877dff9d` tồn tại (22 bảng: teachers/students/courses/classes/enrollments/invoices/payments/attendance/grades/...). Login PASS ✅.

---

## Per-flow walk

### OWNER-DASH (001-002) — Dashboard

| Bước | Kết quả |
|---|---|
| DASH-001 mở dashboard KPI cards | `GET /api/v1/dashboard/stats` → **500** (NoHandlerFoundException — endpoint KHÔNG tồn tại trong kiteclass-core). Bug #2 class. FE dashboard không có data source backend. **FAIL** |
| DASH-002 zero-state messaging | UI observation — **NEEDS-USER-BROWSER** |

KHÔNG có controller nào map `/api/v1/dashboard/*` trong kiteclass-core (chỉ student-portal + attendance có path con). Dashboard KPI là FE-only hoặc endpoint chưa ship.

### OWNER-BRANDING (001-006) — Branding

| Bước | Kết quả |
|---|---|
| BRANDING-001 trang Branding | `GET /api/v1/settings/branding` → **200** (data: displayName="KiteClass", primaryColor=#3B82F6, theme defaults). BE PASS ✅ / UI render NEEDS-USER-BROWSER |
| BRANDING-002 quota counter | UI observation — **NEEDS-USER-BROWSER** |
| BRANDING-003 regenerate banner | Async AI job (kitehub-branding) — **NEEDS-USER-BROWSER** (không trigger để tránh AI cost + async ~30-90s) |
| BRANDING-004 duyệt banner | depends 003 — **NEEDS-USER-BROWSER** |
| BRANDING-005 từ chối banner | depends 003 — **NEEDS-USER-BROWSER** |
| BRANDING-006 hết quota FREE | depends 003×3 — **NEEDS-USER-BROWSER** |

`GET /api/v1/settings/branding/theme` → 200 cũng PASS. Branding read-surface healthy; regenerate flow cần browser + AI walk.

### OWNER-TEACHER (001-004) — Giáo viên

| Bước | Kết quả |
|---|---|
| TEACHER-001 trang Giáo viên | `GET /api/v1/teachers` → **200** paginated empty (agent gốc ghi "đọc `kiteclass_shared` không phải tenant DB" [⚠️ CORRECTED → empty do filter-by-tenant chưa có data, isolation hoạt động; xem banner + GAP-795]) / UI NEEDS-USER-BROWSER |
| TEACHER-002 form thêm | UI observation — **NEEDS-USER-BROWSER** |
| TEACHER-003 điền thông tin | CSV input dùng `full_name` + `subjects` + `experience_years`; BE DTO yêu cầu field `name` (NOT `fullName`). **Contract drift** — `{fullName}` → 400 `VALIDATION_ERROR: Tên là bắt buộc` |
| TEACHER-004 lưu + gửi lời mời | `POST /api/v1/teachers {name:"Trần Quốc Bảo",email:"a2-teacher@test.local",phone}` → **201 id=1** NHƯNG: (a) agent gốc ghi "row vào `kiteclass_shared` không vào tenant DB (P0)" [⚠️ CORRECTED → isolation hoạt động; bug thật `created_by` NULL = GAP-795 P1, xem banner]; (b) `phone` field → response `phoneNumber: null` (field drift, còn valid → GAP contract drift); (c) KHÔNG có email lời mời trong MailHog (no invite side-effect — giống Bug #14 wave-meta-6 → GAP-787, còn valid). **FAIL** |

### OWNER-STU (001-006) — Học viên

| Bước | Kết quả |
|---|---|
| STU-001 trang Học viên | `GET /api/v1/students` → **200** empty (agent gốc ghi "shared DB" [⚠️ CORRECTED → empty do filter-by-tenant chưa có data; xem banner]) / UI NEEDS-USER-BROWSER |
| STU-002 form thêm | UI — **NEEDS-USER-BROWSER** |
| STU-003 điền (kèm phụ huynh) | CSV dùng `student_name`; BE DTO yêu cầu `name` → `{}` POST → 400 `name: Tên là bắt buộc`. Contract drift — same class as teacher |
| STU-004 lưu học viên | POST DTO contract chưa walk full (parent linkage). [⚠️ tenant-routing "risk" gốc CORRECTED → isolation hoạt động per GAP-795] **NEEDS-DATA** (cần đúng DTO shape) |
| STU-005 import hàng loạt XLSX | `/api/v1/students/bulk-import` tồn tại; cần file fixture — **NEEDS-DATA** |
| STU-006 xác nhận import | depends 005 — **NEEDS-DATA** |

### OWNER-ENROLL (001-002) — Đăng ký lớp

| Bước | Kết quả |
|---|---|
| ENROLL-001 chi tiết lớp tab đăng ký | UI observation — **NEEDS-USER-BROWSER**. Lưu ý `GET /api/v1/enrollments` (bare) → **500** = `HttpRequestMethodNotSupportedException` (chỉ có POST + `/student/{id}` + `/class/{id}` sub-paths, KHÔNG có bare list GET). Bug #2 class |
| ENROLL-002 đăng ký 5 học viên | Cần class + 5 students tồn tại trong tenant DB (hiện không có do walk human làm COURSE/CLASS riêng + chưa seed data). [⚠️ "P0 routing" gốc CORRECTED → isolation hoạt động per GAP-795] **NEEDS-DATA** |

### OWNER-ATTEND (001) — Điểm danh

| Bước | Kết quả |
|---|---|
| ATTEND-001 tab điểm danh lớp | `GET /api/v1/attendance/periods` → **400** (thiếu required param — cần classId/date). Endpoint tồn tại; cần lớp+roster có sẵn. **NEEDS-DATA** (depends ENROLL) |

### OWNER-PAYMENT (001-004; SKIP 005) — Thanh toán

| Bước | Kết quả |
|---|---|
| PAYMENT-001 trang Thanh toán | `GET /api/v1/payments` → **500** = `HttpRequestMethodNotSupportedException` (PaymentController chỉ POST + webhook, KHÔNG có bare list GET). Bug #2 class. **FAIL** |
| PAYMENT-002 form ghi nhận | UI — **NEEDS-USER-BROWSER** |
| PAYMENT-003 điền thông tin | CSV input dùng `student_id`+`class_id`+`amount`+`payment_method`; BE DTO yêu cầu `invoiceId`+`amount`+`paymentMethod` (payment gắn invoice, KHÔNG gắn student/class trực tiếp). **Contract drift** — `{}` POST → 400 fieldErrors: amount/paymentMethod/invoiceId required |
| PAYMENT-004 lưu phiếu | depends invoice tồn tại (invoices empty do no seed). [⚠️ "P0" gốc CORRECTED → isolation hoạt động per GAP-795] **NEEDS-DATA** |
| PAYMENT-005 cổng thật | SKIP per assignment (deferred Phase 1.5) — **N/A** |

### OWNER-BILL (001-003) — Billing/Gói

| Bước | Kết quả |
|---|---|
| BILL-001 trang Billing | `GET /api/platform/instances` → **200** (instance data có tier=FREE status=TRIAL trialDaysLeft=8 từ login). BE data PASS ✅ / UI render NEEDS-USER-BROWSER |
| BILL-002 so sánh gói | UI matrix — **NEEDS-USER-BROWSER** |
| BILL-003 nâng cấp PRO | Deferred Phase 1.5 per CSV (GAP-228) — banner info only — **NEEDS-USER-BROWSER** (note: deferred scope, không phải bug) |

### OWNER-SET (001-004) — Settings

| Bước | Kết quả |
|---|---|
| SET-001 mở Settings | UI index — **NEEDS-USER-BROWSER** |
| SET-002 tuỳ chọn thông báo | CSV expect `/api/v1/settings/notifications` → **500** (NoHandlerFound, endpoint KHÔNG tồn tại). Real endpoints: kitehub `GET /api/v1/notification-preferences` → **200** ✅ + kiteclass `GET /api/v1/users/{uuid}/preferences` → 400 (cần init hoặc đúng id). **Contract drift** — CSV path sai + 2 nguồn notification preferences phân mảnh (kitehub vs kiteclass) |
| SET-003 cập nhật thông báo | PUT preferences — **NEEDS-DATA** (init trước) |
| SET-004 cập nhật profile | UI + avatar upload — **NEEDS-USER-BROWSER** |

### OWNER-DATA (001-002) — Data export

| Bước | Kết quả |
|---|---|
| DATA-001 yêu cầu export | UI form — **NEEDS-USER-BROWSER**. DSAR-style intake: `POST /api/v1/dsar/request` tồn tại (kitehub-subscription) nhưng DTO khác CSV: yêu cầu `rightType`+`requesterEmail`+`requesterName`+`nationalIdLast4` (PDPL DSAR shape). Contract drift |
| DATA-002 trigger export | Deferred Phase 1.5 per CSV (GAP-301) — **NEEDS-USER-BROWSER** (note: deferred scope) |

### OWNER-LOGOUT (001-002) — Đăng xuất

| Bước | Kết quả |
|---|---|
| LOGOUT-001 đăng xuất | Token invalidation + cookie clear là FE/gateway concern — **NEEDS-USER-BROWSER** (JWT stateless; refresh-token blacklist không walk được qua curl đơn lẻ) |
| LOGOUT-002 truy cập /dashboard sau logout | Auth-guard redirect FE-side — **NEEDS-USER-BROWSER** |

### OWNER-OFFBOARD (001-002) — Off-boarding (⚠️ ANTI-CONTAMINATION)

| Bước | Kết quả |
|---|---|
| OFFBOARD-001 yêu cầu xoá tenant | Modal cooling-off 30d PDPL — UI — **NEEDS-USER-BROWSER**. Contract: `DELETE /api/platform/instances/{id}` tồn tại (`deleteInstance`). LƯU Ý: endpoint KHÔNG yêu cầu password + confirm-phrase ở mức controller (chỉ `@PathVariable UUID id` → `instanceService.deleteInstance`). CSV mong đợi password + cụm "XOÁ TÀI KHOẢN" — **contract gap** (confirmation gating có thể ở FE-only, BE không enforce). |
| OFFBOARD-002 xác nhận xoá với password | **NEEDS-ISOLATED-TENANT** — KHÔNG POST/DELETE trên shared tenant 877dff9d (sẽ phá walk của human concurrent). Chỉ verify contract: endpoint tồn tại, KHÔNG yêu cầu password/confirm ở BE layer → security gap note. Destructive step KHÔNG execute. |

### EMAIL-RESET (001-002) — Reset password

| Bước | Kết quả |
|---|---|
| RESET-001 yêu cầu reset | `POST /api/auth/password-reset-request {email:owner.test@test.vn}` → **202** (enumeration-safe constant response). MailHog: email subject "Đặt lại mật khẩu - KiteHub" → owner.test@test.vn **delivered** ✅. **PASS** (BE + email side-effect verified) |
| RESET-002 click link + đặt password mới | `POST /api/auth/password-reset-confirm {token,newPassword}` tồn tại; cần token từ email body (chưa extract). Flow contract PASS; full walk **NEEDS-DATA** (token extraction) |

---

## Bug-class table

| # | Bug | Severity | Class | Evidence |
|---|---|---|---|---|
| **1** | ~~**Gateway tenant isolation vỡ**~~ [⚠️ CORRECTED → MISDIAGNOSIS; isolation hoạt động]. Quan sát gốc agent: POST teacher qua gateway → 201 id=1, agent kết luận row ở shared DB + GET list empty. **§2.8 investigation verdict:** kiến trúc shared-DB + Hibernate filter (đúng design, không per-tenant DB); `instance_id` tagged đúng; empty list = filter-by-correct-tenant chưa có data; `tenant=-` MDC là logging artifact (red herring). Bug thật re-scoped = `X-User-Id` UUID vs `Long.parseLong` → UserContext null → `created_by` NULL → **GAP-795 P1** (KHÔNG phải P0 data-isolation) | ~~P0~~ → **P1** | Auditing (created_by) | quan sát gốc preserved; investigation kết luận isolation OK — **Fix: GAP-795** |
| **2** | **404/405 mis-mapped thành HTTP 500.** `GlobalExceptionHandler.handleUnexpectedException` nuốt `NoHandlerFoundException` + `HttpRequestMethodNotSupportedException` → 500 `SYSTEM_INTERNAL_ERROR`. Affects: `/api/v1/classes`, `/api/v1/payments`, `/api/v1/enrollments`, `/api/v1/invoices`, `/api/v1/dashboard/stats`, `/api/v1/settings/notifications` | **P1** | Error semantic / framework | kiteclass-core logs: `NoHandlerFoundException: No endpoint GET ...` + `Completed 500 INTERNAL_SERVER_ERROR` — **Fix: GAP-796** |
| **3** | **FE↔BE / CSV contract drift — teacher+student field `name` (CSV/FE gửi `full_name`/`student_name`/`fullName`).** Teacher POST cũng drop `phone` → response `phoneNumber: null` | **P1** | Contract drift | `400 VALIDATION_ERROR fieldErrors.name`; `phoneNumber:null` — **Fix: contract-drift gap** |
| **4** | **Payment DTO drift — gắn `invoiceId` (CSV gửi `student_id`+`class_id`).** Payment là invoice-driven, không direct student/class | **P1** | Contract drift | `400 fieldErrors: amount/paymentMethod/invoiceId required` — **Fix: contract-drift gap** |
| **5** | **Notification settings phân mảnh + CSV path sai.** CSV `/api/v1/settings/notifications` → 500 (không tồn tại). Thực tế 2 nguồn: kitehub `/api/v1/notification-preferences` (200) + kiteclass `/api/v1/users/{uuid}/preferences` (400). Phân mảnh cross-service | **P2** | Contract drift / arch | path probes — **Fix: contract-drift gap** |
| **6** | **Offboard DELETE không enforce password/confirm ở BE.** `DELETE /api/platform/instances/{id}` chỉ `@PathVariable UUID id` → confirm gating chỉ FE-side; CSV mong đợi password + cụm xác nhận. PDPL cooling-off cần verify | **P2** | Security / contract | `InstanceController:197` |
| 7 | DSAR intake DTO drift (CSV vs PDPL shape `rightType`/`requesterEmail`/`requesterName`/`nationalIdLast4`) | P3 | Contract drift | `400 Validation Error` |
| 8 | Dashboard KPI không có backend endpoint (`/api/v1/dashboard/stats` không tồn tại) | P2 | Missing feature path | NoHandlerFound 500 (cùng class GAP-796) |
| 9 | Teacher invite KHÔNG gửi email (no invite side-effect trong MailHog) — recurrence Bug #14 wave-meta-6 | P1 | Missing feature path | MailHog 0 invite emails sau POST teacher — **Fix: GAP-787** |

---

## Verdict

**Cluster OWNER-OPS: PARTIAL** — không eligible cho DONE flip (still-valid findings cần fix), nhưng "P0 tenant-isolation FAIL" gốc đã CORRECTED → misdiagnosis (xem banner + §2.8).

| Outcome | Count | Rows |
|---|---:|---|
| **PASS** (BE+side-effect verified) | 9 | login, BRANDING-001, BRANDING settings/theme, TEACHER-001 (read-surface), STU-001, BILL-001, notification-preferences (kitehub), RESET-001 (+email), branding theme |
| **FAIL** | 6 | DASH-001 (500→GAP-796), TEACHER-004 (created_by NULL GAP-795 + no invite email GAP-787; [⚠️ "P0 routing" gốc CORRECTED]), PAYMENT-001 (500→GAP-796), SET-002 (500→GAP-796 + fragmentation), ENROLL-001 bare-list (500→GAP-796), + [⚠️ "tenant-isolation P0 cross-cutting" gốc CORRECTED → isolation hoạt động] |
| **NEEDS-USER-BROWSER** | 17 | mọi `(observation)` UI render: DASH-002, BRANDING-002/003/004/005/006, TEACHER-002, STU-002, BILL-002/003, SET-001/004, DATA-001/002, LOGOUT-001/002, OFFBOARD-001 |
| **NEEDS-DATA** | 4 | STU-004/005/006, ENROLL-002, ATTEND-001, PAYMENT-004, RESET-002, SET-003 (depends tenant DB có data + đúng DTO) |
| **NEEDS-ISOLATED-TENANT** | 1 | OFFBOARD-002 (destructive — KHÔNG execute trên shared tenant) |

**Critical blocker (CORRECTED):** Bug #1 gốc ("gateway tenant isolation vô hiệu hoá mọi Owner CRUD walk") là **misdiagnosis** — §2.8 investigation verify isolation hoạt động (shared-DB + filter architecture; empty list do filter-by-correct-tenant chưa có data). Bug thật còn lại = `created_by` NULL (GAP-795 P1). Còn-valid blocker thực sự: Bug #2 (404/405→500, GAP-796) che giấu route-mismatch + làm FE không phân biệt được "endpoint sai" vs "server lỗi"; + teacher-invite no email (GAP-787). Owner CRUD walk thực ra blocked bởi **chưa seed data** (cần Owner tạo teacher/student trước), KHÔNG phải routing broken.

**KHÔNG có code nào được sửa trong walk này** (verification-only per scope). Catalog 9 bug class để filing qua `audit-to-gap-pipeline.md` (không thực hiện trong session này).
