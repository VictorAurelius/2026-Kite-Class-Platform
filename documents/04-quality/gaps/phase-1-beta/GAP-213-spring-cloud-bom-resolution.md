# GAP-213: Spring Cloud BOM fails to resolve on Dependabot Boot bumps

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (blocks Dependabot Spring-touching PRs for 2 services; manual bumps still work)
**Domain:** Backend / Build
**Detected:** 2026-04-24 (Dependabot all-deps group PRs #517 + #518 after config PR #515 merged)
**Related PRs:** #517 (closed), #518 (closed), #515 (config landed)
**Related Docs:**
- `kiteclass/kiteclass-gateway/pom.xml` line 62
- `kitehub/kitehub-gateway/pom.xml` line 36
- `documents/03-planning/plans/plan-dependabot-rollout-2026-04.md`

## Current State (verified 2026-04-24)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Spring Cloud BOM import — kiteclass-gateway | `kiteclass/kiteclass-gateway/pom.xml:38-43` | ✅ declared, imports `spring-cloud-dependencies:${spring-cloud.version}` |
| Spring Cloud BOM import — kitehub-gateway | `kitehub/kitehub-gateway/pom.xml:22-30` | ✅ declared, imports `spring-cloud-dependencies:${spring-cloud.version}` |
| `spring-cloud.version` property | both gateway poms | ✅ set to `2025.0.0` |
| `spring-cloud-starter-gateway` usage (no inline version) | both gateway poms | 🟡 relies on BOM to provide version |
| Main branch CI | `gh run list --branch main --workflow gateway-ci.yml` | ✅ currently green (BOM resolves today) |
| Dependabot all-deps group PRs #517/#518 | CI log | ❌ "dependencies.dependency.version for org.springframework.cloud:spring-cloud-starter-gateway:jar is missing" |

**Grep commands run:**
```bash
grep -l "spring-cloud-dependencies" kiteclass/kiteclass-gateway/pom.xml kitehub/kitehub-gateway/pom.xml
sed -n '30,70p' kiteclass/kiteclass-gateway/pom.xml
sed -n '15,45p' kitehub/kitehub-gateway/pom.xml
gh run view 24884166308 --log-failed | grep 'dependencies.dependency.version'
gh run view 24884191871 --log-failed | grep 'dependencies.dependency.version'
```

## Problem

When the Dependabot `all-deps` group PR bundles `spring-boot-starter-parent` 3.5.13 → 3.5.14 along with other minor/patch updates, Maven reports:

```
dependencies.dependency.version for org.springframework.cloud:spring-cloud-starter-gateway:jar is missing
```

This happens in 2 poms (both gateway services). Main branch currently builds green with the same pom structure — so something about the Dependabot-bumped combination breaks BOM resolution, while main's combination resolves it.

The same issue blocks:
- #517 kiteclass-gateway all-deps (13 updates bundled)
- #518 kitehub all-deps (19 updates bundled, including Boot 3.5.13 → 3.5.14 for the parent that all 6 kitehub-* children inherit from)

## Root Cause (hypothesis — needs investigation)

Candidates:
1. **Boot-BOM / Spring-Cloud-BOM version mismatch.** Boot 3.5.14 may import a different managed Spring Cloud version than what `spring-cloud-dependencies:2025.0.0` provides, causing the import to silently fail to populate the expected artifact:version map.
2. **Transitive POM ordering.** Parent-hierarchy-resolved `dependencyManagement` may be losing the BOM import when Dependabot bumps the parent, since a new parent's own `dependencyManagement` can override child imports.
3. **CI runner network / Maven local-repo state.** The CI jobs that failed ran on freshly-checked-out branches; if Spring Cloud `2025.0.0` BOM wasn't yet cached locally and the runner couldn't pull from Maven Central reliably, the BOM import would fail. (Less likely — main CI pulls fine.)

## Proposed Fix

Investigation-first, then apply the cheapest correct fix:

1. **Reproduce locally** by cherry-picking one of the closed Dependabot diffs onto a fresh branch and running `mvn clean compile -pl kiteclass/kiteclass-gateway`.
2. **If mismatch (hypothesis 1):** bump `spring-cloud.version` explicitly in the same PR when Boot moves (or pin it to a version compatible with the targeted Boot line).
3. **If pom ordering (hypothesis 2):** move the Spring Cloud BOM import to the root `kitehub/pom.xml` `<dependencyManagement>` so children inherit a consistent BOM.
4. **Defensive backstop (always safe):** add an explicit `<version>` property import for each `spring-cloud-starter-*` declaration in gateway poms, driven by `${spring-cloud.version}`. Loses the BOM's transitive management but unblocks Dependabot immediately.

## Acceptance Criteria

- [ ] Re-run of a Dependabot all-deps group PR touching Boot on `kiteclass-gateway` builds green
- [ ] Same for `kitehub` (covering kitehub-gateway's pom)
- [ ] Main branch CI remains green
- [ ] Spring Cloud version strategy documented in `documents/05-guides/` or the service pom header comment

## Related

- Plan: `documents/03-planning/plans/plan-dependabot-rollout-2026-04.md`
- Rule: `.claude/rules/audit-to-gap-pipeline.md` §2.5 (state-check performed above)
- Closed PRs: #517, #518
- Memory: `feedback_dependabot_first_run.md`

## Log

- 2026-06-01 — **Wave meta-8 Bucket B SCOPE-REVISE:** SCOPE-REVISE — PR #523 fix shipped but pom.xml evolved away from "spring-cloud-dependencies" string; gap description outdated relative to current dep tree; reviewer re-validate scope before DONE flip CSV completion_pct adjusted to 60%; gap body Status/AC reflect documented scope BEFORE Wave meta-7 audit — re-read audit artifact for current empirical reality. Source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-b-p1-open-1.md`.

- 2026-04-24 — Initial write-up after #517 + #518 surfaced the issue. 2 PRs closed pending gap resolution. 7 Boot-version IDE warnings persist until fix ships OR manual bump merged.
