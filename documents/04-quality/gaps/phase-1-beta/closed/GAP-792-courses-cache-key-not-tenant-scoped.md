# GAP-792 — Courses `@Cacheable` key not tenant-scoped → cross-tenant cache pollution

**Status:** 🔴 OPEN
**Priority:** P0
**Owner:** Backend
**Phase:** phase-1-beta
**Progress:** 0%
**Filed:** 2026-05-28
**Last Updated:** 2026-05-28
**Surfaced by:** Wave A Bucket D `CourseClassCrudOwnerIT.crossTenantIsolation_directGet` test — symmetric tenant fetch surfaced Redis `SerializationException` indicating cross-tenant cache hit.

---

## Problem

`CourseServiceImpl.getCourseById(Long id)` is annotated:

```java
@Cacheable(value = "courses", key = "#id")
public CourseResponse getCourseById(Long id) { ... }
```

The cache key is `#id` only — no tenant scope. When tenant A fetches course 5, Redis stores `courses::5 → tenant-A-payload`. When tenant B requests course 5 (different ID space conceptually, but DBs share global PK sequence), Redis returns tenant A's payload. Two failure modes:

1. **Cross-tenant data leak** — if payload deserializes successfully, tenant B sees tenant A's course data
2. **500 on serialization mismatch** — if payload schema differs, `GenericJackson2JsonRedisSerializer` throws, surfaces as HTTP 500

Empirically observed in Bucket D test: tenant B → tenant A's courseInA returns 500 (cache hit + deserialize crash) instead of expected 404.

Sister bug to GAP-789 (course list native query bypasses tenant filter). Same root cause class: tenant scope not consistently applied across read paths.

## Reproduction

Bucket D `CourseClassCrudOwnerIT.crossTenantIsolation_directGet` — symmetric tenant-B-fetch-tenant-A-course assertion (now commented out in test pending fix). Stack trace:

```
org.springframework.data.redis.serializer.SerializationException: Could not read JSON:
Could not resolve subtype of [simple type, class java.lang.Object]:
missing type id property '@class'
  at GenericJackson2JsonRedisSerializer.deserialize(...:311)
  at RedisCache.deserialize(...)
  ...
```

## Code locations

- Bug: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/service/impl/CourseServiceImpl.java:136` (`@Cacheable(value = "courses", key = "#id")`)
- Sweep target: all `@Cacheable` annotations across kiteclass-core + kitehub-* services

## Cross-flow sweep (per `cross-flow-bug-class-sweep.md` §3)

**Bug class signature:** `@Cacheable` annotation on tenant-scoped entity method with cache key NOT including tenant identifier.

```bash
grep -rn "@Cacheable" kiteclass/ kitehub/ --include="*.java"
```

Expected: every cache annotation on per-tenant data MUST have key including `T(com.kiteclass.core.common.context.TenantContext).getCurrentTenantId()` OR equivalent tenant context expression.

Sweep deferred to fix wave (batch audit all `@Cacheable` sites).

## Proposed Fix

Update cache key to include tenant:

```java
@Cacheable(
    value = "courses",
    key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenantId() + ':' + #id"
)
public CourseResponse getCourseById(Long id) { ... }
```

Apply same fix pattern to `@CacheEvict` annotations (e.g. `updateCourse` line 202) to maintain consistency.

## Acceptance Criteria

- [ ] All `@Cacheable` annotations on tenant-scoped course/class/etc methods include tenant in key
- [ ] All matching `@CacheEvict` annotations updated correspondingly
- [ ] Symmetric tenant isolation re-enabled in `CourseClassCrudOwnerIT.crossTenantIsolation_directGet` (tenant B → tenant A direction)
- [ ] Cross-flow sweep audit: every `@Cacheable` in kiteclass-core + kitehub-* either has tenant key OR documented exempt (truly global cache)
- [ ] Post-fix Testcontainers IT verifies cross-tenant cache isolation under repeated fetches

## Future scope

- ArchUnit / CI script detector: every `@Cacheable` on `*Service.java` MUST include tenant context in key expression. Defer per `incident-to-rule-pipeline.md` §3.1 until ≥2 recurrences post-fix.
- Audit Redis cache namespace prefix per tenant (`tenant-A:courses::5` pattern) as defense-in-depth.

## References

- Surfaced: Bucket D PR (Wave A) `CourseClassCrudOwnerIT.crossTenantIsolation_directGet`
- Sister bug: `documents/04-quality/gaps/phase-1-beta/GAP-789-course-list-native-query-bypasses-tenant-filter.md`
- Rule: `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.1 (A01 Broken Access Control)
- Rule: `.claude/rules/cross-flow-bug-class-sweep.md` (sweep methodology applied)
