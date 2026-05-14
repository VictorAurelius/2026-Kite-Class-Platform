# Pre-Launch Auth Hardening Checklist — OWASP A07 mandatory gate

**Priority:** 🟠 MANDATORY — pre-launch security gate
**Version:** 1.0.0
**Created:** 2026-05-13
**Last-Reviewed:** 2026-05-13
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-uncovered OWASP A07 enforcement)
**Applies to:** Any release tag `v0.9.0-beta-staging.*` → first `v1.0.0-rc.*` transition; every public auth surface (login, register, refresh, verify-email, resend-verification, password-reset)

---

## 1. The Rule

> **Before any pre-production tag promotes to release-candidate or first production tag, all 8 OWASP A07 (Identification and Authentication Failures) checks below MUST pass OR have a documented `AUTH_HARDENING_DEFER` trailer with risk acceptance + follow-up gap.**

OWASP A07 ranks 7th most common security risk class. Wave 71b incident showed our security-audit skill scored 87/100 but missed:
- 4 auth endpoints had NO gateway rate limit (only `/api/auth/register` had it)
- 0 account lockout mechanism
- 0 2FA for privileged admin role
- 0 login-from-new-IP alerts
- 0 password complexity enforcement for tenant users

This rule fills the coverage gap. Security-audit skill Category 4 rubric extended in same PR.

---

## 2. Mandatory checks (8)

### 2.1 Auth endpoints rate limited at gateway (P0)

Every endpoint under `/api/auth/**` AND `/api/v1/auth/**` MUST have gateway `RequestRateLimiter` filter with concrete replenish + burst values.

Required endpoints + minimum rate:

| Path | Replenish/sec | Burst | Key resolver |
|---|---|---|---|
| `/api/auth/register` | 3 | 5 | ipKeyResolver |
| `/api/auth/login` | 5 | 10 | ipKeyResolver |
| `/api/auth/refresh` | 10 | 20 | userKeyResolver |
| `/api/auth/verify-email` | 10 | 15 | ipKeyResolver |
| `/api/auth/resend-verification` | 1 | 2 | emailKeyResolver |
| `/api/auth/password-reset-request` | 1 | 2 | emailKeyResolver |
| `/api/v1/auth/request-beta-access` | 2 | 5 | ipKeyResolver |

Verify: `grep -A5 'id: auth\|id: <backend-service-prefix>-auth' <api-gateway-service>/.../application.yml` shows RequestRateLimiter on every entry.

### 2.2 Account lockout after failed login attempts (P0)

After 5 failed login attempts on same email within 15 min, account locks for 15 min. Lockout uses exponential backoff (3rd lockout = 1hr, 4th = 24hr).

Verify: search for `failedLoginAttempts`, `accountLocked`, `lockoutUntil` column on `users` table + AuthService logic.

### 2.3 Password complexity policy (P1)

Tenant-user passwords MUST satisfy:
- Min 12 chars (recommend 14)
- Mix of upper + lower + digit + symbol (or passphrase ≥20 chars exempt)
- Reject top-10000 leaked passwords (haveibeenpwned-like list or zxcvbn score ≥3)
- No reuse of last 3 passwords

Admin passwords (PLATFORM_ADMIN role): 16+ chars OR rotated by AWS Secrets Manager.

Verify: `PasswordValidator.java` exists + applied at registration + reset.

### 2.4 2FA mandatory for PLATFORM_ADMIN role (P1)

TOTP-based 2FA enrollment mandatory before any privileged action. Admin login requires both password + 6-digit TOTP code.

Verify: `TwoFactorAuthService` exists + AuthService.login checks role + enforces 2FA challenge.

### 2.5 Login alerts for privileged roles (P2)

Email to admin when login occurs from new IP, new User-Agent, or new geolocation. Cooldown 24h per (user, fingerprint).

Verify: `LoginAuditService` emits event on login + EmailService consumer fires alert for PLATFORM_ADMIN.

### 2.6 JWT secret rotation policy (P1)

JWT signing keys rotated quarterly. Old keys honored for refresh-token TTL window then expired. RS256 with separate signing + verifying key recommended; HS256 acceptable with documented rotation runbook.

Verify: `jwt.secret-current` + `jwt.secret-previous` config slots exist OR documented manual rotation runbook + AWS Secret versioned.

### 2.7 Audit log for admin actions (P1)

Every PLATFORM_ADMIN action (approve/reject beta, suspend instance, modify config) writes `admin_audit_log` row with (timestamp, admin_user_id, action, target_entity_id, request_ip, user_agent).

Verify: `AdminAuditLog` entity exists + interceptor wired on admin controllers.

### 2.8 Session timeout + refresh token rotation (P2)

Access token TTL ≤ 15 min. Refresh token TTL ≤ 7 days. Refresh token rotated on each use (old token blacklisted). Reuse of blacklisted refresh = force logout all sessions.

Verify: JWT TTL config + `RefreshTokenRepository` has `blacklisted_at` + reuse detector logic.

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "We'll add rate limit post-launch" | All 8 checks before any v1.0.0-rc tag |
| "PLATFORM_ADMIN only us, skip 2FA" | 2FA mandatory regardless of headcount |
| Skip lockout "bcrypt is slow enough" | Lockout protects against credential stuffing patterns bcrypt doesn't |
| Use AWS-generated password as "complexity satisfied" | Admin password OK; user password policy separate check |
| Score 87/100 by averaging — auth gaps hidden in 5-point category | Per-check pass/fail; 1 fail = checklist fail |

---

## 4. Worked self-test — Wave 71b current state

**Apply §2 checklist to current main HEAD 2026-05-13:**

| # | Check | Current state | Verdict |
|---|---|---|---|
| 2.1 | Rate limit on auth endpoints | `/api/auth/register` ✅ only (3/sec); `/api/auth/login` ❌ NO rate limit; refresh ❌; verify-email ❌; resend ❌; password-reset ❌ N/A; /api/v1/auth/request-beta-access ❌ | **FAIL** |
| 2.2 | Account lockout | `grep failedLoginAttempts\|accountLocked` → 0 hits | **FAIL** |
| 2.3 | Password complexity | TBD — `grep PasswordValidator` → unclear scope | likely FAIL |
| 2.4 | 2FA admin | `grep TwoFactor\|TOTP` → 0 hits | **FAIL** |
| 2.5 | Login alerts | `grep LoginAuditService\|new IP` → 0 hits | **FAIL** |
| 2.6 | JWT rotation | JWT secret in env var; rotation runbook absent | likely FAIL |
| 2.7 | Admin audit log | `grep AdminAuditLog` → TBD | likely FAIL |
| 2.8 | Refresh rotation | `grep blacklisted_at\|refresh.*rotation` → TBD | likely FAIL |

**Verdict:** 5 confirmed FAIL + 3 likely FAIL. **Cannot promote to v1.0.0-rc until addressed.** This is the originating incident — rule fires correctly.

8 gaps filed same PR as this rule (GAP-514..521). Wave 71c bundles them.

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Security-audit skill rubric extension (paired same PR)

`.claude/skills/quality/security-audit/SKILL.md` Category 4 "Auth & Access Control" rubric extended with 8 explicit per-check rows. Each check pass/fail (no averaging). Sub-score replaces gut-feel 20-pt allocation.

### 5.2 Pre-promotion gate

Before any maintainer creates git tag matching `v1.0.0-rc.*` or `v1.0.0` (first GA), `bash scripts/check-auth-hardening.sh` (deferred per premature-rule guard ≥7 days — manual checklist suffices for v1.0.0) MUST exit 0. Currently: manual run §2 checklist.

### 5.3 Reviewer checklist

When reviewing any PR that touches `<api-gateway-service>/application.yml` OR `<subscription-service>/.../auth/**` OR adds new auth endpoint, reviewer asks: §2 checks for new endpoint still satisfied?

### 5.4 Override mechanism

For genuine schedule pressure (regulator deadline, contract signing):

```
git commit -m "...
AUTH_HARDENING_DEFER: <check ID + reason>
AUTH_HARDENING_FOLLOWUP: <gap link with completion date ≤14d from defer>"
```

Trailer logged. Pattern frequency >2 defers per release = meta-review.

### 5.5 Detector (deferred)

Future: `scripts/check-auth-hardening.sh` parses gateway YAML + greps service code for the 8 §2 markers. Defer until 2nd recurrence of OWASP A07 finding.

---

## 6. Relationship to other rules

- **`security-audit/SKILL.md`** — Category 4 rubric extended same PR
- **`output-review-mandate.md`** §3 — Security audit row already exists; this rule sharpens Category 4 substance
- **`release-deploy-standard.md`** §3.4 — MAJOR/first-PROD checklist includes "Pen-test light (OWASP top 10)"; this rule operationalizes A07 specifically
- **`pre-handoff-self-test-completeness.md`** (sister rule, same PR) — both ship from Wave 71b incident
- **`incident-to-rule-pipeline.md`** — direct output of "security-audit 87/100 missed obvious auth gaps" incident
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + skill rubric extension + 8 gaps + ROADMAP all same PR
- **`gap-done-discipline.md`** — DONE flip on auth surface requires §2 checklist pass for the scope touched

---

## 7. Log

- **2026-05-13 (v1.0.0):** Rule created in response to user-flagged miss — security-audit skill scored 87/100 at Wave 40 milestone but missed 5 confirmed + 3 likely OWASP A07 gaps. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user "audit không review ra các gaps đơn giản này") → Classify ✓ (security-audit Category 4 rubric was vague — "JWT validation, role checks, rate limiting" with no per-endpoint enforcement check; rubric allowed averaging that hid auth gaps) → Rule+Enforce ✓ (this file + security-audit/SKILL.md rubric extension + 8 gap files + worked §4 self-test + paired with `pre-handoff-self-test-completeness.md` per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§4 worked example on current main — 5 confirmed FAIL surfaced, file GAP-514..521) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds per-endpoint OWASP A07 coverage to previously-vague rubric; no constraint loosening for prior tags; existing v0.9.0-beta-staging.* tags grandfathered; rule applies prospectively to v1.0.0-rc promotion).
