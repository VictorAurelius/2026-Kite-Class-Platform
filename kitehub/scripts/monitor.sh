#!/bin/bash
# Background health monitor for KiteHub services
# Logs failures and optionally sends desktop notifications
# Usage: ./scripts/monitor.sh [--interval 30] [--notify]

cd "$(dirname "$0")/.."

# Default settings
INTERVAL=30
NOTIFY=false
LOG_FILE="logs/monitor.log"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --interval|-i)
            INTERVAL=$2
            shift 2
            ;;
        --notify|-n)
            NOTIFY=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [--interval 30] [--notify]"
            echo ""
            echo "Options:"
            echo "  --interval, -i  Check interval in seconds (default: 30)"
            echo "  --notify, -n    Send desktop notifications on failure"
            echo "  --help, -h      Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--interval 30] [--notify]"
            exit 1
            ;;
    esac
done

# Create logs directory if not exists
mkdir -p logs

# Colors for log file
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Notification function
send_notification() {
    local title=$1
    local message=$2

    if [ "$NOTIFY" = true ]; then
        # Try different notification methods based on OS
        if command -v notify-send &> /dev/null; then
            # Linux (most distributions)
            notify-send "$title" "$message" -u critical
        elif command -v osascript &> /dev/null; then
            # macOS
            osascript -e "display notification \"$message\" with title \"$title\""
        elif command -v powershell.exe &> /dev/null; then
            # WSL
            powershell.exe -Command "New-BurntToastNotification -Text '$title', '$message'"
        fi
    fi
}

# Health check function
check_service_health() {
    local service=$1
    local port=$2
    local endpoint=${3:-/actuator/health}

    if curl -s -f --max-time 5 "http://localhost:$port$endpoint" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Log function
log_message() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        ERROR)
            echo -e "${RED}[$timestamp] $level: $message${NC}" | tee -a "$LOG_FILE"
            ;;
        WARN)
            echo -e "${YELLOW}[$timestamp] $level: $message${NC}" | tee -a "$LOG_FILE"
            ;;
        INFO)
            echo -e "${GREEN}[$timestamp] $level: $message${NC}" | tee -a "$LOG_FILE"
            ;;
        *)
            echo "[$timestamp] $level: $message" | tee -a "$LOG_FILE"
            ;;
    esac
}

echo "=============================================="
echo "  KiteHub Health Monitor"
echo "=============================================="
echo ""
echo "  Check interval: ${INTERVAL}s"
echo "  Notifications:  $(if [ "$NOTIFY" = true ]; then echo "enabled"; else echo "disabled"; fi)"
echo "  Log file:       $LOG_FILE"
echo ""
echo "  Press Ctrl+C to stop"
echo ""

log_message "INFO" "Monitor started (interval: ${INTERVAL}s, notify: $NOTIFY)"

# Service configuration
declare -A SERVICES
SERVICES[gateway]=9000
SERVICES[subscription]=8081
SERVICES[branding]=8083
SERVICES[email]=8084
SERVICES[admin]=8085

declare -A SERVICE_STATUS
for service in "${!SERVICES[@]}"; do
    SERVICE_STATUS[$service]="unknown"
done

# Monitor loop
while true; do
    FAILURES=0
    RECOVERIES=0

    for service in "${!SERVICES[@]}"; do
        port=${SERVICES[$service]}
        old_status=${SERVICE_STATUS[$service]}

        if check_service_health "$service" "$port"; then
            SERVICE_STATUS[$service]="healthy"

            # Check if this is a recovery
            if [ "$old_status" = "unhealthy" ]; then
                log_message "INFO" "kitehub-$service recovered (port $port)"
                send_notification "KiteHub Monitor" "Service $service recovered"
                ((RECOVERIES++))
            fi
        else
            SERVICE_STATUS[$service]="unhealthy"

            # Check if this is a new failure
            if [ "$old_status" != "unhealthy" ]; then
                log_message "ERROR" "kitehub-$service is down (port $port)"
                send_notification "KiteHub Monitor" "Service $service is down!"
                ((FAILURES++))
            fi
        fi
    done

    # Log summary if there were changes
    if [ $FAILURES -gt 0 ] || [ $RECOVERIES -gt 0 ]; then
        log_message "WARN" "Status: $FAILURES failure(s), $RECOVERIES recovery(ies)"
    fi

    sleep "$INTERVAL"
done
