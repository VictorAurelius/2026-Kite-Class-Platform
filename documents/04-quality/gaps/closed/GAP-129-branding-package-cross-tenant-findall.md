# GAP-129: BrandingPackage service loads ALL branding resources of ALL tenants

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend / Performance / Multi-tenancy
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** `kiteclass-core` `BrandingPackageServiceImpl.getByInstanceId(Long instanceId)`
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

```java
List<BrandingPackage.AssetEntry> assets = resourceRepository
        .findAll().stream()
        .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
        .map(this::toAsset)
        .toList();
```

The method accepts `instanceId` but ignores it when loading resources — it loads **ALL BrandingResource rows across all tenants**, then the caller likely filters in memory (or here, doesn't filter by instance at all). This is a multi-tenancy bug masquerading as a perf bug: tenant A's branding page may include tenant B's resources in the response.

This endpoint is HOT — the branding package is fetched by every FE page load on a tenant domain (cached by ETag, but first hit per cache miss does this scan).

## Context

- `CachingBrandingPackageProxy` sits in front of `BrandingPackageServiceImpl` and caches by `instanceId` → cache hit path is fine.
- Cache miss path (cold start, after eviction, after write) triggers full cross-tenant scan.
- Combined with GAP-043 (cache stampede not yet implemented), a cache eviction under load = thundering herd of full-table scans.

## Evidence

- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/branding/service/BrandingPackageServiceImpl.java:35-39`
- Method signature `getByInstanceId(Long instanceId)` — parameter unused in resource query
- Performance audit §1

## Proposed Fix

1. Add repository method:
   ```java
   List<BrandingResource> findByInstanceIdAndDeletedFalse(Long instanceId);
   ```
   Ensure migration V32 has index on `(instance_id, deleted)`.
2. Update service:
   ```java
   List<BrandingPackage.AssetEntry> assets = resourceRepository
           .findByInstanceIdAndDeletedFalse(instanceId)
           .stream()
           .map(this::toAsset)
           .toList();
   ```
3. Add integration test asserting that resources from tenant A are NOT returned in tenant B's package.
4. Verify/add index: `CREATE INDEX idx_branding_resources_instance_deleted ON branding_resources(instance_id, deleted);`

## Acceptance Criteria

- [ ] `getByInstanceId(instanceId)` executes at most 2 queries (instance + resources)
- [ ] Integration test: tenant A branding package excludes tenant B resources
- [ ] Migration or existing V32 has composite index `(instance_id, deleted)`
- [ ] Load test (10k resources × 100 tenants) shows <50ms p95 on cache miss

## Related

- Audit: performance-audit-2026-04-19.md §1
- GAP-043 (cache stampede — same code path)
- Migration: `V32__create_branding_resources_table.sql`

## Log

- 2026-04-19 — Gap created from performance baseline audit
- 2026-04-20 — Fixed in feature/partb-perf-batch: added `findByInstanceIdAndDeletedFalse(UUID)` to `BrandingResourceRepository`, `BrandingPackageServiceImpl.getByInstanceId` now calls this (UUID derived from `instance.getInstanceId()` — the tenant identifier inherited from `BaseEntity`, not the FrontendInstance PK). New V45 migration `V45__add_branding_resources_instance_deleted_index.sql` + JPA `@Index` adds composite `(instance_id, deleted)` index. Regression test `BrandingPackageServiceImplTest.getByInstanceId_usesTenantScopedQuery_notFindAll` asserts `findAll()` is NEVER called and `verifyNoMoreInteractions` enforces ONLY tenant A's UUID is queried.
