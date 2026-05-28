---
audience: dev
date: 2026-05-28
cluster: Teacher / Parent / Student personas + EMAIL-VERIFY (KiteClass-core domain)
session-theme: Full-regression RST walk agent a3 — TEACH / PARENT / STU / EMAIL-VERIFY flows on LOCAL stack
stack: 13 services healthy (gateway:9000, kiteclass-core:8088, MailHog:8025, postgres)
rows_walked: 23 (TEACH-LOGIN 001-003, TEACH-ATTEND 001-002, TEACH-GRADE 001-002, TEACH-SCHED 001, PARENT-LOGIN 001-003, PARENT-ATTEND 001, PARENT-GRADE 001, PARENT-BILL 001-002, STU-LOGIN 001, EMAIL-VERIFY 001-004)
verdict: BLOCKED — 18/23 NEEDS-DATA (persona accounts + tenant data un-seeded), 2 DEFERRED, 1 NEEDS-USER-BROWSER; "P0 tenant-resolution" là MISDIAGNOSIS (§2.8 → resolution hoạt động, re-scope GAP-795 P1); still-valid = NoHandler-as-500 (GAP-796 P1) + EMAIL-VERIFY path drift (GAP-797 class)
bugs_surfaced: 3 (1 re-scoped GAP-795 P1 X-User-Id NOT P0 tenant-resolution, 1 P1 error-mapping GAP-796, 1 P2 CSV path drift)
anti_contamination: namespace a3-*@test.local; no shared-account lock; no data created (read-only walk)
---

# Full-regression RST walk — Teacher / Parent / Student + EMAIL-VERIFY (agent a3)

> ⚠️ **CORRECTION 2026-05-28 (đọc TRƯỚC khi action) — "P0 tenant-resolution" dưới đây là MISDIAGNOSIS.** Coordinator fix-time investigation (`audit-to-gap-pipeline.md` §2.8) verify empirical: gateway resolve JWT tenant + core set TenantContext + Hibernate filter active → **tenant resolution WORKS** (shared-DB+filter architecture; MDC `tenant=-` là logging artifact). 18 NEEDS-DATA là do tenant chưa có seeded teacher/student (cần Owner CRUD walk tạo trước), KHÔNG phải routing broken. **Bug thật re-scoped = GAP-795 P1** (`X-User-Id` UUID vs Long → `created_by` NULL). Findings khác valid: 404/405→500 = GAP-796, EMAIL-VERIFY path drift = part GAP-797 class. Đọc INDEX.md + GAP-795/796/797.

## Tóm tắt

Walk 23 rows được giao trên LOCAL stack (13 services healthy). Hầu hết flow **không walk được tới terminal step** vì **toàn bộ persona Teacher/Parent/Student CHƯA được seed**, và tenant DB của Owner (`kiteclass_877dff9d`) **rỗng hoàn toàn** (0 teachers, 0 students, 0 classes). Các flow Teacher/Parent đều phụ thuộc dây chuyền `OWNER-TEACHER-004` (gửi lời mời GV) và `OWNER-STU-004` (tạo HS + phụ huynh) — chưa chạy → mọi flow downstream `NEEDS-DATA`.

Walk vẫn surface **3 lỗi thật** ở tầng API/routing/doc (độc lập với data seeding):
1. ~~**P0** — Tenant context KHÔNG resolve từ JWT~~ [⚠️ CORRECTED → MISDIAGNOSIS; resolution hoạt động, xem banner]. Quan sát gốc agent: `GET /api/v1/teachers` không header trả empty, thêm `X-Tenant-Id` thủ công trả data. **§2.8 investigation:** gateway resolve JWT tenant + core set TenantContext + Hibernate filter active → resolution hoạt động; empty là do tenant chưa có seeded data (read-only walk không tạo). Bug thật re-scoped = `X-User-Id` UUID vs `Long.parseLong` → `created_by` NULL → **GAP-795 P1** (auditing, không phải P0 tenant-resolution). → **Fix: GAP-795**.
2. **P1** — `NoHandlerFoundException` (404) bị trả về thành `500 SYSTEM_INTERNAL_ERROR`. → **Fix: GAP-796**.
3. **P2** — CSV ghi sai path EMAIL-VERIFY (`/api/v1/auth/verify-email` 404; path thật là `/api/auth/verify-email`). → **Fix: doc fix, part GAP-797 route-versioning class**.

## Tình trạng seed data (lý do BLOCKED)

| Kiểm tra | Kết quả |
|---|---|
| `users` table (DB `kitehub`) roles | Chỉ OWNER (8) / PLATFORM_ADMIN (2) / STAFF (3). **KHÔNG có** role TEACHER / PARENT / STUDENT |
| Owner tenant `kiteclass_877dff9d` — teachers | 0 rows |
| Owner tenant — students | 0 rows |
| Owner tenant — classes / courses / enrollments / attendance / grades | 0 rows mỗi bảng |
| `students` table schema | **KHÔNG có** cột `parent_email` / `parent_name` / `parent_id` → liên kết phụ huynh nằm ở module khác (parent-invitations) |
| MailHog | Chỉ có admin device-login email + 2 staff-invite (do agent khác tạo) — KHÔNG có teacher-invite / parent-link / email-verify |

→ Teacher/Parent persona accounts **không seeded**; phụ thuộc Owner đã tạo qua `OWNER-TEACHER-004` + `OWNER-STU-004` (ngoài scope a3, và chưa chạy). Đánh dấu `NEEDS-DATA` per assignment rule.

---

## TEACH-LOGIN (001-003)

### TEACH-LOGIN-001 — GV nhận email mời
- **DB**: 0 teachers trong tenant `877dff9d`; `users` không có role TEACHER.
- **MailHog**: không có email subject "Lời mời tham gia — Trung tâm Sky Education".
- **Phụ thuộc**: `OWNER-TEACHER-004` (Owner lưu GV + gửi lời mời) chưa chạy.
- **Verdict**: `NEEDS-DATA` — GV chưa được tạo/mời.

### TEACH-LOGIN-002 — GV đặt mật khẩu (link mời)
- Không có invite token (TEACH-LOGIN-001 chưa pass).
- **Verdict**: `NEEDS-DATA` (chain blocked).

### TEACH-LOGIN-003 — GV xem dashboard
- **Routing**: `GET /api/v1/teacher/dashboard` → **500** (gateway + core trực tiếp). Log core: `NoHandlerFoundException: No endpoint GET /api/v1/teacher/dashboard` → KHÔNG tồn tại aggregate endpoint cho teacher dashboard (chỉ có `/api/v1/teachers` CRUD). Đây là Bug #2 (404-as-500) + có thể thiếu endpoint dashboard.
- **Verdict**: `NEEDS-DATA` (không có GV để login) + flag endpoint thiếu.

## TEACH-ATTEND (001-002)

### TEACH-ATTEND-001 — Mở điểm danh lớp
- **Routing**: `GET /api/v1/attendance` (Owner JWT) → **500** = `NoHandlerFoundException` (base path không có GET handler; cần sub-path `/api/v1/attendance/class/{id}`). Endpoint điểm danh tồn tại nhưng cần classId — chưa có lớp nào (0 classes).
- **Verdict**: `NEEDS-DATA` — 0 lớp, 0 HS đăng ký.

### TEACH-ATTEND-002 — Điểm danh hôm nay (5 HS)
- Phụ thuộc roster 5 HS (`OWNER-ENROLL-002`) chưa chạy.
- **Verdict**: `NEEDS-DATA`.

## TEACH-GRADE (001-002)

### TEACH-GRADE-001 — Mở bảng điểm lớp
- **Routing**: `GET /api/v1/grades` → **500** = `NoHandlerFoundException` (cần sub-path). Module grade tồn tại (`/api/v1/grades`, `/api/v1/grades/subjects`, `/api/v1/assignments`).
- **Verdict**: `NEEDS-DATA` — 0 lớp/HS/assignments.

### TEACH-GRADE-002 — Tạo bài tập + nhập điểm
- Phụ thuộc lớp + roster + GV login.
- **Verdict**: `NEEDS-DATA`.

## TEACH-SCHED (001)

### TEACH-SCHED-001 — Xem lịch dạy theo tuần
- `class_schedules` table tồn tại nhưng 0 lớp → không có buổi học.
- **Verdict**: `NEEDS-DATA`.

---

## PARENT-LOGIN (001-003)

### PARENT-LOGIN-001 — Phụ huynh nhận email mời
- **DB**: 0 students; `users` không có role PARENT.
- **MailHog**: không có email "Liên kết phụ huynh — Sky Education".
- **Routing**: parent-invitations module tồn tại (`POST /api/v1/parent-invitations`, `POST /api/v1/parent-invitations/redeem/{token}`).
- **Phụ thuộc**: `OWNER-STU-004` (Owner tạo HS + phụ huynh) chưa chạy.
- **Verdict**: `NEEDS-DATA`.

### PARENT-LOGIN-002 — Phụ huynh đặt mật khẩu (token)
- Không có redeem token (PARENT-LOGIN-001 chưa pass).
- **Verdict**: `NEEDS-DATA`.

### PARENT-LOGIN-003 — Dashboard phụ huynh load
- **Routing**: `GET /api/v1/parent` → **500** = `NoHandlerFoundException: No endpoint GET /api/v1/parent` (base path không có GET handler aggregate; module parent có nhiều facet controller `/parent/complaints`, `/parent/consent`, v.v.).
- **Verdict**: `NEEDS-DATA` (không có phụ huynh để login) + flag endpoint dashboard thiếu/404-as-500.

## PARENT-ATTEND (001)

### PARENT-ATTEND-001 — Xem điểm danh của con
- Phụ thuộc `ParentAttendanceFacetController`; không có phụ huynh + 0 attendance.
- **Verdict**: `NEEDS-DATA`. UI render lịch tô màu (xanh/đỏ/vàng) → `NEEDS-USER-BROWSER`.

## PARENT-GRADE (001)

### PARENT-GRADE-001 — Xem điểm của con
- `ParentTranscriptController` tồn tại; 0 grades.
- **Verdict**: `NEEDS-DATA`.

## PARENT-BILL (001-002)

### PARENT-BILL-001 — Xem hoá đơn còn nợ
- `ParentFeesFacetController` + `/api/v1/invoices` tồn tại; `GET /api/v1/invoices` → 500 (NoHandler-as-500, cần sub-path/param). 0 invoices.
- **Verdict**: `NEEDS-DATA`.

### PARENT-BILL-002 — Click 'Pay invoice' (cổng)
- CSV: `deferred: phase-1.5+` — cổng thanh toán chưa active trong BETA.
- **Verdict**: `DEFERRED` (Phase 1.5 per CSV + GAP-228).

---

## STU-LOGIN (001)

### STU-LOGIN-001 — Persona Student Phase 1 BETA
- CSV: `deferred: phase-3-k12` — Student không có direct login Phase 1 BETA; truy cập qua tài khoản phụ huynh per personas-catalog.md.
- **Verdict**: `DEFERRED` (Phase 3 K-12 per CSV).

---

## EMAIL-VERIFY (001-004)

### EMAIL-VERIFY-001 — Nhận email xác minh sau Beta request
- **MailHog**: không có email "Xác minh email — KiteHub Beta" (a3 không tạo beta request mới để tránh contamination; ngoài scope BETA-REQ a3).
- **Phụ thuộc**: `BETA-REQ-004` pass (ngoài cluster a3 — thuộc Anonymous flow agent khác).
- **Verdict**: `NEEDS-DATA` (chain blocked — không tạo beta request để tránh đụng namespace shared).

### EMAIL-VERIFY-002 — Click link verify
- **Routing test (Bug #3)**: CSV verify endpoint `POST /api/v1/auth/verify-email` → **404** `Endpoint not found`. Path thật là `POST /api/auth/verify-email` (AuthController kitehub-subscription `@RequestMapping("/api/auth")`).
- Test path thật với token giả `a3-x`: `POST /api/auth/verify-email` → **400** (token invalid — validation đúng, route OK).
- **Verdict**: route BE hoạt động đúng (400 trên token sai); nhưng **CSV ghi sai path** → catalog Bug #3. UI banner "Email đã được xác minh" → `NEEDS-USER-BROWSER`.

### EMAIL-VERIFY-003 — Verify với token hết hạn
- Cần token thật quá TTL (24h) — không thể walk trong session (không có token + TTL dài).
- **Verdict**: `NEEDS-DATA` (cần token hết hạn từ Beta request thật).

### EMAIL-VERIFY-004 — Gửi lại email xác minh
- **Routing**: `POST /api/v1/auth/resend-verification` → **404** (path drift giống Bug #3; real path `/api/auth/resend-verification`).
- **Verdict**: `NEEDS-DATA` (cần email đã đăng ký) + CSV path drift (Bug #3).

---

## Bug-class table

| # | Bug | Severity | Bằng chứng | Phạm vi fix (không fix trong walk này) |
|---|---|---|---|---|
| 1 | ~~**Tenant context KHÔNG resolve từ JWT cho list endpoints**~~ [⚠️ CORRECTED → MISDIAGNOSIS; resolution hoạt động, xem banner]. Quan sát gốc agent: `GET /api/v1/teachers` không header → `[]` rỗng; cùng request + `X-Tenant-Id: 877dff9d...` → trả teacher `Trần Quốc Bảo`; log `tenant=-`. **§2.8 verdict:** gateway resolve JWT tenant + core set TenantContext + Hibernate filter active → resolution OK; `tenant=-` MDC là logging artifact (red herring); empty list khi không header = expected (agent gọi core trực tiếp bypass gateway). Bug thật re-scoped = `X-User-Id` UUID vs `Long.parseLong` → `created_by` NULL. | ~~P0~~ → **P1** | quan sát gốc preserved | **Fix: GAP-795** (X-User-Id, NOT gateway-tenant). KHÔNG sweep tenant-resolution — đã hoạt động. |
| 2 | **`NoHandlerFoundException` (404) trả về thành `500 SYSTEM_INTERNAL_ERROR`**. `/api/v1/classes`, `/api/v1/teacher/dashboard`, `/api/v1/parent`, `/api/v1/attendance`, `/api/v1/grades`, `/api/v1/enrollments`, `/api/v1/assignments`, `/api/v1/invoices` — tất cả base-path GET không handler → log `NoHandlerFoundException` nhưng response body `code=SYSTEM_INTERNAL_ERROR` status 500. | **P1** | Log core: `Resolved [NoHandlerFoundException: No endpoint GET /api/v1/parent]` + body `{"code":"SYSTEM_INTERNAL_ERROR",...}` status 500 | GlobalExceptionHandler phải map `NoHandlerFoundException` → 404 (RFC 7807 `Not Found`), không phải 500. Sai status che giấu "endpoint không tồn tại" thành "lỗi hệ thống" → khó debug + sai semantics cho FE retry logic. **Fix: GAP-796** |
| 3 | **CSV ghi sai path EMAIL-VERIFY**. `verify_via`/expected dùng `/api/v1/auth/verify-email` + `/api/v1/auth/resend-verification` → cả 2 trả **404**. Path thật: `/api/auth/verify-email` + `/api/auth/resend-verification` (AuthController `@RequestMapping("/api/auth")`, KHÔNG có `/v1`). | **P2** | curl: `/api/v1/auth/verify-email` → 404 "Endpoint not found"; `/api/auth/verify-email` → 400 (token invalid, route OK) | Sửa CSV rows EMAIL-VERIFY-002/004 path từ `/api/v1/auth/...` → `/api/auth/...`. Doc drift, không phải lỗi code. **Fix: part GAP-797 route-versioning class** |

### Quan sát phụ (không phải bug độc lập — cần xác minh khi seed data)
- Aggregate endpoint `/api/v1/teacher/dashboard` + `/api/v1/parent/dashboard` (CSV TEACH-LOGIN-003 / PARENT-LOGIN-003) **không tìm thấy** trong danh sách `@RequestMapping` của kiteclass-core. Có thể FE compose dashboard từ nhiều facet endpoint, HOẶC endpoint thật sự thiếu. Cần xác minh khi có persona account để login.

---

## Verdict tổng hợp

| Status | Count | Rows |
|---|---|---|
| **PASS** | 0 | — |
| **FAIL** | 0 | (không có flow nào walk tới terminal để fail; lỗi surface là bug-class routing/doc) |
| **NEEDS-DATA** | 18 | TEACH-LOGIN 001-003, TEACH-ATTEND 001-002, TEACH-GRADE 001-002, TEACH-SCHED 001, PARENT-LOGIN 001-003, PARENT-ATTEND 001, PARENT-GRADE 001, PARENT-BILL 001, EMAIL-VERIFY 001/003/004 |
| **NEEDS-USER-BROWSER** | 1 | EMAIL-VERIFY-002 (UI banner; route BE đã verify OK) |
| **DEFERRED** | 2 | PARENT-BILL-002 (Phase 1.5), STU-LOGIN-001 (Phase 3 K-12) |
| **BUGS catalogued** | 3 | GAP-795 P1 X-User-Id (re-scoped từ "P0 tenant-resolution" misdiagnosis), GAP-796 P1 NoHandler-as-500, P2 CSV path drift (part GAP-797 class) |

**Blocker chính**: Toàn bộ cluster Teacher/Parent/Student phụ thuộc Owner đã hoàn tất `OWNER-TEACHER-*` + `OWNER-STU-*` + `OWNER-CLASS-*` + `OWNER-ENROLL-*` để seed GV/HS/lớp/roster. Tenant `877dff9d` rỗng → không walk được flow nghiệp vụ. Cần seed data Owner trước (re-run Owner cluster hoặc `seed-data.sh` mở rộng) rồi walk lại cluster này. [⚠️ Lưu ý: 18 NEEDS-DATA là do tenant chưa seeded teacher/student, KHÔNG phải tenant-resolution broken — isolation hoạt động per GAP-795.]

**Lưu ý anti-contamination**: a3 KHÔNG tạo dữ liệu (read-only walk) để tránh đụng namespace shared với human + 2 agent khác đang walk cùng stack. Teacher `Trần Quốc Bảo` (id=1, `a2-teacher@test.local`) thấy được là do agent a2 tạo — KHÔNG phải a3.
