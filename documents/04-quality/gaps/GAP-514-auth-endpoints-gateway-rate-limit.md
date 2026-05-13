# GAP-514: Auth endpoints missing gateway rate limit (OWASP A07)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (pre-launch blocker — brute-force / credential-stuffing vector)
**Domain:** DevOps / Backend
**Found:** 2026-05-13 (Wave 71c meta audit per `pre-launch-auth-hardening-checklist.md` §2.1)
**Affects:** All auth endpoints under `/api/auth/**` + `/api/v1/auth/**` except register

## Problem

Per gateway audit: only `/api/auth/register` has `RequestRateLimiter` filter (3/sec replenish, 5 burst). All other auth endpoints have NO rate limit: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/verify-email`, `/api/auth/resend-verification`, `/api/v1/auth/request-beta-access`. Brute-force online unbounded; resend-verification abusable for Resend quota burn.

## Proposed Fix

Add `RequestRateLimiter` filter to each route per `pre-launch-auth-hardening-checklist.md` §2.1 table. Use existing `ipKeyResolver`/`emailKeyResolver`/`userKeyResolver` beans.

## Acceptance Criteria

- [ ] All 7 auth endpoints have RequestRateLimiter per §2.1 table
- [ ] `bash scripts/check-auth-hardening.sh` (when written) §2.1 PASS
- [ ] Live verify: 11 rapid requests to `/api/auth/login` → 11th returns 429

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.1
- Wave 71c candidate
