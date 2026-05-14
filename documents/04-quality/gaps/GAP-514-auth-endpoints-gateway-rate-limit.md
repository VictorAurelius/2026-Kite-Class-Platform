# GAP-514: Auth endpoints missing gateway rate limit (OWASP A07)

**Status:** 🟡 PARTIAL 2026-05-14 — 4/6 AC checked. Code + tests + local-verify shipped Wave 72a Bucket A; live 429 smoke + `check-auth-hardening.sh` script deferred to post-deploy follow-up (require running gateway + Redis stack OR script not yet written).
**Priority:** 🔴 P0 (pre-launch blocker — brute-force / credential-stuffing vector)
**Domain:** DevOps / Backend
**Found:** 2026-05-13 (Wave 71c meta audit per `pre-launch-auth-hardening-checklist.md` §2.1)
**Affects:** All auth endpoints under `/api/auth/**` + `/api/v1/auth/**` except register

## Problem

Per gateway audit: only `/api/auth/register` has `RequestRateLimiter` filter (3/sec replenish, 5 burst). All other auth endpoints have NO rate limit: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/verify-email`, `/api/auth/resend-verification`, `/api/v1/auth/request-beta-access`. Brute-force online unbounded; resend-verification abusable for Resend quota burn.

## Proposed Fix

Add `RequestRateLimiter` filter to each route per `pre-launch-auth-hardening-checklist.md` §2.1 table. Use existing `ipKeyResolver`/`emailKeyResolver`/`userKeyResolver` beans.

## Acceptance Criteria

- [x] All 6 auth endpoints in scope have RequestRateLimiter per §2.1 table (register pre-existing; 5 new + 1 V1 request-beta-access). `/api/auth/password-reset-request` excluded — route does not exist in subscription service yet; will be added when password-reset feature lands (future gap).
- [x] `emailKeyResolver` + `userKeyResolver` beans defined in `KeyResolverConfig` (non-`@Primary` so SCG autoconfig still picks `ipKeyResolver` as default; addressable by name via SpEL)
- [x] `KeyResolverConfigTest` extended with 5 new tests (15 total, all pass)
- [x] `./mvnw -pl kitehub-gateway clean verify -P strict-warnings` GREEN on rebased HEAD
- [ ] `bash scripts/check-auth-hardening.sh` (when written) §2.1 PASS — deferred, script not yet shipped per `pre-launch-auth-hardening-checklist.md` §5.5
- [ ] Live verify: 11 rapid requests to `/api/auth/login` → 11th returns 429 — deferred to post-deploy smoke test (requires running gateway + Redis)

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.1
- Wave 72a Bucket A — PR coming this session
- Follow-up: live 429 verify + `check-auth-hardening.sh` enforcement script

## Log

- **2026-05-14** Wave 72a Bucket A — shipped 5 new `RequestRateLimiter` filters in `kitehub-gateway/application.yml` (auth-login 5/10 ip / auth-refresh 10/20 user / auth-verify-email 10/15 ip / auth-resend-verification 1/2 email / kitehub-auth-v1-request-beta-access 2/5 ip). Added `emailKeyResolver` (X-User-Email header + IP fallback) and `userKeyResolver` (JWT `sub` claim parse without signature validation + IP fallback) beans; existing `ipKeyResolver` keeps `@Primary` per SCG autoconfig single-bean requirement. 15 unit tests in `KeyResolverConfigTest` (was 10) — all pass under `-P strict-warnings`. `/api/auth/password-reset-request` skipped — subscription service has no such endpoint; will be added when feature lands. Live 429 smoke + `check-auth-hardening.sh` enforcement deferred per `gap-done-discipline.md` §3 (PARTIAL exit ramp not invoked — both deferred items have explicit follow-up tracking in §AC + rule §5.5).
