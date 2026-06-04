# GAP-932: Gateway Spring Cloud TimeLimiter (default 1s) trips fallback on slow-but-successful tenant provisioning — separate from CircuitBreaker

**Status:** 🟡 PARTIAL (config ship Wave flow-kh1 2026-06-04 — empirical re-walk verify pending g2test-an-8+)
**Priority:** 🟠 P1 (user-facing — same false 503 pattern as GAP-928 but rooted in a different resilience4j component; Phase 2 carve was insufficient because TimeLimiter sits ahead of CircuitBreaker)
**Domain:** DevOps
**Found:** 2026-06-04 (Wave flow-kh1 G2 re-walk session, third repro of false 503 on g2test-an-7 at 07:17:54 — BE confirmed status=SIGNED_UP + instance g2test-an-7 TRIAL + DB physically provisioned at 07:17:56.866; gateway WARN "Circuit breaker triggered for auth service" at 07:17:55.879 — 1 second after upstream began, matching Spring Cloud Circuit Breaker's default 1s TimeLimiter timeout)
**Affects:**
- `kitehub/kitehub-gateway/src/main/resources/application.yml` (resilience4j block)
- All write endpoints whose upstream legitimately exceeds 1s (auth-write per GAP-928 Phase 2 carve + instance provisioning + any future tenant-creation chain)

## Problem

GAP-928 Phase 1+2 (commit `5b999e5d` and `f77f58ac`) tuned the resilience4j `circuitbreaker` configs — Phase 1 loosened `slowCallDurationThreshold` to 10s for the auth read route; Phase 2 carved a dedicated `authWriteCircuitBreaker` with `slowCallRateThreshold: 100` so slow-but-successful tenant provisioning never trips the breaker. Verified config landed in JAR. But the re-walk reproduced the false 503: gateway fired `/fallback/auth` 0.88s into a 2s subscription provisioning call, while the upstream eventually returned 201 + DB-confirmed tenant.

Spring Cloud Circuit Breaker auto-pairs **TimeLimiter** with each CircuitBreaker instance. The default `timeoutDuration` is 1 second. When the upstream takes longer than that, the TimeLimiter cancels the request and routes to the fallback BEFORE the CircuitBreaker even sees the call complete. The CircuitBreaker tuning is irrelevant in that path.

Empirical evidence (g2test-an-7 reproduction 2026-06-04 07:17:54-07:17:56):

| Timestamp | Event |
|---|---|
| 07:17:54.872 | BetaAccessService.completeBetaSignup success |
| 07:17:54.982 | Instances table row inserted (g2test-an-7 TRIAL) |
| 07:17:54.99 | DatabaseProvisioningService.provision start |
| 07:17:55.879 | gateway WARN `Circuit breaker triggered for auth service` ← TimeLimiter timeout @ ~880ms |
| 07:17:55.034 | Postgres `CREATE USER kiteclass_636b9b96_user` |
| 07:17:55.658 | Postgres `CREATE DATABASE kiteclass_636b9b96` |
| 07:17:56.866 | DatabaseProvisioningService.provision DONE |

## Root Cause

Default `resilience4j.timelimiter.configs.default.timeoutDuration: 1s` paired with each `authWriteCircuitBreaker` / `instanceCircuitBreaker` instance. Gateway aborts the call at 1s and serves the fallback, regardless of CircuitBreaker config.

## Fix (shipped)

Added a new `resilience4j.timelimiter` block in `application.yml`:

- `authWriteRoute` TimeLimiter config — `timeoutDuration: 60s`, `cancelRunningFuture: false` so the gateway doesn't kill a request that the upstream is going to complete anyway.
- `instanceRoute` TimeLimiter config — `timeoutDuration: 30s`, `cancelRunningFuture: false`.
- `default` TimeLimiter raised to 5s (read paths are fine at 1s but a slightly higher default reduces false positives during local stack warm-up).
- `instances.authWriteCircuitBreaker.baseConfig: authWriteRoute`.
- `instances.instanceCircuitBreaker.baseConfig: instanceRoute`.

## Acceptance Criteria

- [x] Spring Cloud `resilience4j.timelimiter.configs.authWriteRoute` exists and the `authWriteCircuitBreaker` TimeLimiter instance references it
- [x] Spring Cloud `resilience4j.timelimiter.configs.instanceRoute` exists and the `instanceCircuitBreaker` TimeLimiter instance references it
- [x] Default TimeLimiter raised from 1s to 5s
- [ ] Empirical re-walk on local stack: POST /api/v1/auth/beta-signup completes with 201 (NOT 503-while-upstream-committed) for g2test-an-8 or higher
- [ ] Sister probe: POST/PATCH on /api/v1/instances/** does not fire false 503 either

## Related

- Discovered in: Wave flow-kh1 G2 re-walk 2026-06-04 07:17:54 (g2test-an-7, third repro)
- Sister of: GAP-928 Phase 1+2 (CircuitBreaker tuning was correct but insufficient — TimeLimiter is a separate component)
- Per Spring Cloud Circuit Breaker docs (Resilience4j module): `TimeLimiter` is auto-wired alongside `CircuitBreaker` with default timeoutDuration=1s. Configuring CircuitBreaker without TimeLimiter leaves the 1s cap in effect.
- GAP-929 (Phase 3 fallback observability) still open — would have surfaced this faster because the fallback log would have shown upstream returned 201 vs the fallback firing
