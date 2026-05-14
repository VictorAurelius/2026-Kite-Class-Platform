# 2FA (Two-Factor Authentication) — Use Cases

**Domain:** TOTP enrollment + verification + recovery code lifecycle
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)

> **Wave 79 Bucket 0 status:** 5 use cases dưới reflect TwoFactorController shipped Wave 72b Bucket A. Bucket A Wave 79 thêm `/api/v1/` prefix + backward-compat alias.

---

## UC-AUTH-2FA-ENROLL-INIT — User initiate 2FA enrollment

**Actor:** Authenticated tenant user (role OWNER / STAFF / PLATFORM_ADMIN) muốn bật 2FA bằng authenticator app (Google Authenticator, Authy, 1Password).
**Trigger:** User → Settings → Security → click "Bật 2FA".
**Endpoint:** `POST /api/v1/auth/2fa/enroll-init` (alias `POST /api/auth/2fa/enroll-init`)
**Rules:** BR-AUTH-2FA-001 (secret entropy), BR-AUTH-2FA-002 (recovery codes), BR-AUTH-2FA-003 (challenge token), BR-AUTH-2FA-008 (step-up password).

### Pre-condition

- User đã login (access token hợp lệ).
- User đã re-prove password trong ≤ 5 phút → BE đã issue challenge JWT với purpose `TWO_FACTOR_ENROLL` (re-login flow nếu hết).
- User CHƯA bật 2FA (else 409 `ALREADY_ENROLLED`).

### Happy path

1. FE call `POST /api/v1/auth/2fa/enroll-init` với header `Authorization: Bearer <challenge_token>`.
2. BE verify challenge JWT purpose=`TWO_FACTOR_ENROLL`, exp > now (per BR-AUTH-2FA-003).
3. BE generate TOTP secret 32-byte (BR-AUTH-2FA-001) + encrypt qua AES-256-GCM, store `users.totp_secret_encrypted` (chưa commit `totp_enabled=true`).
4. BE generate 10 recovery codes plaintext + BCrypt hash + persist `recovery_codes` table (status=PENDING, gắn với enroll session).
5. BE return 200 với `EnrollInitResponse`: `{ secret, otpauthUri, qrCodeDataUri, recoveryCodes[10] }`.
6. FE render QR code (user scan bằng authenticator app) + show recovery codes ONCE với note "Lưu codes này nơi an toàn — chỉ hiện 1 lần".
7. FE chuyển sang form nhập 6-digit TOTP để confirm (UC-AUTH-2FA-ENROLL-CONFIRM).

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 2 | Challenge token missing/invalid | 401 | `INVALID_CHALLENGE` | Redirect login → re-prove password |
| 2 | Challenge token expired (>10 min) | 410 | `CHALLENGE_EXPIRED` | Toast "Phiên xác thực hết hạn"; redirect login |
| 2 | Wrong purpose claim | 401 | `INVALID_CHALLENGE` | Redirect login (defensive — should not happen normally) |
| 3 | User already enrolled (`totp_enabled=true`) | 409 | `ALREADY_ENROLLED` | Toast "Đã bật 2FA"; redirect Settings |
| (gw) | Rate limit (5/min/user per BR-AUTH-2FA-009) | 429 | `RATE_LIMITED` | Toast "Thử lại sau 60s"; disable button |

### FE behavior notes

- QR code render qua `qrCodeDataUri` (base64 PNG); fallback plain text `secret` cho user nhập manual.
- Recovery codes hiển thị monospace + button "Sao chép tất cả" + button "Tải file .txt".
- Banner cảnh báo: "Bạn sẽ chỉ thấy recovery codes này 1 lần. Nếu mất, dùng 'Tạo lại recovery codes' (cần TOTP fresh)."
- Cancel button: KHÔNG commit `totp_enabled` — secret được dọn dẹp trong job cron-scheduled (24h).

---

## UC-AUTH-2FA-ENROLL-CONFIRM — User confirm enrollment với first TOTP code

**Actor:** Tiếp UC-AUTH-2FA-ENROLL-INIT.
**Trigger:** User scan QR code → authenticator app hiển thị 6-digit code → user nhập + click "Xác nhận".
**Endpoint:** `POST /api/v1/auth/2fa/enroll-confirm` (alias `POST /api/auth/2fa/enroll-confirm`)
**Rules:** BR-AUTH-2FA-003 (challenge), BR-AUTH-2FA-004 (TOTP window).

### Pre-condition

- UC-AUTH-2FA-ENROLL-INIT đã ship pending secret.
- Challenge JWT vẫn còn hợp lệ (purpose=`TWO_FACTOR_ENROLL`).

### Happy path

1. FE call `POST /api/v1/auth/2fa/enroll-confirm` với `{ totpCode: "123456" }` + header `Authorization: Bearer <challenge_token>`.
2. BE verify challenge JWT (BR-AUTH-2FA-003).
3. BE decrypt pending TOTP secret từ `users.totp_secret_encrypted`.
4. BE generate expected TOTP cho current + ±1 window (BR-AUTH-2FA-004); compare với submitted code.
5. Khớp → BE commit `users.totp_enabled=true`, recovery codes status=ACTIVE, issue access + refresh token (login complete với 2FA enabled).
6. BE return 200 với `EnrollConfirmResponse`: `{ accessToken, refreshToken, twoFactorEnabled: true }`.
7. FE replace QR view với success message "2FA đã được bật" + redirect Settings (badge "2FA: BẬT" hiển thị).

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 2 | Challenge invalid/expired | 401/410 | `INVALID_CHALLENGE`/`CHALLENGE_EXPIRED` | Redirect login |
| 4 | TOTP code không khớp | 401 | `INVALID_TOTP` | Inline error "Mã không đúng. Đồng hồ điện thoại lệch?"; allow re-try |
| 4 | TOTP code malformed (≠6 digits, non-numeric) | 400 | `INVALID_REQUEST` | Inline error "Mã phải gồm 6 chữ số" |
| 5 | User somehow đã enroll trước (race) | 409 | `ALREADY_ENROLLED` | Toast + redirect Settings |

### FE behavior notes

- TOTP input field: `inputMode="numeric"`, `pattern="\d{6}"`, auto-submit khi đủ 6 chars.
- Show hint "Mã thay đổi mỗi 30s" + countdown indicator.
- After 3 failed attempts inline (≠429), show alt link "Vấn đề? Quay lại Settings" (cancel enrollment).

---

## UC-AUTH-2FA-VERIFY — User submit TOTP / recovery code at login

**Actor:** User đã enable 2FA, đang login.
**Trigger:** `POST /api/auth/login` returns `{ requires2fa: true, challengeToken }` → FE redirect 2FA verify page.
**Endpoint:** `POST /api/v1/auth/2fa/verify` (alias `POST /api/auth/2fa/verify`)
**Rules:** BR-AUTH-2FA-002 (recovery), BR-AUTH-2FA-003 (challenge), BR-AUTH-2FA-004 (TOTP window).

### Pre-condition

- `POST /api/auth/login` thành công (password đúng) nhưng user có `totp_enabled=true`.
- BE đã issue challenge JWT purpose=`TWO_FACTOR_LOGIN`, TTL 10 min.

### Happy path (TOTP)

1. FE call `POST /api/v1/auth/2fa/verify` với `{ challengeToken, totpCode: "123456" }`.
2. BE verify challenge JWT purpose=`TWO_FACTOR_LOGIN`.
3. BE decrypt TOTP secret, compute expected codes within ±1 window (BR-AUTH-2FA-004).
4. Khớp → BE issue access + refresh token; reset `failedLoginAttempts` counter; emit audit log (BR-AUTH-2FA-010 success).
5. BE return 200 với `VerifyResponse`: `{ accessToken, refreshToken }`.
6. FE store tokens, redirect dashboard.

### Happy path (recovery code fallback)

1. User mất authenticator → click "Dùng recovery code".
2. FE call same endpoint với `{ challengeToken, recoveryCode: "ABCD-1234" }`.
3. BE BCrypt compare submitted code với tất cả ACTIVE recovery codes của user.
4. Khớp → BE mark `recovery_codes.used_at=now`, issue tokens, emit audit log với `recoveryCodeUsed=true`.
5. FE redirect dashboard + toast "Đã dùng 1 recovery code. Bạn còn N codes. Khuyến nghị 'Tạo lại recovery codes' sau khi truy cập."

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 2 | Challenge invalid | 401 | `INVALID_CHALLENGE` | Redirect login (start over) |
| 2 | Challenge expired | 410 | `CHALLENGE_EXPIRED` | Toast "Phiên hết hạn"; redirect login |
| 3-4 | TOTP code sai | 401 | `INVALID_TOTP` | Inline error; increment failedAttempts counter |
| 3-4 | Recovery code sai/đã dùng | 401 | `INVALID_RECOVERY_CODE` | Inline error "Mã không hợp lệ hoặc đã dùng" |
| (svc) | failedAttempts ≥5 trong 15 min | 401 | `ACCOUNT_LOCKED` | (cross-ref GAP-552/553) Banner "Tài khoản tạm khóa 15 phút"; CTA "Liên hệ hỗ trợ" |
| (gw) | Rate limit (10/min/IP per BR-AUTH-2FA-009) | 429 | `RATE_LIMITED` | Toast "Thử lại sau 60s" |

### FE behavior notes

- Page có 2 tabs: "Mã xác thực" (TOTP, default) + "Recovery code".
- Recovery code input format: `XXXX-XXXX` (8 hex chars + dash, auto-insert dash sau 4 chars).
- Bên dưới có link "Mất thiết bị? Liên hệ hỗ trợ" → support ticket form pre-fill subject.

---

## UC-AUTH-2FA-RECOVERY-CODES-REGEN — User regenerate recovery codes

**Actor:** Authenticated user có 2FA enabled, muốn tạo lại recovery codes (vd: đã dùng vài codes, mất printout).
**Trigger:** Settings → Security → "Tạo lại recovery codes".
**Endpoint:** `POST /api/v1/auth/2fa/recovery-codes/regenerate` (alias `POST /api/auth/2fa/recovery-codes/regenerate`)
**Rules:** BR-AUTH-2FA-002, BR-AUTH-2FA-006 (fresh TOTP proof).

### Pre-condition

- User đã enable 2FA (`totp_enabled=true`).
- User có access token hợp lệ.
- User có fresh TOTP code (≤ 2 min trước call) — UI prompt nhập TOTP trước khi submit.

### Happy path

1. FE prompt user nhập TOTP code 6-digit hiện tại.
2. FE call `POST /api/v1/auth/2fa/recovery-codes/regenerate` với `{ totpCode: "123456" }` + access token.
3. BE verify access token + verify TOTP code (BR-AUTH-2FA-006 fresh proof).
4. BE atomic transaction:
   - Mark tất cả ACTIVE recovery codes của user thành `status=INVALIDATED`.
   - Generate 10 new codes plaintext + BCrypt hash + insert `recovery_codes` (status=ACTIVE).
5. BE return 200 với `RegenerateResponse`: `{ recoveryCodes: [10 plaintext codes] }`.
6. FE render codes 1 lần + warning "Codes cũ đã bị vô hiệu. Lưu codes mới ngay."
7. BE emit audit log (BR-AUTH-2FA-010).

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 3 | Access token missing/invalid | 401 | `UNAUTHORIZED` | Redirect login |
| 3 | TOTP code không khớp | 401 | `INVALID_TOTP` | Inline error; allow re-try |
| 3 | TOTP code stale (>2 min từ token issue) | 412 | `TOTP_PRECONDITION_FAILED` | Toast "Vui lòng nhập mã mới"; refresh form |
| (gw) | Rate limit (3/min/user) | 429 | `RATE_LIMITED` | Toast |

---

## UC-AUTH-2FA-DISABLE — User disable 2FA (non-admin only)

**Actor:** Authenticated user có 2FA enabled, role OWNER hoặc STAFF (NOT PLATFORM_ADMIN).
**Trigger:** Settings → Security → "Tắt 2FA".
**Endpoint:** `POST /api/v1/auth/2fa/disable` (alias `POST /api/auth/2fa/disable`)
**Rules:** BR-AUTH-2FA-005 (cannot disable cho admin), BR-AUTH-2FA-008 (re-prove password).

### Pre-condition

- User có 2FA enabled.
- User role không phải PLATFORM_ADMIN (else 403 `CANNOT_DISABLE_2FA_FOR_ADMIN`).
- User confirm re-prove password trong modal.

### Happy path

1. FE prompt confirm password + TOTP code trong modal.
2. FE call `POST /api/v1/auth/2fa/disable` với `{ password: "...", totpCode: "123456" }` + access token.
3. BE verify access token; check role ≠ PLATFORM_ADMIN (BR-AUTH-2FA-005).
4. BE verify password (BCrypt compare) + verify TOTP code (BR-AUTH-2FA-004).
5. BE atomic:
   - Set `users.totp_enabled=false`, clear `users.totp_secret_encrypted`.
   - Mark all `recovery_codes` của user thành `status=DISABLED`.
6. BE return 200 với `DisableResponse`: `{ twoFactorEnabled: false }`.
7. FE redirect Settings, badge "2FA: TẮT" hiển thị, banner cảnh báo "2FA đã tắt — tài khoản bạn ít an toàn hơn".
8. BE emit audit log (BR-AUTH-2FA-010 với severity=HIGH).

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 3 | Role PLATFORM_ADMIN | 403 | `CANNOT_DISABLE_2FA_FOR_ADMIN` | Modal hiển thị banner "Admin role bắt buộc 2FA — không thể tắt"; disable submit |
| 4 | Password sai | 401 | `INVALID_PASSWORD` | Inline error trên password field |
| 4 | TOTP code sai | 401 | `INVALID_TOTP` | Inline error trên TOTP field |
| (gw) | Rate limit (5/min/user) | 429 | `RATE_LIMITED` | Toast |

### FE behavior notes

- Disable button trong Settings có warning style (red border) + tooltip "Sẽ làm giảm bảo mật".
- Confirm modal có double-confirmation pattern: 1) checkbox "Tôi hiểu rủi ro", 2) password + TOTP, 3) button "Tắt 2FA" disabled until cả 2 thoả.
- Sau disable thành công, banner cố định dashboard "2FA đã tắt — bật lại tại Settings → Security" trong 7 ngày.
