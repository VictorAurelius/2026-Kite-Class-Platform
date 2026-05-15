# GAP-582: OAuth callback idempotency — state_token UNIQUE constraint + 409 on duplicate

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A simulation-3axis audit cell 4)
**Affects:** OAuth signup flow cho P2 Owner + future Google/Microsoft SSO

## Problem

Wave 86 Bucket A simulation cell 4: 5 P2 owners concurrent accept invite → signup via Google OAuth; 1 hits transient 503 từ Google identity service. OAuth retry behavior chưa codified:
- FE flow có thể retries automatically
- BE creates duplicate user account → cross-tenant orphan record
- Data integrity nightmare khi cohort scale ≥ 10 tenants

## Root Cause

`oauth_attempts` table thiếu UNIQUE constraint trên `state_token`. Backend OAuth callback handler không reject duplicate state với 409.

## Proposed Fix

1. Flyway migration `V53__oauth_attempts_state_token_unique.sql`:
   - ADD UNIQUE INDEX `idx_oauth_attempts_state_token_unique` ON `oauth_attempts(state_token)`
2. `OAuthCallbackController.java`: catch `DataIntegrityViolationException` → return HTTP 409 với message "Duplicate OAuth state — please retry signup"
3. FE OAuth callback handler: on 409 → redirect to signup start (clean state)
4. Integration test: POST same state_token 2× → 2nd returns 409

## Acceptance Criteria

- [ ] Flyway V53 shipped + applied production
- [ ] OAuth callback returns 409 on duplicate state_token (verified integration test)
- [ ] FE handles 409 gracefully (redirect to clean signup)
- [ ] Zero cross-tenant orphan records trong oauth_attempts table 7 ngày post-deploy

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-simulation-3axis.md` §3 cell 4 + §5 G-AC2
- Wave 86 plan §3 Bucket G AC G-AC2
- `pre-handoff-self-test-completeness.md` §2.7 multi-tenant tenant-switch
