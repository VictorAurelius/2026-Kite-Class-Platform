#!/bin/bash
# seed-walk-tenant.sh — Single-source-of-truth cho Flow Verification Campaign walk data.
#
# Mục đích: tạo 1 tenant walk CỐ ĐỊNH (g2walk) + journey data (teacher/course/class/
# students/enrollments) qua production API path (gateway :9000 + owner JWT), IDEMPOTENT.
# Re-run sau mỗi WSL restart / fresh stack → baseline Y HỆT → agent-G1 (Claude) và
# human-G2 (dev) walk cùng một data. Tránh ad-hoc seed drift (per
# .claude/rules/walk-data-committed-seed.md).
#
# Usage:
#   bash kitehub/scripts/seed-walk-tenant.sh          # seed + report
#   GATEWAY=http://localhost:9000 bash ...            # override gateway
#
# Idempotent: re-run an toàn — register 409 OK; teacher/course/class fetch-by-key nếu
# đã tồn tại; students/enrollments 409 skip. KHÔNG tạo duplicate.
#
# Canonical walk tenant (production-accurate access per g1-browser-walk-before-flip §3.3):
#   KC  : http://g2walk.127.0.0.1.nip.io:3000   (login owner bên dưới)
#   KH  : http://localhost:3001                  (cùng owner, platform-side KC-2)
set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:9000}"
SUBDOMAIN=g2walk
ORG="G2 Walk Center"
EMAIL=g2walk@kite.local
PASS='G2walk@2026'

green="\033[0;32m"; yellow="\033[1;33m"; red="\033[0;31m"; nc="\033[0m"
ok()   { echo -e "  ${green}✓${nc} $1"; }
warn() { echo -e "  ${yellow}⚠${nc} $1"; }
err()  { echo -e "  ${red}✗${nc} $1"; }

echo "=============================================="
echo "  Seed Walk Tenant — $SUBDOMAIN (idempotent)"
echo "=============================================="

# 0. Provision tenant (idempotent — 201 new / 409|400 exists)
echo -e "${yellow}[0/6] Provision tenant${nc}"
REG=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY/api/auth/register" -H "Content-Type: application/json" \
  -d "{\"organizationName\":\"$ORG\",\"subdomain\":\"$SUBDOMAIN\",\"ownerEmail\":\"$EMAIL\",\"ownerPassword\":\"$PASS\"}")
RCODE=$(echo "$REG" | tail -1)
case "$RCODE" in
  201|200) ok "tenant tạo mới ($SUBDOMAIN)";;
  409|400) warn "tenant đã tồn tại (HTTP $RCODE) — tiếp tục";;
  *) err "register HTTP $RCODE: $(echo "$REG" | sed '$d' | head -c 160)";;
esac

# 1. Login → token
echo -e "${yellow}[1/6] Login owner${nc}"
LOGIN=$(curl -s -X POST "$GATEWAY/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json;print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
[ -z "$TOKEN" ] && { err "login FAIL: $(echo "$LOGIN" | head -c 160)"; exit 1; }
ok "login OK"

H=(-H "X-Instance-Subdomain: $SUBDOMAIN" -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
post() { curl -s -w "\n%{http_code}" "${H[@]}" -X POST -d "$2" "$GATEWAY$1"; }
getj() { curl -s "${H[@]}" "$GATEWAY$1"; }
idof() { python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',d).get('id',''))" 2>/dev/null; }
# fetch id from paginated list by a key=value match
find_id() { python3 -c "
import sys,json
key,val=sys.argv[1],sys.argv[2]
d=json.load(sys.stdin); c=d.get('data',d); c=c.get('content',c) if isinstance(c,dict) else c
print(next((str(x['id']) for x in c if str(x.get(key))==val), ''))" "$1" "$2" 2>/dev/null; }

# 2. Teacher (idempotent)
echo -e "${yellow}[2/6] Teacher${nc}"
TEMAIL=huong.nguyen@g2walk.vn
R=$(post /api/v1/teachers "{\"name\":\"Nguyễn Thị Hương\",\"email\":\"$TEMAIL\",\"phoneNumber\":\"0901234567\",\"specialization\":\"Tiếng Anh giao tiếp\",\"qualification\":\"Cử nhân Sư phạm Anh\",\"experienceYears\":6}")
CODE=$(echo "$R"|tail -1)
if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then TID=$(echo "$R"|sed '$d'|idof); ok "teacher id=$TID (mới)"
else TID=$(getj "/api/v1/teachers?page=0&size=100" | find_id email "$TEMAIL"); warn "teacher đã tồn tại → id=$TID"; fi

# 3. Course (idempotent by code)
echo -e "${yellow}[3/6] Course${nc}"
CCODE=ENG-A1
R=$(post /api/v1/courses "{\"name\":\"Tiếng Anh giao tiếp A1\",\"code\":\"$CCODE\",\"description\":\"Khóa giao tiếp cơ bản\",\"level\":\"Beginner\",\"category\":\"English\",\"teacherId\":$TID,\"price\":1200000,\"durationWeeks\":12,\"totalSessions\":24}")
CODE=$(echo "$R"|tail -1)
if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then CID=$(echo "$R"|sed '$d'|idof); ok "course $CCODE id=$CID (mới)"
else CID=$(getj "/api/v1/courses?page=0&size=100" | find_id code "$CCODE"); warn "course $CCODE đã tồn tại → id=$CID"; fi

# 4. Students (idempotent — 409 skip)
echo -e "${yellow}[4/6] Students${nc}"
NAMES=("Trần Văn An" "Lê Thị Mai" "Phạm Minh Quân" "Hoàng Thị Hồng" "Vũ Đức Thành")
NEW=0; EXIST=0
for i in 1 2 3 4 5; do
  nm="${NAMES[$((i-1))]}"
  R=$(post /api/v1/students "{\"name\":\"$nm\",\"email\":\"hv$i@g2walk.vn\",\"phone\":\"091000000$i\"}")
  case "$(echo "$R"|tail -1)" in 201|200) NEW=$((NEW+1));; *) EXIST=$((EXIST+1));; esac
done
ok "students: $NEW mới / $EXIST đã có"

# 5. Class (idempotent by name)
echo -e "${yellow}[5/6] Class${nc}"
CLNAME="Lớp Anh A1 Tối"
START=$(date -d "+7 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
END=$(date -d "+90 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
R=$(post "/api/v1/courses/$CID/classes" "{\"name\":\"$CLNAME\",\"description\":\"Lớp demo g2walk\",\"schedule\":\"Thứ 2-4-6, 18:00-19:30\",\"locationDetail\":\"45 Hai Bà Trưng, Hà Nội\",\"startDate\":\"$START\",\"endDate\":\"$END\",\"maxStudents\":40}")
CODE=$(echo "$R"|tail -1)
if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then CLID=$(echo "$R"|sed '$d'|idof); ok "class '$CLNAME' id=$CLID (mới)"
else CLID=$(getj "/api/v1/classes?page=0&size=100" | find_id name "$CLNAME"); warn "class đã tồn tại → id=$CLID"; fi

# 6. Enroll (idempotent — 409 skip)
echo -e "${yellow}[6/6] Enroll${nc}"
SIDS=$(getj "/api/v1/students?page=0&size=20" | python3 -c "import sys,json;d=json.load(sys.stdin);c=d.get('data',{});c=c.get('content',c) if isinstance(c,dict) else c;print(' '.join(str(s['id']) for s in c))" 2>/dev/null)
EN=0; ES=0
for sid in $SIDS; do
  R=$(post /api/v1/enrollments "{\"studentId\":$sid,\"classId\":$CLID,\"tuitionAmount\":1200000}")
  case "$(echo "$R"|tail -1)" in 201|200) EN=$((EN+1));; *) ES=$((ES+1));; esac
done
ok "enroll: $EN mới / $ES đã có"

echo "=============================================="
echo -e "${green}✓ Walk baseline ready${nc} — tenant=$SUBDOMAIN course=$CID class=$CLID"
echo "  KC walk : http://$SUBDOMAIN.127.0.0.1.nip.io:3000  (login $EMAIL / $PASS)"
echo "  KH walk : http://localhost:3001                     (cùng owner, KC-2 staff)"
echo "=============================================="
