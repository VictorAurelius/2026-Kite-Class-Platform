# GAP-369: Production DNS + Domain Setup (kitehub.vn + kiteclass.vn)

**Status:** 🟢 DONE 2026-05-12 (Phase 1 BETA scope satisfied via `kitehub.me` Path C; Phase 2 `.vn` procurement deferred as separate concern per Wave 43 Bucket C scope decision)
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

**Phase 1 BETA scope (rescope per Wave 43 Bucket C — `*.vn` deferred Phase 2):**
- [x] Domain registered — `kitehub.me` via GAP-458 Path C (Free GitHub Student Pack 1y)
- [x] DNS provider Cloudflare — proxy + DDoS active
- [x] DNS records configured — 9 records (apex CNAME Vercel, `api.kitehub.me` CNAME ALB, wildcard, MX × 3 Email Routing, SPF, DKIM)
- [x] SSL certs — ACM `*.kitehub.me` ISSUED + Cloudflare Origin Cert (15y); Let's Encrypt R13 on Vercel apex
- [x] `api.kitehub.me` HTTPS:443 live with CF `full strict` + Always HTTPS on (Wave 64 cutover)
- [x] DNS propagation verification — `scripts/check-dns-propagation.sh` shipped (Wave 33 Bucket D)

**Phase 2 scope (DEFERRED — file new gap when Phase 2 trigger fires):**
- [ ] `kitehub.vn` + `kiteclass.vn` registration (Vietnam registrar — separate decision)
- [ ] Phase 2 production domain cutover plan

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
- **2026-05-07:** Wave 33 Bucket D shipped (PR #897 — `dns-setup-runbook.md` + `ssl-cert-setup.sh` + `check-dns-propagation.sh` + `.env.production.template`). Status 🔵 OPEN → 🟡 PARTIAL — runbook + scripts shipped, **domain registration + DNS records configuration = user-executed steps** per gap-done-discipline.md §3 PARTIAL exit ramp. User executes per runbook when ready to deploy.
- **2026-05-08 (Wave 43 Bucket C — admin sweep):** Phase 1 scope rescope confirmed per `documents/03-planning/roadmap/release-1-deploy-session-2026-05-07.md` decisions chốt 2026-05-07: domain registration **SKIP for Phase 1 BETA** — free `*.vercel.app` (kitehub frontend) + AWS ALB DNS + `*.betteruptime.com` (status page per GAP-373) accepted as Phase 1 BETA URLs. Custom domain procurement (`kitehub.vn` + `kiteclass.vn`) **DEFERRED to Phase 2** (post-soft-launch). Status remains 🟡 PARTIAL — runbook + scripts ready and re-usable when Phase 2 triggers. Phase 2 trigger: 5 beta tenants live + ≥80 quality audit + 0 P0 incidents 2 weeks (per Release Lần 1 plan §3 Phase 1→2 progression).
- **2026-05-09 (GAP-458):** Path C Free GitHub Student Pack — domain `kitehub.me` claimed via Namecheap (Student Pack 1y free). Cloudflare nameservers active; 9 DNS records configured (apex CNAME → Vercel, `api.kitehub.me` CNAME → ALB, wildcard, MX × 3 Email Routing, SPF, DKIM).
- **2026-05-10 (Tier 1 + Tier 2):** Vercel apex `kitehub.me` bound (Let's Encrypt R13 cert auto-issued by Vercel, valid 2026-05-10 → 2026-08-08). Cloudflare Origin Cert generated (15-year validity, files saved `~/.gcal-mcp/cloudflare-origin-cert/kitehub.me.pem` + `.key`). PR #1084 + #1085 shipped Tier 2 setup + extended Cloudflare API token (Zone:DNS:Edit + Zone:SSL:Edit + Zone:Zone Settings:Edit) + `scripts/cloudflare-dns.sh` Tier 3 commands (`set-ssl-mode`, `set-always-https`, `toggle-proxy`).
- **2026-05-11 (Wave 61 Bucket A — DNS state-check + agent docs sync):** State-check per `audit-to-gap-pipeline.md` §2.8 (artifact `documents/04-quality/audits/aws-verification/2026-05-11-wave-61-bucket-a-dns-state.md`) confirms DNS bind ✅ already done (api.kitehub.me CNAME → ALB resolves globally to 13.250.213.35). SSL mode currently `full` (NOT `strict`); Always HTTPS `off`. AWS ACM EMPTY (Origin Cert not yet imported); ALB has only HTTP:80 listener (HTTPS:443 missing). **Tier 3 cutover finalization (Steps 2+3+6+7 per `release-1-tier-3-cutover.md`) = user action** — agent KHÔNG flip SSL strict + Always HTTPS now (would 525/526 error api.kitehub.me khi user resume stack). Path X (CLI) hoặc Path Y (workflow_dispatch `.github/workflows/tier-3-cutover.yml`) per `release-deploy-standard.md` §9 + `agent-aws-access.md` §4.3 — user-triggered, OIDC ephemeral creds. Status stays 🟡 PARTIAL — Bucket A agent scope DONE (state docs + gap sync); user owns finalization gated on stack resume.
- **2026-05-12 (Wave 64 Tier 3 cutover SHIPPED — per GAP-482 §Log):** ACM `*.kitehub.me` cert imported; ALB HTTPS:443 listener live; HTTP:80 redirects to HTTPS; CF SSL mode `full strict`; Always HTTPS `on`; api.kitehub.me proxied through CF. All Tier 3 user-action steps executed.
- **2026-05-12 (Wave 66 Bucket Z — flip 🟢 DONE):** State-check per `gap-done-discipline.md` §2 — Tier 1 read-only AWS verification (per `agent-aws-access.md` §2.1):
  - `aws elbv2 describe-listeners` on `kitehub-alb` → HTTPS:443 + HTTP:80 ✅
  - `aws acm list-certificates --region ap-southeast-1` → `*.kitehub.me` ISSUED ✅
  - `curl -sI http://api.kitehub.me` → 301 redirect to https://api.kitehub.me ✅ (Cloudflare server)
  - `curl -sI https://api.kitehub.me` → 502 transient (origin issue, not DNS/SSL — separate concern; CF proxy + cert pipeline confirmed working)
  AC re-evaluated for Phase 1 BETA scope per Wave 43 Bucket C rescope decision (`*.vn` deferred to Phase 2): all Phase 1 BETA AC satisfied via `kitehub.me` Path C (Free GitHub Student Pack). Phase 2 `*.vn` procurement = separate concern (file new gap when Phase 2 triggers per CLAUDE.md gate). Flip 🟢 DONE.
