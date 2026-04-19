# GAP-132: @EnableCaching missing in kitehub-subscription / kitehub-admin / kitehub-platform

**Status:** 🔵 OPEN
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

- [ ] `@EnableCaching` present in kitehub-subscription, kitehub-admin, kitehub-platform
- [ ] `RedisCacheManager` Bean with per-cache TTL in each
- [ ] `spring.cache.type: redis` in each `application.yml`
- [ ] Integration test (one per service) verifies a sentinel `@Cacheable` method is hit only once under 10 concurrent calls (proves Redis is wired, not local)
- [ ] backend-standards.md updated with caching convention

## Related

- Audit: performance-audit-2026-04-19.md §4
- GAP-126 (admin dashboard — blocked by this gap)
- GAP-043 (cache stampede — related)

## Log

- 2026-04-19 — Gap created from performance baseline audit
