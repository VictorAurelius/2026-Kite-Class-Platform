#!/bin/bash
#
# Seed demo data for tenant "Trung tâm Anh ngữ Sky Education" (subdomain: sky-education)
# Matches thesis Chương 3 narrative: 78 học viên · 5 khóa học · ~4 giảng viên + classes + enrollments.
#
# Usage: GATEWAY_URL=http://localhost:9000 ./scripts/seed-demo-sky-education.sh
#
# Prerequisites:
#   - Tenant sky-education already exists (owner@skyedu.vn / SkyEdu@2026)
#   - kiteclass-core healthy (RabbitMQ queues class.rescheduled.queue + class.rescheduled.email.queue declared)
#
# Idempotency note: this script is additive. Run the optional --clean flag against the DB first
# if re-seeding (see README block at bottom). Students/courses use unique email/code so duplicates
# are rejected by the API (HTTP 409/400) on re-run — counts will not double.
#
set -uo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:9000}"
SUBDOMAIN="sky-education"
OWNER_EMAIL="owner@skyedu.vn"
OWNER_PASSWORD="SkyEdu@2026"

GREEN="\033[0;32m"; RED="\033[0;31m"; YELLOW="\033[1;33m"; NC="\033[0m"

echo "=============================================="
echo "  Seed Demo Data — Sky Education"
echo "=============================================="

# ------------------------------------------------------------
# 1. Login → token + instance_id
# ------------------------------------------------------------
RESP=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"email\":\"$OWNER_EMAIL\",\"password\":\"$OWNER_PASSWORD\"}" \
  "$GATEWAY/api/auth/login")

TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
INSTANCE_ID=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['instances'][0]['id'])" 2>/dev/null)

if [ -z "$TOKEN" ] || [ -z "$INSTANCE_ID" ]; then
  echo -e "${RED}✗ Login failed — cannot obtain token/instance_id${NC}"
  echo "$RESP" | head -3
  exit 1
fi
echo -e "${GREEN}✓${NC} Logged in. instance_id=$INSTANCE_ID"

# Common headers for tenant-scoped calls (X-User-Id=1 = owner audit actor per seed-data.sh convention)
H_TENANT=(-H "X-Instance-Subdomain: $SUBDOMAIN" -H "X-Tenant-Id: $INSTANCE_ID" -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

post() { # post <path> <json>  -> echoes "HTTP_CODE\nBODY"
  curl -s -w "\n%{http_code}" "${H_TENANT[@]}" -X POST -d "$2" "$GATEWAY$1"
}

# ------------------------------------------------------------
# 2. Teachers (must be created BEFORE courses — course requires teacherId NotNull)
# ------------------------------------------------------------
echo -e "${YELLOW}[1/5] Teachers${NC}"
declare -a TEACHER_IDS=()
TEACHERS=(
  '{"name":"Nguyễn Thị Lan Anh","email":"lananh@teacher.skyedu.vn","phoneNumber":"0912345001","specialization":"IELTS Speaking & Writing","qualification":"MA TESOL","experienceYears":8}'
  '{"name":"Trần Quốc Bảo","email":"quocbao@teacher.skyedu.vn","phoneNumber":"0912345002","specialization":"Business English","qualification":"BA English Studies","experienceYears":6}'
  '{"name":"Lê Thị Mỹ Duyên","email":"myduyen@teacher.skyedu.vn","phoneNumber":"0912345003","specialization":"Tiếng Anh thiếu nhi","qualification":"BA Education","experienceYears":5}'
  '{"name":"Phạm Hoàng Nam","email":"hoangnam@teacher.skyedu.vn","phoneNumber":"0912345004","specialization":"TOEIC & Grammar","qualification":"MA Applied Linguistics","experienceYears":7}'
)
for t in "${TEACHERS[@]}"; do
  R=$(post "/api/v1/teachers" "$t")
  CODE=$(echo "$R" | tail -1); BODY=$(echo "$R" | sed '$d')
  if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
    TID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null)
    TEACHER_IDS+=("$TID")
    echo -e "  ${GREEN}✓${NC} teacher id=$TID"
  else
    echo -e "  ${RED}✗${NC} teacher HTTP $CODE: $(echo "$BODY" | head -c 120)"
  fi
done
echo "      ${#TEACHER_IDS[@]} teachers created"

# Fallback teacher id if any teacher failed (use first available, else 1)
DEFAULT_TID="${TEACHER_IDS[0]:-1}"

# ------------------------------------------------------------
# 3. Courses (5 — matches narrative "5 khóa học")
# ------------------------------------------------------------
echo -e "${YELLOW}[2/5] Courses${NC}"
declare -a COURSE_IDS=()
# teacherId assigned round-robin from seeded teachers
COURSES=(
  "English Basics|ENG101|Khóa tiếng Anh nền tảng cho người mới bắt đầu|Beginner|Language|1200000"
  "IELTS Preparation|IELTS01|Luyện thi IELTS mục tiêu 6.5-7.0|Intermediate|Language|3500000"
  "Business English|BIZ01|Tiếng Anh thương mại cho người đi làm|Advanced|Language|2800000"
  "Kids English|KIDS01|Tiếng Anh vui nhộn cho trẻ 6-11 tuổi|Beginner|Language|1500000"
  "TOEIC Intensive|TOEIC01|Luyện thi TOEIC cấp tốc mục tiêu 650+|Intermediate|Language|2200000"
)
i=0
for c in "${COURSES[@]}"; do
  IFS='|' read -r name code desc level cat price <<< "$c"
  tid="${TEACHER_IDS[$((i % ${#TEACHER_IDS[@]}))]:-$DEFAULT_TID}"
  json="{\"name\":\"$name\",\"code\":\"$code\",\"description\":\"$desc\",\"level\":\"$level\",\"category\":\"$cat\",\"teacherId\":$tid,\"price\":$price,\"durationWeeks\":12,\"totalSessions\":24}"
  R=$(post "/api/v1/courses" "$json")
  CODE=$(echo "$R" | tail -1); BODY=$(echo "$R" | sed '$d')
  if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
    CID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null)
    COURSE_IDS+=("$CID")
    echo -e "  ${GREEN}✓${NC} course $code id=$CID (teacher=$tid)"
  else
    echo -e "  ${RED}✗${NC} course $code HTTP $CODE: $(echo "$BODY" | head -c 120)"
  fi
  i=$((i + 1))
done
echo "      ${#COURSE_IDS[@]} courses created"

# ------------------------------------------------------------
# 4. Students (78 — matches narrative "78 học viên")
#    MUST include 3 narrative names from Hình 3.3.
# ------------------------------------------------------------
echo -e "${YELLOW}[3/5] Students (78)${NC}"

# 3 mandatory narrative names first
MANDATORY_NAMES=("Bùi Văn Dũng" "Cao Văn Sơn" "Châu Thị Bích")

# Generate 75 additional VN-style names via python (deterministic order)
mapfile -t GEN_NAMES < <(python3 - <<'PYEOF'
ho = ["Nguyễn","Trần","Lê","Phạm","Hoàng","Huỳnh","Phan","Vũ","Đặng","Bùi","Đỗ","Hồ","Ngô","Dương","Lý"]
dem_nam = ["Văn","Hữu","Đức","Minh","Quang","Thành","Công","Bá","Xuân","Đình"]
dem_nu = ["Thị","Thanh","Ngọc","Thu","Kim","Mỹ","Hồng","Diễm","Hương","Phương"]
ten_nam = ["An","Bình","Cường","Dũng","Hải","Khoa","Long","Nam","Phong","Quân","Sơn","Tài","Tuấn","Việt","Hùng"]
ten_nu = ["Anh","Chi","Dung","Hoa","Hà","Lan","Linh","Mai","Nhung","Oanh","Quỳnh","Thảo","Trang","Vy","Yến"]
import itertools
names = []
# deterministic spread: alternate nam/nu, cycle through pools
for k in range(75):
    if k % 2 == 0:
        n = f"{ho[k % len(ho)]} {dem_nam[(k//2) % len(dem_nam)]} {ten_nam[(k//3) % len(ten_nam)]}"
    else:
        n = f"{ho[(k+3) % len(ho)]} {dem_nu[(k//2) % len(dem_nu)]} {ten_nu[(k//3) % len(ten_nu)]}"
    names.append(n)
for n in names:
    print(n)
PYEOF
)

ALL_NAMES=("${MANDATORY_NAMES[@]}" "${GEN_NAMES[@]}")

slugify() { # ASCII slug from VN name
  python3 - "$1" <<'PYEOF'
import sys, unicodedata, re
s = sys.argv[1]
s = s.replace("đ","d").replace("Đ","D")
s = unicodedata.normalize("NFD", s)
s = "".join(c for c in s if unicodedata.category(c) != "Mn")
s = re.sub(r"[^a-zA-Z]+", ".", s).strip(".").lower()
print(s)
PYEOF
}

SCOUNT=0
idx=0
for nm in "${ALL_NAMES[@]}"; do
  slug=$(slugify "$nm")
  # ensure uniqueness with index suffix
  email="${slug}.${idx}@student.skyedu.vn"
  # phone: 09 + 8 digits derived from idx
  phone=$(printf "09%08d" $((10000000 + idx)))
  json="{\"name\":\"$nm\",\"email\":\"$email\",\"phone\":\"$phone\"}"
  R=$(post "/api/v1/students" "$json")
  CODE=$(echo "$R" | tail -1)
  if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
    SCOUNT=$((SCOUNT + 1))
  else
    echo -e "  ${RED}✗${NC} student '$nm' HTTP $CODE"
  fi
  idx=$((idx + 1))
done
echo "      $SCOUNT / ${#ALL_NAMES[@]} students created"

# ------------------------------------------------------------
# 5. Classes (1 per course) + enrollments (sample for dashboard realism)
# ------------------------------------------------------------
echo -e "${YELLOW}[4/5] Classes${NC}"
declare -a CLASS_IDS=()
START_DATE=$(date -d "+7 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
END_DATE=$(date -d "+90 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
CLASS_NAMES=("Lớp Anh ngữ 5A1" "Lớp IELTS 7.0 Buổi tối" "Lớp Business English Sáng" "Lớp Kids English K1" "Lớp TOEIC Cấp tốc")
ci=0
for cid in "${COURSE_IDS[@]}"; do
  cname="${CLASS_NAMES[$ci]:-Lớp học $cid}"
  json="{\"name\":\"$cname\",\"description\":\"Lớp demo Sky Education\",\"schedule\":\"Thứ 2-4-6, 18:00-19:30\",\"locationDetail\":\"Cơ sở 1 - 123 Lê Lợi, Q.1, TP.HCM\",\"startDate\":\"$START_DATE\",\"endDate\":\"$END_DATE\",\"maxStudents\":30}"
  R=$(post "/api/v1/courses/$cid/classes" "$json")
  CODE=$(echo "$R" | tail -1); BODY=$(echo "$R" | sed '$d')
  if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
    CLID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null)
    CLASS_IDS+=("$CLID")
    echo -e "  ${GREEN}✓${NC} class '$cname' id=$CLID (course=$cid)"
  else
    echo -e "  ${RED}✗${NC} class '$cname' HTTP $CODE: $(echo "$BODY" | head -c 120)"
  fi
  ci=$((ci + 1))
done
echo "      ${#CLASS_IDS[@]} classes created"

# Enrollments: enroll first ~12 students into classes round-robin
echo -e "${YELLOW}[5/5] Enrollments (sample)${NC}"
ECOUNT=0
# tuition per class index (matches COURSES price order)
ENROLL_TUITION=(1500000 3500000 2800000 1500000 2200000)
if [ "${#CLASS_IDS[@]}" -gt 0 ]; then
  # fetch first 15 student ids (response shape: data.content[])
  SIDS=$(curl -s "${H_TENANT[@]}" "$GATEWAY/api/v1/students?page=0&size=15" \
    | python3 -c "import sys,json; print(' '.join(str(s['id']) for s in json.load(sys.stdin)['data']['content']))" 2>/dev/null)
  ei=0
  for sid in $SIDS; do
    k=$((ei % ${#CLASS_IDS[@]}))
    clid="${CLASS_IDS[$k]}"
    tu="${ENROLL_TUITION[$k]:-1500000}"
    json="{\"studentId\":$sid,\"classId\":$clid,\"tuitionAmount\":$tu}"
    R=$(post "/api/v1/enrollments" "$json")
    CODE=$(echo "$R" | tail -1)
    if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then ECOUNT=$((ECOUNT + 1)); fi
    ei=$((ei + 1))
  done
fi
echo "      $ECOUNT enrollments created"

echo "=============================================="
echo "  Seed Complete — Sky Education"
echo "  instance_id = $INSTANCE_ID"
echo "=============================================="

# ------------------------------------------------------------
# Re-seed cleanup (run manually against DB if needed):
#   INST=<instance_id>
#   docker exec kite-postgres psql -U kitehub -d kiteclass_shared -c \
#     "DELETE FROM enrollments WHERE student_id IN (SELECT id FROM students WHERE instance_id='$INST'); \
#      DELETE FROM classes WHERE instance_id='$INST'; \
#      DELETE FROM courses WHERE instance_id='$INST'; \
#      DELETE FROM teachers WHERE instance_id='$INST'; \
#      DELETE FROM students WHERE instance_id='$INST';"
# ------------------------------------------------------------
