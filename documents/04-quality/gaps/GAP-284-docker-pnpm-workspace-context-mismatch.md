# GAP-284: Docker frontend build broken by pnpm workspace dependency (PR #713 fallout)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — main CI red, blocks ECR pushes
**Domain:** DevOps / Frontend
**Found:** 2026-05-04 (post-merge of #713 ADR-024 Phase 1)
**Affects:** `Build and Push KiteClass Docker Images` workflow on `push: main`

## Problem

Run 25299091899 (`Build and Push KiteClass Docker Images` on main, post-merge of PR chain ending #735) failed:

```
#12 4.807  ERR_PNPM_WORKSPACE_PKG_NOT_FOUND  In : "@kite/shared-ui@workspace:*" is in the dependencies but no package named "@kite/shared-ui" is present in the workspace
```

Both `kiteclass-frontend` and `kitehub-frontend` `package.json` declare `"@kite/shared-ui": "workspace:*"` (introduced PR #713, ADR-024 Phase 1). The Docker workflow narrow-contexts to a single package directory:

```yaml
context: kiteclass/kiteclass-${{ matrix.service }}
```

— so inside the build, `pnpm-workspace.yaml`, repo-root `package.json`, and `packages/shared-ui/` are absent. `pnpm install` cannot resolve the workspace protocol.

## Root Cause

PR #713 introduced workspace consumption (ADR-024 commitment) but didn't update the consumer Docker pattern. PR-level `frontend-ci.yml` doesn't exercise the Docker build path — the Docker workflow only runs on `push: main` (per CLAUDE.md solo-dev policy "Kept push: main on docker-build-push.yml"). So the breakage slipped past PR review.

## Proposed Fix

Update the Docker pattern to use repo-root build context + workspace-aware install:

1. `kiteclass/kiteclass-frontend/Dockerfile` — multi-stage with workspace manifest copy + `pnpm install --filter kiteclass-frontend...` + standalone monorepo runner layout
2. `.github/workflows/docker-build-push.yml` — `context: .` + `file: kiteclass/kiteclass-${{ matrix.service }}/Dockerfile` for frontend service (core/gateway unaffected — Maven, no workspace concern)
3. `kiteclass/kiteclass-frontend/next.config.js` — add `outputFileTracingRoot` pointing to repo root so Next standalone traces workspace deps
4. Mirror Dockerfile fix to `kitehub/kitehub-frontend/Dockerfile` (incidental coverage — same broken pattern, no CI yet but will break when added)
5. `kitehub/kitehub-frontend/next.config.js` — same `outputFileTracingRoot`

## Acceptance Criteria

- [ ] Docker workflow `Build Docker Images (Test) (frontend)` passes on the fix PR
- [ ] kiteclass-frontend Dockerfile uses repo-root context, installs only the target package's deps (`--filter kiteclass-frontend...`)
- [ ] `outputFileTracingRoot` set in both frontends' `next.config.js`
- [ ] kitehub-frontend Dockerfile mirrors the fix (incidental coverage)
- [ ] Standalone runner image still serves Next correctly (verified via local Docker build OR CI green sufficient given lack of staging env)
- [ ] No regression on core / gateway Maven Docker builds (unchanged)

## Related

- Triggers from: PR #713 ADR-024 Phase 1 (3ca938e3)
- ADR: `documents/02-architecture/adr/ADR-024-shared-ui-lib-strategy.md`
- Failing run: 25299091899
- Related rule: `feedback_ci_gate_ship_incidental_coverage.md` (mirror fix to kitehub Dockerfile)

## Log

- **2026-05-04** — Filed during CI triage (session start). Hotfix-tier per `post-wave-audit-mandate.md` §8 — main is red. Fix PR opens immediately.
