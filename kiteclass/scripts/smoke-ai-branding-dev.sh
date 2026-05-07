#!/usr/bin/env bash
#
# smoke-ai-branding-dev.sh — verify the dev-profile BrandingDataSeeder produced
# the expected demo dataset on a running kiteclass-core.
#
# Usage:
#   ./kiteclass/scripts/smoke-ai-branding-dev.sh                    # default localhost:8081
#   ./kiteclass/scripts/smoke-ai-branding-dev.sh http://host:port   # custom base URL
#   CORE_BASE_URL=http://host:port ./kiteclass/scripts/smoke-ai-branding-dev.sh
#
# Tracking: GAP-235 Sub-PR G.
#
# Exit codes:
#   0 = all checks pass
#   1 = at least one assertion failed
#   2 = base URL unreachable
#
set -euo pipefail

CORE_BASE_URL="${1:-${CORE_BASE_URL:-http://localhost:8081}}"
EXPECTED_SLUG="thanglong"

if [ -t 1 ]; then
  GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[0;33m'; NC='\033[0m'
else
  GREEN=''; RED=''; YELLOW=''; NC=''
fi

PASS=0
FAIL=0

pass() { printf '%s✓%s %s\n' "$GREEN" "$NC" "$1"; PASS=$((PASS+1)); }
fail() { printf '%s✗%s %s\n' "$RED" "$NC" "$1"; FAIL=$((FAIL+1)); }
note() { printf '%s…%s %s\n' "$YELLOW" "$NC" "$1"; }

require_jq() {
  if ! command -v jq >/dev/null 2>&1; then
    printf '%s✗%s jq is required (apt install jq / brew install jq)\n' "$RED" "$NC"
    exit 1
  fi
}

reachable() {
  if ! curl -fsS --max-time 5 "${CORE_BASE_URL}/actuator/health" >/dev/null 2>&1; then
    printf '%s✗%s Cannot reach %s/actuator/health — is kiteclass-core running on dev profile?\n' "$RED" "$NC" "${CORE_BASE_URL}"
    printf '    Try: ./kiteclass/scripts/dev-up.sh, then ./kiteclass/scripts/dev-status.sh\n'
    exit 2
  fi
}

check_instances_list() {
  note "GET ${CORE_BASE_URL}/api/v1/instances"
  local body
  if ! body=$(curl -fsS --max-time 10 "${CORE_BASE_URL}/api/v1/instances"); then
    fail "Could not GET /api/v1/instances"
    return
  fi

  local count
  count=$(echo "$body" | jq '[.data[]?] | length // 0')
  if [ "${count}" -lt 1 ]; then
    fail "Expected ≥1 instance from BrandingDataSeeder; got ${count}"
    return
  fi
  pass "Instance count = ${count} (≥1 expected)"

  local seeded
  seeded=$(echo "$body" | jq -r --arg slug "$EXPECTED_SLUG" '.data[] | select(.slug==$slug) | .id // empty')
  if [ -z "${seeded}" ]; then
    fail "No instance with slug=${EXPECTED_SLUG} found"
    return
  fi
  pass "Seeded instance slug=${EXPECTED_SLUG} present (id=${seeded})"

  local status branding_version
  status=$(echo "$body" | jq -r --arg slug "$EXPECTED_SLUG" '.data[] | select(.slug==$slug) | .status')
  branding_version=$(echo "$body" | jq -r --arg slug "$EXPECTED_SLUG" '.data[] | select(.slug==$slug) | .brandingVersion')
  if [ "${status}" != "DEPLOYED" ]; then
    fail "Expected status=DEPLOYED on seeded instance; got ${status}"
  else
    pass "Seeded instance status=DEPLOYED"
  fi
  if [ "${branding_version}" != "1" ]; then
    fail "Expected brandingVersion=1; got ${branding_version}"
  else
    pass "Seeded instance brandingVersion=1"
  fi

  echo "${seeded}" > /tmp/.smoke-ai-branding-instance-id
}

check_branding_package() {
  local id
  id=$(cat /tmp/.smoke-ai-branding-instance-id 2>/dev/null || true)
  if [ -z "${id}" ]; then
    fail "Skipping package check — no seeded id captured above"
    return
  fi

  note "GET ${CORE_BASE_URL}/api/v1/branding/${id}/package"
  local body etag
  if ! body=$(curl -fsS --max-time 10 -D /tmp/.smoke-ai-branding-headers "${CORE_BASE_URL}/api/v1/branding/${id}/package"); then
    fail "Could not GET /api/v1/branding/${id}/package"
    return
  fi
  pass "Branding package endpoint responded 200"

  etag=$(grep -i '^etag:' /tmp/.smoke-ai-branding-headers | tr -d '\r' | awk -F': ' '{print $2}' || true)
  if [ -n "${etag}" ]; then
    pass "ETag header present: ${etag}"
  else
    note "ETag header absent — production controller may use a different cache key"
  fi

  local primary
  primary=$(echo "$body" | jq -r '.data.theme.primaryColor // .data.theme.primary // empty')
  if [ -n "${primary}" ]; then
    pass "Theme.primaryColor present: ${primary}"
  else
    fail "Theme.primaryColor missing from package payload"
  fi
}

check_public_branding() {
  note "GET ${CORE_BASE_URL}/api/v1/branding/public"
  if curl -fsS --max-time 10 "${CORE_BASE_URL}/api/v1/branding/public" >/dev/null 2>&1; then
    pass "Public branding endpoint responded 200"
  else
    fail "Public branding endpoint failed (expected 200)"
  fi
}

main() {
  printf "kiteclass-core base URL: %s\n" "${CORE_BASE_URL}"
  printf "Expected seeded slug: %s\n\n" "${EXPECTED_SLUG}"

  require_jq
  reachable
  check_instances_list
  check_branding_package
  check_public_branding

  printf '\n────────────────────────────\n'
  printf 'Pass: %s%d%s   Fail: %s%d%s\n' "$GREEN" "$PASS" "$NC" "$RED" "$FAIL" "$NC"

  rm -f /tmp/.smoke-ai-branding-instance-id /tmp/.smoke-ai-branding-headers

  if [ "${FAIL}" -gt 0 ]; then
    exit 1
  fi
  exit 0
}

main "$@"
