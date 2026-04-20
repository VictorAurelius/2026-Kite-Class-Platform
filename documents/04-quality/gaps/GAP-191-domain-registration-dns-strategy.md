# GAP-191: Domain Registration & Instance DNS Strategy

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (business-logic tier — tenant-onboarding blocker)
**Domain:** Infrastructure / DevOps / SaaS / BRD
**Found:** 2026-04-20 (action-1 §6 + §15.B)
**Wave:** Wave 9 or 10 (parallel with GAP-190)
**Affects:** KiteHub marketing domain, KiteClass per-instance subdomains, tenant onboarding, custom-domain feature

## Problem

No documented domain / DNS strategy:
- kitehub.vn (or equivalent) — not yet registered / confirmed registrar
- Per-instance subdomain policy undefined — user ask (action-1 line 61): "domain của kitehub và các instance kiteclass sẽ được đăng ký và cấu hình như thế nào"
  - Candidate patterns: `{school}.kiteclass.com`, `{school}.kiteclass.vn`, `{school}.kitehub.app`
- No custom-domain (CNAME) support spec — required for P3/P5 tier schools that want branded domains
- No DNS automation — provisioning a tenant currently assumes DNS pre-exists
- No SSL/TLS automation (Let's Encrypt? ACM? Wildcard?) — impacts branding + auth flows
- No DNS failover / multi-region strategy

## Context

User asked explicitly during session (action-1 §6). Related gaps:
- GAP-009 instance provisioning lifecycle (consumes DNS output)
- GAP-027 multi-brand per tenant (assumes subdomain works)
- GAP-037 branded auth flows (cookie domain + SSO implications)
- GAP-190 KiteHub SEO (sibling marketing concern)

## Proposed Fix

1. **Registrar decision (ADR)** — `documents/02-architecture/adr/ADR-0NN-domain-registrar.md`
   - kitehub.vn (if available) + kitehub.app (or .io backup)
   - TLD policy for VN market (.vn preferred for local trust)
2. **DNS provider decision** — Cloudflare (recommended) vs Route53 vs NS1
3. **Subdomain policy**
   - Reserved list (api., app., www., admin., docs., status., mail.)
   - Tenant pattern: `{tenant-slug}.{core-domain}`
   - Slug generation: lowercase, hyphen-only, 3–32 chars, collision-check against reserved list
4. **Custom domain (CNAME) support** — DNS verification token, SSL issuance flow, renewal automation
5. **SSL strategy** — wildcard cert for `*.kiteclass.com` via DNS-01 + per-custom-domain via HTTP-01
6. **DNS automation** — Terraform modules (`infrastructure/terraform-*/dns/`) + CI pipeline step during instance provisioning
7. **Failover** — health checks + secondary IP + TTL tuning for cutover
8. **Runbook** — `documents/05-guides/dns-operations.md`

## Acceptance Criteria

- [ ] ADR for registrar + DNS provider + TLD approved
- [ ] Subdomain reserved-list + slug rules documented in `rules.md` under `documents/01-business/kitehub/provisioning/`
- [ ] Custom domain CNAME verification flow designed (state machine)
- [ ] Terraform module `dns/` provisions subdomain + SSL in one apply
- [ ] Provisioning E2E test: new tenant → DNS live → HTTPS serves 200 within 5 min
- [ ] Failover runbook reviewed by SRE

## Out of Scope

- Email DNS (SPF, DKIM, DMARC) — handled by email gap (GAP-021)
- CDN strategy — future optimization gap

## Related

- action-1 §6 + §15.B
- GAP-009 instance provisioning lifecycle
- GAP-027 multi-brand per tenant
- GAP-037 branded auth flows
- GAP-190 SEO (sister gap)
- Rule: `.claude/rules/meta-gap-priority.md` §3

## Log

- 2026-04-20 — Created from action-1 §15.B.
