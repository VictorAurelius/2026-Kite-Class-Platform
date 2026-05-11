# GAP-382: Admin scan drift — build-time detector

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🟠 P1 (meta — force multiplier per `meta-gap-priority.md` §3 Meta-P1)
**Domain:** DevOps / Build / Backend
**Found:** 2026-05-07 (Wave 33 Bucket C closure — 3rd recurrence)
**Affects:** Every PR adding new entity package under `kitehub-subscription/src/main/java/com/kitehub/subscription/**`

## Problem

`KitehubAdminApplication` `@EnableJpaRepositories` + `@EntityScan` lists subscription subpackages explicitly. When a new entity package is added (e.g. `com.kitehub.subscription.beta` in Wave 33 GAP-372), admin context fails at runtime with:

```
UnsatisfiedDependencyException: Error creating bean ... 'BetaAccessRequestRepository' available
```

Subscription module passes its own tests (its application file gets updated). Admin module fails CI because admin's scan list wasn't extended. The mismatch is **silent at compile time, loud at admin module test time**.

**Recurrence count = 3:**
- Wave 25 Bucket A (Consent) — `feedback_admin_scan_packages_after_module_add.md` filed
- Wave 26 Bucket A (Dsar)
- Wave 33 Bucket C (Beta) — coordinator-applied fix on agent branch + force-push

The pattern repeats because:
1. Agents work in single-module worktree, run only that module's `mvn verify`
2. Cross-module bean wiring doesn't surface until admin's `@SpringBootTest` boots
3. Memory-as-enforcement (auto-load `feedback_admin_scan_packages_after_module_add.md`) reduces but doesn't eliminate — agents still miss when scope feels narrow

## Root Cause

`@EnableJpaRepositories` + `@EntityScan` are runtime-evaluated annotations with explicit string package lists. Java compiler can't verify drift; only Spring context boot detects.

3 architectural alternatives:
1. **Drop explicit lists** — use `basePackageClasses = SubscriptionMarker.class` so Spring auto-discovers all sub-packages. Risk: pulls beans admin doesn't want (e.g., scheduler beans). Need separate `@ComponentScan(excludeFilters = ...)` discipline.
2. **Build-time detector** — scan `kitehub-subscription` for `@Entity` / `@Repository` packages, compare with `KitehubAdminApplication` lists, fail build if drift. Mechanical fix.
3. **ArchUnit test** — write architecture test asserting "every `*Repository` interface in `com.kitehub.subscription.**` is covered by admin's `basePackages`".

## Proposed Fix

**Option B (build-time detector) recommended for solo-dev mode** — minimal blast radius, mechanical, no architectural decision needed:

1. Script `scripts/check-admin-scan-coverage.sh` (bash + grep):
   - Find all `@Entity` annotations under `kitehub-subscription/src/main/java/com/kitehub/subscription/**`
   - Extract package names (1 per entity)
   - Find all `@Repository` interfaces in same tree, extract packages
   - Read `KiteHubAdminApplication.java` `@EnableJpaRepositories.basePackages` + `@EntityScan.basePackages` lists
   - Diff: any entity/repo package not covered by admin lists → FAIL with exact missing package
2. Wire into `kitehub-admin-ci.yml` job as new step before `mvn verify`
3. Failure message must cite the recurrence + memory: "Add the new package to KitehubAdminApplication per feedback_admin_scan_packages_after_module_add.md"

**Eligible-for-meta-boost** per `meta-gap-priority.md` §3 — fixes a force multiplier (every future subscription-package PR benefits).

## Acceptance Criteria

- [x] `scripts/check-admin-scan-coverage.sh` shipped — exit 0 on current main, exit 1 with diff message on synthetic missing-package fixture
- [x] CI wiring: new step in `kitehub-ci.yml` `test-admin` job runs the script BEFORE `mvn verify` (fail fast)
- [x] Self-test: temporarily revert `BetaAccessRequest` package from admin scan → script exits 1 + lists `com.kitehub.subscription.beta.repository`
- [x] Memory `feedback_admin_scan_packages_after_module_add.md` updated to point to detector (not just manual reminder)
- [x] Recurrence #4 prevented — next entity-package PR fails CI BEFORE admin module test, not at admin's `@SpringBootTest`

## Out of scope (track separately)

- ArchUnit-based architecture test (Option C) — heavier infra, defer until ArchUnit added to project
- Refactor to `basePackageClasses` (Option A) — requires impact analysis for excluded-bean cases

## Related

- `feedback_admin_scan_packages_after_module_add.md` (memory — to be updated post-detector)
- `feedback_coordinator_ci_fix_pattern.md` (workaround pattern that filled the gap during recurrences)
- `meta-gap-priority.md` §3 Meta-P1 tier (force multiplier)
- `incident-to-rule-pipeline.md` 5-stage applies (this gap = Stage 3 Rule+Enforce response to 3rd recurrence; memory was Stage 1+2 only — insufficient per `feedback_incident_to_rule_pipeline.md`)
- Wave 33 Bucket C PR #898 (3rd recurrence; coordinator-applied admin scan fix)
- Wave 25 / Wave 26 (1st + 2nd recurrences)

## Log

- **2026-05-07:** Filed at Wave 33 closure (PR #900 retro). Triggered by 3rd recurrence of admin scan drift pattern (Beta package after Consent + Dsar precedents). Memory-as-enforcement insufficient — needs build-time detector per `incident-to-rule-pipeline.md` Stage 3.
- **2026-05-07 (closure):** Shipped `scripts/check-admin-scan-coverage.sh` (bash + grep awk + prefix-match coverage) wired into `.github/workflows/kitehub-ci.yml` `test-admin` job before `mvn verify`. Memory `feedback_admin_scan_packages_after_module_add.md` updated to point at detector. Self-test (current main): positive case `✅ Admin scan coverage OK (entity packages: 7, repository packages: 7)` exit 0. Negative case (revert `com.kitehub.subscription.beta.repository` + `com.kitehub.subscription.beta.entity` from `KiteHubAdminApplication`): exit 1 + lists exactly the two missing packages with hint pointing to memory file. ShellCheck clean; YAML valid. Per `gap-done-discipline.md` §2 — every AC verified, no banned phrases, recurrence #4 onwards now caught at CI before admin `@SpringBootTest`.
