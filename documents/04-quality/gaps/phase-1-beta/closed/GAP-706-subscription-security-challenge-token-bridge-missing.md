---
gap_id: GAP-706
title: Subscription Security lacks challenge-token→Authentication bridge filter on /api/v1/auth/2fa/**
status: OPEN
priority: P1
domain: Backend
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-22
last_updated: 2026-05-22
filed_by: Wave 103 Bucket C live verify
---

# GAP-706 — Subscription Security challenge-token bridge filter missing

## Problem

`kitehub-subscription` `SecurityConfig` sets path `/api/v1/auth/2fa/**` to `.authenticated()` but **no filter bridges challenge token → Spring Authentication**. The existing `XUserRolesHeaderFilter` only translates `X-User-Id` + `X-User-Roles` headers (set by gateway after gateway-side JWT verify) into Spring SecurityContext — but challenge tokens don't go through that filter chain.

**Symptom:**

```
POST /api/v1/auth/2fa/enroll-init  
  direct to subscription port 8081 
  with Bearer challenge-token
  → 401 Unauthorized (no Authentication context built)

WORKAROUND for Wave 103 Bucket C live verify:
  Add X-User-Id + X-User-Roles headers manually (spoofing what gateway would set)
  + Bearer challenge-token
  → 200 OK (XUserRolesHeaderFilter accepts spoofed headers in test profile)
```

**Impact (P1 — Pair với [[GAP-705]] gateway issue):**
- Even if gateway filter accepts challenge token (GAP-705 fixed), subscription side still can't translate it to Authentication
- 2FA enrollment + verify flow broken end-to-end via gateway pattern
- Local-only path (spoofed headers) won't work in production

## Context

- Wave 72b shipped TwoFactor* services + controller — designed assuming challenge token bridge filter exists
- Subscription Security architecture (Wave 89+) assumes ALL auth comes via gateway (X-User-Id/X-User-Roles headers) — challenge tokens are exception not modeled
- Need to handle 2 token types: access tokens (via gateway→XUserRolesHeader) + challenge tokens (direct subscription verification)

## Proposed Fix

1. **Add new filter** `ChallengeTokenAuthenticationFilter` in subscription `SecurityConfig`:
   - Order before XUserRolesHeaderFilter
   - Scope: path matchers `/api/v1/auth/2fa/enroll-*` + `/api/v1/auth/2fa/verify` + `/api/v1/auth/2fa/setup`
   - Extract Bearer token → verify HS256 with `jwt.challenge-secret`
   - On success: build `Authentication` with role `ROLE_CHALLENGE` + principal = user_id from claim
2. **Add filter chain** for 2FA paths that requires `ROLE_CHALLENGE` (not standard ROLE_USER/ROLE_OWNER/etc.)
3. **Integration test** `TwoFactorControllerIT.shouldAcceptChallengeTokenBearer()` — Bearer challenge → 200 OK
4. **Cross-fix** with [[GAP-705]] gateway side (must land together)

## Acceptance Criteria

- [ ] New `ChallengeTokenAuthenticationFilter` registered in subscription SecurityConfig
- [ ] 2FA enrollment + verify paths require `ROLE_CHALLENGE` (separate from access role)
- [ ] `POST /api/v1/auth/2fa/enroll-init` direct port 8081 with Bearer challenge (NO spoofed headers) → 200 OK
- [ ] IT `TwoFactorControllerIT.shouldAcceptChallengeTokenBearer()` PASS
- [ ] Live verify per Wave 103 Bucket C pattern, but WITHOUT X-User-Id spoofing
- [ ] Combined with GAP-705 fix: full 2FA flow via gateway port 9000 works end-to-end

## Related

- [[GAP-705]] Gateway JWT filter rejects challenge tokens (sister — must land together)
- [[GAP-516]] 2FA TOTP — current 90% PARTIAL, blocked by this + GAP-705 for production via-gateway flow
- Wave 72b TwoFactor* services + V37 migration
- Wave 89+ Subscription SecurityConfig + XUserRolesHeaderFilter
- Wave 103 audit: `documents/04-quality/audits/local-stack/2026-05-22-wave-103-2fa-totp-walk.md`
