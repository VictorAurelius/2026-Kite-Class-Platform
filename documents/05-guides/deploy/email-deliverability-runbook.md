# Email Deliverability Runbook — DNS + Warm-up + Spam-score baseline

**Status:** active (Wave 77 Bucket A — code-side foundation, apply + warm-up = user-action follow-on)
**Created:** 2026-05-14
**Last Updated:** 2026-05-14
**Related:** GAP-533 (deliverability warm-up), GAP-370 (email infra), GAP-530 (end-to-end live verify), [`resend-provisioning-runbook.md`](./resend-provisioning-runbook.md), [`email-ses-setup-runbook.md`](./email-ses-setup-runbook.md), [`cloudflare-setup.md`](./cloudflare-setup.md), [`infrastructure/terraform-cloudflare/`](../../../infrastructure/terraform-cloudflare/), [`scripts/verify-email-deliverability.sh`](../../../scripts/verify-email-deliverability.sh), [`scripts/smoke-resend.sh`](../../../scripts/smoke-resend.sh)
**Audience:** Solo dev / operator chuẩn bị Phase 1 BETA invite cohort 5-10 tenants
**Phase:** phase-1-beta

---

## 1. Mục đích + khi nào dùng

Runbook này hướng dẫn deliverability — khả năng email Phase 1 BETA invite thực sự rơi đúng inbox Gmail/Outlook VN thay vì spam folder. **KHÔNG trùng** với `resend-provisioning-runbook.md` (provisioning = setup account + secret + first-record DNS) hoặc `email-ses-setup-runbook.md` (SES sandbox/production approval). Runbook này ghép phía trên hai cái đó với 4 cấu phần:

1. **DNS records** — SPF + DKIM + DMARC active trong Cloudflare (terraform-managed per `infrastructure/terraform-cloudflare/`)
2. **Warm-up schedule** — gradual ramp 7 ngày để build sender reputation trên domain mới
3. **Spam-score baseline** — mail-tester.com auto-check để gate ≥8/10 trước khi mở rộng cohort
4. **Provider dashboard monitoring** — Resend (hoặc SES) metrics bounce/complaint/delivered

### Khi nào chạy

- **Lần đầu setup** cho domain mới (`kitehub.me`) — Phase 1 BETA pre-invite
- **Sau quarterly rotation** nếu thay đổi DKIM selector (per `secrets-rotation-runbook.md`)
- **Sau incident** — bounce rate >2% hoặc complaint >0.1% trên dashboard
- **Trước khi mở rộng** cohort từ 5-10 → 20+ tenants

### Tiền điều kiện (pre-flight)

| Item | Verify cách nào | Pass criterion |
|------|----------------|---------------|
| Resend account đã có domain `kitehub.me` | Resend dashboard → Domains | Status pending OR verified |
| Resend dashboard hiển thị 5 DNS record values thật | Resend dashboard → kitehub.me → DNS Records | 5 rows hiển thị (1 SPF + 3 DKIM + 1 DMARC) |
| Cloudflare zone `kitehub.me` đã import + có API token Zone:Read+DNS:Edit | Cloudflare dashboard + token UI | Zone active; token rotated trong 90 ngày qua |
| `RESEND_API_KEY` đã trong AWS Secrets Manager + Phase 2 wiring done | `aws secretsmanager get-secret-value --secret-id <id>` (Tier 1 read) | Secret tồn tại + non-empty value |
| Inbox test sẵn sàng | Gmail + Outlook + Yahoo VN test accounts | 3 inbox truy cập được trong session |

---

## 2. DNS setup — terraform-managed Cloudflare records

Wave 77 Bucket A đã codify records trong [`infrastructure/terraform-cloudflare/dns.tf`](../../../infrastructure/terraform-cloudflare/dns.tf). Trước Bucket A: records quản lý thủ công dashboard (per `cloudflare-setup.md`).

### 2.1 Sequence áp dụng

1. **Lấy 3 DKIM CNAME values từ Resend dashboard:**
   - Resend dashboard → Domains → kitehub.me → DNS Records
   - Copy 3 dòng CNAME `resend._domainkey`, `resend2._domainkey`, `resend3._domainkey` → cột "Value" (định dạng `<token>.resend._domainkey.<account>.resend.com`)
   - Copy 1 dòng TXT `_dmarc` (Resend đề xuất `v=DMARC1; p=none; rua=mailto:...`) — runbook này dùng quarantine + rua riêng theo `variables.tf` defaults

2. **Cập nhật `terraform.tfvars` (gitignored):**
   ```bash
   cd infrastructure/terraform-cloudflare
   cp terraform.tfvars.example terraform.tfvars
   $EDITOR terraform.tfvars  # thay placeholder DKIM CNAME + Cloudflare zone_id + api_token
   ```

3. **Lấy Cloudflare zone_id (one-time):**
   ```bash
   CF_TOKEN="<paste-cloudflare-api-token>"
   curl -s -H "Authorization: Bearer $CF_TOKEN" \
     "https://api.cloudflare.com/client/v4/zones?name=kitehub.me" \
     | jq -r '.result[0].id'
   ```

4. **Pre-apply audit artifact** per `pre-mutation-state-check.md` §3:
   - Path: `documents/04-quality/audits/cloudflare-verification/YYYY-MM-DD-bucket-a-email-dns.md`
   - Sections: Scope + Commands + Findings + Prior actions (CF dashboard records hiện có) + Recommendation
   - Document GET hiện trạng DNS qua `curl -H "Authorization: Bearer $CF_TOKEN" https://api.cloudflare.com/client/v4/zones/<zone_id>/dns_records` trước khi POST/PATCH

5. **Apply (operator-triggered):**
   ```bash
   cd infrastructure/terraform-cloudflare
   terraform init
   terraform plan -out=tfplan
   # Review 5 records: 1 add SPF + 3 add DKIM CNAME + 1 add DMARC (nếu chưa có) hoặc update nếu drift
   terraform apply tfplan
   ```

6. **Concurrent ops check** per `concurrent-production-mutation-ops.md` §3:
   - Đảm bảo KHÔNG có DNS PATCH/DELETE manual trong cùng phút
   - Đảm bảo KHÔNG trigger `terraform-apply.yml` trên AWS song song (zones tách biệt nhưng dashboard mismatch nguy hiểm)

### 2.2 DNS verify post-apply

```bash
# Verify SPF
dig +short TXT kitehub.me | grep "v=spf1"
# Expect: "v=spf1 include:_spf.resend.com ~all"

# Verify DKIM CNAMEs (3 selectors)
for sel in resend resend2 resend3; do
  echo "=== $sel ==="
  dig +short CNAME "$sel._domainkey.kitehub.me"
done
# Expect: 3 CNAME targets ending .resend.com

# Verify DMARC
dig +short TXT _dmarc.kitehub.me
# Expect: "v=DMARC1; p=quarantine; pct=100; rua=mailto:dmarc-reports@kitehub.me; ..."
```

DNS propagation toàn cầu thường 5-30 phút; có thể tới 24h. Trước khi chạy warm-up §3, đảm bảo cả 5 records resolved.

### 2.3 Resend dashboard confirm Verified

- Dashboard refresh mỗi 5-30 phút
- Mục tiêu: badge **Verified** xanh trên cả 5 records
- Nếu 1 hoặc nhiều records `Pending` >2h sau khi DNS đã propagate → check Cloudflare proxy status (CNAME phải DNS-only, KHÔNG proxied — xem `resend-provisioning-runbook.md` §2.3)

---

## 3. Warm-up schedule — 7 ngày ramp

Domain mới = sender reputation = 0. Gửi 100 emails ngày đầu → Gmail/Outlook flag spam ngay lập tức. Warm-up = gửi gradient để build reputation.

### 3.1 Schedule

| Ngày | Volume/ngày | Recipients | Goal |
|------|-------------|------------|------|
| Day 1 | 5 emails | 1× Gmail + 1× Outlook + 1× Yahoo + 2× self-loop kitehub.me admin | Verify DKIM signature pass; check spam folder, mark "Not Spam" nếu cần |
| Day 2 | 5 emails | Same recipients + 1-2 trusted devs | Confirm delivery rate >95% trên Resend dashboard |
| Day 3 | 10 emails | Day 1-2 recipients + 5 trusted contacts | Open rate >50% trong 6h |
| Day 4 | 10 emails | Same as Day 3 + 1-2 new contacts | Bounce rate <0.5% |
| Day 5 | 20 emails | Day 3-4 + cohort prep list (5 invite candidates) | Spam complaint rate = 0% |
| Day 6 | 20 emails | Same + Resend "Send test" tool spam-score | mail-tester.com score >=8/10 |
| Day 7 | 20 emails | Final pre-invite verification batch | All KPIs green |
| Day 8+ | Scale to invite cohort | 5-10 beta tenants per Phase 1 plan | -- |

Tổng ~90 emails trong 7 ngày — well within Resend free tier 100/ngày limit.

### 3.2 Content variety

Warm-up emails KHÔNG được giống hệt nhau. Resend/Gmail anti-abuse heuristic flag domain new + identical content. Mix:

- 30% transactional shape: "Test invite email - subject contains tenant name"
- 30% verification shape: "Email verification - 6-digit code"
- 20% notification shape: "Welcome message - HTML template"
- 20% mixed: password reset, support ticket, etc.

Tận dụng existing templates Wave 33: `beta-invite.html`, `beta-request-confirmation.html`, `password-reset.html`, etc.

### 3.3 Daily checks

Mỗi ngày cuối session:

```bash
# Resend dashboard (no CLI; visual check):
#   - Domains -> kitehub.me -> Stats: delivered count, bounce rate, complaint rate
# Bounce rate <0.5% : PASS
# Complaint rate 0% : PASS (Phase 1 BETA scale 5-20 emails/day; 1 complaint = >5% = FAIL)
# Spam folder check on each test inbox - mark "Not Spam" if any landed there

# Optional CLI snapshot (Resend API):
curl -s -H "Authorization: Bearer $RESEND_API_KEY" \
  https://api.resend.com/domains \
  | jq '.data[] | {name, status, region, records: [.records[] | {record, status}]}'
```

Document daily snapshot trong session log hoặc audit artifact.

---

## 4. Spam-score gate — mail-tester.com

mail-tester.com cấp temporary inbox + score (0-10) dựa SPF/DKIM/DMARC/content/blacklist.

### 4.1 Procedure (manual; smoke script tự động ở §4.2)

1. Mở https://www.mail-tester.com/ (no signup needed)
2. Page cấp địa chỉ unique `test-XXXX@srv1.mail-tester.com`
3. Gửi 1 email từ production Resend (qua `kitehub-email` service) tới address đó. Content = production-realistic (beta invite template, links, footer)
4. Sau 30 giây click "Then check your score"
5. Score ≥8/10 = PASS. <8 = investigate breakdown (SPF/DKIM/DMARC/content/blacklist)

### 4.2 Procedure (automated — `verify-email-deliverability.sh`)

Smoke script tự động (lands Wave 77 Bucket A.4 nếu mail-tester API key sẵn):

```bash
bash scripts/verify-email-deliverability.sh
# Exit 0 if score >=8/10
# Exit 1 otherwise + breakdown ra stdout
```

Lưu ý: mail-tester.com free tier có rate limit. Pre-flight: lấy API key trước (https://www.mail-tester.com/api), cấu hình `MAIL_TESTER_API_KEY` env var. Nếu không có API key → script fallback hướng dẫn manual.

### 4.3 Gate criteria

- Mỗi ngày warm-up Day 5+: chạy 1 lần
- 3 consecutive runs score ≥8/10 → cho phép mở rộng cohort
- 1 run score <8 → STOP, fix breakdown, retry sau khi propagate (DNS hoặc content change)

Common breakdown causes + fix:

| Breakdown | Fix |
|-----------|-----|
| SPF fail | Check `dig +short TXT kitehub.me` — verify include:_spf.resend.com active |
| DKIM not signed | Check Resend dashboard Verified status; check `dig +short CNAME resend._domainkey.kitehub.me` |
| DMARC not aligned | Verify `rua=` địa chỉ thuộc cùng domain; check From: header domain match |
| Content flagged | Tránh ALL-CAPS subject, excessive links (>5), `Free!!!` style spam triggers |
| Blacklist | Check https://mxtoolbox.com/blacklists.aspx?domain=kitehub.me — Phase 1 BETA fresh domain ít khả năng, monitor |

---

## 5. Provider dashboard monitoring

### 5.1 Resend dashboard KPIs

Daily during warm-up + weekly after invite launch:

| KPI | Threshold (Phase 1 BETA) | Action if exceeded |
|-----|--------------------------|---------------------|
| Bounce rate | <0.5% (1 in 200) | Investigate invalid addresses; sanitize recipient list |
| Complaint rate | <0.1% (1 in 1000) | URGENT — 1 complaint at Phase 1 scale = pattern; review content/list-source |
| Delivered % | >97% | Investigate delays, queue backup |
| Open rate (transactional) | >40% | Subject line review; provider rep issue if low without content cause |

### 5.2 Resend webhook (deferred — Wave 78+)

Production-grade flow phải subscribe webhook `email.bounced` + `email.complained` → SNS/SQS → suppression-list update. Hiện tại (Wave 77 Bucket A scope) dùng Resend auto-suppression server-side (đủ cho Phase 1 BETA 5-20 emails/day). Webhook ingestion track GAP follow-up.

---

## 6. SES sandbox path (alternative — chỉ khi Resend bất khả)

Nếu Resend không khả dụng (region issue / cost concern Phase 2+ scale), pivot SES Sandbox C1 (per-recipient verify):

1. Set `email_provider = "ses"` trong `infrastructure/terraform-cloudflare/terraform.tfvars`
2. SES domain identity verify per `email-ses-setup-runbook.md` §3 (đã shipped Wave 33)
3. Sandbox C1: trong AWS SES Console "Identities" → add từng recipient email beta cohort 5-10 → mỗi recipient nhận verification link → click verify
4. Gửi qua SES SMTP / API; mỗi recipient pre-verified = sandbox cho phép
5. Spam-score §4 + warm-up §3 vẫn áp dụng (DNS records adapt — SPF `include:amazonses.com`, DKIM 3 CNAMEs từ SES Console)

Sandbox C1 limit: 200/day account, 1/sec rate; PASS cho Phase 1 BETA 5-10 cohort. Mở rộng >10 cần production access approval (đã DENIED Wave 69 CaseId 177857212400418; xem GAP-370 §Log; pivot Resend khuyến nghị).

---

## 7. Troubleshooting matrix

| Symptom | Diagnostic | Fix |
|---------|------------|-----|
| Email không tới inbox sau 5 phút | Resend dashboard → Logs → check delivery state | Nếu "delivered" but inbox không có: check spam folder + mark Not Spam |
| Resend status "Bounced" | Dashboard → recipient row | Verify email address valid; nếu disposable address → remove khỏi list |
| Resend status "Complained" | Dashboard | URGENT — kiểm tra ngay content + permission to send (Phase 1 BETA tenants explicitly opted in via beta-request form) |
| DKIM resolve nhưng signature fail | `dig +short CNAME resend._domainkey.kitehub.me` rồi `nslookup` target | Cloudflare proxy phải DNS-only (grey cloud), KHÔNG orange cloud — fix in dashboard |
| Spam score 5-7 | mail-tester breakdown | Most common: SPF lookup chain exceed 10 (Resend default fine; nếu mix với SES include sẽ fail). Don't mix providers in SPF. |
| Spam score 0-4 | mail-tester breakdown | Blacklist hit hoặc DMARC alignment fail; verify From: header domain = kitehub.me, không phải resend.com |
| mail-tester.com API rate limit | Free tier exhausted | Wait 24h hoặc upgrade API plan |

---

## 8. Acceptance criteria (Wave 77 Bucket A — code-side DONE)

- [x] Terraform files codify SPF + DKIM CNAME × 3 + DMARC records (`infrastructure/terraform-cloudflare/dns.tf`)
- [x] Runbook deliverability ships tại `documents/05-guides/deploy/email-deliverability-runbook.md` per `docs-folder-structure.md`
- [x] Smoke script `scripts/verify-email-deliverability.sh` runnable (manual fallback nếu mail-tester API key absent)
- [x] Smoke script `scripts/smoke-resend.sh` runnable (verify API key + send 1 test email)
- [x] GAP-370 + GAP-533 + GAP-530 files updated với code-side status; user-action follow-on documented

### Acceptance criteria — user-action follow-on (post-merge)

- [ ] Resend dashboard add domain `kitehub.me` + capture 3 DKIM CNAME thật
- [ ] `terraform.tfvars` cập nhật + `terraform apply` chạy thành công (per `pre-mutation-state-check.md` §3 audit artifact)
- [ ] DNS verify post-apply: 5 records resolve qua `dig` (§2.2)
- [ ] Resend dashboard status `Verified` cho cả 5 records
- [ ] Warm-up Day 1 executed (~5 emails) + log session
- [ ] Spam-score smoke ≥8/10 trên 3 consecutive runs Day 5-7
- [ ] Bounce rate <0.5% + complaint 0% trong toàn warm-up week

---

## 9. Liên quan

- ADR-018 Cloudflare DNS primary
- ADR-025 Stream A Resend pivot
- GAP-370 Email Transactional Infrastructure
- GAP-533 Resend Deliverability Warm-up (this runbook satisfies AC code-side)
- GAP-530 Email Flow End-to-End Verify (downstream — runs sau khi deliverability green)
- `documents/05-guides/deploy/resend-provisioning-runbook.md` — Resend account + secret setup
- `documents/05-guides/deploy/email-ses-setup-runbook.md` — SES sandbox/production state
- `documents/05-guides/deploy/cloudflare-setup.md` — historical manual DNS setup (pre-Wave 77)
- `documents/05-guides/operations/secrets-rotation-runbook.md` — RESEND_API_KEY rotation quarterly
- `infrastructure/terraform-cloudflare/` — terraform-managed records (this PR)
- `.claude/rules/pre-mutation-state-check.md` §3 — pre-apply audit mandatory
- `.claude/rules/concurrent-production-mutation-ops.md` §3 — serialize DNS PATCH/DELETE
- `.claude/rules/pre-handoff-self-test-completeness.md` §2.3 — email-driven flow checklist
- `scripts/verify-email-deliverability.sh` — automated spam-score gate
- `scripts/smoke-resend.sh` — Resend API runtime health check
- `scripts/smoke-ses.sh` — SES read-only state check (existing — Wave 61)
