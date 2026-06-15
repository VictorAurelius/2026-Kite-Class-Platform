# GAP-1421: @Cacheable course-detail returns HTTP 500 on cache HIT (Redis serializer can't round-trip the entity)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-15 (KC-3 re-walk — `GET /api/v1/courses/{id}` 500 on cache hit)
**Affects:** `kiteclass-core` `CacheConfig` Redis value serializer → every `@Cacheable` cached JPA entity (Course confirmed; all caches benefit from fix)

## Problem

`GET /api/v1/courses/26` returned HTTP 500 on the SECOND call (cache HIT). The `@Cacheable(value="courses")` getById writes the Course to Redis, but the configured `GenericJackson2JsonRedisSerializer` could not read it back:

```
SerializationException: Unrecognized field "readOnly" (class Course), not marked as ignorable
```

Course exposes a computed getter (`isReadOnly()`) with no settable field. Jackson serializes it as a `readOnly` JSON property; on read, `FAIL_ON_UNKNOWN_PROPERTIES` (default true) throws because there is no matching field/setter → cache READ throws → 500. The first call (cache MISS → DB → write → return) succeeds, so the bug only surfaces on the 2nd+ request — invisible until a cached entity is re-read.

(Stale pre-existing Redis entries also surfaced a separate `missing type id '@class'` error from an older serializer format; flushing those keys cleared it. The persistent bug is the computed-getter round-trip above.)

Pre-existing (the `@Cacheable` getById path was never exercised twice in a walk before); `CourseServiceImpl:136` already noted a "SerializationException surfacing as HTTP 500".

## Fix (this PR)

`CacheConfig.redisValueSerializer()` (extracted, package-visible for test): the cache ObjectMapper now disables `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES`. Computed getters round-trip (ignored on read). Scoped to the cache mapper only — REST request binding (which SHOULD reject unknown fields) is unaffected.

## Acceptance Criteria

- [x] Round-trip unit test: a populated Course serializes → deserializes via `CacheConfig.redisValueSerializer()` without throwing (`CacheConfigSerializationTest`) — handles the `readOnly` computed-getter failure mode.
- [x] Fix scoped to cache mapper (no entity / REST change).
- [ ] **Live: `GET /api/v1/courses/{id}` twice (miss → hit) → 200 both — STILL 500 after clean `--no-cache` core rebuild.**

## ⚠️ PARTIAL — live verify FAILED (second, unresolved failure mode)

The `FAIL_ON_UNKNOWN_PROPERTIES=false` fix resolves the **`Unrecognized field "readOnly"`** mode (unit-test reproduced + green). But the **live** course-detail still returns 500 on cache hit with a DIFFERENT error: `missing type id property '@class'` — the cached JSON root object has **no `@class`** (`{"id":26,...}`), while nested values ARE typed (`unitPrice:["java.math.BigDecimal",...]`).

**Unresolved contradiction:** `CacheConfig.redisValueSerializer().serialize(course)` in the unit test writes a root `@class` (`{"@class":"...Course",...}`) and round-trips fine; the live Spring-Cache write of the same entity via the same (verified-deployed) serializer omits the root `@class`. Same config, divergent output — root cause not yet identified by static inspection (needs runtime debug of GenericJackson2JsonRedisSerializer ↔ Spring Cache `RedisCache.put` serialization path; possible GenericJackson2 root-typing behavior difference vs direct `serialize()`).

**Not blocking KC-3 recurrence** — `GET /courses/{id}/classes` (classes list) is uncached + works; the recurrence walk uses the "Lớp học" nav path, not course detail. Course-detail view + the course-detail "Thêm lớp học" button remain 500 until the root-`@class` mode is fixed.

**Next:** runtime-debug why Spring Cache write omits root `@class`; candidate fixes — switch to `GenericJackson2JsonRedisSerializer.builder()` typing, or wrap cached values in a typed holder, or cache a DTO instead of the entity.

## Related

- Found in: KC-3 academic re-walk 2026-06-15
- Prior awareness: `CourseServiceImpl:136` comment + GAP-792 (cache eviction key tenant-scoping)
- Robustness class: cached JPA entities with computed getters → cache read 500
