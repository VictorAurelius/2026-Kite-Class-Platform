---
id: GAP-816
title: Custom domain SSL provisioning automation + scheduled polling (Phần B v2 + Phần C của GAP-812)
status: OPEN
priority: P2
phase: phase-1-beta
domain: Backend
created: 2026-06-01
parent: GAP-812
---

# GAP-816 — Custom domain SSL provisioning automation + scheduled polling

## Problem

GAP-812 Wave tenant-domain-1 Bucket D (2026-06-01) shipped Phần A (DNS TXT verify thật) + Phần B v1 (terraform ACM scaffold apply DEFERRED) + Phần C v1 (enum CERT_PROVISIONING extended). Deferred follow-up scope:

1. **SSL provisioning automation chưa wire end-to-end:**
   - Tenant verify TXT → status `VERIFIED` directly (skip CERT_PROVISIONING)
   - Khi tenant truy cập `https://lop.skyedu.vn` → browser cert error vì chưa có cert
   - Workaround hiện tại: backup URL `{subdomain}.kiteclass.com` luôn có HTTPS hợp lệ (BR-DOMAIN-007)

2. **Scheduled polling job chưa có:**
   - `verifyCustomDomain` chỉ chạy khi tenant manual trigger qua UI "Verify ngay" button
   - Không có background job tự periodic DNS lookup (mỗi 10 phút) → tenant đã thêm TXT đúng nhưng không bấm verify → status kẹt PENDING_VERIFY mãi
   - Timeout 48h (BR-DOMAIN-003) chưa enforce — không có job flip PENDING_VERIFY → FAILED khi quá deadline

3. **Cloudflare for SaaS integration chưa có:**
   - Industry pattern preferred per GAP-812 §Outside-in: Cloudflare Custom Hostnames API tự cấp cert + auto-renew
   - `CloudflareCustomHostnameClient` adapter chưa code; secret Cloudflare API key chưa setup trong Secrets Manager + terraform IaC declaration

4. **ACM apply chưa execute:**
   - `infrastructure/terraform-aws/acm-tenant-domains.tf` scaffold landed (GAP-812 PR) nhưng default `tenant_custom_domains = []` → apply là no-op
   - Cần manual fill `var.tenant_custom_domains` với verified domain + run apply

5. **SSL-pending fallback UI chưa có:**
   - Khi `status=CERT_PROVISIONING`, truy cập custom domain qua HTTPS → cert error (browser không trust)
   - Cần banner UI "Cert đang cấp..." + auto-redirect tạm về backup subdomain (BR-DOMAIN-007)

## Proposed Fix

### Approach A — Cloudflare for SaaS (preferred per GAP-812 §Phần B + outside-in audit)

**Phase 1 — Cloudflare API integration:**
1. Đăng ký Cloudflare for SaaS subscription cho `kitehub.me` zone (Phase 1.5 budget approval)
2. Implement `CloudflareCustomHostnameClient` adapter trong `kitehub-subscription/.../client/`
   - `POST /zones/{zone_id}/custom_hostnames` — create custom hostname
   - `GET /zones/{zone_id}/custom_hostnames/{id}` — poll cert status
   - `DELETE /zones/{zone_id}/custom_hostnames/{id}` — cleanup on `removeCustomDomain`
3. Cloudflare API token lưu AWS Secrets Manager `kitehub/production/cloudflare-saas-api-token` + terraform IaC declaration + IAM grant
4. `DomainService.verifyCustomDomain()` extend: TXT match → call `CloudflareCustomHostnameClient.createCustomHostname()` → flip status `CERT_PROVISIONING`

**Phase 2 — Cert status polling:**
5. `DomainVerificationScheduler.pollCertProvisioning()` `@Scheduled(fixedDelay = 600_000)` mỗi 10 phút query `CloudflareCustomHostnameClient.getStatus()` cho instances ở `CERT_PROVISIONING`
6. Cert active → flip `VERIFIED` + publish outbox event `domain.verified` (downstream consumers: invalidate cache, send email tenant, etc.)

### Approach B — AWS ACM apply automation (alternate)

Nếu Cloudflare for SaaS không khả thi (cost, vendor lock-in):
1. Manual fill `infrastructure/terraform-aws/acm-tenant-domains.tf` `var.tenant_custom_domains` với verified domain
2. Terraform apply qua dev-trigger workflow per `release-deploy-standard.md` §9
3. Output `tenant_acm_validation_records` → instruct tenant add CNAME records to DNS provider
4. ACM cert provisions automatically once CNAME match
5. ALB listener Lambda subscriber to attach cert dynamically (Phase 2 scope)

### Phase 3 — Scheduled DNS polling + timeout

1. `DomainVerificationScheduler.pollPendingDomains()` `@Scheduled(fixedDelay = 600_000)` mỗi 10 phút:
   - Query instances `WHERE domainStatus=PENDING_VERIFY AND custom_domain IS NOT NULL`
   - Per instance: gọi `DnsTxtLookupService.verifyTxtRecord()` → match → flip `CERT_PROVISIONING`
   - Quá `kitehub.domain.verification.timeout-hours` (48h default) chưa match → flip `FAILED` + log admin_audit_log

2. `mockMode=false` enable cho production profile (`application-production.yml`)

### Phase 4 — SSL-pending fallback UI

1. FE check `domainStatus=CERT_PROVISIONING` → banner "Cert đang cấp..." trên custom domain pages
2. Reverse proxy / Cloudflare Workers redirect rule: nếu cert chưa active → auto-redirect to backup `{subdomain}.kiteclass.com` với banner

## Acceptance Criteria

- [ ] `CloudflareCustomHostnameClient` adapter implementation (Cloudflare API integration) — Approach A
- [ ] Cloudflare API token lưu AWS Secrets Manager + terraform IaC declaration + IAM grant (per `local-fix-production-parity-check.md`)
- [ ] `DomainService.verifyCustomDomain()` extended: TXT match → trigger cert provision → flip CERT_PROVISIONING
- [ ] `DomainVerificationScheduler.pollCertProvisioning()` @Scheduled job — poll cert status mỗi 10 phút → flip VERIFIED khi active
- [ ] `DomainVerificationScheduler.pollPendingDomains()` @Scheduled job — periodic DNS TXT verify + timeout 48h → FAILED enforcement
- [ ] Outbox event `domain.verified` publish + downstream consumer (cache invalidate, tenant email notification)
- [ ] Production profile `mockMode=false` flip + verify in production
- [ ] SSL-pending fallback UI: banner "Cert đang cấp..." + auto-redirect to backup subdomain trong CERT_PROVISIONING state
- [ ] Runbook update `custom-domain-verify-runbook.md` §6 với operational details (cert provision SLA, troubleshooting cert renew fail, CAA/DNSSEC override)
- [ ] RST walk per `feature-ship-runtime-walk-mandate.md`: tenant PREMIUM gắn domain → thêm TXT → verify → cert provision → browse `https://{domain}` có HTTPS hợp lệ + route đúng tenant; backup URL hoạt động trong lúc cert provision

## Related

- Parent: GAP-812 (Phần A DONE + Phần B v1 scaffold + Phần C enum extended)
- Sibling: GAP-811 (FE middleware host→tenant resolution) — cert provisioning unblocks public custom domain HTTPS access
- Sibling: GAP-813 (Public slug→UUID resolve endpoint) — base domain consistency
- ADR-018 Domain Registrar / DNS / TLD
- `infrastructure/terraform-aws/acm-tenant-domains.tf` (scaffold landed GAP-812)
- `kitehub/kitehub-subscription/.../service/DomainService.java` (Phần A integration point)

## Log

- **2026-06-01:** Gap created — Wave tenant-domain-1 Bucket D follow-up to GAP-812. Defers Phần B v2 SSL automation + Phần C scheduler per `release-deploy-standard.md` §9 (terraform apply human-only) + Cloudflare for SaaS budget approval blocker. Phase 1 BETA acceptable: backup URL `{subdomain}.kiteclass.com` always HTTPS available — custom domain HTTPS scope = Phase 1.5+ enhancement. P2 priority retained (PREMIUM/ENTERPRISE tier only; backup unblocks Phase 1 BETA).
