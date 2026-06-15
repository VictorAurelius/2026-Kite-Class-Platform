# GAP-1421: @Cacheable course-detail returns HTTP 500 on cache HIT (Redis serializer can't round-trip the entity)

**Status:** 🟢 DONE
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
- [x] **Live: `GET /api/v1/courses/{id}` ×3 (miss → hit → hit) → 200 all** (verified post clean `--no-cache` core rebuild; cached JSON now carries root `@class`).

## Root cause (confirmed) + fix

Two distinct failure modes, both now fixed:

1. **Root `@class` missing on cached record DTOs (the live 500).** `@Cacheable("courses")` caches `CourseResponse` — a Java **record** (final). The serializer used `DefaultTyping.NON_FINAL`, which **skips final types** → the root DTO was written with no `@class` → cache READ deserializes as `Object` → `missing type id property '@class'` → 500 on cache HIT. (The earlier unit test mistakenly serialized the `Course` *entity* — non-final → got root `@class` → masked the bug. Corrected to serialize the actual cached `CourseResponse` record, which reproduced the live error, then went green.) **Fix: `NON_FINAL` → `EVERYTHING`** (types final records too).
2. **Computed-getter unrecognized field.** `FAIL_ON_UNKNOWN_PROPERTIES=false` on the cache mapper (ignores computed getters like `Course#isReadOnly()` on read).

Both scoped to the cache `ObjectMapper` (`CacheConfig.redisValueSerializer()`); REST request binding unaffected. Cache format changed (NON_FINAL→EVERYTHING) → stale entries flushed on deploy.

Verified live: cached JSON root = `{"@class":"...CourseResponse",...}`; `GET /courses/{id}` 200 across miss + repeated hits.

## Related

- Found in: KC-3 academic re-walk 2026-06-15
- Prior awareness: `CourseServiceImpl:136` comment + GAP-792 (cache eviction key tenant-scoping)
- Robustness class: cached JPA entities with computed getters → cache read 500
