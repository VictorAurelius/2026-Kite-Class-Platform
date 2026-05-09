# GitHub Student Pack — Claim `.me` Domain Free 1 Năm (Path C)

**Đối tượng:** Solo dev đã verified GitHub Student Developer Pack, claim `.me` domain qua Namecheap free 1 năm cho Phase 1 BETA + Phase 1.5 PAID launch.
**Closes (PARTIAL):** GAP-458 — domain procurement free alternative; pair với `02-domain-registrar.md` (.vn paid path).
**Quyết định 2026-05-09:** Domain front door = **`kitehub.me`** (KiteHub là SaaS marketing landing; KiteClass tenants access qua subdomain hoặc path).
**Last reviewed:** 2026-05-09

---

## 0. Pre-flight check

Trước khi bắt đầu:

1. ✅ GitHub account có Student Developer Pack **đã verified** — verify tại https://education.github.com/pack
2. ✅ Email primary GitHub link với student email (`.edu`, `.edu.vn`) hoặc đã upload student ID/transcript
3. ✅ Pack offer "Namecheap — 1 free `.me` domain" vẫn active (check pack benefits page)
4. ⚠️ Chỉ claim **1 lần per Student Pack** — nếu đã từng claim trước đây, không thể claim lại

**Nếu chưa verified Student Pack:**

1. Vào https://education.github.com/pack và click **Get the Pack**
2. Apply với student/educator status:
   - Sinh viên: upload thẻ sinh viên / transcript / email `.edu`
   - Giảng viên: upload teaching credential / faculty email
3. GitHub review: 1-7 ngày (đôi khi instant nếu email `.edu` xác minh tự động)
4. Sau khi approved → quay lại runbook này

---

## 1. Claim `.me` qua Namecheap

### 1.1 Vào Namecheap — Student Pack offer

1. Đăng nhập https://education.github.com/pack
2. Sidebar trái → **All offers** → tìm **Namecheap** card
3. Click **Get access** → Namecheap mở trang Student Pack landing
4. Trang sẽ hiển thị 3 offers:
   - Free `.me` domain (1 year) ← **chọn này**
   - SSL certificate 1 year free
   - WhoisGuard privacy protection forever

### 1.2 Tạo / login Namecheap account

- Nếu chưa có account: Sign up với email khác email GitHub (Namecheap không cho duplicate accounts)
- Nếu đã có: Login

> **Quan trọng:** Namecheap cần verify GitHub Student Pack ownership qua OAuth — KHÔNG bypass bằng cách tạo account mới + manual claim.

### 1.3 Search + claim `kitehub.me`

1. Trong landing page Student Pack, click **Claim free .me**
2. Search box hiện ra → nhập `kitehub`
3. Domain search results sẽ show:
   - `kitehub.me` — **AVAILABLE** ✅ (giả sử) → giá $0.00 với promo
   - Các TLD khác `.com` `.net` etc. — paid, IGNORE
4. Click **Add to cart** cho `kitehub.me`

**Nếu `kitehub.me` đã taken:**

Backup choices theo thứ tự:
1. `kite-hub.me` (hyphenated)
2. `kitehubapp.me`
3. `getkitehub.me`
4. Hoặc switch sang `.tech` (also free 1 year qua `.tech` Domains từ Student Pack)

### 1.4 Checkout — verify $0.00

Cart sẽ hiển thị:

```
kitehub.me — Domain Registration (1 year)
  Price: $0.00 (Student Pack promo)

Subtotal: $0.00
Total: $0.00
```

⚠️ **CỜ ĐỎ** — nếu thấy tổng > $0:
- Có thể Namecheap đang charge cho add-ons (Auto-renew, Premium DNS, SSL upgrade)
- Uncheck tất cả add-ons; chỉ giữ basic registration
- WhoisGuard privacy: free từ Student Pack, OK nếu auto-add

### 1.5 Payment method (cần card dù $0)

Namecheap yêu cầu credit/debit card on file dù total $0 (để bảo vệ chống abuse). Card sẽ KHÔNG bị charge nếu tổng = $0.00.

⚠️ **Cẩn thận với auto-renew:**
- Mặc định Namecheap bật **Auto-renew = ON** sau Year 1
- Phải tắt auto-renew NGAY sau claim để tránh card bị charge $10-20 sau 12 tháng
- Xem §3 dưới

### 1.6 Confirm → ownership

Sau khi checkout success:
1. Email confirmation từ Namecheap
2. Domain xuất hiện trong Namecheap Dashboard → **Domain List**
3. Status: **Active** with expiry date 12 months sau

---

## 2. Cấu hình Cloudflare nameservers

> Chuyển sang follow `documents/05-guides/deploy/cloudflare-setup.md` §1-2 cho phần Cloudflare account setup + nameservers.

Tóm tắt nhanh:

1. Tạo / login Cloudflare account
2. **Add Site** → nhập `kitehub.me`
3. Chọn Free plan
4. Cloudflare scan existing DNS — sẽ rỗng vì domain mới
5. Cloudflare cung cấp 2 nameservers (vd `ana.ns.cloudflare.com` + `bob.ns.cloudflare.com`)
6. Quay lại Namecheap → Domain List → **Manage** `kitehub.me` → **Nameservers** → chọn **Custom DNS**
7. Paste 2 Cloudflare nameservers → Save
8. Đợi propagation 5-30 phút → Cloudflare auto-verify status = **Active**

---

## 3. Tắt Auto-renew (TUYỆT ĐỐI quan trọng)

Để tránh card bị charge $10-20 sau 1 năm:

1. Namecheap Dashboard → **Domain List** → click `kitehub.me`
2. Tab **Domain** → section **Auto-renew settings**
3. Toggle **Auto-renew = OFF** ✅
4. Set calendar reminder 11 tháng từ ngày claim → quyết định renew hay switch sang `.vn` paid

**Nếu sau 1 năm muốn giữ `.me`:**
- Renew thủ công $10-20/year (Namecheap regular price)
- Hoặc transfer sang Cloudflare Registrar — at-cost pricing thường rẻ hơn ~$8-12/year

**Nếu sau 1 năm muốn switch sang `.vn`:**
- Mua `.vn` qua `02-domain-registrar.md` (Mắt Bão / PA Vietnam)
- 301 redirect `kitehub.me` → `kitehub.vn` trong Cloudflare Page Rules
- Email tenants notify URL change ~30 ngày trước cutover

---

## 4. Verification

### 4.1 Cờ check

```bash
# DNS resolution (sau khi Cloudflare nameservers active)
getent hosts kitehub.me  # → Cloudflare IP

# WHOIS check
whois kitehub.me 2>&1 | grep -E "Registrar|Expir|Name Server"
# Expected:
#   Registrar: NameCheap, Inc.
#   Registrar URL: http://www.namecheap.com
#   Registry Expiration Date: 2027-05-09 (~+1 year)
#   Name Server: ana.ns.cloudflare.com
#   Name Server: bob.ns.cloudflare.com
```

### 4.2 Cờ đỏ thường gặp

| Symptom | Nguyên nhân | Fix |
|---|---|---|
| Total checkout > $0 | Add-on enabled (Auto-renew x privacy x SSL) | Uncheck add-ons; chỉ giữ basic registration |
| "Promo not eligible" error | Student Pack chưa verified | Quay lại §0 verify |
| Domain hiển thị giá $9.98 thay vì $0 | Đã claim trước đây | Chọn TLD khác trong Student Pack offer |
| Cloudflare nameserver KHÔNG active sau 1h | Namecheap chưa save Custom DNS | Quay lại Manage → Nameservers — verify Custom DNS đã save |

---

## 5. Cross-references — bước tiếp theo

Sau khi `kitehub.me` claim xong + Cloudflare nameservers active:

1. **Cloudflare DNS records:** [`../deploy/cloudflare-setup.md`](../deploy/cloudflare-setup.md) §3 — A record / CNAME trỏ về AWS ALB
2. **SSL/TLS Full(strict):** [`../deploy/dns-setup-runbook.md`](../deploy/dns-setup-runbook.md) §2.4 — ACM cert + Cloudflare SSL mode
3. **Vercel custom domain bind:** [`../deploy/vercel-production-setup.md`](../deploy/vercel-production-setup.md) §3 — bind `kitehub.me` (apex) hoặc `beta.kitehub.me` (subdomain)
4. **Resume AWS BE compute:** [`../deploy/aws-cost-scheduling.md`](../deploy/aws-cost-scheduling.md) §4
5. **Smoke test verify:** [`../deploy/vercel-production-setup.md`](../deploy/vercel-production-setup.md) §4

---

## 6. KiteClass tenant access pattern

`kitehub.me` cover KiteHub front door (signup + manage). KiteClass tenants access qua 1 trong 3 pattern (chốt theo product decision):

| Pattern | URL example | Setup |
|---|---|---|
| **A — Subdomain per tenant** (Recommended) | `tenant1.kitehub.me`, `tenant2.kitehub.me` | Wildcard DNS A record `*.kitehub.me` → ALB; FE routing parse subdomain → tenantId |
| **B — Path-based** | `kitehub.me/class/tenant1` | Single domain, FE Next.js dynamic route `[tenantSlug]` |
| **C — Single domain shared** | `kitehub.me` cho cả 2 products | KiteClass UI behind login route; 1 codebase combined |

Pattern A phổ biến nhất cho multi-tenant SaaS; KiteClass codebase đã support tenant-from-subdomain (kiểm `kiteclass-frontend/src/lib/tenant.ts` per existing wave). Cloudflare Free tier OK cho wildcard DNS.

---

## 7. Out of scope

- Mua `.vn` paid path → xem [`02-domain-registrar.md`](02-domain-registrar.md)
- Free alternatives khác (`.eu.org`, vercel.app, promo TLDs) → xem GAP-458 §"5 lựa chọn ranked"
- Cloudflare Workers / KV / R2 setup
- Email subdomain MX records (Phase 1.5 PAID khi GAP-370 SES production approved)

## 8. Log

- **2026-05-09** Runbook created — closes GAP-458 Path C decision. User chose Student Pack `.me` route after analyzing 5 free options. Domain choice `kitehub.me` (KiteHub front door SaaS); KiteClass via subdomain Pattern A planned. Auto-renew tắt mandatory.
