---
title: G2 Human Test Recipe — KC-12 Reschedule / payroll / gamification / analytics
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff cho KC-12 (class reschedule + payroll read-only + gamification/analytics no-surface)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kc12-reschedule-payroll-gamification.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kc12-reschedule-payroll-gamification.md
---

# G2 Human Test Recipe — KC-12 Reschedule / payroll / gamification / analytics

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Verify class reschedule chạy thật + an toàn (happy 200 + outbox event, IDOR chặn, state-machine guard), payroll backend OK (qua direct vì routing collision), gamification/analytics không có surface walkable.

**Prereq:**
- Stack UP: `kite-gateway` + `kiteclass-core` + `kite-postgres` + `kite-rabbitmq` healthy.
- Đã merge Wave flow-kc12 (PR #2204).
- Class data: tenant `aaaabbbb-…-0001` (owner.test) có class id 4, 5 (IN_PROGRESS).

**Thời lượng:** ~10 phút (thin flow).

## 2. Setup

```bash
G=http://localhost:9000
export JWT_SECRET=$(docker exec kite-gateway sh -c 'printf %s "$JWT_SECRET"')
# Mint ADMIN token (reschedule cần access-to-class; payroll cần ADMIN)
mint(){ python3 -c "import hmac,hashlib,base64,json,time,os;s=os.environ['JWT_SECRET'].encode();b=lambda x:base64.urlsafe_b64encode(x).rstrip(b'=');h=b(json.dumps({'alg':'HS512','typ':'JWT'},separators=(',',':')).encode());n=int(time.time());p=b(json.dumps({'sub':'$1','email':'$2','role':'$3','type':'access','tenantId':'aaaabbbb-0000-0000-0000-000000000001','iat':n,'exp':n+3600},separators=(',',':')).encode());sig=b(hmac.new(s,h+b'.'+p,hashlib.sha512).digest());print((h+b'.'+p+b'.'+sig).decode())"; }
AT=$(mint "11111111-aaaa-0000-0000-000000000001" "admin.kc@test.vn" "ADMIN")
OT=$(curl -s -X POST $G/api/auth/login -H 'Content-Type: application/json' -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r .accessToken)
RC=GV_OM_BAN_DOT_XUAT   # reasonCategory enum hợp lệ
```

## 3. Các bước

### Bước 1 — Reschedule class IN_PROGRESS (state-machine guard)
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/classes/4/reschedule" -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' \
  -d "{\"newStartDate\":\"2026-07-01\",\"newEndDate\":\"2026-09-30\",\"reasonCategory\":\"$RC\"}" -w '\n[%{http_code}]\n' | head -c 200
```
**✅ Kỳ vọng (PASS):** HTTP **400** `CLASS_CANNOT_RESCHEDULE` "Only SCHEDULED classes can be rescheduled" (class 4 đang IN_PROGRESS → guard đúng).

### Bước 2 — Reschedule happy path (class SCHEDULED)
**Hành động:** Đặt class 4 thành SCHEDULED tạm thời rồi reschedule:
```bash
docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tAc "UPDATE classes SET status='SCHEDULED' WHERE id=4;"
curl -s -X POST "$G/api/v1/classes/4/reschedule" -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' \
  -d "{\"newStartDate\":\"2026-07-01\",\"newEndDate\":\"2026-09-30\",\"reasonCategory\":\"$RC\",\"reasonNotes\":\"G2 test\"}" -w '\n[%{http_code}]\n' | head -c 200
```
**✅ Kỳ vọng (PASS):** HTTP **200**, body có `startDate:2026-07-01`, `endDate:2026-09-30`.
**🔍 Verify:**
```bash
docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tAc "SELECT start_date,end_date FROM classes WHERE id=4;"          # → 2026-07-01 / 2026-09-30
docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tAc "SELECT event_type FROM outbox_events WHERE event_type ILIKE '%reschedul%' ORDER BY created_at DESC LIMIT 1;"  # → class.rescheduled
# Cleanup:
docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tAc "UPDATE classes SET status='IN_PROGRESS', start_date='2026-01-01', end_date=NULL WHERE id=4;"
```

### Bước 3 — IDOR: reschedule class của tenant KHÁC (phải bị chặn)
**Hành động:** class 13 thuộc tenant `ad0fa96e-…` (khác tenant của AT):
```bash
curl -s -X POST "$G/api/v1/classes/13/reschedule" -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' \
  -d "{\"newStartDate\":\"2026-07-01\",\"newEndDate\":\"2026-09-30\",\"reasonCategory\":\"$RC\"}" -w '\n[%{http_code}]\n' | head -c 150
```
**✅ Kỳ vọng (PASS):** HTTP **404** `CLASS_NOT_FOUND` (admin bypass authz nhưng lookup tenant-scoped → không thấy class cross-tenant → KHÔNG leak). OWNER token (không phải teacher của class) → **403**.

### Bước 4 — Reschedule với authz OWNER (không phải teacher)
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/classes/4/reschedule" -H "Authorization: Bearer $OT" -H 'Content-Type: application/json' \
  -d "{\"newStartDate\":\"2026-07-01\",\"newEndDate\":\"2026-09-30\",\"reasonCategory\":\"$RC\"}" -w '\n[%{http_code}]\n' | head -c 150
```
**✅ Kỳ vọng (PASS):** HTTP **403** ACCESS_DENIED (`@authz.hasAccessToClass` yêu cầu là teacher của class hoặc admin; OWNER thường không link teacher_id).

### Bước 5 — Payroll (qua gateway → 404 do routing collision)
**Hành động:**
```bash
curl -s "$G/api/v1/admin/payroll/periods" -H "Authorization: Bearer $AT" -w '\n[%{http_code}]\n' -o /dev/null
# Backend thực sự OK qua direct :8080:
docker exec kite-gateway sh -c 'wget -qO- --header="X-User-Id:11111111-aaaa-0000-0000-000000000001" --header="X-User-Roles:ROLE_ADMIN" --header="X-Tenant-Id:aaaabbbb-0000-0000-0000-000000000001" "http://kiteclass-core:8080/api/v1/admin/payroll/periods" 2>&1' | head -c 120
```
**✅ Kỳ vọng (PASS):** qua gateway → **404** "Endpoint not found" (ℹ️ KNOWN-ISSUE GAP-1041 — routing collision, đã filed); direct :8080 → **200** (backend OK, list rỗng). Đây là bằng chứng payroll backend hoạt động, chỉ kẹt routing.

## 4. Sad path quick checks
- Reschedule `reasonCategory` không hợp lệ (`"OTHER"`) → 400 MALFORMED_REQUEST_BODY (enum). Giá trị hợp lệ: `GV_OM_BAN_DOT_XUAT`, `PHONG_HOC_KHONG_KHA_DUNG`, `MAT_DIEN_INTERNET`, `LE_TET_NGHI_CHINH_THUC`, `HOC_SINH_XIN_NGHI_TAP_THE`, `LY_DO_KHAC`.
- Reschedule thiếu `newStartDate`/`newEndDate` → 400.
- Gamification/analytics: KHÔNG có endpoint user-facing (gamification = internal PointService trên attendance; analytics = ReportController đã test ở KC-11). Không có gì để walk — đúng kỳ vọng.

## 5. ⚠️ KNOWN-ISSUE (đã filed — KHÔNG cần báo lại)
| Gap | Mức | Mô tả |
|-----|-----|-------|
| GAP-1041 | 🔴 P0 | Payroll `/api/v1/admin/payroll/**` bị gateway `/api/v1/admin/**` (kitehub-admin) nuốt → 404. Backend OK. Wave security-1/gateway-audit. |
| GAP-1042 | 🟠 P1 | META — gateway route-predicate audit (3 routing collision GAP-1031/1034/1041 cùng root). |
| GAP-1043 | 🟡 P2 | Reschedule chấp nhận past-date (`newStartDate` quá khứ → 200) vì thiếu `@FutureOrPresent`. |

## 6. Báo kết quả
- ✅ **FULL PASS** → Claude flip KC-12 → ✅ G1+G2 (chờ G3).
- ⚠️ **MOSTLY PASS** (cosmetic ngoài KNOWN-ISSUE) → catalog gap polish.
- 🔴 **BLOCKING** (Bước 2 happy reschedule fail) → báo bước + output + HTTP code.
- ❓ **UNCLEAR** → gửi screenshot/error.

## 7. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|-------------|-----------|
| Bước 2 reschedule 403 dù class SCHEDULED | ADMIN token sai role-literal — verify token có `role:ADMIN`. |
| outbox query rỗng | RabbitMQ/outbox chưa flush — chờ vài giây + query lại. |
| `RC` invalid enum | Dùng đúng 1 trong 6 giá trị ở §4. |
| Quên cleanup class 4 | Chạy lại UPDATE ở Bước 2 cleanup (status=IN_PROGRESS, start_date=2026-01-01). |

**G3 production-parity preview:** G3 verify reschedule trên prod-equivalent (gateway JWT→header thật, Postgres+Flyway). Payroll G3 phải confirm GAP-1041 routing fix (payroll reachable qua gateway). Past-date guard (GAP-1043) nên fix trước G3 để tránh dữ liệu lịch không hợp lệ.
