# 2FA (Two-Factor Authentication) — API Contract

**Domain:** TOTP enrollment + verification + recovery codes (Wave 72b GAP-516 implementation → Wave 79 Bucket 0 contract GAP-547)
**Source-of-truth controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TwoFactorController.java`
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)

This contract là source-of-truth cross-layer cho Wave 79 Bucket A, consumed by:
- Bucket A (GAP-547) — Add `/api/v1/auth/2fa/*` versioned path + backward-compat alias 30 days
- FE auth client — typed schema `kitehub-frontend/src/lib/api/auth-2fa.ts` (consumer)
- Security audit checklist — `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.4 (2FA mandatory cho admin)

---

## Endpoints overview

Five endpoints. Canonical path `/api/v1/auth/2fa/*` (Wave 79 Bucket A target); backward-compat alias `/api/auth/2fa/*` honored 30 days (until 2026-06-14 per BR-AUTH-2FA-007).

| Method | Canonical path | Alias path | Use case |
|--------|---------------|------------|----------|
| POST | `/api/v1/auth/2fa/enroll-init` | `/api/auth/2fa/enroll-init` | UC-AUTH-2FA-ENROLL-INIT |
| POST | `/api/v1/auth/2fa/enroll-confirm` | `/api/auth/2fa/enroll-confirm` | UC-AUTH-2FA-ENROLL-CONFIRM |
| POST | `/api/v1/auth/2fa/verify` | `/api/auth/2fa/verify` | UC-AUTH-2FA-VERIFY |
| POST | `/api/v1/auth/2fa/recovery-codes/regenerate` | `/api/auth/2fa/recovery-codes/regenerate` | UC-AUTH-2FA-RECOVERY-CODES-REGEN |
| POST | `/api/v1/auth/2fa/disable` | `/api/auth/2fa/disable` | UC-AUTH-2FA-DISABLE |

**Auth model:**
- `enroll-init` / `enroll-confirm`: require challenge JWT (`Authorization: Bearer <challenge_token>`) với purpose `TWO_FACTOR_ENROLL`.
- `verify`: require challenge JWT trong body (`challengeToken` field) với purpose `TWO_FACTOR_LOGIN`. NO Authorization header.
- `recovery-codes/regenerate` / `disable`: require regular access token (`Authorization: Bearer <access_token>`) + body proof (TOTP code và/hoặc password).

---

## POST /api/v1/auth/2fa/enroll-init

**Use case:** UC-AUTH-2FA-ENROLL-INIT
**Auth:** Challenge JWT (purpose `TWO_FACTOR_ENROLL`) trong `Authorization: Bearer ...`
**Request body:** (empty)

**Response 200 OK (`EnrollInitResponse`):**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "otpauthUri": "otpauth://totp/KiteHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=KiteHub",
  "qrCodeDataUri": "data:image/png;base64,iVBORw0KGgo...",
  "recoveryCodes": [
    "ABCD-1234",
    "EFGH-5678",
    "IJKL-9012",
    "MNOP-3456",
    "QRST-7890",
    "UVWX-1234",
    "YZAB-5678",
    "CDEF-9012",
    "GHIJ-3456",
    "KLMN-7890"
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `secret` | string (base32) | TOTP secret cho manual entry trong authenticator app. ≥16 chars. |
| `otpauthUri` | string | RFC 6238 `otpauth://` URI cho QR code generation. |
| `qrCodeDataUri` | string (data URI) | Base64 PNG ≤2KB của QR code (FE render qua `<img src={...} />`). |
| `recoveryCodes` | string[10] | 10 plaintext codes format `XXXX-XXXX` (8 hex + dash). SHOW ONCE — không persist plaintext sau response. |

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 401 | `INVALID_CHALLENGE` | Challenge token missing/invalid/wrong purpose |
| 410 | `CHALLENGE_EXPIRED` | Challenge JWT exp < now |
| 409 | `ALREADY_ENROLLED` | User đã enable 2FA |
| 429 | `RATE_LIMITED` | Gateway rate limit (5 req/min/user) |

---

## POST /api/v1/auth/2fa/enroll-confirm

**Use case:** UC-AUTH-2FA-ENROLL-CONFIRM
**Auth:** Challenge JWT (purpose `TWO_FACTOR_ENROLL`) trong `Authorization: Bearer ...`

**Request body (`EnrollConfirmRequest`):**
```json
{
  "totpCode": "123456"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `totpCode` | string | yes | EXACTLY 6 numeric chars (`^\d{6}$`). |

**Response 200 OK (`EnrollConfirmResponse`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLXV1aWQiLCJ0eXBlIjoiYWNjZXNzIi...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLXV1aWQiLCJ0eXBlIjoicmVmcmVzaCI...",
  "twoFactorEnabled": true
}
```

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | string (JWT) | Full-access JWT TTL 24h. |
| `refreshToken` | string (JWT) | Refresh JWT TTL 7d. |
| `twoFactorEnabled` | boolean | Confirmation flag (always `true` for success path). |

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `INVALID_REQUEST` | `totpCode` malformed |
| 401 | `INVALID_CHALLENGE` | Challenge invalid/wrong purpose |
| 401 | `INVALID_TOTP` | Code không khớp ±1 window |
| 410 | `CHALLENGE_EXPIRED` | Challenge exp |
| 409 | `ALREADY_ENROLLED` | Race condition — user enrolled bằng session khác |
| 429 | `RATE_LIMITED` | Gateway 5 req/min/user |

---

## POST /api/v1/auth/2fa/verify

**Use case:** UC-AUTH-2FA-VERIFY
**Auth:** Challenge JWT trong BODY (`challengeToken`), purpose `TWO_FACTOR_LOGIN`. **No Authorization header.**

**Request body (`VerifyRequest`):**
```json
{
  "challengeToken": "eyJhbGciOiJIUzI1NiJ9...",
  "totpCode": "123456",
  "recoveryCode": null
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `challengeToken` | string (JWT) | yes | Issued bởi `POST /api/auth/login` khi `requires2fa=true`. |
| `totpCode` | string | conditional | Either `totpCode` OR `recoveryCode` MUST be present (XOR). Format `^\d{6}$`. |
| `recoveryCode` | string | conditional | XOR với `totpCode`. Format `^[A-Z0-9]{4}-[A-Z0-9]{4}$` (8 hex + dash). |

**Response 200 OK (`VerifyResponse`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "recoveryCodeUsed": false,
  "remainingRecoveryCodes": null
}
```

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | string (JWT) | Access JWT TTL 24h. |
| `refreshToken` | string (JWT) | Refresh JWT TTL 7d. |
| `recoveryCodeUsed` | boolean | `true` nếu user dùng recovery code (FE prompt regenerate). |
| `remainingRecoveryCodes` | integer? | Số recovery codes ACTIVE còn lại (null nếu TOTP path). |

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `INVALID_REQUEST` | Neither `totpCode` nor `recoveryCode` provided, OR both, OR malformed format |
| 401 | `INVALID_CHALLENGE` | Challenge invalid |
| 401 | `INVALID_TOTP` | TOTP code sai |
| 401 | `INVALID_RECOVERY_CODE` | Recovery code sai hoặc đã dùng |
| 401 | `ACCOUNT_LOCKED` | failedAttempts ≥5 trong 15 min (per `pre-launch-auth-hardening-checklist.md` §2.2; Wave 79 Bucket C target) |
| 410 | `CHALLENGE_EXPIRED` | Challenge exp |
| 429 | `RATE_LIMITED` | Gateway 10 req/min/IP |

---

## POST /api/v1/auth/2fa/recovery-codes/regenerate

**Use case:** UC-AUTH-2FA-RECOVERY-CODES-REGEN
**Auth:** Access token trong `Authorization: Bearer ...`

**Request body (`RegenerateRequest`):**
```json
{
  "totpCode": "123456"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `totpCode` | string | yes | Fresh TOTP code (≤2 min). Format `^\d{6}$`. |

**Response 200 OK (`RegenerateResponse`):**
```json
{
  "recoveryCodes": [
    "NEWA-1111",
    "NEWB-2222",
    "NEWC-3333",
    "NEWD-4444",
    "NEWE-5555",
    "NEWF-6666",
    "NEWG-7777",
    "NEWH-8888",
    "NEWI-9999",
    "NEWJ-0000"
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `recoveryCodes` | string[10] | 10 plaintext new codes. SHOW ONCE. Old codes invalidated atomically. |

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 401 | `UNAUTHORIZED` | Access token missing/invalid/expired |
| 401 | `INVALID_TOTP` | TOTP code sai |
| 412 | `TOTP_PRECONDITION_FAILED` | TOTP code stale (>2 min từ access token issue) — call fresh TOTP |
| 429 | `RATE_LIMITED` | Gateway 3 req/min/user |

---

## POST /api/v1/auth/2fa/disable

**Use case:** UC-AUTH-2FA-DISABLE
**Auth:** Access token trong `Authorization: Bearer ...`

**Request body (`DisableRequest`):**
```json
{
  "password": "user-current-password",
  "totpCode": "123456"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `password` | string | yes | BCrypt-verify trên server side. |
| `totpCode` | string | yes | Fresh TOTP. Format `^\d{6}$`. |

**Response 200 OK (`DisableResponse`):**
```json
{
  "twoFactorEnabled": false
}
```

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 401 | `UNAUTHORIZED` | Access token missing/invalid |
| 401 | `INVALID_PASSWORD` | Password sai |
| 401 | `INVALID_TOTP` | TOTP code sai |
| 403 | `CANNOT_DISABLE_2FA_FOR_ADMIN` | Role PLATFORM_ADMIN (per BR-AUTH-2FA-005) |
| 429 | `RATE_LIMITED` | Gateway 5 req/min/user |

---

## Backward-compat alias path policy

Per BR-AUTH-2FA-007:
- Until **2026-06-14**: cả `/api/v1/auth/2fa/*` (canonical) và `/api/auth/2fa/*` (alias) đều hoạt động. Identical request/response shape.
- After **2026-06-14**: alias return `410 Gone` với header `X-Deprecated-Path: /api/v1/auth/2fa/<endpoint>`. Body: `{ "error": "PATH_DEPRECATED", "message": "Use /api/v1/auth/2fa/<endpoint>" }`.

FE migration: Wave 79 Bucket A swap base URL constant; mobile / SDK clients tự upgrade ≤ 30 ngày.

---

## Rate limits

Per BR-AUTH-2FA-009 + `pre-launch-auth-hardening-checklist.md` §2.1:

| Endpoint | Limit | Key resolver |
|----------|-------|--------------|
| `enroll-init` / `enroll-confirm` | 5 req/min/user (access token claim) | userKeyResolver |
| `verify` | 10 req/min/IP | ipKeyResolver |
| `recovery-codes/regenerate` | 3 req/min/user | userKeyResolver |
| `disable` | 5 req/min/user | userKeyResolver |

Exceed → `429 RATE_LIMITED` với header `Retry-After: 60`.

---

## Audit logging

Per BR-AUTH-2FA-010, every endpoint emit `admin_audit_log` row với:
```json
{
  "timestamp": "2026-05-14T09:00:00Z",
  "userId": "user-uuid",
  "action": "AUTH_2FA_ENROLL_CONFIRM" | "AUTH_2FA_VERIFY_SUCCESS" | "AUTH_2FA_VERIFY_FAIL" | "AUTH_2FA_RECOVERY_REGEN" | "AUTH_2FA_DISABLE",
  "requestIp": "1.2.3.4",
  "userAgent": "Mozilla/5.0 ...",
  "status": "SUCCESS" | "FAIL",
  "severity": "INFO" | "HIGH",
  "metadata": { "recoveryCodeUsed": false }
}
```

`severity=HIGH` cho:
- Disable success
- ≥5 failed verify trong 15 min (account_locked event)
- Recovery code used

---

## Side effects

- Enroll-confirm thành công → emit `auth.2fa.enabled` event qua outbox (subscribers: email "Đã bật 2FA cho tài khoản"; analytics).
- Disable success → emit `auth.2fa.disabled` event (subscribers: email cảnh báo "2FA đã tắt"; audit log severity=HIGH).
- Recovery code use → emit `auth.2fa.recovery-code-used` event (subscribers: email "Recovery code đã dùng — bạn còn N codes").

---

## Related

- BR-AUTH-2FA-001..010: `documents/01-business/kitehub/auth-2fa/rules.md`
- UC-AUTH-2FA-{ENROLL-INIT,ENROLL-CONFIRM,VERIFY,RECOVERY-CODES-REGEN,DISABLE}: `documents/01-business/kitehub/auth-2fa/use-cases.md`
- Implementation: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/**` (Wave 72b Bucket A — PR #1301)
- Parent auth contract: `documents/01-business/kitehub/auth/api-contract.md`
- Wave 79 plan: `documents/03-planning/waves/wave-2026-05-14-79-beta-invite-close-out.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
- Auth hardening: `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.4 (admin mandatory 2FA)
- Gap: GAP-547 (this contract closes P0 v1.0.0-rc gate API contract audit finding)
