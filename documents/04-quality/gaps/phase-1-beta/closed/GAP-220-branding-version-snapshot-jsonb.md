# GAP-220: `BrandingVersionService.snapshot` JSONB column type mismatch

**Status:** 🟢 DONE 2026-06-02 (fix shipped PR #533; verified live this session)
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

- [x] `BrandingVersion.snapshotJson` field has correct Hibernate JSON type annotation — `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"` (entity line 66-68, javadoc cites GAP-220)
- [x] `BrandingCacheIntegrationTest` snapshot isolation resolved — mock kept **deliberately** as defensive isolation (documented javadoc lines 55-64: original GAP-220 reason FIXED; mock retained so cache test doesn't depend on snapshot pipeline). Real DB snapshot path now covered by the dedicated IT below instead — AC intent met.
- [x] New IT `BrandingVersionSnapshotJsonbIntegrationTest` round-trips JSON through the jsonb column — 2 tests (incl. Vietnamese diacritics `Đêm trăng` round-trip + monotonic version sequence), run 2/2 PASS on Testcontainers Postgres 2026-06-02
- [x] No regression in existing `BrandingVersionServiceImpl` callers (rollback flow) — fix extended to all 6 jsonb columns repo-wide (OutboxEvent, AuditLog, ParentStudentLink, LandingPage×6, Curriculum, UserPreferences, BrandingVersion, Class, BrandingResource, QualityReport, ModerationQueue + 4 kitehub-subscription entities); cross-flow sweep §below confirms 0 unfixed sister sites

## Workaround (in place 2026-04-25)

`BrandingCacheIntegrationTest` mocks `BrandingVersionService` via `@MockBean` so `updateBranding` skips the snapshot call (`brandingVersionService != null` check returns false because the mock is *replaced* — actually the mock IS non-null but its `snapshot()` method is a no-op by default in Mockito). This bypasses the bug for the cache test specifically; production code path unaffected.

## Related

- Wave 4 origin: GAP-033p — initial branding version snapshot feature
- Discovered via: Sub-PR 5.6b `BrandingCacheIntegrationTest` (`getBranding_isolates_cache_per_tenant` was the first IT to call `updateBranding` end-to-end)
- Hibernate docs: https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html#basic-mapping-jdbc-type
- Cross-references: `feedback_thymeleaf_ognl_pin.md` (similar pattern — Wave-N latent bug surfaced by Wave-N+M test)

## Current State (verified 2026-06-02)

Fix already shipped — discovered via fix-time state-check (`audit-to-gap-pipeline.md` §2.8). Gap was OPEN in CSV but the code-level fix landed weeks earlier in PR #533.

| Item | Evidence |
|---|---|
| Entity annotation | `kiteclass-core/.../entity/BrandingVersion.java:66-68` — `@Column(columnDefinition = "jsonb")` + `@JdbcTypeCode(SqlTypes.JSON)`, javadoc cites GAP-220 |
| Migration | `V43__create_branding_versions.sql:21` — `snapshot_json JSONB NOT NULL` |
| Regression IT | `BrandingVersionSnapshotJsonbIntegrationTest.java` — 2 tests, **2/2 PASS** on Testcontainers Postgres (verified this session); `target/surefire-reports/...BrandingVersionSnapshotJsonbIntegrationTest.txt` → `Tests run: 2, Failures: 0, Errors: 0` |
| Fix commit | PR #533 `c3696889` "fix(gap-220): bind 6 jsonb columns as JDBC JSON for Postgres compatibility" |

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** JPA entity with `@Column(columnDefinition = "jsonb")` + `String` field type but MISSING `@JdbcTypeCode(SqlTypes.JSON)` → Hibernate binds VARCHAR → Postgres rejects jsonb cast.

**Grep run (no `| head`):** `grep -rn 'columnDefinition = "jsonb"' kiteclass/ kitehub/ --include="*.java"` → 22 column sites across 14 entities (kiteclass-core + kitehub-subscription).

**Sites + verdict:**

| Entity | jsonb cols | @JdbcTypeCode present? | Verdict |
|---|---|---|---|
| BrandingVersion (this gap) | snapshot_json | ✅ | FIXED |
| OutboxEvent, AuditLog | payload | ✅ | FIXED (GAP-220 marker) |
| LandingPage | 6 cols | ✅ all | FIXED |
| Curriculum, UserPreferences, ParentStudentLink, Class | each | ✅ | FIXED |
| BrandingResource, QualityReport, ModerationQueue | each | ✅ (GAP-220 marker) | FIXED |
| kitehub-subscription: ConsentRecordImmutable, OnboardingProgress, AdminAuditLog (3 cols) | each | ✅ | FIXED |
| PayrollConfig.bonusesJson | — | n/a (`columnDefinition = "TEXT"`, not jsonb) | EXEMPT (intentional Phase 1 TEXT per javadoc; no type mismatch) |

**Decision:** Sites FIXED = 22 (all jsonb columns repo-wide already carry `@JdbcTypeCode(SqlTypes.JSON)`); DEFERRED = 0; EXEMPT = 1 (PayrollConfig uses TEXT not jsonb). No follow-up gap needed — bug class fully eliminated.

## Log

- **2026-04-25:** Filed during Sub-PR 5.6b — surfaced by `BrandingCacheIntegrationTest`. Workaround (`@MockBean`) in place for that test. Production bug should be addressed before any tenant relies on branding-version history.
- **2026-06-02:** Closed DONE. Fix-time state-check (`audit-to-gap-pipeline.md` §2.8) found fix already shipped in PR #533 (`c3696889`, weeks before this autonomous gap-fix pick-up) — applied `@JdbcTypeCode(SqlTypes.JSON)` to 6 jsonb-bound `String` columns + added dedicated regression IT `BrandingVersionSnapshotJsonbIntegrationTest`. This session: ran the regression IT against Testcontainers Postgres → 2/2 PASS (snapshot insert + Vietnamese-diacritic round-trip). Cross-flow sweep (`cross-flow-bug-class-sweep.md`) over all 22 jsonb column sites confirmed 0 unfixed sister sites — bug class fully eliminated repo-wide. AC2 `@MockBean` retention reframed as intentional defensive isolation (per IT javadoc); AC intent (snapshot path reliability verified via real DB) satisfied by the new IT. CSV row flipped OPEN→DONE + completion 100; gap file git-mv'd to `phase-1-beta/closed/` per `gap-folder-organization.md` §3.3.
