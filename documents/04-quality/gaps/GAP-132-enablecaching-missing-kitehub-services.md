# GAP-132: @EnableCaching missing in kitehub-subscription / kitehub-admin / kitehub-platform

**Status:** 🟡 PARTIAL — subscription + admin DONE via Caffeine (Wave 9-E); platform is a shared-entity module (no Spring Boot app, no `@EnableCaching` needed); Redis belt-and-braces still open
**Priority:** 🟠 P1
**Domain:** Backend / Caching / Performance
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** `kitehub-subscription`, `kitehub-admin`, `kitehub-platform`
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

`grep '@EnableCaching|RedisCacheManager'` across `**/main/**/*.java` finds:
- `kiteclass-core/.../CacheConfig.java` — `@EnableCaching` + explicit `RedisCacheManager`
- `kitehub-gateway/.../GatewayBrandingCacheConfig.java` — `@EnableCaching`
- `kitehub-email/.../BrandingCacheConfig.java` — `@EnableCaching`

**Missing** (services that would benefit from caching but have none configured):
- `kitehub-subscription` — highest-traffic service (webhooks, dashboard API, instance CRUD)
- `kitehub-admin` — AnalyticsService.getDashboardStats could be `@Cacheable` but annotation would no-op
- `kitehub-platform` — Instance domain entity service

Additionally, **no service** sets `spring.cache.type: redis` in `application.yml`. Spring falls back to `ConcurrentMapCacheManager` (local JVM heap) unless an explicit `RedisCacheManager` Bean overrides it. This means:
- If a dev adds `@Cacheable` to `kitehub-subscription`, it goes into in-memory heap cache — not shared across pods → staleness.
- kitehub-admin same problem.

## Context

GAP-126 (admin dashboard findAll) proposes caching `DashboardStats`; this gap is a prerequisite — caching must be enabled first.

## Evidence

- Absence of `@EnableCaching` / `RedisCacheManager` in three services (verified by grep)
- No `spring.cache.type: redis` anywhere in `**/application*.yml`
- Performance audit §4

## Proposed Fix

1. Create `CacheConfig.java` in `kitehub-subscription`, `kitehub-admin`, `kitehub-platform` — analogous to `kiteclass-core` pattern, with per-cache TTL differentiation:
   ```java
   .withCacheConfiguration("admin-dashboard", config.entryTtl(Duration.ofMinutes(5)))
   .withCacheConfiguration("instance-summary", config.entryTtl(Duration.ofMinutes(1)))
   .withCacheConfiguration("revenue-report", config.entryTtl(Duration.ofHours(1)))
   ```
2. Add `spring.cache.type: redis` to each `application.yml` as belt-and-braces.
3. Update `backend-standards.md` skill: every service MUST declare `CacheConfig` with `@EnableCaching`, even if no caches defined yet.
4. Audit kiteclass-gateway (not checked in this audit) — add if missing.

## Acceptance Criteria

- [x] `@EnableCaching` present in kitehub-subscription (Wave 9-E — `CacheConfig.java`)
- [x] `@EnableCaching` present in kitehub-admin (Wave 9-E — `CacheConfig.java`)
- [ ] kitehub-platform — **N/A after state-check**: shared-entity module with no `@SpringBootApplication`. Classes imported by subscription/admin inherit their cache managers.
- [x] Caffeine-backed `CacheManager` Bean with per-cache TTL in each service (pattern aligned with existing kitehub-email / kitehub-gateway caches)
- [x] `spring.cache.type: caffeine` in each `application.yml` (prevents silent ConcurrentMapCacheManager fallback)
- [x] Integration test verifies a sentinel cache is hit only once under concurrent calls — covered by `kitehub-email/src/test/.../BrandingCacheStampedeTest` (GAP-043 coupling); per-service smoke tests added (`CacheConfigTest` in each module).
- [ ] Redis belt-and-braces — deferred. Current Caffeine is per-JVM; cross-pod coherence still needs Redis migration when subscription/admin scale horizontally. Tracked as follow-up under this gap.
- [ ] backend-standards.md updated with caching convention — deferred to follow-up doc PR (meta)

## Related

- Audit: performance-audit-2026-04-19.md §4
- GAP-126 (admin dashboard — this gap unblocks `@Cacheable` on `DashboardStats`)
- GAP-043 (cache stampede — closed in same Wave 9-E PR)

## Log

- 2026-04-21 — Wave 9-E shipped Caffeine CacheConfig + `@EnableCaching` for kitehub-subscription and kitehub-admin. Per-service smoke tests pass (3 + 3). `spring.cache.type: caffeine` added to both application.yml. Platform module reclassified N/A (not a runnable Spring Boot app). Remaining AC: Redis migration for cross-pod coherence; backend-standards.md update — both deferred to follow-up.
- 2026-04-19 — Gap created from performance baseline audit
