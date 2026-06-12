# Cloudflare CDN Setup — Hướng Dẫn Cài Đặt

**Last updated:** 2026-05-09 (GAP-458 — `.me` Free path support)
**Applies to:**
- **Free path Release 1 (Recommended, GAP-458):** `kitehub.me` apex domain — 1 domain duy nhất qua GitHub Student Pack
- **Paid path alternative:** `kitehub.vn` + `kitehub.me` — 2 production domains qua VN registrar
**Related:** GAP-371 (CDN Cloudflare), GAP-369 (DNS production setup), GAP-458 (domain decision `kitehub.me`), Wave 38 Bucket D (staging runbook)
**Tier:** Cloudflare Free (Phase 1 BETA) — xem §11 để đánh giá Pro

---

## §1 Bối Cảnh và Kiến Trúc

Kite Platform dùng **AWS ap-southeast-1 (Singapore)** làm origin (per ADR-025 + `documents/02-architecture/deployment-strategy.md`). Cloudflare đứng trước AWS làm reverse proxy / CDN:

```
User (VN/SEA)
    │
    ▼
Cloudflare Edge (Anycast — nearest PoP)
    │  - DDoS mitigation
    │  - WAF managed rules
    │  - Static asset cache (/_next/*, /static/*)
    │  - SSL termination (Full strict)
    │
    ▼
AWS ALB / EC2 (ap-southeast-1)
    │  - Origin certificate (Cloudflare origin CA)
    │  - Services: kitehub-gateway :8080, kiteclass-gateway :8081
    │
    ▼
Internal services (subscription, branding, core, ...)
```

**Tại sao Cloudflare Free đủ cho Phase 1 BETA:**
- Free tier: unlimited bandwidth, shared WAF, Bot Fight Mode, Always HTTPS, Auto Minify, Brotli
- Không cần custom WAF rules hay advanced analytics ở giai đoạn BETA nhỏ (<5 tenants)
- Upgrade lên Pro khi: traffic >1M requests/day HOẶC cần custom page rules >3 HOẶC cần image optimization

---

## §2 Tạo Account Cloudflare

> **Yêu cầu thao tác thủ công** — không thể tự động hóa

1. Truy cập [https://dash.cloudflare.com/sign-up](https://dash.cloudflare.com/sign-up)
2. Dùng email: `vannkite@outlook.com` (hoặc account organization nếu có)
3. Chọn plan **Free** → continue
4. Xác nhận email

**Sau khi tạo account:**
- Ghi lại **Account ID** (xuất hiện trong URL sau khi login): `https://dash.cloudflare.com/<ACCOUNT_ID>/`
- Lưu Account ID vào GitHub Secrets (`CLOUDFLARE_ACCOUNT_ID`) cho CI/CD sau này (GAP-374)

---

## §3 Thêm Domain vào Cloudflare

> **Free path Release 1 (GAP-458 decision 2026-05-09):** chỉ 1 domain `kitehub.me` — Student Pack chỉ cấp 1 free domain. KiteClass tenants access qua wildcard subdomain `*.kitehub.me` (Pattern A — vd `tenant1.kitehub.me`).
>
> **Paid path alternative:** nếu user mua `.vn` paid sau này, làm lần lượt cho **cả 2 domains** `kitehub.vn` + `kitehub.me`.

### 3.1 Add site

**Free path (Recommended — 1 domain duy nhất):**
```
Cloudflare Dashboard → Add a Site → nhập kitehub.me → Continue
```

**Paid path (2 domains):**
```
Cloudflare Dashboard → Add a Site → nhập kitehub.vn → Continue
... (lặp lại với kitehub.me)
```

Chọn plan **Free** → Continue (scroll xuống cuối list, $0/month — tránh chọn Pro/Business).

### 3.2 Cloudflare scan DNS records

Cloudflare tự scan DNS records hiện tại. **Đối với domain mới claim từ Student Pack** scan sẽ rỗng (đúng — domain chưa có records). **Kiểm tra:**

| Record Type | Name | Expected (post-launch) |
|---|---|---|
| A | `@` (apex) | IP của AWS ALB (sẽ thêm sau §5) |
| CNAME | `*` (wildcard) | `kitehub.me` apex (cho subdomain pattern, Free path) |
| CNAME | `app` hoặc `www` | Vercel project alias (cho FE) |
| A | `api` | IP của AWS ALB |
| MX | `@` | Nếu setup email (SES) — giữ nguyên |
| TXT | `@` | SPF/DKIM/DMARC nếu có (post GAP-370 SES production) |

> **QUAN TRỌNG:** Đảm bảo tất cả records proxied (cam ☁️) trừ MX + records không cần CDN.

Nếu Cloudflare miss records nào, thêm thủ công trước khi proceed (xem §5).

### 3.3 Ghi lại Cloudflare nameservers

Cloudflare sẽ cấp 2 nameservers, ví dụ:
```
ana.ns.cloudflare.com   (mỗi account Cloudflare khác nhau)
bob.ns.cloudflare.com   (mỗi account Cloudflare khác nhau)
```

Ghi lại 2 dòng này — sẽ paste vào Namecheap (Free path) hoặc VN registrar (Paid path) ở §4.

---

## §4 Đổi Nameservers tại Domain Registrar

> **Yêu cầu thao tác thủ công tại registrar** — không thể tự động hóa
> Xem GAP-369 (DNS production setup) để biết chi tiết registrar hiện tại

### 4.1 Tìm registrar

**Free path:** Namecheap (claim qua GitHub Student Pack — `account-prep/02b-github-student-pack-free-domain.md`).

**Paid path:** Domain `.vn` thường được đăng ký qua:
- VNPT/VinaPhone domain (khách hàng enterprise)
- PA Vietnam (pavietnam.vn)
- Mắt Bão (matbao.net)
- Nhân Hòa (nhanhoa.com)

Kiểm tra registrar (nếu không nhớ):
```bash
# Free path
curl -s "https://rdap.verisign.com/com/v1/domain/kitehub.me" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('registrar',{}).get('name','?'))"

# Paid path (cần whois CLI cài qua apt-get install whois)
whois kitehub.vn | grep -i "registrar\|name server"
```

### 4.2 Thay nameservers

**Free path (Namecheap):**

1. Mở https://ap.www.namecheap.com/domains/list/
2. Click **Manage** bên cạnh `kitehub.me`
3. Tab **Domain** (mặc định mở) → scroll xuống section **Nameservers**
4. Dropdown chọn **Custom DNS** (mặc định "Namecheap BasicDNS")
5. 2 ô input xuất hiện → paste 2 Cloudflare nameservers từ §3.3
6. Click ✓ (checkmark) bên phải để save
7. Confirm popup nếu hiện

**Paid path (VN registrar):**

Login vào registrar panel → DNS Management / Nameserver Settings → xóa nameservers cũ → nhập 2 nameservers Cloudflare từ §3.3.

**Thời gian propagation:**
- Namecheap (`.me`): 5-30 phút (thường nhanh)
- VN `.vn` registrars: 2-24 giờ (thường <4h)

### 4.3 Xác nhận propagation

```bash
# Free path — check từ DNS resolver công khai
getent hosts kitehub.me
# (hoặc nếu có dig: dig NS kitehub.me @8.8.8.8 ; dig NS kitehub.me @1.1.1.1)

# Paid path
dig NS kitehub.vn @8.8.8.8
dig NS kitehub.vn @1.1.1.1

# Kết quả mong đợi: nameservers của Cloudflare
```

Hoặc dùng [https://dnschecker.org/](https://dnschecker.org/) để kiểm tra toàn cầu.

Sau khi propagation xong, Cloudflare Dashboard sẽ hiện **"Active"** status cho domain.

---

## §5 Cấu Hình DNS Records (Proxied)

> **Free path note (GAP-458):** examples dưới đây dùng `kitehub.vn` từ thiết kế cũ. Khi user đi Free path `kitehub.me`, mentally substitute mọi `*.kitehub.vn` → `*.kitehub.me`. Pattern wildcard subdomain `*.kitehub.me` = wildcard `*.kitehub.vn` về mặt logic CDN/cache.


Sau khi domain Active trong Cloudflare, vào **DNS** tab và cấu hình:

### 5.1 Records bắt buộc

| Type | Name | Content | Proxy | TTL |
|---|---|---|---|---|
| A | `@` | `<AWS_ALB_IP hoặc EC2_IP>` | ☁️ Proxied | Auto |
| A | `www` | `<AWS_ALB_IP hoặc EC2_IP>` | ☁️ Proxied | Auto |
| CNAME | `api` | `<AWS_ALB_DNS>` | ☁️ Proxied | Auto |

> **Note:** Nếu dùng AWS ALB (hostname, không phải IP), record `@` apex không thể là CNAME. Giải pháp: dùng Cloudflare **CNAME Flattening** (tự động với Free tier) — add CNAME record cho `@` trỏ về ALB DNS name, Cloudflare tự flatten.

### 5.2 Kiểm tra proxy status

Tất cả records cần thiết phải có biểu tượng **cam ☁️** (proxied), không phải **xám ☁️** (DNS only).

Records không nên proxy:
- MX records (email)
- TXT records (SPF/DKIM/DMARC)
- Records cho subdomain không dùng CDN (internal tools)

---

## §6 SSL/TLS — Full (Strict) Mode

> **Quan trọng:** Full (strict) yêu cầu origin server có certificate hợp lệ.

### 6.1 Bật Full (strict) mode

```
Cloudflare Dashboard → SSL/TLS → Overview → chọn "Full (strict)"
```

**Tại sao Full strict, không phải Full:**
- `Full`: Cloudflare verify cert nhưng accept self-signed → dễ bị MITM giữa Cloudflare và origin
- `Full (strict)`: Cloudflare verify cert phải do trusted CA ký → an toàn hơn

### 6.2 Cài đặt Cloudflare Origin Certificate trên AWS

> **Trạng thái 2026-05-10:** Origin Cert đã generate qua Cloudflare Dashboard (Path C — `release-1-tier-3-cutover.md` §0 explains why CLI route fail with token combo). Files saved tới `~/.gcal-mcp/cloudflare-origin-cert/{kitehub.me.pem, kitehub.me.key}`. Subject: `CloudFlare Origin Certificate`, validity 2026-05-10 → 2041-05-06 (~15 năm), SAN: `kitehub.me + *.kitehub.me`, RSA 2048. ALB import + binding tracked trong **Tier 3 cutover runbook** (`release-1-tier-3-cutover.md` §2). Step content dưới đây retained cho tham khảo formal procedure khi need re-issue cert tương lai.

Cloudflare Origin Certificate là free certificate cho encrypted traffic giữa Cloudflare và origin server:

```
Cloudflare Dashboard → SSL/TLS → Origin Server → Create Certificate
```

Cấu hình:
- **Private key type:** RSA (2048)
- **Hostnames:** `kitehub.vn`, `*.kitehub.vn` (wildcard)
- **Certificate validity:** 15 years (default)

Download:
- `kitehub.vn.pem` — Origin certificate
- `kitehub.vn.key` — Private key

**Cài trên AWS:**

Nếu dùng **AWS ALB**:
```bash
# Upload certificate lên ACM hoặc IAM
aws acm import-certificate \
  --certificate fileb://kitehub.vn.pem \
  --private-key fileb://kitehub.vn.key \
  --region ap-southeast-1

# Attach certificate vào ALB listener HTTPS :443
aws elbv2 add-listener-certificates \
  --listener-arn <ALB_LISTENER_ARN> \
  --certificates CertificateArn=<CERT_ARN>
```

Nếu dùng **EC2 + Nginx**:
```nginx
# /etc/nginx/sites-available/kitehub.vn
server {
    listen 443 ssl;
    server_name kitehub.vn www.kitehub.vn;

    ssl_certificate     /etc/ssl/cloudflare/kitehub.vn.pem;
    ssl_certificate_key /etc/ssl/cloudflare/kitehub.vn.key;

    # Cloudflare IP ranges only (security hardening — §8)
    # allow 103.21.244.0/22; (xem §8 cho đầy đủ)

    location / {
        proxy_pass http://localhost:8080;  # kitehub-gateway
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $http_cf_connecting_ip;
        proxy_set_header X-Forwarded-For $http_cf_connecting_ip;
    }
}
```

### 6.3 Bật Always Use HTTPS

```
Cloudflare Dashboard → SSL/TLS → Edge Certificates → Always Use HTTPS → ON
```

### 6.4 Bật HSTS

```
Cloudflare Dashboard → SSL/TLS → Edge Certificates → HTTP Strict Transport Security (HSTS)
```

Cấu hình:
- **Status:** Enabled
- **Max-Age:** 6 months (15768000 seconds) — bắt đầu conservative, tăng sau
- **Include subdomains:** Yes
- **Preload:** No (bật sau khi stable ≥6 tháng)

---

## §7 Page Rules — Cache + Bypass

### 7.1 Cấu hình Page Rules (Free: tối đa 3 rules)

Vào **Rules → Page Rules** (hoặc **Cache Rules** nếu dùng new UI):

**Rule 1 — Cache Next.js static assets:**
```
URL pattern: *.kitehub.vn/_next/*
Setting:     Cache Level = Cache Everything
             Edge Cache TTL = 1 month
```

**Rule 2 — Cache generic static:**
```
URL pattern: *.kitehub.vn/static/*
Setting:     Cache Level = Cache Everything
             Edge Cache TTL = 1 week
```

**Rule 3 — Bypass cache cho API:**
```
URL pattern: *.kitehub.vn/api/*
Setting:     Cache Level = Bypass
```

> **Free tier giới hạn 3 Page Rules.** Ưu tiên: static assets > API bypass > CDN behaviors khác.
> Nếu cần thêm rules → upgrade Pro ($20/mo) hoặc dùng **Cache Rules** mới (beta, flexible hơn).

### 7.2 Làm tương tự cho kitehub.me

Rules tương tự với pattern `*.kitehub.me/...`

---

## §8 Security Settings

### 8.1 Bot Fight Mode

```
Cloudflare Dashboard → Security → Bots → Bot Fight Mode → ON
```

Chặn common bots + scrapers tự động (free, không cần thêm cấu hình).

### 8.2 WAF Managed Rules

```
Cloudflare Dashboard → Security → WAF → Managed Rules
```

- **Cloudflare Managed Ruleset** → Deploy (free tier)
- **OWASP Core Ruleset** → Deploy (free tier, sensitivity = Medium)

### 8.3 Security Level

```
Cloudflare Dashboard → Security → Settings → Security Level → Medium
```

Medium = block known bad IPs + challenge suspicious traffic. Có thể tăng lên High nếu thấy attack patterns.

### 8.4 Restrict origin chỉ nhận traffic từ Cloudflare IPs (recommended)

Cấu hình AWS Security Group hoặc Nginx để chỉ accept traffic từ [Cloudflare IP ranges](https://www.cloudflare.com/ips/):

```bash
# Script helper để lấy Cloudflare IPs mới nhất
curl -s https://www.cloudflare.com/ips-v4 | while read ip; do
  echo "allow $ip;"
done
```

Cấu hình Nginx:
```nginx
# /etc/nginx/conf.d/cloudflare-ips.conf
# Auto-generated — cập nhật khi Cloudflare thêm IP ranges mới
allow 103.21.244.0/22;
allow 103.22.200.0/22;
allow 103.31.4.0/22;
allow 104.16.0.0/13;
allow 104.24.0.0/14;
allow 108.162.192.0/18;
allow 131.0.72.0/22;
allow 141.101.64.0/18;
allow 162.158.0.0/15;
allow 172.64.0.0/13;
allow 173.245.48.0/20;
allow 188.114.96.0/20;
allow 190.93.240.0/20;
allow 197.234.240.0/22;
allow 198.41.128.0/17;
deny all;
```

---

## §9 Speed Settings

### 9.1 Auto Minify

```
Cloudflare Dashboard → Speed → Optimization → Content Optimization
```

Bật:
- [x] JavaScript
- [x] CSS
- [x] HTML

> **Note:** Có thể conflict với Next.js built output đã minified. Nếu thấy lỗi JS sau bật → tắt HTML minify trước, test lại.

### 9.2 Brotli Compression

```
Cloudflare Dashboard → Speed → Optimization → Content Optimization → Brotli → ON
```

Brotli nén tốt hơn Gzip ~15-20%, giảm bandwidth origin.

### 9.3 HTTP/2 + HTTP/3 (QUIC)

```
Cloudflare Dashboard → Network → HTTP/2 → ON
Cloudflare Dashboard → Network → HTTP/3 (with QUIC) → ON
```

HTTP/3 giảm latency đáng kể với mobile users ở VN (high packet loss networks).

---

## §10 Smoke Verification

Sau khi cấu hình xong và propagation hoàn tất, chạy verify script:

```bash
# Verify kitehub.vn
bash scripts/verify-cdn-headers.sh kitehub.vn

# Verify kitehub.me
bash scripts/verify-cdn-headers.sh kitehub.me

# Kết quả mong đợi (exit 0):
# [PASS] CF-Ray: detected
# [PASS] CF-Cache-Status: detected
# [PASS] Server: cloudflare detected
# [PASS] Strict-Transport-Security: detected
# All Cloudflare CDN headers present — CDN active.
```

### 10.1 Manual checks bổ sung

```bash
# Check SSL certificate issuer (phải là Cloudflare, Inc.)
curl -vI https://kitehub.vn 2>&1 | grep -E "issuer|subject|SSL"

# Check HTTP → HTTPS redirect
curl -I http://kitehub.vn

# Check cache hit cho static asset
curl -I https://kitehub.vn/_next/static/chunks/main.js | grep CF-Cache-Status
# Mong đợi: HIT (sau request đầu tiên)

# Check API bypass (không được cache)
curl -I https://kitehub.vn/api/v1/health | grep CF-Cache-Status
# Mong đợi: BYPASS hoặc MISS
```

### 10.2 Cloudflare Analytics

Sau 24h traffic:
```
Cloudflare Dashboard → Analytics & Logs → Traffic
```

Kiểm tra:
- **Cached requests %** — mục tiêu >60% sau vài ngày (static assets chiếm nhiều)
- **Threats blocked** — Bot Fight Mode + WAF hoạt động
- **Bandwidth saved** — so sánh cached vs uncached

---

## §11 Free vs Pro — Trade-off Analysis

| Feature | Free | Pro ($20/mo) | Verdict Phase 1 BETA |
|---|---|---|---|
| Bandwidth | Unlimited | Unlimited | — |
| DDoS protection | Basic (L3/L4) | Advanced (L3/L4/L7) | Free OK cho BETA |
| WAF managed rules | Cloudflare + OWASP | + Custom rules | Free OK |
| Page Rules | 3 | 20 | ⚠️ Tight — cần quản lý cẩn thận |
| SSL certificates | Shared cert | Dedicated cert | Free OK |
| Cache Rules (new) | 10 | 100 | Free OK với 10 |
| Image optimization | No | Polish + Mirage | No (Next.js tự handle) |
| Priority support | Community | Email | Community OK cho BETA |
| Workers | 100k req/day | Unlimited | Not needed Phase 1 |
| Analytics | 24h | 72h history | 24h OK Phase 1 |

**Khuyến nghị Phase 1 BETA:** Dùng **Free**. Evaluate upgrade lên **Pro** khi:
- Traffic > 500k requests/day (cần analytics history >24h)
- Cần >3 Page Rules (cần cache rules granular)
- Attack patterns vượt quá Free WAF coverage
- Cần custom error pages / custom response headers

**Ước tính chi phí:** Pro = $20/tháng × 2 domains = $40/tháng. Tính vào release budget từ Phase 2.

---

## Troubleshooting

### Domain hiển thị "Pending" trong Cloudflare

- Nameservers chưa propagate (chờ 2-24h)
- Registrar chưa save nameservers mới (kiểm tra lại)
- `dig NS kitehub.vn` vẫn trả về nameservers cũ

### Lỗi SSL "ERR_SSL_VERSION_OR_CIPHER_MISMATCH"

- Origin không có certificate hợp lệ (xem §6.2)
- SSL mode đang là "Full" không phải "Full (strict)" — kiểm tra lại
- Origin server chạy HTTP không phải HTTPS → cài nginx HTTPS listener

### Cache không hoạt động (CF-Cache-Status: MISS liên tục)

- Page Rules chưa đúng pattern (test với `curl -I` và xem header)
- Origin response có header `Cache-Control: no-store` → override bằng Cloudflare Cache Rule
- Content type không được cache mặc định (vd: HTML — cần explicit "Cache Everything")

### API trả 403 / blocked

- WAF đang block legitimate traffic → giảm OWASP sensitivity xuống Low
- Security Level quá cao → đổi về Low tạm thời để debug
- Bot Fight Mode block API client → whitelist client IP trong Firewall Rules

### Timeout sau khi enable Cloudflare

- Cloudflare không connect được đến origin
- Security Group AWS chưa allow Cloudflare IP ranges (§8.4)
- ALB health check fail → kiểm tra target group health

---

## Related Documents

- `documents/04-quality/gaps/GAP-371-cdn-cloudflare-setup.md` — Gap file tracking status
- `documents/04-quality/gaps/GAP-369-dns-production-setup.md` — DNS records setup (prerequisite)
- `documents/02-architecture/deployment-strategy.md` — Architecture overview (AWS ap-southeast-1)
- `documents/05-guides/deploy/deploy-go-nogo-checklist.md` — Pre-launch checklist
- `scripts/verify-cdn-headers.sh` — Automated verification script
- Wave 38 Bucket D — Staging activation runbook (staging DNS + CDN test environment)
