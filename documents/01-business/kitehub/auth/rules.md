# Authentication — Business Rules

**Domain:** Authentication (login + lockout + 2FA + login audit + session management)
**Source-of-truth code:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/**`
**Last verified:** 2026-05-14 (Wave 72b Bucket 0 Foundation)

This rules.md is the contract for Wave 72b authentication hardening per `.claude/rules/pre-launch-auth-hardening-checklist.md` (OWASP A07 mandatory gate). It governs the 8 checks the security-audit Cat 4 rubric now enforces per-endpoint.

Cross-references:
- `documents/01-business/kitehub/auth/use-cases.md` — actor flows
- `documents/01-business/kitehub/auth/api-contract.md` — endpoint shapes
- Wave 72a (DB schema): V35 lockout columns + V36 admin_audit_log table
- Wave 72c (FE consumer): 2FA wizard + recovery codes screens

---

## BR-AUTH-001 — Failed login attempts: exponential backoff lockout

- **Value:** 5 failed attempts within 15 min → lock 15 min. 3rd consecutive lockout → 1 hr. 4th+ → 24 hr.
- **Source:** OWASP ASVS V2 — Authentication; NIST SP 800-63B §5.2.2 (rate limiting); industry benchmark (Microsoft, Google, AWS)
- **Rationale:** 5/15min balances usability (humans typo) vs brute-force defence. Exponential backoff makes credential-stuffing economically infeasible without locking legit users permanently.
- **Reviewer:** @nguyenvankiet (acting Security Lead + Product Owner, solo-dev, 2026-05-14). Threat-model review queued — GAP-515 follow-up.
- **Compliance check:** **Considered** — PDPL 2023 không mandate cụ thể nhưng OWASP A07 + Luật An ninh mạng 2018 yêu cầu reasonable access controls.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: incident report > 100 lockouts/day, user-complaint pattern.
- **Config keys:**
  - `kitehub.auth.lockout.max-attempts=5`
  - `kitehub.auth.lockout.window-minutes=15`
  - `kitehub.auth.lockout.base-duration-minutes=15`
  - `kitehub.auth.lockout.escalation-multipliers=1,1,4,96` (15min, 15min, 1hr, 24hr)
- **DB schema:** `users.failed_login_attempts INT`, `users.account_locked BOOLEAN`, `users.lockout_until TIMESTAMP`, `users.lockout_count INT` (Wave 72a V35)
- **Cross-ref:** GAP-515 (Wave 72a backend); BR-AUTH-002 (response shape)

---

## BR-AUTH-002 — Account lockout HTTP response

- **Value:** HTTP 423 LOCKED + `Retry-After: <seconds>` header + body `{error: "ACCOUNT_LOCKED", lockedUntil: ISO8601, attemptsRemaining: 0}`
- **Source:** RFC 4918 §11.3 (HTTP 423 LOCKED); RFC 9110 §10.2.3 (Retry-After)
- **Rationale:** 423 distinct from 401 (wrong credentials) and 403 (insufficient role) — clients can show "try again in X minutes" specifically. Retry-After header lets clients schedule retry without polling.
- **Reviewer:** @nguyenvankiet (acting Security Lead, solo-dev, 2026-05-14).
- **Compliance check:** N/A — HTTP semantics, not regulated.
- **Review cadence:** Annual. **Next review:** 2027-05-14.
- **Cross-ref:** GAP-515 Wave 72a; api-contract.md §POST /api/auth/login error 423

---

## BR-AUTH-003 — JWT TTL and refresh token rotation

- **Value:**
  - Access token TTL ≤ **15 minutes**
  - Refresh token TTL ≤ **7 days**
  - Refresh token MUST rotate on each `/refresh` call (old token marked `blacklisted_at=now()`)
  - Reuse of blacklisted refresh token → **force-logout ALL user sessions** + email alert (security-breach signal)
- **Source:** OWASP ASVS V3 — Session Management; OAuth 2.0 RFC 6749 §6 (refresh rotation); industry standard (Auth0, Okta)
- **Rationale:** Short access TTL bounds stolen-token blast radius. Refresh rotation detects token theft (legit user + attacker can't both have valid refresh). Force-logout-all on reuse = "fail open hot, fail closed cold" — attacker locked out, user re-logs.
- **Reviewer:** @nguyenvankiet (acting Security Lead, solo-dev, 2026-05-14).
- **Compliance check:** **Considered** — PDPL access controls, Luật An ninh mạng 2018 §22 (data access logs).
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: token-theft incident, refresh reuse alerts > 1/week.
- **Config keys:**
  - `kitehub.auth.jwt.access-ttl-minutes=15`
  - `kitehub.auth.jwt.refresh-ttl-days=7`
  - `kitehub.auth.jwt.refresh-rotation-enabled=true`
- **DB schema:** `refresh_tokens.blacklisted_at TIMESTAMP NULL` (Wave 72a V35)
- **Cross-ref:** BR-AUTH-004 (signing); UC-AUTH-001 (login flow)

---

## BR-AUTH-004 — JWT dual-key signing + quarterly rotation

- **Value:**
  - Production uses **HS256** (Phase 1 BETA acceptable) OR **RS256** (recommended Phase 1.5+)
  - Two simultaneous signing slots: `jwt.secret.current` + `jwt.secret.previous`
  - Verifier tries `current` first, then `previous` (grace period for in-flight tokens during rotation)
  - Rotation cadence: **quarterly** (every 90 days)
  - Rotation procedure documented in `documents/05-guides/operations/secrets-rotation-runbook.md` §JWT Signing Key
- **Source:** OWASP ASVS V3.5; NIST SP 800-57 Part 1 §5.3.5 (cryptoperiod); industry standard (AWS Secrets Manager rotation pattern)
- **Rationale:** Dual-key enables zero-downtime rotation — refresh tokens issued with old key still verify during transition window. Quarterly cadence balances key-compromise window vs operational overhead.
- **Reviewer:** @nguyenvankiet (acting Security Lead, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL 2023 Art 27 (technical security measures), Luật An ninh mạng 2018 §22.
- **Review cadence:** Quarterly (key rotation IS the review). **Next review:** 2026-08-14 (next rotation due).
- **Config keys:**
  - `kitehub.auth.jwt.algorithm=HS256` (Phase 1 BETA) | `RS256` (Phase 1.5+)
  - `kitehub.auth.jwt.secret.current=${JWT_SECRET_CURRENT}` (AWS Secrets Manager)
  - `kitehub.auth.jwt.secret.previous=${JWT_SECRET_PREVIOUS}` (AWS Secrets Manager, optional during initial deploy)
- **AWS Secrets Manager:** `kitehub/production/jwt-signing-key` versioned (`AWSCURRENT` + `AWSPREVIOUS`)
- **Cross-ref:** GAP-520 Wave 72a (rotation runbook); `pre-launch-secrets-hardening-checklist.md` §2.3+§2.5

---

## BR-AUTH-005 — 2FA TOTP enforcement scope

- **Value:**
  - **PLATFORM_ADMIN role:** 2FA TOTP enrollment **MANDATORY** before any privileged action. Login flow forces enrollment if `totp_enrolled_at IS NULL`.
  - **OWNER+ roles (tenant admins):** Optional opt-in via profile settings. Phase 1.5+ may flip to mandatory.
  - **TEACHER / PARENT / STUDENT roles:** Not enforced in Phase 1 BETA. Optional opt-in available.
  - PLATFORM_ADMIN cannot disable 2FA after enrollment (per BR-AUTH-007 §disable check).
- **Source:** OWASP ASVS V2.8; NIST SP 800-63B §5.1.5 (multi-factor); industry standard (admin 2FA mandatory at AWS, GCP, GitHub)
- **Rationale:** PLATFORM_ADMIN has cross-tenant blast radius — credential compromise = global breach. Tenant-role 2FA adds friction; defer to Phase 1.5 when revenue justifies UX cost.
- **Reviewer:** @nguyenvankiet (acting Security Lead + Product Owner, solo-dev, 2026-05-14). Tenant 2FA UX research queued Phase 1.5.
- **Compliance check:** **Considered** — PDPL access controls; admin 2FA exceeds baseline.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: any PLATFORM_ADMIN account compromise (instant escalation), Phase 1.5 launch.
- **Config keys:**
  - `kitehub.auth.2fa.required-for-roles=PLATFORM_ADMIN` (CSV list)
  - `kitehub.auth.2fa.optional-for-roles=OWNER,TEACHER,PARENT,STUDENT`
- **DB schema:** `users.totp_required BOOLEAN DEFAULT FALSE`, `users.totp_enrolled_at TIMESTAMP NULL`, `users.totp_secret VARCHAR(64) NULL` (Wave 72a V36)
- **Cross-ref:** GAP-516 Wave 72a; UC-AUTH-002 (enrollment); BR-AUTH-006 (TOTP standard)

---

## BR-AUTH-006 — 2FA TOTP cryptographic standard

- **Value:**
  - Algorithm: **RFC 6238 TOTP** (Time-based One-Time Password)
  - Code length: **6 digits**
  - Time window: **30 seconds**
  - Skew tolerance: **±1 step** (accepts code from previous or next 30s window — net 90s acceptance)
  - Hash function: **SHA-1** (RFC 6238 default; SHA-256 acceptable v2)
  - Secret length: **160 bits** (20 bytes, base32-encoded → 32 chars displayed)
- **Source:** RFC 6238 (TOTP); RFC 4226 (HOTP base); compatible with Google Authenticator, Authy, 1Password, Microsoft Authenticator
- **Rationale:** RFC 6238 = industry standard; SHA-1 RFC default ensures all authenticator apps compatible. ±1 step skew handles clock-drift without widening attack window meaningfully. SHA-256 cryptographically stronger but reduces app compat.
- **Reviewer:** @nguyenvankiet (acting Security Lead, solo-dev, 2026-05-14).
- **Compliance check:** N/A — cryptographic standard, not regulated specifically.
- **Review cadence:** Annual. **Next review:** 2027-05-14. Event triggers: RFC supersession, app compat issues.
- **Config keys:**
  - `kitehub.auth.2fa.totp.digits=6`
  - `kitehub.auth.2fa.totp.period-seconds=30`
  - `kitehub.auth.2fa.totp.skew-steps=1`
  - `kitehub.auth.2fa.totp.algorithm=SHA1`
- **Library:** `dev.samstevens.totp:totp:1.7.1` (Java) or equivalent RFC 6238-compliant lib
- **Cross-ref:** GAP-516; UC-AUTH-002, UC-AUTH-003

---

## BR-AUTH-007 — 2FA recovery codes: 10 single-use codes, shown ONCE

- **Value:**
  - **Count:** 10 recovery codes generated at enrollment
  - **Format:** 8-char alphanumeric (lowercase + digits, ambiguous chars excluded: no `0`, `o`, `1`, `l`)
  - **Display:** Shown to user **ONCE** at enrollment + on regenerate. NOT recoverable after dismissal.
  - **Storage:** bcrypt-hashed (cost ≥10) in DB, NEVER plaintext
  - **Use:** Each code single-use; consumed code marked `used_at=now()` immediately
  - **Regenerate:** User can regenerate at any time (requires recent TOTP within 5min); regenerate **INVALIDATES all 10 previous codes** atomically
  - **Disable 2FA:** Requires (a) recent TOTP within 5min AND (b) password reconfirm AND (c) role ≠ PLATFORM_ADMIN (BR-AUTH-005 inviolable)
- **Source:** OWASP ASVS V2.7; industry standard (GitHub, AWS, Google all use 8-10 single-use recovery codes)
- **Rationale:** 10 codes balances paper-storage usability vs codebook-theft risk. bcrypt prevents codebook leak via DB dump. "Show ONCE" forces user to commit to safe storage (vault, printout). PLATFORM_ADMIN disable-block prevents self-foot-gun.
- **Reviewer:** @nguyenvankiet (acting Security Lead + Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** **Considered** — PDPL data minimization (don't keep plaintext); reasonable security per Luật An ninh mạng.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: recovery-code phishing pattern, user-complaint UX.
- **Config keys:**
  - `kitehub.auth.2fa.recovery-codes.count=10`
  - `kitehub.auth.2fa.recovery-codes.length=8`
  - `kitehub.auth.2fa.recovery-codes.alphabet=abcdefghijkmnpqrstuvwxyz23456789` (no 0,o,1,l)
  - `kitehub.auth.2fa.recovery-codes.bcrypt-cost=10`
  - `kitehub.auth.2fa.recovery-codes.recent-totp-window-minutes=5`
- **DB schema:** `recovery_codes (id, user_id, code_hash, used_at NULL, created_at)` (Wave 72a V36)
- **Cross-ref:** GAP-516; UC-AUTH-004 (recovery use); api-contract.md regenerate + disable endpoints

---

## BR-AUTH-008 — Login alert on new fingerprint (PLATFORM_ADMIN)

- **Value:**
  - **Scope:** PLATFORM_ADMIN role only in Phase 1 BETA (cross-tenant blast radius)
  - **Trigger:** Successful login where `(user_id, ip_hash, user_agent_hash)` fingerprint not seen in last 24 hours
  - **Action:** Emit `admin.login.new-fingerprint` event via outbox → Resend email template `admin-new-login-alert` → user receives within ≤5 min target
  - **Cooldown:** 24 hours per (user, fingerprint) — don't spam on repeated logins from same office IP
  - **Fingerprint hash:** SHA-256 of (ip, user_agent) — store hash not raw (PDPL minimization)
  - **OWNER+ roles:** Optional opt-in via profile (Phase 1.5+ scope)
- **Source:** OWASP ASVS V2.5; industry standard (GitHub, Stripe, AWS "new device sign-in" alerts)
- **Rationale:** PLATFORM_ADMIN credential compromise needs detection within minutes. New-fingerprint alert is signal humans recognize. 24h cooldown prevents alert-fatigue from same office network. Hash-not-raw fingerprint = PDPL data-minimization compliant.
- **Reviewer:** @nguyenvankiet (acting Security Lead + Product Owner, solo-dev, 2026-05-14).
- **Compliance check:** **Compliant** — PDPL 2023 Art 27 (security measures), Art 17 (data minimization via hashing).
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: missed-detection incident, email-delivery latency > 5 min.
- **Config keys:**
  - `kitehub.auth.login-alert.enabled-roles=PLATFORM_ADMIN`
  - `kitehub.auth.login-alert.cooldown-hours=24`
  - `kitehub.auth.login-alert.fingerprint-hash=SHA-256`
- **DB schema:** `login_audit_log (id, user_id, ip_hash, user_agent_hash, login_at, alert_sent BOOLEAN)` (Wave 72a V36)
- **Outbox event:** `admin.login.new-fingerprint` consumed by `kitehub-email` service
- **Cross-ref:** GAP-517 Wave 72a; UC-AUTH-005

---

## BR-AUTH-009 — Admin audit log: 7-year PDPL retention

- **Value:**
  - **Scope:** Every PLATFORM_ADMIN action that modifies tenant state, approves/rejects beta requests, suspends instances, modifies config, edits other users
  - **Row schema:** `(id, timestamp, admin_user_id, action_type, target_entity_type, target_entity_id, request_ip, user_agent, request_payload_summary, result)`
  - **Retention:** **7 years** post-action (per PDPL 2023 + financial records best practice)
  - **Immutable:** No UPDATE / DELETE allowed on rows after insert (DB constraint: trigger or RLS)
  - **Access:** Read-only via admin dashboard; export to CSV/JSON for compliance audit
  - **Cross-ref:** Sister of BR-AUTH-008 — login_audit_log focuses on access; admin_audit_log focuses on actions
- **Source:** PDPL 2023 Art 41 (data processing records); Luật Quản lý Thuế 2019 (financial record retention); OWASP ASVS V9 (logging)
- **Rationale:** Regulatory + forensic requirement. 7-year window covers PDPL audit + statute of limitations on contractual disputes. Immutable rows prevent insider-cover-up.
- **Reviewer:** @nguyenvankiet (acting Security Lead + Compliance, solo-dev, 2026-05-14). Legal counsel review queued — GAP-521 follow-up.
- **Compliance check:** **Compliant** — PDPL 2023 Art 41 (≥3 years; we ship 7 for safety), Luật Quản lý Thuế 2019 (financial actions 5+ years).
- **Review cadence:** Annual + event-driven. **Next review:** 2027-05-14 OR within 30 days of any PDPL implementing-decree publication.
- **Config keys:**
  - `kitehub.auth.admin-audit.retention-years=7`
  - `kitehub.auth.admin-audit.immutable=true`
- **DB schema:** `admin_audit_log` table (Wave 72a V36) + trigger preventing UPDATE/DELETE
- **Cross-ref:** GAP-521 Wave 72a; `pre-launch-auth-hardening-checklist.md` §2.7

---

## BR-AUTH-010 — Password complexity policy

- **Value:**
  - **Tenant users (TEACHER / PARENT / STUDENT / OWNER):**
    - Min 12 chars (recommend 14)
    - Mix: ≥1 upper + ≥1 lower + ≥1 digit + ≥1 symbol
    - OR passphrase ≥20 chars (exempt mix requirement)
    - Reject top-10000 leaked passwords (haveibeenpwned-like list OR zxcvbn library score ≥3)
    - No reuse of last 3 passwords (DB history table)
  - **PLATFORM_ADMIN:**
    - Min 16 chars OR auto-generated 32-char via AWS Secrets Manager rotation
    - Same complexity rules + 2FA mandatory (BR-AUTH-005)
  - **Hashing:** bcrypt cost ≥10 (Spring Security default acceptable); never plaintext, never SHA-1/MD5
- **Source:** OWASP ASVS V2.1; NIST SP 800-63B §5.1.1 (length over complexity); zxcvbn scoring (Dropbox 2014 research)
- **Rationale:** NIST modern guidance favors length over symbol-mandate. Top-10000 reject catches "Password123!" et al. that satisfy complexity but are dictionary-easy. bcrypt cost 10 = ~250ms hash time (good defence vs offline brute-force).
- **Reviewer:** @nguyenvankiet (acting Security Lead, solo-dev, 2026-05-14).
- **Compliance check:** **Considered** — PDPL technical security measures; exceeds VN regulatory baseline.
- **Review cadence:** Annual + event-driven. **Next review:** 2027-05-14. Event triggers: NIST 800-63 revision, mass credential leak affecting baseline.
- **Config keys:**
  - `kitehub.auth.password.min-length=12` (tenant) / `16` (admin via env override)
  - `kitehub.auth.password.require-mix=true`
  - `kitehub.auth.password.passphrase-min-length=20`
  - `kitehub.auth.password.zxcvbn-min-score=3`
  - `kitehub.auth.password.history-depth=3`
  - `kitehub.auth.password.bcrypt-cost=10`
- **Library:** `nbvcxz` (zxcvbn-java) for scoring + leaked-password check
- **DB schema:** `password_history (id, user_id, password_hash, created_at)` — depth-3 retention
- **Cross-ref:** Future scope post Wave 72b — file follow-up gap when implementation begins; `pre-launch-auth-hardening-checklist.md` §2.3

---

## Open Items / Follow-ups

Per `gap-done-discipline.md` §3 PARTIAL exit ramp — these are framing rules; implementation tracked per-gap:

- [ ] BR-AUTH-001/002 implementation — GAP-515 Wave 72a (BE V35 + AuthService logic)
- [ ] BR-AUTH-003/004 implementation — GAP-520 Wave 72a (rotation runbook + dual-key verifier)
- [ ] BR-AUTH-005/006/007 implementation — GAP-516 Wave 72a (TwoFactorAuthService + V36 + UC-AUTH-002/003/004)
- [ ] BR-AUTH-008 implementation — GAP-517 Wave 72a (LoginAuditService + outbox + email template)
- [ ] BR-AUTH-009 implementation — GAP-521 Wave 72a (admin_audit_log entity + interceptor)
- [ ] BR-AUTH-010 implementation — Future scope, file follow-up gap when picking up

---

## Log

- **2026-05-14 (v1.0.0):** rules.md created as part of Wave 72b Bucket 0 Foundation paired with use-cases.md + api-contract.md + MSW handlers. 10 business rules (BR-AUTH-001..010) frame the OWASP A07 auth-hardening checklist (`pre-launch-auth-hardening-checklist.md` §2.1-§2.8 + BR-AUTH-010 password complexity for §2.3). Each rule has 5-attribute structure per `.claude/rules/business-logic-review.md` (Source / Rationale / Reviewer / Compliance / Review cadence). Reviewer: @nguyenvankiet (solo-dev, acting Security Lead + Product Owner — flagged for legal counsel review queued GAP-521 follow-up where compliance-touching). Cross-references: Wave 72a (BE + DB schema) + Wave 72c (FE consumer screens) + `pre-launch-auth-hardening-checklist.md` (canonical security baseline). Source-of-truth code path: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/**` (paths created by Wave 72a Bucket A backend).
