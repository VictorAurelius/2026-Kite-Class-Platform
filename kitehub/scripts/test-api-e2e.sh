#!/bin/bash
#
# KiteHub Backend E2E API Tests
# Tests all endpoints through the gateway (localhost:9000)
#
# Usage: ./scripts/test-api-e2e.sh
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

assert_json_field() {
  local test_name="$1"
  local json="$2"
  local field="$3"
  TOTAL=$((TOTAL + 1))

  if echo "$json" | python3 -c "import sys,json; d=json.load(sys.stdin); assert '$field' in d" 2>/dev/null; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✓${NC} $test_name (has field '$field')"
  else
    FAIL=$((FAIL + 1))
    FAILURES="$FAILURES\n  ✗ $test_name: missing field '$field'"
    echo -e "  ${RED}✗${NC} $test_name (missing field '$field')"
  fi
}

assert_json_value() {
  local test_name="$1"
  local json="$2"
  local field="$3"
  local expected="$4"
  TOTAL=$((TOTAL + 1))

  local actual
  actual=$(echo "$json" | python3 -c "import sys,json; print(json.load(sys.stdin).get('$field',''))" 2>/dev/null)

  if [ "$actual" = "$expected" ]; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✓${NC} $test_name ($field = $expected)"
  else
    FAIL=$((FAIL + 1))
    FAILURES="$FAILURES\n  ✗ $test_name: $field expected '$expected', got '$actual'"
    echo -e "  ${RED}✗${NC} $test_name ($field: expected '$expected', got '$actual')"
  fi
}

http_get() {
  local url="$1"
  local token="${2:-}"
  if [ -n "$token" ]; then
    curl -s -w "\n%{http_code}" -H "Authorization: Bearer $token" "$GATEWAY$url"
  else
    curl -s -w "\n%{http_code}" "$GATEWAY$url"
  fi
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

http_put() {
  local url="$1"
  local data="$2"
  local token="${3:-}"
  curl -s -w "\n%{http_code}" -X PUT -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d "$data" "$GATEWAY$url"
}

http_patch() {
  local url="$1"
  local data="$2"
  local token="${3:-}"
  curl -s -w "\n%{http_code}" -X PATCH -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d "$data" "$GATEWAY$url"
}

http_delete() {
  local url="$1"
  local token="${2:-}"
  curl -s -w "\n%{http_code}" -X DELETE -H "Authorization: Bearer $token" "$GATEWAY$url"
}

extract_body() {
  echo "$1" | sed '$d'
}

extract_status() {
  echo "$1" | tail -1
}

# ============================================================
echo ""
echo "=============================================="
echo "  KiteHub Backend E2E API Tests"
echo "=============================================="
echo ""

# ============================================================
# 1. GATEWAY HEALTH
# ============================================================
echo -e "${YELLOW}[1/8] Gateway Health${NC}"

RESP=$(http_get "/actuator/health")
STATUS=$(extract_status "$RESP")
assert_status "Gateway health endpoint" 200 "$STATUS"

# ============================================================
# 2. AUTH - REGISTER
# ============================================================
echo ""
echo -e "${YELLOW}[2/8] Auth Endpoints${NC}"

TIMESTAMP=$(date +%s)
REG_EMAIL="e2e-test-${TIMESTAMP}@example.com"
REG_SUBDOMAIN="e2e-test-${TIMESTAMP}"

RESP=$(http_post "/api/auth/register" "{
  \"organizationName\": \"E2E Test Org\",
  \"subdomain\": \"$REG_SUBDOMAIN\",
  \"ownerEmail\": \"$REG_EMAIL\",
  \"ownerPassword\": \"Test@12345\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")

assert_status "POST /api/auth/register" 201 "$STATUS" "$BODY"
assert_json_field "Register returns user" "$BODY" "user"
assert_json_field "Register returns accessToken" "$BODY" "accessToken"
assert_json_field "Register returns refreshToken" "$BODY" "refreshToken"
assert_json_field "Register returns instance" "$BODY" "instance"

ACCESS_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
REFRESH_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('refreshToken',''))" 2>/dev/null)
USER_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('user',{}).get('id',''))" 2>/dev/null)
INSTANCE_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('instance',{}).get('id',''))" 2>/dev/null)

# AUTH - DUPLICATE REGISTER
RESP=$(http_post "/api/auth/register" "{
  \"organizationName\": \"Duplicate\",
  \"subdomain\": \"$REG_SUBDOMAIN\",
  \"ownerEmail\": \"$REG_EMAIL\",
  \"ownerPassword\": \"Test@12345\"
}")
STATUS=$(extract_status "$RESP")
assert_status "POST /api/auth/register (duplicate) returns 400" 400 "$STATUS"

# AUTH - LOGIN
RESP=$(http_post "/api/auth/login" "{
  \"email\": \"$REG_EMAIL\",
  \"password\": \"Test@12345\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")

assert_status "POST /api/auth/login" 200 "$STATUS" "$BODY"
assert_json_field "Login returns user" "$BODY" "user"
assert_json_field "Login returns accessToken" "$BODY" "accessToken"
assert_json_field "Login returns instances" "$BODY" "instances"

# AUTH - LOGIN INVALID
RESP=$(http_post "/api/auth/login" "{
  \"email\": \"wrong@example.com\",
  \"password\": \"wrong\"
}")
STATUS=$(extract_status "$RESP")
assert_status "POST /api/auth/login (invalid) returns 400" 400 "$STATUS"

# AUTH - REFRESH TOKEN
RESP=$(http_post "/api/auth/refresh" "{
  \"refreshToken\": \"$REFRESH_TOKEN\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")

assert_status "POST /api/auth/refresh" 200 "$STATUS" "$BODY"
assert_json_field "Refresh returns accessToken" "$BODY" "accessToken"
assert_json_field "Refresh returns refreshToken" "$BODY" "refreshToken"

# Update access token from refresh
NEW_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
if [ -n "$NEW_TOKEN" ] && [ "$NEW_TOKEN" != "None" ]; then
  ACCESS_TOKEN="$NEW_TOKEN"
fi

# AUTH - REFRESH INVALID
RESP=$(http_post "/api/auth/refresh" "{
  \"refreshToken\": \"invalid-token\"
}")
STATUS=$(extract_status "$RESP")
assert_status "POST /api/auth/refresh (invalid) returns 400" 400 "$STATUS"

# AUTH - RE-LOGIN (verify register → login flow works)
RESP=$(http_post "/api/auth/login" "{
  \"email\": \"$REG_EMAIL\",
  \"password\": \"Test@12345\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")

assert_status "POST /api/auth/login (re-login registered user)" 200 "$STATUS" "$BODY"

# Check user has instances after re-login
LOGIN_INSTANCES=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('instances',[])))" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$LOGIN_INSTANCES" -gt 0 ] 2>/dev/null; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Re-login user has instances ($LOGIN_INSTANCES)"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ Re-login user has no instances"
  echo -e "  ${RED}✗${NC} Re-login user has no instances"
fi

# ============================================================
# 3. INSTANCES
# ============================================================
echo ""
echo -e "${YELLOW}[3/8] Instance Endpoints${NC}"

# GET instance by ID
RESP=$(http_get "/api/platform/instances/$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/instances/{id}" 200 "$STATUS" "$BODY"
assert_json_field "Instance has id" "$BODY" "id"
assert_json_field "Instance has subdomain" "$BODY" "subdomain"

# GET instance by subdomain
RESP=$(http_get "/api/platform/instances/subdomain/$REG_SUBDOMAIN" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/instances/subdomain/{subdomain}" 200 "$STATUS"

# GET instances by owner
RESP=$(http_get "/api/platform/instances/owner/$USER_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/instances/owner/{ownerId}" 200 "$STATUS" "$BODY"

# GET all instances
RESP=$(http_get "/api/platform/instances" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/instances (list all)" 200 "$STATUS" "$BODY"

# PUT update instance
RESP=$(http_put "/api/platform/instances/$INSTANCE_ID" "{
  \"organizationName\": \"Updated Org Name\"
}" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "PUT /api/platform/instances/{id}" 200 "$STATUS" "$BODY"

# PATCH update instance
RESP=$(http_patch "/api/platform/instances/$INSTANCE_ID" "{
  \"organizationName\": \"Patched Org Name\"
}" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "PATCH /api/platform/instances/{id}" 200 "$STATUS" "$BODY"

# GET trial status
RESP=$(http_get "/api/platform/instances/$INSTANCE_ID/trial-status" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/instances/{id}/trial-status" 200 "$STATUS" "$BODY"

# GET non-existent instance
RESP=$(http_get "/api/platform/instances/00000000-0000-0000-0000-000000000099" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/instances/{id} (not found) returns 404" 404 "$STATUS"

# ============================================================
# 4. ADMIN
# ============================================================
echo ""
echo -e "${YELLOW}[4/8] Admin Endpoints${NC}"

# Admin endpoints don't require JWT auth on backend (gateway would handle it)
RESP=$(http_get "/api/platform/admin/dashboard" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/admin/dashboard" 200 "$STATUS" "$BODY"
assert_json_field "Dashboard has totalInstances" "$BODY" "totalInstances"

# Admin list instances
RESP=$(http_get "/api/platform/admin/instances" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/admin/instances" 200 "$STATUS" "$BODY"

# Admin instance detail
RESP=$(http_get "/api/platform/admin/instances/$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/admin/instances/{id}" 200 "$STATUS" "$BODY"

# Admin instance detail not found
RESP=$(http_get "/api/platform/admin/instances/00000000-0000-0000-0000-000000000099" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/admin/instances/{id} (not found) returns 404" 404 "$STATUS"

# Admin suspend instance
RESP=$(http_patch "/api/platform/admin/instances/$INSTANCE_ID/suspend" "{}" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "PATCH /api/platform/admin/instances/{id}/suspend" 200 "$STATUS" "$BODY"

# Admin activate instance
RESP=$(http_patch "/api/platform/admin/instances/$INSTANCE_ID/activate" "{}" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "PATCH /api/platform/admin/instances/{id}/activate" 200 "$STATUS" "$BODY"

# Admin revenue
RESP=$(http_get "/api/platform/admin/revenue" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/admin/revenue" 200 "$STATUS" "$BODY"

# Admin pending payments
RESP=$(http_get "/api/platform/admin/payments/pending" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/admin/payments/pending" 200 "$STATUS" "$BODY"

# Admin subscriptions
RESP=$(http_get "/api/platform/admin/subscriptions" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/admin/subscriptions" 200 "$STATUS" "$BODY"

# ============================================================
# 5. CORS
# ============================================================
echo ""
echo -e "${YELLOW}[5/10] CORS & Gateway${NC}"

# OPTIONS preflight
RESP=$(curl -s -o /dev/null -w "%{http_code}" -X OPTIONS \
  -H "Origin: http://localhost:3001" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  "$GATEWAY/api/auth/login")
TOTAL=$((TOTAL + 1))
if [ "$RESP" -eq 200 ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} CORS preflight returns 200"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ CORS preflight: expected 200, got $RESP"
  echo -e "  ${RED}✗${NC} CORS preflight returns $RESP (expected 200)"
fi

# CORS headers present
CORS_HEADER=$(curl -s -D - -o /dev/null -X OPTIONS \
  -H "Origin: http://localhost:3001" \
  -H "Access-Control-Request-Method: POST" \
  "$GATEWAY/api/auth/login" 2>/dev/null | grep -i "access-control-allow-origin" | head -1)
TOTAL=$((TOTAL + 1))
if echo "$CORS_HEADER" | grep -qi "localhost:3001"; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} CORS allows localhost:3001"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ CORS: Access-Control-Allow-Origin missing or wrong"
  echo -e "  ${RED}✗${NC} CORS Access-Control-Allow-Origin header not set for localhost:3001"
fi

# Gateway routes non-existent service
RESP=$(http_get "/api/nonexistent/endpoint")
STATUS=$(extract_status "$RESP")
TOTAL=$((TOTAL + 1))
if [ "$STATUS" -eq 404 ] || [ "$STATUS" -eq 503 ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Non-existent route returns $STATUS"
else
  FAIL=$((FAIL + 1))
  echo -e "  ${RED}✗${NC} Non-existent route returns $STATUS (expected 404 or 503)"
fi

# ============================================================
# 6. SUBSCRIPTION ENDPOINTS
# ============================================================
echo ""
echo -e "${YELLOW}[6/10] Subscription Endpoints${NC}"

# Get subscriptions for instance (may be empty for trial)
RESP=$(http_get "/api/platform/subscriptions/instance/$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/subscriptions/instance/{instanceId}" 200 "$STATUS"

# Get active subscription (may be 404 for trial without subscription)
RESP=$(http_get "/api/platform/subscriptions/instance/$INSTANCE_ID/active" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
TOTAL=$((TOTAL + 1))
if [ "$STATUS" -eq 200 ] || [ "$STATUS" -eq 404 ] || [ "$STATUS" -eq 400 ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} GET /api/platform/subscriptions/.../active returns $STATUS (ok for trial)"
else
  FAIL=$((FAIL + 1))
  echo -e "  ${RED}✗${NC} GET /api/platform/subscriptions/.../active returns $STATUS"
fi

# ============================================================
# 7. BRANDING ENDPOINTS (via gateway)
# ============================================================
echo ""
echo -e "${YELLOW}[7/10] Branding Endpoints (via gateway)${NC}"

# Branding assets list (may be empty)
RESP=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$GATEWAY/api/platform/branding/assets/$INSTANCE_ID")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/branding/assets/{instanceId}" 200 "$STATUS"

# Branding jobs list
RESP=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Instance-Id: $INSTANCE_ID" \
  "$GATEWAY/api/platform/branding/jobs")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/platform/branding/jobs" 200 "$STATUS"

# ============================================================
# 8. TENANT ROUTING
# ============================================================
echo ""
echo -e "${YELLOW}[8/11] Tenant Routing${NC}"

# Get instance subdomain for routing test
RESP=$(http_get "/api/platform/admin/instances/$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
INSTANCE_SUBDOMAIN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('subdomain',''))" 2>/dev/null)

# Test tenant routing with X-Instance-Subdomain header
# This should resolve to the instance and route to KiteClass Core
# KiteClass Core may not be running (503/502), but the tenant RESOLVER should work
RESP=$(curl -s -w "\n%{http_code}" \
  -H "X-Instance-Subdomain: $INSTANCE_SUBDOMAIN" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$GATEWAY/api/v1/students")
STATUS=$(extract_status "$RESP")
TOTAL=$((TOTAL + 1))
# 502/503 = KiteClass Core not running (expected in PR 6.2, will be fixed in PR 6.3)
# 200 = KiteClass Core running and responding
# 400/404 = Tenant resolver rejected (BAD - means filter doesn't work)
if [ "$STATUS" -eq 502 ] || [ "$STATUS" -eq 503 ] || [ "$STATUS" -eq 200 ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Tenant routing resolved (HTTP $STATUS - KiteClass Core ${STATUS} is expected)"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ Tenant routing failed: got $STATUS (expected 502/503 since KiteClass Core not running)"
  echo -e "  ${RED}✗${NC} Tenant routing failed (HTTP $STATUS)"
fi

# Test tenant routing with invalid subdomain
RESP=$(curl -s -w "\n%{http_code}" \
  -H "X-Instance-Subdomain: nonexistent-subdomain-12345" \
  "$GATEWAY/api/v1/students")
STATUS=$(extract_status "$RESP")
assert_status "Tenant routing rejects unknown subdomain" 404 "$STATUS"

# Test tenant routing without any subdomain/header (localhost)
RESP=$(curl -s -w "\n%{http_code}" "$GATEWAY/api/v1/students")
STATUS=$(extract_status "$RESP")
assert_status "Tenant routing rejects no-subdomain request" 400 "$STATUS"

# ============================================================
# 9. KITECLASS API (via TenantResolver)
# ============================================================
echo ""
echo -e "${YELLOW}[9/12] KiteClass API (via Gateway)${NC}"

# Get instance subdomain
RESP=$(http_get "/api/platform/admin/instances/$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
INSTANCE_SUBDOMAIN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('subdomain',''))" 2>/dev/null)

# Test KiteClass students endpoint via gateway
RESP=$(curl -s -w "\n%{http_code}" \
  -H "X-Instance-Subdomain: $INSTANCE_SUBDOMAIN" \
  -H "X-Tenant-Id: $INSTANCE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$GATEWAY/api/v1/students")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/students (via TenantResolver)" 200 "$STATUS"

# Test KiteClass courses endpoint
RESP=$(curl -s -w "\n%{http_code}" \
  -H "X-Instance-Subdomain: $INSTANCE_SUBDOMAIN" \
  -H "X-Tenant-Id: $INSTANCE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$GATEWAY/api/v1/courses")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/courses (via TenantResolver)" 200 "$STATUS"

# Test KiteClass teachers endpoint
RESP=$(curl -s -w "\n%{http_code}" \
  -H "X-Instance-Subdomain: $INSTANCE_SUBDOMAIN" \
  -H "X-Tenant-Id: $INSTANCE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$GATEWAY/api/v1/teachers")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/teachers (via TenantResolver)" 200 "$STATUS"

# Test create student via KiteClass
RESP=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -H "X-Instance-Subdomain: $INSTANCE_SUBDOMAIN" \
  -H "X-Tenant-Id: $INSTANCE_ID" \
  -H "X-User-Id: 1" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{\"name\":\"E2E Test Student\",\"email\":\"student@test.com\"}" \
  "$GATEWAY/api/v1/students")
STATUS=$(extract_status "$RESP")
TOTAL=$((TOTAL + 1))
if [ "$STATUS" -eq 200 ] || [ "$STATUS" -eq 201 ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} POST /api/v1/students (create student) HTTP $STATUS"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ POST /api/v1/students: expected 200/201, got $STATUS"
  echo -e "  ${RED}✗${NC} POST /api/v1/students (HTTP $STATUS)"
fi

# ============================================================
# 10. DATABASE PROVISIONING
# ============================================================
echo ""
echo -e "${YELLOW}[10/12] Database Provisioning${NC}"

# Check instance has real database URL via admin endpoint (not "pending")
RESP=$(http_get "/api/platform/admin/instances/$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
DB_URL=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('databaseUrl','pending'))" 2>/dev/null)

TOTAL=$((TOTAL + 1))
if [ "$DB_URL" != "pending" ] && [ "$DB_URL" != "None" ] && [ -n "$DB_URL" ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Instance has real databaseUrl: $(echo "$DB_URL" | head -c 60)..."
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ Instance databaseUrl is still 'pending' or empty (DB provisioning not enabled?)"
  echo -e "  ${RED}✗${NC} Instance databaseUrl = '$DB_URL' (expected real JDBC URL)"
fi

# Check database name follows pattern kiteclass_*
TOTAL=$((TOTAL + 1))
if echo "$DB_URL" | grep -q "kiteclass_"; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Database name follows kiteclass_{id} pattern"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ Database URL doesn't contain 'kiteclass_' pattern"
  echo -e "  ${RED}✗${NC} Database URL doesn't follow naming pattern"
fi

# Verify database actually exists by connecting via PostgreSQL
DB_NAME=$(echo "$DB_URL" | python3 -c "import sys; url=sys.stdin.read().strip(); print(url.split('/')[-1] if '/' in url else '')" 2>/dev/null)
if [ -n "$DB_NAME" ] && [ "$DB_NAME" != "pending" ]; then
  # Check DB exists via admin connection
  DB_EXISTS=$(docker exec kitehub-postgres psql -U kitehub -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>/dev/null | tr -d '[:space:]')
  TOTAL=$((TOTAL + 1))
  if [ "$DB_EXISTS" = "1" ]; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✓${NC} Database '$DB_NAME' exists in PostgreSQL"
  else
    FAIL=$((FAIL + 1))
    FAILURES="$FAILURES\n  ✗ Database '$DB_NAME' does NOT exist in PostgreSQL"
    echo -e "  ${RED}✗${NC} Database '$DB_NAME' not found in PostgreSQL"
  fi

  # Check tables were created by Flyway migrations
  TABLE_COUNT=$(docker exec kitehub-postgres psql -U kitehub -d "$DB_NAME" -tAc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'" 2>/dev/null | tr -d '[:space:]')
  TOTAL=$((TOTAL + 1))
  if [ -n "$TABLE_COUNT" ] && [ "$TABLE_COUNT" -gt 5 ] 2>/dev/null; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✓${NC} Database has $TABLE_COUNT tables (Flyway migrations ran)"
  else
    FAIL=$((FAIL + 1))
    FAILURES="$FAILURES\n  ✗ Database has $TABLE_COUNT tables (expected >5 from Flyway)"
    echo -e "  ${RED}✗${NC} Database has ${TABLE_COUNT:-0} tables (expected >5)"
  fi

  # Check key tables exist (students, courses, teachers)
  for TABLE in students courses teachers; do
    TABLE_EXISTS=$(docker exec kitehub-postgres psql -U kitehub -d "$DB_NAME" -tAc "SELECT 1 FROM information_schema.tables WHERE table_name='$TABLE'" 2>/dev/null | tr -d '[:space:]')
    TOTAL=$((TOTAL + 1))
    if [ "$TABLE_EXISTS" = "1" ]; then
      PASS=$((PASS + 1))
      echo -e "  ${GREEN}✓${NC} Table '$TABLE' exists"
    else
      FAIL=$((FAIL + 1))
      FAILURES="$FAILURES\n  ✗ Table '$TABLE' missing in provisioned DB"
      echo -e "  ${RED}✗${NC} Table '$TABLE' missing"
    fi
  done
else
  echo -e "  ${YELLOW}⚠${NC} Skipping DB existence checks (no valid DB name)"
fi

# ============================================================
# 9. DATA ISOLATION (2nd instance)
# ============================================================
echo ""
echo -e "${YELLOW}[11/12] Data Isolation${NC}"

TIMESTAMP2=$(date +%s)
REG_EMAIL2="e2e-iso-${TIMESTAMP2}@example.com"
REG_SUBDOMAIN2="e2e-iso-${TIMESTAMP2}"

RESP=$(http_post "/api/auth/register" "{
  \"organizationName\": \"Isolation Test Org\",
  \"subdomain\": \"$REG_SUBDOMAIN2\",
  \"ownerEmail\": \"$REG_EMAIL2\",
  \"ownerPassword\": \"Test@12345\"
}")
BODY2=$(extract_body "$RESP")
STATUS2=$(extract_status "$RESP")
assert_status "Register 2nd instance for isolation test" 201 "$STATUS2"

INSTANCE_ID2=$(echo "$BODY2" | python3 -c "import sys,json; print(json.load(sys.stdin).get('instance',{}).get('id',''))" 2>/dev/null)
TOKEN2=$(echo "$BODY2" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)

# Verify 2nd instance has different database (via admin)
RESP2=$(http_get "/api/platform/admin/instances/$INSTANCE_ID2" "$TOKEN2")
BODY2=$(extract_body "$RESP2")
DB_URL2=$(echo "$BODY2" | python3 -c "import sys,json; print(json.load(sys.stdin).get('databaseUrl','pending'))" 2>/dev/null)

TOTAL=$((TOTAL + 1))
if [ "$DB_URL" != "$DB_URL2" ] && [ "$DB_URL2" != "pending" ] && [ "$DB_URL2" != "None" ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} 2nd instance has different database (isolation confirmed)"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ 2nd instance has same or pending DB URL (no isolation)"
  echo -e "  ${RED}✗${NC} Data isolation failed: DB_URL1=$DB_URL, DB_URL2=$DB_URL2"
fi

# Cleanup 2nd instance
http_delete "/api/platform/instances/$INSTANCE_ID2" "$TOKEN2" > /dev/null 2>&1

# ============================================================
# 10. CLEANUP - DELETE INSTANCE
# ============================================================
echo ""
echo -e "${YELLOW}[12/12] Cleanup & Delete${NC}"

RESP=$(http_delete "/api/platform/instances/$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "DELETE /api/platform/instances/{id}" 204 "$STATUS"

# Verify deleted
RESP=$(http_get "/api/platform/instances/$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "GET deleted instance returns 404" 404 "$STATUS"

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
  echo -e "${GREEN}All tests passed! Zero bugs. ✓${NC}"
  echo ""
  exit 0
fi
