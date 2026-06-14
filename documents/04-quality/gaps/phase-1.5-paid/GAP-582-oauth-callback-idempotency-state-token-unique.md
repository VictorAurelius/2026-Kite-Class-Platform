# GAP-582: OAuth callback idempotency — state_token UNIQUE constraint + 409 on duplicate

**Status:** 🟡 PARTIAL (35%) — AC#1 DB-layer idempotency shipped (Flyway V51 + UNIQUE state_token); AC#2/3/4 deferred until OAuth callback controller + FE OAuth signup flow exist (currently no `/auth/callback/{provider}` endpoint shipped — table is defensive scaffolding ahead of provider integration)
**Priority:** 🟠 P1
**Domain:** Backend
**Phase:** phase-1.5-paid
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

- [x] Flyway migration shipped (V51 — renumbered from V53 since V51/V52 slots open; creates `oauth_attempts` table với UNIQUE state_token + status enum + cleanup index). Applied automatically next deploy.
- [ ] OAuth callback returns 409 on duplicate state_token (verified integration test) — **deferred**: OAuth callback controller not yet implemented in `kitehub-subscription` (no `/auth/callback/{provider}` endpoint). Will be wired same PR as OAuth signup flow lands.
- [ ] FE handles 409 gracefully (redirect to clean signup) — **deferred**: FE OAuth callback handler not yet implemented; gated by OAuth signup feature.
- [ ] Zero cross-tenant orphan records trong oauth_attempts table 7 ngày post-deploy — **deferred**: requires OAuth signup live traffic; verify post Phase 1.5 OAuth rollout.

## Follow-up

After OAuth signup feature lands (Phase 1.5 estimated), close AC#2-4 in dedicated fix PR:

1. Implement `OAuthCallbackController` với `@PostMapping("/auth/callback/{provider}")`
2. Catch `DataIntegrityViolationException` from `oauth_attempts` insert → return HTTP 409 với problem-detail `{"type":"oauth-state-replay","title":"State token already used","status":409}`
3. FE OAuth callback handler: on 409 → redirect `/signup` với toast "Phiên đăng nhập OAuth đã được sử dụng — vui lòng thử lại"
4. Integration test: POST same state_token 2× → 2nd returns 409 + DB row count stays 1

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-simulation-3axis.md` §3 cell 4 + §5 G-AC2
- Wave 86 plan §3 Bucket G AC G-AC2
- `pre-handoff-self-test-completeness.md` §2.7 multi-tenant tenant-switch
- Migration: `kitehub/kitehub-subscription/src/main/resources/db/migration/V51__create_oauth_attempts.sql`

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-1.5-paid (notes '3/4 AC defer Phase 1.5').
- **2026-05-16** **🟡 PARTIAL (35%)** — AC#1 shipped via Wave 86 BE security agent. Flyway V51 creates `oauth_attempts` table với `state_token VARCHAR(255) NOT NULL UNIQUE` (constraint `uk_oauth_attempts_state_token`) + lifecycle status enum (PENDING/SUCCEEDED/FAILED) + cleanup index on `(status, initiated_at)` for stale-row purge. Migration renumbered V53→V51 since V51/V52 slots open (last shipped V50). Defensive scaffolding ahead of OAuth provider integration — DB layer rejects duplicate state_token via UNIQUE constraint regardless of controller logic, eliminating the cross-tenant orphan-record race at the data layer. AC#2 (controller 409 mapping) + AC#3 (FE 409 handler) + AC#4 (7d production stability) deferred — gated by OAuth callback controller + FE OAuth signup flow which do not yet exist in codebase (grep confirmed: zero `OAuth*` Java classes in `kitehub/kitehub-subscription/src/main/java`). Follow-up §"Follow-up" section enumerates concrete next steps for the OAuth-feature wave that lands the controller + FE.
