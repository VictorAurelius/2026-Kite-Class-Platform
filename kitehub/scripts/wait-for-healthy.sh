#!/bin/bash
# Wait for all KiteHub services to be healthy
# Usage: ./scripts/wait-for-healthy.sh [timeout_seconds]

set -euo pipefail

TIMEOUT=${1:-180}
INTERVAL=5
ELAPSED=0

SERVICES=(
  "kitehub-postgres"
  "kitehub-redis"
  "kitehub-rabbitmq"
  "kitehub-minio"
  "kitehub-subscription"
  "kitehub-branding"
  "kitehub-email"
  "kitehub-admin"
  "kitehub-gateway"
  "kiteclass-core"
)

echo "🔄 Waiting for all services to be healthy (timeout: ${TIMEOUT}s)..."

while [ $ELAPSED -lt $TIMEOUT ]; do
  ALL_HEALTHY=true
  UNHEALTHY_SERVICES=""

  for SERVICE in "${SERVICES[@]}"; do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$SERVICE" 2>/dev/null || echo "not_found")

    if [ "$STATUS" != "healthy" ]; then
      ALL_HEALTHY=false
      UNHEALTHY_SERVICES="$UNHEALTHY_SERVICES $SERVICE($STATUS)"
    fi
  done

  if [ "$ALL_HEALTHY" = true ]; then
    echo "✅ All services healthy! (${ELAPSED}s)"
    exit 0
  fi

  echo "  ⏳ [${ELAPSED}s] Waiting:${UNHEALTHY_SERVICES}"
  sleep $INTERVAL
  ELAPSED=$((ELAPSED + INTERVAL))
done

echo "❌ Timeout after ${TIMEOUT}s. Unhealthy services:${UNHEALTHY_SERVICES}"
echo ""
echo "Check logs with: cd kitehub && ./scripts/logs.sh <service>"
exit 1
