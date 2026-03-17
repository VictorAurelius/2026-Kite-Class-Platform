#!/bin/bash
# Check KiteHub stack status
# Usage: ./scripts/status.sh [--health]

set -e
cd "$(dirname "$0")/.."

echo "=============================================="
echo "  KiteHub Stack Status"
echo "=============================================="
echo ""

docker-compose -f docker-compose.kitehub.yml ps -a

echo ""
echo "=============================================="
echo "  Service URLs"
echo "=============================================="
echo ""
echo "  Frontend:      http://localhost:3001"
echo "  Gateway API:   http://localhost:9000"
echo "  Subscription:  http://localhost:8081"
echo "  Branding:      http://localhost:8083"
echo "  Email:         http://localhost:8084"
echo "  Admin:         http://localhost:8085"
echo ""
echo "  PostgreSQL:    localhost:5433"
echo "  Redis:         localhost:6380"
echo "  RabbitMQ:      http://localhost:15673 (admin: kitehub/kitehub_dev_password)"
echo "  MinIO Console: http://localhost:9191 (admin: kitehub/kitehub_dev_password)"
echo ""

if [ "$1" = "--health" ] || [ "$1" = "-h" ]; then
    echo "=============================================="
    echo "  Health Checks"
    echo "=============================================="
    echo ""

    for service in gateway subscription branding email admin; do
        port=8080
        case $service in
            gateway) port=9000 ;;
            subscription) port=8081 ;;
            branding) port=8083 ;;
            email) port=8084 ;;
            admin) port=8085 ;;
        esac

        echo -n "  kitehub-$service: "
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo "✅ healthy"
        else
            echo "❌ unhealthy"
        fi
    done

    echo -n "  kitehub-frontend: "
    if curl -s -f "http://localhost:3001" > /dev/null 2>&1; then
        echo "✅ healthy"
    else
        echo "❌ unhealthy"
    fi
    echo ""
fi
