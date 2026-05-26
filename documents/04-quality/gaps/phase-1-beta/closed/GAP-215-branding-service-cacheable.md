# GAP-215: `BrandingService.getBranding()` not `@Cacheable` — DB hit per document render

**Status:** 🟢 DONE 100%
**Priority:** 🔴 P0 — blocks Sub-PR 5.6b per Wave 5 plan §4 "5.6a P0 → block 5.6b" policy
**Domain:** Backend / Performance
**Found:** 2026-04-25 (Wave 5 audit suite — performance audit `documents/04-quality/audits/performance/performance-audit-2026-04-25-wave5.md` finding P0-1)
**Affects:** Every `POST /api/v1/documents/{format}/{preview|download}` request

## Problem

`DocumentGenerationController.render()` calls `brandingService.getBranding()` per request. The implementation has no `@Cacheable`:

```java
// kiteclass-core/src/main/java/com/kiteclass/core/module/settings/service/BrandingServiceImpl.java:51-60
@Override
@Transactional(readOnly = true)  // ← no @Cacheable
public BrandingResponse getBranding() {
    UUID instanceId = TenantContext.getCurrentTenant();
    Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
            .orElseGet(() -> createDefaultBranding(instanceId));
    return brandingMapper.toResponse(branding);
}
```

Every document render hits PostgreSQL. Under 10+ concurrent renders for the same tenant, this becomes a hot read path — projected -300ms per render savings if cached.

There IS a `CachingBrandingPackageProxy` in the codebase, but it wraps a different service (`BrandingPackageService.getByInstanceId(Long)` — Wave 3 frontend instance package), not the `settings.BrandingService` that the document path uses.

## Root Cause

Wave 5 wired the document HTTP path to `BrandingService.getBranding()` (the right call given the multi-tenant + theme-source architecture) but inherited an uncached implementation. The Wave 3 caching effort focused on a different service signature.

## Proposed Fix

Add `@Cacheable` to `BrandingServiceImpl.getBranding()`:

```java
@Override
@Transactional(readOnly = true)
@Cacheable(value = "branding-by-tenant", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant()", sync = true)
public BrandingResponse getBranding() { ... }
```

Cache name: `branding-by-tenant` (separate from `branding-package` for clarity). Configure TTL ~5 min in `CacheConfig` (short enough that branding changes propagate without explicit eviction; long enough to absorb burst).

Eviction: invalidate on `BrandingService.updateBranding()` / `uploadLogo()` / `uploadFavicon()` via `@CacheEvict("branding-by-tenant", key=...)`. Cross-reference Wave 3 outbox event `branding.updated` — could also drive eviction via listener.

## Acceptance Criteria

- [ ] `@Cacheable` applied to `BrandingServiceImpl.getBranding()` with `sync = true`
- [ ] `@CacheEvict` on `updateBranding()` / `uploadLogo()` / `uploadFavicon()`
- [ ] `CacheConfig` declares the new cache name with appropriate TTL
- [ ] Test: integration test verifies second `getBranding()` call within TTL hits cache (use `@SpyBean` on repository, assert `findByInstanceIdAndDeletedFalse` invoked exactly once per cache window)
- [ ] Test: `updateBranding()` evicts cache (subsequent `getBranding()` returns updated values)
- [ ] No regression in `DocumentGenerationControllerTest`

## Related

- Audit: `documents/04-quality/audits/performance/performance-audit-2026-04-25-wave5.md`
- Wave plan: `documents/03-planning/waves/wave-05-document-generation.md` §4 Sub-PR 5.6a
- GAP-214: parent audit suite gap (closes when this + the other 3 P0s ship)
- GAP-043: Wave 3 cache stampede protection — pattern to follow

## Log

- **2026-04-25:** Filed from Wave 5 audit suite (performance audit finding P0-1). Blocks Sub-PR 5.6b.

- **2026-05-26 (Wave br-7 Bucket A inline verify — 5/5 IT PASS BrandingCacheIntegrationTest; code shipped prior wave (Sub-PR 5.6 era) closure):** Flipped DONE 100% — . CSV row updated + file moved to phase-1-beta/closed/ per `gap-done-discipline.md` §2.
