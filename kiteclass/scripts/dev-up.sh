#!/bin/bash
set -euo pipefail

# Standalone dev infrastructure for KiteClass (no KiteHub dependency).
# Starts only PostgreSQL and Redis on non-conflicting ports.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

docker compose -f docker-compose.standalone.yml up -d

echo ""
echo "KiteClass dev stack started"
echo "  PostgreSQL: localhost:5434  (user: kiteclass / pass: kiteclass_dev_password)"
echo "  Redis:      localhost:6381"
echo ""
echo "Spring Boot profiles:"
echo "  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/kiteclass_dev"
echo "  SPRING_DATA_REDIS_HOST=localhost"
echo "  SPRING_DATA_REDIS_PORT=6381"
