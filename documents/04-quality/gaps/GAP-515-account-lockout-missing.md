# GAP-515: Account lockout missing (OWASP A07)

**Status:** 🟢 DONE 2026-05-14 — Wave 78 Bucket C ships FE Retry-After UX (countdown timer + submit disabled + tests). BE (Wave 72a Bucket B PR #1287) + FE (this PR) combined = 100% AC coverage.
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
- [x] FE consumes Retry-After header in 423 response — `kitehub/kitehub-frontend/src/app/(auth)/login/page.tsx` parses `Retry-After` (delta-seconds + HTTP-date per RFC 7231 §7.1.3), renders countdown `mm:ss` / `Hh Mm Ss`, disables submit, surfaces `data-testid="login-retry-countdown"`. Test in `__tests__/page.test.tsx` asserts 423 → MSW `Retry-After: 900` → "15:00" countdown + disabled submit. Same path also handles 429 (gateway rate-limit) — sister surface per pre-launch-auth-hardening-checklist §2.1.

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.2
- Sibling: GAP-514 (rate limit), GAP-516 (2FA)
- PR: #1287 (Wave 72a Bucket B)

## Log

- **2026-05-14** Wave 72a Bucket B PR #1287 ships BE implementation: V35 migration + `User` lockout fields + `AuthService.login` lockout-before-password-compare + `AccountLockedException` → 423 + `Retry-After` + `AccountLockoutPolicy` exponential backoff schedule + 7 unit tests. Status → 🟡 PARTIAL pending FE retry-after handling.
- **2026-05-14** Wave 78 Bucket C ships FE Retry-After UX in `kitehub-frontend/src/app/(auth)/login/page.tsx`:
  - `parseRetryAfterSeconds()` helper handles both delta-seconds + HTTP-date per RFC 7231 §7.1.3
  - `formatCountdown()` helper renders `mm:ss` for short windows + `Hh Mm Ss` for long
  - `useEffect` interval ticks countdown once per second; cleared on unmount via `clearInterval`
  - Inline `role="alert"` error banner shows lockout message + countdown
  - Separate `aria-live="polite"` countdown badge updates without blocking screen readers
  - Submit button disabled (`aria-disabled`) while `lockoutSecondsRemaining > 0`; label flips to "Tạm khóa — mm:ss"
  - Same handler covers HTTP 429 (gateway rate-limit) — Vietnamese message "Quá nhiều yêu cầu. Thử lại sau ..."
  - Test `parses Retry-After header on 423 and renders countdown + disables submit` asserts the MSW handler's `Retry-After: 900` → "15:00" countdown + disabled state
  Status → 🟢 DONE 100%.
