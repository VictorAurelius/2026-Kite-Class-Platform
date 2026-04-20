---
name: GAP-147 — KiteHub Admin OpenAPI bean conflict (pre-existing)
description: Pre-existing test failure in kitehub-admin discovered during GAP-131/133 fix work — bean conflict openApiConfig subscription vs admin
type: gap
---

# GAP-147: KiteHub Admin OpenAPI Bean Conflict (Pre-existing)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kitehub-admin)
**Found:** 2026-04-20 (discovered during Part B perf batch PR #375; verified pre-existing on main)
**Affects:** `kitehub-admin/KiteHubAdminApplicationTest.contextLoads`

## Problem

`kitehub-admin/src/test/java/.../KiteHubAdminApplicationTest#contextLoads` fails with:

```
ConflictingBeanDefinitionException: openApiConfig is defined twice —
  com.kitehub.subscription.config.OpenApiConfig
  com.kitehub.admin.config.OpenApiConfig
```

Two `OpenApiConfig` classes live in overlapping Spring component-scan paths. Both are annotated `@Configuration` with bean name `openApiConfig` (default — class name lowercased). Context startup fails.

## Root Cause

Likely that kitehub-admin depends on kitehub-subscription (or shared parent), scan picks up both. Pre-Part B, the test was tolerated because `contextLoads` is trivial — failure was not in CI path OR test was skipped.

## Verification

Reproduced via `git stash` of Part B changes on main before PR #375 merge — same failure on clean main. Confirmed unrelated to Part B changes.

## Proposed Fix

Option 1: Rename one bean via `@Bean(name = ...)` or `@Configuration("adminOpenApiConfig")`.
Option 2: Use explicit `@ComponentScan(basePackages)` to exclude other module's config.
Option 3: Consolidate to shared `OpenApiConfig` in common module with per-app overrides.

## Acceptance Criteria

- [ ] `KiteHubAdminApplicationTest.contextLoads` passes
- [ ] No regression in subscription module OpenAPI docs (/v3/api-docs)
- [ ] CI runs this test (currently might be skipped — verify)

## Related

- Surfaced during: PR #375 (Part B perf batch)
- Module hierarchy issue — may affect other `*Config` classes with default bean names
