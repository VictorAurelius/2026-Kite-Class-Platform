#!/usr/bin/env bash
# check-stale-images.sh — pre-walk static check: Docker image vs source commit time
#
# Origin: Wave flow-kh1 G2 walk session 2026-06-04 — kc-core image was 2 days
# old (missing GAP-866 fix) → wasted rebuild cycle mid-walk. Catching pre-walk
# would have saved ~2-3 min wall-clock + cognitive context switch.
#
# Logic:
#   For each running container matching kitehub-* / kiteclass-* / kite-gateway:
#     - inspect image Created timestamp
#     - map container name → source dir (e.g., kitehub-subscription → kitehub/kitehub-subscription/)
#     - git log -1 --format=%ct -- <dir> = latest source commit time
#     - if image_created < latest_commit → image is STALE (rebuild needed)
#
# Default: exit 0 + WARN-only (advisory).
# --strict: exit 1 when any service stale (CI-blocking mode).
#
# Per pre-walk-static-audit-bundle.md §3 (this rule landing same PR).

set -euo pipefail

STRICT=0
for arg in "$@"; do
  case "$arg" in
    --strict) STRICT=1 ;;
    -h|--help)
      echo "Usage: $0 [--strict]"
      echo "  --strict   exit 1 if any service stale (default: warn only)"
      exit 0
      ;;
  esac
done

# Map container → source dir (extend as services grow)
container_to_dir() {
  case "$1" in
    kitehub-gateway)       echo "kitehub/kitehub-gateway" ;;
    kitehub-platform)      echo "kitehub/kitehub-platform" ;;
    kitehub-subscription)  echo "kitehub/kitehub-subscription" ;;
    kitehub-branding)      echo "kitehub/kitehub-branding" ;;
    kitehub-email)         echo "kitehub/kitehub-email" ;;
    kitehub-admin)         echo "kitehub/kitehub-admin" ;;
    kitehub-frontend)      echo "kitehub/kitehub-frontend" ;;
    kiteclass-core)        echo "kiteclass/kiteclass-core" ;;
    kiteclass-gateway)     echo "kiteclass/kiteclass-gateway" ;;
    kiteclass-frontend)    echo "kiteclass/kiteclass-frontend" ;;
    kite-gateway)          echo "kitehub/kitehub-gateway" ;;
    *)                     echo "" ;;
  esac
}

if ! command -v docker >/dev/null 2>&1; then
  echo "WARN: docker not installed — skipping stale-image check"
  exit 0
fi

# Detect Docker daemon availability — bail cleanly if unreachable
if ! docker info >/dev/null 2>&1; then
  echo "WARN: docker daemon not reachable — skipping stale-image check"
  exit 0
fi

CONTAINERS=$(docker ps --format '{{.Names}}' 2>/dev/null | grep -E '^(kitehub-|kiteclass-|kite-gateway$)' || true)

if [ -z "$CONTAINERS" ]; then
  echo "INFO: no kitehub-*/kiteclass-*/kite-gateway containers running (stack down?)"
  exit 0
fi

STALE_COUNT=0
printf "%-25s %-22s %-22s %-10s\n" "service" "image_created" "latest_commit" "verdict"
printf "%-25s %-22s %-22s %-10s\n" "-------" "-------------" "-------------" "-------"

while IFS= read -r name; do
  [ -z "$name" ] && continue
  DIR=$(container_to_dir "$name")
  if [ -z "$DIR" ]; then
    continue
  fi

  IMG_ISO=$(docker inspect --format '{{.Created}}' "$name" 2>/dev/null || echo "")
  if [ -z "$IMG_ISO" ]; then
    continue
  fi
  IMG_EPOCH=$(date -d "$IMG_ISO" +%s 2>/dev/null || echo 0)
  COMMIT_EPOCH=$(git log -1 --format=%ct -- "$DIR" 2>/dev/null || echo 0)

  if [ "$IMG_EPOCH" -eq 0 ] || [ "$COMMIT_EPOCH" -eq 0 ]; then
    printf "%-25s %-22s %-22s %-10s\n" "$name" "?" "?" "skip"
    continue
  fi

  IMG_HUMAN=$(date -d "@$IMG_EPOCH" '+%Y-%m-%d %H:%M' 2>/dev/null || echo "?")
  COMMIT_HUMAN=$(date -d "@$COMMIT_EPOCH" '+%Y-%m-%d %H:%M' 2>/dev/null || echo "?")

  if [ "$IMG_EPOCH" -lt "$COMMIT_EPOCH" ]; then
    printf "%-25s %-22s %-22s %-10s\n" "$name" "$IMG_HUMAN" "$COMMIT_HUMAN" "STALE"
    STALE_COUNT=$((STALE_COUNT + 1))
  else
    printf "%-25s %-22s %-22s %-10s\n" "$name" "$IMG_HUMAN" "$COMMIT_HUMAN" "fresh"
  fi
done <<<"$CONTAINERS"

echo ""
if [ "$STALE_COUNT" -gt 0 ]; then
  echo "WARN: $STALE_COUNT service(s) have images older than latest source commit."
  echo "      Rebuild via: bash kitehub/scripts/rebuild.sh <service>"
  if [ "$STRICT" -eq 1 ]; then
    exit 1
  fi
else
  echo "OK: all running services are at-or-after latest source commit time"
fi

exit 0
