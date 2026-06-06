---
title: G2 Human Test Recipe — KC-10 Per-tenant branding wizard
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff cho KC-10 (KiteClass per-tenant branding: settings + version + rollback)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kc10-per-tenant-branding-wizard.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kc10-per-tenant-branding-wizard.md
---

# G2 Human Test Recipe — KC-10 Per-tenant branding wizard

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Verify per-tenant branding (KiteClass): xem + cập nhật branding settings (logo/favicon/màu), version history + rollback. Xác nhận IDOR cross-tenant bị chặn. Lưu ý 3/5 controller bị routing collision (đi qua direct :8080).

**Prereq:**
- Stack UP: `kite-gateway` + `kiteclass-core` + `kite-postgres` + `kite-minio` healthy.
- Đã merge Wave flow-kc10 (PR #2202).

**Thời lượng:** ~12 phút.

## 2. Setup

```bash
G=http://localhost:9000
export JWT_SECRET=$(docker exec kite-gateway sh -c 'printf %s "$JWT_SECRET"')
OT=$(curl -s -X POST $G/api/auth/login -H 'Content-Type: application/json' -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r .accessToken)
# STAFF token để test authz (mint vì seed staff password khác)
ST=$(python3 -c "import hmac,hashlib,base64,json,time,os;s=os.environ['JWT_SECRET'].encode();b=lambda x:base64.urlsafe_b64encode(x).rstrip(b'=');h=b(json.dumps({'alg':'HS512','typ':'JWT'},separators=(',',':')).encode());n=int(time.time());p=b(json.dumps({'sub':'deadbeef-0000-0000-0000-0000000000ff','email':'staff.walk@test.vn','role':'STAFF','type':'access','tenantId':'aaaabbbb-0000-0000-0000-000000000001','iat':n,'exp':n+3600},separators=(',',':')).encode());sig=b(hmac.new(s,h+b'.'+p,hashlib.sha512).digest());print((h+b'.'+p+b'.'+sig).decode())")
INST=aaaabbbb-0000-0000-0000-000000000001   # tenant instanceId của owner.test
```

## 3. Các bước

### Bước 1 — Xem branding settings (cold-start defaults)
**Hành động:**
```bash
curl -s "$G/api/v1/settings/branding" -H "Authorization: Bearer $OT" | jq '.data | {displayName, primaryColor, accentColor}'
```
**✅ Kỳ vọng (PASS):** HTTP 200, defaults KiteClass (`displayName:"KiteClass"`, `primaryColor:"#3B82F6"`...).

### Bước 2 — Cập nhật branding (OWNER)
**Hành động:**
```bash
curl -s -X PUT "$G/api/v1/settings/branding" -H "Authorization: Bearer $OT" -H 'Content-Type: application/json' \
  -d '{"displayName":"Sky Test Academy","primaryColor":"#FF5733","secondaryColor":"#222222","accentColor":"#10B981","tagline":"G2 test"}' \
  -w '\n[%{http_code}]\n' | jq -c '.data | {id, displayName, primaryColor}' 2>/dev/null
```
**✅ Kỳ vọng (PASS):** HTTP **200**, body có `displayName:"Sky Test Academy"`, `primaryColor:"#FF5733"`.
**⚠️ Sad path:** thiếu `accentColor` → 400 "Accent color is required" (cả 3 màu primary/secondary/accent bắt buộc).

### Bước 3 — Version history + rollback (qua direct :8080 — routing collision)
**Hành động:** `/api/v1/branding/**` bị gateway shadow (xem KNOWN-ISSUE GAP-1034), test backend qua direct:
```bash
docker exec kite-gateway sh -c 'wget -qO- --header="X-User-Id:e1507d51-7f65-42a7-a909-f12500946138" --header="X-User-Roles:ROLE_OWNER" --header="X-Tenant-Id:aaaabbbb-0000-0000-0000-000000000001" "http://kiteclass-core:8080/api/v1/branding/aaaabbbb-0000-0000-0000-000000000001/versions" 2>&1' | head -c 200
```
**✅ Kỳ vọng (PASS):** direct :8080 → 200 (danh sách versions, có thể rỗng). Qua gateway `/api/v1/branding/.../versions` → **401** (ℹ️ KNOWN-ISSUE GAP-1034 routing shadow).

### Bước 4 — IDOR: đọc versions của tenant KHÁC (phải bị chặn)
**Hành động:** instance `11111111-…` (thanglong) khác tenant của OWNER:
```bash
docker exec kite-gateway sh -c 'wget -S -qO- --header="X-User-Id:e1507d51-7f65-42a7-a909-f12500946138" --header="X-User-Roles:ROLE_OWNER" --header="X-Tenant-Id:aaaabbbb-0000-0000-0000-000000000001" "http://kiteclass-core:8080/api/v1/branding/11111111-1111-1111-1111-111111111111/versions" 2>&1 | grep "HTTP/"'
```
**✅ Kỳ vọng (PASS):** HTTP **400** (tenant-mismatch — KHÔNG leak versions của tenant khác). IDOR DEFENDED ✅.

### Bước 5 — Authz: STAFF cập nhật branding (ℹ️ KNOWN-ISSUE)
**Hành động:**
```bash
curl -s -X PUT "$G/api/v1/settings/branding" -H "Authorization: Bearer $ST" -H 'Content-Type: application/json' \
  -d '{"displayName":"STAFF-TEST","primaryColor":"#000000","secondaryColor":"#111111","accentColor":"#222222"}' \
  -w '\n[%{http_code}]\n' | head -c 120
```
**⚠️ Kỳ vọng (KNOWN-ISSUE):** Hiện STAFF → **200** (sửa được branding) = lỗ hổng GAP-1035 A01 (BrandingController thiếu @PreAuthorize). **Đây là bug đã biết — đừng báo lại.** Sau fix sẽ là 403.

> Nhớ reset branding sau test: PUT lại với `displayName:"Sky Test Academy"` ở Bước 2.

## 4. Sad path quick checks
- Logo upload (`POST /api/v1/settings/branding/logo`, multipart field `logo`) → hiện **500** NoSuchBucketException (ℹ️ KNOWN-ISSUE GAP-1036 — bucket `kiteclass-files` chưa tạo trong MinIO). Đừng test upload cho tới khi bucket được seed.
- PUT branding sai format JSON → 400.

## 5. ⚠️ KNOWN-ISSUE (đã filed — KHÔNG cần báo lại)
| Gap | Mức | Mô tả |
|-----|-----|-------|
| GAP-1034 | 🔴 P0 | Gateway routing collision: `/api/v1/branding/**` đi nhầm kitehub-branding → 3/5 controller KC-10 shadow → login page mất tenant branding. |
| GAP-1035 | 🟠 P1 | BrandingController thiếu @PreAuthorize → STAFF sửa được branding (A01). |
| GAP-1036 | 🟠 P1 | Logo/favicon upload → 500 (bucket `kiteclass-files` thiếu trong MinIO). |
| GAP-1037 | 🟡 P2 | Logo cho phép `image/svg+xml` → SVG-XSS latent. |
| GAP-1038 | 🟢 P3 | `rebrand_approvals` orphan — flow "→ approval" là misnomer (rollback = apply). |

## 6. Báo kết quả
- ✅ **FULL PASS** (Bước 1-4 PASS, Bước 5 = KNOWN-ISSUE như mô tả) → Claude flip KC-10 → ✅ G1+G2 (chờ G3).
- ⚠️ **MOSTLY PASS** (cosmetic ngoài KNOWN-ISSUE) → catalog gap.
- 🔴 **BLOCKING** (Bước 1/2 settings fail) → báo bước + output.
- ❓ **UNCLEAR** → gửi screenshot/error.

## 7. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|-------------|-----------|
| Bước 2 PUT 400 dù đủ field | Kiểm tra cả 3 màu primary/secondary/accent đều có. |
| Bước 3 direct :8080 timeout | kiteclass-core chưa healthy — `docker ps`. |
| Quên reset branding | PUT lại Bước 2 với displayName gốc. |

**G3 production-parity preview:** G3 verify branding trên prod-equivalent. **Bắt buộc** confirm GAP-1034 routing fix (3 controller reachable qua gateway + login page render tenant branding) + GAP-1035 authz (STAFF → 403) + GAP-1036 bucket seed trước khi expose production.
