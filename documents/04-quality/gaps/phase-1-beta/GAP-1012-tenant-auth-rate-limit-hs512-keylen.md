# GAP-1012: kc-tenant-auth login route no rate-limit + gateway HS512 key-check ≥32 not ≥64 (auth-1)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-06 (Wave auth-1 post-wave audit suite — ops-readiness P1 + business-logic P2)
**Affects:** `kitehub-gateway/src/main/resources/application.yml:726-734` (`kc-tenant-auth` route) + `JwtAuthenticationGatewayFilter.java:90-94`

## Problem

1. **No rate-limit on public token-minting login.** The `kc-tenant-auth` gateway route added by auth-1 has no `RequestRateLimiter` filter → brute-force / credential-stuffing throttled only by BCrypt cost. GAP-514 (DONE) added rate-limit to kitehub `/auth/*` routes but did NOT cover the new `kc-tenant-auth` route.
2. **Gateway HS512 secret length check is `≥32` bytes** (`JwtAuthenticationGatewayFilter:90-94`) while `AuthTokenService:47` correctly requires `≥64` for HS512. Inconsistent: gateway would accept a 32-byte secret that the issuer rejects.

## Proposed Fix

(1) Add `RequestRateLimiter` to `kc-tenant-auth` route mirroring GAP-514 pattern (replenishRate 3 / burst 5). (2) Change gateway key-length guard `≥32` → `≥64` bytes to match HS512 issuer + add comment.

## Acceptance Criteria

- [ ] `kc-tenant-auth` route has RequestRateLimiter (verify 429 after burst)
- [ ] Gateway HS512 key-check ≥64 bytes; comment cites HS512 requirement
- [ ] Both verified against running gateway

## Related

- Audit reports: `documents/04-quality/audits/ops-readiness/2026-06-06-wave-auth-1-ops-readiness.md` + `../business-logic/2026-06-06-wave-auth-1-business-logic.md`
- GAP-514 (rate-limit pattern, DONE for kitehub /auth/*)
