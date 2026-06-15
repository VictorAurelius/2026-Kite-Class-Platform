# GAP-1422: default landing page created without template_type → NOT NULL violation → 500 on every request

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-15 (KC-3 re-walk — `GET /api/v1/courses/{id}/classes` 500)
**Affects:** `kiteclass-core` `LandingPageServiceImpl.getOrCreateDefault` → any tenant with no `landing_pages` row

## Problem

`GET /api/v1/courses/26/classes` returned 500. Root cause is unrelated to that endpoint: `TenantAwareDataSourceInterceptor.setTenantGucIfNeeded` calls `LandingPageServiceImpl.getLandingPage` on the request path, which calls `getOrCreateDefault`. For a tenant with no `landing_pages` row it builds `new LandingPage()`, sets `instanceId` (+ optional branding fields) and `save()`s it — but never sets `templateType`.

`landing_pages.template_type` is **NOT NULL** in the DB (entity field is a plain nullable `String` with no Java default; the entity javadoc even claims "Default values are set in LandingPage entity fields" — false). Hibernate INSERTs an explicit `NULL` → `DataIntegrityViolationException` → 500.

Severity: the interceptor runs **per request**, so any tenant missing a landing page gets 500 on **every** request (full outage for that tenant), not just course endpoints. Surfaced when the demo tenant `sky-education-074901` (no landing page row) was hit after its `landingPages` cache was flushed.

## Fix (this PR)

`getOrCreateDefault` sets `newLandingPage.setTemplateType("organization")` (the center/org default; existing DB values are `organization` / `personal`) before save.

## Acceptance Criteria

- [x] Unit test: `getOrCreateDefault` (via `getLandingPage`) saves a LandingPage with non-blank `templateType` (`LandingPageServiceTest#getLandingPage_notExists_createdDefaultHasNonNullTemplateType_GAP1422`).
- [x] Live: after core rebuild, a tenant with no landing page → `GET` succeeds (getOrCreateDefault creates with template_type, no 500). Demo tenant unblocked immediately via a seed row (workaround); code fix covers all tenants.

## Related

- Found in: KC-3 academic re-walk 2026-06-15 (exposed by flushing the `landingPages` cache during GAP-1421 work)
- Sibling marketing landing default-value gaps: GAP-809 / GAP-1083 (branding inheritance into default landing page)
