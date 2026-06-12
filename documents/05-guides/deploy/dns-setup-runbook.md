# DNS + SSL Setup Runbook — Hướng Dẫn Cài Đặt Production / Beta

**Đối tượng:** SRE / DevOps / on-call engineer cài đặt DNS lần đầu hoặc cutover sang production.
**Closes (PARTIAL):** GAP-369 — DNS runbook artifact. Domain registration + DNS records + cert provisioning là user steps, không tự động chạy được.
**Tiêu chuẩn áp dụng:** AWS Well-Architected (Reliability + Operational Excellence) · Twelve-Factor (config in env) · `.claude/rules/release-deploy-standard.md` §3.1 + §3.4 · `.claude/rules/logs-format-standard.md` (không log PII).

---

## 1. Phase 1 BETA strategy

> **Decision 2026-05-09 (GAP-458):** Release 1 dùng **`kitehub.me`** Free path qua GitHub Student Pack. Bảng dưới support cả paid `.vn` lẫn free `.me` paths.

### Free path (Recommended Release 1)

| Item | Value |
|------|-------|
| Production domain | `kitehub.me` (Student Pack 1 năm free; renew ~$10-20 hoặc switch `.vn`) |
| Beta domain | `beta.kitehub.me` hoặc apex `kitehub.me` |
| Tenant access | `tenant1.kitehub.me` (Pattern A subdomain) qua wildcard `*.kitehub.me` |
| Origin host | AWS ALB `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` |
| TTL | **300s** (5 min) cutover window; revert 3600s post-stable |
| Proxy | Cloudflare Free orange-cloud (DDoS + cache + WAF) — GAP-371 |
| SSL | ACM cert AWS + Cloudflare Full (strict) |
| Registrar | Namecheap (Student Pack offer) — `02b-github-student-pack-free-domain.md` |

### Paid path (alternative `.vn`)

| Item | Value |
|------|-------|
| Production domain | `kitehub.vn` + `kitehub.me` (cutover Phase 1.5 PAID) |
| Beta domain | `beta.kitehub.vn` + `beta.kitehub.me` |
| Origin host | AWS ALB DNS |
| TTL | **300s** trong cutover window; revert 3600s post-stable |
| Proxy | Cloudflare orange-cloud (DDoS + cache + WAF free tier) — GAP-371 |
| SSL | Let's Encrypt (certbot) hoặc ACM — automated cron renewal |
| Registrar | **Nhân Hòa** (preferred for `.vn` per VNNIC) hoặc Mắt Bão / FPT / PA Vietnam |

`.com` alternative: Namecheap / Cloudflare Registrar (cheaper renewals; instant DNS provisioning).

---

## 2. Step-by-step — cài đặt lần đầu

### 2.1 Domain registration (one-time, ~30 min)

```
[USER_INPUT_REQUIRED]
1. Mua domain `kitehub.vn` + `kitehub.me` qua Nhân Hòa hoặc registrar tương đương
2. Verify ownership: chuẩn bị giấy tờ doanh nghiệp / CMND theo yêu cầu VNNIC
3. Lock domain transfer (registrar dashboard → Lock domain)
4. Enable WHOIS privacy (nếu registrar hỗ trợ)
```

### 2.2 Cloudflare onboarding (~20 min)

1. Đăng ký account [cloudflare.com](https://cloudflare.com) (Free tier OK cho Phase 1 BETA).
2. Add Site → nhập `kitehub.vn` → chọn Free plan.
3. Cloudflare scan existing DNS records (nếu có). Verify list.
4. Cloudflare cấp 2 nameservers (vd: `aria.ns.cloudflare.com`, `bob.ns.cloudflare.com`).
5. Quay về registrar → DNS / Nameserver settings → đổi sang 2 nameserver Cloudflare.
6. Đợi NS propagation 5-30 phút. Verify: `dig NS kitehub.vn @8.8.8.8` → trả về `*.ns.cloudflare.com`.
7. Lặp lại cho `kitehub.me`.

### 2.3 DNS records — Phase 1 BETA

Cloudflare Dashboard → DNS → Records → Add. Apply for both `kitehub.vn` + `kitehub.me`.

```
[USER_INPUT_REQUIRED: VM_PUBLIC_IP]   # Oracle Cloud VM A1 free tier IP

Type     Name                  Content                                         TTL    Proxy
─────────────────────────────────────────────────────────────────────────────────────────
A        beta                  <VM_PUBLIC_IP>                                  Auto   Proxied (orange)
A        beta.api              <VM_PUBLIC_IP>                                  Auto   Proxied
A        beta.admin            <VM_PUBLIC_IP>                                  Auto   Proxied
CNAME    www.beta              beta.kitehub.vn                                 Auto   Proxied
TXT      beta                  v=spf1 include:amazonses.com -all              Auto   DNS only
TXT      _dmarc.beta           v=DMARC1; p=quarantine; rua=mailto:dmarc@kitehub.vn; ruf=mailto:dmarc@kitehub.vn; sp=quarantine; aspf=r  Auto   DNS only
MX       beta                  10 inbound-smtp.ap-southeast-1.amazonaws.com    Auto   DNS only
```

**DKIM** — added AFTER SES verifies sender (3 CNAMEs, see GAP-370 email-ses-setup-runbook):

```
CNAME    <token1>._domainkey.beta    <token1>.dkim.amazonses.com    Auto   DNS only
CNAME    <token2>._domainkey.beta    <token2>.dkim.amazonses.com    Auto   DNS only
CNAME    <token3>._domainkey.beta    <token3>.dkim.amazonses.com    Auto   DNS only
```

**AAAA (IPv6)** — only if Oracle VM has public IPv6:
```
AAAA     beta                  <VM_PUBLIC_IPv6>                                Auto   Proxied
```

### 2.4 SSL certificates (Let's Encrypt + Cloudflare Origin Cert)

> **Trạng thái Tier 2 (2026-05-10):**
> - **Vercel apex** `kitehub.me` — Let's Encrypt R13 cert auto-issued by Vercel sau apex bind 2026-05-09; valid May 10 → Aug 8 2026 (auto-renew 90-day cycle).
> - **Cloudflare Origin Cert** — generated 2026-05-10 cho ALB binding (Tier 3); files local `~/.gcal-mcp/cloudflare-origin-cert/`; validity 2026-05-10 → 2041-05-06 (15 năm).
> - **AWS ACM cert** — KHÔNG cần riêng; ACM imports Cloudflare Origin Cert ở Tier 3 step (free).
> - **ALB HTTPS listener cert binding** — pending Tier 3 cutover (per `release-1-tier-3-cutover.md` §2-3).
>
> **Trạng thái Wave 61 Bucket A state-check (2026-05-11):** verified live via `agent-aws-access.md` Tier 1 commands —
> - Cloudflare DNS records: ✅ 9 records present (CNAME `api.kitehub.me` → ALB DNS, apex → Vercel, wildcard, MX × 3, SPF, DKIM); DNS resolves globally (`getent hosts api.kitehub.me` → 13.250.213.35).
> - Cloudflare SSL mode: `full` (NOT `strict`); Always Use HTTPS: `off` — Tier 3 cutover pending.
> - AWS ACM (region ap-southeast-1): EMPTY — Origin Cert chưa import.
> - AWS ALB `kitehub-alb`: HTTP:80 listener only; HTTPS:443 missing.
> - Stack state: EC2 + RDS STOPPED (Wave 61 stop-when-idle path per `documents/03-planning/waves/wave-2026-05-12-61-stop-when-idle-cutover.md`).
>
> **Path forward:** finalize via Tier 3 cutover Steps 2 (ACM import) + 3 (ALB HTTPS listener) + 6 (SSL strict) + 7 (Always HTTPS) per `release-1-tier-3-cutover.md`. Choose Path X (CLI manual) or Path Y (`.github/workflows/tier-3-cutover.yml` workflow_dispatch + confirm "APPLY") per §0.5. Verification artifact: `documents/04-quality/audits/aws-verification/2026-05-11-wave-61-bucket-a-dns-state.md`.

Run on Oracle Cloud VM as `root` or `sudo`:

```bash
sudo bash scripts/ssl-cert-setup.sh beta.kitehub.vn admin@kitehub.vn
sudo bash scripts/ssl-cert-setup.sh beta.kitehub.me admin@kitehub.vn
```

The script (see `scripts/ssl-cert-setup.sh`):
- Installs `certbot` (apt) nếu chưa có
- Requests cert qua HTTP-01 challenge (port 80 must reach VM — temporarily disable Cloudflare proxy = grey cloud during issuance)
- Configures cron weekly renewal
- Reloads nginx post-renewal

After cert issued, re-enable Cloudflare proxy (orange cloud).

### 2.5 Verify DNS propagation

```bash
bash scripts/check-dns-propagation.sh beta.kitehub.vn
bash scripts/check-dns-propagation.sh beta.kitehub.me
```

Script checks A, AAAA, MX, SPF (TXT), DKIM (CNAME), DMARC (TXT) against multiple DNS resolvers (Google `8.8.8.8`, Cloudflare `1.1.1.1`). Exit 0 = all pass.

Manual cross-check:
```
https://www.whatsmydns.net/#A/beta.kitehub.vn
https://dnschecker.org/#A/beta.kitehub.vn
```

---

## 3. Phase 1.5 PAID cutover — `kitehub.vn` apex

Trigger: Phase 1 BETA stable ≥4 tuần, ≥5 tenants live, 0 P0 incidents.

### 3.1 Pre-cutover (T-7 days)

- [ ] Provision Phase 1.5 production VM (separate from beta) với fresh deploy
- [ ] Smoke test on staging.kitehub.vn equivalent
- [ ] Backup DNS records (export Cloudflare DNS as JSON)
- [ ] Communicate maintenance window to beta tenants (status page + email)

### 3.2 Cutover steps (T-0)

1. **Lower TTL** to 300s on apex `A` record 24h before cutover (so revert is fast).
2. Add new `A` records pointing to v1.0.0 production IP:
   ```
   A    @                <PROD_VM_IP>     300s    Proxied
   A    api              <PROD_VM_IP>     300s    Proxied
   A    admin            <PROD_VM_IP>     300s    Proxied
   CNAME www              kitehub.vn       300s    Proxied
   ```
3. **DO NOT remove `beta.*` records** — keep beta env alive 30 days for rollback safety.
4. Update SES from-address to `noreply@kitehub.vn` (apex). Verify DKIM CNAMEs added at apex.
5. Update DMARC TXT at apex (replace `beta.` references):
   ```
   TXT  _dmarc           v=DMARC1; p=quarantine; rua=mailto:dmarc@kitehub.vn  300s    DNS only
   ```
6. Run `ssl-cert-setup.sh` on production VM for apex + `www`:
   ```bash
   sudo bash scripts/ssl-cert-setup.sh kitehub.vn admin@kitehub.vn
   sudo bash scripts/ssl-cert-setup.sh www.kitehub.vn admin@kitehub.vn
   sudo bash scripts/ssl-cert-setup.sh api.kitehub.vn admin@kitehub.vn
   sudo bash scripts/ssl-cert-setup.sh admin.kitehub.vn admin@kitehub.vn
   ```
7. Run `check-dns-propagation.sh kitehub.vn` — verify all records.
8. Smoke test via `scripts/smoke-test.sh https://kitehub.vn` (see GAP-377).
9. Monitor error rate + latency for 1 hour post-cutover.

### 3.3 Post-cutover (T+24h → T+7 days)

- T+1h: revert TTL to 3600s if stable
- T+24h: announce cutover complete, retire beta if migration confirmed
- T+7 days: remove `beta.*` DNS records (keep MX 30 more days for delayed mail)

---

## 4. Email subdomain strategy

| Address | Use |
|---------|-----|
| `noreply@kitehub.vn` | Transactional (signup, password reset, invoices) |
| `support@kitehub.vn` | User-replyable support inbox |
| `dmarc@kitehub.vn` | DMARC report inbox (auto-parsed) |
| `admin@kitehub.vn` | Internal admin only (never displayed to tenants) |

Mail subdomain `mail.kitehub.vn` — **NOT used**. Sending via SES through MX of apex hoặc beta subdomain. See GAP-370 for SES configuration.

---

## 5. Kế hoạch rollback cutover

Nếu Phase 1.5 cutover fails (error rate >1%, P95 latency >2s, signup conversion drops >50%):

1. Cloudflare Dashboard → DNS → revert apex A record về beta IP (TTL 300s ⇒ propagates ≤5 min).
2. Notify users via status page (xem GAP-373).
3. Tag rollback in Sentry / Grafana.
4. Run RCA within 48h, file post-mortem.

Beta env stays live 30d post-cutover specifically for this scenario.

---

## 6. Ước tính chi phí (Cloudflare Free + Let's Encrypt)

| Item | Cost |
|------|------|
| Cloudflare Free tier | $0/mo (DDoS, cache, WAF basics) |
| Let's Encrypt cert | $0/90d (renewal automated) |
| `.vn` domain registration | ~500.000 VND/year (Nhân Hòa) |
| `.com` alternative | ~$10/year (Cloudflare Registrar) |

Phase 1.5 PAID: consider Cloudflare Pro ($25/mo per zone) for image optimization + better WAF rules.

---

## 7. Tiêu chí nghiệm thu — runbook đã "verified" khi nào

- [ ] User has performed §2.1-2.5 successfully on at least beta subdomain
- [ ] `check-dns-propagation.sh` exits 0 against `beta.kitehub.vn`
- [ ] HTTPS responds 200 from `https://beta.kitehub.vn` with valid Let's Encrypt cert
- [ ] Test email từ `noreply@beta.kitehub.vn` delivered + DMARC pass

---

## 8. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| TTL 86400 (1d) trong cutover window | TTL 300s during cutover; revert sau khi stable |
| Hardcode VM IP trong app config | DNS-resolved hostname với short TTL |
| Skip DMARC because "we don't send much email" | DMARC `p=quarantine` từ ngày 1 — phòng spoofing |
| Issue cert qua DNS-01 trong Phase 1 BETA | HTTP-01 đủ; DNS-01 thêm complexity (cần API token) |
| Remove `beta.*` records ngay sau cutover | Giữ ≥30 ngày làm rollback insurance |
| Trust 1 DNS resolver (e.g., chỉ `8.8.8.8`) | Cross-check Google + Cloudflare + Quad9 |

---

## 9. Liên quan

- `documents/05-guides/deploy/secrets-seeding-runbook.md` (sister deploy runbook) + `documents/05-guides/operations/secrets-rotation-runbook.md` (rotation cadence)
- `documents/03-planning/roadmap/release-1-deploy-plan.md` (parent)
- `documents/02-architecture/deployment-strategy.md` (5 nguyên tắc)
- `infrastructure/terraform-aws/secrets.tf` (existing AWS secrets)
- `scripts/ssl-cert-setup.sh` (cert provisioning)
- `scripts/check-dns-propagation.sh` (verification)
- `.env.production.template` (env vars consuming DNS-resolved hostnames)

---

## 10. Log

- **2026-05-07** Wave 33 Bucket D — runbook + cert script + propagation check shipped. GAP-369 stays 🟡 PARTIAL: domain registration + DNS records + cert issuance must run on Oracle Cloud VM (manual user steps documented but not automatable from CI).
