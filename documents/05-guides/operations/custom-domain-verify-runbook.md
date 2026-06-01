# Custom Domain Verify Runbook

**Last verified:** 2026-06-01
**Audience:** Center Owner (PREMIUM/ENTERPRISE tier) + KiteHub support team
**Created:** Wave tenant-domain-1 Bucket D (GAP-812)
**Related:**
- Business rules: `documents/01-business/kitehub/custom-domain/rules.md`
- API contract: `documents/01-business/kitehub/custom-domain/api-contract.md`
- ADR-018 Domain Registrar / DNS / TLD

## 1. Tổng quan

Custom Domain cho phép tenant gắn domain riêng (vd `lop.skyedu.vn`) vào instance KiteClass thay vì chỉ dùng subdomain `{subdomain}.kiteclass.com`.

Yêu cầu:
- Instance ở tier **PREMIUM** hoặc **ENTERPRISE**
- Owner đã đăng ký domain ở vendor riêng và có quyền sửa DNS

**Lưu ý:** Backup URL `{subdomain}.kiteclass.com` **luôn hoạt động** — đảm bảo không downtime trong lúc setup.

## 2. Quy trình setup (Owner-facing)

### Bước 1: Initiate trên KiteHub admin UI

1. Login KiteHub admin → **Settings → Custom Domain**
2. Nhập domain (vd `lop.skyedu.vn`) → click **"Initiate verify"**
3. UI hiển thị TXT record cần thêm:
   ```
   Host:  _kitehub-verify.lop.skyedu.vn
   Type:  TXT
   Value: kitehub-verify=abc12345-67de-...
   TTL:   300 (5 phút) hoặc lowest available
   ```
4. **Copy** giá trị TXT (UI có nút Copy)

### Bước 2: Thêm TXT record ở DNS provider

Xem §3 cho hướng dẫn theo từng vendor VN phổ biến.

### Bước 3: Chờ DNS propagate + verify

1. Chờ **5-15 phút** để DNS propagate (một số provider VN có thể mất 30-60 phút)
2. Test propagate bằng command line:
   ```bash
   dig TXT _kitehub-verify.lop.skyedu.vn +short
   # Expected: "kitehub-verify=abc12345-..."
   ```
   Hoặc dùng tool web: `https://dnschecker.org/#TXT/_kitehub-verify.lop.skyedu.vn`
3. Quay lại KiteHub UI → click **"Verify ngay"**
4. Nếu thành công: badge chuyển **"Verified"** (xanh) → cert provisioning bắt đầu

### Bước 4: Cert provisioning (v1.1+)

- Status **CERT_PROVISIONING** (badge xanh dương) — cert đang được cấp qua Cloudflare for SaaS / AWS ACM
- Thời gian: 5-15 phút
- Trong lúc này, truy cập custom domain sẽ **redirect tạm về backup subdomain** với banner "Cert đang cấp" (SSL-pending fallback)
- Status **VERIFIED** (xanh lá) → custom domain live qua HTTPS

## 3. Hướng dẫn theo DNS provider VN

### 3.1 Mat Bao (matbao.net)

1. Login → **Quản lý tên miền** → chọn domain
2. Click **"Quản lý DNS"** → **"Thêm bản ghi"**
3. Điền:
   - **Loại:** TXT
   - **Tên/Host:** `_kitehub-verify` (chỉ phần trước `.domain.vn`, không nhập domain đầy đủ)
   - **Giá trị:** `kitehub-verify=abc12345-...` (paste từ KiteHub UI)
   - **TTL:** 300
4. Save → chờ propagate 15-30 phút

### 3.2 PA Vietnam (pavietnam.vn)

1. Login → **Hosting/Dịch vụ** → **Quản lý DNS**
2. Click **"Thêm bản ghi mới"**
3. Điền:
   - **Type:** TXT
   - **Host:** `_kitehub-verify`
   - **Points to:** `kitehub-verify=abc12345-...`
   - **TTL:** 300
4. Save

### 3.3 Nhân Hòa (nhanhoa.com)

1. Login → **Quản trị domain** → chọn domain → **DNS records**
2. **Add record** → Type=TXT, Name=`_kitehub-verify`, Value=`kitehub-verify=abc...`, TTL=300
3. Save

### 3.4 Cloudflare (cloudflare.com)

1. Login → chọn domain → **DNS** tab
2. **Add record** → Type=TXT, Name=`_kitehub-verify`, Content=`kitehub-verify=abc...`, TTL=Auto
3. **Proxy status:** DNS only (gray cloud — KHÔNG bật proxy cho TXT record)
4. Save → propagate gần như instant

### 3.5 Namecheap (namecheap.com)

1. Login → **Domain List** → **Manage** → **Advanced DNS**
2. **Add New Record** → Type=TXT Record, Host=`_kitehub-verify`, Value=`kitehub-verify=abc...`, TTL=Automatic
3. Save

## 4. Troubleshooting

### "Verify ngay" return status PENDING (không VERIFIED)

**Cause:** DNS chưa propagate hoặc TXT record sai.

**Steps:**
1. `dig TXT _kitehub-verify.{your-domain} +short` — có thấy record không?
2. Check value khớp **chính xác** với token UI hiển thị (token có thể có dash, không cắt bớt)
3. Một số provider yêu cầu thêm `.` cuối (apex notation) — thử cả 2
4. Đợi thêm 15-30 phút rồi verify lại (DNS propagate có thể chậm với provider VN)

### Quá 48h vẫn PENDING_VERIFY

**Cause:** Timeout cleanup job (BR-DOMAIN-003) flip thành FAILED.

**Resolution:**
- UI hiển thị FAILED → click **"Re-verify"** → token mới được sinh
- Update TXT record với token mới + verify lại

### Custom domain truy cập browser → cert error

**Cause:** Cert đang trong CERT_PROVISIONING (5-15 phút sau verify).

**Resolution:**
- Đợi cert provision xong (status flip VERIFIED)
- Tạm thời dùng backup URL `{subdomain}.kiteclass.com` (luôn có HTTPS hợp lệ)

### CAA / DNSSEC chặn cert issue (hiếm gặp)

**Cause:** Domain có CAA record chỉ allow một CA cụ thể (không có Cloudflare/Amazon).

**Resolution:**
- Check: `dig CAA {your-domain} +short`
- Nếu có CAA: thêm `0 issue "amazon.com"` + `0 issue "letsencrypt.org"` + `0 issue "comodoca.com"` (cho Cloudflare)
- Hoặc remove CAA record nếu không cần

## 5. Operations — Support team

### Check tenant domain status (manual)

```sql
SELECT id, subdomain, custom_domain, domain_status, domain_verified_at,
       domain_verify_token
FROM instances
WHERE custom_domain = 'lop.skyedu.vn';
```

### Force re-verify (support escalation)

```sql
UPDATE instances
SET domain_status = 'PENDING_VERIFY',
    domain_verify_token = 'kitehub-verify=' || gen_random_uuid()::text,
    domain_verified_at = NULL
WHERE id = '<instance-uuid>';
```

Thông báo Owner: token mới đã được sinh, cần update TXT record tương ứng.

### Reset to NONE (gỡ hoàn toàn — emergency)

Equivalent gọi `DELETE /api/instances/{id}/domain` (BR-DOMAIN-012):
```sql
UPDATE instances
SET custom_domain = NULL,
    domain_verify_token = NULL,
    domain_verified_at = NULL,
    domain_status = 'NONE'
WHERE id = '<instance-uuid>';
```

## 6. Cert provisioning (future v1.1+)

**Current state (Wave tenant-domain-1 Bucket D — v1.0):**
- Terraform scaffold ở `infrastructure/terraform-aws/acm-tenant-domains.tf` (apply deferred per `release-deploy-standard.md` §9)
- Cloudflare for SaaS integration deferred
- Manual: ops team có thể tạo ACM cert thủ công cho beta tenant qua AWS console khi cần

**Phase B v1.1 plan:**
- Lambda subscriber tới outbox event `domain.verified` → tự trigger ACM cert request
- Per-tenant ACM cert ARN lưu vào output terraform (đã scaffold)
- Cloudflare Custom Hostname API integration cho production (preferred)

Follow-up gap: `GAP-812-followup-acm-apply-automation` (file sau khi Bucket D scaffold merged).

## 7. Log

- **2026-06-01:** Runbook created — Wave tenant-domain-1 Bucket D (GAP-812). 5 vendor VN guides + troubleshooting + ops team queries.
