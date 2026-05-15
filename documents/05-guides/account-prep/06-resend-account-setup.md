# 06 — Resend Account Setup Runbook

**Audience:** Solo dev tạo Resend account lần đầu cho Phase 1 BETA — transactional email delivery (beta invites, email verification, password reset, system notices).
**Standards:** AWS Well-Architected (Reliability + Cost Optimization) · ADR-025 Stream A (Resend HTTP API path) · `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2.
**Cross-link upstream:** Yêu cầu `05-cloudflare-account-setup.md` complete (cần DNS records cho domain verify) + domain `kitehub.me` đã active.
**Cross-link downstream:** Blocks `documents/05-guides/deploy/resend-provisioning-runbook.md` (production seeding + secret rotation) + `documents/05-guides/operations/email-deliverability-runbook.md` (SPF/DKIM/DMARC tuning per GAP-533) + `documents/05-guides/operations/secrets-rotation-runbook.md` (quarterly API key rotation).
**Estimated time:** ~45 min (chưa kể đợi DKIM propagation 5 min – 24h).
**Last-Updated:** 2026-05-15

---

## TL;DR

> Tạo tài khoản Resend Free → add domain `kitehub.me` → copy 3 DKIM CNAME records về Cloudflare DNS → verify domain → tạo API key full-access → lưu vào AWS Secrets Manager `kitehub/production/resend-api-key` → test send qua API → kiểm tra Inbox tab.

Quick path 6 bước cho Phase 1 BETA:

1. Signup tại [resend.com/signup](https://resend.com/signup) (Free plan 100 emails/day, 3000/month)
2. Domains → Add Domain → nhập `kitehub.me`
3. Copy 3 CNAME records (DKIM) → paste vào Cloudflare DNS (§2.5 trong `05-cloudflare-account-setup.md`)
4. Quay lại Resend → click "Verify DNS Records" → wait DKIM status `Verified`
5. API Keys → Create → scope `Full access` → COPY ngay → lưu Secrets Manager `kitehub/production/resend-api-key`
6. Test send: `curl -X POST https://api.resend.com/emails -H "Authorization: Bearer $RESEND_API_KEY" -d '{...}'` → check Inbox tab

---

## 1. Trước khi bắt đầu — chuẩn bị

| Item | Yêu cầu |
|------|---------|
| Email | Email cá nhân để signup Resend (Resend KHÔNG cần email theo domain tại signup). Recommend cùng email với `cloudflare-admin@kitehub.me` sau khi MX active. |
| Domain `kitehub.me` | Đã active tại Cloudflare (`05-cloudflare-account-setup.md` xong) — cần update DNS records. |
| Cloudflare API access | Login Cloudflare dashboard ready, hoặc API token scope `Zone.DNS:Edit` (từ §2.7 trong `05-cloudflare-account-setup.md`). |
| AWS Secrets Manager | Setup từ `01-aws-account-creation.md` — sẽ lưu Resend API key tại `kitehub/production/resend-api-key`. |
| Test email inbox | Email cá nhân (Gmail/Outlook/Yahoo) để nhận test email + check Spam folder. |
| Password manager | Vault active (`03-password-manager.md`) để lưu Resend credentials + API key backup. |

⚠️ **Critical:** Resend API key = production credential = full email send access. Leak = spam/phishing risk + reputation damage. Lưu Secrets Manager NGAY, KHÔNG commit vào git, KHÔNG paste vào Slack/Discord.

---

## 2. Step-by-step

### 2.1 Signup Resend Free (~5 min)

1. Mở [resend.com/signup](https://resend.com/signup).
2. Sign up với:
   - Email + Password (recommend dùng password manager generate ≥16 char)
   - HOẶC GitHub OAuth (quicker, no password to manage; recommend cho solo-dev)
3. Verify email qua link Resend gửi.
4. Login → onboarding screen.
5. Plan: **Free** (100 emails/day, 3000/month, 1 domain). KHÔNG cần thẻ credit cho Free tier.

Free tier coverage Phase 1 BETA:
- ≤5 tenants × ≤20 emails/day = 100/day = đủ
- Beta invites + email verification + password reset = primary use cases

Pro upgrade trigger ($20/month, 50k emails/month):
- ≥6 tenants active hoặc invite blast > 100/day
- Cần custom domain reply-to multiple
- Webhooks volume > Free tier limit

### 2.2 Add domain `kitehub.me` (~5 min)

1. Resend dashboard → Domains (sidebar) → "Add Domain".
2. Domain: `kitehub.me` (apex, KHÔNG subdomain trong Phase 1 BETA — subdomain như `mail.kitehub.me` defer Phase 1.5+ khi tách reputation).
3. Region: chọn **US East (N. Virginia)** (default, sticky) — closest to AWS SES backbone qua Resend infra. Defer `eu-west-1` upgrade Phase 2 nếu cần EU compliance.
4. Click "Add" → Resend hiển thị 3 DKIM CNAME records cần add vào DNS:

```
Type    Host                        Value
CNAME   resend._domainkey           resend.<unique-id>.amazonses.com
CNAME   resend2._domainkey          resend2.<unique-id>.amazonses.com
CNAME   send._domainkey             send.<unique-id>.amazonses.com
```

⚠️ **COPY 3 records này** — sẽ paste vào Cloudflare §2.3.

### 2.3 Add DKIM records vào Cloudflare DNS (~5 min)

1. Cloudflare dashboard → `kitehub.me` zone → DNS → Records → "Add record".
2. Thêm record 1:
   - Type: **CNAME**
   - Name: `resend._domainkey`
   - Target: `resend.<unique-id>.amazonses.com` (paste từ §2.2)
   - Proxy status: **DNS only** (⚪ grey cloud — Cloudflare KHÔNG proxy DKIM records)
   - TTL: Auto
   - Save.
3. Lặp lại cho record 2: `resend2._domainkey` + record 3: `send._domainkey`.
4. Verify: `dig CNAME resend._domainkey.kitehub.me +short` → expect Amazon SES hostname.

### 2.4 Verify domain tại Resend (~5 min — 24h DKIM propagation)

1. Resend dashboard → Domains → `kitehub.me` → "Verify DNS Records".
2. Status hiển thị 3 record:
   - 🟡 `Pending` = đang chờ DNS propagation
   - 🟢 `Verified` = DKIM active
3. Wait 5 min – 24h cho propagation. Re-check qua refresh page hoặc "Verify" button.
4. Sau khi status `Verified` cho cả 3 records → domain ready for send.

⚠️ **Nếu sau 24h vẫn `Pending`:** check `dig CNAME resend._domainkey.kitehub.me +short` từ public DNS. Nếu empty → record chưa save tại Cloudflare. Nếu trả về wrong value → typo lúc paste.

### 2.5 Add SPF + DMARC records (recommended, Phase 1 BETA)

⚠️ **SPF + DMARC = deliverability boost** — cải thiện inbox placement, giảm spam-folder risk. Tracked qua GAP-533 (deliverability).

Cloudflare dashboard → `kitehub.me` zone → DNS → Records:

#### SPF record

| Type | Name | Content | Proxy | TTL |
|------|------|---------|-------|-----|
| TXT | `@` | `v=spf1 include:_spf.resend.com ~all` | DNS only | Auto |

⚠️ Nếu đã có SPF record cũ (vd Gmail/Google Workspace), **MERGE thành 1 record duy nhất**:
```
v=spf1 include:_spf.google.com include:_spf.resend.com ~all
```
KHÔNG duplicate 2 SPF records — spec RFC 7208 = 1 record per domain.

#### DMARC record

| Type | Name | Content | Proxy | TTL |
|------|------|---------|-------|-----|
| TXT | `_dmarc` | `v=DMARC1; p=quarantine; rua=mailto:dmarc@kitehub.me; ruf=mailto:dmarc@kitehub.me; pct=100; adkim=s; aspf=s` | DNS only | Auto |

Policy progression Phase 1 BETA → Production:
- Phase 1 BETA: `p=quarantine` (suspicious emails → spam folder)
- Phase 1.5+: `p=reject` (suspicious emails → bounce, strictest)

Cần inbox `dmarc@kitehub.me` để nhận aggregate reports (set up qua Cloudflare Email Routing nếu chưa có).

### 2.6 Tạo API key full-access (~5 min)

1. Resend dashboard → API Keys → "Create API Key".
2. Configuration:
   - **Name:** `kitehub-production-full-access`
   - **Permission:** **Full access** (Send + Read; restricted scope `Sending access` defer Phase 1.5+ nếu cần)
   - **Domain:** All Domains (sẽ scope cho `kitehub.me` only — Free tier có 1 domain duy nhất)
3. Click "Create" → API key hiển thị **CHỈ 1 LẦN**.
4. **COPY NGAY** — format `re_<random-string-32-char>`.
5. Lưu vào AWS Secrets Manager:

```bash
aws secretsmanager create-secret \
  --name kitehub/production/resend-api-key \
  --description "Resend API key full-access for kitehub.me transactional email" \
  --secret-string "<token-value>" \
  --region ap-southeast-1
```

Hoặc nếu Secrets Manager chưa setup → lưu tạm password manager + plan migrate.

6. Verify storage:
```bash
aws secretsmanager get-secret-value \
  --secret-id kitehub/production/resend-api-key \
  --region ap-southeast-1 \
  --query SecretString --output text | head -c 10
# Expected: "re_..." (first 10 chars)
```

### 2.7 Test send qua API (~5 min)

1. Export key tạm thời (terminal):
```bash
export RESEND_API_KEY=$(aws secretsmanager get-secret-value \
  --secret-id kitehub/production/resend-api-key \
  --region ap-southeast-1 \
  --query SecretString --output text)
```

2. Send test email:
```bash
curl -X POST 'https://api.resend.com/emails' \
  -H "Authorization: Bearer $RESEND_API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{
    "from": "test@kitehub.me",
    "to": ["your-personal-email@gmail.com"],
    "subject": "Hello from KiteHub Phase 1 BETA",
    "html": "<p>Test email — Phase 1 BETA setup verification</p>"
  }'
```

3. Response expected:
```json
{"id": "<email-uuid>"}
```

4. Check inbox cá nhân (đợi 1-2 min):
   - ✅ Email arrives in Inbox → setup successful
   - ⚠️ Email in Spam folder → SPF/DKIM/DMARC chưa active hoặc cần warm-up. Check `email-deliverability-runbook.md` (GAP-533).
   - ❌ Email không arrives → check Resend dashboard → Emails tab → status row cho test email.

### 2.8 Verify trong Inbox tab Resend (~2 min)

1. Resend dashboard → Emails (sidebar).
2. Tìm test email vừa send:
   - Status: `delivered` = sent successfully + recipient SMTP accepted
   - Status: `bounced` = recipient rejected (typo / disabled inbox)
   - Status: `complained` = recipient marked spam (rare cho test)
3. Click row → detail panel hiển thị:
   - Headers (From / To / Subject / Date)
   - DKIM signature
   - SPF check result
   - DMARC alignment

### 2.9 Sending limits + Pro upgrade trigger

Free tier limits Phase 1 BETA:
- 100 emails / day
- 3000 emails / month
- 1 verified domain
- 1 API key
- Webhooks limited

Resend dashboard → Usage → check daily/monthly consumption.

Pro upgrade trigger ($20/month):
- Daily > 80/day average 3 ngày liên tiếp (proactive)
- Monthly > 2500 (80% threshold)
- Cần 2+ domains (vd `kitehub.me` + `kiteclass.me`)
- Cần webhook reliability cho event tracking

Cost-effective alternative: switch sang AWS SES nếu volume > 10k/month (SES = $0.10/1k = $1/10k vs Resend Pro $20 fixed). Defer SES migration Phase 2 per ADR-025 Stream A pivot.

---

## 3. Verify-via

| Check | Command | Expected |
|-------|---------|----------|
| Domain verified | Resend dashboard → Domains → `kitehub.me` | 3/3 DKIM records `Verified` (green) |
| DKIM resolves | `dig CNAME resend._domainkey.kitehub.me +short` | Amazon SES hostname |
| SPF active | `dig TXT kitehub.me +short \| grep spf1` | `v=spf1 include:_spf.resend.com ~all` |
| DMARC active | `dig TXT _dmarc.kitehub.me +short` | `v=DMARC1; p=quarantine; ...` |
| API key works | `curl -H "Authorization: Bearer $RESEND_API_KEY" https://api.resend.com/domains` | JSON list of domains, no auth error |
| Test email delivered | Resend dashboard → Emails | Status `delivered`, DKIM ✓, SPF ✓, DMARC ✓ |
| Inbox placement | Personal Gmail/Outlook inbox | Email arrives in Inbox (NOT spam folder) |
| Secret stored | `aws secretsmanager get-secret-value --secret-id kitehub/production/resend-api-key --region ap-southeast-1 --query SecretString --output text \| wc -c` | ~35 chars (re_ + 32 random) |

---

## 4. Troubleshooting

### 4.1 DKIM verification stuck "Pending" >24h

**Symptom:** Resend dashboard hiển thị 3 records `Pending` sau 24h.

**Debug:**
1. `dig CNAME resend._domainkey.kitehub.me @8.8.8.8 +short` → check Google DNS thấy không.
2. Nếu empty: Cloudflare record chưa save → re-check spelling tại Cloudflare DNS.
3. Nếu trả wrong target: paste sai. Copy LẠI từ Resend dashboard (chú ý unique-id phần).
4. Proxy status: phải là **DNS only** (grey cloud) — proxied (orange) sẽ break DKIM lookup.

**Fix:** Re-add records với spelling chính xác + DNS only proxy. Trigger "Verify DNS Records" tại Resend.

### 4.2 Test email vào Spam folder

**Symptom:** Email delivered theo Resend, nhưng inbox cá nhân thấy trong Spam.

**Debug:**
1. Test qua [mail-tester.com](https://www.mail-tester.com) — send từ Resend đến địa chỉ unique mail-tester provide → score ≥8/10 expected.
2. Check SPF/DKIM/DMARC active (§3 verify table).
3. Domain warm-up: domain mới = low reputation. Spam folder placement first 1-2 weeks là normal khi volume thấp.

**Fix:**
1. Add SPF + DMARC records (§2.5) nếu chưa.
2. Warm-up: gửi 10-20 emails/day đến variety inboxes (Gmail/Outlook/Yahoo) trong 1-2 weeks.
3. Avoid spam triggers: subject không có `!!!`, body không có "FREE!!! BUY NOW", From address khớp domain DKIM signed.
4. Long-term: monitor `email-deliverability-runbook.md` (GAP-533) cho deep tuning.

### 4.3 API key 401 unauthorized

**Symptom:** `curl -H "Authorization: Bearer ..."` → `{"name":"missing_api_key","message":"API key is missing"}` hoặc `{"name":"invalid_api_key"}`.

**Debug:**
1. Key bị typo? Re-read từ Secrets Manager → compare format `re_<32-char>`.
2. Key bị revoke? Resend dashboard → API Keys → status check.
3. Header format đúng `Authorization: Bearer <key>` (KHÔNG `Token <key>` hoặc `<key>` plain).

**Fix:** Re-generate key qua dashboard, update Secrets Manager NGAY. Old key auto-invalidated sau revoke.

### 4.4 From address bị reject "Domain not verified"

**Symptom:** Send qua API → `{"name":"validation_error","message":"From email address must be from a verified domain"}`.

**Cause:** Domain chưa pass DKIM verification, hoặc From address typo (vd `@kitehub.com` thay vì `@kitehub.me`).

**Fix:**
1. Verify domain status §2.4.
2. From address: must match verified domain exact (`<anything>@kitehub.me`).

### 4.5 Free tier rate limit hit

**Symptom:** `{"name":"rate_limit_exceeded","message":"You have exceeded the rate limit. Please try again later."}`.

**Cause:** > 100 emails/day Free tier limit.

**Fix:**
1. Short-term: throttle send loop trong code (rate limit client-side).
2. Long-term: upgrade Pro ($20/month, 50k emails/month).

---

## 5. Audit + cost guard

### 5.1 Cost monitoring

Resend dashboard → Usage tab → biểu đồ daily/monthly volume. Snapshot weekly trong quality audit cadence (`output-review-mandate.md` §3 row "Cost Optimization").

### 5.2 Audit trail

Resend retains email events 30 days Free tier (Pro: 90 days). Cho long-term audit:

```bash
# Export emails last 7 days qua API
curl -H "Authorization: Bearer $RESEND_API_KEY" \
  "https://api.resend.com/emails?limit=100&before=$(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%SZ)" \
  > resend-emails-$(date +%Y-%m-%d).json
```

Lưu vào AWS S3 cho long-term retention (PDPL 90-day mandate).

### 5.3 Key rotation cadence

Per `secrets-rotation-runbook.md` quarterly cadence:
- Q1 review API key creation date
- Rotate nếu > 90 ngày
- Test new key trước revoke old key (dual-active 24h window)

---

## 6. References

- [Resend Docs — Getting Started](https://resend.com/docs/introduction)
- [Resend API Reference](https://resend.com/docs/api-reference)
- [Resend Domains + DKIM](https://resend.com/docs/dashboard/domains/introduction)
- [`documents/05-guides/deploy/resend-provisioning-runbook.md`](../deploy/resend-provisioning-runbook.md) — production seeding + secret rotation
- [`documents/05-guides/operations/email-deliverability-runbook.md`](../operations/email-deliverability-runbook.md) — SPF/DKIM/DMARC tuning (GAP-533)
- [`documents/05-guides/operations/secrets-rotation-runbook.md`](../operations/secrets-rotation-runbook.md) — quarterly rotation cadence
- [ADR-025](../../02-architecture/adr/ADR-025-aws-singapore-free-tier.md) — Stream A Resend HTTP API pivot
- [Mail Tester](https://www.mail-tester.com) — deliverability score tool

---

## 7. Log

- **2026-05-15:** Runbook created (Wave 84 Bucket C, GAP-394). Closes 2/3 missing account-prep runbooks cho Phase 1 BETA onboarding. Cross-link production setup tại `deploy/resend-provisioning-runbook.md`. Reviewer: @nguyenvankiet (solo-dev).
