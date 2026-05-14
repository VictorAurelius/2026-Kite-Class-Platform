# Authentication — API Contract

**Domain:** Authentication (2FA + lockout + login audit) — Wave 72b foundation per `pre-launch-auth-hardening-checklist.md` OWASP A07
**Source-of-truth controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TwoFactorController.java` (NEW per GAP-516 Wave 72a Bucket A)
**Last verified:** 2026-05-14 (Wave 72b Bucket 0 Foundation)

This contract is the cross-layer source-of-truth consumed by:
- Wave 72b **Bucket A** BE (GAP-516 TwoFactorController + GAP-515 lockout extension + GAP-517 LoginAuditService) — creates these endpoints
- Wave 72b **Bucket B** FE (GAP-518/519 admin 2FA wizard + recovery codes UI) — consumes MSW handlers shipped this PR, then real endpoints post-merge
- Wave 72b **Bucket C** BE (GAP-517 login audit service event signature) — reuses event emission contract here
- Wave 72b **Bucket D** Email templates (GAP-517 `admin-new-login-alert` template) — consumes event payload here

**Scope clarification:** This file focuses on 2FA-specific endpoints + login response shape extension. Existing endpoints already documented:
- `POST /api/v1/auth/request-beta-access` → see `documents/01-business/kitehub/beta-access/api-contract.md`
- `POST /api/auth/login` / `register` / `refresh` / `verify-email` / `resend-verification` → planned future `documents/01-business/kitehub/auth-session/` sub-domain (not yet split out). Login response shape EXTENSION documented at end of this file under §"Login endpoint extension".

---

## Endpoints

### POST /api/auth/2fa/enroll-init

**Use case:** UC-AUTH-002 — First-time 2FA enrollment (PLATFORM_ADMIN mandatory)
**Auth:** Bearer with valid `challenge_token` (from POST /api/auth/login when `requires2fa_enrollment: true`) OR valid access_token for already-logged users opting in (Phase 1.5+ scope)
**Idempotent:** No — generates new secret each call (intentional; user might lose first response before confirm)

**Request body:** None (empty POST)

**Response 200 OK:**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qr_uri": "otpauth://totp/KiteHub:admin@kitehub.me?secret=JBSWY3DPEHPK3PXP&issuer=KiteHub&algorithm=SHA1&digits=6&period=30",
  "recovery_codes": [
    "ab23cd45",
    "ef67gh89",
    "ij2kmnp3",
    "qr4st5uv",
    "wx6yzab7",
    "cd8efgh9",
    "ij2kmnp4",
    "qr5st6uv",
    "wx7yzab8",
    "cd9efgh2"
  ]
}
```

**Field semantics:**

| Field | Type | Description |
|---|---|---|
| `secret` | string (base32) | 160-bit TOTP secret (32 base32 chars). Per BR-AUTH-006. |
| `qr_uri` | string (otpauth URI) | RFC 6238 standard URI; FE renders as QR for authenticator app scan. |
| `recovery_codes` | string[10] | 10 single-use codes (8 chars each, alphabet excludes 0/o/1/l per BR-AUTH-007). **Shown ONCE in this response only.** |

**Errors:**

| HTTP | Error code | Trigger |
|---|---|---|
| 401 | `INVALID_CHALLENGE` | challenge_token signature invalid OR access_token expired |
| 409 | `ALREADY_ENROLLED` | User has `totp_enrolled_at IS NOT NULL` already |
| 410 | `CHALLENGE_EXPIRED` | challenge_token age > 5 minutes |

**Side effects:** None until `enroll-confirm` is called. Secret + recovery codes generated server-side but NOT yet persisted to user record.

---

### POST /api/auth/2fa/enroll-confirm

**Use case:** UC-AUTH-002 — User submits first TOTP code to confirm enrollment
**Auth:** Bearer with valid `challenge_token` (same one used for `enroll-init`)
**Idempotent:** No — first successful confirm persists state; subsequent calls return 409

**Request body:**
```json
{
  "first_totp_code": "123456"
}
```

**Response 200 OK:**
```json
{
  "enrolled": true,
  "totp_enrolled_at": "2026-05-14T10:23:00Z",
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "...",
    "email": "admin@kitehub.me",
    "role": "PLATFORM_ADMIN",
    "totp_enrolled_at": "2026-05-14T10:23:00Z"
  }
}
```

**Field semantics:**

| Field | Type | Description |
|---|---|---|
| `enrolled` | boolean | Always true on 200 |
| `totp_enrolled_at` | ISO8601 | Timestamp persisted on `users.totp_enrolled_at` |
| `access_token` | JWT | 15-min TTL access token (per BR-AUTH-003) |
| `refresh_token` | JWT | 7-day TTL refresh token |
| `user` | object | User profile (subset for FE consumption) |

**Errors:**

| HTTP | Error code | Trigger |
|---|---|---|
| 401 | `INVALID_TOTP` | First TOTP code does not match generated secret (±1 step skew) |
| 401 | `INVALID_CHALLENGE` | challenge_token invalid |
| 409 | `ALREADY_ENROLLED` | Race condition: another session already confirmed |
| 410 | `CHALLENGE_EXPIRED` | challenge_token age > 5 minutes |

**Side effects on 200:**
- `users.totp_secret = <secret-from-init>` (encrypted at rest via KMS — Phase 1.5+; plaintext in Phase 1 BETA acceptable per ADR)
- `users.totp_enrolled_at = now()`
- 10 rows inserted to `recovery_codes(user_id, code_hash, used_at NULL, created_at)` with bcrypt-hashed codes
- Audit log: `admin_audit_log` row with `action_type=AUTH_2FA_ENROLLED`

---

### POST /api/auth/2fa/verify

**Use case:** UC-AUTH-003 (TOTP path) + UC-AUTH-004 (recovery code path) — Subsequent login 2FA challenge
**Auth:** Bearer with valid `challenge_token` (from POST /api/auth/login when `requires2fa: true`)
**Idempotent:** No — recovery code variant consumes single-use code

**Request body (TOTP variant):**
```json
{
  "challenge_token": "<opaque-token>",
  "totp_code": "123456"
}
```

**Request body (recovery code variant):**
```json
{
  "challenge_token": "<opaque-token>",
  "recovery_code": "ab23cd45"
}
```

Exactly one of `totp_code` OR `recovery_code` MUST be present. If both provided → HTTP 400.

**Response 200 OK (TOTP path):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "...",
    "email": "admin@kitehub.me",
    "role": "PLATFORM_ADMIN"
  }
}
```

**Response 200 OK (recovery code path):** Same as TOTP path + 2 additional fields:
```json
{
  "access_token": "...",
  "refresh_token": "...",
  "user": {...},
  "regenerate_recommended": true,
  "codes_remaining": 9
}
```

**Errors:**

| HTTP | Error code | Trigger |
|---|---|---|
| 400 | `INVALID_REQUEST` | Both totp_code AND recovery_code provided, OR neither |
| 401 | `INVALID_TOTP` | totp_code wrong (±1 skew window failed) |
| 401 | `INVALID_RECOVERY_CODE` | recovery_code wrong OR already used (do NOT distinguish — enumeration defence) |
| 401 | `INVALID_CHALLENGE` | challenge_token signature invalid |
| 410 | `CHALLENGE_EXPIRED` | challenge_token age > 5 minutes |
| 423 | `ACCOUNT_LOCKED` | Lockout triggered by repeated failed TOTPs (per BR-AUTH-001) |

**Side effects on 200:**
- Access + refresh tokens issued
- login_audit_log row written (BR-AUTH-008)
- If new fingerprint AND role=PLATFORM_ADMIN → `admin.login.new-fingerprint` outbox event emitted (UC-AUTH-005)
- If recovery_code variant → that specific `recovery_codes.used_at = now()` AND audit event `auth.recovery-code.used` written

---

### POST /api/auth/2fa/recovery-codes/regenerate

**Use case:** User wants new recovery codes (e.g., after using one, after suspected codebook compromise)
**Auth:** Bearer access_token (must be currently logged in) + recent TOTP within 5 min (re-prove possession)
**Idempotent:** No — each call invalidates previous 10 codes

**Request body:**
```json
{
  "current_totp_code": "654321"
}
```

**Response 200 OK:**
```json
{
  "new_recovery_codes": [
    "p3qrst45",
    "uv6wxyz7",
    "ab8cde29",
    "fg2hij3k",
    "mn4pqr5s",
    "tu6vwx7y",
    "za8bc29d",
    "ef2gh3ij",
    "km4np5qr",
    "st6uv7wx"
  ],
  "previous_codes_invalidated": 10,
  "message": "All previous recovery codes are now invalid. Save these new codes — they will not be shown again."
}
```

**Errors:**

| HTTP | Error code | Trigger |
|---|---|---|
| 401 | `INVALID_TOTP` | current_totp_code wrong — must re-prove TOTP possession |
| 401 | `UNAUTHORIZED` | access_token expired/invalid |
| 412 | `TOTP_PRECONDITION_FAILED` | TOTP code valid but not within last 5 min freshness window (replay defence) |

**Side effects on 200:**
- All existing rows in `recovery_codes` for user_id marked `used_at=now()` (atomic transaction) — soft-delete pattern preserves audit trail
- 10 new rows inserted to `recovery_codes` with bcrypt-hashed new codes
- Audit log: `admin_audit_log` row with `action_type=AUTH_RECOVERY_CODES_REGENERATED`, summary `{previous_count: 10, new_count: 10}`

---

### POST /api/auth/2fa/disable

**Use case:** User wants to disable 2FA on their account (NOT permitted for PLATFORM_ADMIN per BR-AUTH-005)
**Auth:** Bearer access_token (currently logged in) + recent TOTP within 5 min + password reconfirm
**Idempotent:** Yes (calling disable when already disabled returns 200 with `disabled: true`)

**Request body:**
```json
{
  "current_totp_code": "789012",
  "password_reconfirm": "<plaintext-current-password>"
}
```

**Response 200 OK:**
```json
{
  "disabled": true,
  "disabled_at": "2026-05-14T10:30:00Z"
}
```

**Errors:**

| HTTP | Error code | Trigger |
|---|---|---|
| 401 | `INVALID_TOTP` | TOTP code wrong |
| 401 | `INVALID_PASSWORD` | password_reconfirm wrong |
| 403 | `CANNOT_DISABLE_2FA_FOR_ADMIN` | User role = PLATFORM_ADMIN — disable blocked per BR-AUTH-005 |
| 412 | `TOTP_PRECONDITION_FAILED` | TOTP not within last 5 min |

**Side effects on 200 (non-admin only):**
- `users.totp_enrolled_at = NULL`
- `users.totp_secret = NULL`
- All `recovery_codes` for user marked `used_at=now()` (soft-delete; audit trail kept)
- Audit log: `admin_audit_log` action_type=`AUTH_2FA_DISABLED` (logged even for non-admin disable; security signal)
- Next login: user falls back to password-only (no 2FA challenge)

---

## Login endpoint extension (response shape)

Existing endpoint `POST /api/auth/login` (currently documented in `documents/01-business/kitehub/beta-access/api-contract.md` + future `auth-session/api-contract.md` sub-domain) EXTENDS response shape in Wave 72b. Both shapes valid; caller MUST handle:

**Success without 2FA (existing — unchanged):**
```json
{
  "access_token": "...",
  "refresh_token": "...",
  "user": {...}
}
```

**Success with 2FA enrolled (NEW Wave 72b):**
```json
{
  "requires2fa": true,
  "challenge_token": "<opaque-5min-TTL-token>"
}
```

**Success with 2FA enrollment required (NEW Wave 72b — first-time PLATFORM_ADMIN):**
```json
{
  "requires2fa_enrollment": true,
  "challenge_token": "<opaque-5min-TTL-token>"
}
```

**Caller behavior:** FE checks `requires2fa` / `requires2fa_enrollment` flags FIRST before consuming `access_token`. If either flag true → redirect to `/login/2fa-challenge` or `/login/2fa-enroll` with `challenge_token` query param. If neither flag → consume tokens as today.

**Error response 423 LOCKED (NEW Wave 72b per BR-AUTH-002):**
```json
{
  "error": "ACCOUNT_LOCKED",
  "lockedUntil": "2026-05-14T10:38:00Z",
  "attemptsRemaining": 0
}
```
Headers: `Retry-After: 900` (seconds until `lockedUntil`)

---

## Outbox event payloads

Events emitted by Auth domain via outbox pattern (consumed cross-service):

### Event `admin.login.new-fingerprint`

**Trigger:** UC-AUTH-005 — PLATFORM_ADMIN login from new (ip, user_agent) fingerprint
**Routing key:** `admin.login.new-fingerprint`
**Consumer:** `kitehub-email` service (loads template `admin-new-login-alert`)

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "admin.login.new-fingerprint",
  "eventTime": "2026-05-14T10:23:00Z",
  "userId": "user-uuid",
  "userEmail": "admin@kitehub.me",
  "userName": "Nguyễn Văn Admin",
  "loginAt": "2026-05-14T10:23:00Z",
  "ipFingerprint": "ab12cd34",
  "userAgentDisplay": "Chrome 125 on macOS",
  "geoHint": "VN/HCMC (Viettel)"
}
```

### Event `auth.recovery-code.used`

**Trigger:** UC-AUTH-004 — User logs in via recovery code (security-signal event)
**Routing key:** `auth.recovery-code.used`
**Consumer:** `kitehub-email` (admin notification) + potential SIEM in future

**Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "auth.recovery-code.used",
  "eventTime": "2026-05-14T10:23:00Z",
  "userId": "user-uuid",
  "codesRemaining": 9,
  "loginContext": {
    "ipFingerprint": "...",
    "userAgentDisplay": "..."
  }
}
```

---

## Error code reference (cross-endpoint)

| Code | HTTP | Meaning | Applicable endpoints |
|---|---|---|---|
| `INVALID_CHALLENGE` | 401 | challenge_token invalid signature | enroll-init, enroll-confirm, verify |
| `CHALLENGE_EXPIRED` | 410 | challenge_token > 5 min old | enroll-init, enroll-confirm, verify |
| `ALREADY_ENROLLED` | 409 | totp_enrolled_at already set | enroll-init, enroll-confirm |
| `INVALID_TOTP` | 401 | TOTP code wrong | enroll-confirm, verify, regenerate, disable |
| `INVALID_RECOVERY_CODE` | 401 | recovery code wrong or used (no distinction) | verify |
| `INVALID_PASSWORD` | 401 | password_reconfirm wrong | disable |
| `CANNOT_DISABLE_2FA_FOR_ADMIN` | 403 | PLATFORM_ADMIN disable blocked | disable |
| `TOTP_PRECONDITION_FAILED` | 412 | TOTP valid but not within 5-min freshness window | regenerate, disable |
| `ACCOUNT_LOCKED` | 423 | Lockout active per BR-AUTH-001 | login, verify (TOTP failures count) |
| `INVALID_REQUEST` | 400 | Body malformed or invalid field combination | verify (both totp + recovery), etc. |
| `RATE_LIMITED` | 429 | Gateway rate limit hit | all auth endpoints (per `pre-launch-auth-hardening-checklist.md` §2.1) |

---

## Cross-references

- **Business rules:** `documents/01-business/kitehub/auth/rules.md` BR-AUTH-001..010
- **Use cases:** `documents/01-business/kitehub/auth/use-cases.md` UC-AUTH-001..005
- **Pre-launch checklist:** `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.1-§2.8 (this contract operationalizes those checks)
- **Wave 72a (DB schema):** V35 lockout columns (BR-AUTH-001) + V36 admin_audit_log table (BR-AUTH-009) + totp_* columns (BR-AUTH-005-007)
- **Wave 72b Bucket A (BE controller):** This file defines the controller signature
- **Wave 72b Bucket B (FE):** Consumes MSW handlers `kitehub/kitehub-frontend/src/test/msw/handlers/auth.ts` shipped this PR + real endpoints after Bucket A merge
- **Wave 72b Bucket C (login audit):** Reuses event payloads defined here
- **Wave 72b Bucket D (email templates):** Consumes `admin.login.new-fingerprint` payload defined here

---

## Log

- **2026-05-14 (v1.0.0):** api-contract.md created as part of Wave 72b Bucket 0 Foundation paired with rules.md + use-cases.md + MSW handlers. 5 new 2FA endpoints (`enroll-init`, `enroll-confirm`, `verify`, `recovery-codes/regenerate`, `disable`) + login response shape extension (3 shapes: success-no-2fa, requires-2fa, requires-enrollment) + 423 lockout response + 2 outbox event payloads (`admin.login.new-fingerprint`, `auth.recovery-code.used`). Cross-references BR-AUTH-001..009 in rules.md and UC-AUTH-001..005 in use-cases.md. Error code reference table consolidates per-endpoint error semantics for FE handling. Per `contract-first-for-cross-layer.md` §3.1 Bucket 0 Foundation — Wave 72b Phase 2 (Buckets A/B/C/D) spawned AFTER this PR merges.
