# GAP-369: Production DNS + Domain Setup (kitehub.vn + kiteclass.vn)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING (Phase 1 BETA + Phase 1.5 PAID launch)
**Domain:** Infrastructure / DevOps
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** Public marketing surface accessibility, beta launch URL, production launch URL

## Problem

KHÔNG có DNS / domain setup cho production launch. Cần:
- Domain registration (kitehub.vn / kiteclass.vn or alternative)
- DNS records configuration (A/AAAA, CNAME, MX, TXT)
- SSL certs (Let's Encrypt automated)
- Subdomain strategy (beta.kitehub.vn vs kitehub.vn cutover)

## Proposed Fix

### Phase 1 BETA (v0.9.0-beta)
- Subdomain `beta.kitehub.vn` + `beta.kiteclass.vn` cho beta period
- DNS records pointing to Oracle Cloud VM IP
- Let's Encrypt SSL via certbot (cron renewal)
- TTL 5 min cho fast cutover khi cần

### Phase 1.5 PAID (v1.0.0)
- Production domain cutover: `kitehub.vn` + `kiteclass.vn`
- Blue-green: provision v1.0.0 IP first, DNS cut sau
- TTL 5 min trong cutover; revert to 1h post-stable

## Acceptance Criteria

- [ ] Domain registered (kitehub.vn + kiteclass.vn or chosen alternatives)
- [ ] DNS provider chosen (Cloudflare recommended cho proxy + DDoS — see GAP-371)
- [ ] DNS records configured: A/AAAA, MX (cho email — see GAP-370), TXT (SPF/DMARC/DKIM)
- [ ] SSL certs Let's Encrypt automated (certbot cron)
- [ ] beta.kitehub.vn + beta.kiteclass.vn live cho Phase 1 BETA
- [ ] kitehub.vn + kiteclass.vn cutover plan documented
- [ ] DNS propagation verification step trong runbook

## Open decisions

- Domain registrar: Vietnam hosting (Mat Bao, FPT, ...) vs international (Namecheap, GoDaddy)?
- Cloudflare proxy: yes (recommend) — provides DDoS + cache + WAF free tier
- Subdomain strategy: beta.* prefix vs separate domain
- Email subdomain: noreply@kitehub.vn? mail.kitehub.vn?

## Effort estimate

~1-2 ngày. Quick once decisions made; certificate provisioning + DNS propagation is bottleneck.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md`
- Sister: GAP-371 (CDN/Cloudflare setup), GAP-370 (email transactional)
- Architecture: `documents/03-planning/infrastructure/kitehub-oracle-cloud-deployment.md`

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. BLOCKING cho Phase 1 BETA launch — không có domain = không có public URL.
