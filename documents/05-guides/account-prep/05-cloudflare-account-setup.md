# 05 — Cloudflare Account Setup Runbook

**Audience:** Solo dev tạo Cloudflare account lần đầu cho Phase 1 BETA — DNS authoritative + CDN proxy + DDoS protection cho `kitehub.me`.
**Standards:** AWS Well-Architected (Reliability + Security) · ADR-025 (AWS Singapore origin) · `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2.
**Cross-link upstream:** Yêu cầu hoàn tất `01-aws-account-creation.md` (ALB DNS sẽ trỏ về AWS) + `02-domain-registrar.md` HOẶC `02b-github-student-pack-free-domain.md` (sở hữu domain `kitehub.me`).
**Cross-link downstream:** Blocks `documents/05-guides/deploy/cloudflare-setup.md` (production DNS records + Worker config) + `documents/05-guides/deploy/dns-setup-runbook.md` (apex/CNAME records) + `documents/05-guides/operations/email-deliverability-runbook.md` (SPF/DKIM/DMARC).
**Estimated time:** ~1h (chưa kể đợi nameserver propagation 5 min – 24h).
**Last-Updated:** 2026-05-15

---

## TL;DR

> Tạo tài khoản Cloudflare Free → add zone `kitehub.me` → chuyển nameservers từ registrar về Cloudflare → cấu hình DNS records (A/CNAME cho ALB, MX cho Resend, TXT cho SPF/DKIM/DMARC) → bật SSL/TLS Full (strict) + DDoS auto + API token least-privilege cho CI.

Quick path 6 bước cho Phase 1 BETA:

1. Signup tại [dash.cloudflare.com/sign-up](https://dash.cloudflare.com/sign-up) (Free plan)
2. Add Site `kitehub.me` → import existing DNS records
3. Copy 2 Cloudflare nameservers → update tại registrar (Namecheap / GitHub Student Pack Porkbun)
4. Verify zone status `Active` (chờ propagation 5 min – 24h)
5. Configure DNS records (A cho ALB, CNAME `app` cho Vercel, MX + TXT cho Resend)
6. Generate API token scope `Zone.DNS:Edit` cho `kitehub.me` zone → lưu vào AWS Secrets Manager `kitehub/production/cloudflare-api-token`

---

## 1. Trước khi bắt đầu — chuẩn bị

| Item | Yêu cầu |
|------|---------|
| Email | Email riêng cho Cloudflare account (recommend `cloudflare-admin@kitehub.me` sau khi MX records ready hoặc `kitehub.cloudflare@gmail.com` tạm). KHÔNG dùng email cá nhân chính. |
| Domain | `kitehub.me` đã active tại registrar (Phase 1 BETA Free path Student Pack — Porkbun). Phải có quyền update nameservers. |
| Tài khoản registrar | Login credentials cho registrar (Porkbun / Namecheap) để đổi nameservers. |
| AWS ALB DNS name | Sẽ có sau Phase 2.3 production apply (vd `kitehub-prod-alb-xxx.ap-southeast-1.elb.amazonaws.com`). Có thể defer record creation đến sau khi ALB live. |
| Vercel project URL | Cần cho CNAME `app.kitehub.me` → `cname.vercel-dns.com`. Defer nếu chưa setup Vercel (`07-vercel-account-setup.md`). |
| Resend domain DKIM records | 3 CNAME records từ Resend dashboard sau khi add domain (`06-resend-account-setup.md`). Defer nếu chưa setup Resend. |
| Password manager | Vault active (`03-password-manager.md`) để lưu Cloudflare credentials + API tokens. |

⚠️ **Critical:** Nameserver change = downtime risk. Schedule trong off-hours nếu domain đang serve traffic. Phase 1 BETA pre-launch = an toàn chuyển bất kỳ lúc nào.

---

## 2. Step-by-step

### 2.1 Signup Cloudflare Free (~10 min)

1. Mở [dash.cloudflare.com/sign-up](https://dash.cloudflare.com/sign-up).
2. Email: nhập email Cloudflare chuẩn bị §1.
3. Password: ≥12 chars, mix upper/lower/digit/special. **Lưu vào password manager NGAY.**
4. Click "Create Account" → verify email qua link Cloudflare gửi (~1 min).
5. Login → dashboard hiển thị "Add a Site" button.

### 2.2 Add zone `kitehub.me` (~5 min)

1. Click "Add a Site" → nhập `kitehub.me` (apex, không có `www.`) → Continue.
2. Chọn **Free plan** ($0/month) → Continue. Pro upgrade defer Phase 1.5+ khi cần WAF advanced + image optimization.
3. Cloudflare scan existing DNS records từ registrar (nếu có) → import tự động. Review danh sách:
   - Nếu records empty (domain mới mua) → skip, sẽ add manual §2.5
   - Nếu có records cũ (vd parking page) → uncheck records không cần
4. Click "Continue" → Cloudflare assign 2 nameservers (vd `aria.ns.cloudflare.com` + `kai.ns.cloudflare.com`).

⚠️ **Lưu 2 nameservers này** — cần cho §2.3.

### 2.3 Update nameservers tại registrar (~10 min + propagation 5min-24h)

Theo registrar:

#### Porkbun (Free path Student Pack)

1. Login [porkbun.com/account/login](https://porkbun.com/account/login).
2. Domain Management → click `kitehub.me`.
3. Section "Authoritative Nameservers" → click "Edit".
4. Remove default Porkbun nameservers → add 2 Cloudflare nameservers (§2.2).
5. Save → confirm.

#### Namecheap (Paid path — nếu mua qua Namecheap)

1. Login [ap.www.namecheap.com](https://ap.www.namecheap.com).
2. Domain List → `kitehub.me` → Manage.
3. Section "Nameservers" → chọn dropdown "Custom DNS".
4. Nhập 2 Cloudflare nameservers → tick green checkmark.

#### GoDaddy (nếu áp dụng)

1. Login [dcc.godaddy.com](https://dcc.godaddy.com).
2. My Products → DNS → `kitehub.me`.
3. Nameservers → "Change" → "I'll use my own nameservers".
4. Nhập 2 Cloudflare nameservers → save.

Sau khi update:
- Cloudflare dashboard → check zone status. Initial state = `Pending`. Sau propagation = `Active`.
- DNS propagation: 5 min – 24h (thường <2h cho .me TLD).
- Verify: `dig NS kitehub.me +short` → expect 2 Cloudflare nameservers.

### 2.4 SSL/TLS Full (strict) mode (~5 min)

⚠️ **DO THIS BEFORE adding production traffic records.** Default mode `Flexible` = HTTPS Cloudflare → HTTP origin = insecure middle-mile. `Full (strict)` = HTTPS end-to-end + origin cert validation.

1. Cloudflare dashboard → `kitehub.me` zone → SSL/TLS → Overview.
2. Encryption mode: chọn **Full (strict)**.
3. SSL/TLS → Edge Certificates → bật:
   - **Always Use HTTPS:** ON (redirect HTTP → HTTPS edge level)
   - **Automatic HTTPS Rewrites:** ON
   - **Minimum TLS Version:** TLS 1.2 (PDPL + OWASP baseline)
   - **Opportunistic Encryption:** ON
   - **TLS 1.3:** ON
4. SSL/TLS → Origin Server → click "Create Certificate" → 15 năm cert cho origin (AWS ALB sẽ install qua ACM, defer nếu ALB chưa live).

### 2.5 DNS Records cho Phase 1 BETA (~15 min, deferrable nếu downstream chưa live)

Cloudflare dashboard → `kitehub.me` zone → DNS → Records → "Add record".

Per [`documents/05-guides/deploy/dns-setup-runbook.md`](../deploy/dns-setup-runbook.md) production DNS plan, các records cần thiết:

| Type | Name | Content | Proxy status | TTL | Mục đích |
|------|------|---------|--------------|-----|----------|
| A | `@` (apex) | `<AWS-ALB-IP>` HOẶC CNAME flattening sang ALB DNS name | 🟠 Proxied | Auto | API + landing fallback |
| CNAME | `app` | `cname.vercel-dns.com` | ⚪ DNS only | Auto | FE production trên Vercel |
| CNAME | `api` | `<kitehub-prod-alb-xxx.ap-southeast-1.elb.amazonaws.com>` | 🟠 Proxied | Auto | BE API endpoint |
| CNAME | `www` | `kitehub.me` | 🟠 Proxied | Auto | Redirect www → apex |
| MX | `@` | `feedback-smtp.us-east-1.amazonses.com` priority 10 | ⚪ DNS only | Auto | Resend inbound (defer §06) |
| TXT | `@` | `v=spf1 include:_spf.resend.com ~all` | ⚪ DNS only | Auto | SPF anti-spoofing |
| TXT | `_dmarc` | `v=DMARC1; p=quarantine; rua=mailto:dmarc@kitehub.me` | ⚪ DNS only | Auto | DMARC policy |
| CNAME | `resend._domainkey` | từ Resend dashboard sau khi verify domain | ⚪ DNS only | Auto | DKIM authentication |
| CNAME | `resend2._domainkey` | từ Resend dashboard | ⚪ DNS only | Auto | DKIM backup key |
| CNAME | `send._domainkey` | từ Resend dashboard | ⚪ DNS only | Auto | DKIM additional |

⚠️ **SPF/DKIM/DMARC status:** Tracked qua GAP-533 (deliverability). Records lấy chính xác từ Resend dashboard sau khi `06-resend-account-setup.md` complete.

⚠️ **Proxy status:**
- 🟠 Proxied (orange cloud): traffic qua Cloudflare CDN + DDoS + cache → dùng cho web + API.
- ⚪ DNS only (grey cloud): bypass Cloudflare → dùng cho email + SPF/DKIM/DMARC records (Cloudflare không proxy SMTP).

### 2.6 DDoS Protection — auto (~2 min verify)

Free tier mặc định include unlimited DDoS protection cho zone proxied. Verify:

1. Cloudflare dashboard → `kitehub.me` → Security → DDoS.
2. Section "HTTP DDoS Attack Protection" → Sensitivity Level = **High** (default).
3. Section "Network-layer DDoS Attack Protection" = Active (Free tier).

Không cần config thêm. Phase 1.5+ upgrade Pro để custom rules.

### 2.7 API Token least-privilege cho CI (~10 min)

⚠️ **NEVER use Cloudflare Global API Key** trong CI hoặc script. Global key = full account access + cannot scope.

1. Cloudflare dashboard → My Profile (avatar top-right) → API Tokens.
2. Click "Create Token" → chọn template "Edit zone DNS" hoặc Custom token.
3. Configuration:
   - **Token name:** `kitehub-me-dns-edit-ci`
   - **Permissions:**
     - Zone | DNS | Edit
     - Zone | Zone Settings | Read (optional, cho cert rotation tự động)
   - **Zone Resources:** Include | Specific zone | `kitehub.me`
   - **Client IP Address Filtering:** optional — restrict tới GitHub Actions egress range nếu muốn paranoid (defer Phase 1.5+)
   - **TTL:** không set expiry cho Phase 1 BETA (rotation cadence qua `secrets-rotation-runbook.md`)
4. Click "Continue to summary" → "Create Token".
5. **COPY TOKEN NGAY** — chỉ hiển thị 1 lần. Lưu vào AWS Secrets Manager:

```bash
aws secretsmanager create-secret \
  --name kitehub/production/cloudflare-api-token \
  --description "Cloudflare API token scope=Zone.DNS:Edit zone=kitehub.me for CI" \
  --secret-string "<token-value>" \
  --region ap-southeast-1
```

Hoặc lưu tạm trong password manager nếu Secrets Manager chưa setup.

### 2.8 Cloudflare Pages (defer Phase 1.5+ optional)

Cloudflare Pages = static site hosting alternative cho Vercel. Phase 1 BETA dùng Vercel làm FE production (`07-vercel-account-setup.md`). Pages defer khi:
- Vercel free tier exhausted (Pro upgrade trigger)
- Hoặc đánh giá Cloudflare ecosystem deeper integration (Workers + R2 + D1)

Quick setup khi cần:
1. Cloudflare dashboard → Workers & Pages → Create application → Pages.
2. Connect to Git → authorize GitHub → chọn `VictorAurelius/2026-Kite-Class-Platform`.
3. Build config: framework Next.js, build command `pnpm build`, output `.next`.
4. Custom domain: `app-cf.kitehub.me` (parallel với Vercel để A/B test).

---

## 3. Verify-via

| Check | Command | Expected |
|-------|---------|----------|
| Nameservers active | `dig NS kitehub.me +short` | 2 Cloudflare nameservers (aria.ns.cloudflare.com, kai.ns.cloudflare.com hoặc tương tự) |
| Zone status Active | Cloudflare dashboard → zone overview | Badge "Active" green |
| SSL/TLS Full strict | `curl -I https://kitehub.me/` | HTTP/2 200 OR 522 (origin not ready) — KHÔNG phải SSL handshake error |
| HTTPS redirect | `curl -I http://kitehub.me/` | HTTP 301 → location https://kitehub.me/ |
| TLS 1.2 minimum | `nmap --script ssl-enum-ciphers -p 443 kitehub.me` | No TLS 1.0/1.1 ciphers, TLS 1.2 + 1.3 only |
| API token works | `curl -H "Authorization: Bearer <token>" https://api.cloudflare.com/client/v4/zones/<zone-id>/dns_records` | JSON list of DNS records, no auth error |
| Apex A record | `dig A kitehub.me +short` | Cloudflare IP (104.x.x.x hoặc 172.x.x.x) khi proxied |
| API CNAME | `dig CNAME api.kitehub.me +short` | Cloudflare IP khi proxied, OR ALB hostname khi DNS-only |

---

## 4. Troubleshooting

### 4.1 Zone status stuck "Pending" >24h

**Symptom:** Nameservers updated tại registrar nhưng Cloudflare dashboard vẫn báo `Pending`.

**Debug:**
1. `dig NS kitehub.me @8.8.8.8 +short` → check Google DNS có thấy Cloudflare NS chưa.
2. Nếu chưa: registrar chưa propagate. Wait thêm (max 48h cho .me TLD).
3. Nếu Google DNS thấy nhưng Cloudflare vẫn Pending: trong dashboard click "Re-check now".
4. Verify registrar lock không enabled — một số registrar lock nameservers theo policy.

**Fix:** Contact registrar support nếu >48h. Cloudflare support cho Free tier = community forum only.

### 4.2 SSL Error 525 (handshake failed)

**Symptom:** `curl -I https://api.kitehub.me/` → `error code: 525`.

**Cause:** Cloudflare SSL/TLS mode = Full (strict) nhưng origin chưa install cert hoặc cert invalid.

**Fix:**
1. Tạm chuyển sang mode "Full" (không strict) trong khi setup origin cert.
2. Generate Cloudflare Origin Certificate (§2.4) → install vào AWS ALB qua ACM.
3. Sau khi ALB cert active → switch back sang Full (strict).

### 4.3 API token unauthorized (403)

**Symptom:** `curl -H "Authorization: Bearer <token>"` → `{"success":false,"errors":[{"code":10000,"message":"Authentication error"}]}`.

**Debug:**
1. Token bị typo? Re-generate qua dashboard, copy lại.
2. Scope sai? Verify token có Zone.DNS:Edit cho đúng zone `kitehub.me`.
3. Token revoked? Dashboard → API Tokens → check status.

**Fix:** Re-generate token với scope chính xác, update Secrets Manager.

### 4.4 Email delivery fail sau khi setup MX/SPF/DKIM

**Symptom:** Resend báo email delivered, nhưng inbox không nhận hoặc rơi vào spam.

**Debug:**
1. Verify MX record `dig MX kitehub.me +short` → expect `feedback-smtp.us-east-1.amazonses.com` (hoặc Resend equivalent).
2. Verify SPF `dig TXT kitehub.me | grep spf1` → expect `v=spf1 include:_spf.resend.com ~all`.
3. Verify DKIM Resend dashboard → Domain settings → check 3 CNAME records resolve.
4. Send test qua [mail-tester.com](https://www.mail-tester.com) → score ≥8/10 expected.

**Fix:** Re-add records nếu missing. Cross-link `documents/05-guides/operations/email-deliverability-runbook.md` cho deep debug.

### 4.5 Cloudflare cache phục vụ stale content

**Symptom:** Update FE/BE nhưng users vẫn thấy bản cũ.

**Fix:**
1. Cloudflare dashboard → Caching → Purge Cache → "Purge Everything".
2. Hoặc selective: Purge by URL cho specific paths.
3. Long-term: set Cache Rules tại Caching → Cache Rules → exclude `/api/*` paths khỏi cache.

---

## 5. Audit + cost guard

### 5.1 Cost monitoring

Free tier limits:
- DNS queries: unlimited
- DDoS protection: unlimited (HTTP + network layer)
- SSL certs: unlimited (universal cert + origin cert)
- API requests: 1200/5 min (rate limit)
- Workers: 100k requests/day (defer Phase 1.5+ usage)

Pro upgrade trigger ($20/month):
- WAF custom rules cần thiết
- Image optimization needed cho FE perf
- Mobile redirect rules
- Polish + Mirage features

### 5.2 Audit trail

Cloudflare audit log: Dashboard → Manage Account → Audit Log. Free tier 30-day retention.

Cho production: enable Audit Logs Logpush sang AWS S3 (Pro+ feature) hoặc tự pull qua API định kỳ:

```bash
curl -H "Authorization: Bearer <token>" \
  "https://api.cloudflare.com/client/v4/accounts/<account-id>/audit_logs?since=2026-05-15T00:00:00Z" \
  > cloudflare-audit-$(date +%Y-%m-%d).json
```

---

## 6. References

- [Cloudflare Docs — DNS](https://developers.cloudflare.com/dns/)
- [Cloudflare Docs — SSL/TLS](https://developers.cloudflare.com/ssl/)
- [Cloudflare API Tokens](https://developers.cloudflare.com/api/tokens/)
- [`documents/05-guides/deploy/cloudflare-setup.md`](../deploy/cloudflare-setup.md) — production DNS records + Worker config
- [`documents/05-guides/deploy/dns-setup-runbook.md`](../deploy/dns-setup-runbook.md) — apex/CNAME records detailed
- [`documents/05-guides/operations/email-deliverability-runbook.md`](../operations/email-deliverability-runbook.md) — SPF/DKIM/DMARC deep dive (GAP-533)
- [ADR-025](../../02-architecture/adr/ADR-025-aws-singapore-free-tier.md) — Phase 1 BETA AWS Singapore region

---

## 7. Log

- **2026-05-15:** Runbook created (Wave 84 Bucket C, GAP-394). Closes 1/3 missing account-prep runbooks cho Phase 1 BETA onboarding. Cross-link production setup tại `deploy/cloudflare-setup.md`. Reviewer: @nguyenvankiet (solo-dev).
