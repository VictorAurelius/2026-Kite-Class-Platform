# GAP-928: Gateway returns 503 to client while upstream beta-signup actually commits successfully

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (user-facing — client thinks signup failed, may retry → double-submit risk; invitee blocked from completing onboarding because UI shows error despite tenant being live)
**Domain:** DevOps
**Found:** 2026-06-04 (Wave flow-kh1 G2 walk — invitee g2test-an-4 saw 503 on POST /api/v1/auth/beta-signup; DB shows status=SIGNED_UP, instance `g2test-an-4` TRIAL, user OWNER row both created at 04:47:33 — upstream succeeded, gateway falsely reported failure)
**Affects:**
- `kite-gateway` Spring Cloud Gateway circuit-breaker configuration on the `auth` (or whichever route maps `/api/v1/auth/**`) RouteDefinition
- Indirect: every BE call that completes successfully but slower than the circuit-breaker `slowCallDurationThreshold`

## Problem

Empirical Wave flow-kh1 G2 walk 2026-06-04 04:47:33-04:47:34 UTC:

| Layer | Evidence |
|---|---|
| Browser | `POST http://localhost:9000/api/v1/auth/beta-signup → 503 Service Unavailable` |
| Gateway log 04:47:34.617 | `WARN c.k.g.controller.FallbackController - Circuit breaker triggered for auth service` |
| Subscription DB row id=31 | status flipped APPROVED → SIGNED_UP, invite_token nulled — committed successfully at 04:47:33 |
| `instances` table | `g2test-an-4` row inserted at 04:47:33.916 with status=TRIAL |
| `users` table | `g2test-an-4@example.com / OWNER` row inserted at 04:47:33.754 |

The gateway circuit-breaker fired ~1 second after the upstream finished committing. Tenant provisioning (subscription → kiteclass-core → DB writes) appears to exceed the configured `slowCallDurationThreshold`; the request completes server-side but the gateway already returned the fallback 503 to the client. The client believes the signup failed and would normally retry, producing duplicate-tenant errors on the next attempt.

The "auth service" circuit-breaker also has lingering OPEN/HALF_OPEN state from earlier in the session — likely correlated with the `kitehub-subscription` restart (during GAP-925 fix rebuild) and the `kiteclass-core` rebuild (stale image surfaced earlier in the same walk). Even after both upstreams went healthy, the breaker stayed sensitive enough to trip on the next slow call.

## Root Cause

Two contributing factors:

1. **`slowCallDurationThreshold` too aggressive vs. real tenant-provisioning latency.** The beta-signup chain does: validate token → flip request status → provision tenant → publish outbox event → return. With JPA flush + RabbitMQ fast-path publish + cross-module DB writes, the happy-path on a cold-cache local stack can take 2-5s — quite likely above the default 1s slow-call threshold used by Spring Cloud Gateway examples.
2. **Failure-rate window too sensitive after restarts.** During the same session we rebuilt `kitehub-subscription` (GAP-925) and `kiteclass-core` (GAP-866 image staleness). Each restart triggered a wave of 503s from the gateway. The circuit-breaker's sliding window still carries those failures, so a single slow but successful call after restart trips the breaker.

The breaker is doing what it was configured to do — but the config does not match operational reality of a local stack mid-rebuild + a write-heavy first endpoint.

## Proposed Fix

**Phase 1 — immediate** (loosen thresholds for the auth/signup route):
- `slowCallDurationThreshold` → 10s for the auth route (tenant provisioning needs the headroom)
- `slidingWindowSize` → keep at default but `minimumNumberOfCalls` increase from 5 → 20 so a single slow call after restart doesn't trip
- `slowCallRateThreshold` → 80% (allow 1 in 5 slow calls to be OK)

**Phase 2 — robustness** (treat write endpoints differently):
- Carve `/api/v1/auth/beta-signup`, `/api/v1/auth/register` and other "write that creates a tenant/user" endpoints into a dedicated route with a no-slow-call-threshold circuit breaker (only counts hard failures, not slow successes)
- Pair with client-side idempotency key per `GAP-730` (already shipped Wave-10 Bucket A) so retry-on-503 is safe even if the original committed

**Phase 3 — observability**:
- Log the upstream HTTP status + body when the fallback fires, so we can tell "real upstream failure" apart from "slow but successful" without DB forensics

## Acceptance Criteria

- [ ] `kitehub-gateway/src/main/resources/application.yml` (or wherever the route + circuit-breaker config lives) updated with the Phase 1 thresholds documented inline
- [ ] Smoke test on local stack: POST beta-signup with cold cache after a fresh `up.sh` → expect 200/4xx, not a 503-while-DB-committed
- [ ] CloudWatch / local-log probe demonstrates the fallback controller now logs *both* upstream response status and request body fingerprint when it fires
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3 — any other route with circuit breaker on a write endpoint gets the same threshold treatment
- [ ] Wave flow-kh1 G2 walk: invitee can submit beta-signup once and see the same status the BE persisted (no false 503)

## Related

- Discovered in: Wave flow-kh1 G2 walk session 2026-06-04 (g2test-an-4 saw 503, DB shows full success)
- Compounding: GAP-925 (subscription rebuild surfaced the slow-call window), GAP-866 / kc-core stale image (kiteclass-core repeated restarts left the breaker sensitive)
- Sister: GAP-926 (FE generic catch — would have surfaced misleading message had the 503 been treated as failure on client)
- Sister: GAP-927 (BE rollback rotates token — combined with this false 503, an invitee who retries on 503 would hit the rotated-token 404 → permanent lock-out)
- Per `pre-handoff-self-test-completeness.md` §2.2 (b) — "Form submit works end-to-end" requires upstream + gateway agree on the outcome
- Per `design-patterns.md` §3.6 — circuit breaker tuning belongs to operational config, not "set once and forget"
