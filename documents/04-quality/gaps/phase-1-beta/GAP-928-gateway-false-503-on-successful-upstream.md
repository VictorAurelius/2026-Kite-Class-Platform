# GAP-928: Gateway returns 503 to client while upstream beta-signup actually commits successfully

**Status:** 🟡 PARTIAL — Phase 1 shipped Wave flow-kh1 2026-06-04 (auth route circuit-breaker threshold tuning); Phase 2 (carve write routes) + Phase 3 (fallback logging) deferred
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

- [x] `kitehub-gateway/src/main/resources/application.yml` (or wherever the route + circuit-breaker config lives) updated with the Phase 1 thresholds documented inline — shipped Wave flow-kh1 (this PR): new `authRoute` resilience4j config (slidingWindow=10, minimumNumberOfCalls=20, slowCallDurationThreshold=10s, slowCallRateThreshold=80, failureRateThreshold=50); `authCircuitBreaker` baseConfig flipped from `default` → `authRoute`
- [ ] Smoke test on local stack: POST beta-signup with cold cache after a fresh `up.sh` → expect 200/4xx, not a 503-while-DB-committed — needs user re-walk on local stack after rebuild
- [ ] CloudWatch / local-log probe demonstrates the fallback controller now logs *both* upstream response status and request body fingerprint when it fires — Phase 3 deferred (FallbackController logging upgrade)
- [x] Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3 — any other route with circuit breaker on a write endpoint gets the same threshold treatment — sweep done (see §Cross-flow sweep below); other write-endpoint routes intentionally DEFERRED to Phase 2 carve-out (the right fix for them is dedicated no-slow-call breaker, not threshold-loosening of `default` config which other read paths share)
- [ ] Wave flow-kh1 G2 walk: invitee can submit beta-signup once and see the same status the BE persisted (no false 503) — needs G2 re-walk after gateway rebuild

## Related

- Discovered in: Wave flow-kh1 G2 walk session 2026-06-04 (g2test-an-4 saw 503, DB shows full success)
- Compounding: GAP-925 (subscription rebuild surfaced the slow-call window), GAP-866 / kc-core stale image (kiteclass-core repeated restarts left the breaker sensitive)
- Sister: GAP-926 (FE generic catch — would have surfaced misleading message had the 503 been treated as failure on client)
- Sister: GAP-927 (BE rollback rotates token — combined with this false 503, an invitee who retries on 503 would hit the rotated-token 404 → permanent lock-out)
- Per `pre-handoff-self-test-completeness.md` §2.2 (b) — "Form submit works end-to-end" requires upstream + gateway agree on the outcome
- Per `design-patterns.md` §3.6 — circuit breaker tuning belongs to operational config, not "set once and forget"

## Cross-flow sweep (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** Spring Cloud Gateway route filter `CircuitBreaker` that uses a shared `default` resilience4j config on a route forwarding a write-endpoint (POST/PUT/PATCH) whose upstream can legitimately take 2-5s on cold start (tenant provisioning, multi-service DB writes, outbox publish). The `default` config's tight `slowCallDurationThreshold` + 5-call sliding window classes a slow-but-successful upstream as a failure and trips the breaker → fallback 503 to the client while the upstream actually committed.

**Grep command run:**

```bash
grep -n -E "name: instancesCircuitBreaker|name: subscriptionCircuitBreaker|name: paymentCircuitBreaker|name: brandingCircuitBreaker|name: adminCircuitBreaker" \
  kitehub/kitehub-gateway/src/main/resources/application.yml
```

**Sites found + verdict** (sample of write-endpoint routes sharing a `baseConfig: default` circuit breaker on Spring Cloud Gateway):

| # | Site | Verdict | Reason |
|---|---|---|---|
| 1 | `auth-register` (POST `/api/auth/register`) → `authCircuitBreaker` | **FIXED** | Same breaker as beta-signup; auth catch-all `/api/auth/**` route also uses `authCircuitBreaker`. Phase 1 threshold tuning covers all `/api/auth/**` write paths. |
| 2 | `auth-login` / `auth-refresh` / `auth-verify-email` / `auth-resend-verification` / `auth-password-reset-request` → `authCircuitBreaker` | **FIXED** | Same breaker — Phase 1 fix applies. Auth catch-all `/api/auth/**` also routed via `authCircuitBreaker`. |
| 3 | `platform-instances` + `kitehub-instance-domain-verify` (POST/PATCH `/api/platform/instances/**` + `/api/instances/**`) → `instancesCircuitBreaker` (baseConfig: default) | **DEFER** to Phase 2 | Tenant instance creation can be 2-5s same class as beta-signup, BUT `instancesCircuitBreaker` is shared with admin force-convert + rollback-migration routes that have different timing profiles. Right fix is Phase 2 dedicated write-route carve-out, not loosening shared `default` (could mask real failures on read paths). |
| 4 | `platform-admin-instances-force-convert-subscription` + `-rollback-migration-subscription` (POST admin-only) → `instancesCircuitBreaker` | **EXEMPT** for Phase 1 | Admin-triggered, low traffic, not on the beta-signup path. Phase 2 will move these to a dedicated breaker as well. |
| 5 | `platform-subscription` + `platform-payment` + `platform-config` → `subscriptionCircuitBreaker` / `paymentCircuitBreaker` (baseConfig: default) | **EXEMPT** for Phase 1 | Mostly GET/read routes; no reported false 503 in walk evidence. Phase 2 evaluation: carve out write subscriptions/payments if recurrence ≥1. |
| 6 | `branding` / `admin-*` routes → `brandingCircuitBreaker` / `adminCircuitBreaker` (baseConfig: default) | **EXEMPT** for Phase 1 | Not on the G2 walk path; no recurrence to date. |

**Decision:**

- Sites FIXED this PR: all `/api/auth/**` routes (5+ routes share `authCircuitBreaker`) via single config change to `authCircuitBreaker.baseConfig: authRoute`
- Sites DEFERRED to Phase 2: `instancesCircuitBreaker` write paths (3 routes) — Phase 2 will carve a dedicated `instanceWriteRoute` baseConfig with no slow-call counting; tracked inline in this gap §Proposed Fix Phase 2 + §Open Items
- Sites EXEMPT (Phase 1 scope): read-heavy or low-traffic admin routes that did not exhibit the symptom; revisit if any future RST walk surfaces a false 503 on a non-auth route

This is honest scope-limiting per Phase 1 (loosen thresholds on the auth route only) — generalizing the threshold treatment to every breaker via the `default` config would over-broaden, hiding real slow-call signals on read paths.

## Log

- **2026-06-04** Phase 1 shipped — Wave flow-kh1 (this PR, agent-gap-928 worktree).
  - **Config change**: added new `resilience4j.circuitbreaker.configs.authRoute` config with `slowCallDurationThreshold: 10s`, `minimumNumberOfCalls: 20`, `slowCallRateThreshold: 80`, `failureRateThreshold: 50`, `slidingWindowSize: 10`, `waitDurationInOpenState: 10s`, `automaticTransitionFromOpenToHalfOpenEnabled: true`. Flipped `authCircuitBreaker.baseConfig` from `default` → `authRoute`. Inline comments cite GAP-928 + walk-evidence summary (g2test-an-4 04:47:33-34 UTC).
  - **Sweep findings**: other write-endpoint routes (`instancesCircuitBreaker`) carry the same bug class but are intentionally deferred to Phase 2 — the right fix is per-route carve-out, not loosening the shared `default` config which read paths also depend on. See §Cross-flow sweep above.
  - **Build evidence**: YAML parses (`python3 -c "import yaml; yaml.safe_load(...)"` clean); maven module name confirmed via grep on `kitehub/pom.xml`. Container rebuild + smoke walk = G2 re-test scope (user to verify the false-503 disappears on next walk).
  - **Phase 2 deferred**: carve dedicated route(s) for `/api/v1/auth/beta-signup`, `/api/v1/auth/register`, `/api/v1/instances/**` and other tenant-provisioning write endpoints with a no-slow-call breaker that only counts hard failures (not slow successes). Pair with client idempotency-key per GAP-730 so retry on legitimate failure is safe.
  - **Phase 3 deferred**: extend `FallbackController.fallbackAuth` to log upstream HTTP status + response body fingerprint (per `logs-format-standard.md` §3.1 PII-safe shape — no token / no email body content) when the fallback fires, so "false 503" is distinguishable from "real upstream failure" without DB forensics.
