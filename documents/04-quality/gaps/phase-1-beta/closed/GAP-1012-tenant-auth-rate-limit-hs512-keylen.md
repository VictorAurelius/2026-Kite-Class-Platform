# GAP-1012: kc-tenant-auth login route no rate-limit + gateway HS512 key-check ≥32 not ≥64 (auth-1)

**Status:** 🟢 DONE
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

- [x] `kc-tenant-auth` route has RequestRateLimiter (3/5 IP-keyed) — live-verified 429: req 1-5 → 401, req 6-8 → 429
- [x] Gateway HS512 key-check ≥64 bytes; comment cites HS512 (512 bits) — `<32`→`<64` in JwtAuthenticationGatewayFilter + TenantHeaderGuardFilter (cross-flow sweep); challenge HS256 block left at `<32`
- [x] Both verified against running gateway — rebuilt kite-gateway:9000 (healthy with real 82-byte secret) + live 429 burst PASS

## Log

- **2026-06-06** DONE — PR #2189. Cross-flow sweep (`cross-flow-bug-class-sweep.md`): HS512 `<32` guard found in BOTH `JwtAuthenticationGatewayFilter` + `TenantHeaderGuardFilter` (audit flagged only former). Caller sweep (`api-contract-change-caller-sweep.md`): TEST_SECRET 33→66 bytes in 2 filter tests + KiteHubGatewayApplicationTest jwt.secret — now exercise real HS512. `./mvnw -pl kitehub-gateway test` PASS. Live-verified against rebuilt local gateway: 429 fires on req 6+ (burst 5); gateway boots healthy (≥64 guard safe — local 82B / prod 64B). Challenge-secret block correctly left at `<32` (HS256).

## Related

- Audit reports: `documents/04-quality/audits/ops-readiness/2026-06-06-wave-auth-1-ops-readiness.md` + `../business-logic/2026-06-06-wave-auth-1-business-logic.md`
- GAP-514 (rate-limit pattern, DONE for kitehub /auth/*)
