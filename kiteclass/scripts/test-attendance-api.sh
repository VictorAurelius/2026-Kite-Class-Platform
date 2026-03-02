#!/bin/bash

# Test Attendance API - Manual Testing Script
# PR 3.8 Frontend + PR 2.7 Backend Integration

BASE_URL="http://localhost:8081/api/v1"
TENANT_ID="550e8400-e29b-41d4-a716-446655440000"

echo "=========================================="
echo "Attendance API Integration Test"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    echo -e "${YELLOW}Testing: ${description}${NC}"
    echo "Endpoint: ${method} ${endpoint}"

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" \
            -H "X-Tenant-Id: ${TENANT_ID}" \
            "${BASE_URL}${endpoint}")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" \
            -X POST \
            -H "Content-Type: application/json" \
            -H "X-Tenant-Id: ${TENANT_ID}" \
            -d "${data}" \
            "${BASE_URL}${endpoint}")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo -e "${GREEN}✓ SUCCESS (HTTP ${http_code})${NC}"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        echo -e "${RED}✗ FAILED (HTTP ${http_code})${NC}"
        echo "$body"
    fi
    echo ""
}

# Check backend health
echo "1. Checking backend health..."
curl -s http://localhost:8081/actuator/health | jq '.' || echo "Backend not running"
echo ""

# Test 1: Get all students
test_endpoint "GET" "/students?page=0&size=5" "" "Get students list"

# Test 2: Get all classes
test_endpoint "GET" "/classes?page=0&size=5" "" "Get classes list"

# Test 3: Get enrollments for a class (assuming class ID = 1)
test_endpoint "GET" "/enrollments/class/1?page=0&size=10" "" "Get enrollments for class 1"

# Test 4: Mark single attendance
ATTENDANCE_DATA='{
  "enrollmentId": 1,
  "sessionId": 1,
  "status": "PRESENT",
  "notes": "Test attendance from API"
}'
test_endpoint "POST" "/attendance" "$ATTENDANCE_DATA" "Mark single attendance"

# Test 5: Mark bulk attendance
BULK_DATA='{
  "sessionId": 1,
  "records": [
    {"enrollmentId": 1, "status": "PRESENT"},
    {"enrollmentId": 2, "status": "ABSENT", "notes": "Sick"},
    {"enrollmentId": 3, "status": "LATE"}
  ]
}'
test_endpoint "POST" "/attendance/bulk" "$BULK_DATA" "Mark bulk attendance"

# Test 6: Get attendance by session
test_endpoint "GET" "/attendance/session/1?page=0&size=10" "" "Get session attendance"

# Test 7: Get student statistics
test_endpoint "GET" "/attendance/stats/student/1" "" "Get student attendance stats"

# Test 8: Get class statistics
test_endpoint "GET" "/attendance/stats/class/1" "" "Get class attendance stats"

echo "=========================================="
echo "Test completed!"
echo "=========================================="
