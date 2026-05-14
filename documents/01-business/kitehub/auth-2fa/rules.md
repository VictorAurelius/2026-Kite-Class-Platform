# 2FA (Two-Factor Authentication) — Business Rules

**Domain:** TOTP-based 2FA enrollment + verification + recovery (Wave 72b Bucket A — GAP-516 → Wave 79 Bucket 0 contract — GAP-547)
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)
**Config prefix:** `kitehub.auth.2fa`

File này document business values cho 2FA flow. Mỗi rule có 5 attributes theo `.claude/rules/business-logic-review.md` §2.

> **Wave 79 Bucket 0 context:** 2FA implementation đã ship Wave 72b Bucket A (PR #1301) dưới path `/api/auth/2fa/*`. Wave 79 Bucket 0 tạo contract doc + định nghĩa rules để GAP-547 close (P0 v1.0.0-rc gate: API contract audit caught endpoints undocumented + unversioned). Bucket A của Wave 79 sẽ add `/api/v1/auth/2fa/*` versioned path với backward-compat alias 30 days.

---

## BR-AUTH-2FA-001 — TOTP secret 32-byte random, AES-256-GCM encrypted at rest

- **Value:** TOTP secret generate qua `SecureRandom.generateSeed(32)` (256-bit entropy). Persist trong DB column `users.totp_secret_encrypted` (BYTEA) sau khi encrypt qua AES-256-GCM với master key từ `kitehub.auth.2fa.master-key` (≥32 chars, env-injected).
- **Source:** RFC 6238 (TOTP) recommends ≥160-bit secret; Wave 72b chose 256-bit cho future-proof. AES-256-GCM = NIST SP 800-38D authenticated encryption standard.
- **Rationale:** Plaintext TOTP secret leak = permanent compromise cho user account (attacker generate codes forever cho đến khi user disable + re-enroll). Encryption at rest mitigates DB dump leak; GCM mode provides authentication preventing tampered ciphertext. 256-bit entropy excessive cho HOTP/TOTP (RFC chỉ cần 160) nhưng zero cost với SecureRandom.
- **Reviewer:** @nguyenvankiet (acting Security scout + Compliance, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL 2023 Art 27 (technical measures protecting personal data); Luật An ninh mạng 2018 Art 26.2.b (encryption-at-rest for sensitive data); ISO 27001 A.10.1 Cryptography.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: NIST SP 800-38D update; AES-256-GCM CVE published.
- **Code reference:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TotpSecretCipher.java`

## BR-AUTH-2FA-002 — Recovery codes: 10 single-use, BCrypt hashed

- **Value:** Enrollment generate 10 recovery codes (format `XXXX-XXXX`, 8 hex chars). Mỗi code SHOW MỘT LẦN tại enrollment + regenerate (không recall). Persist BCrypt-hashed (cost ≥10) trong `recovery_codes` table. Mỗi code single-use (mark `used_at` sau verify success).
- **Source:** GitHub / Google / Microsoft pattern (10 codes industry standard); BCrypt = OWASP recommended password hashing per `pre-launch-owasp-rest-hardening-checklist.md` §2.2.
- **Rationale:** Recovery codes là fallback khi user mất authenticator app. 10 codes đủ cho rare event (lost phone) + không quá nhiều để user lưu cẩu thả. Single-use prevents replay nếu user accidentally leak. BCrypt thay vì SHA-256 chỉ để consistency với password hashing pattern + slow brute-force trên DB dump.
- **Reviewer:** @nguyenvankiet (acting Security scout, solo-dev, 2026-05-14).
- **Compliance check:** **Considered** — PDPL Art 27 (technical measures). Recovery codes là authentication factor, không phải PII per se; nhưng leak → impersonation risk → treat as sensitive.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/RecoveryCode.java` + `RecoveryCodeService.java`

## BR-AUTH-2FA-003 — Challenge token JWT, single-purpose, TTL 10 phút

- **Value:** Khi login response yêu cầu 2FA, BE issue challenge JWT với claims `{ sub: userId, purpose: TWO_FACTOR_ENROLL | TWO_FACTOR_LOGIN, exp: now+10min }`. Verify/enroll-confirm endpoints REQUIRE matching purpose claim. Challenge token KHÔNG có quyền truy cập API khác (purpose-scoped).
- **Source:** RFC 7519 JWT + OAuth 2.0 step-up auth pattern.
- **Rationale:** Mở rộng access window quá lâu (>10 min) tăng risk attacker steal challenge token + complete enrollment. Single-purpose claim prevents challenge token reuse cho normal API calls. 10 phút đủ user enter TOTP code (typically <30s) + buffer cho slow typists / clipboard paste.
- **Reviewer:** @nguyenvankiet (acting Security scout, solo-dev, 2026-05-14).
- **Compliance check:** N/A — JWT mechanics, không phải PII.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: user complaint "code expired" pattern; phishing campaign targeting challenge tokens.
- **Code reference:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/ChallengeTokenService.java`

## BR-AUTH-2FA-004 — TOTP code 6 digits, time-skew tolerance ±1 window (30s)

- **Value:** TOTP code 6 digits (HOTP standard). Verification accept code within ±1 30-second window (90s total tolerance). Failed verify counter increment; ≥5 fails trong 15 min trigger account lockout per `pre-launch-auth-hardening-checklist.md` §2.2.
- **Source:** RFC 6238 §5.2 (recommended skew ±1 step); industry standard authenticator apps (Google Authenticator, Authy).
- **Rationale:** Phone clock drift ±30s common; ±1 window đủ accommodate. ±2 windows tăng attack surface (more valid codes/window). Mobile network latency cũng ~5-10s nên ±1 window comfortable. Cap 5 fails prevents brute-force của 6-digit space (1M combos).
- **Reviewer:** @nguyenvankiet (acting Security scout, solo-dev, 2026-05-14).
- **Compliance check:** N/A.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TwoFactorAuthService.java`

## BR-AUTH-2FA-005 — Cannot disable 2FA cho PLATFORM_ADMIN role

- **Value:** Endpoint `POST /api/v1/auth/2fa/disable` reject với HTTP 403 `CANNOT_DISABLE_2FA_FOR_ADMIN` nếu user có role `PLATFORM_ADMIN` (hoặc alias `OWNER` per Wave 78 GAP-518 compat).
- **Source:** `pre-launch-auth-hardening-checklist.md` §2.4 — 2FA mandatory cho PLATFORM_ADMIN role.
- **Rationale:** Admin role có blast radius lớn (approve/reject beta requests, suspend instances, modify config). 2FA mandatory + non-bypassable prevent single-credential compromise. Disable endpoint vẫn tồn tại cho tenant users (role OWNER/STAFF) nhưng PLATFORM_ADMIN khoá cứng.
- **Reviewer:** @nguyenvankiet (acting Security scout + Compliance, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — OWASP A07 Identification and Authentication Failures (per `pre-launch-auth-hardening-checklist.md`).
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** `TwoFactorEnrollmentService.disable()` enforcement; `TwoFactorController.disable()` endpoint.

## BR-AUTH-2FA-006 — Recovery code regenerate requires fresh TOTP proof

- **Value:** Endpoint `POST /api/v1/auth/2fa/recovery-codes/regenerate` requires (a) valid access token + (b) fresh TOTP code (verified within ≤2 min trước call). Old recovery codes invalidated atomically.
- **Source:** Sensitive-action re-authentication pattern (per OWASP ASVS v4.0 §2.10).
- **Rationale:** Recovery codes là last-resort credential. Compromise scenarios: attacker steal session JWT mà không có TOTP authenticator. Fresh TOTP requirement ensure caller có physical control của authenticator at regenerate time. Atomic invalidation prevents race condition (attacker regenerate during user session).
- **Reviewer:** @nguyenvankiet (acting Security scout, solo-dev, 2026-05-14).
- **Compliance check:** N/A.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** `TwoFactorEnrollmentService.regenerate()`.

## BR-AUTH-2FA-007 — Path versioning `/api/v1/` + backward-compat alias 30 days

- **Value:** Canonical 2FA endpoints sống tại `/api/v1/auth/2fa/*` (Wave 79 Bucket A target). Backward-compat alias `/api/auth/2fa/*` (Wave 72b Bucket A original path) hoạt động đồng thời 30 ngày (Wave 79 launch → 2026-06-14). Sau 30 ngày, alias return 410 Gone với hint header `X-Deprecated-Path: /api/v1/auth/2fa/*`.
- **Source:** GAP-547 (Wave 78 API Contract audit P0 — 2FA endpoints undocumented + unversioned); `versioning-policy.md` §7.1 URL-based versioning.
- **Rationale:** Wave 72b ship 2FA quickly cho beta sec hardening, không kịp version. Wave 79 close-out gate v1.0.0-rc cần versioning chính thức. Alias 30 ngày cho phép tenant đã setup TOTP store không bị disrupt (re-enrollment QR code path không thay đổi cho client; chỉ endpoint URL khác). 30 ngày = 2 sprint window đủ để mobile/SDK clients cập nhật.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Security, solo-dev, 2026-05-14).
- **Compliance check:** N/A — URL versioning mechanics.
- **Review cadence:** Once at 2026-06-14 to enforce alias removal. Default Quarterly.
- **Code reference:** Wave 79 Bucket A — `TwoFactorController` `@RequestMapping("/api/v1/auth/2fa")` + alias controller delegating.

## BR-AUTH-2FA-008 — Enrollment requires re-prove password (step-up)

- **Value:** `POST /api/v1/auth/2fa/enroll-init` requires challenge token với purpose `TWO_FACTOR_ENROLL`. Challenge issued bởi `POST /api/auth/login` chỉ khi user cung cấp password chính xác trong session ≤ 5 phút trước.
- **Source:** OWASP ASVS v4.0 §2.10 (re-authentication for sensitive operations).
- **Rationale:** Enrollment 2FA = high-value action (changes auth posture). Stolen session JWT alone không đủ; require fresh password proof prevent silent enrollment hijack scenario (attacker enroll their own TOTP device → user locked out).
- **Reviewer:** @nguyenvankiet (acting Security scout, solo-dev, 2026-05-14).
- **Compliance check:** N/A.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** `AuthController.login()` issuing challenge với purpose `TWO_FACTOR_ENROLL` sau password verify.

## BR-AUTH-2FA-009 — Rate limit 2FA endpoints per `pre-launch-auth-hardening-checklist.md`

- **Value:** Gateway `RequestRateLimiter` filter applied to `/api/v1/auth/2fa/*` + alias `/api/auth/2fa/*`:
  - `enroll-init` / `enroll-confirm`: 5 req/min/user (per access token claim)
  - `verify`: 10 req/min/IP (login flow rate)
  - `recovery-codes/regenerate`: 3 req/min/user
  - `disable`: 5 req/min/user
- **Source:** `pre-launch-auth-hardening-checklist.md` §2.1; Wave 79 Bucket A (GAP-547+551+555) gateway YAML update.
- **Rationale:** Mỗi 2FA endpoint có attack surface khác nhau: verify (login) brute-force-prone → tighter IP limit; regenerate (privileged) tighter user limit; enroll low frequency by design. Per-user keys cho authenticated paths prevent single bad-actor saturating service.
- **Reviewer:** @nguyenvankiet (acting Security scout, solo-dev, 2026-05-14).
- **Compliance check:** N/A — operational defense, not regulated.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** Wave 79 Bucket A — `kitehub-gateway/src/main/resources/application*.yml` filter config.

## BR-AUTH-2FA-010 — Audit log every 2FA action

- **Value:** Every 2FA action (enroll-init, enroll-confirm, verify-success, verify-fail, recovery-code-use, regenerate, disable) writes `admin_audit_log` row với `(timestamp, user_id, action, request_ip, user_agent, status)`. Privileged role actions (PLATFORM_ADMIN) gắn `severity=HIGH`.
- **Source:** `pre-launch-auth-hardening-checklist.md` §2.7 + `pre-launch-owasp-rest-hardening-checklist.md` §2.8 (A09 Security Logging).
- **Rationale:** 2FA actions = high-signal audit events. Failed verify cluster phát hiện brute-force; sudden disable phát hiện account takeover. Privileged-role tagging giúp SIEM filter critical events. Compliance cũng yêu cầu audit trail (PDPL Art 27 + ISO 27001 A.12.4).
- **Reviewer:** @nguyenvankiet (acting Security scout + Compliance, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL 2023 Art 27 + ISO 27001 A.12.4.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** Bucket C GAP-521 (admin_audit_log entity) — extended cho 2FA events. Wave 72b TwoFactorAuthService hiện đã log SLF4J; structured audit-log entity ship Wave 79 Bucket C.

---

## Config

| Key | Default | Purpose | Wired |
|-----|---------|---------|:-----:|
| `kitehub.auth.2fa.master-key` | (env-injected, ≥32 chars) | AES-256-GCM encryption key cho TOTP secret storage | ✅ (Wave 72b — `TotpSecretCipher`) |
| `kitehub.auth.2fa.challenge-ttl-minutes` | `10` | Challenge JWT TTL (enroll + login) | ✅ (Wave 72b — `ChallengeTokenService`) |
| `kitehub.auth.2fa.totp-window-tolerance` | `1` | TOTP skew tolerance (±N 30s windows) | ✅ (Wave 72b — `TwoFactorAuthService`) |
| `kitehub.auth.2fa.recovery-codes-count` | `10` | Recovery codes generated per enroll/regenerate | ✅ (Wave 72b — `RecoveryCodeService`) |
| `kitehub.auth.2fa.totp-fails-before-lockout` | `5` | Failed verify count → trigger lockout (per §2.2 of pre-launch-auth-hardening-checklist) | ⚠️ Wave 79 Bucket C target (GAP-552 default-deny + lockout integration) |
| `kitehub.auth.2fa.backward-compat-alias-expires` | `2026-06-14` | `/api/auth/2fa/*` alias cutoff date | 🆕 Wave 79 Bucket A target |

Config keys nằm `application.yml` của `kitehub-subscription` module.
