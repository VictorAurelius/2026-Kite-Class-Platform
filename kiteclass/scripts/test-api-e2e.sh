#!/bin/bash
#
# KiteClass Backend E2E API Tests
# Tests all KiteClass endpoints through the KiteHub gateway (localhost:9000)
#
# Requires: KiteHub Docker stack running with KiteClass Core
#
# Usage: ./scripts/test-api-e2e.sh
#
# Headers used:
#   X-Instance-Subdomain: tenant routing (resolved by TenantResolver filter)
#   X-Tenant-Id: tenant ID (set by TenantResolver after subdomain lookup)
#   X-User-Id: user ID for audit fields
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
# Test helpers (same style as kitehub/scripts/test-api-e2e.sh)
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

assert_status_one_of() {
  local test_name="$1"
  shift
  local actual_status="${!#}"  # last argument
  local args=("$@")
  local expected_list=("${args[@]:0:$((${#args[@]}-1))}")
  TOTAL=$((TOTAL + 1))

  for expected in "${expected_list[@]}"; do
    if [ "$actual_status" -eq "$expected" ]; then
      PASS=$((PASS + 1))
      echo -e "  ${GREEN}✓${NC} $test_name (HTTP $actual_status)"
      return
    fi
  done

  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ $test_name: expected one of [${expected_list[*]}], got $actual_status"
  echo -e "  ${RED}✗${NC} $test_name (expected one of [${expected_list[*]}], got $actual_status)"
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

assert_json_list_empty() {
  local test_name="$1"
  local json="$2"
  local list_field="$3"
  TOTAL=$((TOTAL + 1))

  local count
  count=$(echo "$json" | python3 -c "
import sys, json
d = json.load(sys.stdin)
items = d.get('$list_field', d.get('data', {}).get('$list_field', d.get('data', {}).get('content', [])))
if isinstance(items, list):
    print(len(items))
else:
    print(0)
" 2>/dev/null || echo "-1")

  if [ "$count" = "0" ]; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✓${NC} $test_name (list is empty)"
  else
    FAIL=$((FAIL + 1))
    FAILURES="$FAILURES\n  ✗ $test_name: expected empty list, got $count items"
    echo -e "  ${RED}✗${NC} $test_name (expected empty, got $count items)"
  fi
}

# HTTP helpers with tenant headers
# Usage: kc_get <path> [subdomain] [tenant_id] [token]
kc_get() {
  local url="$1"
  local subdomain="${2:-}"
  local tenant_id="${3:-}"
  local token="${4:-}"
  local headers=()

  [ -n "$subdomain" ] && headers+=(-H "X-Instance-Subdomain: $subdomain")
  [ -n "$tenant_id" ] && headers+=(-H "X-Tenant-Id: $tenant_id")
  [ -n "$token" ] && headers+=(-H "Authorization: Bearer $token")

  curl -s -w "\n%{http_code}" "${headers[@]}" "$GATEWAY$url"
}

# Usage: kc_post <path> <json_data> [subdomain] [tenant_id] [token]
kc_post() {
  local url="$1"
  local data="$2"
  local subdomain="${3:-}"
  local tenant_id="${4:-}"
  local token="${5:-}"
  local headers=(-H "Content-Type: application/json")

  [ -n "$subdomain" ] && headers+=(-H "X-Instance-Subdomain: $subdomain")
  [ -n "$tenant_id" ] && headers+=(-H "X-Tenant-Id: $tenant_id")
  [ -n "$token" ] && headers+=(-H "Authorization: Bearer $token")
  headers+=(-H "X-User-Id: 1")

  curl -s -w "\n%{http_code}" -X POST "${headers[@]}" -d "$data" "$GATEWAY$url"
}

# Usage: kc_put <path> <json_data> [subdomain] [tenant_id] [token]
kc_put() {
  local url="$1"
  local data="$2"
  local subdomain="${3:-}"
  local tenant_id="${4:-}"
  local token="${5:-}"
  local headers=(-H "Content-Type: application/json")

  [ -n "$subdomain" ] && headers+=(-H "X-Instance-Subdomain: $subdomain")
  [ -n "$tenant_id" ] && headers+=(-H "X-Tenant-Id: $tenant_id")
  [ -n "$token" ] && headers+=(-H "Authorization: Bearer $token")

  curl -s -w "\n%{http_code}" -X PUT "${headers[@]}" -d "$data" "$GATEWAY$url"
}

# Usage: kc_delete <path> [subdomain] [tenant_id] [token]
kc_delete() {
  local url="$1"
  local subdomain="${2:-}"
  local tenant_id="${3:-}"
  local token="${4:-}"
  local headers=()

  [ -n "$subdomain" ] && headers+=(-H "X-Instance-Subdomain: $subdomain")
  [ -n "$tenant_id" ] && headers+=(-H "X-Tenant-Id: $tenant_id")
  [ -n "$token" ] && headers+=(-H "Authorization: Bearer $token")

  curl -s -w "\n%{http_code}" -X DELETE "${headers[@]}" "$GATEWAY$url"
}

# Standard HTTP helpers (no tenant headers, for platform API)
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

json_get() {
  local json="$1"
  local path="$2"
  echo "$json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(eval('d$path'))" 2>/dev/null
}

# ============================================================
echo ""
echo "=============================================="
echo "  KiteClass Backend E2E API Tests"
echo "=============================================="
echo ""

# ============================================================
# 0. SETUP: Register a test tenant via KiteHub
# ============================================================
echo -e "${YELLOW}[0/7] Setup - Register test tenant${NC}"

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
  echo -e "  ... waiting for gateway (${WAITED}s)"
done

if [ $WAITED -ge $MAX_WAIT ]; then
  echo -e "  ${RED}✗${NC} Gateway not ready after ${MAX_WAIT}s"
  echo "  Make sure Docker stack is running: ./scripts/up.sh (from kitehub/)"
  exit 1
fi

TIMESTAMP=$(date +%s)
REG_EMAIL="e2e-kc-${TIMESTAMP}@example.com"
REG_SUBDOMAIN="e2e-kc-${TIMESTAMP}"

RESP=$(http_post "/api/auth/register" "{
  \"organizationName\": \"KiteClass E2E Test\",
  \"subdomain\": \"$REG_SUBDOMAIN\",
  \"ownerEmail\": \"$REG_EMAIL\",
  \"ownerPassword\": \"Test@12345\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Register test tenant" 201 "$STATUS" "$BODY"

ACCESS_TOKEN=$(json_get "$BODY" "['accessToken']" || echo "")
INSTANCE_ID=$(json_get "$BODY" "['instance']['id']" || echo "")

echo "  Tenant: $REG_SUBDOMAIN (ID: $INSTANCE_ID)"

# Wait for KiteClass Core to be routable
echo "  ... waiting for KiteClass Core routing"
for i in 1 2 3 4 5; do
  KC_CHECK=$(curl -sf -o /dev/null -w "%{http_code}" \
    -H "X-Instance-Subdomain: $REG_SUBDOMAIN" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    "$GATEWAY/api/v1/students" 2>/dev/null || echo "000")
  if [ "$KC_CHECK" = "200" ]; then
    echo -e "  ${GREEN}✓${NC} KiteClass Core reachable"
    break
  fi
  sleep 3
done

echo ""

# ============================================================
# 1. HEALTH CHECK
# ============================================================
echo -e "${YELLOW}[1/7] Health Check${NC}"

RESP=$(http_get "/actuator/health")
STATUS=$(extract_status "$RESP")
assert_status "Gateway health endpoint" 200 "$STATUS"

echo ""

# ============================================================
# 2. STUDENT CRUD
# ============================================================
echo -e "${YELLOW}[2/7] Student CRUD${NC}"

# CREATE student
RESP=$(kc_post "/api/v1/students" "{
  \"name\": \"E2E Test Student\",
  \"email\": \"student-${TIMESTAMP}@test.com\",
  \"phone\": \"0901234567\",
  \"gender\": \"MALE\",
  \"address\": \"123 Test Street\"
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "POST /api/v1/students (create)" 201 "$STATUS" "$BODY"
assert_json_field "Student response has 'data'" "$BODY" "data"

STUDENT_ID=$(json_get "$BODY" "['data']['id']" || echo "")
echo "  Created student ID: $STUDENT_ID"

# GET student by ID
RESP=$(kc_get "/api/v1/students/$STUDENT_ID" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/students/{id}" 200 "$STATUS" "$BODY"

# GET students list (search)
RESP=$(kc_get "/api/v1/students?page=0&size=10" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/students (list)" 200 "$STATUS" "$BODY"

# PUT update student
RESP=$(kc_put "/api/v1/students/$STUDENT_ID" "{
  \"name\": \"Updated E2E Student\",
  \"email\": \"student-${TIMESTAMP}@test.com\",
  \"phone\": \"0901234567\"
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "PUT /api/v1/students/{id} (update)" 200 "$STATUS" "$BODY"

# DELETE student
RESP=$(kc_delete "/api/v1/students/$STUDENT_ID" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status_one_of "DELETE /api/v1/students/{id}" 200 204 "$STATUS"

echo ""

# ============================================================
# 3. TEACHER CRUD
# ============================================================
echo -e "${YELLOW}[3/7] Teacher CRUD${NC}"

# CREATE teacher
RESP=$(kc_post "/api/v1/teachers" "{
  \"name\": \"E2E Test Teacher\",
  \"email\": \"teacher-${TIMESTAMP}@test.com\",
  \"phoneNumber\": \"0912345678\",
  \"specialization\": \"Mathematics\",
  \"experienceYears\": 5
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "POST /api/v1/teachers (create)" 201 "$STATUS" "$BODY"

TEACHER_ID=$(json_get "$BODY" "['data']['id']" || echo "")
echo "  Created teacher ID: $TEACHER_ID"

# GET teacher by ID
RESP=$(kc_get "/api/v1/teachers/$TEACHER_ID" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/teachers/{id}" 200 "$STATUS" "$BODY"

# GET teachers list
RESP=$(kc_get "/api/v1/teachers?page=0&size=10" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/teachers (list)" 200 "$STATUS"

# PUT update teacher
RESP=$(kc_put "/api/v1/teachers/$TEACHER_ID" "{
  \"name\": \"Updated E2E Teacher\",
  \"email\": \"teacher-${TIMESTAMP}@test.com\",
  \"phoneNumber\": \"0912345678\",
  \"specialization\": \"Physics\"
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "PUT /api/v1/teachers/{id} (update)" 200 "$STATUS"

# DELETE teacher (will be recreated for course test)
RESP=$(kc_delete "/api/v1/teachers/$TEACHER_ID" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status_one_of "DELETE /api/v1/teachers/{id}" 200 204 "$STATUS"

# Re-create teacher for course tests
RESP=$(kc_post "/api/v1/teachers" "{
  \"name\": \"Course Teacher\",
  \"email\": \"course-teacher-${TIMESTAMP}@test.com\",
  \"phoneNumber\": \"0923456789\",
  \"specialization\": \"General\"
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
TEACHER_ID=$(json_get "$BODY" "['data']['id']" || echo "")
echo "  Re-created teacher ID: $TEACHER_ID (for course tests)"

echo ""

# ============================================================
# 4. COURSE CRUD
# ============================================================
echo -e "${YELLOW}[4/7] Course CRUD${NC}"

COURSE_CODE="E2E-$(echo $TIMESTAMP | tail -c 7)"

# CREATE course
RESP=$(kc_post "/api/v1/courses" "{
  \"name\": \"E2E Test Course\",
  \"code\": \"$COURSE_CODE\",
  \"description\": \"A test course for E2E testing\",
  \"teacherId\": $TEACHER_ID,
  \"durationWeeks\": 8,
  \"totalSessions\": 16,
  \"price\": 500000,
  \"level\": \"Beginner\",
  \"category\": \"Testing\"
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "POST /api/v1/courses (create)" 201 "$STATUS" "$BODY"

COURSE_ID=$(json_get "$BODY" "['data']['id']" || echo "")
echo "  Created course ID: $COURSE_ID"

# GET course by ID
RESP=$(kc_get "/api/v1/courses/$COURSE_ID" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/courses/{id}" 200 "$STATUS" "$BODY"

# GET courses list
RESP=$(kc_get "/api/v1/courses?page=0&size=10" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/courses (list)" 200 "$STATUS"

# PUT update course
RESP=$(kc_put "/api/v1/courses/$COURSE_ID" "{
  \"name\": \"Updated E2E Course\",
  \"description\": \"Updated description\"
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "PUT /api/v1/courses/{id} (update)" 200 "$STATUS"

# DELETE course
RESP=$(kc_delete "/api/v1/courses/$COURSE_ID" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status_one_of "DELETE /api/v1/courses/{id}" 200 204 "$STATUS"

# Re-create course for class tests
COURSE_CODE2="E2E-$(echo $((TIMESTAMP + 1)) | tail -c 7)"
RESP=$(kc_post "/api/v1/courses" "{
  \"name\": \"E2E Class Test Course\",
  \"code\": \"$COURSE_CODE2\",
  \"description\": \"Course for class tests\",
  \"teacherId\": $TEACHER_ID,
  \"durationWeeks\": 4,
  \"totalSessions\": 8,
  \"price\": 300000,
  \"level\": \"Intermediate\",
  \"category\": \"Testing\"
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
COURSE_ID=$(json_get "$BODY" "['data']['id']" || echo "")
echo "  Re-created course ID: $COURSE_ID (for class tests)"

echo ""

# ============================================================
# 5. CLASS CRUD
# ============================================================
echo -e "${YELLOW}[5/7] Class CRUD${NC}"

# CREATE class (under course)
RESP=$(kc_post "/api/v1/courses/$COURSE_ID/classes" "{
  \"name\": \"E2E Test Class A1\",
  \"description\": \"Test class for E2E\",
  \"schedule\": \"Mon/Wed 18:00-20:00\",
  \"locationType\": \"ONLINE\",
  \"locationDetail\": \"https://meet.google.com/e2e-test\",
  \"startDate\": \"2026-04-01\",
  \"endDate\": \"2026-05-01\",
  \"maxStudents\": 30
}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "POST /api/v1/courses/{courseId}/classes (create)" 201 "$STATUS" "$BODY"

CLASS_ID=$(json_get "$BODY" "['data']['id']" || echo "")
echo "  Created class ID: $CLASS_ID"

# GET class by ID
RESP=$(kc_get "/api/v1/classes/$CLASS_ID" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/classes/{classId}" 200 "$STATUS" "$BODY"

# GET classes list (under course)
RESP=$(kc_get "/api/v1/courses/$COURSE_ID/classes?page=0&size=10" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status "GET /api/v1/courses/{courseId}/classes (list)" 200 "$STATUS"

echo ""

# ============================================================
# 6. ATTENDANCE (basic endpoint check)
# ============================================================
echo -e "${YELLOW}[6/7] Attendance Endpoints${NC}"

# GET attendance stats for class (should return stats even if empty)
RESP=$(kc_get "/api/v1/attendance/stats/class/${CLASS_ID:-1}" "$REG_SUBDOMAIN" "$INSTANCE_ID" "$ACCESS_TOKEN")
STATUS=$(extract_status "$RESP")
assert_status_one_of "GET /api/v1/attendance/stats/class/{classId}" 200 404 "$STATUS"

echo ""

# ============================================================
# 7. MULTI-TENANT ISOLATION (KC-4)
# ============================================================
echo -e "${YELLOW}[7/7] Multi-tenant Data Isolation${NC}"

# Register tenant-a
TIMESTAMP_A=$(date +%s)
REG_EMAIL_A="e2e-tenant-a-${TIMESTAMP_A}@example.com"
REG_SUBDOMAIN_A="e2e-tnt-a-${TIMESTAMP_A}"

RESP=$(http_post "/api/auth/register" "{
  \"organizationName\": \"Tenant A Org\",
  \"subdomain\": \"$REG_SUBDOMAIN_A\",
  \"ownerEmail\": \"$REG_EMAIL_A\",
  \"ownerPassword\": \"Test@12345\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Register tenant-a" 201 "$STATUS"

TOKEN_A=$(json_get "$BODY" "['accessToken']" || echo "")
INSTANCE_ID_A=$(json_get "$BODY" "['instance']['id']" || echo "")
echo "  Tenant A: $REG_SUBDOMAIN_A (ID: $INSTANCE_ID_A)"

# Register tenant-b
sleep 1  # ensure different timestamp
TIMESTAMP_B=$(date +%s)
REG_EMAIL_B="e2e-tenant-b-${TIMESTAMP_B}@example.com"
REG_SUBDOMAIN_B="e2e-tnt-b-${TIMESTAMP_B}"

RESP=$(http_post "/api/auth/register" "{
  \"organizationName\": \"Tenant B Org\",
  \"subdomain\": \"$REG_SUBDOMAIN_B\",
  \"ownerEmail\": \"$REG_EMAIL_B\",
  \"ownerPassword\": \"Test@12345\"
}")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Register tenant-b" 201 "$STATUS"

TOKEN_B=$(json_get "$BODY" "['accessToken']" || echo "")
INSTANCE_ID_B=$(json_get "$BODY" "['instance']['id']" || echo "")
echo "  Tenant B: $REG_SUBDOMAIN_B (ID: $INSTANCE_ID_B)"

# Wait for KiteClass Core routing for both tenants
for sub in "$REG_SUBDOMAIN_A" "$REG_SUBDOMAIN_B"; do
  for i in 1 2 3 4 5; do
    KC_CHECK=$(curl -sf -o /dev/null -w "%{http_code}" \
      -H "X-Instance-Subdomain: $sub" \
      -H "Authorization: Bearer $TOKEN_A" \
      "$GATEWAY/api/v1/students" 2>/dev/null || echo "000")
    if [ "$KC_CHECK" = "200" ]; then
      break
    fi
    sleep 2
  done
done

# Create student in tenant-a
echo ""
echo "  [Multi-tenant] Creating student in tenant-a..."
RESP=$(kc_post "/api/v1/students" "{
  \"name\": \"Student Alpha (Tenant A)\",
  \"email\": \"alpha-${TIMESTAMP_A}@tenant-a.com\",
  \"phone\": \"0911111111\"
}" "$REG_SUBDOMAIN_A" "$INSTANCE_ID_A" "$TOKEN_A")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Create student in tenant-a" 201 "$STATUS" "$BODY"

STUDENT_A_NAME="Student Alpha (Tenant A)"

# Create student in tenant-b
echo "  [Multi-tenant] Creating student in tenant-b..."
RESP=$(kc_post "/api/v1/students" "{
  \"name\": \"Student Beta (Tenant B)\",
  \"email\": \"beta-${TIMESTAMP_B}@tenant-b.com\",
  \"phone\": \"0922222222\"
}" "$REG_SUBDOMAIN_B" "$INSTANCE_ID_B" "$TOKEN_B")
BODY=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "Create student in tenant-b" 201 "$STATUS" "$BODY"

STUDENT_B_NAME="Student Beta (Tenant B)"

# Verify tenant-a only sees its own student
echo "  [Multi-tenant] Verifying tenant-a sees only its data..."
RESP=$(kc_get "/api/v1/students?search=Alpha" "$REG_SUBDOMAIN_A" "$INSTANCE_ID_A" "$TOKEN_A")
BODY_A=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET students from tenant-a" 200 "$STATUS"

# Check tenant-a has Alpha
TOTAL=$((TOTAL + 1))
HAS_ALPHA=$(echo "$BODY_A" | python3 -c "
import sys, json
d = json.load(sys.stdin)
content = d.get('data', {}).get('content', [])
found = any('Alpha' in s.get('name','') for s in content)
print('yes' if found else 'no')
" 2>/dev/null || echo "no")
if [ "$HAS_ALPHA" = "yes" ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Tenant-a sees Student Alpha"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ Tenant-a cannot see Student Alpha"
  echo -e "  ${RED}✗${NC} Tenant-a cannot see Student Alpha"
fi

# Verify tenant-a does NOT see tenant-b student
echo "  [Multi-tenant] Verifying tenant-a cannot see tenant-b data..."
RESP=$(kc_get "/api/v1/students?search=Beta" "$REG_SUBDOMAIN_A" "$INSTANCE_ID_A" "$TOKEN_A")
BODY_A_CHECK=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET students (search Beta) from tenant-a" 200 "$STATUS"

TOTAL=$((TOTAL + 1))
HAS_BETA_IN_A=$(echo "$BODY_A_CHECK" | python3 -c "
import sys, json
d = json.load(sys.stdin)
content = d.get('data', {}).get('content', [])
found = any('Beta' in s.get('name','') for s in content)
print('yes' if found else 'no')
" 2>/dev/null || echo "no")
if [ "$HAS_BETA_IN_A" = "no" ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Tenant-a does NOT see Tenant-b's Student Beta (isolation confirmed)"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ ISOLATION BREACH: Tenant-a can see Tenant-b's student!"
  echo -e "  ${RED}✗${NC} ISOLATION BREACH: Tenant-a sees Tenant-b's Student Beta!"
fi

# Verify tenant-b only sees its own student
echo "  [Multi-tenant] Verifying tenant-b sees only its data..."
RESP=$(kc_get "/api/v1/students?search=Beta" "$REG_SUBDOMAIN_B" "$INSTANCE_ID_B" "$TOKEN_B")
BODY_B=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET students from tenant-b" 200 "$STATUS"

TOTAL=$((TOTAL + 1))
HAS_BETA=$(echo "$BODY_B" | python3 -c "
import sys, json
d = json.load(sys.stdin)
content = d.get('data', {}).get('content', [])
found = any('Beta' in s.get('name','') for s in content)
print('yes' if found else 'no')
" 2>/dev/null || echo "no")
if [ "$HAS_BETA" = "yes" ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Tenant-b sees Student Beta"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ Tenant-b cannot see Student Beta"
  echo -e "  ${RED}✗${NC} Tenant-b cannot see Student Beta"
fi

# Verify tenant-b does NOT see tenant-a student
echo "  [Multi-tenant] Verifying tenant-b cannot see tenant-a data..."
RESP=$(kc_get "/api/v1/students?search=Alpha" "$REG_SUBDOMAIN_B" "$INSTANCE_ID_B" "$TOKEN_B")
BODY_B_CHECK=$(extract_body "$RESP")
STATUS=$(extract_status "$RESP")
assert_status "GET students (search Alpha) from tenant-b" 200 "$STATUS"

TOTAL=$((TOTAL + 1))
HAS_ALPHA_IN_B=$(echo "$BODY_B_CHECK" | python3 -c "
import sys, json
d = json.load(sys.stdin)
content = d.get('data', {}).get('content', [])
found = any('Alpha' in s.get('name','') for s in content)
print('yes' if found else 'no')
" 2>/dev/null || echo "no")
if [ "$HAS_ALPHA_IN_B" = "no" ]; then
  PASS=$((PASS + 1))
  echo -e "  ${GREEN}✓${NC} Tenant-b does NOT see Tenant-a's Student Alpha (isolation confirmed)"
else
  FAIL=$((FAIL + 1))
  FAILURES="$FAILURES\n  ✗ ISOLATION BREACH: Tenant-b can see Tenant-a's student!"
  echo -e "  ${RED}✗${NC} ISOLATION BREACH: Tenant-b sees Tenant-a's Student Alpha!"
fi

# Test invalid subdomain rejection
echo "  [Multi-tenant] Verifying invalid subdomain is rejected..."
RESP=$(kc_get "/api/v1/students" "nonexistent-subdomain-99999" "" "")
STATUS=$(extract_status "$RESP")
assert_status "Invalid subdomain rejected" 404 "$STATUS"

# Test no subdomain rejection
echo "  [Multi-tenant] Verifying missing subdomain is rejected..."
RESP=$(curl -s -w "\n%{http_code}" "$GATEWAY/api/v1/students")
STATUS=$(extract_status "$RESP")
assert_status "No subdomain rejected" 400 "$STATUS"

# ============================================================
# CLEANUP
# ============================================================
echo ""
echo -e "${YELLOW}[Cleanup] Deleting test tenants${NC}"

http_delete "/api/platform/instances/$INSTANCE_ID" "$ACCESS_TOKEN" > /dev/null 2>&1 || true
http_delete "/api/platform/instances/$INSTANCE_ID_A" "$TOKEN_A" > /dev/null 2>&1 || true
http_delete "/api/platform/instances/$INSTANCE_ID_B" "$TOKEN_B" > /dev/null 2>&1 || true
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
  echo -e "${GREEN}All KiteClass E2E tests passed!${NC}"
  echo ""
  exit 0
fi
