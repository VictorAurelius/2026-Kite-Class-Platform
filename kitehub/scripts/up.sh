#!/bin/bash
# Start KiteHub stack
# Usage: ./scripts/up.sh [--profile PROFILE] [service...]
#
# Profiles (per GAP-407 Wave 37):
#   infra-only            ~1.5 GB  PG + Redis + RMQ + MinIO + MailHog
#   branding-only-no-ai   ~5 GB    infra + KH-branding + KH-frontend + KH-gateway (template path)
#   branding-only         ~12 GB   above + Ollama (AI generation path)
#   beta-funnel           ~9 GB    infra + KH-subscription/email/admin/gateway/frontend
#   kc-only               ~6 GB    infra + KC-core + KC-frontend + KH-gateway
#   full                  ~18 GB   everything except Ollama (default)
#   ai-local              opt-in   Ollama + setup (overlay with branding-only)
#   monitoring            opt-in   Prometheus + Grafana
#   backup                opt-in   MinIO mirror sidecar
#   build-only            opt-in   kite-base image build

set -e
cd "$(dirname "$0")/.."

PROFILE=""
SERVICES=()

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        *)
            SERVICES+=("$1")
            shift
            ;;
    esac
done

# Default profile = "full" (preserves prior behavior of starting all primary services).
# Set KITE_COMPOSE_PROFILE env to override, or pass --profile <name> explicitly.
if [ -z "$PROFILE" ]; then
    PROFILE="${KITE_COMPOSE_PROFILE:-full}"
fi

CMD="docker-compose -f docker-compose.kitehub.yml --profile $PROFILE"

if [ ${#SERVICES[@]} -eq 0 ]; then
    echo "Starting KiteHub stack (profile: $PROFILE)..."
    $CMD up -d
else
    echo "Starting (profile: $PROFILE): ${SERVICES[*]}"
    $CMD up -d "${SERVICES[@]}"
fi

echo ""
echo "Waiting for services to be ready..."
sleep 3
docker-compose -f docker-compose.kitehub.yml --profile "$PROFILE" ps
