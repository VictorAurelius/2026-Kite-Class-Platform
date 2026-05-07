# GAP-418: kitehub-frontend Dockerfile build context mismatch

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (BLOCKING — `up.sh --profile beta-funnel` cold setup fails; CI `docker-build-push.yml` may also fail when frontend image rebuild needed)
**Domain:** DevOps / Docker
**Found:** 2026-05-07 (Option B' real-backend E2E session)
**Affects:** `bash kitehub/scripts/up.sh --profile beta-funnel` (Wave 37 GAP-407 profile), `docker-build-push.yml` workflow (Wave 37 Bucket B GAP-398..402) — when image not pre-built

## Problem

`docker-compose.kitehub.yml` declares `kitehub-frontend` with build context `./kitehub-frontend`, but the Dockerfile inside expects context to be repo root:

```yaml
# docker-compose.kitehub.yml line ~424
kitehub-frontend:
  build:
    context: ./kitehub-frontend         # ← compose context (subdir)
    dockerfile: Dockerfile
```

```dockerfile
# kitehub/kitehub-frontend/Dockerfile line 18
COPY kitehub/kitehub-frontend ./kitehub/kitehub-frontend  # ← expects repo root
```

Result on cold build:
```
failed to compute cache key: failed to calculate checksum of ref ...:
"/kitehub/kitehub-frontend": not found
```

Same crash blocks `bash scripts/build-all.sh` step 4 + any `up.sh` profile that includes frontend.

## Root Cause

Wave 37 Bucket B (GAP-398..402, PR #936) refactored the Dockerfile to use **repo-root context** for multi-arch + ECR push CI workflow. The compose file's `context: ./kitehub-frontend` was not updated to match. Two consumers of same Dockerfile diverged.

`.github/workflows/docker-build-push.yml` likely passes correct context (repo root) so CI image build works; but local `docker-compose build` fails.

## Reproduction

```bash
docker rmi kitehub-frontend:latest 2>/dev/null
cd kitehub
docker-compose -f docker-compose.kitehub.yml build kitehub-frontend
# → "/kitehub/kitehub-frontend: not found"
```

## Proposed Fix

Two paths — pick one:

**Option A (recommended):** Update compose context to repo root, match Dockerfile expectation
```yaml
kitehub-frontend:
  build:
    context: ..                         # repo root
    dockerfile: kitehub/kitehub-frontend/Dockerfile
```

Same fix likely needed for `kiteclass-frontend` block (also has `context: ../kiteclass/kiteclass-frontend` per current compose; verify).

**Option B:** Add 2nd Dockerfile `kitehub-frontend/Dockerfile.local` with subdir-relative paths for compose, keep root-context Dockerfile for CI. **Not recommended** — drift risk.

## Acceptance Criteria

- [ ] `docker-compose.kitehub.yml` `kitehub-frontend` context aligns with Dockerfile
- [ ] `docker rmi kitehub-frontend:latest && docker-compose build kitehub-frontend` succeeds
- [ ] `bash scripts/up.sh --profile beta-funnel` no longer fails on frontend build step
- [ ] `kiteclass-frontend` checked similarly (may have same issue)
- [ ] CI `docker-build-push.yml` still passes (Wave 37 GAP-398..402 unbroken)

## Related

- Wave 37 Bucket B (PR #936) — refactor that introduced root-context Dockerfile
- Wave 37 GAP-407 — beta-funnel profile (depends on this image)
- Surfaced 2026-05-07 Option B' session (PR #951 dev-stack fixes)
- Memory `feedback_kitehub_frontend_msw_missing.md` — separate gap, no overlap
