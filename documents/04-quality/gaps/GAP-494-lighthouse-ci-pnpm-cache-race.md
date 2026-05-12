# GAP-494: Lighthouse CI pnpm store path resolution race (admin-merge follow-up)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (CI infra flake — admin-merge follow-up obligation per `admin-merge-discipline.md`)
**Domain:** DevOps / CI
**Found:** 2026-05-12 (PR #1220 vercel.json change triggered Lighthouse CI twice; both runs failed at `Setup Node` step with same error)
**Affects:** Every PR touching `kitehub/kitehub-frontend/**` or `kiteclass/kiteclass-frontend/**` — Lighthouse CI workflow currently red 100% of the time

## Problem

`.github/workflows/lighthouse-ci.yml` Setup Node step fails with:

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

## Proposed Fix

Add `pnpm/action-setup@v4` BEFORE `actions/setup-node@v6` in the Lighthouse workflow:

```yaml
- name: Setup pnpm
  uses: pnpm/action-setup@v4
  with:
    version: 9  # match pnpm-lock.yaml version

- name: Setup Node
  uses: actions/setup-node@v6
  with:
    node-version: 20
    cache: pnpm
    cache-dependency-path: kitehub/kitehub-frontend/pnpm-lock.yaml
```

This is the documented order in pnpm/action-setup README. The Lighthouse workflow likely was authored before pnpm was project-wide and never re-tested.

## Reproduce

Trigger any workflow_run by editing a file in `kitehub/kitehub-frontend/` or `kiteclass/kiteclass-frontend/` — Lighthouse will fail.

## Acceptance Criteria

- [ ] `.github/workflows/lighthouse-ci.yml` adds `pnpm/action-setup@v4` step BEFORE setup-node
- [ ] Test PR (e.g., a docs comment in `kitehub-frontend/README.md`) → Lighthouse passes Setup Node
- [ ] Workflow runs to completion (whether Lighthouse score passes/fails is separate concern)

## Related

- **Origin:** `admin-merge-discipline.md` §3 override trailer obligation — admin-merged PR #1220 cited `ADMIN_MERGE_FOLLOWUP: GAP-494`. Closing this gap discharges that obligation.
- **Adjacent:** `.github/workflows/lighthouse-ci.yml` (file to edit)
- **Adjacent:** `.github/workflows/frontend-ci.yml` — verify same pattern is correctly ordered there (Frontend CI passes on these PRs)

## Log

- **2026-05-12:** Filed as admin-merge follow-up for PR #1220 (Vercel ignoreCommand regression fix). Lighthouse failed twice with same error; pre-existing flake unrelated to vercel.json change. Per `admin-merge-discipline.md` §6.1 reviewer-checklist: admin-merge requires follow-up gap with completion-date target.
