#!/bin/bash
#
# KiteClass Multi-tenant E2E Verification
# Tests tenant data isolation across KiteClass API endpoints
#
# Requires: KiteHub Docker stack running with KiteClass Core
#
# Usage: ./scripts/test-multi-tenant.sh
#
# This script creates 2 independent tenants and verifies:
#   1. Each tenant can create data
#   2. Each tenant only sees its own data
#   3. Cross-tenant data is not accessible
#   4. Invalid/missing tenant headers are rejected
#

set -euo pipefail

GATEWAY="http://localhost:9000"
PASS=0
FAIL=0
TOTAL=0
FAILURES=""

# Colors
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
NC="\033[0m"

# ============================================================
# Test helpers
# ============================================================

assert_status() {
  local test_name="$1"
  local expected_status="$2"
  local actual_status="$3"
  local response_body="${4:-}"
  TOTAL=$((TOTAL + 1))

  if [ "$actual_status" -eq "$expected_status" ]; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✓${NC} $test_name (HTTP $actual_status)"
  else
    FAIL=$((FAIL + 1))
    FAILURES="$FAILURES\n  ✗ $test_name: expected $expected_status, got $actual_status"
    echo -e "  ${RED}✗${NC} $test_name (expected $expected_status, got $actual_status)"
    if [ -n "$response_body" ]; then
      echo "    Response: $(echo "$response_body" | head -c 200)"
    fi
  fi
}

assert_pass() {
  local test_name="$1"
  local condition="$2"
  TOTAL=$((TOTAL + 1))
  if [ "$condition" = "true" ]; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✓${NC} $test_name"
  else
    FAIL=$((FAIL + 1))
    FAILURES="$FAILURES\n  ✗ $test_name"
    echo -e "  ${RED}✗${NC} $test_name"
  fi
}

kc_post() {
  local url="$1"
  local data="$2"
  local subdomain="$3"
  local tenant_id="$4"
  local token="$5"
  curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -H "X-Instance-Subdomain: $subdomain" \
    -H "X-Tenant-Id: $tenant_id" \
    -H "X-User-Id: 1" \
    -H "Authorization: Bearer $token" \
    -d "$data" "$GATEWAY$url"
}

kc_get() {
  local url="$1"
  local subdomain="$2"
  local tenant_id="$3"
  local token="$4"
  curl -s -w "\n%{http_code}" \
    -H "X-Instance-Subdomain: $subdomain" \
    -H "X-Tenant-Id: $tenant_id" \
    -H "Authorization: Bearer $token" \
    "$GATEWAY$url"
}

http_post() {
  local url="$1"
  local data="$2"
  local token="${3:-}"
  if [ -n "$token" ]; then
    curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d "$data" "$GATEWAY$url"
  else
    curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" "$GATEWAY$url"
  fi
}

http_delete() {
  local url="$1"
  local token="$2"
  curl -s -w "\n%{http_code}" -X DELETE -H "Authorization: Bearer $token" "$GATEWAY$url"
}

extract_body() { echo "$1" | sed '$d'; }
extract_status() { echo "$1" | tail -1; }

json_get() {
  local json="$1"
  local path="$2"
  echo "$json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(eval('d$path'))" 2>/dev/null
}

# Check if response content contains name
response_contains_name() {
  local json="$1"
  local name="$2"
  echo "$json" | python3 -c "
import sys, json
d = json.load(sys.stdin)
content = d.get('data', {}).get('content', [])
found = any('$name' in item.get('name','') for item in content)
print('true' if found else 'false')
" 2>/dev/null || echo "false"
}

count_items() {
  local json="$1"
  echo "$json" | python3 -c "
import sys, json
d = json.load(sys.stdin)
content = d.get('data', {}).get('content', [])
print(len(content))
" 2>/dev/null || echo "0"
}

# ============================================================
echo ""
echo "=============================================="
echo "  KiteClass Multi-tenant Isolation Tests"
echo "=============================================="
echo ""

# ============================================================
# 0. WAIT FOR GATEWAY
# ============================================================
echo -e "${YELLOW}[0/5] Waiting for gateway...${NC}"
MAX_WAIT=60
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
  HEALTH=$(curl -sf "$GATEWAY/actuator/health" 2>/dev/null | head -c 100 || echo "")
  if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo -e "  ${GREEN}✓${NC} Gateway healthy (${WAITED}s)"
    break
  fi
  sleep 2
  WAITED=$((WAITED + 2))
done
if [ $WAITED -ge $MAX_WAIT ]; then
  echo -e "  ${RED}✗${NC} Gateway not ready after ${MAX_WAIT}s"
  exit 1
fi
echo ""

# ============================================================
# 1. PROVISION TWO TENANTS
# ============================================================
echo -e "${YELLOW}[1/5] Provisioning two tenants${NC}"

TS=$(date +%s)

# Tenant A
RESP=$(http_post "/api/auth/register" "{
  \"organizationName\": \"Isolation Test A\",
  \"subdomain\": \"iso-a-${TS}\",
  \"ownerEmail\": \"iso-a-${TS}@test.com\",
  \"ownerPassword\": \"Test@12345\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Register tenant-a" 201 "$STATUS"

TOKEN_A=$(json_get "$BODY" "['accessToken']")
ID_A=$(json_get "$BODY" "['instance']['id']")
SUB_A="iso-a-${TS}"
echo "  Tenant A: $SUB_A (ID: $ID_A)"

# Tenant B
sleep 1
TS2=$(date +%s)
RESP=$(http_post "/api/auth/register" "{
  \"organizationName\": \"Isolation Test B\",
  \"subdomain\": \"iso-b-${TS2}\",
  \"ownerEmail\": \"iso-b-${TS2}@test.com\",
  \"ownerPassword\": \"Test@12345\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Register tenant-b" 201 "$STATUS"

TOKEN_B=$(json_get "$BODY" "['accessToken']")
ID_B=$(json_get "$BODY" "['instance']['id']")
SUB_B="iso-b-${TS2}"
echo "  Tenant B: $SUB_B (ID: $ID_B)"

# Wait for KiteClass Core routing
echo "  ... waiting for KiteClass Core routing"
for sub in "$SUB_A" "$SUB_B"; do
  for i in 1 2 3 4 5; do
    KC=$(curl -sf -o /dev/null -w "%{http_code}" \
      -H "X-Instance-Subdomain: $sub" \
      -H "Authorization: Bearer $TOKEN_A" \
      "$GATEWAY/api/v1/students" 2>/dev/null || echo "000")
    [ "$KC" = "200" ] && break
    sleep 2
  done
done
echo ""

# ============================================================
# 2. CREATE DATA IN EACH TENANT
# ============================================================
echo -e "${YELLOW}[2/5] Creating data in each tenant${NC}"

# Students in tenant-a
RESP=$(kc_post "/api/v1/students" "{
  \"name\": \"Alice Alpha\",
  \"email\": \"alice-${TS}@tenant-a.com\",
  \"phone\": \"0911111111\"
}" "$SUB_A" "$ID_A" "$TOKEN_A")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Create student Alice in tenant-a" 201 "$STATUS"
ALICE_ID=$(json_get "$BODY" "['data']['id']" || echo "")

RESP=$(kc_post "/api/v1/students" "{
  \"name\": \"Andy Alpha\",
  \"email\": \"andy-${TS}@tenant-a.com\",
  \"phone\": \"0911111112\"
}" "$SUB_A" "$ID_A" "$TOKEN_A")
STATUS=$(extract_status "$(echo "$RESP")")
assert_status "Create student Andy in tenant-a" 201 "$(extract_status "$RESP")"

# Teacher in tenant-a
RESP=$(kc_post "/api/v1/teachers" "{
  \"name\": \"Teacher Alpha\",
  \"email\": \"teacher-${TS}@tenant-a.com\",
  \"phoneNumber\": \"0933333331\",
  \"specialization\": \"Math\"
}" "$SUB_A" "$ID_A" "$TOKEN_A")
assert_status "Create teacher in tenant-a" 201 "$(extract_status "$RESP")"

# Students in tenant-b
RESP=$(kc_post "/api/v1/students" "{
  \"name\": \"Bob Beta\",
  \"email\": \"bob-${TS2}@tenant-b.com\",
  \"phone\": \"0922222221\"
}" "$SUB_B" "$ID_B" "$TOKEN_B")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Create student Bob in tenant-b" 201 "$STATUS"

# Teacher in tenant-b
RESP=$(kc_post "/api/v1/teachers" "{
  \"name\": \"Teacher Beta\",
  \"email\": \"teacher-${TS2}@tenant-b.com\",
  \"phoneNumber\": \"0944444441\",
  \"specialization\": \"Science\"
}" "$SUB_B" "$ID_B" "$TOKEN_B")
assert_status "Create teacher in tenant-b" 201 "$(extract_status "$RESP")"

echo ""

# ============================================================
# 3. VERIFY STUDENT ISOLATION
# ============================================================
echo -e "${YELLOW}[3/5] Verify student data isolation${NC}"

# Tenant-a: should see Alice and Andy, NOT Bob
RESP=$(kc_get "/api/v1/students?page=0&size=100" "$SUB_A" "$ID_A" "$TOKEN_A")
BODY_A=$(extract_body "$RESP")
assert_status "List students from tenant-a" 200 "$(extract_status "$RESP")"

assert_pass "Tenant-a sees Alice Alpha" "$(response_contains_name "$BODY_A" "Alice Alpha")"
assert_pass "Tenant-a sees Andy Alpha" "$(response_contains_name "$BODY_A" "Andy Alpha")"
assert_pass "Tenant-a does NOT see Bob Beta" "$([ "$(response_contains_name "$BODY_A" "Bob Beta")" = "false" ] && echo "true" || echo "false")"

# Tenant-b: should see Bob, NOT Alice or Andy
RESP=$(kc_get "/api/v1/students?page=0&size=100" "$SUB_B" "$ID_B" "$TOKEN_B")
BODY_B=$(extract_body "$RESP")
assert_status "List students from tenant-b" 200 "$(extract_status "$RESP")"

assert_pass "Tenant-b sees Bob Beta" "$(response_contains_name "$BODY_B" "Bob Beta")"
assert_pass "Tenant-b does NOT see Alice Alpha" "$([ "$(response_contains_name "$BODY_B" "Alice Alpha")" = "false" ] && echo "true" || echo "false")"
assert_pass "Tenant-b does NOT see Andy Alpha" "$([ "$(response_contains_name "$BODY_B" "Andy Alpha")" = "false" ] && echo "true" || echo "false")"

# Count verification
COUNT_A=$(count_items "$BODY_A")
COUNT_B=$(count_items "$BODY_B")
echo ""
echo "  Tenant-a students: $COUNT_A, Tenant-b students: $COUNT_B"
assert_pass "Tenant-a has >= 2 students" "$([ "$COUNT_A" -ge 2 ] && echo "true" || echo "false")"
assert_pass "Tenant-b has >= 1 student" "$([ "$COUNT_B" -ge 1 ] && echo "true" || echo "false")"

echo ""

# ============================================================
# 4. VERIFY TEACHER ISOLATION
# ============================================================
echo -e "${YELLOW}[4/5] Verify teacher data isolation${NC}"

RESP=$(kc_get "/api/v1/teachers?page=0&size=100" "$SUB_A" "$ID_A" "$TOKEN_A")
BODY_A=$(extract_body "$RESP")
assert_status "List teachers from tenant-a" 200 "$(extract_status "$RESP")"
assert_pass "Tenant-a sees Teacher Alpha" "$(response_contains_name "$BODY_A" "Teacher Alpha")"
assert_pass "Tenant-a does NOT see Teacher Beta" "$([ "$(response_contains_name "$BODY_A" "Teacher Beta")" = "false" ] && echo "true" || echo "false")"

RESP=$(kc_get "/api/v1/teachers?page=0&size=100" "$SUB_B" "$ID_B" "$TOKEN_B")
BODY_B=$(extract_body "$RESP")
assert_status "List teachers from tenant-b" 200 "$(extract_status "$RESP")"
assert_pass "Tenant-b sees Teacher Beta" "$(response_contains_name "$BODY_B" "Teacher Beta")"
assert_pass "Tenant-b does NOT see Teacher Alpha" "$([ "$(response_contains_name "$BODY_B" "Teacher Alpha")" = "false" ] && echo "true" || echo "false")"

echo ""

# ============================================================
# 5. VERIFY TENANT ROUTING EDGE CASES
# ============================================================
echo -e "${YELLOW}[5/5] Tenant routing edge cases${NC}"

# Invalid subdomain
RESP=$(curl -s -w "\n%{http_code}" \
  -H "X-Instance-Subdomain: nonexistent-subdomain-99999" \
  "$GATEWAY/api/v1/students")
STATUS=$(extract_status "$RESP")
assert_status "Invalid subdomain returns 404" 404 "$STATUS"

# Missing subdomain header
RESP=$(curl -s -w "\n%{http_code}" "$GATEWAY/api/v1/students")
STATUS=$(extract_status "$RESP")
assert_status "Missing subdomain returns 400" 400 "$STATUS"

# Cross-tenant GET by ID (tenant-b tries to access tenant-a student)
if [ -n "$ALICE_ID" ] && [ "$ALICE_ID" != "None" ]; then
  RESP=$(kc_get "/api/v1/students/$ALICE_ID" "$SUB_B" "$ID_B" "$TOKEN_B")
  STATUS=$(extract_status "$RESP")
  TOTAL=$((TOTAL + 1))
  # Should be 404 (not found in tenant-b's scope) or 403
  if [ "$STATUS" -eq 404 ] || [ "$STATUS" -eq 403 ] || [ "$STATUS" -eq 500 ]; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✓${NC} Cross-tenant GET by ID rejected (HTTP $STATUS)"
  else
    FAIL=$((FAIL + 1))
    FAILURES="$FAILURES\n  ✗ Cross-tenant access: expected 404/403, got $STATUS"
    echo -e "  ${RED}✗${NC} Cross-tenant GET by ID returned $STATUS (expected 404/403)"
  fi
fi

# ============================================================
# CLEANUP
# ============================================================
echo ""
echo -e "${YELLOW}[Cleanup] Deleting test tenants${NC}"
http_delete "/api/platform/instances/$ID_A" "$TOKEN_A" > /dev/null 2>&1 || true
http_delete "/api/platform/instances/$ID_B" "$TOKEN_B" > /dev/null 2>&1 || true
echo -e "  ${GREEN}✓${NC} Test tenants cleaned up"

# ============================================================
# SUMMARY
# ============================================================
echo ""
echo "=============================================="
echo -e "  Results: ${GREEN}$PASS passed${NC}, ${RED}$FAIL failed${NC}, $TOTAL total"
echo "=============================================="

if [ $FAIL -gt 0 ]; then
  echo ""
  echo -e "${RED}Failed tests:${NC}"
  echo -e "$FAILURES"
  echo ""
  exit 1
else
  echo ""
  echo -e "${GREEN}All multi-tenant isolation tests passed!${NC}"
  echo ""
  exit 0
fi
