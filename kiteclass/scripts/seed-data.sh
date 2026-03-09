#!/bin/bash
#
# Seed sample data for testing
# Tạo dữ liệu mẫu để test
#
# Usage: ./scripts/seed-data.sh [access_token] [tenant_id]
#

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

GATEWAY_URL="http://localhost:8080"
ACCESS_TOKEN="${1:-}"
TENANT_ID="${2:-}"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}📊 Seed Sample Data${NC}"
echo -e "${BLUE}================================${NC}\n"

# Nếu không có token, hướng dẫn user
if [ -z "$ACCESS_TOKEN" ] || [ -z "$TENANT_ID" ]; then
    echo -e "${YELLOW}⚠️  Cần access token và tenant ID để tạo dữ liệu${NC}\n"
    echo -e "${BLUE}Cách lấy token:${NC}"
    echo -e "1. Truy cập http://localhost:3000/login"
    echo -e "2. Đăng nhập với tài khoản"
    echo -e "3. Mở DevTools (F12) → Application → Local Storage"
    echo -e "4. Copy 'accessToken' và 'tenantId'\n"
    echo -e "${BLUE}Sau đó chạy:${NC}"
    echo -e "  ${GREEN}./scripts/seed-data.sh YOUR_TOKEN YOUR_TENANT_ID${NC}\n"
    exit 0
fi

echo -e "${GREEN}🔑 Token: ${ACCESS_TOKEN:0:20}...${NC}"
echo -e "${GREEN}🏢 Tenant: $TENANT_ID${NC}\n"

# Function để tạo student
create_student() {
    local name=$1
    local email=$2
    local phone=$3
    local gender=$4

    echo -e "${YELLOW}👤 Tạo học viên: $name...${NC}"

    response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/api/v1/students" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Tenant-Id: $TENANT_ID" \
        -d "{
            \"name\": \"$name\",
            \"email\": \"$email\",
            \"phone\": \"$phone\",
            \"dateOfBirth\": \"2005-03-15\",
            \"gender\": \"$gender\",
            \"address\": \"123 Đường ABC, Quận 1, TP.HCM\"
        }")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 201 ] || [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✅ Đã tạo: $name${NC}"
    else
        echo -e "${RED}❌ Lỗi ($http_code): $body${NC}"
    fi
}

# Function để tạo teacher
create_teacher() {
    local name=$1
    local email=$2
    local phone=$3
    local specialization=$4

    echo -e "${YELLOW}👨‍🏫 Tạo giáo viên: $name...${NC}"

    response=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/api/v1/teachers" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Tenant-Id: $TENANT_ID" \
        -d "{
            \"name\": \"$name\",
            \"email\": \"$email\",
            \"phone\": \"$phone\",
            \"specialization\": \"$specialization\",
            \"qualifications\": \"Bachelor of Education\",
            \"hireDate\": \"2020-01-15\"
        }")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 201 ] || [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✅ Đã tạo: $name${NC}"
    else
        echo -e "${RED}❌ Lỗi ($http_code): $body${NC}"
    fi
}

# Tạo dữ liệu mẫu
echo -e "${BLUE}📝 Tạo học viên mẫu...${NC}\n"

create_student "Nguyễn Văn An" "nguyenvanan@example.com" "0901234567" "MALE"
create_student "Trần Thị Bình" "tranthib@example.com" "0902234567" "FEMALE"
create_student "Lê Hoàng Cường" "lehoangcuong@example.com" "0903234567" "MALE"
create_student "Phạm Thị Dung" "phamthidung@example.com" "0904234567" "FEMALE"
create_student "Võ Minh Khang" "vominhkhang@example.com" "0905234567" "MALE"

echo -e "\n${BLUE}📝 Tạo giáo viên mẫu...${NC}\n"

create_teacher "Nguyễn Văn A" "nguyenvana.teacher@example.com" "0911234567" "English Grammar"
create_teacher "Trần Thị B" "tranthib.teacher@example.com" "0912234567" "IELTS Preparation"
create_teacher "Lê Văn C" "levanc.teacher@example.com" "0913234567" "TOEIC"

echo -e "\n${GREEN}================================${NC}"
echo -e "${GREEN}✅ Đã tạo xong dữ liệu mẫu!${NC}"
echo -e "${GREEN}================================${NC}\n"

echo -e "${BLUE}📍 Kiểm tra:${NC}"
echo -e "  Students: ${GREEN}http://localhost:3000/students${NC}"
echo -e "  Teachers: ${GREEN}http://localhost:3000/teachers${NC}\n"
