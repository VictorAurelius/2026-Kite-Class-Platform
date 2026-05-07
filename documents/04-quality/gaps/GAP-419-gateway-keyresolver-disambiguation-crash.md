# GAP-419: kite-gateway 3-KeyResolver bean disambiguation crash

**Status:** 🔵 OPEN
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

- [ ] One of `ipKeyResolver` / `tenantKeyResolver` / `apiKeyResolver` annotated `@Primary`
- [ ] `kite-gateway` boots clean to `(healthy)` state in `docker-compose ps`
- [ ] `curl http://localhost:9000/actuator/health` returns 200 OK
- [ ] Existing per-route rate-limit config still uses non-default resolvers via SpEL (verify in `application.yml`)
- [ ] Add unit test: `@SpringBootTest` boots gateway context without bean conflict
- [ ] Self-test: `rm kitehub/.env && bash kitehub/scripts/setup.sh && bash kitehub/scripts/up.sh --profile beta-funnel` → all 9 services healthy within 3 min

## Related

- Likely Wave 35 GAP-388 (security cluster) introduced 3rd resolver
- Surfaced 2026-05-07 Option B' session (PR #951 dev-stack fixes — sister gap)
- Blocks: real-backend E2E (Phase 4.5 `e2e-pre-release.yml` against staging would also crash unless staging gateway has same fix — verify staging.tf-deployed image is up-to-date)
- Could explain why no prior session reached real-backend E2E successfully → all sessions hit this implicitly
