# Authentication — Use Cases

**Domain:** Authentication (login + 2FA + lockout + recovery + login audit)
**Last verified:** 2026-05-14 (Wave 72b Bucket 0 Foundation)

Use cases describing end-to-end actor flows for auth-domain endpoints. Each UC maps to BR-AUTH-NNN business rules (`rules.md`) and api-contract.md endpoints.

---

## UC-AUTH-001 — Login (password + optional 2FA challenge)

**Actor:** Any authenticated role (PLATFORM_ADMIN, OWNER, TEACHER, PARENT, STUDENT)
**Trigger:** User submits credentials via `POST /api/auth/login`
**Pre-conditions:** User account exists, not soft-deleted

### Happy path

1. User submits `{email, password}` via login form (browser) or curl
2. BE validates email format + finds user by email
3. BE checks `account_locked` flag (BR-AUTH-001) — if locked AND `lockout_until > now()` → return 423 (UC end)
4. BE verifies password via bcrypt comparison
5. If password OK:
   - Reset `failed_login_attempts=0`
   - Check `totp_required` flag (BR-AUTH-005) AND `totp_enrolled_at IS NOT NULL`:
     - **If 2FA enrolled:** Issue opaque `challenge_token` (5-min TTL) → return `{requires2fa: true, challenge_token}` → Client redirects to 2FA prompt (UC-AUTH-003 picks up)
     - **If 2FA NOT enrolled AND role=PLATFORM_ADMIN AND `totp_required=true`:** Force enrollment (UC-AUTH-002 picks up) → return `{requires2fa_enrollment: true, challenge_token}`
     - **Otherwise:** Issue access_token (15min) + refresh_token (7d) → return `{access_token, refresh_token, user}`
6. Async: write `login_audit_log` row + fingerprint check (BR-AUTH-008) — if new fingerprint AND role=PLATFORM_ADMIN → emit `admin.login.new-fingerprint` event (UC-AUTH-005 fulfills)

### Failure paths

| Failure | Response | Action |
|---|---|---|
| Wrong password | HTTP 401 `{error: "INVALID_CREDENTIALS"}` | Increment `failed_login_attempts`. If reaches threshold (BR-AUTH-001) → lock account + set `lockout_until` per backoff schedule |
| Account locked (in lockout window) | HTTP 423 `{error: "ACCOUNT_LOCKED", lockedUntil: ISO8601, attemptsRemaining: 0}` + `Retry-After: <seconds>` header (BR-AUTH-002) | Log attempt; do NOT increment counter (already locked) |
| User not found | HTTP 401 `{error: "INVALID_CREDENTIALS"}` (do NOT distinguish from wrong-password — username enumeration defence) | Log attempt with IP for rate-limit gateway |
| Invalid email format | HTTP 400 `{error: "INVALID_EMAIL_FORMAT"}` | Surface field-validation error |
| Rate-limit exceeded (gateway) | HTTP 429 `{error: "RATE_LIMITED"}` | Gateway-level — login endpoint limited 5/sec replenish + burst 10 (per `pre-launch-auth-hardening-checklist.md` §2.1) |

### FE behavior pointers

- On `requires2fa: true` → redirect to `/login/2fa-challenge?token=<challenge_token>`
- On `requires2fa_enrollment: true` → redirect to `/login/2fa-enroll?token=<challenge_token>`
- On 423 → show "Tài khoản tạm khóa, thử lại sau {N} phút" + countdown timer using `Retry-After` header
- On 401 generic → show "Email hoặc mật khẩu không đúng" (DO NOT leak which one)

### Post-conditions

- Successful flow: JWT tokens issued, audit row written
- Failed flow: Counter incremented OR account locked + audit row written
- Async event: login_audit_log entry, optional admin.login.new-fingerprint event

### Cross-references

- BR-AUTH-001 (lockout), BR-AUTH-002 (423 response), BR-AUTH-003 (JWT TTL), BR-AUTH-005 (2FA enforcement), BR-AUTH-008 (alert)
- api-contract.md: `POST /api/auth/login` (existing endpoint — Wave 72a EXTENDS response shape with 2FA challenge)

---

## UC-AUTH-002 — First-time 2FA enrollment (PLATFORM_ADMIN mandatory)

**Actor:** PLATFORM_ADMIN role
**Trigger:** First login after `totp_required=true` set on user AND `totp_enrolled_at IS NULL`
**Pre-conditions:** User has valid password, just completed UC-AUTH-001 step 5 → received `requires2fa_enrollment: true`

### Happy path

1. FE shows enrollment wizard at `/login/2fa-enroll?token=<challenge_token>`
2. User clicks "Begin enrollment" → FE calls `POST /api/auth/2fa/enroll-init` with bearer = challenge_token
3. BE:
   - Generates 160-bit TOTP secret (RFC 6238 per BR-AUTH-006)
   - Generates 10 recovery codes (8-char, per BR-AUTH-007 alphabet)
   - Returns `{secret: base32, qr_uri: "otpauth://totp/...", recovery_codes: [10 strings]}` ← recovery codes shown ONCE
   - Does NOT persist `totp_enrolled_at` yet — waits for confirm
4. FE renders:
   - QR code from `qr_uri` (using `qrcode.react` or similar)
   - 10 recovery codes displayed in copy-friendly format (paper/print/vault prompts)
   - User MUST tick "I have saved my recovery codes" checkbox
   - User scans QR with authenticator app (Google Authenticator, Authy, 1Password, etc.)
5. User enters first 6-digit TOTP code → FE calls `POST /api/auth/2fa/enroll-confirm` with `{first_totp_code}` + bearer = challenge_token
6. BE verifies TOTP code against stored secret (±1 step skew per BR-AUTH-006):
   - **If match:** Persist `users.totp_secret`, `users.totp_enrolled_at=now()`, bcrypt-hash all 10 recovery codes into `recovery_codes` table
   - Issue access + refresh tokens (per UC-AUTH-001 happy path step 5 final)
   - Return `{enrolled: true, totp_enrolled_at, access_token, refresh_token, user}`
7. FE redirects to `/admin` (admin dashboard)

### Failure paths

| Failure | Response | Action |
|---|---|---|
| First TOTP code wrong | HTTP 401 `{error: "INVALID_TOTP"}` | FE shows error; allow retry (no lockout on enroll-confirm; user fixing clock) |
| Already enrolled (race condition) | HTTP 409 `{error: "ALREADY_ENROLLED"}` | FE redirect to login screen |
| Challenge_token expired (>5min) | HTTP 410 `{error: "CHALLENGE_EXPIRED"}` | FE redirect to login screen for re-auth |
| Recovery-codes checkbox not ticked | FE-only — disable submit button until checked | No backend call |

### FE behavior pointers

- QR display must work on mobile screens (responsive sizing)
- Recovery codes must be selectable for copy (consider "Download as .txt" button)
- "I have saved" checkbox required before "Confirm" button enables
- Show clear copy: "These codes are shown ONCE. We CANNOT recover them. Save now."
- Post-confirm redirect: `/admin` dashboard with welcome banner "2FA enrolled successfully"

### Post-conditions

- `users.totp_enrolled_at` set
- `recovery_codes` rows created (10 × bcrypt-hash)
- Access + refresh tokens issued
- Audit log: enrollment event written

### Cross-references

- BR-AUTH-005, BR-AUTH-006, BR-AUTH-007
- api-contract.md: `POST /api/auth/2fa/enroll-init`, `POST /api/auth/2fa/enroll-confirm`

---

## UC-AUTH-003 — 2FA verify during login (subsequent logins)

**Actor:** Any role with `totp_enrolled_at IS NOT NULL`
**Trigger:** Post-password login when `requires2fa: true` (UC-AUTH-001 step 5 → challenge_token issued)
**Pre-conditions:** User has valid `challenge_token` from `POST /api/auth/login` (5-min TTL)

### Happy path

1. FE at `/login/2fa-challenge?token=<challenge_token>` shows 6-digit input field
2. User opens authenticator app, reads current code
3. User enters 6-digit code → FE calls `POST /api/auth/2fa/verify` with `{challenge_token, totp_code}`
4. BE:
   - Validates `challenge_token` not expired (5min TTL)
   - Verifies TOTP code against `users.totp_secret` (±1 step skew per BR-AUTH-006)
   - **If match:**
     - Issue access (15min) + refresh (7d) tokens
     - Write login_audit_log (BR-AUTH-008) + fingerprint check
     - Return `{access_token, refresh_token, user}`
5. FE stores tokens, redirects to role-appropriate dashboard (admin → `/admin`, owner → `/dashboard`, etc.)

### Alternative path — recovery code instead of TOTP

1. User lost authenticator (phone broken, app uninstalled) → clicks "Use recovery code" link
2. FE shows recovery-code input field (different shape from 6-digit)
3. User enters 8-char recovery code → FE calls `POST /api/auth/2fa/verify` with `{challenge_token, recovery_code}` (this branch = UC-AUTH-004)

### Failure paths

| Failure | Response | Action |
|---|---|---|
| Wrong TOTP code | HTTP 401 `{error: "INVALID_TOTP"}` | Increment lockout counter (this counts as failed login per BR-AUTH-001); FE shows error |
| Challenge_token expired | HTTP 410 `{error: "CHALLENGE_EXPIRED"}` | FE redirect to `/login` for fresh auth |
| Challenge_token invalid (signature/format) | HTTP 401 `{error: "INVALID_CHALLENGE"}` | FE redirect to `/login` |

### FE behavior pointers

- 6-digit input: auto-focus + auto-submit when 6 digits entered (UX)
- "Use recovery code" link toggles to alt input
- Timer showing token expiry (5min countdown) — visual urgency
- Post-success: redirect to role-appropriate dashboard URL

### Post-conditions

- Access + refresh tokens issued
- login_audit_log row + optional fingerprint alert (UC-AUTH-005)
- If recovery_code used → `recovery_codes.used_at=now()` (UC-AUTH-004 details)

### Cross-references

- BR-AUTH-001 (lockout counter), BR-AUTH-003 (JWT), BR-AUTH-006 (TOTP standard), BR-AUTH-008 (login alert)
- api-contract.md: `POST /api/auth/2fa/verify`

---

## UC-AUTH-004 — Recovery code use (TOTP unavailable)

**Actor:** User with enrolled 2FA who has lost authenticator app
**Trigger:** User clicks "Use recovery code" on UC-AUTH-003 challenge screen
**Pre-conditions:** User has access to their saved recovery codes (from UC-AUTH-002 enrollment)

### Happy path

1. FE shows recovery-code input field (8-char alphanumeric)
2. User enters one of their 10 codes → FE calls `POST /api/auth/2fa/verify` with `{challenge_token, recovery_code}`
3. BE:
   - Validates challenge_token (5min TTL)
   - Iterates `recovery_codes` for user_id, bcrypt-compares each unused code against submitted code
   - **If match found:**
     - Mark that recovery code `used_at=now()` (single-use enforcement per BR-AUTH-007)
     - Issue access + refresh tokens
     - Add `regenerate_recommended: true` flag in response (UX hint)
     - Write login_audit_log + special event `auth.recovery-code.used`
     - Return `{access_token, refresh_token, user, regenerate_recommended: true, codes_remaining: N}`
4. FE:
   - Store tokens
   - Show warning banner: "Bạn vừa dùng recovery code. Còn lại N codes. Khuyến nghị regenerate sau khi thiết lập lại authenticator."
   - Redirect to role-appropriate dashboard

### Failure paths

| Failure | Response | Action |
|---|---|---|
| Recovery code wrong (no match) | HTTP 401 `{error: "INVALID_RECOVERY_CODE"}` | Increment lockout counter; FE shows error |
| Recovery code already used | HTTP 401 `{error: "INVALID_RECOVERY_CODE"}` (do NOT distinguish from wrong — prevents enumeration) | Increment lockout counter |
| All 10 codes used | HTTP 401 `{error: "INVALID_RECOVERY_CODE"}` | Same response — user must contact support |
| Challenge_token expired | HTTP 410 `{error: "CHALLENGE_EXPIRED"}` | FE redirect to `/login` |

### Post-conditions

- Consumed code marked `used_at` (immutable after this)
- Audit event `auth.recovery-code.used` written — security signal for admin review
- User has `codes_remaining-1` codes left
- User logged in, FE shows regenerate-recommended warning

### FE behavior pointers

- After successful recovery-code use, NEXT login shows persistent banner "X codes remaining" until user regenerates or returns above safe threshold
- Profile settings → "Security" tab shows "Recovery codes: N/10 remaining" + "Regenerate" button

### Cross-references

- BR-AUTH-007 (single-use semantics)
- api-contract.md: `POST /api/auth/2fa/verify` (recovery_code variant)

---

## UC-AUTH-005 — Login alert delivery (new fingerprint)

**Actor:** System (async event consumer)
**Trigger:** Successful PLATFORM_ADMIN login where `(user_id, ip_hash, user_agent_hash)` fingerprint NOT in last 24 hours
**Pre-conditions:** UC-AUTH-001 or UC-AUTH-003 completed successfully + role=PLATFORM_ADMIN + new fingerprint

### Happy path

1. Post-login (in async after-commit phase of login transaction):
   - LoginAuditService computes `fingerprint_hash = SHA256(ip || user_agent)`
   - Checks `login_audit_log` for any row with same `(user_id, fingerprint_hash)` in last 24 hours
   - **If none found (= new fingerprint):**
     - Write login_audit_log row with `alert_sent=true`
     - Emit outbox event `admin.login.new-fingerprint`:
       ```json
       {
         "userId": "...",
         "userEmail": "...",
         "loginAt": "ISO8601",
         "ipFingerprint": "<truncated for display>",
         "userAgentDisplay": "<parsed Browser/OS for display>",
         "geoHint": "<optional ISP/region from IP — no exact lat/long>"
       }
       ```
   - **If found:** Write login_audit_log with `alert_sent=false`
2. Outbox poller → RabbitMQ → `kitehub-email` consumer
3. kitehub-email service:
   - Loads email template `admin-new-login-alert` (Resend HTML template)
   - Substitutes fields: user name, IP fingerprint display, browser/OS, login time
   - Sends via Resend API (per ADR-025)
4. User receives email within ≤5 min target (Resend SLA + outbox poll interval)

### Failure paths

| Failure | Handling |
|---|---|
| Outbox event publish fails | Retry per outbox infrastructure; login itself NOT blocked |
| Email send fails (Resend down) | Outbox consumer marks event as failed → retry per consumer retry policy (3 attempts then DLQ) |
| User's email bounces | Log to admin_audit_log; security team manual check next session |

### Email content (template `admin-new-login-alert`)

```
Subject: [KiteHub Security] New sign-in to your admin account

Xin chào {userName},

Chúng tôi phát hiện đăng nhập admin từ thiết bị mới:
- Thời gian: {loginAt}
- Trình duyệt: {userAgentDisplay}
- Vị trí ước lượng: {geoHint}
- Mã định danh thiết bị: {ipFingerprint}

Nếu đây là bạn — bỏ qua email này.
Nếu KHÔNG phải bạn — đổi mật khẩu ngay tại: https://kitehub.me/admin/security

Liên hệ: security@kitehub.me
```

### Post-conditions

- login_audit_log row with `alert_sent=true` or `false`
- If alert sent: outbox event published + email delivered to admin

### FE behavior pointers

- N/A — async backend flow; admin sees email out-of-band
- Optional: admin dashboard shows "Recent sign-ins" list with fingerprints (Phase 1.5+ scope)

### Cross-references

- BR-AUTH-008 (alert scope + cooldown), BR-AUTH-009 (audit log immutability)
- api-contract.md: NO direct endpoint — pure async event flow
- Email template: managed by `kitehub-email` service per Wave 4

---

## Log

- **2026-05-14 (v1.0.0):** use-cases.md created as part of Wave 72b Bucket 0 Foundation paired with rules.md + api-contract.md + MSW handlers. 5 UCs (UC-AUTH-001..005) cover login + 2FA enrollment + 2FA verify + recovery code use + login alert async flow. Cross-references BR-AUTH-001..009 in rules.md and api-contract.md endpoints. Each UC has actor + preconditions + happy path + failure paths + FE behavior pointers + postconditions per CLAUDE.md §"Business Logic Documents 3-Layer" pattern.
