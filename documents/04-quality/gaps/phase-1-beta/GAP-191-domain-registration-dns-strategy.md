# GAP-191: Domain Registration & Instance DNS Strategy

**Status:** 🟡 PARTIAL (strategy + ADR + rules + runbook + Terraform skeleton shipped Wave 9-B; execution deferred to infra wave)
**Priority:** 🟠 P1 (business-logic tier — tenant-onboarding blocker)
**Domain:** Infrastructure / DevOps / SaaS / BRD
**Found:** 2026-04-20 (action-1 §6 + §15.B)
**Wave:** Wave 9 or 10 (parallel with GAP-190)
**Affects:** KiteHub marketing domain, KiteClass per-instance subdomains, tenant onboarding, custom-domain feature

## Problem

No documented domain / DNS strategy:
- kitehub.vn (or equivalent) — not yet registered / confirmed registrar
- Per-instance subdomain policy undefined — user ask (action-1 line 61): "domain của kitehub và các instance kiteclass sẽ được đăng ký và cấu hình như thế nào"
  - Candidate patterns: `{school}.kitehub.me`, `{school}.kitehub.me`, `{school}.kitehub.app`
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
5. **SSL strategy** — wildcard cert for `*.kitehub.me` via DNS-01 + per-custom-domain via HTTP-01
6. **DNS automation** — Terraform modules (`infrastructure/terraform-*/dns/`) + CI pipeline step during instance provisioning
7. **Failover** — health checks + secondary IP + TTL tuning for cutover
8. **Runbook** — `documents/05-guides/infrastructure/dns-operations.md`

## Acceptance Criteria

- [x] ADR for registrar + DNS provider + TLD approved (ADR-018 — Matbao for `.vn`, Cloudflare Registrar for `.com`/`.app`, Cloudflare DNS, Custom Hostnames for tenant CNAME SSL)
- [x] Subdomain reserved-list + slug rules documented (extended `documents/01-business/kitehub/domain-management/rules.md` — 7 reserved categories, 10 slug rules SLG-01…SLG-10)
- [x] Custom domain CNAME verification flow designed as state machine (rules.md §Custom-Domain Verification Flow)
- [x] Terraform module `dns/` skeleton lands (`infrastructure/terraform-aws/modules/dns/` with README + main.tf stub — full HCL deferred until Cloudflare token + registration complete)
- [ ] Provisioning E2E test: new tenant → DNS live → HTTPS 200 within 5 min — deferred (depends on Cloudflare Business plan + backend adapter; tracked as follow-up infra gap)
- [x] Failover runbook authored (`documents/05-guides/infrastructure/dns-operations.md` — SRE review deferred to post-procurement drill)

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
- 2026-04-21 — **Wave 9-B PARTIAL.** Strategy layer shipped: ADR-018 (registrar + DNS + TLD + SSL decisions), rules.md extension (DOM-11..13 + reserved slugs + SLG-01..10 + verification state machine), dns-operations.md runbook, Terraform skeleton at `modules/dns/`. Operational execution (procurement, Cloudflare Business plan, E2E test, SRE drill) deferred to follow-up infra wave — gap stays PARTIAL until executed; not GA-blocking for marketing site.
