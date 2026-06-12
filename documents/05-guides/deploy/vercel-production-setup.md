# Vercel Production Setup — Hướng Dẫn Cài Đặt Env Vars + Custom Domain

**Đối tượng:** Solo dev cài đặt FE production trên Vercel cho `kitehub.me` + `kitehub.vn` lần đầu.
**Closes (PARTIAL):** GAP-457 — covers user-executable actions §5 (env vars) + §6 (custom domain) per session audit 2026-05-09.
**Tham chiếu:** `release-1-deploy-plan.md` §2.1 pre-deploy checklist · `dns-setup-runbook.md` (DNS phải xong trước) · `cloudflare-setup.md` (Cloudflare proxy phải active)
**Last reviewed:** 2026-05-09

---

## 0. Pre-flight — phải xong trước khi vào guide này

Theo thứ tự dependency:

1. ✅ **Domain claim xong** (`account-prep/02b-github-student-pack-free-domain.md` qua Free path Student Pack 2026-05-09 hoặc `02-domain-registrar.md` Paid path)
2. ✅ **Cloudflare nameservers active** (`cloudflare-setup.md` §1-2) — domain trỏ về Cloudflare NS
3. ✅ **Cloudflare DNS records** (`cloudflare-setup.md` §3 + `dns-setup-runbook.md` §2.3) — apex CNAME → Vercel + `api.<domain>` → AWS ALB
4. ⏳ **SSL/TLS Full(strict)** — Vercel apex cert ✅ Let's Encrypt auto-issued; ALB HTTPS listener cert binding **block trên Tier 3** (`release-1-tier-3-cutover.md`)
5. ⏳ **AWS BE running** — EC2 + RDS đang STOPPED cost-save; resume qua `aws-cost-scheduling.md` §4 hoặc `release-1-tier-3-cutover.md` §1

> **Tier 1 + 2 status (per session 2026-05-09/10):** Vercel custom domain + env var ✅; Cloudflare DNS + Email Routing + Origin Cert generated ✅. **Tier 3 cutover** (resume EC2 → ACM import → ALB binding → Cloudflare Full strict) tracked riêng trong `release-1-tier-3-cutover.md`.

Nếu BẤT KỲ pre-flight item nào chưa xong → quay lại guide tương ứng. Vercel setup này KHÔNG hoạt động độc lập — FE phải gọi được backend qua HTTPS để smoke test pass.

---

## 1. Mục đích

Wire Vercel FE production deployments tới AWS BE qua 2 cài đặt:

| Action | Mục đích | Effort |
|---|---|---|
| §5 Environment Variables | FE biết URL backend ở runtime (`NEXT_PUBLIC_API_URL`) | ~10 phút |
| §6 Custom Domain bindings | `kitehub.vn` / `kitehub.me` trỏ về Vercel project, không phải `*.vercel.app` URL | ~15 phút (mỗi project) |

**Kết quả:** truy cập `https://kitehub.vn` → Vercel serve FE bundle → FE call `https://api.kitehub.vn/api/v1/...` → Cloudflare proxy → AWS ALB → backend services.

---

## 2. §5 — Environment Variables (Production)

### 2.1 Đăng nhập Vercel + chọn project

1. Mở https://vercel.com/dashboard
2. Đăng nhập (nếu chưa có account, tạo qua GitHub OAuth — link với repo `VictorAurelius/2026-Kite-Class-Platform`)
3. Chọn project **kitehub** (hoặc **kiteclass** — làm 2 project riêng, cùng quy trình)

### 2.2 Vào Settings → Environment Variables

1. Tab **Settings** trên project header
2. Sidebar trái → **Environment Variables**
3. Trang sẽ hiển thị 3 environment scope: **Production / Preview / Development**

### 2.3 Thêm `NEXT_PUBLIC_API_URL` cho Production

| Field | Giá trị |
|---|---|
| Key | `NEXT_PUBLIC_API_URL` |
| Value (kitehub project) | `https://api.kitehub.vn` |
| Value (kiteclass project) | `https://api.kitehub.me` |
| Environments | ✅ Production (CHỈ check Production; KHÔNG check Preview/Development) |

Click **Save**.

> **Quan trọng:** Tên biến PHẢI prefix `NEXT_PUBLIC_` để Next.js inject vào client-side bundle ở build time. Không có prefix = chỉ available server-side, FE component sẽ không truy cập được.

### 2.4 Các env vars khác cần thêm Production

Theo thứ tự ưu tiên:

| Key | Value | Environments | Mục đích |
|---|---|---|---|
| `NEXT_PUBLIC_API_URL` | `https://api.kitehub.vn` (hoặc kiteclass) | Production | Backend URL (đã làm §2.3) |
| `NEXT_PUBLIC_ENV` | `production` | Production | FE biết đang ở production để bật strict mode + tắt debug logs |
| `NEXT_PUBLIC_SENTRY_DSN` | `https://...@sentry.io/...` | Production | Error tracking (sau khi GAP-113 ship Sentry) |
| `NEXT_PUBLIC_GA_ID` | `G-XXXXXXXXXX` | Production | Google Analytics (nếu enable) |
| `NEXT_PUBLIC_HCAPTCHA_SITE_KEY` | `<hcaptcha key>` | Production | Bot protection (kitehub-frontend dùng cho beta-request form) |

Preview environment có thể dùng cùng giá trị Production HOẶC trỏ về staging URL nếu staging stack live (`staging-activation-runbook.md`).

### 2.5 Verify — không trigger redeploy

- Vercel KHÔNG tự động redeploy khi thêm env var
- Để áp dụng: deploy lần kế tiếp (push to main hoặc create PR) sẽ inherit env mới
- Hoặc click **Redeploy** trên latest deployment trong tab **Deployments** (không cần re-merge code)

---

## 3. §6 — Custom Domain Bindings

### 3.1 Domain plan — pick 1

> **Decision 2026-05-09 (GAP-458):** Release 1 dùng **`kitehub.me`** (Free 1 năm qua GitHub Student Pack — `account-prep/02b-github-student-pack-free-domain.md`). Bảng dưới support cả paid `.vn` lẫn free `.me`.

| Plan | Vercel domain (`.me` Free) | Vercel domain (`.vn` paid) | Backend domain | Phù hợp |
|---|---|---|---|---|
| **A — Apex + subdomain** | `kitehub.me` | `kitehub.vn` | `api.kitehub.me` (hoặc `.vn`) | Phase 1 BETA invite-only — branded, simple |
| **B — Subdomain only** | `app.kitehub.me` | `app.kitehub.vn` | `api.kitehub.me` | Phase 1.5 PAID public — apex cho marketing/landing trên Vercel |
| **C — Beta prefix** | `beta.kitehub.me` | `beta.kitehub.vn` | `api.kitehub.me` | Phase 1 BETA early — apex còn placeholder, beta tách riêng |

**Khuyến nghị Release 1 (Free path):** Plan **A** với `kitehub.me` apex — domain mới claim, không có legacy → dùng apex luôn rõ ràng. KiteClass tenants access qua subdomain pattern `tenant1.kitehub.me`, `tenant2.kitehub.me` (wildcard DNS A record `*.kitehub.me` → ALB; FE Next.js parse subdomain → tenantId per `kiteclass-frontend/src/lib/tenant.ts`).

**Khuyến nghị Phase 1 BETA invite-only (paid path nếu user prefer `.vn`):** Plan **C** (`beta.kitehub.vn` + `beta.kitehub.me`) — apex domain còn `Coming soon` placeholder.

### 3.2 Vào Settings → Domains

1. Project **kitehub** → tab **Settings** → sidebar **Domains**
2. Trang hiển thị existing domain (default: `<project-name>-<hash>.vercel.app`)

### 3.3 Add custom domain

1. Click **Add Domain**
2. Nhập domain (theo plan đã pick): `beta.kitehub.vn` (Plan C) hoặc `kitehub.vn` (Plan A)
3. Click **Add**

### 3.4 Vercel sẽ yêu cầu DNS verification

Vercel hiển thị 2 cách verify:

**Cách 1 (recommended) — Cloudflare CNAME flattening cho apex domain:**

| Type | Name | Value | Proxy |
|---|---|---|---|
| CNAME | `beta` | `cname.vercel-dns.com` | ✅ Proxied (orange cloud) |

Cloudflare auto-flatten CNAME thành A record cho apex/subdomain.

**Cách 2 — A record direct:**

Vercel cung cấp 1 IP (vd `76.76.21.21`); thêm A record:

| Type | Name | Value | Proxy |
|---|---|---|---|
| A | `beta` | `76.76.21.21` | ✅ Proxied |

> **Quan trọng — Cloudflare proxy mode:** Bật proxy (orange cloud) cho 2 lợi ích: DDoS protection + CDN edge cache. Nhưng phải set Cloudflare SSL mode = **Full (strict)** vì Vercel có cert riêng; nếu để Flexible sẽ infinite redirect.

### 3.5 Đợi propagation

- Cloudflare DNS: 1-5 phút
- Verify trong Vercel UI: trang Domains sẽ hiển thị badge **Valid Configuration** khi DNS resolve đúng
- Vercel auto-issue SSL cert (Let's Encrypt) sau khi DNS verify — thêm 1-3 phút

### 3.6 Set primary domain

Sau khi domain `Valid`:

1. Trong Vercel Domains list, click `...` bên cạnh `beta.kitehub.vn` (hoặc apex) → **Set as Primary**
2. Vercel sẽ redirect `<project>.vercel.app` → primary domain (301)

### 3.7 Lặp lại cho project kiteclass

Cùng quy trình §3.2-3.6 cho project **kiteclass** với domain `beta.kitehub.me` (hoặc plan đã chọn).

---

## 4. Verification — smoke test FE → BE

### 4.1 Trình duyệt — manual

1. Mở https://beta.kitehub.vn (hoặc apex per plan)
2. Mở DevTools → tab **Network**
3. Reload trang
4. Verify:
   - ✅ Status 200 cho document HTML
   - ✅ Headers có `cf-cache-status` (Cloudflare proxy active)
   - ✅ TLS valid (browser không cảnh báo cert)
   - ✅ FE load không có error trong console
   - ✅ FE call `https://api.kitehub.vn/api/v1/...` thành công (không CORS error, không 502/503)

Lặp cho `https://beta.kitehub.me`.

### 4.2 CLI — automated check

```bash
# Check FE root
curl -sI https://beta.kitehub.vn | head -5
# Expected: HTTP/2 200 + cf-cache-status header

# Check FE call backend (CORS preflight)
curl -sI -X OPTIONS https://api.kitehub.vn/api/v1/health \
  -H "Origin: https://beta.kitehub.vn" \
  -H "Access-Control-Request-Method: GET" | head -10
# Expected: 204 + Access-Control-Allow-Origin: https://beta.kitehub.vn

# Check Vercel SSL cert chain
curl -sv https://beta.kitehub.vn 2>&1 | grep -E "subject|issuer|expire" | head -5
# Expected: subject=CN=beta.kitehub.vn (Let's Encrypt issued)
```

### 4.3 Cờ đỏ thường gặp

| Symptom | Nguyên nhân | Fix |
|---|---|---|
| `ERR_TOO_MANY_REDIRECTS` | Cloudflare SSL mode = Flexible | Đổi sang Full (strict) |
| `502 Bad Gateway` từ Vercel | Vercel project chưa deploy thành công | Check Vercel Deployments tab — latest deploy phải Ready |
| `503 từ ALB` khi FE call API | EC2 stopped (cost-save) | Resume EC2 per `aws-cost-scheduling.md` §4 |
| CORS error console | BE chưa allow Vercel domain | Update BE `allowed.origins` config + redeploy backend |
| `NEXT_PUBLIC_API_URL is undefined` trong FE bundle | Env var chỉ check Preview/Dev, không Production | Vercel Settings → Environment Variables — verify Production checked |

---

## 5. Pricing — Vercel plans

| Plan | Cost | Phù hợp |
|---|---|---|
| Hobby (Free) | $0/month | Personal projects, dev testing, **non-commercial** |
| Pro | $20/user/month | Commercial paid SaaS, team features, multiple custom domains/project |
| Enterprise | $custom | Large scale, SLA, SSO |

**Phase 1 BETA invite-only:** Hobby Free OK — beta là free trial, technically non-commercial. KHÔNG vi phạm Vercel TOS.

**Phase 1.5 PAID public launch:** PHẢI upgrade Pro ($20/user/month) — paid SaaS = commercial use; Vercel TOS yêu cầu Pro plan. Một user license đủ cho solo dev.

Tham khảo: https://vercel.com/pricing

---

## 6. Out of scope

- Vercel Team setup (chỉ áp dụng nếu có nhiều dev) → tự config khi cần
- Vercel Preview Deployments per branch (mặc định bật cho mọi PR — không cần thay đổi)
- A/B testing / Edge Config / Edge Functions / Speed Insights → Phase 2+
- Vercel Analytics — free tier đủ dev; production analytics qua Google Analytics riêng

---

## 7. Cross-references

- `documents/05-guides/account-prep/02-domain-registrar.md` — mua domain `.vn`
- `documents/05-guides/deploy/cloudflare-setup.md` — Cloudflare account + DNS proxy
- `documents/05-guides/deploy/dns-setup-runbook.md` — DNS records + SSL chain
- `documents/05-guides/deploy/aws-cost-scheduling.md` §4 — resume EC2 + RDS từ cost-save
- `documents/05-guides/deploy/email-ses-setup-runbook.md` — SES production approval (cho beta invite emails)
- `documents/03-planning/roadmap/release-1-deploy-plan.md` §2.1 — Phase 1 BETA pre-deploy checklist
- `.claude/rules/release-deploy-standard.md` §3.4 — production deployment artifact requirements

## 8. Log

- **2026-05-09** Guide created — closes user-flagged audit "có guide tiếng Việt cho tất cả 8 action chưa?" (§5 + §6 Vercel-related actions trước đây không có guide). Combined env vars + custom domain trong 1 file vì cả 2 cài qua cùng UI Vercel Settings; audience giống nhau (solo dev cài lần đầu). GAP-457.
