# GAP-515: Account lockout missing (OWASP A07)

**Status:** 🟡 PARTIAL (80% — Wave 72a Bucket B PR #1287)
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-13 (Wave 71c per `pre-launch-auth-hardening-checklist.md` §2.2)
**Affects:** All login endpoints; current `AuthService` accepts unlimited failed attempts per email

## Problem

`grep failedLoginAttempts|accountLocked|MAX_LOGIN_ATTEMPTS` returns 0 hits across `kitehub/kitehub-subscription/src/main/java`. Spring Security default does not enforce lockout. Combined with GAP-514 (no rate limit), brute-force surface is wide open.

## Proposed Fix

1. Add columns `failed_login_attempts INT DEFAULT 0`, `locked_until TIMESTAMP` to `users` table (Flyway V*)
2. AuthService.login increments on failure, locks for 15min after 5 attempts within 15min window
3. Exponential backoff: 3rd lockout = 1hr, 4th = 24hr
4. Unit + integration tests

## Acceptance Criteria

- [x] Columns + migration shipped (`V35__add_account_lockout_columns.sql` — Wave 72a Bucket B PR #1287)
- [x] 5 wrong passwords → 6th attempt → HTTP 423 LOCKED (`AuthServiceLockoutTest`)
- [x] After 15min, 1st attempt allowed; correct password resets counter (`successResetsCounterPreservesHistory` test)
- [x] Exponential backoff verified by unit tests (`secondLockoutIsOneHour`, `thirdLockoutIs24Hours`)
- [ ] FE consumes Retry-After header in 423 response (FE work — deferred to follow-up GAP)

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.2
- Sibling: GAP-514 (rate limit), GAP-516 (2FA)
- PR: #1287 (Wave 72a Bucket B)

## Log

- **2026-05-14** Wave 72a Bucket B PR #1287 ships BE implementation: V35 migration + `User` lockout fields + `AuthService.login` lockout-before-password-compare + `AccountLockedException` → 423 + `Retry-After` + `AccountLockoutPolicy` exponential backoff schedule + 7 unit tests. Status → 🟡 PARTIAL pending FE retry-after handling.
