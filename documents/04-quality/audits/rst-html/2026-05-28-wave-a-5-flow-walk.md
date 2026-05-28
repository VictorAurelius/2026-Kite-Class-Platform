---
audience: dev
date: 2026-05-28
session-theme: Wave A pre-deploy — 5 luồng RST walk (user tự test browser + API pre-check)
walk_status: IN_PROGRESS — flow 5 (GAP-794) PASS API-layer; flow 1-4 chờ user walk browser
walk_branch: docs/session-handoff-2026-05-28-wave-a-fixes-merged
stack: local (13 service healthy, 4 service rebuilt 2026-05-28 từ main HEAD)
gaps_under_walk: [GAP-790, GAP-791, GAP-792, GAP-787, GAP-793, GAP-794]
---

# Wave A — 5 luồng RST walk (2026-05-28)

5 luồng map trực tiếp 6 gap Wave A vừa merged (PR #1935/#1937/#1938/#1939). Code đã trên main + 4 service rebuilt. Walk này để flip gap OPEN/PARTIAL → DONE per `feature-ship-runtime-walk-mandate.md`.

## Endpoint + credential (local stack)

| Thành phần | URL |
|---|---|
| Gateway API | `http://localhost:9000` |
| FE KiteHub | `http://localhost:3001` |
| FE KiteClass | `http://localhost:3000` |
| MailHog UI | `http://localhost:8025` |
| Postgres | `docker exec kite-postgres psql -U kitehub -d kitehub` |

| Persona | Credential |
|---|---|
| Owner | `owner.test@test.vn` / `Test@1234` — tenant `877dff9d-c354-4faf-8c44-3c17196dbf24` |
| Admin | `admin@kitehub.com` / `Admin@KiteHub123` (PLATFORM_ADMIN) |
| Login API | `POST /api/auth/login` (CHÚ Ý: `/api/auth/login`, KHÔNG phải `/api/v1/auth/login` như CSV ghi — finding #1) |

---

## Flow 1 — Anonymous signup → admin approve → email → set password → login

**Gap:** BETA-REQ + ADM-BETA-APPROVE + OWNER-SIGNUP chain. **Browser steps (user):**

1. `http://localhost:3001/request-beta-access` → điền form (email `walk-owner-001@test.local`, tên `Nguyễn Thị Hương`, org `Trung tâm Sky Education`, persona P2_CENTER_OWNER, consent PDPL) → Submit.
   - **Kỳ vọng:** HTTP 201 + banner thành công.
   - **API pre-check (đã chạy):** `POST /api/v1/auth/request-beta-access` route OK (BetaAccessController exists).
2. DB verify: `SELECT id,email,status FROM beta_access_request WHERE email='walk-owner-001@test.local';` → row PENDING.
3. Login admin `http://localhost:3001/login` (`admin@kitehub.com` / `Admin@KiteHub123`) → `/admin`.
4. `/admin/beta-requests` → mở request → Duyệt (tier TRIAL, 14 ngày) → Xác nhận.
   - **Kỳ vọng:** status APPROVED + invite_token generate + email queue.
5. MailHog `http://localhost:8025` → tìm email "Mời bạn — KiteHub Beta" tới `walk-owner-001@test.local`.
   - **(GAP-793/787 liên quan — consumer fix #1938):** email PHẢI tới MailHog (không drop ở queue).
6. Click link signup trong email → đặt password → submit → tự login → `/dashboard`.

**Kết quả:** 🔴 **FAIL tại Bước 5/6** (user walk, `documents/image-7.png`). Email beta-invite render `------` (không có mã 6 số) + link `/beta/accept` (không token/code) → user KHÔNG signup được. Root cause: var-name contract drift (`EmailServiceClient` gửi `claimCode`/`signupUrl`, template `beta-invite.txt` đọc `verificationCode`/`inviteUrl` → fallback default). **→ GAP-797 P0 filed** (cross-flow sweep bắt thêm welcome + beta-invite.html cùng class).

---

## Flow 2 — Owner onboarding wizard (route onboarding-progress)

**Gap:** GAP-790 (gateway TenantResolver cho onboarding-progress route). **Browser steps:**

1. Login Owner (`owner.test@test.vn` / `Test@1234`) → `/dashboard`.
2. Vào onboarding wizard (nếu chưa hoàn tất) HOẶC trigger `/setup`.
   - **API pre-check (đã chạy ✅):** `GET /api/v1/onboarding-progress` qua gateway + owner JWT → **HTTP 200** (trả steps list, KHÔNG 401/400). TenantResolver fix GAP-790 hoạt động ở API-layer.
3. Đi qua các bước wizard → lưu state mỗi bước (gateway forward tenant header đúng).

**Kết quả:** ✅ API-layer PASS (GAP-790). Browser-layer (wizard UI render + step persist) chờ bạn walk.

---

## Flow 3 — Course/Class CRUD cross-tenant (GAP-791 + GAP-792)

**Gap:** GAP-791 (course list native query tenant predicate) + GAP-792 (@Cacheable tenant key — cache không poison cross-tenant). **Steps:**

1. Login Owner tenant A → tạo Course → list courses → chỉ thấy course tenant A.
2. **Cross-tenant leak check:** login Owner tenant B (nếu có) → list courses → KHÔNG thấy course tenant A.
3. **Cache poison check:** tenant A list (warm cache) → tenant B list cùng endpoint → tenant B KHÔNG nhận cache tenant A.

**API pre-check (đã chạy ✅ một phần):** `GET /api/v1/courses` + owner JWT (tenant 877dff9d) → **HTTP 200** + `data.content: []` (empty, tenant chưa có course). Native query tenant predicate KHÔNG lỗi (GAP-791 không crash). **Cross-tenant leak + cache poison cần 2 tenant có data** — đây là phần bạn walk browser (tạo course tenant A → verify tenant B không thấy + cache không poison).

**Kết quả:** ✅ API-layer responds OK (GAP-791 query tenant-scoped, no crash). Cross-tenant leak/cache poison chờ bạn walk 2-tenant.

---

## Flow 4 — Email delivery MailHog: staff-invite (GAP-787) + beta-invite (GAP-702)

**Gap:** GAP-793/787 (kitehub-email thiếu @RabbitListener cho queue `email.send` → email bị drop; fix #1938 thêm consumer + provider router). **Steps:**

1. Trigger staff-invite (Owner mời nhân viên) → MailHog có email staff-invite.
2. Trigger beta-invite (admin approve flow 1) → MailHog có email beta-invite.
   - **Pre-check (đã chạy):** MailHog hiện có sẵn 1 email `staff.invite+...` → consumer đang chạy. Cần verify cả 2 loại sau rebuild.

**Kết quả:** [ ] PASS / [ ] FAIL.

---

## Flow 5 — PDPL consent anonymous (GAP-794) — ✅ PASS (API-layer verified)

**Gap:** GAP-794 (SecurityConfig permitAll trỏ sai endpoint → anonymous 401). **Verified by Claude 2026-05-28:**

```
POST /api/v1/consent/record (anonymous, no auth) → HTTP 201 ✅ (KHÔNG 401 — fix hoạt động)
  body: {visitorId: <UUID>, essentialConsented, analyticsConsented, marketingConsented, consentVersion}
GET  /api/v1/consent/{uuid} (anonymous) → HTTP 200 ✅ (KHÔNG 401)
DB: consent_record row created (visitor_id, essential_consented=t) ✅
```

**Lưu ý DTO:** visitorId PHẢI là UUID; field tên `essentialConsented`/`analyticsConsented`/`marketingConsented` (không phải `necessary`/`analytics`/`marketing`).

**Kết quả:** ✅ PASS — flow 5 sẵn sàng flip GAP-794 DONE sau khi flow 1-4 xong.

---

## Bug catalog (điền khi walk — catalog-then-batch per feature-ship-runtime-walk-mandate.md §3.4)

| # | Flow | Bước | Symptom | File:line dự đoán | Severity |
|---|---|---|---|---|---|
| 1 | (recipe) | login | CSV ghi `/api/v1/auth/login`, thực tế `/api/auth/login` — route versioning inconsistent (`/api/auth` vs `/api/v1/auth`) | AuthController.java:24 vs BetaAccessController:80 | P3 (doc/consistency) |
| 2 | Flow 1 | Bước 5/6 email | beta-invite render `------` (no code) + `/beta/accept` (no token) → user không signup. Var-name drift: sender `claimCode`/`signupUrl` ≠ template `verificationCode`/`inviteUrl`. Sweep: +welcome +beta-invite.html cùng class. **→ GAP-797 P0** | EmailServiceClient.java:707-712 + beta-invite.txt:13-17 | 🔴 P0 (signup-blocking) |
