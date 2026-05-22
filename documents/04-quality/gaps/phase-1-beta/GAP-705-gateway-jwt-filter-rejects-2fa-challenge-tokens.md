---
gap_id: GAP-705
title: Gateway JWT filter rejects 2FA challenge tokens — HS512 vs HS256 secret mismatch
status: OPEN
priority: P1
domain: Backend
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-22
last_updated: 2026-05-22
filed_by: Wave 103 Bucket C live verify
---

# GAP-705 — Gateway JWT filter rejects 2FA challenge tokens

## Problem

`JwtAuthenticationGatewayFilter` (kite-gateway) tries to verify all Bearer tokens with HS512 algorithm + `jwt.secret` key. But 2FA challenge tokens (issued by `TwoFactorService` for the brief window between login-with-2FA-enabled and enroll-confirm/verify) use HS256 algorithm + separate `jwt.challenge-secret` key.

**Symptom:**

```
POST /api/v1/auth/2fa/enroll-init  (with Bearer challenge-token via gateway port 9000)
  → Gateway JwtAuthenticationFilter parses → JwtException ("Signature does not match")
  → 401 short-circuit (never reaches subscription)

VS

POST /api/v1/auth/2fa/enroll-init  (with Bearer challenge-token direct subscription port 8081 + spoofed X-User-Id/X-User-Roles)
  → 200 OK (works because subscription accepts spoofed headers in test profile)
```

**Impact (P1 — 2FA setup flow inaccessible via gateway):**
- Production flow REQUIRES gateway (port 9000) — direct-to-subscription only works in dev
- 2FA enrollment + verify endpoints not reachable from FE which routes through gateway
- Workaround for Wave 103 Bucket C live verify: bypass gateway, use direct port 8081 with spoofed headers — NOT production-viable

## Context

- Wave 89 Bucket A (GAP-604) shipped `JwtAuthenticationGatewayFilter` — designed for ACCESS tokens (HS512)
- 2FA challenge tokens (Wave 72b TwoFactor* services) use different key/algorithm by security design — separate secret namespace prevents access-token-as-challenge-token confusion attack
- Gateway filter doesn't distinguish — treats all Bearer tokens identically

## Proposed Fix

1. **Distinguish token type** at gateway filter:
   - Parse JWT header to extract `alg` claim
   - If `alg == "HS256"` → try `jwt.challenge-secret` for signature verify
   - If `alg == "HS512"` → use existing `jwt.secret`
   - If neither → 401 as before
2. **Alternative (simpler)**: Use JWT `type` claim (`access` vs `challenge`) to select secret
3. **Add filter route whitelist**: paths `/api/v1/auth/2fa/**` accept challenge tokens; other paths require access tokens
4. **Integration test** `GatewaySecurityIT.shouldRouteChallenge2faPath()` — challenge token Bearer → 200 OK on /2fa/enroll-init
5. **Cross-check** subscription security config (see [[GAP-706]] sister bug)

## Acceptance Criteria

- [ ] Gateway filter accepts both HS512 access tokens + HS256 challenge tokens
- [ ] `POST /api/v1/auth/2fa/enroll-init` via port 9000 with challenge Bearer → 200 OK
- [ ] `POST /api/v1/auth/2fa/verify` via port 9000 with challenge Bearer → 200 OK
- [ ] IT `GatewaySecurityIT.shouldRouteChallenge2faPath()` PASS
- [ ] Live verify per Wave 103 Bucket C pattern, but via gateway port 9000 (not direct 8081)
- [ ] GAP-516 status flip to 100% DONE when this + GAP-706 land

## Related

- [[GAP-516]] 2FA TOTP — current 90% PARTIAL, blocked by this + GAP-706 for production-via-gateway flow
- [[GAP-706]] Subscription Security challenge-token bridge (sister bug)
- [[GAP-604]] Wave 89 Bucket A JwtAuthenticationGatewayFilter
- Wave 103 audit: `documents/04-quality/audits/local-stack/2026-05-22-wave-103-2fa-totp-walk.md`
