#!/bin/bash
# seed-demo-trio-academic.sh — enrich academic data (enrollment + buổi học + điểm danh +
# điểm tổng kết + học phí/thanh toán) cho bộ 3 GIẢNG VIÊN ĐỘC LẬP của thesis Chương 3.
#
#   1. Cô Đỗ Lan Khánh    — khanh-phapluat  (THPT · Pháp luật)  — TENANT CHÍNH
#   2. Cô Nguyễn Thị Hà    — ha-toantieuhoc  (Tiểu học · Toán)
#   3. Thầy Nguyễn Đình Nhì — nhi-hoathcs     (THCS · Hóa)
#
# Vì sao tách script này (KHÔNG sửa seed-demo-independent-teachers.sh)?
#   - seed-demo-independent-teachers.sh tạo teacher/course/class/HS (base). Script NÀY enrich
#     academic depth lên data base đó → dashboard/báo cáo trông "sống", không rỗng.
#   - Run order:
#         GATEWAY=... bash kitehub/scripts/seed-demo-independent-teachers.sh   # base (teacher/course/class/HS)
#         GATEWAY=... bash kitehub/scripts/seed-demo-trio-academic.sh           # enrich (enroll/buổi/điểm danh/điểm/học phí)
#
# Cơ chế (production-accurate qua gateway API + 2 thao tác SQL set-state cục bộ):
#   - enroll HS vào lớp (API) → auto-tạo enrollment + invoice (OVERDUE) + grade skeleton.
#   - activate enrollment → ACTIVE (roster điểm danh chỉ lấy ACTIVE).
#   - set class dates + tuition + IN_PROGRESS (SQL) → schedule sinh buổi trong khoảng.
#   - schedule buổi học (API) → các buổi Thứ 2-4 trong khoảng -35..+7 ngày.
#   - điểm danh bulk (API) trên buổi ĐÃ DIỄN RA → phân bố PRESENT/LATE/ABSENT/EXCUSED/MAKEUP thật.
#   - record-payment (API) ~2/3 invoice → mix PAID / PARTIAL / OVERDUE.
#   - finalize grade (SQL) → final_score/letter/gpa/status thật cho gradebook.
#
# IDEMPOTENT: re-run an toàn — enroll 409 skip, attendance ON CONFLICT skip, payment skip nếu đã PAID,
#   grade finalize chỉ UPDATE skeleton chưa có final_score.
#
# Usage:
#   GATEWAY=http://localhost:9000 bash kitehub/scripts/seed-demo-trio-academic.sh
#   SEED_TENANTS="khanh nhi"  bash ...    # mặc định cả 3; Hà thường đã enrich sẵn
#   PG_CONTAINER=kite-postgres PG_DB=kiteclass_shared ...  # override DB cục bộ
#
# Access sau khi seed (production-accurate nip.io, per g1-browser-walk §3.1):
#   http://khanh-phapluat.127.0.0.1.nip.io:3000   owner khanh.do@gmail.com / Khanh@2026
#   http://ha-toantieuhoc.127.0.0.1.nip.io:3000   owner ha.nguyen@gmail.com / HaToan@2026
#   http://nhi-hoathcs.127.0.0.1.nip.io:3000      owner nhi.nguyen@gmail.com / Nhi@2026
set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:9000}"
PG_CONTAINER="${PG_CONTAINER:-kite-postgres}"
PG_DB="${PG_DB:-kiteclass_shared}"
PG_USER="${PG_USER:-kitehub}"
SEED_TENANTS="${SEED_TENANTS:-khanh ha nhi}"

green="\033[0;32m"; yellow="\033[1;33m"; red="\033[0;31m"; nc="\033[0m"
ok()   { echo -e "  ${green}\xE2\x9C\x93${nc} $1"; }
warn() { echo -e "  ${yellow}\xE2\x9A\xA0${nc} $1"; }
err()  { echo -e "  ${red}\xE2\x9C\x97${nc} $1"; }

psql_q() { docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tAc "$1" 2>/dev/null; }

# ------------------------------------------------------------
# enrich_tenant: $1 subdomain  $2 owner_email  $3 owner_password  $4 tuition
# ------------------------------------------------------------
enrich_tenant() {
  local SUB="$1" EMAIL="$2" PASS="$3" TUITION="$4"
  echo "----------------------------------------------"
  echo -e "  Tenant: ${yellow}$SUB${nc}  (tuition=${TUITION}đ)"
  echo "----------------------------------------------"

  # instance_id (resolve từ kitehub.instances — KHÔNG hardcode)
  local IID
  IID=$(docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d kitehub -tAc \
    "SELECT id FROM instances WHERE subdomain='$SUB';" 2>/dev/null)
  [ -z "$IID" ] && { err "không tìm thấy instance subdomain=$SUB"; return 1; }

  # 1. Owner login
  local TOK
  TOK=$(curl -s -X POST "$GATEWAY/api/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" \
    | python3 -c "import sys,json;print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
  [ -z "$TOK" ] && { err "owner login FAIL ($EMAIL)"; return 1; }
  local H=(-H "X-Instance-Subdomain: $SUB" -H "X-User-Id: 1" -H "Authorization: Bearer $TOK" -H "Content-Type: application/json")
  ok "owner login OK (instance=$IID)"

  # 2. Resolve lớp duy nhất + students
  local CLID
  CLID=$(curl -s "${H[@]}" "$GATEWAY/api/v1/classes?page=0&size=50" | python3 -c "
import sys,json
d=json.load(sys.stdin); c=d.get('data',{}); c=c.get('content',c) if isinstance(c,dict) else c
print(c[0]['id'] if c else '')" 2>/dev/null)
  [ -z "$CLID" ] && { err "không có lớp cho $SUB (chạy seed-demo-independent-teachers.sh trước)"; return 1; }
  ok "lớp id=$CLID"

  # 3. Set class dates + tuition + IN_PROGRESS (SQL) → schedule sinh buổi trong khoảng quá khứ→tương lai gần
  psql_q "UPDATE classes SET start_date=CURRENT_DATE - INTERVAL '35 days',
            end_date=CURRENT_DATE + INTERVAL '7 days',
            tuition_amount=$TUITION, tuition_type='fixed', status='IN_PROGRESS', updated_at=now()
          WHERE id=$CLID AND instance_id='$IID';" >/dev/null
  ok "class dates/tuition/status set (IN_PROGRESS, học phí ${TUITION}đ)"

  # 3.5 Publish courses → hiện trong catalog công khai (publicApi.getCourses lọc PUBLISHED only,
  #     per GAP-958 data-honesty). Seed tạo course để DRAFT → catalog rỗng. Publish yêu cầu
  #     syllabus + objectives + durationWeeks>0 (CourseServiceImpl.validatePublishRequirements);
  #     set VN-default nếu null (SQL) rồi publish qua API (production-accurate state transition).
  psql_q "UPDATE courses SET
            syllabus = COALESCE(NULLIF(syllabus,''), 'Nội dung khóa học theo lộ trình giảng dạy của giảng viên.'),
            objectives = COALESCE(NULLIF(objectives,''), 'Trang bị kiến thức nền tảng và kỹ năng theo chương trình.'),
            duration_weeks = COALESCE(NULLIF(duration_weeks,0), 12)
          WHERE instance_id='$IID' AND deleted=false;" >/dev/null
  local CIDS_DRAFT PUB=0
  CIDS_DRAFT=$(curl -s "${H[@]}" "$GATEWAY/api/v1/courses?status=DRAFT&page=0&size=100" | python3 -c "
import sys,json
d=json.load(sys.stdin); c=d.get('data',{}); c=c.get('content',c) if isinstance(c,dict) else c
print(' '.join(str(x['id']) for x in c))" 2>/dev/null)
  for cid in $CIDS_DRAFT; do
    rc=$(curl -s -o /dev/null -w "%{http_code}" "${H[@]}" -X POST "$GATEWAY/api/v1/courses/$cid/publish")
    [ "$rc" = "200" ] && PUB=$((PUB+1))
  done
  local PUBN
  PUBN=$(curl -s "${H[@]}" "$GATEWAY/api/v1/courses?status=PUBLISHED&page=0&size=100" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('totalElements','?'))" 2>/dev/null)
  ok "publish course: $PUB mới | tổng PUBLISHED=$PUBN (catalog công khai)"

  # 4. Enroll tất cả HS (idempotent 409 skip) → auto-tạo enrollment + invoice + grade skeleton
  local SIDS NEW=0
  SIDS=$(curl -s "${H[@]}" "$GATEWAY/api/v1/students?page=0&size=300" | python3 -c "
import sys,json
d=json.load(sys.stdin); c=d.get('data',{}); c=c.get('content',c) if isinstance(c,dict) else c
print(' '.join(str(s['id']) for s in c))" 2>/dev/null)
  for sid in $SIDS; do
    local rc
    rc=$(curl -s -o /dev/null -w "%{http_code}" "${H[@]}" -X POST "$GATEWAY/api/v1/enrollments" \
      -d "{\"studentId\":$sid,\"classId\":$CLID,\"tuitionAmount\":$TUITION}")
    [ "$rc" = "201" ] && NEW=$((NEW+1))
  done

  # 5. Activate enrollment chưa ACTIVE (roster điểm danh chỉ lấy ACTIVE)
  local EIDS_PENDING ACT=0
  EIDS_PENDING=$(curl -s "${H[@]}" "$GATEWAY/api/v1/enrollments/class/$CLID?page=0&size=300" | python3 -c "
import sys,json
d=json.load(sys.stdin); c=d.get('data',{}); c=c.get('content',c) if isinstance(c,dict) else c
print(' '.join(str(e['id']) for e in c if e.get('status')!='ACTIVE'))" 2>/dev/null)
  for eid in $EIDS_PENDING; do
    local rc
    rc=$(curl -s -o /dev/null -w "%{http_code}" "${H[@]}" -X PUT "$GATEWAY/api/v1/enrollments/$eid/status" \
      -d '{"status":"ACTIVE","notes":"Demo seed academic: payment confirmed"}')
    [ "$rc" = "200" ] && ACT=$((ACT+1))
  done
  ok "enroll: $NEW mới | kích hoạt: $ACT → ACTIVE"

  # 6. Schedule buổi học (Thứ 2 + Thứ 4) — dùng owner token
  local SCH SCH_CODE
  SCH=$(curl -s -w "\n%{http_code}" "${H[@]}" -X POST "$GATEWAY/api/v1/classes/$CLID/schedule" \
    -d '{"daysOfWeek":["MONDAY","WEDNESDAY"],"startTime":"18:00:00","endTime":"19:30:00"}')
  SCH_CODE=$(echo "$SCH" | tail -1)
  case "$SCH_CODE" in
    201|200) ok "schedule buổi học OK (HTTP $SCH_CODE)";;
    409|400) warn "schedule đã tồn tại (HTTP $SCH_CODE) — skip";;
    *) warn "schedule HTTP $SCH_CODE";;
  esac

  # 7. Điểm danh trên buổi ĐÃ DIỄN RA (direct SQL — reliable).
  #    Lý do dùng SQL thay vì API bulk: endpoint /attendance bulk flaky khi gọi liên tiếp
  #    nhiều buổi (intermittent fail dưới load) → seed không đáng tin. SQL deterministic +
  #    khớp schema thật (status UPPERCASE PRESENT/LATE/ABSENT/EXCUSED/MAKEUP + enrollment_id
  #    cho uk_attendance_enrollment_session). Phân bố ~78% có mặt, rải trễ/vắng/phép/bù.
  #    DELETE trước → re-insert clean (trio attendance do seed này tạo, an toàn xoá).
  psql_q "
    DELETE FROM attendance a USING class_sessions cs
      WHERE a.session_id = cs.id AND cs.class_id = $CLID;
    INSERT INTO attendance (instance_id, session_id, student_id, enrollment_id, status,
                            marked_at, marked_date, points_awarded, version, deleted, created_at, updated_at)
    SELECT '$IID', s.session_id, e.student_id, e.enrollment_id,
      CASE
        WHEN ((e.rn*13 + s.sn*7 + e.rn*s.sn) % 100) < 78 THEN 'PRESENT'
        WHEN ((e.rn*13 + s.sn*7 + e.rn*s.sn) % 100) < 86 THEN 'LATE'
        WHEN ((e.rn*13 + s.sn*7 + e.rn*s.sn) % 100) < 93 THEN 'ABSENT'
        WHEN ((e.rn*13 + s.sn*7 + e.rn*s.sn) % 100) < 97 THEN 'EXCUSED'
        ELSE 'MAKEUP' END,
      (s.sdate + time '18:00')::timestamptz, (s.sdate)::timestamptz, 0, 0, false, now(), now()
    FROM (SELECT en.id AS enrollment_id, en.student_id, row_number() OVER (ORDER BY en.student_id) AS rn
          FROM enrollments en WHERE en.instance_id = '$IID' AND en.class_id = $CLID AND en.status='ACTIVE') e
    CROSS JOIN (SELECT cs.id AS session_id, cs.session_number AS sn, cs.session_date AS sdate
                FROM class_sessions cs WHERE cs.class_id = $CLID AND cs.session_date <= CURRENT_DATE) s;" >/dev/null
  local ATT
  ATT=$(psql_q "SELECT count(*)||' điểm danh / '||count(DISTINCT session_id)||' buổi'
                FROM attendance WHERE instance_id='$IID';")
  ok "điểm danh: $ATT"

  # 8. Record-payment ~2/3 invoice → mix PAID / PARTIAL / (OVERDUE giữ nguyên)
  local PAID
  PAID=$(python3 - "$GATEWAY" "$TOK" "$SUB" "$TUITION" <<'PY'
import sys, json, urllib.request
gw, tok, sub, tuition = sys.argv[1], sys.argv[2], sys.argv[3], float(sys.argv[4])
def req(path, method="GET", body=None):
    h = {"X-Instance-Subdomain": sub, "Authorization": "Bearer "+tok, "Content-Type": "application/json", "X-User-Id": "1"}
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(gw+path, data=data, headers=h, method=method)
    try:
        d = json.load(urllib.request.urlopen(r, timeout=20)); return d
    except Exception: return None
# Lấy danh sách invoice (chưa PAID) qua endpoint list
d = req("/api/v1/invoices?page=0&size=300")
c = (d or {}).get('data', {}); c = c.get('content', c) if isinstance(c, dict) else c
invs = c if isinstance(c, list) else []
paid = part = 0
for i, inv in enumerate(invs):
    iid = inv.get('id'); status = (inv.get('status') or '').upper()
    if status in ('PAID',): continue
    # phân bố: i%3==0 unpaid (giữ OVERDUE) | i%3==1 partial 50% | else full
    m = i % 3
    if m == 0: continue
    amt = round(tuition/2) if m == 1 else tuition
    method = ["CASH", "BANK_TRANSFER", "VIETQR"][i % 3]
    ok = req(f"/api/v1/invoices/{iid}/record-payment", "POST", {"method": method, "amount": amt})
    if ok:
        if m == 1: part += 1
        else: paid += 1
print(f"{paid} PAID | {part} PARTIAL | còn lại OVERDUE")
PY
)
  ok "thanh toán: $PAID"

  # 9. Finalize grade (SQL) trên grade skeleton (chưa final_score) → final_score/letter/gpa/status thật
  local TEACHER_ID
  TEACHER_ID=$(psql_q "SELECT id FROM teachers WHERE instance_id='$IID' ORDER BY id LIMIT 1;")
  psql_q "
    WITH ranked AS (
      SELECT g.id, row_number() OVER (ORDER BY g.student_id) AS rn
      FROM grades g
      WHERE g.instance_id='$IID' AND g.class_id=$CLID AND g.final_score IS NULL AND g.deleted=false
    ), scored AS (
      SELECT id, round((55 + (rn*11) % 45)::numeric, 1) AS sc FROM ranked
    )
    UPDATE grades g SET
      final_score = s.sc,
      letter_grade = CASE WHEN s.sc>=85 THEN 'A' WHEN s.sc>=70 THEN 'B' WHEN s.sc>=55 THEN 'C' ELSE 'D' END,
      gpa = CASE WHEN s.sc>=85 THEN 4.0 WHEN s.sc>=70 THEN 3.0 WHEN s.sc>=55 THEN 2.0 ELSE 1.0 END,
      status = CASE WHEN s.sc>=50 THEN 'PASSED' ELSE 'FAILED' END,
      comments = 'Điểm tổng kết khóa học (demo seed)',
      calculated_at = now(), finalized_at = now(), finalized_by = ${TEACHER_ID:-NULL},
      updated_at = now()
    FROM scored s WHERE g.id = s.id;" >/dev/null
  local GF
  GF=$(psql_q "SELECT count(*) FROM grades WHERE instance_id='$IID' AND class_id=$CLID AND final_score IS NOT NULL AND deleted=false;")
  ok "điểm tổng kết finalize: $GF grade có final_score"

  # 10. Summary
  echo -e "  ${green}→${nc} http://$SUB.127.0.0.1.nip.io:3000  ($EMAIL / $PASS)"
}

echo "=============================================="
echo "  Seed Academic — Bộ 3 GV độc lập (idempotent)"
echo "  Tenants: $SEED_TENANTS"
echo "=============================================="

[[ " $SEED_TENANTS " == *" khanh "* ]] && enrich_tenant "khanh-phapluat" "khanh.do@gmail.com"  "Khanh@2026"  800000
[[ " $SEED_TENANTS " == *" ha "*    ]] && enrich_tenant "ha-toantieuhoc" "ha.nguyen@gmail.com" "HaToan@2026" 500000
[[ " $SEED_TENANTS " == *" nhi "*   ]] && enrich_tenant "nhi-hoathcs"    "nhi.nguyen@gmail.com" "Nhi@2026"    700000

echo "=============================================="
echo -e "  ${green}\xE2\x9C\x93 Seed academic complete${nc} — walk dashboard/báo cáo để confirm data sống."
echo "=============================================="
