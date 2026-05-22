# GAP-516: 2FA TOTP mandatory for PLATFORM_ADMIN

**Status:** 🟡 PARTIAL — BE done (Wave 72b Bucket A this PR) + FE wizard done (Wave 72b Bucket B PR #1297 already on main); live IT + end-to-end verify pending
**Priority:** 🟠 P1 (Phase 1 BETA admin surface)
**Domain:** Backend + Frontend
**Found:** 2026-05-13 (Wave 71c per `pre-launch-auth-hardening-checklist.md` §2.4)

## Problem

PLATFORM_ADMIN role currently login với chỉ email+password. Một password compromise → full platform takeover (suspend tenants, approve fake beta requests, edit system config).

## Proposed Fix

1. Add `totp_secret VARCHAR(64)`, `totp_enrolled_at TIMESTAMP` to `users`
2. Enrollment flow: first PLATFORM_ADMIN login → forced to enroll TOTP (QR code via OtpAuth URI)
3. Subsequent logins: password OK → present TOTP challenge → verify 6-digit code → issue JWT
4. Library: `dev.samstevens.totp:totp:1.7.1` (well-maintained, RFC 6238)
5. FE: enrollment page + login TOTP step

## Acceptance Criteria

- [x] BE — V37 migration adds `users.totp_secret_encrypted`, `totp_enrolled_at`, `totp_required` + `recovery_codes` table
- [x] BE — `TwoFactorAuthService` (samstevens.totp wrapper) generates secrets + verifies codes
- [x] BE — `RecoveryCodeService` issues 10 single-use bcrypt-hashed codes; regenerate + verify+consume covered
- [x] BE — `ChallengeTokenService` mints 5-min HS256 challenge tokens with dedicated secret
- [x] BE — `TwoFactorController` exposes 5 endpoints per `auth/api-contract.md` (enroll-init, enroll-confirm, verify, regenerate, disable)
- [x] BE — `AuthService.login` returns `requires2fa` / `requires2fa_enrollment` + `challenge_token` when enrollment present/required (per contract)
- [x] BE — `BR-AUTH-005` enforced (PLATFORM_ADMIN cannot disable 2FA)
- [x] BE — Unit + service tests: TOTP verify, recovery code single-use, recovery code regenerate, challenge token round-trip, AuthService 2FA challenge branches (16 new tests; 494 total green)
- [x] FE — 2FA enrollment wizard (`/2fa-setup` page, Bucket B PR #1297)
- [x] FE — Login challenge step (`/2fa-challenge` page, Bucket B PR #1297)
- [x] FE — Recovery codes display + print (`RecoveryCodesDisplay` component, Bucket B PR #1297)
- [ ] IT — `TwoFactorControllerIT` (5 endpoints HTTP wiring) — DEFERRED to follow-up gap inside same wave; service-level coverage via `TwoFactorEnrollmentServiceTest` planned alongside Bucket B integration tests
- [ ] Admin enrolls via QR scan (Google Authenticator / 1Password) — verified end-to-end after Bucket B
- [ ] Login với wrong TOTP → 401 — wired in service (`INVALID_TOTP`)
- [ ] Login với correct TOTP → JWT issued — wired in service via `TokenIssuer`

## Log

- **2026-05-21** (Wave 102.9 Bucket C fix-time state-check): Per `audit-to-gap-pipeline.md` §2.8 verified Wave 72b Bucket A (BE) + Bucket B (FE) work intact — `TwoFactorAuthService` + `TwoFactorController` + `RecoveryCodeService` + `ChallengeTokenService` + `TotpSecretCipher` + 7 DTOs + V37 migration all exist trong `auth/twofactor/` package; FE pages `2fa-setup/page.tsx` + `2fa-challenge/page.tsx` đều render. 12/16 AC ✅ shipped; 4 outstanding (TwoFactorControllerIT + 3 live-verify items) blocked GAP-612 AWS suspension. Status PARTIAL 80% retained — no progress this wave; Bucket C scope reality-mismatched (code already shipped Wave 72b). State-check evidence: `documents/04-quality/audits/persona-review/2026-05-21-wave-102.9-bucket-c-2fa-totp-state-check.md`. Sister to Bucket A + Bucket B same pattern.
- **2026-05-14** (Wave 72b Bucket A rebased onto main containing Wave 77 + Bucket B FE): rebased PR #1301 onto current main (was 56 commits behind). Duplicate Bucket 0 foundation commit (`645de2a0`) auto-dropped (patches identical to merged PR #1294). 3 files conflicted: `gap-status.csv`, `GAP-516.md`, `AuthService.java`. Resolved keeping Bucket A BE changes + Wave 77 tenant signup security (GAP-534/535/536) on AuthService. Combined BE half (this PR) + FE half (PR #1297 already merged) → completion ~80%; remaining: dedicated `TwoFactorControllerIT` + production end-to-end verify per `pre-handoff-self-test-completeness.md` §2.4.
- **2026-05-14** (Wave 72b Bucket A — BE half, ~65% complete): Shipped per `documents/01-business/kitehub/auth/api-contract.md` (Bucket 0 Foundation). V37 migration + User entity extension + `auth.twofactor` package (TwoFactorAuthService, RecoveryCodeService, ChallengeTokenService, TotpSecretCipher, TwoFactorEnrollmentService, TwoFactorController, 7 DTOs, RecoveryCode entity + repository). AuthService.login extended with 2FA branch. `dev.samstevens.totp:1.7.1` dep added. 16 new tests added (TwoFactorAuthServiceTest 6, RecoveryCodeServiceTest 6, ChallengeTokenServiceTest 4, AuthServiceTwoFactorChallengeTest 3, plus AuthServiceLockoutTest constructor adapted). Local verify `./mvnw -pl kitehub-subscription verify -P strict-warnings` GREEN (494/494). Status stays PARTIAL pending FE Bucket B + dedicated controller IT.

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.4
- Contract: `documents/01-business/kitehub/auth/api-contract.md` (Wave 72b Bucket 0)
- Wave plan: `documents/03-planning/waves/wave-2026-05-14-72b-2fa-audit-rubric-review.md`
