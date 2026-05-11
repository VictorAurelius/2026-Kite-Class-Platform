# GAP-238: `cacheConfig` bean-name collision admin↔subscription

**Status:** 🟢 DONE 2026-04-27 (bean collision resolved via @ConditionalOnMissingBean on subscription's CacheManager + admin's manager declares transitive cache names)
**Priority:** 🟠 P1 (potential CI flake; latent risk for full SpringBootTest contexts)
**Domain:** Backend / Spring Configuration
**Detected:** 2026-04-26 (Wave 7-Perf Agent A return finding — pre-existing on main)

## Current State (verified 2026-04-26)

Two `CacheConfig` classes exist with same default bean name `cacheConfig`:
- `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/config/CacheConfig.java`
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/CacheConfig.java`

Agent A reproduced (without their changes via `git stash`):
```
ERROR ... org.springframework.beans.factory.support.BeanDefinitionOverrideException:
Invalid bean definition with name 'cacheConfig'
```

Affected tests:
- `AdminControllerTest`
- `KiteHubAdminApplicationTest`

Why CI didn't fail Wave 7-Perf PRs: each module's test isolated to its own `@SpringBootTest(classes = {SpecificConfig.class})` — full application context not assembled in CI.

## Problem

If a PR adds a test that uses `@SpringBootTest` (no `classes` filter) covering BOTH admin + subscription configs (e.g., integration test scanning both modules), bean collision would fail context startup. Latent regression hazard.

## Proposed Fix

Option A (preferred — minimal):
- Rename one bean explicitly: `@Configuration("adminCacheConfig")` in admin module
- Same for subscription: `@Configuration("subscriptionCacheConfig")`

Option B:
- Move common `CacheConfig` to shared module (kitehub-shared) — single source of truth
- Risk: scope creep, requires shared dep verification

Option A recommended; cheaper.

## Acceptance Criteria

- [ ] Both `CacheConfig` classes have explicit `@Configuration("...")` names
- [ ] Integration test with `@SpringBootTest` scanning both modules' configs starts without `BeanDefinitionOverrideException`
- [ ] No regression in existing kitehub-admin / kitehub-subscription tests

## Related

- Discovered by: Wave 7-Perf Agent A (PR #569 GAP-126)
- Pre-existing on main (Agent A reproduced via stash without their changes)

## Log

- **2026-04-27** — Status 🔵 OPEN → 🟢 DONE. Fix shipped in same-day follow-up PR: (1) added `@ConditionalOnMissingBean(CacheManager.class)` to subscription's `cacheManager()` bean — gates registration when admin context (which already provides one) is loaded; (2) renamed both `@Configuration` classes to `adminCacheConfig` / `subscriptionCacheConfig` (defensive); (3) admin's `CacheManager` now declares transitive cache names `subscriptionByInstance` + `instanceSummary` so `@Cacheable` annotations from subscription module work in admin context; (4) updated `CacheConfigTest` assertion to match new 4-name list. Verification: `BeanDefinitionOverrideException` no longer in admin context startup; admin unit tests 15/15 pass; subscription full suite 355/355 pass. **Out-of-scope follow-up surfaced**: `AdminControllerTest` + `KiteHubAdminApplicationTest` (8 tests) still fail with separate pre-existing issue — `SubscriptionOutboxRepository` not on admin's `@EnableJpaRepositories` scan path. Filed as GAP-240 (admin JPA repository scan completeness). CI didn't catch this before because `kitehub-ci.yml` doesn't include admin module test job — also gap (filed GAP-241 admin CI coverage).
- **2026-04-26** — Filed during Wave 7-Perf consolidation. Agent A flagged as pre-existing (out-of-bounds for them). P1 because latent CI failure mode triggered by future cross-module test additions.
