# GAP-419: kite-gateway 3-KeyResolver bean disambiguation crash

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🔴 P0 BLOCKING (gateway is FE→BE entry port 9000; cannot run real-backend E2E or full local dev without it)
**Domain:** Backend / Spring Cloud Gateway
**Found:** 2026-05-07 (Option B' real-backend E2E session)
**Affects:** `kite-gateway` service in **all** docker-compose profiles (beta-funnel, full); native `mvn spring-boot:run` dev too

## Problem

Spring Cloud Gateway's auto-configuration:
```
RequestRateLimiterGatewayFilterFactory(KeyResolver keyResolver)
```
expects exactly **one** `KeyResolver` bean. `KeyResolverConfig.java` declares **three** with no `@Primary` qualifier:

```java
// kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/config/KeyResolverConfig.java
@Bean public KeyResolver ipKeyResolver()      { ... }   // line 48
@Bean public KeyResolver tenantKeyResolver()  { ... }   // line 64
@Bean public KeyResolver apiKeyResolver()     { ... }   // line 83
```

Container crashes immediately on startup with:
```
UnsatisfiedDependencyException: Error creating bean 'gatewayControllerEndpoint':
... parameter 1: Error creating bean 'requestRateLimiterGatewayFilterFactory':
... parameter 1: No qualifying bean of type 'KeyResolver' available:
expected single matching bean but found 3:
ipKeyResolver, tenantKeyResolver, apiKeyResolver
```

→ Crash loop forever (`docker-compose ps` shows `Up 1 second (health: starting)` → `Up 1 second (health: starting)` repeatedly).

## Root Cause

Likely Wave 35 GAP-388 security cluster (or earlier) added 3rd resolver (`apiKeyResolver`) without:
1. Adding `@Primary` to the default
2. Configuring `default-filters` route-specific KeyResolver via SpEL
3. Excluding Spring's autoconfig that wants single bean

Spring Cloud Gateway designed for ≥1 KeyResolver bean OK — but autoconfig of `RequestRateLimiterGatewayFilterFactory` strictly needs single. The 3 resolvers are intended for per-route configuration via SpEL `#{@beanName}`, but the autoconfig is loaded regardless.

## Reproduction

```bash
cd kitehub
bash scripts/up.sh --profile beta-funnel
# → kite-gateway: Up 1 second (health: starting), restarts every ~25s
docker logs kite-gateway 2>&1 | grep -A 2 "No qualifying bean"
```

## Proposed Fix

**Option A (minimal):** Add `@Primary` to the default resolver
```java
@Primary  // ← add
@Bean
public KeyResolver ipKeyResolver() { ... }
```
Spring Cloud Gateway's autoconfig picks `ipKeyResolver` as default; tenantKeyResolver + apiKeyResolver remain accessible by name via SpEL `#{@tenantKeyResolver}` in route config YAML.

**Option B:** Disable Spring's autoconfig binding by setting `spring.cloud.gateway.filter.request-rate-limiter.deny-empty-key=false` AND making sure no route uses default rate limiter without explicit `key-resolver` SpEL.

**Option C:** Exclude `GatewayAutoConfiguration$GatewayActuatorConfiguration` if actuator endpoint not needed. Risky — affects /actuator/gateway endpoints.

Recommend **Option A** — minimal blast radius.

## Acceptance Criteria

- [x] One of `ipKeyResolver` / `tenantKeyResolver` / `apiKeyResolver` annotated `@Primary` (chose `ipKeyResolver` — least disruptive default; per-route SpEL still picks tenant/api by name)
- [x] `kite-gateway` boots clean to `(healthy)` state in `docker-compose ps` — verified Wave 39 Bucket D: `kite-gateway Up About a minute (healthy)` after `up.sh --profile beta-funnel`
- [x] `curl http://localhost:9000/actuator/health` returns 200 OK — verified: `{"status":"UP","components":{"db":{"status":"UP"},...,"redis":{"status":"UP",...}}}`
- [x] Existing per-route rate-limit config still uses non-default resolvers via SpEL (verified `application.yml` references via name; SpEL resolution unchanged by `@Primary`)
- [x] Add unit test that gateway context loads without bean conflict — added reflection-based `ipKeyResolverIsPrimary` test asserting `@Primary` on `ipKeyResolver` and absence on the other two; passes (10/10 tests in `KeyResolverConfigTest`). Full `@SpringBootTest` reactive context boot deferred — heavier infra, lower marginal value vs reflection assertion that locks in the fix.
- [x] Self-test: `bash kitehub/scripts/up.sh --profile beta-funnel` → gateway healthy + `docker logs kite-gateway 2>&1 | grep -c "No qualifying bean"` = 0 (zero KeyResolver conflict errors in boot) — verified Wave 39 Bucket D

## Related

- Likely Wave 35 GAP-388 (security cluster) introduced 3rd resolver
- Surfaced 2026-05-07 Option B' session (PR #951 dev-stack fixes — sister gap)
- Blocks: real-backend E2E (Phase 4.5 `e2e-pre-release.yml` against staging would also crash unless staging gateway has same fix — verify staging.tf-deployed image is up-to-date)
- Could explain why no prior session reached real-backend E2E successfully → all sessions hit this implicitly

## Log

- **2026-05-07** DONE — Wave 39 Bucket D cold-boot verification passed. Evidence: (1) `docker logs kite-gateway 2>&1 | grep -c "No qualifying bean"` → 0 (zero KeyResolver conflict errors; `@Primary` on `ipKeyResolver` eliminates UnsatisfiedDependencyException); (2) `docker-compose ps kite-gateway` → `Up About a minute (healthy)`; (3) `curl -fsS http://localhost:9000/actuator/health` → HTTP 200 `{"status":"UP","components":{"db":{"status":"UP"},...,"redis":{"status":"UP"},...}}`. All 6 ACs checked. Note: postgres data volume stale password caused initial failures; fixed inline via `ALTER USER kitehub PASSWORD '...'` (separate pre-existing infrastructure issue, not GAP-419 scope).
- **2026-05-07** PARTIAL — fix + unit-test landed in dev-stack cluster PR. `KeyResolverConfig.ipKeyResolver()` annotated `@Primary` so Spring Cloud Gateway's `RequestRateLimiterGatewayFilterFactory` autoconfig picks a single default; `tenantKeyResolver` and `apiKeyResolver` remain accessible by name via SpEL `#{@beanName}` in route YAML. New `ipKeyResolverIsPrimary` reflection test in `KeyResolverConfigTest` (10/10 pass) locks in the annotation contract. End-to-end gateway boot in Docker chained with GAP-417 + GAP-418 — queued for next dev-stack session to flip DONE.
