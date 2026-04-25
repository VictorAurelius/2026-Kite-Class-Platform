# GAP-220: `BrandingVersionService.snapshot` JSONB column type mismatch

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 — blocks `BrandingService.updateBranding(...)` end-to-end on Postgres ≥9.2 (jsonb column)
**Domain:** Backend / Persistence
**Found:** 2026-04-25 (Sub-PR 5.6b — surfaced by `BrandingCacheIntegrationTest.updateBranding_evicts_cache_for_tenant` running against TestContainers Postgres)
**Affects:** Every `PUT /api/v1/settings/branding` call that triggers `BrandingVersionService.snapshot()`

## Problem

`BrandingServiceImpl.updateBranding()` calls `brandingVersionService.snapshot(branding, null)` (Wave 4 GAP-033p). The `branding_versions.snapshot_json` column is defined as Postgres `jsonb`, but the JDBC binding sends `String` as `character varying` — Postgres rejects the mismatch:

```
org.springframework.dao.InvalidDataAccessResourceUsageException:
  could not execute statement [ERROR: column "snapshot_json" is of type jsonb
  but expression is of type character varying
  Hint: You will need to rewrite or cast the expression.
  Position: 179]
[insert into branding_versions (active, created_at, ..., snapshot_json, ...) values (..., ?, ...)]
```

Stack trace originates in:
```
HibernateJpaDialect.translateExceptionIfPossible
  → BrandingVersionServiceImpl.snapshot(...)  (Wave 4)
  → BrandingServiceImpl.updateBranding(...)
```

## Why has nobody noticed

Existing `BrandingControllerTest.shouldUpdateBranding` mocks `BrandingService.updateBranding(...)` so the real DB path is never exercised. Wave 4's `BrandingVersionServiceImpl` was added without an end-to-end IT covering the snapshot path. The bug stayed latent for ~3 weeks until Sub-PR 5.6b's `BrandingCacheIntegrationTest` invoked `updateBranding(...)` against TestContainers Postgres — failing immediately.

This means **production tenants attempting to update their branding will hit this 500** when they trigger a snapshot — unless production is using a different Postgres version where the implicit cast works, or `BrandingVersionService` was somehow disabled.

## Root cause (preliminary)

The `BrandingVersion` entity's `snapshotJson` field is mapped as `String` without explicit Hibernate JSON type configuration:

```java
// (preliminary — verify by reading the actual file)
@Column(name = "snapshot_json", columnDefinition = "TEXT")
private String snapshotJson;
```

If `columnDefinition` is `TEXT` but the migration creates `jsonb`, Hibernate sends the wrong JDBC type. Fix paths:

1. **Add Hibernate JSON type:** annotate with `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6.2+) or use a custom `@Type(JsonType.class)` from `hypersistence-utils`.
2. **Migration align:** if business need accepts plain TEXT, alter the column to `text` instead of `jsonb` (loses Postgres JSON operators but resolves the type mismatch).
3. **Application-side serialization:** keep column as `jsonb`, ensure entity uses `@JdbcTypeCode(SqlTypes.JSON)` so Hibernate writes correct JDBC type.

Recommended: option 3 (preserve Postgres JSON capabilities for future query needs).

## Proposed Fix

1. Read `kiteclass-core/src/main/java/com/kiteclass/core/module/settings/entity/BrandingVersion.java` to confirm current annotation.
2. Read the Flyway migration that created `branding_versions.snapshot_json` to confirm jsonb.
3. Apply `@JdbcTypeCode(SqlTypes.JSON)` (or equivalent custom type) on the field.
4. Re-run `BrandingCacheIntegrationTest.updateBranding_evicts_cache_for_tenant` — should pass without the `@MockBean BrandingVersionService` workaround currently in place.
5. Add an IT specifically for `BrandingVersionService.snapshot` writing/reading the JSON round-trip.

## Acceptance Criteria

- [ ] `BrandingVersion.snapshotJson` field has correct Hibernate JSON type annotation
- [ ] `BrandingCacheIntegrationTest.updateBranding_evicts_cache_for_tenant` re-enabled (remove `@MockBean BrandingVersionService` once snapshot path is reliable)
- [ ] New IT for `BrandingVersionServiceImpl.snapshot()` round-trips JSON through the jsonb column
- [ ] No regression in existing `BrandingVersionServiceImpl` callers (rollback flow)

## Workaround (in place 2026-04-25)

`BrandingCacheIntegrationTest` mocks `BrandingVersionService` via `@MockBean` so `updateBranding` skips the snapshot call (`brandingVersionService != null` check returns false because the mock is *replaced* — actually the mock IS non-null but its `snapshot()` method is a no-op by default in Mockito). This bypasses the bug for the cache test specifically; production code path unaffected.

## Related

- Wave 4 origin: GAP-033p — initial branding version snapshot feature
- Discovered via: Sub-PR 5.6b `BrandingCacheIntegrationTest` (`getBranding_isolates_cache_per_tenant` was the first IT to call `updateBranding` end-to-end)
- Hibernate docs: https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html#basic-mapping-jdbc-type
- Cross-references: `feedback_thymeleaf_ognl_pin.md` (similar pattern — Wave-N latent bug surfaced by Wave-N+M test)

## Log

- **2026-04-25:** Filed during Sub-PR 5.6b — surfaced by `BrandingCacheIntegrationTest`. Workaround (`@MockBean`) in place for that test. Production bug should be addressed before any tenant relies on branding-version history.
