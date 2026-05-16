---
title: Security — Account Lockout Verification (Wave 86 Bucket E Fix 1)
status: complete
created: 2026-05-16
phase: phase-1-beta
wave: 86
gaps: [GAP-515]
---

# Security Audit — Account Lockout (OWASP A07) Verification

## Scope

Wave 86 Bucket E Fix 1 — verify `users.failed_login_attempts` + lockout mechanism per OWASP A07 Identification & Authentication Failures + `.claude/rules/pre-launch-auth-hardening-checklist.md` Cat 4.

**Originating finding:** Cat 4 Auth sweep claimed "no `users.failed_login_attempts` column + no lockout service". State-check per `audit-to-gap-pipeline.md` §2.5 surfaces the claim is **STALE** — lockout shipped Wave 72a GAP-515.

## State-check evidence

### Flyway migration

`kitehub/kitehub-subscription/src/main/resources/db/migration/V35__add_account_lockout_columns.sql`:

```sql
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_failed_login_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS locked_until          TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lockout_count         INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_users_locked_until
    ON users (locked_until) WHERE locked_until IS NOT NULL;
```

✅ All 4 columns present + partial index for lookup.

### Entity (canonical)

`kitehub/kitehub-platform/src/main/java/com/kitehub/platform/domain/entity/User.java` — fields `failedLoginAttempts`, `lastFailedLoginAt`, `lockedUntil`, `lockoutCount` all mapped with javadoc citing GAP-515 + OWASP A07.

### Service — AccountLockoutPolicy

`kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/AccountLockoutPolicy.java`:

| Constant | Value | Rule mapping |
|----------|-------|--------------|
| `MAX_FAILED_ATTEMPTS` | 5 | BR-AUTH-001 §1 row 1 |
| `ATTEMPT_WINDOW_MINUTES` | 15 | BR-AUTH-001 §1 row 2 |
| `computeLockedUntil(0)` | 15 min | 1st lockout |
| `computeLockedUntil(1)` | 1 hr | 2nd lockout |
| `computeLockedUntil(2+)` | 24 hr | 3rd+ lockout |

✅ Exponential backoff matches `documents/01-business/kitehub/auth/rules.md` BR-AUTH-001.

### Service — AuthService integration

`AuthService.java` lines 357-489:
- Pre-login check: `if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) throw AccountLockedException(user.getLockedUntil())`
- Failed login: `recordFailedLogin(user, now)` increments `failedLoginAttempts` + resets if window expired + sets `lockedUntil` when threshold hit
- Success: clears counter + preserves `lockoutCount` for backoff history

### Exception → HTTP

`AccountLockedException` → `GlobalExceptionHandler` → **HTTP 423 LOCKED** with body `{error: "ACCOUNT_LOCKED", lockedUntil: ISO8601}` per BR-AUTH-002.

### Unit tests

`AuthServiceLockoutTest.java` (Wave 72a GAP-515):
- Wrong password increments `failedLoginAttempts`
- 5th wrong password sets `lockedUntil` (15min, lockoutCount=1)
- Subsequent attempt while locked → `AccountLockedException` BEFORE password compare (timing-safe)
- Success resets counter but preserves `lockoutCount`
- Exponential backoff 2nd = 1h, 3rd = 24h

✅ Tests exercise all rule §2 invariants.

### Business rules doc

`documents/01-business/kitehub/auth/rules.md`:
- BR-AUTH-001 — Failed login exponential backoff lockout
- BR-AUTH-002 — Account lockout HTTP 423 response
- Config keys documented: `kitehub.auth.lockout.{max-attempts,window-minutes,base-duration-minutes,escalation-multipliers}`

### API contract

`documents/01-business/kitehub/auth/api-contract.md` line 311-385:
- 423 LOCKED response shape documented
- ACCOUNT_LOCKED error code mapped to BR-AUTH-001/002

## Verdict

✅ **Account lockout (OWASP A07) FULLY IMPLEMENTED** — pre-dating Wave 86 since Wave 72a (GAP-515 DONE).

The Cat 4 Auth sweep finding "no lockout service" is **STALE** — re-audited Wave 86 Bucket E Fix 1 confirms 6-layer presence:

1. ✅ DB schema (V35 migration)
2. ✅ Entity (User.java)
3. ✅ Service (AccountLockoutPolicy)
4. ✅ Integration (AuthService.login)
5. ✅ HTTP 423 (AccountLockedException → GlobalExceptionHandler)
6. ✅ Unit tests (AuthServiceLockoutTest)
7. ✅ Docs (rules.md + api-contract.md)

**Pass Phase 1 BETA gate.** No further action required Wave 86.

## Follow-up (defer Wave 87+)

Per `AccountLockoutPolicy` javadoc (v1):

> Values are intentionally hardcoded constants (not @Value config) for v1 — the cost of mistakenly relaxing these via env var outweighs flex benefits. If tuning becomes necessary, promote to `application.yml` keys `kitehub.auth.lockout.*` in a follow-up gap.

→ Tracked as Phase 1.5 enhancement (NOT blocker).

## Self-test (pre-handoff per pre-handoff-self-test-completeness.md §2.4)

| Check | Verdict |
|---|---|
| (a) BR-AUTH-001/002 in rules.md | ✅ |
| (b) Code matches rule values (5 attempts / 15 min / exp backoff) | ✅ |
| (c) AuthService.login enforces gate BEFORE password compare | ✅ |
| (d) HTTP 423 documented in api-contract.md | ✅ |
| (e) Unit tests exercise lockout + reset + backoff scenarios | ✅ |
| (f) `audit-to-gap-pipeline.md` §2.7 (code-sync) verified | ✅ |

## References

- GAP-515 (Wave 72a — account lockout shipped)
- BR-AUTH-001/002 — `documents/01-business/kitehub/auth/rules.md`
- `.claude/rules/pre-launch-auth-hardening-checklist.md` Cat 4
- OWASP Top 10 A07 — Identification & Authentication Failures
- `audit-to-gap-pipeline.md` §2.5 state-check + §2.7 decision-doc code-sync

## Log

- **2026-05-16:** Re-audit Wave 86 Bucket E Fix 1 — original Cat 4 finding STALE; lockout fully implemented Wave 72a (GAP-515). 7-layer evidence chain verified. Pass.
