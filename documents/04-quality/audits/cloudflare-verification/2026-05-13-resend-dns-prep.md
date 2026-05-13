---
title: Cloudflare DNS audit — Resend prep state-check
status: complete
created: 2026-05-13
phase: Wave 71b parallel — GAP-513 user-action prep
gaps: [GAP-513]
---

# Cloudflare DNS Audit — Resend Provisioning Prep

## Scope

Read-only audit of `kitehub.me` Cloudflare zone DNS records to determine current state of email-related records (SES + Resend) before user provisions Resend API key per `documents/05-guides/deploy/resend-provisioning-runbook.md`.

## Commands run (Tier 1 read-only per agent-aws-access.md §2.1)

```
GET https://api.cloudflare.com/client/v4/user/tokens/verify         # token validity
GET https://api.cloudflare.com/client/v4/zones?name=kitehub.me      # zone metadata
GET https://api.cloudflare.com/client/v4/zones/{zone_id}/dns_records?per_page=100
```

## Zone metadata

- **Zone ID:** `bb54ef8f69b0ef03085ce8903d90a5a4`
- **Status:** active
- **Plan:** Free Website
- **Name servers:** `melody.ns.cloudflare.com`, `randy.ns.cloudflare.com`
- **Total records:** 17

## DNS records (full inventory)

| Type | Proxy | Name | Content |
|---|---|---|---|
| CNAME | grey | `*.kitehub.me` | `kitehub.me` |
| CNAME | **orange** | `api.kitehub.me` | `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` |
| CNAME | grey | `bwi47im4yhh7mg3r5icm57epjtsjq7gc._domainkey.kitehub.me` | `bwi47im4...dkim.amazonses.com` |
| CNAME | grey | `caljer7brmlf2t4k3nlszwamrdurruhk._domainkey.kitehub.me` | `caljer7b...dkim.amazonses.com` |
| CNAME | grey | `ehl6hjjfash2xoa3uqwcfvseyr23yrjv._domainkey.kitehub.me` | `ehl6hjjf...dkim.amazonses.com` |
| CNAME | grey | `kitehub.me` | `cname.vercel-dns.com` |
| CNAME | grey | `www.kitehub.me` | `kitehub.me` |
| MX | grey | `kitehub.me` | `route1/2/3.mx.cloudflare.net` (3 records) |
| MX | grey | `send.kitehub.me` | `feedback-smtp.ap-northeast-1.amazonses.com` |
| TXT | grey | `_amazonses.kitehub.me` | `T1ep7Ks+wWU6M7DVIpQe+Sery9QhGwOUPHXN7yMUJrM=` |
| TXT | grey | `_dmarc.kitehub.me` | `v=DMARC1; p=quarantine; rua=mailto:dmarc@kitehub.me; ruf=mailto:dmarc@kitehub.me` |
| TXT | grey | `cf2024-1._domainkey.kitehub.me` | `v=DKIM1; h=sha256; k=rsa; p=MIIBIj...` (Cloudflare Email Routing DKIM) |
| TXT | grey | `kitehub.me` (root SPF) | `v=spf1 include:_spf.mx.cloudflare.net include:amazonses.com ~all` |
| TXT | grey | `resend._domainkey.kitehub.me` | `p=MIGfMA0G...` ⭐ **RESEND DKIM PRESENT** |
| TXT | grey | `send.kitehub.me` (subdomain SPF) | `v=spf1 include:amazonses.com ~all` |

## Findings

### ✅ Real changes (records relevant to Resend)

1. **Resend DKIM record EXISTS** — `resend._domainkey.kitehub.me TXT` đã được set với DKIM public key. Indicates user (or prior session) đã add Resend domain trong Resend dashboard và copy DNS records.
2. **Grey-cloud (DNS only)** — đúng pattern; DKIM không bị proxy strip.

### ⚠️ Gaps requiring verification

1. **Resend dashboard status unknown** — DNS record present nhưng chưa biết Resend đã verify domain `kitehub.me` chưa. Need user check `dashboard.resend.com` → Domains → kitehub.me → status field.
2. **Root SPF KHÔNG include resend.com** — current `v=spf1 include:_spf.mx.cloudflare.net include:amazonses.com ~all`. Resend KHÔNG bắt buộc SPF (uses envelope sender + DKIM is sufficient for inbox placement) nhưng best practice add `include:_spf.resend.com` để cover bounce path.
3. **From address chưa rõ** — `noreply@kitehub.me` (root) vs `noreply@send.kitehub.me` (subdomain). Resend với DKIM `resend._domainkey` on root → from address sẽ là `*@kitehub.me`. Compose env `AWS_SES_FROM_EMAIL=noreply@kitehub.me` đã đúng.

### 📋 Existing AWS SES infrastructure (parallel — không xung đột với Resend)

- 3 SES DKIM CNAMEs (`*.dkim.amazonses.com`)
- `_amazonses.kitehub.me` verification TXT
- `send.kitehub.me` MX + SPF cho SES bounces
- Indicates AWS SES production approval has been pursued separately

This is fine: KiteHub `EMAIL_PROVIDER=resend` in production compose; SES infra is dormant fallback. Both can coexist.

### 🟡 Cloudflare Email Routing inbound

- `kitehub.me MX → route1/2/3.mx.cloudflare.net` — handles inbound to `*@kitehub.me`
- `cf2024-1._domainkey.kitehub.me` — Cloudflare's signing key cho forwarded emails
- This is OUTBOUND-agnostic — Resend handles outbound, CF handles inbound. No conflict.

### Verdict

State sufficient for user to:
1. **Skip Bước 1-3** of resend-provisioning-runbook IF Resend dashboard shows `kitehub.me` already verified (DKIM record exists → high probability domain verify succeeded)
2. **Proceed directly to Bước 5** (Generate API key) and Bước 6 (Store in AWS Secrets Manager)

If Resend dashboard shows domain `unverified` despite DKIM record present:
- Check key value matches (Resend might have rotated key; current record may be stale)
- Click "Verify DNS records" in Resend dashboard to re-probe

## Recommendations

1. **User action** — Open https://resend.com/domains → check `kitehub.me` status
2. If verified → generate API key + AWS Secrets store
3. If not verified → diff Resend's expected DKIM value vs current `resend._domainkey.kitehub.me TXT`
4. Optionally tighten SPF: add `include:_spf.resend.com` (defense-in-depth; not required for Resend to work)

## References

- GAP-513 Resend manual provisioning
- `documents/05-guides/deploy/resend-provisioning-runbook.md`
- Cloudflare API: `https://api.cloudflare.com/#dns-records-for-a-zone-list-dns-records`
- Token verify: `cfut_*` user token, scope `Read all resources` (active per 2026-05-13 verify call)
