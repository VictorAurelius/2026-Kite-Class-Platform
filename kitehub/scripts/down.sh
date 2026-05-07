#!/bin/bash
# Stop KiteHub stack
# Usage: ./scripts/down.sh [--profile PROFILE] [--volumes|-v]
#
# Profiles must match those used by up.sh (per GAP-407 Wave 37):
#   infra-only, branding-only-no-ai, branding-only, beta-funnel,
#   kc-only, full (default), ai-local, monitoring, backup, build-only

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
            echo "Usage: $0 [--profile PROFILE] [--volumes|-v]" >&2
            exit 1
            ;;
    esac
done

# Default profile = "full" (matches up.sh default per GAP-407).
# Set KITE_COMPOSE_PROFILE env to override.
if [ -z "$PROFILE" ]; then
    PROFILE="${KITE_COMPOSE_PROFILE:-full}"
fi

CMD=(docker compose -f docker-compose.kitehub.yml --profile "$PROFILE" down --remove-orphans)
if [ "$REMOVE_VOLUMES" = true ]; then
    CMD+=(-v)
    echo "Stopping KiteHub (profile: $PROFILE) and removing volumes..."
else
    echo "Stopping KiteHub (profile: $PROFILE, preserving volumes)..."
fi

"${CMD[@]}"
echo "Done."
