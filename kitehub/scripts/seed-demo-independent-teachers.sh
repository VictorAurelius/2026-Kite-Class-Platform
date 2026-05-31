#!/bin/bash
#
# Seed demo data cho 3 GIẢNG VIÊN ĐỘC LẬP (KHÔNG phải trung tâm) — chủ thể thesis Chương 3.
#
#   1. Cô Đỗ Lan Khánh   — THPT, Pháp luật & Đời sống — TENANT CHÍNH (Hình 3.x).
#                          Theme navy+gold. Lớp "Lớp Pháp luật 12A1". ~30 học viên.
#   2. Cô Nguyễn Thị Hà   — Tiểu học, Toán — gói FREE. Theme blue. (gọn)
#   3. Thầy Nguyễn Đình Nhì — THCS, Hóa — gói PREMIUM. Theme green. (gọn)
#
# Mỗi giảng viên là 1 tenant độc lập (1 người dạy nhiều lớp, không có nhân viên quản lý).
#
# Usage: GATEWAY_URL=http://localhost:9000 ./scripts/seed-demo-independent-teachers.sh
#        SEED_TENANTS=khanh ./scripts/seed-demo-independent-teachers.sh   # chỉ seed tenant chính
#
# Prerequisites:
#   - Stack local chạy (gateway :9000 healthy). Script tự provision tenant qua
#     dev register shortcut POST /api/auth/register (không cần tạo tenant trước).
#   - kiteclass-core healthy (RabbitMQ queues class.rescheduled.queue + class.rescheduled.email.queue declared).
#
# Idempotency note: script additive. Owner account + teacher/course/student dùng email/code unique
# nên trùng bị API từ chối (HTTP 409/400) khi chạy lại — số lượng không nhân đôi. Để re-seed sạch:
# xoá teacher/course/class/student/enrollment của tenant trong DB kiteclass_<uuid8> + kiteclass_shared
# rồi chạy lại.
#
set -uo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:9000}"
# Tập tenant cần seed (mặc định cả 3). Override: SEED_TENANTS="khanh ha nhi" hoặc "khanh".
SEED_TENANTS="${SEED_TENANTS:-khanh ha nhi}"

GREEN="\033[0;32m"; RED="\033[0;31m"; YELLOW="\033[1;33m"; NC="\033[0m"

echo "=============================================="
echo "  Seed Demo Data — 3 Giảng viên độc lập"
echo "=============================================="

# ------------------------------------------------------------
# Hàm seed 1 tenant. Tham số (positional):
#   $1 subdomain  $2 owner_email  $3 owner_password
#   $4 teacher_json  $5 course_spec (name|code|desc|level|category|price)
#   $6 class_name    $7 num_students
#   $8 theme_primary_hex  $9 theme_secondary_hex (metadata — branding set qua KiteHub admin)
# ------------------------------------------------------------
seed_tenant() {
  local SUBDOMAIN="$1" OWNER_EMAIL="$2" OWNER_PASSWORD="$3"
  local TEACHER_JSON="$4" COURSE_SPEC="$5" CLASS_NAME="$6" NUM_STUDENTS="$7"
  local THEME_PRIMARY="$8" THEME_SECONDARY="$9"

  echo "----------------------------------------------"
  echo -e "  Tenant: ${YELLOW}$SUBDOMAIN${NC}  (theme primary=$THEME_PRIMARY secondary=$THEME_SECONDARY)"
  echo "----------------------------------------------"

  # 0. Provision tenant + owner account qua dev register shortcut (idempotent —
  #    nếu đã tồn tại thì register trả 409/400, bỏ qua, login bước 1 vẫn chạy).
  local ORG_NAME
  ORG_NAME=$(echo "$TEACHER_JSON" | python3 -c "import sys,json; print('Lớp học ' + json.load(sys.stdin)['name'])" 2>/dev/null)
  [ -z "$ORG_NAME" ] && ORG_NAME="Lớp học $SUBDOMAIN"
  local REG_CODE
  REG_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" \
    -d "{\"organizationName\":\"$ORG_NAME\",\"subdomain\":\"$SUBDOMAIN\",\"ownerEmail\":\"$OWNER_EMAIL\",\"ownerPassword\":\"$OWNER_PASSWORD\"}" \
    "$GATEWAY/api/auth/register" 2>/dev/null)
  if [ "$REG_CODE" = "200" ] || [ "$REG_CODE" = "201" ]; then
    echo -e "  ${GREEN}✓${NC} Đăng ký tenant mới: $ORG_NAME ($OWNER_EMAIL)"
  else
    echo -e "  ${YELLOW}⚠${NC} Register HTTP $REG_CODE (có thể đã tồn tại) — thử login"
  fi

  # 1. Login → token + instance_id
  local RESP TOKEN INSTANCE_ID
  RESP=$(curl -s -X POST -H "Content-Type: application/json" \
    -d "{\"email\":\"$OWNER_EMAIL\",\"password\":\"$OWNER_PASSWORD\"}" \
    "$GATEWAY/api/auth/login")
  TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
  INSTANCE_ID=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['instances'][0]['id'])" 2>/dev/null)

  if [ -z "$TOKEN" ] || [ -z "$INSTANCE_ID" ]; then
    echo -e "${RED}✗ Login thất bại ($OWNER_EMAIL) — bỏ qua tenant $SUBDOMAIN${NC}"
    echo "$RESP" | head -3
    return 1
  fi
  echo -e "${GREEN}✓${NC} Đăng nhập OK. instance_id=$INSTANCE_ID"

  # Branding theme = metadata cho narrative thesis. Set màu thực tế qua KiteHub admin
  # (BrandingPackageController hiện chỉ GET; theme writer nằm ở kitehub-branding).
  # Lưu lại để doc/screenshot Chương 3 dùng đúng màu mỗi GV.
  echo -e "      theme: primary=$THEME_PRIMARY · secondary=$THEME_SECONDARY"

  local H_TENANT=(-H "X-Instance-Subdomain: $SUBDOMAIN" -H "X-Tenant-Id: $INSTANCE_ID" -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
  local post; post() { curl -s -w "\n%{http_code}" "${H_TENANT[@]}" -X POST -d "$2" "$GATEWAY$1"; }

  # 2. Teacher (1 — GV độc lập tự dạy)
  echo -e "${YELLOW}[1/5] Giảng viên${NC}"
  local R CODE BODY TID
  R=$(post "/api/v1/teachers" "$TEACHER_JSON")
  CODE=$(echo "$R" | tail -1); BODY=$(echo "$R" | sed '$d')
  if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
    TID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null)
    echo -e "  ${GREEN}✓${NC} giảng viên id=$TID"
  else
    # Idempotent: 409 đã tồn tại → fetch id theo email
    local TEMAIL; TEMAIL=$(echo "$TEACHER_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['email'])" 2>/dev/null)
    TID=$(curl -s "${H_TENANT[@]}" "$GATEWAY/api/v1/teachers?page=0&size=100" \
      | python3 -c "import sys,json;d=json.load(sys.stdin);c=d.get('data',{});c=c.get('content',c) if isinstance(c,dict) else c;print(next((str(t['id']) for t in c if t.get('email')=='$TEMAIL'),''))" 2>/dev/null)
    [ -z "$TID" ] && TID=1
    echo -e "  ${YELLOW}⚠${NC} giảng viên đã tồn tại (HTTP $CODE) → dùng id=$TID"
  fi

  # 3. Course (1 — môn của GV)
  echo -e "${YELLOW}[2/5] Khóa học${NC}"
  local name code desc level cat price json CID
  IFS='|' read -r name code desc level cat price <<< "$COURSE_SPEC"
  json="{\"name\":\"$name\",\"code\":\"$code\",\"description\":\"$desc\",\"level\":\"$level\",\"category\":\"$cat\",\"teacherId\":$TID,\"price\":$price,\"durationWeeks\":12,\"totalSessions\":24}"
  R=$(post "/api/v1/courses" "$json")
  CODE=$(echo "$R" | tail -1); BODY=$(echo "$R" | sed '$d')
  if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
    CID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null)
    echo -e "  ${GREEN}✓${NC} khóa $code id=$CID"
  else
    # Idempotent: 409 đã tồn tại → fetch id theo code (KHÔNG return — students vẫn cần seed)
    CID=$(curl -s "${H_TENANT[@]}" "$GATEWAY/api/v1/courses?page=0&size=100" \
      | python3 -c "import sys,json;d=json.load(sys.stdin);c=d.get('data',{});c=c.get('content',c) if isinstance(c,dict) else c;print(next((str(x['id']) for x in c if x.get('code')=='$code'),''))" 2>/dev/null)
    echo -e "  ${YELLOW}⚠${NC} khóa $code đã tồn tại (HTTP $CODE) → dùng id=$CID"
  fi

  # 4. Students (NUM_STUDENTS — tên VN, deterministic)
  echo -e "${YELLOW}[3/5] Học viên ($NUM_STUDENTS)${NC}"
  local GEN_NAMES SCOUNT idx slug email phone
  mapfile -t GEN_NAMES < <(python3 - "$NUM_STUDENTS" "$SUBDOMAIN" <<'PYEOF'
import sys
n = int(sys.argv[1]); seed = sys.argv[2]
ho = ["Nguyễn","Trần","Lê","Phạm","Hoàng","Huỳnh","Phan","Vũ","Đặng","Bùi","Đỗ","Hồ","Ngô","Dương","Lý"]
dem_nam = ["Văn","Hữu","Đức","Minh","Quang","Thành","Công","Bá","Xuân","Đình"]
dem_nu = ["Thị","Thanh","Ngọc","Thu","Kim","Mỹ","Hồng","Diễm","Hương","Phương"]
ten_nam = ["An","Bình","Cường","Dũng","Hải","Khoa","Long","Nam","Phong","Quân","Sơn","Tài","Tuấn","Việt","Hùng"]
ten_nu = ["Anh","Chi","Dung","Hoa","Hà","Lan","Linh","Mai","Nhung","Oanh","Quỳnh","Thảo","Trang","Vy","Yến"]
off = sum(ord(c) for c in seed) % 7  # lệch nhẹ theo tenant để tên không trùng tuyệt đối
for k in range(n):
    j = k + off
    if j % 2 == 0:
        print(f"{ho[j % len(ho)]} {dem_nam[(j//2) % len(dem_nam)]} {ten_nam[(j//3) % len(ten_nam)]}")
    else:
        print(f"{ho[(j+3) % len(ho)]} {dem_nu[(j//2) % len(dem_nu)]} {ten_nu[(j//3) % len(ten_nu)]}")
PYEOF
)
  slug() { python3 - "$1" <<'PYEOF'
import sys, unicodedata, re
s = sys.argv[1].replace("đ","d").replace("Đ","D")
s = unicodedata.normalize("NFD", s)
s = "".join(c for c in s if unicodedata.category(c) != "Mn")
print(re.sub(r"[^a-zA-Z]+", ".", s).strip(".").lower())
PYEOF
  }
  # Phone base riêng theo tenant (cksum subdomain) → tránh collision cross-tenant
  # (lỗi cũ: mọi tenant idx 0 đều = 0910000000 → STUDENT_PHONE_EXISTS 409).
  local TBASE; TBASE=$(echo -n "$SUBDOMAIN" | cksum | cut -d' ' -f1); TBASE=$((20000000 + (TBASE % 70000000)))
  SCOUNT=0; idx=0
  for nm in "${GEN_NAMES[@]}"; do
    local sl; sl=$(slug "$nm")
    email="${sl}.${idx}@hocvien.${SUBDOMAIN}.vn"
    phone=$(printf "09%08d" $((TBASE + idx)))
    json="{\"name\":\"$nm\",\"email\":\"$email\",\"phone\":\"$phone\"}"
    R=$(post "/api/v1/students" "$json"); CODE=$(echo "$R" | tail -1)
    if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then SCOUNT=$((SCOUNT + 1)); fi
    idx=$((idx + 1))
  done
  echo "      $SCOUNT / ${#GEN_NAMES[@]} học viên"

  # 5. Class (1 — lớp của GV) + enrollments mẫu
  echo -e "${YELLOW}[4/5] Lớp học${NC}"
  local START_DATE END_DATE CLID
  START_DATE=$(date -d "+7 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
  END_DATE=$(date -d "+90 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
  json="{\"name\":\"$CLASS_NAME\",\"description\":\"Lớp demo $SUBDOMAIN\",\"schedule\":\"Thứ 2-4-6, 18:00-19:30\",\"locationDetail\":\"45 Hai Bà Trưng, Hà Nội\",\"startDate\":\"$START_DATE\",\"endDate\":\"$END_DATE\",\"maxStudents\":40}"
  R=$(post "/api/v1/courses/$CID/classes" "$json")
  CODE=$(echo "$R" | tail -1); BODY=$(echo "$R" | sed '$d')
  if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
    CLID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('id',''))" 2>/dev/null)
    echo -e "  ${GREEN}✓${NC} lớp '$CLASS_NAME' id=$CLID"
  else
    echo -e "  ${RED}✗${NC} lớp '$CLASS_NAME' HTTP $CODE: $(echo "$BODY" | head -c 120)"
    return 0
  fi

  echo -e "${YELLOW}[5/5] Ghi danh (mẫu)${NC}"
  local SIDS ECOUNT=0
  SIDS=$(curl -s "${H_TENANT[@]}" "$GATEWAY/api/v1/students?page=0&size=12" \
    | python3 -c "import sys,json; print(' '.join(str(s['id']) for s in json.load(sys.stdin)['data']['content']))" 2>/dev/null)
  for sid in $SIDS; do
    json="{\"studentId\":$sid,\"classId\":$CLID,\"tuitionAmount\":$price}"
    R=$(post "/api/v1/enrollments" "$json"); CODE=$(echo "$R" | tail -1)
    if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then ECOUNT=$((ECOUNT + 1)); fi
  done
  echo "      $ECOUNT ghi danh"
  echo -e "${GREEN}✓ Tenant $SUBDOMAIN xong${NC} (instance_id=$INSTANCE_ID)"
}

# ============================================================
# TENANT 1 — Cô Đỗ Lan Khánh (THPT · Pháp luật & Đời sống) — CHỦ THỂ Chương 3
# ============================================================
if [[ " $SEED_TENANTS " == *" khanh "* ]]; then
  seed_tenant \
    "khanh-phapluat" \
    "khanh.do@gmail.com" \
    "Khanh@2026" \
    '{"name":"Đỗ Lan Khánh","email":"khanh.do@gmail.com","phoneNumber":"0901234001","specialization":"Pháp luật & Đời sống (THPT)","qualification":"Cử nhân Luật","experienceYears":9}' \
    "Pháp luật & Đời sống 12|PLDS12|Khóa Pháp luật & Đời sống lớp 12 (THPT)|Advanced|Law|800000" \
    "Lớp Pháp luật 12A1" \
    30 \
    "#1E3A5F" "#C9A227"   # navy + gold
fi

# ============================================================
# TENANT 2 — Cô Nguyễn Thị Hà (Tiểu học · Toán) — gói FREE — gọn
# ============================================================
if [[ " $SEED_TENANTS " == *" ha "* ]]; then
  seed_tenant \
    "ha-toantieuhoc" \
    "ha.nguyen@gmail.com" \
    "HaToan@2026" \
    '{"name":"Nguyễn Thị Hà","email":"ha.nguyen@gmail.com","phoneNumber":"0901234002","specialization":"Toán Tiểu học","qualification":"Cử nhân Sư phạm Toán","experienceYears":5}' \
    "Toán nâng cao lớp 5|TOAN5|Khóa Toán nâng cao lớp 5 (Tiểu học)|Beginner|Math|500000" \
    "Lớp Toán 5B" \
    18 \
    "#2563EB" "#60A5FA"   # blue
fi

# ============================================================
# TENANT 3 — Thầy Nguyễn Đình Nhì (THCS · Hóa) — gói PREMIUM — gọn
# ============================================================
if [[ " $SEED_TENANTS " == *" nhi "* ]]; then
  seed_tenant \
    "nhi-hoathcs" \
    "nhi.nguyen@gmail.com" \
    "Nhi@2026" \
    '{"name":"Nguyễn Đình Nhì","email":"nhi.nguyen@gmail.com","phoneNumber":"0901234003","specialization":"Hóa học THCS","qualification":"Cử nhân Sư phạm Hóa","experienceYears":7}' \
    "Hóa học lớp 9|HOA9|Khóa Hóa học lớp 9 luyện thi vào 10 (THCS)|Intermediate|Chemistry|700000" \
    "Lớp Hóa 9C" \
    22 \
    "#16A34A" "#4ADE80"   # green
fi

echo "=============================================="
echo "  Seed Complete — 3 Giảng viên độc lập"
echo "  Đã seed: $SEED_TENANTS"
echo "=============================================="

# ------------------------------------------------------------
# Re-seed cleanup (chạy thủ công với DB nếu cần). Lặp cho từng instance_id:
#   INST=<instance_id>
#   docker exec kite-postgres psql -U kitehub -d kiteclass_shared -c \
#     "DELETE FROM enrollments WHERE student_id IN (SELECT id FROM students WHERE instance_id='$INST'); \
#      DELETE FROM classes WHERE instance_id='$INST'; \
#      DELETE FROM courses WHERE instance_id='$INST'; \
#      DELETE FROM teachers WHERE instance_id='$INST'; \
#      DELETE FROM students WHERE instance_id='$INST';"
# ------------------------------------------------------------
