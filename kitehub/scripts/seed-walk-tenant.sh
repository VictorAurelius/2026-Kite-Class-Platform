#!/bin/bash
# seed-walk-tenant.sh — Single-source-of-truth cho Flow Verification Campaign walk data.
#
# Mục đích: tạo 1 tenant walk CỐ ĐỊNH (g2walk) + journey data (teacher/course/class/
# students/enrollments) + tenant-member credentials (teacher login KC-6, parent login KC-8)
# qua production API path (gateway :9000 + JWT), IDEMPOTENT. Re-run sau mỗi WSL restart /
# fresh stack → baseline Y HỆT → agent-G1 (Claude) và human-G2 (dev) walk cùng một data.
# Tránh ad-hoc seed drift (per .claude/rules/walk-data-committed-seed.md).
#
# Usage:
#   bash kitehub/scripts/seed-walk-tenant.sh          # seed + report
#   GATEWAY=http://localhost:9000 bash ...            # override gateway
#
# Idempotent: re-run an toàn — register 409 OK; teacher/course/class fetch-by-key;
# teacher-credential upsert (rotate); students/enroll 409 skip; parent skip nếu login đã OK.
#
# Canonical walk creds (production-accurate access per g1-browser-walk-before-flip §3.3):
#   KC  : http://g2walk.127.0.0.1.nip.io:3000   (owner / teacher / parent — login bên dưới)
#   KH  : http://localhost:3001                  (cùng owner, platform-side KC-2)
#   owner   : g2walk@kite.local      / G2walk@2026   (OWNER)
#   teacher : huong.nguyen@g2walk.vn / Teacher@2026  (TEACHER — KC-6 grade)
#   parent  : phuhuynh@g2walk.vn     / Parent@2026   (PARENT — KC-8 portal)
#
# GAP-1466: class.teacher_id = CREATOR's actor-UUID (ClassServiceImpl GAP-727; no reassign
# endpoint). Class PHẢI do TEACHER tạo (teacher JWT) → teacher truy cập gradebook được.
set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:9000}"
CORE_DIRECT="${CORE_DIRECT:-http://localhost:8088}"   # kiteclass-core direct (parent redeem — gateway strips X-Tenant-Id on public route)
SUBDOMAIN=g2walk
ORG="G2 Walk Center"
EMAIL=g2walk@kite.local;        PASS='G2walk@2026'
TEACHER_EMAIL=huong.nguyen@g2walk.vn; TEACHER_PASS='Teacher@2026'
PARENT_EMAIL=phuhuynh@g2walk.vn;      PARENT_PASS='Parent@2026'

green="\033[0;32m"; yellow="\033[1;33m"; red="\033[0;31m"; nc="\033[0m"
ok()   { echo -e "  ${green}✓${nc} $1"; }
warn() { echo -e "  ${yellow}⚠${nc} $1"; }
err()  { echo -e "  ${red}✗${nc} $1"; }

echo "=============================================="
echo "  Seed Walk Tenant — $SUBDOMAIN (idempotent)"
echo "=============================================="

# 0. Provision tenant (idempotent — 201 new / 409|400 exists)
echo -e "${yellow}[0/8] Provision tenant${nc}"
RC=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/auth/register" -H "Content-Type: application/json" \
  -d "{\"organizationName\":\"$ORG\",\"subdomain\":\"$SUBDOMAIN\",\"ownerEmail\":\"$EMAIL\",\"ownerPassword\":\"$PASS\"}")
case "$RC" in 201|200) ok "tenant tạo mới";; 409|400) warn "tenant đã tồn tại (HTTP $RC)";; *) err "register HTTP $RC";; esac

# 1. Owner login → token + tenantId
echo -e "${yellow}[1/8] Owner login${nc}"
TOKEN=$(curl -s -X POST "$GATEWAY/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | python3 -c "import sys,json;print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
[ -z "$TOKEN" ] && { err "owner login FAIL"; exit 1; }
TENANT=$(curl -s "$GATEWAY/api/v1/public/tenants/by-subdomain/$SUBDOMAIN" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',d).get('id',''))" 2>/dev/null)
ok "owner login OK (tenantId=${TENANT:0:8}…)"

H=(-H "X-Instance-Subdomain: $SUBDOMAIN" -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
post() { curl -s -w "\n%{http_code}" "${H[@]}" -X POST -d "$2" "$GATEWAY$1"; }
getj() { curl -s "${H[@]}" "$GATEWAY$1"; }
idof() { python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',d).get('id',''))" 2>/dev/null; }
find_id() { python3 -c "
import sys,json
key,val=sys.argv[1],sys.argv[2]
d=json.load(sys.stdin); c=d.get('data',d); c=c.get('content',c) if isinstance(c,dict) else c
print(next((str(x['id']) for x in c if str(x.get(key))==val), ''))" "$1" "$2" 2>/dev/null; }

# 2. Teacher (idempotent)
echo -e "${yellow}[2/8] Teacher${nc}"
R=$(post /api/v1/teachers "{\"name\":\"Nguyễn Thị Hương\",\"email\":\"$TEACHER_EMAIL\",\"phoneNumber\":\"0901234567\",\"specialization\":\"Tiếng Anh giao tiếp\",\"qualification\":\"Cử nhân Sư phạm Anh\",\"experienceYears\":6}")
if [ "$(echo "$R"|tail -1)" = "201" ]; then TID=$(echo "$R"|sed '$d'|idof); ok "teacher id=$TID (mới)"
else TID=$(getj "/api/v1/teachers?page=0&size=100" | find_id email "$TEACHER_EMAIL"); warn "teacher đã tồn tại → id=$TID"; fi

# 3. Teacher login credential (idempotent — upsert rotates password)
echo -e "${yellow}[3/8] Teacher login credential${nc}"
RC=$(curl -s -o /dev/null -w "%{http_code}" "${H[@]}" -X POST "$GATEWAY/api/v1/teachers/$TID/credentials" -d "{\"password\":\"$TEACHER_PASS\"}")
{ [ "$RC" = "200" ] || [ "$RC" = "201" ]; } && ok "teacher credential set ($TEACHER_EMAIL)" || warn "teacher credential HTTP $RC"

# 4. Teacher login → teacher token (class MUST be created by teacher — GAP-1466/GAP-727)
echo -e "${yellow}[4/8] Teacher login${nc}"
TTOKEN=$(curl -s -X POST "$GATEWAY/api/v1/tenant-auth/login" -H "X-Instance-Subdomain: $SUBDOMAIN" -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEACHER_EMAIL\",\"password\":\"$TEACHER_PASS\"}" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('accessToken') or d.get('data',{}).get('accessToken',''))" 2>/dev/null)
[ -z "$TTOKEN" ] && { err "teacher login FAIL"; exit 1; }
TH=(-H "X-Instance-Subdomain: $SUBDOMAIN" -H "Authorization: Bearer $TTOKEN" -H "Content-Type: application/json")
tpost() { curl -s -w "\n%{http_code}" "${TH[@]}" -X POST -d "$2" "$GATEWAY$1"; }
ok "teacher login OK"

# 5. Course (idempotent by code — owner creates; course.teacherId = teacher domain id)
echo -e "${yellow}[5/8] Course${nc}"
CCODE=ENG-A1
R=$(post /api/v1/courses "{\"name\":\"Tiếng Anh giao tiếp A1\",\"code\":\"$CCODE\",\"description\":\"Khóa giao tiếp cơ bản\",\"level\":\"Beginner\",\"category\":\"English\",\"teacherId\":$TID,\"price\":1200000,\"durationWeeks\":12,\"totalSessions\":24}")
if [ "$(echo "$R"|tail -1)" = "201" ]; then CID=$(echo "$R"|sed '$d'|idof); ok "course $CCODE id=$CID (mới)"
else CID=$(getj "/api/v1/courses?page=0&size=100" | find_id code "$CCODE"); warn "course $CCODE đã tồn tại → id=$CID"; fi

# 6. Class (idempotent by name — TEACHER creates → class.teacher_id = teacher actor-UUID per GAP-1466)
echo -e "${yellow}[6/8] Class (teacher-owned)${nc}"
CLNAME="Lớp Anh A1 GV"
CLID=$(getj "/api/v1/classes?page=0&size=100" | find_id name "$CLNAME")
if [ -n "$CLID" ]; then warn "class '$CLNAME' đã tồn tại → id=$CLID"
else
  START=$(date -d "+7 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
  END=$(date -d "+90 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
  R=$(tpost "/api/v1/courses/$CID/classes" "{\"name\":\"$CLNAME\",\"description\":\"Lớp demo g2walk (teacher-owned)\",\"schedule\":\"Thứ 2-4-6, 18:00-19:30\",\"locationDetail\":\"45 Hai Bà Trưng, Hà Nội\",\"startDate\":\"$START\",\"endDate\":\"$END\",\"maxStudents\":40}")
  if [ "$(echo "$R"|tail -1)" = "201" ]; then CLID=$(echo "$R"|sed '$d'|idof); ok "class '$CLNAME' id=$CLID (mới, teacher-owned)"
  else err "class create HTTP $(echo "$R"|tail -1)"; fi
fi

# 7. Students (idempotent 409 skip) + enroll
echo -e "${yellow}[7/8] Students + enroll${nc}"
NAMES=("Trần Văn An" "Lê Thị Mai" "Phạm Minh Quân" "Hoàng Thị Hồng" "Vũ Đức Thành")
NEW=0
for i in 1 2 3 4 5; do
  R=$(post /api/v1/students "{\"name\":\"${NAMES[$((i-1))]}\",\"email\":\"hv$i@g2walk.vn\",\"phone\":\"091000000$i\"}")
  [ "$(echo "$R"|tail -1)" = "201" ] && NEW=$((NEW+1))
done
SIDS=$(getj "/api/v1/students?page=0&size=20" | python3 -c "import sys,json;d=json.load(sys.stdin);c=d.get('data',{});c=c.get('content',c) if isinstance(c,dict) else c;print(' '.join(str(s['id']) for s in c))" 2>/dev/null)
EN=0
for sid in $SIDS; do
  R=$(post /api/v1/enrollments "{\"studentId\":$sid,\"classId\":$CLID,\"tuitionAmount\":1200000}")
  [ "$(echo "$R"|tail -1)" = "201" ] && EN=$((EN+1))
done
# GAP-1474: confirm payment → activate enrollments so the attendance roster
# (ACTIVE-only per BR-ATTEND-001) has students. Idempotent: re-fetch class
# enrollments + PUT status=ACTIVE for any not already ACTIVE (owner = ROLE_OWNER
# → tenant-admin bypass on the per-class authz guard). POST defaults to
# PENDING_PAYMENT, so without this step every walk attendance is empty.
ACT=0
PENDING_EIDS=$(getj "/api/v1/enrollments/class/$CLID?page=0&size=100" | python3 -c "
import sys,json
d=json.load(sys.stdin); c=d.get('data',{}); c=c.get('content',c) if isinstance(c,dict) else c
print(' '.join(str(e['id']) for e in c if e.get('status')!='ACTIVE'))" 2>/dev/null)
for eid in $PENDING_EIDS; do
  RC=$(curl -s -o /dev/null -w "%{http_code}" "${H[@]}" -X PUT "$GATEWAY/api/v1/enrollments/$eid/status" \
    -d '{"status":"ACTIVE","notes":"Walk seed: payment confirmed (GAP-1474)"}')
  [ "$RC" = "200" ] && ACT=$((ACT+1))
done
ok "students: $NEW mới | enroll: $EN mới | kích hoạt: $ACT ACTIVE (class $CLID)"

# 8. Parent (idempotent — skip nếu parent login đã OK). invite (owner gateway) → redeem (direct-core; gateway strips X-Tenant-Id on public route)
echo -e "${yellow}[8/8] Parent (invite→redeem)${nc}"
PLOGIN=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/v1/tenant-auth/login" -H "X-Instance-Subdomain: $SUBDOMAIN" -H "Content-Type: application/json" -d "{\"email\":\"$PARENT_EMAIL\",\"password\":\"$PARENT_PASS\"}")
if [ "$PLOGIN" = "200" ]; then warn "parent đã provisioned (login OK) — skip"
else
  PSID=$(echo "$SIDS" | awk '{print $1}')
  TOK=$(post /api/v1/parent-invitations "{\"studentId\":$PSID,\"parentEmail\":\"$PARENT_EMAIL\"}" | sed '$d' | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('token',''))" 2>/dev/null)
  if [ -n "$TOK" ]; then
    # redeem direct-core (X-Tenant-Id explicit — gateway public route strips it; documented exception per walk-data-committed-seed §3.1)
    RC=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$CORE_DIRECT/api/v1/parent-invitations/redeem/$TOK" \
      -H "X-Tenant-Id: $TENANT" -H "Content-Type: application/json" \
      -d "{\"password\":\"$PARENT_PASS\",\"fullName\":\"Trần Thị Phụ Huynh\",\"phoneNumber\":\"0905555666\",\"relationship\":\"MOTHER\"}")
    [ "$RC" = "200" ] && ok "parent provisioned ($PARENT_EMAIL ↔ student $PSID)" || err "parent redeem HTTP $RC"
  else err "parent invite — no token"; fi
fi

echo "=============================================="
echo -e "${green}✓ Walk baseline ready${nc} — tenant=$SUBDOMAIN teacher=$TID course=$CID class=$CLID"
echo "  điểm danh: $ACT enrollment ACTIVE → roster điểm danh có học sinh (GAP-1474)"
echo "  KC owner   : http://$SUBDOMAIN.127.0.0.1.nip.io:3000 ($EMAIL / $PASS)"
echo "  KC teacher : .../teacher/grades/$CLID ($TEACHER_EMAIL / $TEACHER_PASS) — KC-6"
echo "  KC parent  : .../parent ($PARENT_EMAIL / $PARENT_PASS) — KC-8"
echo "  KH owner   : http://localhost:3001 — KC-2 staff"
echo "=============================================="
