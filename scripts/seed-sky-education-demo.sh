#!/usr/bin/env bash
#
# seed-sky-education-demo.sh — Seed a fresh "Sky Education" demo tenant end-to-end
# via the real signup pipeline (NOT direct SQL), then populate standard-depth
# teaching data: teachers, courses, classes, students, enrollments.
#
# Flow (all steps verified live against local stack gateway :9000 on 2026-05-28):
#   1. request-beta-access      POST /api/v1/auth/request-beta-access
#   2. admin login              POST /api/auth/login          (PLATFORM_ADMIN)
#   3. approve request          POST /api/v1/admin/beta-requests/{id}/approve
#   4. fetch claim code         DB beta_access_request.claim_code (MailHog fallback)
#   5. exchange claim code      POST /api/v1/auth/beta-signup/exchange-claim-code
#   6. beta-signup (provision)  POST /api/v1/auth/beta-signup
#   7. owner login              POST /api/auth/login          (OWNER)
#   8. teachers                 POST /api/v1/teachers
#   9. courses                  POST /api/v1/courses
#  10. classes                  POST /api/v1/courses/{courseId}/classes
#  11. students                 POST /api/v1/students
#  12. enrollments              POST /api/v1/enrollments
#
# NOTE: each run provisions a NEW tenant (subdomain/email carry a timestamp
# suffix) so re-runs do not collide. Step 11 (CC0 cover images) is OUT OF SCOPE
# — blocked on GAP-798b (storage controller needs X-User-Reference-Id producer).
#
# Usage:
#   bash scripts/seed-sky-education-demo.sh                 # default local stack
#   GW=http://localhost:9000 bash scripts/seed-sky-education-demo.sh
#
# Requires: curl, python3, docker (for DB claim-code read). Local stack healthy.
set -euo pipefail

# ── Config ──────────────────────────────────────────────────────────────────
GW="${GW:-http://localhost:9000}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@kitehub.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@KiteHub123}"
PG_CONTAINER="${PG_CONTAINER:-kite-postgres}"
PG_USER="${PG_USER:-kitehub}"
PG_DB="${PG_DB:-kitehub}"

# Fresh-tenant identity (timestamp suffix avoids collision on re-run)
SUFFIX="$(date +%H%M%S)"
OWNER_EMAIL="${OWNER_EMAIL:-owner+${SUFFIX}@skyedu.vn}"
OWNER_PASSWORD="${OWNER_PASSWORD:-SkyEdu@2026}"
SUBDOMAIN="${SUBDOMAIN:-sky-education-${SUFFIX}}"
ORG_NAME="${ORG_NAME:-Trung tâm Anh ngữ Sky Education}"
OWNER_NAME="${OWNER_NAME:-Trần Thị Hồng}"

# ── Helpers ───────────────────────────────────────────────────────────────────
say()  { printf '\n\033[1;36m▶ %s\033[0m\n' "$1"; }
ok()   { printf '  \033[1;32m✓\033[0m %s\n' "$1"; }
die()  { printf '\n\033[1;31m✗ %s\033[0m\n' "$1" >&2; exit 1; }

# jget '<json>' '<key path, e.g. data.id>' — extract a value (tolerates {data:{...}} wrapper)
jget() {
  python3 -c "
import sys, json
d = json.loads(sys.stdin.read())
for k in '$1'.split('.'):
    d = d.get(k) if isinstance(d, dict) else None
    if d is None: break
print('' if d is None else d)
" 2>/dev/null
}

post() { # post <url> <json> [auth-bearer]
  local url="$1" body="$2" auth="${3:-}"
  if [ -n "$auth" ]; then
    curl -s -X POST "$url" -H "Authorization: Bearer $auth" -H "Content-Type: application/json" -d "$body" --max-time 20
  else
    curl -s -X POST "$url" -H "Content-Type: application/json" -d "$body" --max-time 20
  fi
}

login() { # login <email> <password> → prints accessToken
  post "$GW/api/auth/login" "{\"email\":\"$1\",\"password\":\"$2\"}" | jget accessToken
}

# ── Preflight ─────────────────────────────────────────────────────────────────
say "Pre-flight: kiểm tra stack local"
curl -sf -o /dev/null --max-time 5 "$GW/actuator/health" || die "Gateway $GW không healthy — chạy bash kitehub/scripts/up.sh trước"
ok "Gateway $GW healthy"
command -v python3 >/dev/null || die "cần python3"

# ── Step 1: request beta access ───────────────────────────────────────────────
say "1/12 Gửi yêu cầu truy cập Beta cho '$ORG_NAME'"
REQ=$(post "$GW/api/v1/auth/request-beta-access" \
  "{\"name\":\"$OWNER_NAME\",\"orgName\":\"$ORG_NAME\",\"email\":\"$OWNER_EMAIL\",\"persona\":\"P2_CENTER_OWNER\",\"consentGiven\":true,\"website\":\"\"}")
REQ_ID=$(echo "$REQ" | jget id)
[ -n "$REQ_ID" ] || die "request-beta-access thất bại: $REQ"
ok "Beta request id=$REQ_ID ($OWNER_EMAIL)"

# ── Step 2: admin login ───────────────────────────────────────────────────────
say "2/12 Đăng nhập admin ($ADMIN_EMAIL)"
ADMIN_JWT=$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
[ -n "$ADMIN_JWT" ] || die "admin login thất bại"
ok "Admin JWT đã lấy"

# ── Step 3: approve ───────────────────────────────────────────────────────────
say "3/12 Admin duyệt beta request $REQ_ID"
APP=$(post "$GW/api/v1/admin/beta-requests/$REQ_ID/approve" "{\"approverId\":\"$ADMIN_EMAIL\"}" "$ADMIN_JWT")
[ "$(echo "$APP" | jget status)" = "APPROVED" ] || die "approve thất bại: $APP"
ok "Trạng thái: APPROVED (claim code + invite token đã phát hành)"

# ── Step 4: fetch claim code (DB canonical; MailHog fallback) ──────────────────
say "4/12 Lấy mã claim code (6 chữ số)"
sleep 1
CLAIM=$(docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tAc \
  "SELECT claim_code FROM beta_access_request WHERE id=$REQ_ID;" 2>/dev/null | tr -d '[:space:]')
if [ -z "$CLAIM" ]; then
  # MailHog fallback — parse 6-digit code from the beta email body
  CLAIM=$(curl -s "http://localhost:8025/api/v2/messages?limit=50" --max-time 10 | python3 -c "
import sys, json, re, quopri
items = json.load(sys.stdin).get('items', [])
for it in items:
    to = it.get('Content', {}).get('Headers', {}).get('To', [])
    if any('$OWNER_EMAIL' in t for t in to):
        body = quopri.decodestring(it.get('Content', {}).get('Body', '').encode()).decode('utf-8', 'ignore')
        codes = re.findall(r'(?<!\d)\d{6}(?!\d)', body)
        if codes: print(codes[0]); break
" 2>/dev/null)
fi
[ -n "$CLAIM" ] || die "không lấy được claim code (DB + MailHog đều rỗng)"
ok "Claim code: $CLAIM"

# ── Step 5: exchange claim code → invite token ────────────────────────────────
say "5/12 Đổi claim code lấy invite token"
EX=$(post "$GW/api/v1/auth/beta-signup/exchange-claim-code" "{\"claimCode\":\"$CLAIM\"}")
INVITE_TOKEN=$(echo "$EX" | jget inviteToken)
[ -n "$INVITE_TOKEN" ] || die "exchange-claim-code thất bại: $EX"
ok "Invite token: $INVITE_TOKEN"

# ── Step 6: beta-signup (provision tenant + owner) ────────────────────────────
# NOTE: beta-signup can return an HTML circuit-breaker fallback even on success;
# we verify provisioning via owner login (step 7), not this response body.
say "6/12 Hoàn tất beta-signup (tạo tenant + tài khoản owner)"
post "$GW/api/v1/auth/beta-signup" \
  "{\"token\":\"$INVITE_TOKEN\",\"subdomain\":\"$SUBDOMAIN\",\"ownerPassword\":\"$OWNER_PASSWORD\"}" >/dev/null || true
ok "Đã gọi beta-signup (subdomain=$SUBDOMAIN) — xác minh qua login bước 7"

# ── Step 7: owner login ───────────────────────────────────────────────────────
say "7/12 Đăng nhập owner ($OWNER_EMAIL)"
sleep 1
OWNER_LOGIN=$(post "$GW/api/auth/login" "{\"email\":\"$OWNER_EMAIL\",\"password\":\"$OWNER_PASSWORD\"}")
OWNER_JWT=$(echo "$OWNER_LOGIN" | jget accessToken)
[ -n "$OWNER_JWT" ] || die "owner login thất bại — provisioning có thể đã lỗi: $(echo "$OWNER_LOGIN" | head -c200)"
# Unique phone base derived from run suffix (phone uniqueness is NOT tenant-scoped
# in current backend — see seed-script findings; suffix avoids cross-run collision)
PHONE_BASE="09${SUFFIX}"   # 8 digits; append <group><idx> → 10-digit VN mobile
ROLE=$(echo "$OWNER_LOGIN" | jget user.role)
ok "Owner đăng nhập OK (role=$ROLE) — tenant đã được provision"

# ── Steps 8-12: standard-depth teaching data ──────────────────────────────────
# Sample data VN-friendly per .claude/rules/vn-localization-audit-checklist.md §3
say "8/12 Tạo giảng viên (teachers)"
declare -a TEACHER_IDS=()
ti=0
for t in "Nguyễn Văn An|an.nguyen+${SUFFIX}@skyedu.vn" \
         "Lê Thị Bình|binh.le+${SUFFIX}@skyedu.vn"; do
  IFS='|' read -r tn te <<< "$t"
  tp="${PHONE_BASE}1${ti}"; ti=$((ti+1))
  R=$(post "$GW/api/v1/teachers" "{\"name\":\"$tn\",\"email\":\"$te\",\"phone\":\"$tp\"}" "$OWNER_JWT")
  TID=$(echo "$R" | jget data.id)
  [ -n "$TID" ] || die "tạo teacher '$tn' thất bại: $R"
  TEACHER_IDS+=("$TID"); ok "Giảng viên: $tn (id=$TID)"
done

say "9/12 Tạo khóa học (courses)"
declare -a COURSE_IDS=()
i=0
for c in "SKY-IELTS-RW-${SUFFIX}|Lớp IELTS Reading-Writing 6.5|Khóa luyện IELTS RW mục tiêu 6.5" \
         "SKY-TOEIC-${SUFFIX}|Lớp TOEIC 700+|Khóa luyện TOEIC mục tiêu 700+"; do
  IFS='|' read -r cc cn cd <<< "$c"
  TID="${TEACHER_IDS[$((i % ${#TEACHER_IDS[@]}))]}"
  R=$(post "$GW/api/v1/courses" "{\"teacherId\":$TID,\"code\":\"$cc\",\"name\":\"$cn\",\"description\":\"$cd\"}" "$OWNER_JWT")
  CID=$(echo "$R" | jget data.id)
  [ -n "$CID" ] || die "tạo course '$cn' thất bại: $R"
  COURSE_IDS+=("$CID"); ok "Khóa học: $cn (id=$CID, GV id=$TID)"
  i=$((i+1))
done

say "10/12 Tạo lớp học (classes)"
declare -a CLASS_IDS=()
i=0
for cl in "Lớp IELTS 6.5 - Tối Thứ 246|Thứ 2-4-6, 19:00-21:00|Phòng A1, 123 Lê Lợi, Q.1, TP.HCM" \
          "Lớp TOEIC 700 - Tối Thứ 357|Thứ 3-5-7, 19:00-21:00|Phòng A2, 123 Lê Lợi, Q.1, TP.HCM"; do
  IFS='|' read -r cln cls cld <<< "$cl"
  CID="${COURSE_IDS[$i]}"
  R=$(post "$GW/api/v1/courses/$CID/classes" \
    "{\"name\":\"$cln\",\"schedule\":\"$cls\",\"locationType\":\"IN_PERSON\",\"locationDetail\":\"$cld\",\"startDate\":\"2026-06-01\",\"endDate\":\"2026-08-31\",\"maxStudents\":15}" "$OWNER_JWT")
  CLID=$(echo "$R" | jget data.id)
  [ -n "$CLID" ] || die "tạo class '$cln' thất bại: $R"
  CLASS_IDS+=("$CLID"); ok "Lớp: $cln (id=$CLID, khóa id=$CID)"
  i=$((i+1))
done

say "11/12 Tạo học viên (students)"
declare -a STUDENT_IDS=()
si=0
for s in "Phạm Thị Mai|mai.pham+${SUFFIX}@gmail.com" \
         "Hoàng Văn Nam|nam.hoang+${SUFFIX}@gmail.com" \
         "Đỗ Thị Lan|lan.do+${SUFFIX}@gmail.com" \
         "Vũ Minh Quang|quang.vu+${SUFFIX}@gmail.com"; do
  IFS='|' read -r sn se <<< "$s"
  sp="${PHONE_BASE}2${si}"; si=$((si+1))
  R=$(post "$GW/api/v1/students" "{\"name\":\"$sn\",\"email\":\"$se\",\"phone\":\"$sp\"}" "$OWNER_JWT")
  SID=$(echo "$R" | jget data.id)
  [ -n "$SID" ] || die "tạo student '$sn' thất bại: $R"
  STUDENT_IDS+=("$SID"); ok "Học viên: $sn (id=$SID)"
done

say "12/12 Ghi danh học viên vào lớp (enrollments)"
ENROLL_COUNT=0
# 2 học viên đầu vào lớp IELTS, 2 học viên sau vào lớp TOEIC
for idx in "${!STUDENT_IDS[@]}"; do
  SID="${STUDENT_IDS[$idx]}"
  CLID="${CLASS_IDS[$((idx / 2 % ${#CLASS_IDS[@]}))]}"
  R=$(post "$GW/api/v1/enrollments" "{\"classId\":$CLID,\"studentId\":$SID,\"tuitionAmount\":3500000}" "$OWNER_JWT")
  EID=$(echo "$R" | jget data.id)
  [ -n "$EID" ] || die "ghi danh student=$SID class=$CLID thất bại: $R"
  ENROLL_COUNT=$((ENROLL_COUNT+1)); ok "Ghi danh: học viên id=$SID → lớp id=$CLID (3.500.000đ, id=$EID)"
done

# ── Summary ───────────────────────────────────────────────────────────────────
say "Hoàn tất — Tenant demo '$ORG_NAME' đã seed"
cat <<EOF
  Subdomain     : $SUBDOMAIN
  Owner login   : $OWNER_EMAIL / $OWNER_PASSWORD
  Giảng viên    : ${#TEACHER_IDS[@]}
  Khóa học      : ${#COURSE_IDS[@]}
  Lớp học       : ${#CLASS_IDS[@]}
  Học viên      : ${#STUDENT_IDS[@]}
  Ghi danh      : $ENROLL_COUNT

  Bước 11 (ảnh bìa CC0) BỎ QUA — blocked GAP-798b (storage cần X-User-Reference-Id).
EOF
