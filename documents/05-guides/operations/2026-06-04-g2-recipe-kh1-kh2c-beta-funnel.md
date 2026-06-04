---
title: G2 Human Test Recipe — KH-1 beta funnel + KH-2c owner login + onboarding wizard
audience: dev
created: 2026-06-04
scope: Flow Verification Campaign G2 handoff for KH-1 + KH-2c chain (G1 PASS from Wave flow-kh1)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-04-flow-kh1-beta-funnel.md
  - documents/03-planning/waves/wave-2026-06-03-flow-kh2-auth-onboarding.md
---

# G2 Human Test Recipe — KH-1 + KH-2c chain

**Mục tiêu:** Bạn (user) tự test full chain Phase 1 BETA invite flow qua browser, confirm trải nghiệm thật đúng. Kết quả → tôi flip campaign rows KH-1 + KH-2c → ✅ THÔNG (sau G3 production parity).

**Thời lượng ước tính:** ~10-15 phút (5-7 phút walk + verify steps).

**Prereq state (đã có sẵn từ session 2026-06-04 Wave flow-kh1 G1 PASS):**
- Stack đang UP healthy (verify: `docker ps --format "table {{.Names}}\t{{.Status}}"`)
- Admin TOTP đã enroll trong DB (encrypted column — re-enroll nếu cần)
- GAP-916 fix shipped main → gateway header propagation OK
- Test user mẫu: `prospect+kh1walk-1780540178@example.com` (đã SIGNED_UP từ G1 — KHÔNG dùng lại email này, tạo mới)

---

## Setup

1. Browser mở **http://localhost:3001** (KiteHub frontend)
2. F12 DevTools → tab **Network** → filter Fetch/XHR (để watch API calls)
3. Tab khác mở **MailHog UI**: http://localhost:8025 (sẽ check email)
4. Optional: terminal mở để query DB nếu cần debug

---

## Bước 1 — Submit beta access request (KH-1.S1)

### Hành động
1. Trên landing KiteHub, click CTA **"Dùng thử miễn phí 14 ngày"** (button chính hoặc trong section pricing)
2. → redirect tới `/request-beta-access`
3. Điền form (dùng email FRESH, KHÔNG reuse):
   - **Email:** `g2test-<your-name>@example.com` (vd `g2test-an@example.com`)
   - **Họ tên / Name:** vd `Nguyễn Văn G2`
   - **Tên tổ chức / Organization name:** vd `G2 Test Center`
   - **Persona:** chọn **P2 Center Owner**
   - **Referral source:** (optional) `g2-walk`
   - **Tick checkbox PDPL consent** (BẮT BUỘC — submit button sẽ disabled nếu chưa tick)
4. Submit form

### ✅ Kỳ vọng (PASS)
- **HTTP 201** trong Network tab cho `POST /api/v1/auth/request-beta-access`
- FE render success screen: "Yêu cầu đã gửi" / "Coordinator sẽ duyệt và email cho bạn"
- KHÔNG có console error trong DevTools

### ⚠️ Sad path (nếu lòi)
- Nếu HTTP 503 + trang "Dịch vụ tạm ngưng" → GAP-918 cold-start circuit breaker. Wait 30s + retry. Nếu vẫn 503 sau 2 phút → catalog blocker
- Nếu HTTP 400 `BETA_CONSENT_REQUIRED` → bạn chưa tick checkbox, tick lại
- Nếu HTTP 409 `BETA_DUPLICATE_EMAIL` → email đã tồn tại, dùng email khác

### 🔍 Verify DB (optional)
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT id, email, status, consent_given FROM beta_access_request ORDER BY id DESC LIMIT 3;"
```
Expect row mới với `status=PENDING` + `consent_given=t`.

---

## Bước 2 — Admin approve request (KH-1.S2 + KH-2a admin auth)

### Setup admin login (tab khác hoặc incognito)

1. Mở tab mới browser → **http://localhost:3001**
2. Click "Đăng nhập" / "Login" → điền:
   - **Email:** `admin@kitehub.com`
   - **Password:** `Admin@KiteHub123`
3. Submit → expect 2FA challenge screen

### 2FA TOTP code

Admin đã enroll TOTP session trước nhưng secret encrypted trong DB → 2 options:

**Option A (Recommended): Reset enrollment + re-enroll**
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "UPDATE users SET totp_enrolled_at=NULL, totp_secret_encrypted=NULL WHERE email='admin@kitehub.com';"
```
Sau đó login lại → FE redirect tới enrollment wizard → scan QR bằng Google Authenticator / Authy / 1Password → nhập 6-digit code → confirm → enrolled

**Option B: Skip 2FA cho local test (chỉ dùng nếu hiểu rủi ro)**

KHÔNG khuyến nghị. 2FA là Phase 1 BETA P0 security gate cho PLATFORM_ADMIN.

### Approve request

1. Sau admin login + 2FA verify → admin dashboard render
2. Click sidebar / menu **"Beta Requests"** → list pending requests
3. Tìm request bạn vừa submit Bước 1 (email `g2test-...@example.com`)
4. Click **"Approve"** button
5. Modal hoặc inline form → điền `approverId: admin@kitehub.com` (hoặc just confirm nếu FE auto-fills) → submit

### ✅ Kỳ vọng
- HTTP 200 cho `POST /api/v1/admin/beta-requests/{id}/approve`
- Row status updates `PENDING → APPROVED`
- FE shows success notification

### 🔍 Verify
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT id, email, status, invite_token, invite_sent_at FROM beta_access_request WHERE email LIKE 'g2test-%' ORDER BY id DESC LIMIT 1;"
```
Expect: `status=APPROVED` + `invite_token` (UUID) NOT NULL + `invite_sent_at` recent timestamp.

---

## Bước 3 — Verify invite email delivered (KH-1.S3)

### Mở MailHog
1. Browser tab **http://localhost:8025**
2. Click **Refresh** (icon top-right)
3. Inbox list → tìm email với:
   - **To:** `g2test-<your-name>@example.com`
   - **Subject:** `Mã truy cập Beta KiteHub của bạn` (UTF-8 quoted-printable encoded ở raw, render tiếng Việt khi UI hiển thị)
4. Click email để mở

### ✅ Kỳ vọng
- Email body render Vietnamese: "Kính gửi anh/chị [Name]," + "Cảm ơn anh/chị đã đăng ký..."
- Body chứa invite URL dạng: `http://localhost:3001/beta-signup/code?code=<6-digit>` (vd `code=169628`)
- Email subject + body Vietnamese tone đúng

### ⚠️ Nếu email không tới
- Wait ~30s (outbox dispatcher poll interval)
- Refresh MailHog
- Verify outbox row dispatched:
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT event_type, topic, dispatched_at FROM subscription_outbox WHERE event_type LIKE '%beta.invite%' ORDER BY created_at DESC LIMIT 3;"
```
Expect `dispatched_at NOT NULL`. Nếu NULL → outbox dispatcher stuck (check `docker logs kitehub-subscription` cho scheduled job errors).

---

## Bước 4 — Click invite link → Register (KH-2b = KH-1.S5)

### Hành động
1. Copy invite URL từ MailHog email body
2. Paste vào browser **incognito tab** (clean state)
3. → redirect tới `/beta-signup/code?code=<6-digit>`
4. FE auto-exchange claim code → invite token → fetch pre-fill data
5. Form prefilled với email + name + organization (từ Bước 1)
6. Điền 2 field còn thiếu:
   - **Mật khẩu / Password:** vd `G2Walk@KiteHub123` (≥8 ký tự, có chữ + số + special)
   - **Subdomain:** vd `g2test-an` (lowercase kebab, ≤100 chars, unique)
7. (Optional tick) accept ToS / privacy
8. Submit form

### ✅ Kỳ vọng
- HTTP 200 cho `POST /api/v1/auth/beta-signup`
- Auto-login → redirect dashboard
- Owner role JWT issued
- Tenant provisioned (subdomain registered)

### ⚠️ Sad path
- HTTP 400 missing field → check `ownerPassword` + `subdomain` đều điền (FE form should require)
- HTTP 404 token invalid → invite link expired hoặc đã used (TTL 24h, single-use)
- HTTP 409 subdomain exists → đổi subdomain khác
- HTTP 503 gateway cold → wait + retry (GAP-918)

### 🔍 Verify
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT u.email, u.role, u.email_verified, i.subdomain, i.tier, i.status FROM users u JOIN instances i ON i.owner_id=u.id WHERE u.email LIKE 'g2test-%' ORDER BY u.created_at DESC LIMIT 1;"
```
Expect: `role=OWNER`, `email_verified=t`, `subdomain=<yours>`, `tier=FREE`, `status=TRIAL`.

```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT status FROM beta_access_request WHERE email LIKE 'g2test-%' ORDER BY id DESC LIMIT 1;"
```
Expect: `SIGNED_UP`.

---

## Bước 5 — Owner login + Onboarding wizard (KH-2c chain)

### Hành động
1. (Nếu auto-login từ Bước 4 đã đưa bạn vào dashboard → skip login, đi thẳng tới step 4)
2. Nếu chưa → logout (icon user → "Đăng xuất")
3. Click "Đăng nhập" → email + password vừa tạo Bước 4
4. → redirect dashboard

### ✅ Kỳ vọng dashboard
- Render với organization name của bạn ở header
- Section / widget **"Onboarding"** hiển thị checklist 5 step:
  1. PROFILE_SETUP / "Hoàn thiện hồ sơ"
  2. INVITE_TEAM / "Mời nhân viên"
  3. IMPORT_DATA / "Nhập dữ liệu"
  4. CREATE_FIRST_CLASS / "Tạo lớp đầu tiên"
  5. EXPLORE_FEATURES / "Khám phá tính năng"
- Progress bar 0% / 0/5 hoàn thành

### Tương tác
1. Click step 1 "PROFILE_SETUP" → mark complete (checkbox / button)
2. Progress bar → 20% / 1/5
3. Network tab: `PUT /api/v1/onboarding-progress` → HTTP 200

### Refresh test
1. Refresh page (Cmd+R / F5)
2. Step 1 vẫn marked complete (state persisted)
3. Progress bar vẫn 20%

### 🔍 Verify
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT tenant_id, completion_percent, last_updated_at FROM onboarding_progress WHERE tenant_id IN (SELECT id FROM instances WHERE owner_id IN (SELECT id FROM users WHERE email LIKE 'g2test-%')) ORDER BY last_updated_at DESC LIMIT 1;"
```
Expect: `completion_percent=20` + `last_updated_at` recent.

---

## Bước 6 — Sad path quick checks

### Wrong password login
1. Logout
2. Login với email đúng + password SAI
3. ✅ Expect: error message "Email hoặc mật khẩu không đúng" (note: HTTP 400 hay 401 cosmetic — GAP-917 spec drift)

### Re-use invite link
1. Mở lại invite URL từ MailHog (Bước 3 link)
2. Submit form
3. ✅ Expect: HTTP 404 `TOKEN_NOT_FOUND` hoặc 409 `ALREADY_USED` + error message

### Try /register direct (Phase 1 BETA gate)
1. Browser → `http://localhost:3001/register`
2. ✅ Expect: HTTP 307 redirect → `/request-beta-access` (chứng minh Phase 1 BETA gate self-service)

---

## Báo kết quả

### Khi G2 xong, báo lại 1 trong 4:

**✅ FULL PASS:** Tất cả 5 bước + sad path PASS như mong đợi → tôi flip campaign rows KH-1 + KH-2c → ✅ G1+G2 (chờ G3 production parity)

**⚠️ MOSTLY PASS với cosmetic issues:** PASS core flow nhưng có UX cosmetic (font, color, label, copy text) → catalog gap cho FE polish

**🔴 BLOCKING ISSUE:** Có step không PASS được hoặc data integrity issue → tôi catalog blocker + fix loop tiếp + re-walk

**❓ UNCLEAR / cần help:** Bước nào không hiểu / không tìm thấy nút / DB query khác expected → ping tôi với screenshot hoặc error message

---

## Troubleshooting nhanh

| Symptom | Quick fix |
|---|---|
| Stack down (FE 502) | `bash kitehub/scripts/up.sh` |
| Container unhealthy | `docker logs <container-name>` xem error |
| Admin TOTP secret mất | SQL reset enrollment (Bước 2 §Option A) |
| Gateway 503 sau action | Wait 30s + retry (GAP-918 cold-start) |
| MailHog không nhận email | Wait 30s outbox poll + verify outbox `dispatched_at` |
| Browser cache cũ | Hard reload Cmd+Shift+R hoặc clear browser data |

---

## Production parity preview (G3 — sau G2 PASS)

Sau G2, production verify cần:
- `EMAIL_VERIFICATION_ENABLED=true` (local `false` bypass)
- AWS SES email signing (vs MailHog local)
- Cloudflare DNS invite URL reachable (`kitehub.me` apex)
- Captcha enabled (local `captcha.enabled=false`)
- Browser test trên production-equivalent env

Tracking: campaign §1 G3 + wave plan §7.3 row 5.

---

Generated 2026-06-04 cho Wave flow-kh1 G1 PASS handoff.
