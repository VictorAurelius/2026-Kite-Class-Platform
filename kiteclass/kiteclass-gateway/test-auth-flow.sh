#!/bin/bash

# =====================================================
# KiteClass Gateway - Auth Module Manual Test Script
# PR 1.4 - Authentication Flow Testing
# =====================================================

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "KiteClass Gateway - Auth Flow Testing"
echo "=========================================="
echo ""

# Check if server is running
echo "Checking if server is running..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ Server is not running at $BASE_URL${NC}"
    echo ""
    echo "Please start the server first:"
    echo "  ./mvnw spring-boot:run"
    echo ""
    exit 1
fi
echo -e "${GREEN}✓ Server is running${NC}"
echo ""

# =====================================================
# Test 1: Login with default owner account
# =====================================================
echo "=========================================="
echo "Test 1: Login with Default Owner Account"
echo "=========================================="
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@kiteclass.local",
    "password": "Admin@123"
  }')

echo "Response:"
echo "$LOGIN_RESPONSE" | jq '.'

# Extract tokens
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken // empty')
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.refreshToken // empty')

if [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}❌ Login failed - No access token received${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Login successful${NC}"
echo "Access Token: ${ACCESS_TOKEN:0:50}..."
echo "Refresh Token: ${REFRESH_TOKEN:0:50}..."
echo ""

# =====================================================
# Test 2: Access Protected Endpoint with JWT
# =====================================================
echo "=========================================="
echo "Test 2: Access Protected Endpoint"
echo "=========================================="
USERS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/users" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Response:"
echo "$USERS_RESPONSE" | jq '.'

if echo "$USERS_RESPONSE" | jq -e '.success == true' > /dev/null; then
    echo -e "${GREEN}✓ Protected endpoint accessible with JWT${NC}"
else
    echo -e "${RED}❌ Failed to access protected endpoint${NC}"
fi
echo ""

# =====================================================
# Test 3: Access Protected Endpoint WITHOUT JWT
# =====================================================
echo "=========================================="
echo "Test 3: Access Protected Endpoint WITHOUT JWT"
echo "=========================================="
NO_AUTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/api/v1/users")

HTTP_CODE=$(echo "$NO_AUTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ Correctly rejected with 401 Unauthorized${NC}"
else
    echo -e "${RED}❌ Expected 401, got $HTTP_CODE${NC}"
fi
echo ""

# =====================================================
# Test 4: Refresh Access Token
# =====================================================
echo "=========================================="
echo "Test 4: Refresh Access Token"
echo "=========================================="
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }")

echo "Response:"
echo "$REFRESH_RESPONSE" | jq '.'

NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.data.accessToken // empty')

if [ -z "$NEW_ACCESS_TOKEN" ]; then
    echo -e "${RED}❌ Refresh token failed${NC}"
else
    echo -e "${GREEN}✓ Token refreshed successfully${NC}"
    ACCESS_TOKEN="$NEW_ACCESS_TOKEN"
    REFRESH_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.data.refreshToken // empty')
fi
echo ""

# =====================================================
# Test 5: Test Failed Login Attempts
# =====================================================
echo "=========================================="
echo "Test 5: Failed Login Attempts (Account Locking)"
echo "=========================================="
echo "Creating test user for locking test..."

# Create test user
CREATE_USER=$(curl -s -X POST "$BASE_URL/api/v1/users" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "locktest@example.com",
    "password": "Test@123",
    "name": "Lock Test User",
    "roleIds": [2]
  }')

TEST_USER_ID=$(echo "$CREATE_USER" | jq -r '.data.id // empty')

if [ -z "$TEST_USER_ID" ]; then
    echo -e "${YELLOW}⚠ Could not create test user (may already exist)${NC}"
else
    echo -e "${GREEN}✓ Test user created (ID: $TEST_USER_ID)${NC}"
fi

echo ""
echo "Attempting 5 failed logins..."

for i in {1..5}; do
    echo "Attempt $i/5..."
    FAILED_LOGIN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d '{
        "email": "locktest@example.com",
        "password": "WrongPassword"
      }')

    sleep 1
done

echo ""
echo "Attempting 6th login (should be locked)..."
LOCKED_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "locktest@example.com",
    "password": "WrongPassword"
  }')

echo "Response:"
echo "$LOCKED_RESPONSE" | jq '.'

if echo "$LOCKED_RESPONSE" | grep -q "khóa"; then
    echo -e "${GREEN}✓ Account correctly locked after 5 failed attempts${NC}"
else
    echo -e "${YELLOW}⚠ Account locking behavior unclear${NC}"
fi
echo ""

# =====================================================
# Test 6: Logout (Invalidate Refresh Token)
# =====================================================
echo "=========================================="
echo "Test 6: Logout"
echo "=========================================="
LOGOUT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/api/v1/auth/logout" \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }")

HTTP_CODE=$(echo "$LOGOUT_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" == "204" ]; then
    echo -e "${GREEN}✓ Logout successful (204 No Content)${NC}"
else
    echo -e "${RED}❌ Logout failed, HTTP code: $HTTP_CODE${NC}"
fi
echo ""

# Try to use refresh token after logout
echo "Attempting to use refresh token after logout..."
AFTER_LOGOUT=$(curl -s -X POST "$BASE_URL/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }")

if echo "$AFTER_LOGOUT" | jq -e '.success == false' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Refresh token correctly invalidated after logout${NC}"
else
    echo -e "${YELLOW}⚠ Refresh token invalidation unclear${NC}"
fi
echo ""

# =====================================================
# Test 7: Role-Based Access Control
# =====================================================
echo "=========================================="
echo "Test 7: Role-Based Access Control"
echo "=========================================="
echo "Testing with OWNER role (should have full access)..."

# Try to create a user (requires ADMIN or OWNER)
CREATE_RBAC_TEST=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/api/v1/users" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "rbactest@example.com",
    "password": "Test@123",
    "name": "RBAC Test User",
    "roleIds": [3]
  }')

HTTP_CODE=$(echo "$CREATE_RBAC_TEST" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓ OWNER can create users${NC}"
else
    echo -e "${YELLOW}⚠ OWNER user creation failed (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# =====================================================
# Summary
# =====================================================
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "✓ Completed all auth flow tests"
echo ""
echo "Tests performed:"
echo "  1. Login with default owner account"
echo "  2. Access protected endpoint with JWT"
echo "  3. Reject access without JWT"
echo "  4. Refresh access token"
echo "  5. Account locking after failed attempts"
echo "  6. Logout and token invalidation"
echo "  7. Role-based access control"
echo ""
echo "=========================================="
echo "Manual Testing Complete!"
echo "=========================================="
