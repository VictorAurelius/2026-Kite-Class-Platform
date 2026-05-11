---
title: AWS Verification — Wave 61 Bucket A DNS bind + SSL state-check
status: complete
created: 2026-05-11
phase: wave-61
wave: 61
gaps: [GAP-369, GAP-449]
---

# AWS Verification — Wave 61 Bucket A DNS bind + SSL state-check

## Scope

State-check trước khi propose fix cho Wave 61 Bucket A (per `audit-to-gap-pipeline.md` §2.8 Fix-Time State-Check). Verify hiện trạng:
- Cloudflare DNS records cho `api.kitehub.me` + `kitehub.me`
- Cloudflare SSL mode + Always Use HTTPS settings
- AWS ALB `kitehub-alb` listener config + cert binding
- AWS ACM certs imported

Quy chiếu rules: `agent-aws-access.md` Tier 1 (read-only describe-* / list-*) only; KHÔNG mutation.

## Commands run

### Cloudflare (qua `scripts/cloudflare-dns.sh`)

```bash
bash scripts/cloudflare-dns.sh list             # list DNS records
bash scripts/cloudflare-dns.sh get-ssl-mode     # current SSL mode
bash scripts/cloudflare-dns.sh get-always-https # current Always Use HTTPS
```

### AWS (Tier 1 read-only)

```bash
aws elbv2 describe-load-balancers --names kitehub-alb --region ap-southeast-1
aws elbv2 describe-listeners --load-balancer-arn <ARN> --region ap-southeast-1
aws acm list-certificates --region ap-southeast-1
aws sts get-caller-identity
```

### DNS resolution

```bash
getent hosts api.kitehub.me
getent hosts kitehub.me
```

## Results

### Cloudflare zone `kitehub.me` (Zone ID `3adf4fc6532225cb928acbf57ca0206c`)

9 DNS records (verified live):

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| CNAME | `api.kitehub.me` | `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` | DNS only |
| CNAME | `*.kitehub.me` | `kitehub.me` | DNS only |
| CNAME | `kitehub.me` (apex) | `cname.vercel-dns.com` | DNS only |
| CNAME | `www.kitehub.me` | `kitehub.me` | DNS only |
| MX × 3 | `kitehub.me` | `route1/2/3.mx.cloudflare.net` (Email Routing) | DNS only |
| TXT | `cf2024-1._domainkey.kitehub.me` | DKIM | DNS only |
| TXT | `kitehub.me` | `v=spf1 include:_spf.mx.cloudflare.net ~all` | DNS only |

SSL settings:
- **SSL mode:** `full` (NOT `strict` — Tier 3 cutover pending)
- **Always Use HTTPS:** `off`

Wave plan §3 Bucket A target (final state):
- SSL mode: `strict`
- Always HTTPS: `on`

### AWS resources (account `906286017800`, region `ap-southeast-1`)

ALB `kitehub-alb` state:

| Field | Value |
|---|---|
| DNS name | `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` |
| State | `active` |
| Scheme | `internet-facing` |
| HostedZoneId | `Z1LMS91P8CMLE5` |
| Security Group | `sg-02dfda0973b34a130` |

ALB Listeners (verified):

| Port | Protocol | Certificates |
|---|---|---|
| 80 | HTTP | null |

**HTTPS:443 listener: MISSING.** Required for Cloudflare SSL Full (strict) per Tier 3 cutover §3.

AWS ACM (region ap-southeast-1):

```json
[]
```

**ACM is EMPTY** — Cloudflare Origin Cert (generated 2026-05-10 per PR #1084, stored local `~/.gcal-mcp/cloudflare-origin-cert/kitehub.me.pem`) **NOT yet imported**. Required for ALB HTTPS listener cert binding per Tier 3 cutover §2.

AWS auth (verification only):

```json
{
  "UserId": "AIDA5GAW3FUEDJ4ZZLVRK",
  "Account": "906286017800",
  "Arn": "arn:aws:iam::906286017800:user/solo-dev-admin"
}
```

### DNS resolution (global)

```
api.kitehub.me → 13.250.213.35 (kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com)
kitehub.me     → 76.76.21.164 + 66.33.60.34 (Vercel)
```

DNS bind ✅ working globally (resolves to ALB IP + Vercel anycast).

## Findings

### F1. DNS bind ALREADY DONE (Wave plan §3 Bucket A item 1)

`api.kitehub.me` CNAME → ALB DNS name **đã tồn tại từ Tier 2 (PR #1085 + earlier sessions)**. Wave 61 Bucket A scope cho item này = no-op; already in target state.

### F2. SSL Full strict + Always HTTPS BLOCKED bởi 2 dependency

Wave plan §3 Bucket A spec final state = SSL `strict` + Always HTTPS `on`. **KHÔNG thể flip toggle này từ Wave 61 Bucket A** vì:

1. **ALB HTTPS:443 listener missing** — Cloudflare SSL `strict` mode kiểm tra origin trả về cert hợp lệ. ALB hiện chỉ listen HTTP:80 → Cloudflare → ALB HTTPS handshake sẽ fail với `525 SSL handshake failed`.
2. **ACM cert empty** — Origin Cert chưa import → không thể bind vào HTTPS listener khi tạo listener.

Flip SSL `strict` lúc này khi user resume stack sẽ **break api.kitehub.me hoàn toàn** (Cloudflare error 525/526 cho mọi request).

### F3. Tier 3 cutover là user action (Path X CLI hoặc Path Y workflow_dispatch)

Per `release-1-tier-3-cutover.md` Step 2-3:
- §2 Import Cloudflare Origin Cert vào AWS ACM (`aws acm import-certificate` — Tier 3 mutation, banned cho agent per `agent-aws-access.md` §4.1)
- §3 Add HTTPS listener vào ALB (`aws elbv2 create-listener` — Tier 3 mutation, banned)
- §6 Flip SSL strict (qua `scripts/cloudflare-dns.sh set-ssl-mode strict` — user action sau Step 3 done)
- §7 Enable Always HTTPS (qua `scripts/cloudflare-dns.sh set-always-https on` — user action sau Step 6 done)

Plus: §1 Resume EC2 + RDS (`aws ec2 start-instances` — Tier 3 banned cho agent). Wave 61 mục tiêu stop-when-idle → stack STOPPED hiện tại.

→ Bucket A finalization = user resume stack + run Path X CLI hoặc Path Y workflow_dispatch.

## Verdict (per `audit-to-gap-pipeline.md` §2.8 Decision matrix)

State-check result: **"Symptom partially present (sub-set drifted)"**.

- DNS bind: ✅ self-corrected by earlier sessions
- SSL Full strict + Always HTTPS: ❌ blocked by ALB HTTPS listener + ACM cert dependency
- Stack STOPPED: by design (Wave 61 stop-when-idle pivot path (e))

→ Bucket A status = 🟡 PARTIAL. Scope revised: agent ships state-check + runbook sync, user owns Tier 3 finalization.

## Next steps

### Agent (this PR) — docs-only

1. Update `documents/05-guides/deploy/dns-setup-runbook.md` §2.4 reference current state + path forward
2. Update `documents/04-quality/gaps/GAP-369-production-dns-domain-setup.md` AC + Log với current state
3. Update `documents/04-quality/gaps/GAP-449-terraform-apply-workflow-dispatch-rule-revise.md` cross-link (Tier 3 cutover path Y workflow_dispatch eligibility — Wave 61 Bucket A pre-cutover state)
4. Cập nhật `documents/04-quality/gaps/gap-status.csv` rows tương ứng (per `gap-architecture-v2.md` canonical source)

### User action (post-merge, separate session)

When ready cho Phase 1 BETA invite cohort smoke test (depends on Wave 61 Buckets C/D/E + AWS Activate decision):

1. Path X CLI flow OR Path Y workflow_dispatch (per `release-1-tier-3-cutover.md` §0.5):
   - Step 1: `aws ec2 start-instances` (resume stack) — user manual per `agent-aws-access.md` §4.1
   - Step 2: `aws acm import-certificate` (Origin Cert)
   - Step 3: `aws elbv2 create-listener` HTTPS:443 với cert
   - Step 4: smoke test direct ALB HTTPS
   - Step 5: `bash scripts/cloudflare-dns.sh toggle-proxy api.kitehub.me` (proxied)
   - Step 6: `bash scripts/cloudflare-dns.sh set-ssl-mode strict`
   - Step 7: `bash scripts/cloudflare-dns.sh set-always-https on`
2. Smoke verify Bucket C (smoke-test.sh stop-when-idle scenario)
3. Stop stack post-smoke

### Wave 61 closure

- Bucket A status post-PR: 🟡 PARTIAL — agent scope DONE (state docs + gap sync), user action queued (Tier 3 cutover Step 2+3+6+7 per runbook)
- GAP-369 stays 🟡 PARTIAL (was 🟡 PARTIAL pre-Wave-61; no status flip — agent scope cũng PARTIAL)
- GAP-449 advances (Path Y workflow_dispatch path Y eligibility confirmed cho `api.kitehub.me` step)
