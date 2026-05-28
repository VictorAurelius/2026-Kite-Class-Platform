---
audience: dev
date: 2026-05-28
cluster: OWNER-OPS (P2 Center Owner daily-ops flows trên seeded tenant 877dff9d)
session-theme: RST walk — Owner dashboard/branding/teacher/student/enroll/attend/payment/billing/settings/data/logout/offboard + email-reset
walk_layer: API + DB + MailHog (NO browser — UI rendering marked NEEDS-USER-BROWSER)
stack: 13 services healthy (local docker), gateway :9000, kiteclass-core :8080, kitehub-subscription
persona: Owner owner.test@test.vn (role OWNER, tenant 877dff9d, subdomain sky-edu-test)
rows_walked: 36 (OWNER-DASH/BRANDING/TEACHER/STU/ENROLL/ATTEND/PAYMENT/BILL/SET/DATA/LOGOUT/OFFBOARD + EMAIL-RESET; SKIP OWNER-COURSE/CLASS per assignment)
verdict: FAIL — 1 P0 tenant-isolation bug (gateway writes Owner data to kiteclass_shared not tenant DB) + 1 P1 framework bug (404/405 mis-mapped to HTTP 500) + multiple FE↔BE contract drifts
counts: PASS 9 / FAIL 6 / NEEDS-USER-BROWSER 17 / NEEDS-DATA 4
---

# RST Walk — Cluster OWNER-OPS (full regression 2026-05-28)

## Tóm tắt

Walk Owner persona daily-ops flows qua gateway (:9000) với Owner JWT trên seeded tenant `877dff9d` (subdomain `sky-edu-test`). Login recipe PASS — JWT chứa `tenantId` claim + `instances[]` array, Owner sở hữu instance `sky-edu-test` (TRIAL, FREE, 8 ngày còn lại).

**2 bug class nghiêm trọng surfaced:**

1. **P0 — Tenant isolation vỡ qua gateway.** Owner write (POST teacher) trả `201 id=1` nhưng row KHÔNG nằm trong tenant DB `kiteclass_877dff9d` — nó nằm trong `kiteclass_shared` (fallback DB). Mọi GET list (teachers/students/courses) qua gateway đọc `kiteclass_shared` (empty), KHÔNG đọc tenant DB. Direct-to-core call (bypass gateway) WITH `X-Tenant-Id` thì routing đúng → bug ở tầng gateway `TenantResolverGatewayFilterFactory` re-derivation.

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
| TEACHER-001 trang Giáo viên | `GET /api/v1/teachers` → **200** paginated empty (đọc `kiteclass_shared`, KHÔNG phải tenant DB — xem Bug P0) / UI NEEDS-USER-BROWSER |
| TEACHER-002 form thêm | UI observation — **NEEDS-USER-BROWSER** |
| TEACHER-003 điền thông tin | CSV input dùng `full_name` + `subjects` + `experience_years`; BE DTO yêu cầu field `name` (NOT `fullName`). **Contract drift** — `{fullName}` → 400 `VALIDATION_ERROR: Tên là bắt buộc` |
| TEACHER-004 lưu + gửi lời mời | `POST /api/v1/teachers {name:"Trần Quốc Bảo",email:"a2-teacher@test.local",phone}` → **201 id=1** NHƯNG: (a) row vào `kiteclass_shared` KHÔNG vào tenant DB (**P0**); (b) `phone` field → response `phoneNumber: null` (field drift); (c) KHÔNG có email lời mời trong MailHog (no invite side-effect — giống Bug #14 wave-meta-6). **FAIL** |

### OWNER-STU (001-006) — Học viên

| Bước | Kết quả |
|---|---|
| STU-001 trang Học viên | `GET /api/v1/students` → **200** empty (shared DB) / UI NEEDS-USER-BROWSER |
| STU-002 form thêm | UI — **NEEDS-USER-BROWSER** |
| STU-003 điền (kèm phụ huynh) | CSV dùng `student_name`; BE DTO yêu cầu `name` → `{}` POST → 400 `name: Tên là bắt buộc`. Contract drift — same class as teacher |
| STU-004 lưu học viên | POST DTO contract chưa walk full (parent linkage); cùng P0 tenant-routing risk như teacher. **NEEDS-DATA** (cần đúng DTO shape) |
| STU-005 import hàng loạt XLSX | `/api/v1/students/bulk-import` tồn tại; cần file fixture — **NEEDS-DATA** |
| STU-006 xác nhận import | depends 005 — **NEEDS-DATA** |

### OWNER-ENROLL (001-002) — Đăng ký lớp

| Bước | Kết quả |
|---|---|
| ENROLL-001 chi tiết lớp tab đăng ký | UI observation — **NEEDS-USER-BROWSER**. Lưu ý `GET /api/v1/enrollments` (bare) → **500** = `HttpRequestMethodNotSupportedException` (chỉ có POST + `/student/{id}` + `/class/{id}` sub-paths, KHÔNG có bare list GET). Bug #2 class |
| ENROLL-002 đăng ký 5 học viên | Cần class + 5 students tồn tại trong tenant DB (hiện không có do walk human làm COURSE/CLASS riêng + P0 routing). **NEEDS-DATA** |

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
| PAYMENT-004 lưu phiếu | depends invoice tồn tại (invoices empty do P0 + no seed). **NEEDS-DATA** |
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
| **1** | **Gateway tenant isolation vỡ — Owner write vào `kiteclass_shared` thay vì tenant DB `kiteclass_877dff9d`.** POST teacher qua gateway → 201 id=1 nhưng row ở shared DB; direct-to-core WITH `X-Tenant-Id` → đúng tenant context. Gateway `TenantResolverGatewayFilterFactory` strip client `X-Tenant-Id` (đúng, anti-spoof) rồi re-derive sai → fallback shared DB. Mọi GET list đọc shared DB (empty) | **P0** | Tenant routing / data isolation | `kiteclass_shared.teachers` có id=1; `kiteclass_877dff9d.teachers` 0 rows; logs `tenant=-` trên mọi gateway-routed call |
| **2** | **404/405 mis-mapped thành HTTP 500.** `GlobalExceptionHandler.handleUnexpectedException` nuốt `NoHandlerFoundException` + `HttpRequestMethodNotSupportedException` → 500 `SYSTEM_INTERNAL_ERROR`. Affects: `/api/v1/classes`, `/api/v1/payments`, `/api/v1/enrollments`, `/api/v1/invoices`, `/api/v1/dashboard/stats`, `/api/v1/settings/notifications` | **P1** | Error semantic / framework | kiteclass-core logs: `NoHandlerFoundException: No endpoint GET ...` + `Completed 500 INTERNAL_SERVER_ERROR` |
| **3** | **FE↔BE / CSV contract drift — teacher+student field `name` (CSV/FE gửi `full_name`/`student_name`/`fullName`).** Teacher POST cũng drop `phone` → response `phoneNumber: null` | **P1** | Contract drift | `400 VALIDATION_ERROR fieldErrors.name`; `phoneNumber:null` |
| **4** | **Payment DTO drift — gắn `invoiceId` (CSV gửi `student_id`+`class_id`).** Payment là invoice-driven, không direct student/class | **P1** | Contract drift | `400 fieldErrors: amount/paymentMethod/invoiceId required` |
| **5** | **Notification settings phân mảnh + CSV path sai.** CSV `/api/v1/settings/notifications` → 500 (không tồn tại). Thực tế 2 nguồn: kitehub `/api/v1/notification-preferences` (200) + kiteclass `/api/v1/users/{uuid}/preferences` (400). Phân mảnh cross-service | **P2** | Contract drift / arch | path probes |
| **6** | **Offboard DELETE không enforce password/confirm ở BE.** `DELETE /api/platform/instances/{id}` chỉ `@PathVariable UUID id` → confirm gating chỉ FE-side; CSV mong đợi password + cụm xác nhận. PDPL cooling-off cần verify | **P2** | Security / contract | `InstanceController:197` |
| 7 | DSAR intake DTO drift (CSV vs PDPL shape `rightType`/`requesterEmail`/`requesterName`/`nationalIdLast4`) | P3 | Contract drift | `400 Validation Error` |
| 8 | Dashboard KPI không có backend endpoint (`/api/v1/dashboard/stats` không tồn tại) | P2 | Missing feature path | NoHandlerFound 500 |
| 9 | Teacher invite KHÔNG gửi email (no invite side-effect trong MailHog) — recurrence Bug #14 wave-meta-6 | P1 | Missing feature path | MailHog 0 invite emails sau POST teacher |

---

## Verdict

**Cluster OWNER-OPS: FAIL** — không eligible cho DONE flip.

| Outcome | Count | Rows |
|---|---:|---|
| **PASS** (BE+side-effect verified) | 9 | login, BRANDING-001, BRANDING settings/theme, TEACHER-001 (read-surface), STU-001, BILL-001, notification-preferences (kitehub), RESET-001 (+email), branding theme |
| **FAIL** | 6 | DASH-001 (500), TEACHER-004 (P0 routing + no invite email), PAYMENT-001 (500), SET-002 (500 + fragmentation), ENROLL-001 bare-list (500), + tenant-isolation P0 cross-cutting |
| **NEEDS-USER-BROWSER** | 17 | mọi `(observation)` UI render: DASH-002, BRANDING-002/003/004/005/006, TEACHER-002, STU-002, BILL-002/003, SET-001/004, DATA-001/002, LOGOUT-001/002, OFFBOARD-001 |
| **NEEDS-DATA** | 4 | STU-004/005/006, ENROLL-002, ATTEND-001, PAYMENT-004, RESET-002, SET-003 (depends tenant DB có data + đúng DTO) |
| **NEEDS-ISOLATED-TENANT** | 1 | OFFBOARD-002 (destructive — KHÔNG execute trên shared tenant) |

**Critical blocker:** Bug #1 (gateway tenant isolation) làm vô hiệu hoá mọi Owner CRUD walk — data đi vào shared DB, không vào tenant DB. Phải fix trước khi bất kỳ Owner daily-ops flow nào hoạt động end-to-end trên production-equivalent stack. Bug #2 (404/405→500) che giấu route-mismatch + làm FE không phân biệt được "endpoint sai" vs "server lỗi".

**KHÔNG có code nào được sửa trong walk này** (verification-only per scope). Catalog 9 bug class để filing qua `audit-to-gap-pipeline.md` (không thực hiện trong session này).
