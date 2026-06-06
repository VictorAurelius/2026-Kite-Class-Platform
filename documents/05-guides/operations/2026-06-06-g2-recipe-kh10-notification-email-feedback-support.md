---
title: G2 Human Test Recipe — KH-10 Notification / email / feedback / support
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff cho KH-10 (feedback + notification-preferences + admin-email console + support menu)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kh10-notification-email-feedback-support.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh10-notification-email-feedback-support.md
---

# G2 Human Test Recipe — KH-10 Notification / email / feedback / support

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Con người tự verify 4 sub-flow KH-10 chạy thật trên local stack: gửi feedback (anonymous + có đăng nhập), notification preferences (xem + bật/tắt), admin email console (history/stats/config/trigger), support menu links. Xác nhận trải nghiệm đúng + các security gate.

**Prereq:**
- Local stack UP: `docker ps` thấy `kite-gateway` + `kitehub-subscription` + `kitehub-email` + `kite-postgres` + `kite-mailhog` đều `healthy`. Nếu chưa → `cd kitehub && ./scripts/up.sh` rồi chờ ~60s.
- Đã merge Wave flow-kh10 (PR #2201).
- Seed users tồn tại: `owner.test@test.vn` / `Test@1234` (OWNER), `admin.test@test.vn` (PLATFORM_ADMIN, 2FA-gated).

**Thời lượng:** ~12-15 phút.

## 2. Setup

- Mở terminal cho curl. (Tùy chọn) Mở browser + DevTools Network tab nếu muốn test qua FE.
- MailHog UI: `http://localhost:8025` (xem email side-effect).
- Lấy OWNER token:
```bash
G=http://localhost:9000
OT=$(curl -s -X POST $G/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r .accessToken)
echo "owner token len: ${#OT}"   # >300 = OK
```
- Admin token: admin login bị **2FA enrollment gate** (by-design). Cho G2 admin-email, mint HS512 PLATFORM_ADMIN JWT:
```bash
export JWT_SECRET=$(docker exec kite-gateway sh -c 'printf %s "$JWT_SECRET"')
ADMIN_UUID=$(docker exec kite-postgres psql -U kitehub -d kitehub -tAc \
  "SELECT id FROM users WHERE email='admin.test@test.vn';")
AT=$(python3 -c "import hmac,hashlib,base64,json,time,os;s=os.environ['JWT_SECRET'].encode();b=lambda x:base64.urlsafe_b64encode(x).rstrip(b'=');h=b(json.dumps({'alg':'HS512','typ':'JWT'},separators=(',',':')).encode());n=int(time.time());p=b(json.dumps({'sub':'$ADMIN_UUID','email':'admin.test@test.vn','role':'PLATFORM_ADMIN','type':'access','tenantId':'aaaabbbb-0000-0000-0000-000000000001','iat':n,'exp':n+3600},separators=(',',':')).encode());sig=b(hmac.new(s,h+b'.'+p,hashlib.sha512).digest());print((h+b'.'+p+b'.'+sig).decode())")
echo "admin token len: ${#AT}"
```

## 3. Các bước

### Bước 1 — Gửi feedback ẩn danh (anonymous)
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/feedback" -H 'Content-Type: application/json' \
  -d '{"rating":5,"comment":"Trải nghiệm tốt — G2 test","category":"GENERAL","email":"khach@example.com","pageUrl":"/dashboard"}' -w '\n[%{http_code}]\n'
```
**✅ Kỳ vọng (PASS):** HTTP **201**, body có `"status":"RECEIVED"` + `id` (uuid).
**⚠️ Sad path:** category sai (`"category":"HACKER"`) → 400 VALIDATION_ERROR "category must be one of: BUG, USABILITY, FEATURE_REQUEST, GENERAL".
**🔍 Verify (tùy chọn):** `docker exec kite-postgres psql -U kitehub -d kitehub -tAc "SELECT rating,category,client_ip FROM feedback_submissions ORDER BY created_at DESC LIMIT 1;"` → thấy row mới, client_ip có giá trị.

### Bước 2 — Gửi feedback khi đã đăng nhập (OWNER)
**Hành động:**
```bash
curl -s -X POST "$G/api/v1/feedback" -H "Authorization: Bearer $OT" -H 'Content-Type: application/json' \
  -d '{"rating":4,"comment":"Feedback có đăng nhập","category":"BUG"}' -w '\n[%{http_code}]\n'
```
**✅ Kỳ vọng (PASS):** HTTP **201**. DB row có `user_id` + `tenant_id` đính kèm (khác Bước 1 ẩn danh).
**⚠️ Sad path:** thiếu `rating` → 400 "rating is required"; comment <5 ký tự → 400 "comment must be 5-2000 chars".

### Bước 3 — Notification preferences (cold-start defaults)
**Hành động:**
```bash
curl -s "$G/api/v1/notification-preferences" -H "Authorization: Bearer $OT" | jq '.preferences[] | {notificationType, mandatory}'
```
**✅ Kỳ vọng (PASS):** HTTP 200, danh sách defaults (ABSENCE/FEE_REMINDER/EXAM_RESULT = `mandatory:false`; TRIAL_ENDING/BILLING_INVOICE/SECURITY_ALERT = `mandatory:true`).
**⚠️ Sad path:** bỏ token → 401.

### Bước 4 — Bật/tắt notification preference
**Hành động:**
```bash
# Tắt 1 loại không bắt buộc
curl -s -X PATCH "$G/api/v1/notification-preferences/ABSENCE" -H "Authorization: Bearer $OT" \
  -H 'Content-Type: application/json' -d '{"enabledChannels":[]}' -w '\n[%{http_code}]\n'
# Thử tắt loại BẮT BUỘC → phải bị chặn
curl -s -X PATCH "$G/api/v1/notification-preferences/SECURITY_ALERT" -H "Authorization: Bearer $OT" \
  -H 'Content-Type: application/json' -d '{"enabledChannels":[]}' -w '\n[%{http_code}]\n'
```
**✅ Kỳ vọng (PASS):** ABSENCE → **200**; SECURITY_ALERT → **400** `MANDATORY_TYPE_CANNOT_BE_DISABLED`.
**⚠️ Sad path:** loại không hợp lệ (`/api/v1/notification-preferences/__BAD__`) → 400 `INVALID_NOTIFICATION_TYPE`.

### Bước 5 — Admin email console (cần admin token)
**Hành động:**
```bash
curl -s "$G/api/platform/admin/emails/history?page=0&size=3" -H "Authorization: Bearer $AT" -w '\n[%{http_code}]\n' | head -c 200
curl -s "$G/api/platform/admin/emails/stats" -H "Authorization: Bearer $AT" | jq
# Inverse-authz: OWNER token KHÔNG được vào admin console
curl -s "$G/api/platform/admin/emails/stats" -H "Authorization: Bearer $OT" -w '[%{http_code}]\n' -o /dev/null
```
**✅ Kỳ vọng (PASS):** history → 200 (paged); stats → 200 (`totalSentToday`, `failedToday`...); OWNER → **403** (gate PLATFORM_ADMIN hiệu lực).
**🔍 Verify:** `failedToday` thường = 0 (ℹ️ KNOWN-ISSUE GAP-1032 P2 — `email_sent_log` thiếu cột status nên metric này luôn ~0; không phải bug bạn cần báo).

### Bước 6 — Support menu (FE links)
**Hành động:** Mở browser tới các route FE: `/beta-status`, `/help`, `/help/anonymous`. Hoặc verify file tồn tại:
```bash
ls kitehub/kitehub-frontend/src/app/'(public)'/beta-status/page.tsx kitehub/kitehub-frontend/src/app/help/anonymous/page.tsx 2>/dev/null
```
**✅ Kỳ vọng (PASS):** Các page render (hoặc file tồn tại). Support menu có link `mailto:support@kitehub.me` + Zalo OA.

## 4. Sad path quick checks
- Feedback comment >2000 ký tự → 400.
- PATCH notification với JSON sai format → 400.
- Admin trigger resend cùng loại email/instance → **409 dedup** (ℹ️ KNOWN-ISSUE GAP-1033 P3 — admin manual resend bị idempotency chặn; không phải bug bạn cần báo).

## 5. ⚠️ KNOWN-ISSUE (đã filed — KHÔNG cần báo lại)
| Gap | Mức | Mô tả |
|-----|-----|-------|
| GAP-1031 | 🔴 P0 | `POST /api/platform/emails/send` gửi email **không cần auth** (gateway pass-through × email service zero-security). Đây là lỗ hổng đã biết, đang chờ Wave security-1. **Đừng test gửi email tùy ý.** |
| GAP-1032 | 🟡 P2 | admin stats `failedToday` luôn ~0 (thiếu cột status). |
| GAP-1033 | 🟢 P3 | admin trigger resend → 409 dedup. |

## 6. Báo kết quả
Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** → Claude flip KH-10 → ✅ G1+G2 (chờ G3 production-parity).
- ⚠️ **MOSTLY PASS** (chỉ cosmetic, không phải KNOWN-ISSUE ở trên) → catalog gap polish.
- 🔴 **BLOCKING** (1 bước happy-path fail bất ngờ) → báo bước + output + HTTP code; Claude fix loop + re-walk.
- ❓ **UNCLEAR** → gửi screenshot/error, Claude điều tra.

## 7. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|-------------|-----------|
| `owner token len: 0` | Stack chưa up hoặc seed thiếu — `cd kitehub && ./scripts/up.sh`; check `docker exec kite-postgres psql -U kitehub -d kitehub -tAc "SELECT email FROM users;"`. |
| Feedback 401 thay vì 201 (anonymous) | Feedback phải là public endpoint — nếu 401, gateway whitelist drift; báo BLOCKING. |
| admin token len 0 | python3 thiếu hoặc JWT_SECRET trống — check `docker exec kite-gateway sh -c 'echo ${#JWT_SECRET}'` ≥64. |

**G3 production-parity preview:** Sau G2, G3 sẽ verify trên production-equivalent (cùng Docker image tag, gateway JWT→header thật, prod-profile). Riêng KH-10: G3 phải xác nhận GAP-1031 (email arbitrary-send) đã đóng — production gateway KHÔNG được expose `/api/platform/emails`.
