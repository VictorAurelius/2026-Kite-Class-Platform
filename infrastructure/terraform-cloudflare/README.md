# Terraform — Cloudflare DNS (Phase 1 BETA)

**Status:** Bucket A scaffold (Wave 77) — NOT yet applied
**Created:** 2026-05-14
**Related:** GAP-370, GAP-533, GAP-530, ADR-018 (Cloudflare DNS primary), [`documents/05-guides/deploy/email-deliverability-runbook.md`](../../documents/05-guides/deploy/email-deliverability-runbook.md), [`documents/05-guides/deploy/resend-provisioning-runbook.md`](../../documents/05-guides/deploy/resend-provisioning-runbook.md)

---

## 1. Mục đích

Codify Cloudflare DNS records cho `kitehub.me` theo Infrastructure-as-Code. Trước đây DNS records được quản lý thủ công qua Cloudflare dashboard (per `documents/05-guides/deploy/cloudflare-setup.md`). Phase 1 BETA email SEND foundation (Wave 77 Bucket A) yêu cầu DKIM/SPF/DMARC records phải ổn định + tracked-in-git để re-apply nếu drift.

**Scope ban đầu (Bucket A):** chỉ email-related records (SPF + DKIM CNAMEs + DMARC). App records (apex A/CNAME → ALB hoặc Vercel) vẫn quản lý dashboard cho đến khi có wave riêng migrate.

## 2. Provider precedence

Per ADR-018: **Cloudflare DNS là primary**. AWS Route53 là optional fallback (xem `infrastructure/terraform-aws/route53.tf` — `manage_route53_zone = false` mặc định).

Khi Resend được setup (per `resend-provisioning-runbook.md`), Resend dashboard issue DKIM CNAME selectors thật (`resend._domainkey`, etc.). File `dns.tf` này chứa **placeholder values** — operator phải cập nhật `var.resend_dkim_*` (định nghĩa trong `variables.tf`) với giá trị thật từ Resend dashboard trước khi `terraform apply`.

## 3. Apply procedure

⚠️ **Wave 77 Bucket A KHÔNG run apply.** Apply là user-action follow-on theo runbook deliverability §3.

```bash
cd infrastructure/terraform-cloudflare
terraform init
terraform plan -out=tfplan
# Review plan: 5 records (SPF + 3 DKIM CNAME + DMARC)
# THEN terraform apply tfplan — only when Resend dashboard sẵn sàng và đã cấp DKIM values thật
```

Pre-apply checklist per `pre-mutation-state-check.md` §3:
- [ ] Resend dashboard đã add domain `kitehub.me` + có 5 record values thật (per `resend-provisioning-runbook.md` §2.2)
- [ ] `terraform.tfvars` (KHÔNG commit) cập nhật `cloudflare_api_token` + 3 DKIM CNAME targets thật
- [ ] Pre-mutation audit artifact tại `documents/04-quality/audits/cloudflare-verification/YYYY-MM-DD-bucket-a-email-dns.md` sẵn sàng
- [ ] Concurrent ops check (per `concurrent-production-mutation-ops.md` §3) — không có DNS PATCH/DELETE manual trong cùng phút

## 4. Files

| File | Purpose |
|------|---------|
| `dns.tf` | Cloudflare DNS records — SPF + DKIM CNAME × 3 + DMARC |
| `variables.tf` | Input vars (zone_id, api_token, DKIM CNAME targets) |
| `providers.tf` | Cloudflare provider declaration |
| `terraform.tfvars.example` | Sample tfvars (KHÔNG copy actual token vào git) |

## 5. State backend

Cùng remote backend với `infrastructure/terraform-aws/` (S3 `kitehub-tfstate-906286017800` + DynamoDB lock) nhưng KEY khác: `infrastructure/terraform-cloudflare/state.tfstate`. Cấu hình trong `providers.tf`.

## 6. Outside scope

- Apex A record (`kitehub.me`) — quản lý qua dashboard, migrate sau
- Vercel CNAME — quản lý qua Vercel
- AWS ACM domain validation records — Route53 nếu cần (xem ADR-018 §4)

## 7. Liên quan

- ADR-018 Cloudflare DNS primary
- GAP-370 Email Transactional Infrastructure
- GAP-533 Resend Deliverability Warm-up
- GAP-530 Email Flow End-to-End Verify
- `documents/05-guides/deploy/cloudflare-setup.md` — manual dashboard setup history
- `documents/05-guides/deploy/email-deliverability-runbook.md` — warm-up procedure consuming records này
