---
title: G2 Human Test Recipe — KH-9 Admin console (instance / audit / beta-request management)
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff cho KH-9 (PLATFORM_ADMIN console: dashboard + instance suspend/activate + beta-requests)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kh9-admin-console.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh9-admin-console.md
---

# G2 Human Test Recipe — KH-9 Admin console

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Verify PLATFORM_ADMIN console: dashboard, list instances, suspend/activate instance, beta-requests. Xác nhận gate PLATFORM_ADMIN hiệu lực (non-admin → 403).

**Prereq:**
- Stack UP: `kite-gateway` + `kitehub-admin` + `kitehub-subscription` + `kite-postgres` healthy.
- Đã merge Wave flow-kh9 (PR #2200).

**Thời lượng:** ~12 phút.

## 2. Setup

Admin login (`admin.test@test.vn`) bị **2FA enrollment gate** (by-design). Cho G2 có 2 lựa chọn:

**Cách (a) — mint HS512 PLATFORM_ADMIN JWT (nhanh, khuyến nghị cho G2):**
```bash
G=http://localhost:9000
export JWT_SECRET=$(docker exec kite-gateway sh -c 'printf %s "$JWT_SECRET"')
ADMIN_UUID=$(docker exec kite-postgres psql -U kitehub -d kitehub -tAc "SELECT id FROM users WHERE email='admin.test@test.vn';")
AT=$(python3 -c "import hmac,hashlib,base64,json,time,os;s=os.environ['JWT_SECRET'].encode();b=lambda x:base64.urlsafe_b64encode(x).rstrip(b'=');h=b(json.dumps({'alg':'HS512','typ':'JWT'},separators=(',',':')).encode());n=int(time.time());p=b(json.dumps({'sub':'$ADMIN_UUID','email':'admin.test@test.vn','role':'PLATFORM_ADMIN','type':'access','tenantId':'aaaabbbb-0000-0000-0000-000000000001','iat':n,'exp':n+3600},separators=(',',':')).encode());sig=b(hmac.new(s,h+b'.'+p,hashlib.sha512).digest());print((h+b'.'+p+b'.'+sig).decode())")
echo "admin token len: ${#AT}"
OT=$(curl -s -X POST $G/api/auth/login -H 'Content-Type: application/json' -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r .accessToken)
```

**Cách (b) — hoàn tất 2FA enrollment qua UI** (nếu muốn test full login UX): login → enroll TOTP → verify → nhận accessToken. Lâu hơn; dùng (a) cho G2 nhanh.

## 3. Các bước

### Bước 1 — Dashboard
**Hành động:**
```bash
curl -s "$G/api/platform/admin/dashboard" -H "Authorization: Bearer $AT" -w '\n[%{http_code}]\n' | head -c 200
```
**✅ Kỳ vọng (PASS):** HTTP **200** (số liệu dashboard; tổng có thể = 0 do mock — không phải bug).

### Bước 2 — List instances
**Hành động:**
```bash
curl -s "$G/api/platform/admin/instances" -H "Authorization: Bearer $AT" | jq -c '[.data[]? // .[]? | {id, status}] | .[0:3]' 2>/dev/null
```
**✅ Kỳ vọng (PASS):** HTTP **200**, danh sách ~6 instances với `status`.

### Bước 3 — Suspend instance
**Hành động:** Lấy 1 instanceId từ Bước 2 (gán `IID`), rồi:
```bash
IID=<instanceId-từ-Bước-2>
curl -s -X PATCH "$G/api/platform/admin/instances/$IID/suspend" -H "Authorization: Bearer $AT" -w '\n[%{http_code}]\n' | head -c 150
docker exec kite-postgres psql -U kitehub -d kitehub -tAc "SELECT status FROM instances WHERE id='$IID';" 2>/dev/null
```
**✅ Kỳ vọng (PASS):** HTTP **200**, DB `status = SUSPENDED`.

### Bước 4 — Activate instance (khôi phục)
**Hành động:**
```bash
curl -s -X PATCH "$G/api/platform/admin/instances/$IID/activate" -H "Authorization: Bearer $AT" -w '\n[%{http_code}]\n' | head -c 150
docker exec kite-postgres psql -U kitehub -d kitehub -tAc "SELECT status FROM instances WHERE id='$IID';" 2>/dev/null
```
**✅ Kỳ vọng (PASS):** HTTP **200**, DB `status = ACTIVE` (khôi phục).

### Bước 5 — Beta-requests
**Hành động:**
```bash
curl -s "$G/api/v1/admin/beta-requests" -H "Authorization: Bearer $AT" -w '\n[%{http_code}]\n' | head -c 200
```
**✅ Kỳ vọng (PASS):** HTTP **200**, danh sách beta-requests.

### Bước 6 — Inverse authz: OWNER KHÔNG được vào admin console
**Hành động:**
```bash
curl -s "$G/api/platform/admin/dashboard" -H "Authorization: Bearer $OT" -w '[dashboard %{http_code}]\n' -o /dev/null
curl -s -X PATCH "$G/api/platform/admin/instances/$IID/suspend" -H "Authorization: Bearer $OT" -w '[suspend %{http_code}]\n' -o /dev/null
```
**✅ Kỳ vọng (PASS):** cả 2 → **403** (gate PLATFORM_ADMIN hiệu lực — OWNER không vào được).

## 4. Sad path quick checks
- Suspend instance không tồn tại → 404 (không 500).
- Double-suspend (suspend instance đã SUSPENDED) → hiện **200** thay vì 409 (ℹ️ KNOWN-ISSUE GAP-1030, thiếu state guard).
- Audit-log list (`GET /api/v1/admin/audit-logs`) → hiện **500** (ℹ️ KNOWN-ISSUE GAP-1028). Đừng báo lại.

## 5. ⚠️ KNOWN-ISSUE (đã filed — KHÔNG cần báo lại)
| Gap | Mức | Mô tả |
|-----|-----|-------|
| GAP-1028 | 🟠 P1 | Admin audit-log list → 500 "could not determine data type of parameter $5" (nullable filter; IT pass nhưng live fail — cần investigation). |
| GAP-1029 | 🟠 P1 | Suspend/activate không ghi audit row (thiếu @Auditable) + table drift `admin_audit_log` (V36) vs `admin_audit_logs` (V50). |
| GAP-1030 | 🟡 P2 | Double-suspend → 200 thay vì 409 (thiếu state-machine guard). |

## 6. Báo kết quả
- ✅ **FULL PASS** (Bước 1-6 PASS, KNOWN-ISSUE như mô tả) → Claude flip KH-9 → ✅ G1+G2 (chờ G3).
- ⚠️ **MOSTLY PASS** (cosmetic ngoài KNOWN-ISSUE) → catalog gap.
- 🔴 **BLOCKING** (suspend/activate happy fail) → báo bước + output + HTTP code.
- ❓ **UNCLEAR** → gửi screenshot/error.

## 7. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|-------------|-----------|
| admin token len 0 | JWT_SECRET trống hoặc python3 thiếu — `docker exec kite-gateway sh -c 'echo ${#JWT_SECRET}'` ≥64. |
| Dashboard/instances 403 dù dùng AT | Token role-literal sai — phải là `PLATFORM_ADMIN` (KHÔNG phải alias `ADMIN` cho admin-service). |
| Quên restore instance | Chạy lại Bước 4 activate cho instance đã suspend. |

**G3 production-parity preview:** G3 verify admin console trên prod-equivalent (admin 2FA enrollment thật, gateway JWT→header). G3 nên confirm GAP-1028 audit-log fix (live không 500) + GAP-1029 audit-completeness (suspend/activate ghi audit) trước khi admin dùng production.
