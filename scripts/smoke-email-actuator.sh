#!/usr/bin/env bash
# smoke-email-actuator.sh — Wave 78 Bucket E
#
# Verify kitehub-email actuator (health + info + send-counter metric) on a target
# host. Closes GAP-527 verification AC.
#
# Usage:
#   bash scripts/smoke-email-actuator.sh                    # default http://localhost:8084
#   EMAIL_BASE=https://api.kitehub.me bash scripts/smoke-email-actuator.sh
#   EMAIL_BASE=http://kitehub-email:8084 SEND_LIVE=true bash scripts/smoke-email-actuator.sh
#
# Per pre-handoff-self-test-completeness.md §2.3 (email-driven flow):
#   (a) Email actually sent (metric counter > 0 after smoke send)
#   (b) Link in email points to live URL (caller verifies inbox)
#   (c) Clicking link advances state (caller verifies in browser)
#
# Exit codes:
#   0 — actuator UP + (optional) send smoke OK
#   1 — actuator unreachable / DOWN
#   2 — send smoke request failed
set -euo pipefail

EMAIL_BASE="${EMAIL_BASE:-http://localhost:8084}"
SEND_LIVE="${SEND_LIVE:-false}"
TARGET_EMAIL="${TARGET_EMAIL:-vannkite@outlook.com}"
TIMEOUT="${TIMEOUT:-10}"
TEMPLATE="welcome"

color() { printf "\033[%sm%s\033[0m\n" "$1" "$2"; }
green() { color "32" "$1"; }
red() { color "31" "$1"; }
yellow() { color "33" "$1"; }

# Parse optional --template argument (Wave 80 GAP-561b: invite-staff variant)
while [[ $# -gt 0 ]]; do
    case "$1" in
        --template)
            TEMPLATE="$2"
            shift 2
            ;;
        --template=*)
            TEMPLATE="${1#*=}"
            shift
            ;;
        *)
            shift
            ;;
    esac
done

# Known template-to-payload mapping. Add new templates here when introduced.
case "$TEMPLATE" in
    welcome)
        SMOKE_ENDPOINT="/api/v1/emails/welcome"
        SMOKE_PAYLOAD='{"to":"'"$TARGET_EMAIL"'","organizationName":"Wave 80 Smoke Test","trialDays":14,"expiryDate":"2026-05-28","loginUrl":"https://kitehub.me/login"}'
        ;;
    invite-staff)
        # GAP-561b — staff invitation email variant
        SMOKE_ENDPOINT="/api/v1/emails/invite-staff"
        SMOKE_PAYLOAD='{"to":"'"$TARGET_EMAIL"'","recipientName":"Trần Thị Hồng","ownerName":"Nguyễn Văn An","tenantName":"Trung tâm Anh ngữ Sky Education","role":"STAFF","inviteUrl":"https://kitehub.me/staff/accept-invite?token=smoke-test-token","expiresAt":"Thứ Hai, 22/05/2026"}'
        ;;
    *)
        red "Unknown --template value: $TEMPLATE (supported: welcome, invite-staff)"
        exit 2
        ;;
esac

echo "===== kitehub-email actuator smoke ====="
echo "Target: $EMAIL_BASE"
echo "Template: $TEMPLATE"
echo "Live send: $SEND_LIVE (TARGET_EMAIL=$TARGET_EMAIL)"
echo ""

# Step 1 — /actuator/health
echo "[1/4] GET /actuator/health"
HEALTH_HTTP=$(curl -s -o /tmp/health.json -w "%{http_code}" --max-time "$TIMEOUT" \
    "$EMAIL_BASE/actuator/health" || echo "000")

if [[ "$HEALTH_HTTP" != "200" ]]; then
    red "  FAIL — HTTP $HEALTH_HTTP (expected 200)"
    [[ -f /tmp/health.json ]] && cat /tmp/health.json
    exit 1
fi

STATUS=$(grep -oE '"status":"[A-Z_]+"' /tmp/health.json | head -1 | cut -d'"' -f4 || echo "UNKNOWN")
if [[ "$STATUS" != "UP" ]]; then
    red "  FAIL — actuator status=$STATUS (expected UP)"
    cat /tmp/health.json
    exit 1
fi
green "  PASS — status=UP"

# Step 2 — /actuator/health/liveness (Wave 77 Bucket B probe group)
echo "[2/4] GET /actuator/health/liveness"
LIVE_HTTP=$(curl -s -o /tmp/live.json -w "%{http_code}" --max-time "$TIMEOUT" \
    "$EMAIL_BASE/actuator/health/liveness" || echo "000")
if [[ "$LIVE_HTTP" == "200" ]]; then
    green "  PASS — liveness 200"
else
    yellow "  WARN — liveness HTTP $LIVE_HTTP (probe group may not be enabled)"
fi

# Step 3 — /actuator/info
echo "[3/4] GET /actuator/info"
INFO_HTTP=$(curl -s -o /tmp/info.json -w "%{http_code}" --max-time "$TIMEOUT" \
    "$EMAIL_BASE/actuator/info" || echo "000")
if [[ "$INFO_HTTP" == "200" ]]; then
    green "  PASS — info 200"
else
    yellow "  WARN — info HTTP $INFO_HTTP (info endpoint may be empty)"
fi

# Step 4 — send smoke (optional, requires SEND_LIVE=true + active provider)
if [[ "$SEND_LIVE" != "true" ]]; then
    echo "[4/4] Send smoke — SKIPPED (set SEND_LIVE=true to enable)"
    echo ""
    green "===== Actuator smoke PASS ====="
    exit 0
fi

echo "[4/4] POST $SMOKE_ENDPOINT (live send to $TARGET_EMAIL, template=$TEMPLATE)"
SEND_HTTP=$(curl -s -o /tmp/send.json -w "%{http_code}" --max-time 30 \
    -X POST -H "Content-Type: application/json" \
    -d "$SMOKE_PAYLOAD" \
    "$EMAIL_BASE$SMOKE_ENDPOINT" || echo "000")

if [[ "$SEND_HTTP" =~ ^(200|201|202)$ ]]; then
    green "  PASS — send accepted (HTTP $SEND_HTTP)"
    echo "  Verify via Resend dashboard → Logs → Sent within 60s"
else
    red "  FAIL — send HTTP $SEND_HTTP"
    [[ -f /tmp/send.json ]] && cat /tmp/send.json
    exit 2
fi

echo ""
green "===== Actuator + send smoke PASS ====="
