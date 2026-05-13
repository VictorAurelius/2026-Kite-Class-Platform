# GAP-509: Gateway routing fix — /api/v1/auth + /api/v1/admin

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (Plan 1 BETA blocker)
**Domain:** Backend / DevOps
**Found:** 2026-05-13 (Plan 1 self-test + Bucket E audit-gateway-routes.sh)
**Closed:** 2026-05-13 (PR #1269, Wave 71 Bucket A)
**Affects:** All `/api/v1/auth/**` + `/api/v1/admin/**` flows through gateway

## Problem

Gateway `instance-apis` route `Path=/api/v1/**` → `kiteclass-core:8080` (catch-all). But kitehub-subscription `BetaAccessController` exposes `/api/v1/auth/{register,verify-email,request-beta-access}` + `/api/v1/admin/beta-requests/**`. These never reached the correct backend → Plan 1 Bước 2 failure.

## Fix

Added 2 specific routes BEFORE `instance-apis` in `kitehub-gateway/src/main/resources/application.yml`:
- `kitehub-auth-v1` → `kitehub-subscription:8080` for `/api/v1/auth/**`
- `kitehub-admin-v1` → `kitehub-admin:8080` for `/api/v1/admin/**`

## Acceptance Criteria

- [x] Specific routes precede `instance-apis` catch-all
- [x] mvn compile PASS
- [x] CircuitBreaker beans wired (`authCircuitBreaker`, `adminCircuitBreaker`)
- [x] Post-deploy live verify: `POST /api/v1/auth/request-beta-access` from kitehub.me → 201 + DB row (id=1, PENDING)

## Related

- Parent: Wave 71 pre-launch hardening
- PR: #1269
- Sibling P0: GAP-507 (CORS, done) + GAP-510 (SERVER_PORT) + GAP-511 (profile rename)
- Follow-up: GAP-512 (Wave 71b — 22 remaining routing findings; `admin-v1` route mis-targets kitehub-admin while admin/beta-requests controller lives in kitehub-subscription)

## Log

- **2026-05-13:** Filed retroactively at Wave 71 closure. Plan 1 Bước 2 verified end-to-end (HTTP 201 + DB row inserted via api.kitehub.me from kitehub.me origin).
