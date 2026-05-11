# GAP-425: Cold rebuild script chỉ rebuild FE images — BE images stale

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🟡 P2 (dev-friction; first-time-cold-setup blocker — Phase 1 BETA prep)
**Domain:** DevOps / Local dev tooling
**Found:** 2026-05-07 Wave 39 closure session — "visual lần 1" cold rebuild test
**Affects:** Solo dev / new dev machine cold setup; Wave 39 GAP-418/419 fix verification on Bucket D was **warm** path (BE images pre-built). Cold rebuild test post-Wave-39 closure surfaced this.

## Problem

`kitehub/scripts/up.sh --profile beta-funnel` from cold state với BE images stale → `kitehub-subscription` crashes on Flyway V11 syntax error fixed in source ~6 weeks ago (GAP-242 2026-04-27, V11 split function-based UNIQUE INDEX).

```
Migration of schema "public" to version "11 - create email sent log" failed!
SQL State : 42601
Message   : ERROR: syntax error at or near "("
  Position: 452
Location  : db/migration/V11__create_email_sent_log.sql
```

`kitehub-subscription:latest` image age `6 weeks ago` (per `docker images | grep kite`); source V11 file đã fix nhưng image chưa rebuild kể từ đó.

Result: cold rebuild giống first-time setup trên new machine → subscription/admin/gateway restart loop indefinitely.

## Reproduction

```bash
# Stop running stack
cd kitehub && bash scripts/down.sh --profile beta-funnel

# Remove FE images only (typical "cold rebuild" workflow)
docker rmi kitehub-frontend:latest kiteclass-frontend:latest

# Remove volumes for fresh DB
docker volume rm kitehub_kitehub-postgres-data

# Cold up
bash scripts/up.sh --profile beta-funnel
# → kitehub-subscription crash loop với V11 SQL syntax error
# → kitehub-admin chained crash (depends on subscription healthy)
# → kite-gateway boot OK but downstream BE unhealthy
```

## Root Cause

`up.sh` không trigger image rebuild — chỉ `docker compose up -d` against existing images. `setup.sh` cũng không rebuild. Solo dev expectation: "remove volumes + run up" = clean state → fail because BE images tồn tại từ session cũ với stale source.

`scripts/build-all.sh` exists nhưng:
1. Không tự động fire trong `up.sh` flow
2. Không document rõ trong cold-setup runbook
3. Solo dev không biết cần rebuild BE images sau khi pull main + có Flyway migrations mới

## Proposed Fix

**Option A (recommended):** Add `--rebuild` flag to `up.sh` invoking `build-all.sh` first. **PLUS** auto-set `--force-recreate` khi `--rebuild` fires — vì build mới mà không recreate container = compose vẫn dùng container cũ với image SHA cũ (root cause GAP-425 surfaced 2026-05-07: rebuild xong vẫn thấy V11 broken vì container chạy image cũ 6 tuần SHA `9e95bc3d7db8` thay vì `:latest` mới).

```bash
# kitehub/scripts/up.sh
if [[ "$*" == *"--rebuild"* ]]; then
  echo "Rebuilding all images first..."
  bash "$(dirname "$0")/build-all.sh"
fi
docker compose ... up -d
```

Cold setup workflow:
```bash
bash scripts/setup.sh
bash scripts/up.sh --profile beta-funnel --rebuild  # ← first-time
```

Subsequent runs (warm):
```bash
bash scripts/up.sh --profile beta-funnel  # uses cached images
```

**Option B:** Auto-detect stale image — `up.sh` checks `docker image inspect kite-gateway:latest --format='{{.Created}}'` vs latest commit on `kitehub-gateway/` — if image older than commit, run `build-all.sh`. Heavier logic, more correct.

**Option C (simplest):** Add `--no-cache` reminder in `up.sh` first-time-detection (no `.env` exists → recommend `build-all.sh` first).

**Option D (docs-only):** Update `kitehub/QUICK_START.md` + `documents/05-guides/account-prep/04-kitehub-superadmin-first-login.md` §pre-conditions để mention `build-all.sh` BEFORE first `up.sh`.

**Option E (production parity — added 2026-05-07):** Add `--pull-from-ecr TAG` flag để pull pre-built CI images từ ECR thay vì build local. Use case: Phase 4 staging E2E gate cần image production-parity (bit-for-bit identical với prod deploy). Local rebuild ≠ ECR build (cache khác, JDK khác, multi-arch khác). Per khuyến nghị "khi nào local vs CI":

| Use case | Cách | Lý do |
|----------|------|-------|
| Iteration code dev | Local `--rebuild` | Cycle 2-5 min, không cần network |
| Visual smoke local | Local `--rebuild --force-recreate` | Catch source-vs-image drift (GAP-425) |
| Phase 4 staging gate | `--pull-from-ecr v0.9.0-staging.X` | Production parity, Trivy/SBOM/Cosign verified |
| Phase 7 prod | CI tag-driven only | Locally-built BANNED in prod |

Recommend **Option A + Option E + Option D combined** — A cho dev iteration, E cho staging-parity, D cho onboarding clarity.

## Acceptance Criteria

- [x] `kitehub/scripts/up.sh` accepts `--rebuild` flag → triggers `build-all.sh` before docker-compose up
- [x] `kitehub/scripts/up.sh` accepts `--force-recreate` flag (auto-on khi --rebuild)
- [x] `kitehub/scripts/up.sh` accepts `--pull-from-ecr TAG` flag — pull CI image từ ECR (Option E)
- [x] Self-test inline: setup.sh sau khi gen .env verify ENCRYPTION_MASTER_KEY decode = 32 bytes (GAP-426 paired)
- [ ] `kitehub/QUICK_START.md` updates first-time setup section reference `--rebuild --force-recreate` (defer — out-of-scope this PR)
- [ ] `documents/05-guides/account-prep/04-kitehub-superadmin-first-login.md` §1 pre-conditions cross-link (defer — Phase 4 release-1-deploy-runbook.md update covers more critical path)
- [x] `release-1-deploy-runbook.md` Phase 4 — local-vs-CI guidance added

## Related

- GAP-242 (2026-04-27) — original V11 syntax fix that doesn't propagate without rebuild
- GAP-417/418/419/420 (Wave 39) — sister cold-setup blockers; GAP-418/419 verified Wave 39 Bucket D in WARM path (BE images pre-built from earlier session)
- `kitehub/scripts/build-all.sh` (already exists)
- `kitehub/QUICK_START.md` (target doc update)
- Phase 1 BETA `documents/05-guides/account-prep/04-kitehub-superadmin-first-login.md` §pre-conditions

## Estimated effort

~30 min (Option A flag implementation + 2-doc cross-link update).

## Log

- **2026-05-07** Filed during Wave 39 closure session "visual lần 1" cold-rebuild test. User cold-rebuilt FE images + volumes; BE images stale → kitehub-subscription crash loop on Flyway V11. Wave 39 Bucket D verified WARM path successfully — cold path different surface, deserves dedicated gap. Recommended Option A + D combined fix (~30 min, P2 per dev-friction tier).
