#!/bin/bash
# seed-attendance-demo.sh — GAP-1479 — committed idempotent seed cho 1 LỚP DEMO đầy đủ
# (12 học sinh + ~12 buổi học + điểm danh phân bố thật) để báo cáo điểm danh + roster
# trông trực quan khi walk.
#
# Mục đích: trang /attendance/reports (KC-3 attendance) chỉ có 1 học sinh + 1 buổi nếu chỉ
# chạy seed-walk-tenant.sh → báo cáo rỗng/không thực tế. Script này thêm 1 lớp demo độc lập
# "Lớp Demo Báo Cáo" với data phong phú → báo cáo có phân bố Có mặt/Vắng/Trễ/Phép thật.
#
# Tenant: g2walk (CỐ ĐỊNH, khớp seed-walk-tenant.sh — per walk-data-committed-seed.md §3.1).
# IDEMPOTENT: re-run an toàn — fetch-by-key (email/code/name), 409 skip, attendance đã có → skip.
# Chạy SAU (hoặc độc lập với) seed-walk-tenant.sh: tự đảm bảo teacher + course tồn tại.
#
# Usage:
#   bash kitehub/scripts/seed-attendance-demo.sh            # seed demo class + report
#   GATEWAY=http://localhost:9000 bash ...                  # override gateway
#
# Access (walk báo cáo demo): http://g2walk.127.0.0.1.nip.io:3000  → đăng nhập owner/teacher
#   owner   : g2walk@kite.local      / G2walk@2026
#   teacher : huong.nguyen@g2walk.vn / Teacher@2026
#   → vào lớp "Lớp Demo Báo Cáo" → tab Điểm danh / Báo cáo điểm danh.
set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:9000}"
SUBDOMAIN=g2walk
ORG="G2 Walk Center"
EMAIL=g2walk@kite.local;        PASS='G2walk@2026'
TEACHER_EMAIL=huong.nguyen@g2walk.vn; TEACHER_PASS='Teacher@2026'
CCODE=ENG-A1                       # course code (khớp seed-walk-tenant.sh)
CLNAME="Lớp Demo Báo Cáo"          # demo class name (idempotent key)

green="\033[0;32m"; yellow="\033[1;33m"; red="\033[0;31m"; nc="\033[0m"
ok()   { echo -e "  ${green}\xE2\x9C\x93${nc} $1"; }
warn() { echo -e "  ${yellow}\xE2\x9A\xA0${nc} $1"; }
err()  { echo -e "  ${red}\xE2\x9C\x97${nc} $1"; }

echo "=============================================="
echo "  Seed Attendance Demo — $CLNAME (idempotent)"
echo "=============================================="

# 0. Provision tenant (idempotent — đảm bảo tồn tại nếu chạy độc lập)
RC=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY/api/auth/register" -H "Content-Type: application/json" \
  -d "{\"organizationName\":\"$ORG\",\"subdomain\":\"$SUBDOMAIN\",\"ownerEmail\":\"$EMAIL\",\"ownerPassword\":\"$PASS\"}")
case "$RC" in 201|200) ok "tenant tạo mới";; 409|400) warn "tenant đã tồn tại (HTTP $RC)";; *) warn "register HTTP $RC";; esac

# 1. Owner login → token
TOKEN=$(curl -s -X POST "$GATEWAY/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | python3 -c "import sys,json;print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
[ -z "$TOKEN" ] && { err "owner login FAIL"; exit 1; }
ok "owner login OK"

H=(-H "X-Instance-Subdomain: $SUBDOMAIN" -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
post()    { curl -s -w "\n%{http_code}" "${H[@]}" -X POST -d "$2" "$GATEWAY$1"; }
getj()    { curl -s "${H[@]}" "$GATEWAY$1"; }
idof()    { python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',d).get('id',''))" 2>/dev/null; }
find_id() { python3 -c "
import sys,json
key,val=sys.argv[1],sys.argv[2]
d=json.load(sys.stdin); c=d.get('data',d); c=c.get('content',c) if isinstance(c,dict) else c
print(next((str(x['id']) for x in c if str(x.get(key))==val), ''))" "$1" "$2" 2>/dev/null; }

# 2. Teacher (idempotent fetch-or-create) — class PHẢI do teacher tạo (GAP-1466)
TID=$(getj "/api/v1/teachers?page=0&size=100" | find_id email "$TEACHER_EMAIL")
if [ -z "$TID" ]; then
  R=$(post /api/v1/teachers "{\"name\":\"Nguyễn Thị Hương\",\"email\":\"$TEACHER_EMAIL\",\"phoneNumber\":\"0901234567\",\"specialization\":\"Tiếng Anh giao tiếp\",\"qualification\":\"Cử nhân Sư phạm Anh\",\"experienceYears\":6}")
  TID=$(echo "$R"|sed '$d'|idof); ok "teacher id=$TID (mới)"
else warn "teacher đã tồn tại → id=$TID"; fi
curl -s -o /dev/null "${H[@]}" -X POST "$GATEWAY/api/v1/teachers/$TID/credentials" -d "{\"password\":\"$TEACHER_PASS\"}"

# 3. Teacher login → teacher token
TTOKEN=$(curl -s -X POST "$GATEWAY/api/v1/tenant-auth/login" -H "X-Instance-Subdomain: $SUBDOMAIN" -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEACHER_EMAIL\",\"password\":\"$TEACHER_PASS\"}" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('accessToken') or d.get('data',{}).get('accessToken',''))" 2>/dev/null)
[ -z "$TTOKEN" ] && { err "teacher login FAIL"; exit 1; }
TH=(-H "X-Instance-Subdomain: $SUBDOMAIN" -H "Authorization: Bearer $TTOKEN" -H "Content-Type: application/json")
tpost() { curl -s -w "\n%{http_code}" "${TH[@]}" -X POST -d "$2" "$GATEWAY$1"; }
tgetj() { curl -s "${TH[@]}" "$GATEWAY$1"; }
ok "teacher login OK"

# 4. Course (idempotent fetch-or-create by code)
CID=$(getj "/api/v1/courses?page=0&size=100" | find_id code "$CCODE")
if [ -z "$CID" ]; then
  R=$(post /api/v1/courses "{\"name\":\"Tiếng Anh giao tiếp A1\",\"code\":\"$CCODE\",\"description\":\"Khóa giao tiếp cơ bản\",\"level\":\"Beginner\",\"category\":\"English\",\"teacherId\":$TID,\"price\":1200000,\"durationWeeks\":12,\"totalSessions\":24}")
  CID=$(echo "$R"|sed '$d'|idof); ok "course $CCODE id=$CID (mới)"
else warn "course $CCODE đã tồn tại → id=$CID"; fi

# 5. Demo class (idempotent by name — TEACHER tạo; dates QUÁ KHỨ → buổi học đã diễn ra để điểm danh thật)
echo -e "${yellow}[demo] Class${nc}"
START=$(date -d "-28 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
END=$(date -d "+14 days" +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)
CLID=$(tgetj "/api/v1/classes?page=0&size=200" | find_id name "$CLNAME")
if [ -n "$CLID" ]; then warn "class '$CLNAME' đã tồn tại → id=$CLID"
else
  R=$(tpost "/api/v1/courses/$CID/classes" "{\"name\":\"$CLNAME\",\"description\":\"Lớp demo data đầy đủ cho báo cáo điểm danh (GAP-1479)\",\"schedule\":\"Thứ 2-4, 18:00-19:30\",\"locationDetail\":\"45 Hai Bà Trưng, Hà Nội\",\"startDate\":\"$START\",\"endDate\":\"$END\",\"maxStudents\":40}")
  if [ "$(echo "$R"|tail -1)" = "201" ]; then CLID=$(echo "$R"|sed '$d'|idof); ok "class '$CLNAME' id=$CLID (mới)"
  else err "class create HTTP $(echo "$R"|tail -1)"; exit 1; fi
fi

# 6. 12 học sinh (idempotent 409 skip) + enroll vào demo class + activate
echo -e "${yellow}[demo] 12 học sinh + enroll + activate${nc}"
NAMES=("Trần Thị Hồng" "Nguyễn Văn An" "Lê Thị Mai" "Phạm Minh Quân" "Hoàng Văn Đức"
       "Vũ Thị Lan" "Đặng Minh Khôi" "Bùi Thị Ngọc" "Đỗ Văn Hùng" "Ngô Thị Thu"
       "Dương Minh Tuấn" "Lý Thị Hà")
NEW=0
for i in $(seq 1 12); do
  EMAIL_HV="demo_hv$i@g2walk.vn"
  R=$(post /api/v1/students "{\"name\":\"${NAMES[$((i-1))]}\",\"email\":\"$EMAIL_HV\",\"phone\":\"09120000$(printf '%02d' "$i")\"}")
  [ "$(echo "$R"|tail -1)" = "201" ] && NEW=$((NEW+1))
done
# Lấy enrollment-id của 12 hv demo trong demo class (enroll nếu chưa, activate nếu PENDING)
DEMO_SIDS=$(getj "/api/v1/students?page=0&size=200" | python3 -c "
import sys,json
d=json.load(sys.stdin); c=d.get('data',{}); c=c.get('content',c) if isinstance(c,dict) else c
print(' '.join(str(s['id']) for s in c if str(s.get('email','')).startswith('demo_hv')))" 2>/dev/null)
EN=0
for sid in $DEMO_SIDS; do
  R=$(post /api/v1/enrollments "{\"studentId\":$sid,\"classId\":$CLID,\"tuitionAmount\":1200000}")
  [ "$(echo "$R"|tail -1)" = "201" ] && EN=$((EN+1))
done
# activate (PUT status ACTIVE cho enrollment chưa ACTIVE — roster điểm danh chỉ lấy ACTIVE, GAP-1474)
ACT=0
PENDING_EIDS=$(getj "/api/v1/enrollments/class/$CLID?page=0&size=200" | python3 -c "
import sys,json
d=json.load(sys.stdin); c=d.get('data',{}); c=c.get('content',c) if isinstance(c,dict) else c
print(' '.join(str(e['id']) for e in c if e.get('status')!='ACTIVE'))" 2>/dev/null)
for eid in $PENDING_EIDS; do
  RC=$(curl -s -o /dev/null -w "%{http_code}" "${H[@]}" -X PUT "$GATEWAY/api/v1/enrollments/$eid/status" \
    -d '{"status":"ACTIVE","notes":"Demo seed: payment confirmed (GAP-1479)"}')
  [ "$RC" = "200" ] && ACT=$((ACT+1))
done
ok "học sinh: $NEW mới | enroll: $EN mới | kích hoạt: $ACT ACTIVE"

# 7. Tạo buổi học (createSchedule — teacher token; Thứ 2 + Thứ 4 trên range -28..+14 ≈ 12 buổi)
echo -e "${yellow}[demo] Tạo buổi học (lịch Thứ 2-4)${nc}"
SCH=$(tpost "/api/v1/classes/$CLID/schedule" "{\"daysOfWeek\":[\"MONDAY\",\"WEDNESDAY\"],\"startTime\":\"18:00:00\",\"endTime\":\"19:30:00\"}")
SCH_CODE=$(echo "$SCH"|tail -1)
case "$SCH_CODE" in
  201|200) ok "đã tạo lịch buổi học (HTTP $SCH_CODE)";;
  409|400) warn "lịch buổi học đã tồn tại (HTTP $SCH_CODE) — idempotent skip";;
  *) warn "createSchedule HTTP $SCH_CODE";;
esac

# 8. Điểm danh phân bố thật trên các buổi ĐÃ DIỄN RA (date <= hôm nay), bulk per session
echo -e "${yellow}[demo] Điểm danh (phân bố Có mặt/Vắng/Trễ/Phép thật)${nc}"
TODAY=$(date +%Y-%m-%d)
# (enrollment_id, attended_session_id_csv) — lấy enrollments + sessions; tính status deterministic
# per (student_index, session_index): phần lớn PRESENT, rải ABSENT/LATE/EXCUSED/MAKEUP, rate khác nhau /hs.
MARKED=$(python3 - "$GATEWAY" "$CLID" "$TODAY" "$TTOKEN" "$TOKEN" "$SUBDOMAIN" <<'PY'
import sys, json, urllib.request, datetime
gw, clid, today, ttok, otok, sub = sys.argv[1:7]
def get(path, tok):
    req = urllib.request.Request(gw+path, headers={"X-Instance-Subdomain": sub, "Authorization": "Bearer "+tok})
    try:
        d = json.load(urllib.request.urlopen(req, timeout=15))
    except Exception:
        return []
    c = d.get('data', d); c = c.get('content', c) if isinstance(c, dict) else c
    return c if isinstance(c, list) else []
def post(path, body, tok, owner_hdr=False):
    h = {"X-Instance-Subdomain": sub, "Authorization": "Bearer "+tok, "Content-Type": "application/json"}
    if owner_hdr: h["X-User-Id"] = "1"
    req = urllib.request.Request(gw+path, data=json.dumps(body).encode(), headers=h, method="POST")
    try:
        urllib.request.urlopen(req, timeout=20); return True
    except Exception:
        return False

sessions = get(f"/api/v1/classes/{clid}/sessions", ttok)
enrolls  = get(f"/api/v1/enrollments/class/{clid}?page=0&size=200", otok)
eids = [e['id'] for e in enrolls if e.get('status') == 'ACTIVE']
def past(s):
    d = s.get('sessionDate') or s.get('date') or ''
    return bool(d) and d <= today
past_sessions = sorted([s for s in sessions if past(s)], key=lambda s: s.get('sessionNumber', 0))

def status_for(i, j):
    v = (i*13 + j*7 + i*j) % 100
    if v < 78: return "PRESENT"
    if v < 86: return "LATE"
    if v < 93: return "ABSENT"
    if v < 97: return "EXCUSED"
    return "MAKEUP"

marked = 0
for j, s in enumerate(past_sessions):
    sid = s['id']
    records = [{"enrollmentId": eid, "status": status_for(i, j)} for i, eid in enumerate(eids)]
    # Mark as OWNER (X-User-Id:1): teacher-created class has teacherId=None so the teacher
    # token hits 403 TEACHER_NOT_IN_CLASS; owner/admin can mark attendance for any class.
    if post(f"/api/v1/attendance/classes/{clid}/sessions/{sid}/attendance",
            {"sessionId": sid, "records": records}, otok, owner_hdr=True):
        marked += 1
print(f"{marked}/{len(past_sessions)} buổi đã điểm danh | {len(eids)} hs ACTIVE")
PY
)
ok "$MARKED"

echo "=============================================="
echo -e "${green}\xE2\x9C\x93 Demo class ready${nc} — tenant=$SUBDOMAIN class=$CLID '$CLNAME'"
echo "  Báo cáo điểm danh giờ có data phong phú (12 hs, ~12 buổi, phân bố thật)."
echo "  Walk: http://$SUBDOMAIN.127.0.0.1.nip.io:3000 → lớp '$CLNAME' → Điểm danh / Báo cáo"
echo "    owner   : $EMAIL / $PASS"
echo "    teacher : $TEACHER_EMAIL / $TEACHER_PASS"
echo "=============================================="
