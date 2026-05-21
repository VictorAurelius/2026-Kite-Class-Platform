#!/bin/bash
# Start KiteHub stack
# Usage: ./scripts/up.sh [--profile PROFILE] [--rebuild] [--force-recreate] [--pull-from-ecr TAG] [service...]
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
#
# Flags (per GAP-425 — cold-rebuild orchestration):
#   --rebuild             Run scripts/build-all.sh BEFORE up to rebuild all images
#                         from current source (catches stale-image-vs-source drift).
#                         Use after `git pull` or first-time cold setup.
#   --force-recreate      Pass `--force-recreate` to docker compose so containers
#                         spawn fresh against current image SHAs (else compose
#                         reuses old container against stale image — GAP-425 root
#                         cause). Defaults ON when --rebuild fires.
#   --pull-from-ecr TAG   Pull pre-built CI images from ECR with tag TAG instead
#                         of using local images. Use for staging-parity validation
#                         (Phase 4) — bit-for-bit identical to production deploy.
#                         Requires `aws ecr get-login-password` configured.

set -e

# GAP-694 Phase 0B preflight: verify Docker daemon reachable before any compose op.
bash "$(dirname "$0")/check-docker.sh" || { echo "[up.sh] Docker preflight failed. See output above." >&2; exit 1; }

cd "$(dirname "$0")/.."

PROFILE=""
SERVICES=()
DO_REBUILD=0
DO_FORCE_RECREATE=0
ECR_TAG=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --rebuild)
            DO_REBUILD=1
            DO_FORCE_RECREATE=1   # rebuild without recreate is a footgun (GAP-425)
            shift
            ;;
        --force-recreate)
            DO_FORCE_RECREATE=1
            shift
            ;;
        --pull-from-ecr)
            ECR_TAG="$2"
            DO_FORCE_RECREATE=1   # ECR pull only meaningful with recreate
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

# GAP-425 Option E: pull CI-built images from ECR for production-parity validation.
if [ -n "$ECR_TAG" ]; then
    : "${ECR_REGISTRY:?ECR_REGISTRY must be set (e.g. <account>.dkr.ecr.ap-southeast-1.amazonaws.com)}"
    : "${AWS_REGION:=ap-southeast-1}"
    echo "==> Logging into ECR ($ECR_REGISTRY)..."
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"
    SERVICES_TO_PULL=(kitehub-subscription kitehub-email kitehub-admin kitehub-branding kite-gateway kitehub-frontend kiteclass-core kiteclass-frontend)
    for svc in "${SERVICES_TO_PULL[@]}"; do
        IMG="$ECR_REGISTRY/kite/$svc:$ECR_TAG"
        echo "==> Pulling $IMG"
        docker pull "$IMG"
        docker tag "$IMG" "$svc:latest"
    done
    echo "==> ECR pull complete; $ECR_TAG → :latest"
fi

# GAP-425 Option A: rebuild all local images before up.
if [ "$DO_REBUILD" = "1" ]; then
    echo "==> Rebuilding all images via scripts/build-all.sh (GAP-425 --rebuild)..."
    bash "$(dirname "$0")/build-all.sh"
fi

CMD="docker-compose -f docker-compose.kitehub.yml --profile $PROFILE"

UP_FLAGS="-d"
if [ "$DO_FORCE_RECREATE" = "1" ]; then
    UP_FLAGS="$UP_FLAGS --force-recreate"
fi

if [ ${#SERVICES[@]} -eq 0 ]; then
    echo "Starting KiteHub stack (profile: $PROFILE${DO_REBUILD:+, rebuilt}${ECR_TAG:+, ECR $ECR_TAG}${DO_FORCE_RECREATE:+, force-recreate})..."
    $CMD up $UP_FLAGS
else
    echo "Starting (profile: $PROFILE): ${SERVICES[*]}"
    $CMD up $UP_FLAGS "${SERVICES[@]}"
fi

echo ""
echo "Waiting for services to be ready..."
sleep 3
docker-compose -f docker-compose.kitehub.yml --profile "$PROFILE" ps
