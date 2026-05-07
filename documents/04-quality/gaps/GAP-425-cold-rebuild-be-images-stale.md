# GAP-425: Cold rebuild script chỉ rebuild FE images — BE images stale

**Status:** 🔵 OPEN
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

**Option A (recommended):** Add `--rebuild` flag to `up.sh` invoking `build-all.sh` first.

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

Recommend Option A + D combined.

## Acceptance Criteria

- [ ] `kitehub/scripts/up.sh` accepts `--rebuild` flag → triggers `build-all.sh` before docker-compose up
- [ ] Self-test: `rm kitehub/.env && rm kitehub/local volumes && bash setup.sh && bash up.sh --profile beta-funnel --rebuild` → all 10 services healthy within 15 min on first try (cold path with image rebuild)
- [ ] `kitehub/QUICK_START.md` updates first-time setup section reference `--rebuild` flag
- [ ] `documents/05-guides/account-prep/04-kitehub-superadmin-first-login.md` §1 pre-conditions add line "BE images fresh: run `bash scripts/build-all.sh` if first deploy or pull-main since last build"

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
