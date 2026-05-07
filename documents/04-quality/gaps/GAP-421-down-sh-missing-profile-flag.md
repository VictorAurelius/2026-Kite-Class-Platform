# GAP-421: kitehub/scripts/down.sh missing --profile flag

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX bug; workaround exists; doesn't block any deploy)
**Domain:** DevOps / Local dev tooling
**Found:** 2026-05-07 (WSL kite-dev stack-up validation session)
**Affects:** Any dev who runs `bash scripts/up.sh --profile <X>` followed by `bash scripts/down.sh` — containers from non-default profile remain running

---

## Problem

`kitehub/scripts/down.sh` invokes `docker-compose -f docker-compose.kitehub.yml down` WITHOUT a `--profile` flag. Docker Compose down with no profile only stops services from the default (no-profile) profile group — services started under a specific profile (e.g. `infra-only`, `branding-only`, `kc-only` per GAP-407 Wave 37) remain running.

**Symptom observed 2026-05-07:**
```bash
bash scripts/up.sh --profile infra-only   # 5 containers up healthy
bash scripts/down.sh                       # script says "Stopping KiteHub... Done."
docker ps -a --filter "name=kite-"        # 5 containers STILL Up (healthy)
```

Workaround: `docker compose -f docker-compose.kitehub.yml --profile <profile> down --remove-orphans`

## State-Check (2026-05-07)

```bash
$ cat kitehub/scripts/down.sh
#!/bin/bash
set -e
cd "$(dirname "$0")/.."
if [ "$1" = "--volumes" ] || [ "$1" = "-v" ]; then
    docker-compose -f docker-compose.kitehub.yml down -v
else
    docker-compose -f docker-compose.kitehub.yml down
fi
```

→ No `--profile` handling at all. Confirmed bug (not WSL-move artifact).

## Root Cause

`up.sh` shipped `--profile` parameter parsing in Wave 37 (GAP-407 Compose profiles). `down.sh` was not updated symmetrically. Asymmetry between up + down = silent leak.

## Proposed Fix

Match `up.sh` argument-parsing pattern in `down.sh`:

```bash
#!/bin/bash
# Stop KiteHub stack
# Usage: ./scripts/down.sh [--profile PROFILE] [--volumes|-v]

set -e
cd "$(dirname "$0")/.."

PROFILE=""
REMOVE_VOLUMES=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --volumes|-v)
            REMOVE_VOLUMES=true
            shift
            ;;
        *)
            echo "Unknown arg: $1" >&2
            exit 1
            ;;
    esac
done

if [ -z "$PROFILE" ]; then
    PROFILE="${KITE_COMPOSE_PROFILE:-full}"
fi

CMD="docker compose -f docker-compose.kitehub.yml --profile $PROFILE down --remove-orphans"
[ "$REMOVE_VOLUMES" = true ] && CMD="$CMD -v"

echo "Stopping KiteHub (profile: $PROFILE)..."
$CMD
echo "Done."
```

Also update inline help via `kitehub/scripts/help.sh` to mention `--profile` arg.

## Acceptance Criteria

- [ ] `bash scripts/up.sh --profile infra-only` followed by `bash scripts/down.sh --profile infra-only` removes all 5 infra containers
- [ ] `bash scripts/down.sh` with no profile defaults to `full` (matches `up.sh` default)
- [ ] `bash scripts/down.sh --profile infra-only --volumes` also removes named volumes
- [ ] `bash scripts/help.sh` mentions `--profile` arg for `down.sh`
- [ ] `docker-compose` → `docker compose` modernization (V1 deprecated; aligns with rest of new scripts)

## Related

- GAP-407 (Compose profiles introduction — root cause source)
- `kitehub/scripts/up.sh` (the symmetric pair that already supports `--profile`)
- `.claude/rules/agent-action-bias.md` v1.0.0 (filed same session)

## Log

- **2026-05-07:** Filed during WSL kite-dev stack-up validation. Confirmed via direct `docker ps` after `down.sh`. ~10-line bash fix.
