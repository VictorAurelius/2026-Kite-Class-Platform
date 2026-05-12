# GAP-494: Lighthouse CI pnpm store path resolution race (admin-merge follow-up)

**Status:** 🟢 DONE 2026-05-12
**Priority:** 🟡 P2 (CI infra flake — admin-merge follow-up obligation per `admin-merge-discipline.md`)
**Domain:** DevOps / CI
**Found:** 2026-05-12 (PR #1220 vercel.json change triggered Lighthouse CI twice; both runs failed at `Setup Node` step with same error)
**Affects:** Every PR touching `kitehub/kitehub-frontend/**` or `kiteclass/kiteclass-frontend/**` — Lighthouse CI workflow currently red 100% of the time

## Problem

`.github/workflows/lighthouse.yml` Setup Node step fails with:

```
[command]/home/runner/setup-pnpm/node_modules/.bin/bin/pnpm store path --silent
/home/runner/setup-pnpm/node_modules/.bin/store/v3
##[error]Some specified paths were not resolved, unable to cache dependencies.
```

Root cause hypothesis: `actions/setup-node@v6` with `cache: pnpm` runs BEFORE pnpm itself is installed → `pnpm store path` returns a directory that doesn't exist yet → caching step fails.

Sequence issue:
1. setup-node tries to compute pnpm store path for caching
2. pnpm not yet installed → `pnpm store path` returns ephemeral path
3. actions/cache step can't resolve that path → workflow fails

## Actual Fix Applied (Wave 66 Bucket 0, 2026-05-12)

**State-check finding:** Gap's proposed fix (re-order setup-node after pnpm-action-setup) was ALREADY in place when investigation started. Workflow line 36-46 (pre-fix) had `pnpm/action-setup@v6` BEFORE `setup-node@v6`. Yet failures persisted on 2026-05-12T17:01 and 2026-05-12T02:34 runs.

**Real root cause:** `setup-node@v6` built-in `cache: 'pnpm'` option calls `pnpm store path` at cache-resolution time. Even with pnpm installed before setup-node, the built-in cache resolution fails with `Some specified paths were not resolved, unable to cache dependencies` — likely due to pnpm store directory not existing yet at the cache-setup phase (chicken-and-egg).

**Control case:** `.github/workflows/frontend-ci.yml` works on the same PRs because it uses **explicit `actions/cache@v5`** with computed `STORE_PATH=$(pnpm store path --silent)` step AFTER pnpm is installed — bypassing setup-node's built-in cache entirely.

**Fix applied:** Mirror the proven frontend-ci.yml pattern in `lighthouse.yml`:
1. Remove `cache: 'pnpm'` + `cache-dependency-path` from setup-node block
2. Move setup-node BEFORE pnpm-action-setup (matches frontend-ci.yml ordering)
3. Add explicit `Get pnpm store directory` step (sets STORE_PATH env)
4. Add explicit `actions/cache@v5` step keyed on `hashFiles('kitehub/kitehub-frontend/pnpm-lock.yaml')`

Filename correction: gap referenced `lighthouse-ci.yml` but actual file is `lighthouse.yml` (3 places fixed in this gap).

## Reproduce

Trigger any workflow_run by editing a file in `kitehub/kitehub-frontend/` or `kiteclass/kiteclass-frontend/` — Lighthouse will fail.

## Acceptance Criteria

- [x] `.github/workflows/lighthouse.yml` Setup Node step no longer fails with `Some specified paths were not resolved` (fix: remove built-in `cache: 'pnpm'`, add explicit `actions/cache@v5` matching frontend-ci.yml pattern)
- [x] Test PR triggers Lighthouse workflow → Setup Node step passes (verification via Wave 66 Bucket 0 fix PR — see Log)
- [x] Workflow runs to completion past Setup Node (whether Lighthouse score passes/fails is separate concern; workflow is advisory `continue-on-error: true`)

## Related

- **Origin:** `admin-merge-discipline.md` §3 override trailer obligation — admin-merged PR #1220 cited `ADMIN_MERGE_FOLLOWUP: GAP-494`. Closing this gap discharges that obligation.
- **Adjacent:** `.github/workflows/lighthouse.yml` (file edited — actual filename, gap originally referenced non-existent `lighthouse-ci.yml`)
- **Adjacent:** `.github/workflows/frontend-ci.yml` — proven control case; fix mirrors this workflow's cache pattern

## Log

- **2026-05-12 (DONE — Wave 66 Bucket 0):** Fix applied to `.github/workflows/lighthouse.yml`. State-check at fix-time revealed gap's proposed fix (re-ordering) was already in place. Real root cause = `setup-node@v6` built-in `cache: 'pnpm'` option fails at cache-resolution time even with pnpm pre-installed. Applied frontend-ci.yml proven pattern: removed built-in cache, added explicit `actions/cache@v5` with `STORE_PATH` env. Test PR verified workflow Setup Node step passes. Discharges `admin-merge-discipline.md` §3 follow-up obligation from PR #1220.
- **2026-05-12:** Filed as admin-merge follow-up for PR #1220 (Vercel ignoreCommand regression fix). Lighthouse failed twice with same error; pre-existing flake unrelated to vercel.json change. Per `admin-merge-discipline.md` §6.1 reviewer-checklist: admin-merge requires follow-up gap with completion-date target.
