#!/bin/bash
#
# KiteHub Seed Data Script
# Creates test users and instances for local development
#
# Usage: ./scripts/seed-data.sh
#
# Creates:
#   - Admin user (admin@kitehub.com)
#   - KiteTeam internal test instance (kiteteam-dev)
#   - KiteTeam demo showcase instance (kiteteam-demo)
#

set -euo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:9000}"
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
NC="\033[0m"

echo ""
echo "=============================================="
echo "  KiteHub Seed Data"
echo "=============================================="
echo ""

# ============================================================
# Helper functions
# ============================================================

register_user() {
  local org_name="$1"
  local subdomain="$2"
  local email="$3"
  local password="$4"

  RESP=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -d "{\"organizationName\":\"$org_name\",\"subdomain\":\"$subdomain\",\"ownerEmail\":\"$email\",\"ownerPassword\":\"$password\"}" \
    "$GATEWAY/api/auth/register")

  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | sed '$d')

  if [ "$STATUS" -eq 201 ]; then
    echo -e "  ${GREEN}✓${NC} Created: $email ($org_name)" >&2
    # Output JSON to stdout for parsing
    echo "$BODY"
    return 0
  elif [ "$STATUS" -eq 400 ]; then
    # Already exists - try login
    RESP=$(curl -s -w "\n%{http_code}" -X POST \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"$email\",\"password\":\"$password\"}" \
      "$GATEWAY/api/auth/login")
    STATUS=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | sed '$d')

    if [ "$STATUS" -eq 200 ]; then
      echo -e "  ${YELLOW}⚠${NC} Already exists: $email (logged in)" >&2
      echo "$BODY"
      return 0
    fi
  fi

  echo -e "  ${RED}✗${NC} Failed to create: $email (HTTP $STATUS)" >&2
  return 1
}

seed_students() {
  local token="$1"
  local instance_id="$2"
  local subdomain="$3"

  local students=(
    '{"name":"Nguyễn Văn An","email":"an@student.com","phone":"0901234567"}'
    '{"name":"Trần Thị Bình","email":"binh@student.com","phone":"0901234568"}'
    '{"name":"Lê Hoàng Cường","email":"cuong@student.com","phone":"0901234569"}'
    '{"name":"Phạm Minh Dũng","email":"dung@student.com","phone":"0901234570"}'
    '{"name":"Hoàng Thị Hoa","email":"hoa@student.com","phone":"0901234571"}'
  )

  local count=0
  for student in "${students[@]}"; do
    RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
      -H "Content-Type: application/json" \
      -H "X-Instance-Subdomain: $subdomain" \
      -H "X-Tenant-Id: $instance_id" \
      -H "X-User-Id: 1" \
      -H "Authorization: Bearer $token" \
      -d "$student" \
      "$GATEWAY/api/v1/students")
    if [ "$RESP" -eq 200 ] || [ "$RESP" -eq 201 ]; then
      count=$((count + 1))
    fi
  done
  echo "       $count students created"
}

seed_teachers() {
  local token="$1"
  local instance_id="$2"
  local subdomain="$3"

  local teachers=(
    '{"name":"Nguyễn Thị Lan","email":"lan@teacher.com","phone":"0911234567","specialization":"English"}'
    '{"name":"Trần Văn Minh","email":"minh@teacher.com","phone":"0911234568","specialization":"Mathematics"}'
    '{"name":"Lê Thị Phương","email":"phuong@teacher.com","phone":"0911234569","specialization":"Science"}'
  )

  local count=0
  for teacher in "${teachers[@]}"; do
    RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
      -H "Content-Type: application/json" \
      -H "X-Instance-Subdomain: $subdomain" \
      -H "X-Tenant-Id: $instance_id" \
      -H "X-User-Id: 1" \
      -H "Authorization: Bearer $token" \
      -d "$teacher" \
      "$GATEWAY/api/v1/teachers")
    if [ "$RESP" -eq 200 ] || [ "$RESP" -eq 201 ]; then
      count=$((count + 1))
    fi
  done
  echo "       $count teachers created"
}

seed_courses() {
  local token="$1"
  local instance_id="$2"
  local subdomain="$3"

  # VN sample data per GAP-538 AC8 + .claude/rules/vn-localization-audit-checklist.md §2 row 3
  # Mix K-12 (Toán/Văn/Lý/KHTN/Sử Địa) + ngoại ngữ + tin học để cover diverse tenant types
  local courses=(
    '{"name":"Toán 6","code":"TOAN06","description":"Toán lớp 6 chương trình GDPT 2018","level":"Cơ bản","category":"Khoa học tự nhiên"}'
    '{"name":"Văn 7","code":"VAN07","description":"Ngữ văn lớp 7 chương trình GDPT 2018","level":"Cơ bản","category":"Khoa học xã hội"}'
    '{"name":"Tiếng Anh Cambridge","code":"ENG-CAM","description":"Tiếng Anh giao tiếp theo khung Cambridge Starters–Movers","level":"Nâng cao","category":"Ngoại ngữ"}'
    '{"name":"Vật lý 10","code":"LY10","description":"Vật lý lớp 10 chương trình GDPT 2018","level":"Nâng cao","category":"Khoa học tự nhiên"}'
    '{"name":"Tin học cơ bản","code":"TINHOC01","description":"Tin học văn phòng + tư duy thuật toán cho học sinh THCS","level":"Cơ bản","category":"Công nghệ"}'
  )

  local count=0
  for course in "${courses[@]}"; do
    RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
      -H "Content-Type: application/json" \
      -H "X-Instance-Subdomain: $subdomain" \
      -H "X-Tenant-Id: $instance_id" \
      -H "X-User-Id: 1" \
      -H "Authorization: Bearer $token" \
      -d "$course" \
      "$GATEWAY/api/v1/courses")
    if [ "$RESP" -eq 200 ] || [ "$RESP" -eq 201 ]; then
      count=$((count + 1))
    fi
  done
  echo "       $count courses created"
}

# ============================================================
# 1. KiteTeam Internal (Dev Testing)
# ============================================================
echo -e "${YELLOW}[1/3] KiteTeam Internal (dev testing)${NC}"

RESULT=$(register_user "KiteTeam Dev" "kiteteam-dev" "dev@kiteteam.com" "KiteTeam@Dev123")
TOKEN=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo "")
INSTANCE_ID=$(echo "$RESULT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('instance',d.get('instances',[{}])[0] if d.get('instances') else {}).get('id',''))" 2>/dev/null || echo "")

if [ -n "$TOKEN" ] && [ "$TOKEN" != "None" ] && [ -n "$INSTANCE_ID" ] && [ "$INSTANCE_ID" != "None" ]; then
  seed_students "$TOKEN" "$INSTANCE_ID" "kiteteam-dev"
  seed_teachers "$TOKEN" "$INSTANCE_ID" "kiteteam-dev"
  seed_courses "$TOKEN" "$INSTANCE_ID" "kiteteam-dev"
else
  echo -e "  ${YELLOW}⚠${NC} Skipping data seed (no token/instance)"
fi

# ============================================================
# 2. KiteTeam Demo (Showcase)
# ============================================================
echo ""
echo -e "${YELLOW}[2/3] KiteTeam Demo (showcase)${NC}"

RESULT=$(register_user "KiteTeam Demo School" "kiteteam-demo" "demo@kiteteam.com" "KiteTeam@Demo123")
TOKEN=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo "")
INSTANCE_ID=$(echo "$RESULT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('instance',d.get('instances',[{}])[0] if d.get('instances') else {}).get('id',''))" 2>/dev/null || echo "")

if [ -n "$TOKEN" ] && [ "$TOKEN" != "None" ] && [ -n "$INSTANCE_ID" ] && [ "$INSTANCE_ID" != "None" ]; then
  seed_students "$TOKEN" "$INSTANCE_ID" "kiteteam-demo"
  seed_teachers "$TOKEN" "$INSTANCE_ID" "kiteteam-demo"
  seed_courses "$TOKEN" "$INSTANCE_ID" "kiteteam-demo"
else
  echo -e "  ${YELLOW}⚠${NC} Skipping data seed (no token/instance)"
fi

# ============================================================
# 3. Admin User
# ============================================================
echo ""
echo -e "${YELLOW}[3/3] Admin User${NC}"

register_user "KiteHub Admin" "admin-portal" "admin@kitehub.com" "Admin@KiteHub123" > /dev/null 2>&1 && \
  echo -e "  ${GREEN}✓${NC} Admin user: admin@kitehub.com" || \
  echo -e "  ${YELLOW}⚠${NC} Admin user already exists"

# ============================================================
# Summary
# ============================================================
echo ""
echo "=============================================="
echo "  Seed Data Complete!"
echo "=============================================="
echo ""
echo "  Test Accounts:"
echo "    dev@kiteteam.com     / KiteTeam@Dev123   (KiteTeam Dev)"
echo "    demo@kiteteam.com    / KiteTeam@Demo123  (KiteTeam Demo)"
echo "    admin@kitehub.com    / Admin@KiteHub123  (Admin)"
echo ""
echo "  Instances:"
echo "    kiteteam-dev   → Internal testing (5 students VN, 3 teachers VN, 5 courses VN K-12+ngoại ngữ)"
echo "    kiteteam-demo  → Showcase demo"
echo "    admin-portal   → Admin dashboard"
echo ""
