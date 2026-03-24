#!/bin/bash
# KiteClass Monitoring Helper
# Usage: ./scripts/monitor.sh [status|urls|health]

case "$1" in
  status)
    echo "=== KiteClass Service Health ==="
    curl -s http://localhost:8081/actuator/health | python3 -m json.tool 2>/dev/null || echo "Core: DOWN"
    curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || echo "Gateway: DOWN"
    ;;
  urls)
    echo "Prometheus: http://localhost:9090"
    echo "Grafana:    http://localhost:3001 (admin/admin)"
    echo "Core:       http://localhost:8081/actuator/health"
    echo "Gateway:    http://localhost:8080/actuator/health"
    ;;
  health)
    # Quick health check all services
    for svc in "Core:8081" "Gateway:8080"; do
      name=${svc%%:*}; port=${svc##*:}
      status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health)
      [ "$status" = "200" ] && echo "✅ $name: UP" || echo "❌ $name: DOWN ($status)"
    done
    ;;
  *)
    echo "Usage: $0 {status|urls|health}"
    ;;
esac
