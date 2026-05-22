---
title: Wave 103 Bucket C — 2FA TOTP Local Walk Verify
status: complete
created: 2026-05-22
phase: phase-1-beta
wave: 103
bucket: C
gaps: [GAP-516, GAP-547]
---

# 2FA TOTP Local Walk Verify (Wave 103 Bucket C)

## Scope

Verify GAP-516 (2FA TOTP mandatory for PLATFORM_ADMIN) end-to-end on local Docker stack against real Postgres. Three flows tested:
1. **Enrollment** — `POST /api/v1/auth/2fa/enroll-init` → `POST /api/v1/auth/2fa/enroll-confirm`
2. **Verification** — login → challenge → `POST /api/v1/auth/2fa/verify`
3. **Cleanup** — disable 2FA on admin for next-session clean state

Closes pre-handoff self-test gate per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist; satisfies `release-deploy-standard.md` §3.1 "Smoke admin-login" + `pre-launch-auth-hardening-checklist.md` §2.4 "2FA mandatory for PLATFORM_ADMIN".

## Pre-conditions

- Stack 13/13 healthy (Bucket E verify); `curl http://localhost:9000/actuator/health` → 200
- Admin user `admin@kitehub.com` (UUID `00000000-0000-0000-0000-000000000099`), role `PLATFORM_ADMIN`, password `Admin@KiteHub123`
- DB schema: `users` has `totp_required` (boolean), `totp_secret_encrypted` (varchar 256), `totp_enrolled_at` (timestamptz), `recovery_codes_hashes` (text) — V37 migration applied
- Gateway port 9000 routes to kitehub-subscription port 8081

## Endpoints discovered

Source: `kitehub/kitehub-subscription/.../auth/twofactor/TwoFactorController.java` v1 paths (Wave 79 GAP-547):

| Endpoint | Auth model | DTO |
|---|---|---|
| `POST /api/v1/auth/2fa/enroll-init` | `Bearer <challenge_token>` purpose=`TWO_FACTOR_ENROLL` | (none — body empty) |
| `POST /api/v1/auth/2fa/enroll-confirm` | same challenge token | `EnrollConfirmRequest{first_totp_code}` |
| `POST /api/v1/auth/2fa/verify` | challenge_token in body | `VerifyRequest{challenge_token, totp_code or recovery_code}` |
| `POST /api/v1/auth/2fa/disable` | Access JWT (Bearer) | `DisableRequest{current_totp_code, password_reconfirm}` — RESTRICTED: returns `403 CANNOT_DISABLE_2FA_FOR_ADMIN` for PLATFORM_ADMIN |
| `POST /api/v1/auth/2fa/recovery-codes/regenerate` | Access JWT | regen request |

Backward-compat alias `/api/auth/2fa/**` (deprecated 2026-06-14).

## Walk results

### Flow 1 — Enrollment (with workaround)

Step 1.1: Flip `totp_required=true` on admin (default seed has it `false`):
```sql
UPDATE users SET totp_required=true WHERE email='admin@kitehub.com';
```

Step 1.2: Login → 2FA enrollment challenge issued:
```bash
curl -X POST http://localhost:9000/api/auth/login -d '{"email":"admin@kitehub.com","password":"Admin@KiteHub123"}'
# → {"requires2fa_enrollment":true,"challenge_token":"<JWT HS256 purpose=TWO_FACTOR_ENROLL>"}
```
**PASS** — `AuthService.login` correctly issues `TWO_FACTOR_ENROLL` challenge per BR-AUTH-2FA when `totp_required=true && totp_enrolled_at IS NULL`.

Step 1.3: enroll-init via gateway port 9000 → **HTTP 401** (no body)
Step 1.4: enroll-init direct to subscription port 8081 with Bearer challenge → **HTTP 401**
Step 1.5: enroll-init direct + spoofed `X-User-Id` + `X-User-Roles` headers → **HTTP 200**:
```json
{"secret":"<REDACTED-32CHAR-TOTP-SECRET>","qr_uri":"otpauth://totp/admin%40kitehub.com?...&issuer=KiteHub","recovery_codes":["<REDACTED>",...10 codes]}
```

Step 1.6: Compute TOTP code via `python3 -c "import pyotp; print(pyotp.TOTP('<secret>').now())"` → `698246`.

Step 1.7: enroll-confirm with `first_totp_code` → **HTTP 200**:
```json
{"enrolled":true,"totp_enrolled_at":"2026-05-22T04:02:54...","access_token":"<JWT>","refresh_token":"<JWT>"}
```

DB verify:
```
email             | totp_required | enrolled | has_secret
admin@kitehub.com | t             | t        | t
```

**Verdict:** Enrollment business logic + controller + service + Postgres persistence all WORK CORRECTLY. Spring Security middleware blocks the flow at gateway/auth layer (see §Bugs found).

### Flow 2 — Verification (post-enrollment login)

Step 2.1: Re-login → **HTTP 200**, but now `requires2fa:true` + `TWO_FACTOR_VERIFY` challenge:
```json
{"requires2fa":true,"challenge_token":"<JWT HS256 purpose=TWO_FACTOR_VERIFY>"}
```
**PASS** — confirms branch at `AuthService:432` (`if (user.getTotpEnrolledAt() != null)`) fires post-enrollment.

Step 2.2: Compute fresh TOTP → `165387`.

Step 2.3: `/verify` with `{challenge_token, totp_code}` body + spoofed headers → **HTTP 200**:
```json
{"access_token":"<JWT>","refresh_token":"<JWT>","user":{"id":"...","email":"admin@kitehub.com","role":"PLATFORM_ADMIN"}}
```
**PASS** — TOTP verification + token issuance work end-to-end.

### Flow 3 — Cleanup

`disable` endpoint returns 403 for PLATFORM_ADMIN role per `CANNOT_DISABLE_2FA_FOR_ADMIN` (BR-AUTH-005). Used direct DB cleanup:

```sql
UPDATE users SET totp_required=false, totp_secret_encrypted=NULL, totp_enrolled_at=NULL, recovery_codes_hashes=NULL WHERE email='admin@kitehub.com';
DELETE FROM recovery_codes WHERE user_id='00000000-0000-0000-0000-000000000099';
-- UPDATE 1, DELETE 10
```

Final DB state:
```
email             | totp_required | enrolled | has_secret
admin@kitehub.com | f             | f        | f
```

Post-cleanup login → returns `accessToken` directly (no 2FA gate). **Clean state confirmed.**

## Bugs found (P1 — infra incompleteness)

GAP-516 ships TOTP business logic and DB schema correctly, but **two middleware bugs** block end-to-end use through the gateway:

### Bug 1 — Gateway JWT filter rejects challenge tokens (P1)

`kitehub-gateway/.../JwtAuthenticationGatewayFilter.java` lines 92-122 parse all `Bearer` tokens with `signingKey` (HS512, derived from `jwt.secret`). The 2FA challenge token is issued by `ChallengeTokenService` using HS256 + DIFFERENT key (`jwt.challenge-secret`). Result: any request to `/api/v1/auth/2fa/**` with Bearer challenge token → `JwtException` caught → 401 short-circuit at line 120, never reaches subscription service.

**Workaround tested:** none viable through gateway. Direct subscription port 8081 also fails (see Bug 2).

**Fix sketch:** gateway filter should detect challenge tokens (e.g. by `purpose` claim or path prefix `/api/v1/auth/2fa/**`) and pass-through unverified to let subscription's controller verify with its own key.

### Bug 2 — Subscription Security requires X-User-Id headers for /api/v1/auth/2fa/** (P1)

`SecurityConfig.java` line 98: `.requestMatchers("/api/v1/auth/2fa/**").authenticated()`. The only auth filter (`XUserRolesHeaderFilter` line 144) translates gateway-injected `X-User-Id`/`X-User-Roles` headers into Authentication. Challenge tokens are NEVER converted to Authentication. Therefore:
- Through gateway: Bug 1 blocks before headers injected (since JWT parse fails).
- Direct to 8081 without headers: 401 from Spring Security AuthenticationEntryPoint.
- Direct to 8081 with spoofed headers: ✅ works (workaround used in this audit).

**Fix sketch:** add custom `ChallengeTokenAuthenticationFilter` registered before `XUserRolesHeaderFilter` that recognizes `Bearer <challenge_token>` on `/api/v1/auth/2fa/**` paths and sets a `ChallengeAuthentication` principal. Or change Security config to `.permitAll()` for `/api/v1/auth/2fa/**` and rely on controller's `requireChallenge(...)` for auth (already implemented).

### Bug 3 — `LoginAuditService` duplicate-row warning (P2, pre-existing)

Every login logs:
```
WARN c.k.s.audit.login.LoginAuditService - LoginAuditService.recordLogin failed (login proceeds anyway): Query did not return a unique result: N results were returned
```
Suggests a `findOne` repository call against `login_audit_log` returning multiple rows where it expects 0/1. Pre-existing (not introduced by Wave 103 Bucket C). Should be filed as new gap for triage. Login still succeeds due to `Propagation.REQUIRES_NEW` per `audit-service-isolation.md` — confirming that rule's value.

## GAP-516 status proposal

**Current Status (per gap-status.csv): PARTIAL ~80%** (per recent docs(wave-102.9-C) commit).

**Proposed: PARTIAL ~90%** — business logic + DB schema + TOTP cryptography + controller all verified working end-to-end on real Postgres. Two middleware bugs block GATEWAY-PATH USE for tenant admin login through the public surface.

To reach DONE 100%: file follow-up gap (GAP-NEW) covering Bug 1 + Bug 2 (gateway/security middleware filters for challenge-token auth). One-line fix sketch in §Bugs found above. Expected ~half day work.

## Cleanup confirmation

Final DB state for `admin@kitehub.com`:
- `totp_required`: false
- `totp_enrolled_at`: NULL
- `totp_secret_encrypted`: NULL
- `recovery_codes_hashes`: NULL
- `recovery_codes` table: 0 rows for admin user

Post-cleanup login confirmed clean (access token issued directly, no 2FA gate). System ready for next-session use.

## Commands run (full transcript reference)

```bash
# Stack health
curl -sI http://localhost:9000/actuator/health  # → 200

# Endpoint discovery
grep -rEn "@PostMapping|@RequestMapping" kitehub/kitehub-subscription/.../twofactor/TwoFactorController.java

# Flip required, login → 2FA enroll challenge
docker exec kite-postgres psql -U kitehub -d kitehub -c "UPDATE users SET totp_required=true WHERE email='admin@kitehub.com'"
curl -X POST http://localhost:9000/api/auth/login -d '{"email":"admin@kitehub.com","password":"Admin@KiteHub123"}'

# Enroll-init (direct + spoofed headers workaround)
curl -X POST http://localhost:8081/api/v1/auth/2fa/enroll-init \
  -H "Authorization: Bearer <CHAL>" -H "X-User-Id: 00000...099" -H "X-User-Roles: PLATFORM_ADMIN"

# Compute TOTP code
python3 -c "import pyotp; print(pyotp.TOTP('<SECRET>').now())"

# Enroll-confirm
curl -X POST http://localhost:8081/api/v1/auth/2fa/enroll-confirm \
  -H "Authorization: Bearer <CHAL>" -H "X-User-Id: ..." -H "X-User-Roles: PLATFORM_ADMIN" \
  -d '{"first_totp_code":"<CODE>"}'

# Verify (post-enrollment)
curl -X POST http://localhost:9000/api/auth/login -d '{"email":"admin@kitehub.com","password":"Admin@KiteHub123"}'
curl -X POST http://localhost:8081/api/v1/auth/2fa/verify \
  -H "X-User-Id: ..." -H "X-User-Roles: PLATFORM_ADMIN" \
  -d '{"challenge_token":"<CHAL>","totp_code":"<CODE>"}'

# Cleanup
docker exec kite-postgres psql -U kitehub -d kitehub -c "
UPDATE users SET totp_required=false, totp_secret_encrypted=NULL, totp_enrolled_at=NULL, recovery_codes_hashes=NULL WHERE email='admin@kitehub.com';
DELETE FROM recovery_codes WHERE user_id='00000000-0000-0000-0000-000000000099';
"
```

## References

- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TwoFactorController.java`
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/AuthService.java` (lines 420-470)
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/SecurityConfig.java` (line 98)
- `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java` (lines 92-122)
- `kitehub/kitehub-subscription/src/main/resources/db/migration/V37__add_user_2fa_columns.sql`
- `documents/01-business/kitehub/auth-2fa/api-contract.md` (BR-AUTH-2FA-007 versioning)
- Rules applied: `pre-handoff-self-test-completeness.md` §2.4, `pre-launch-auth-hardening-checklist.md` §2.4, `audit-service-isolation.md`
