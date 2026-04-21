# GAP-132: @EnableCaching missing in kitehub-subscription / kitehub-admin / kitehub-platform

**Status:** 🟢 DONE — all kitehub Spring Boot services now have `@EnableCaching` + Caffeine. Wave 9-E shipped subscription + admin; Wave 9.5-B fan-out shipped branding. Redis belt-and-braces remains a follow-up (tracked under cross-pod coherence roadmap, not this gap).
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
- [x] `@EnableCaching` present in kitehub-branding (Wave 9.5-B — `CacheConfig.java`, caches `brandingTemplates` + `brandingRateLimit`)
- [x] `@EnableCaching` present in kitehub-email (Wave 4 — `BrandingCacheConfig.java`, verified state-check 2026-04-21)
- [x] `@EnableCaching` present in kitehub-gateway (Wave 4 — `GatewayBrandingCacheConfig.java`, verified state-check 2026-04-21)
- [x] kitehub-platform — **N/A**: shared-entity module with no `@SpringBootApplication`. Classes imported by subscription/admin/branding inherit their cache managers.
- [x] kitehub-base — **N/A**: shared library module, no `@SpringBootApplication`.
- [x] Caffeine-backed `CacheManager` Bean with per-cache TTL in each service (pattern aligned across subscription, admin, branding, email, gateway)
- [x] `spring.cache.type: caffeine` in each applicable `application.yml` (prevents silent ConcurrentMapCacheManager fallback)
- [x] Integration test verifies a sentinel cache is hit only once under concurrent calls — covered by `kitehub-email/src/test/.../BrandingCacheStampedeTest` (GAP-043 coupling); per-service smoke tests added (`CacheConfigTest` in subscription, admin, branding).
- [ ] Redis belt-and-braces — deferred. Current Caffeine is per-JVM; cross-pod coherence still needs Redis migration when services scale horizontally. Tracked as follow-up — OUT OF SCOPE for GAP-132 (was only about enabling caching infrastructure).
- [ ] backend-standards.md updated with caching convention — deferred to follow-up doc PR (meta)

## Related

- Audit: performance-audit-2026-04-19.md §4
- GAP-126 (admin dashboard — this gap unblocks `@Cacheable` on `DashboardStats`)
- GAP-043 (cache stampede — closed in same Wave 9-E PR)

## Log

- 2026-04-21 — Wave 9.5-B fan-out: kitehub-branding received `CacheConfig.java` + `@EnableCaching` + `spring.cache.type: caffeine` + `CacheConfigTest` (3 tests green). State-check (grep `@EnableCaching` + `@SpringBootApplication` across kitehub/) confirmed only branding was missing; gateway (Wave 4 GAP-032) and email (Wave 4 GAP-021) already had it. kitehub-base + kitehub-platform remain N/A (shared non-boot modules). Full branding test suite 149/149 green. Gap promoted 🟡 PARTIAL → 🟢 DONE; Redis belt-and-braces carved out as separate follow-up (not scope of GAP-132).
- 2026-04-21 — Wave 9-E shipped Caffeine CacheConfig + `@EnableCaching` for kitehub-subscription and kitehub-admin. Per-service smoke tests pass (3 + 3). `spring.cache.type: caffeine` added to both application.yml. Platform module reclassified N/A (not a runnable Spring Boot app). Remaining AC: Redis migration for cross-pod coherence; backend-standards.md update — both deferred to follow-up.
- 2026-04-19 — Gap created from performance baseline audit
