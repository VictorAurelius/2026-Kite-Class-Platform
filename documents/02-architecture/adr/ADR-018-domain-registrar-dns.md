# ADR-018: Domain Registrar, DNS Provider, and TLD Policy

**Status:** Accepted (draft — awaiting procurement confirmation of `.vn` availability)
**Date:** 2026-04-21
**Deciders:** @nguyenvankiet (founder), Infrastructure / Ops lead
**Supersedes:** —
**Related:** GAP-191, GAP-009 (instance provisioning), GAP-027 (multi-brand), GAP-037 (branded auth), ADR-004 (instance lifecycle)

---

## Context

KiteHub is a Vietnam-first SaaS that ships two public-facing surfaces:

1. **KiteHub marketing site** — `kitehub.vn` (primary), managed by founding team.
2. **KiteClass tenant instances** — `{slug}.kitehub.me` (platform subdomain) and optional custom domains for PREMIUM/ENTERPRISE tenants (per `documents/01-business/kitehub/domain-management/rules.md` DOM-01).

Until now, DNS was ad-hoc: `kitehub.vn` was assumed available but not yet verified as registered; no formal choice of registrar, DNS provider, or TLD policy; subdomain reserved-list undocumented; custom-domain (CNAME) verification flow had a mock-mode backend but no production operational spec (SSL issuance, renewal, failover).

Stakeholder decision points raised (GAP-191 §Proposed Fix):
- Registrar for `.vn`: Vietnamese TLD requires a local registrar accredited by VNNIC
- DNS provider: Cloudflare vs AWS Route 53 vs NS1
- Subdomain pattern: `{slug}.kitehub.me` vs `{slug}.kitehub.app` vs regional `.vn`
- Custom-domain SSL: wildcard DNS-01 vs per-domain HTTP-01
- Automation: Terraform? Manual? Hybrid?

---

## Decision

### 1. Registrar — two-provider split

| Domain | Registrar | Rationale |
|--------|-----------|-----------|
| `kitehub.vn` | **Matbao** (VNNIC-accredited) or **PA Vietnam** | `.vn` mandatory local registrar. Matbao has best VietQR/portal UX; PA Vietnam is fallback. Evaluate during procurement. |
| `kitehub.me` | **Cloudflare Registrar** (at-cost, USD ~$9/yr) | Already our DNS provider; no markup; native DNSSEC; no renewal markups. |
| `kitehub.app` (defensive) | Cloudflare Registrar | Same reasoning. Held as backup brand / dev environment. |

**TLD policy:**
- `.vn` is the **canonical** marketing TLD — VN consumers trust `.vn` substantially more than `.com`/`.app` for local services (per established VN e-commerce UX research).
- `.com` serves the multi-tenant platform layer (subdomains are API/runtime, not user-facing brand).
- No `.io`, `.net`, `.co` — not trust-signals in VN market; redirect attempts will 301 to `.vn` only if someone registers them defensively.

### 2. DNS provider — Cloudflare

All DNS (both TLDs) served by **Cloudflare** for:

- Free tier plenty for v1 traffic; scales to enterprise transparently
- Built-in DDoS + WAF + bot management (PREMIUM tenants inherit)
- API-first — Terraform provider (`cloudflare/cloudflare`) is first-class
- Native Let's Encrypt integration via Universal SSL + Custom Hostnames (SaaS feature) — zero-touch SSL for custom tenant domains
- Low TTL + rapid propagation — critical for tenant custom-domain onboarding (user expects their domain live within minutes)

**Rejected alternatives:**

| Provider | Why rejected |
|----------|--------------|
| AWS Route 53 | Stronger for AWS-native infra but we're not AWS-locked. No native custom-hostname SSL for SaaS — would require ACM + per-tenant cert automation, higher complexity. |
| NS1 | Excellent traffic steering but overkill; expensive; no consumer registrar integration. |
| Matbao DNS (same registrar) | Web UI only; no API; no DDoS — relegated to registrar role for `.vn`, not DNS. |

### 3. Subdomain pattern

**Canonical tenant URL:** `{slug}.kitehub.me`

Reasons over alternatives:
- `.com` on the platform layer is well-understood globally — tenants who scale internationally don't hit VN-specific friction
- One TLD for ALL tenants keeps wildcard SSL + Terraform module simple
- `.vn` for tenant subdomains would force every tenant into VN-registrar SLA + VNNIC WHOIS exposure — not the tenants' problem

Slug generation rules: see `domain-management/rules.md` §Subdomain Policy.

### 4. Custom-domain (CNAME) strategy

**Verification:** TXT record at `_kitehub-verify.{customer-domain}` (already implemented per DOM-02 mock mode). Production flow: DNS lookup via Cloudflare resolver with 30-min check cadence during 48h window.

**SSL issuance:**
- Cloudflare **Custom Hostnames** SaaS feature issues Let's Encrypt certs at their edge
- Cert automatically renewed by Cloudflare; no work for us
- Fallback: if tenant's domain is locked behind another CDN, they delegate NS or provide Cloudflare Strict SSL by copying our cert — runbook covers both

### 5. DNS automation — Terraform

- Module `infrastructure/terraform-aws/modules/dns/` owns all Cloudflare zone + record primitives
- Module consumed by:
  - Bootstrap (one-time `kitehub.vn` + `kitehub.me` root records)
  - Instance provisioning Saga (per-tenant subdomain record on tenant creation)
- Custom-hostname additions happen via **runtime Cloudflare API** (not Terraform) because they're tenant-driven, not infrastructure — Terraform would create drift

Skeleton ships in this PR (GAP-191); full apply deferred to infrastructure wave.

### 6. Failover

Single-origin for MVP (AWS ALB in ap-southeast-1). Multi-region deferred to post-GA. Failover runbook in `documents/05-guides/infrastructure/dns-operations.md` describes TTL tuning (300s for A, 60s during cutover) and the manual steps to repoint during incident.

---

## Consequences

### Positive

- Vietnamese user trust maximized (`.vn` canonical)
- Zero-cost DNS + DDoS + basic WAF via Cloudflare free tier
- Custom-hostname SSL is a solved problem, not a per-tenant engineering burden
- One DNS API surface (Cloudflare) for both TLDs — Terraform module stays simple
- SLA decoupled: marketing site outage does not cascade to tenant runtime

### Negative / Tradeoffs

- Locked to Cloudflare — migration cost if they raise prices or change SaaS pricing (mitigated: standard DNS records portable; only Custom Hostnames is sticky)
- Matbao has worse API than Cloudflare — `.vn` record management is semi-manual; acceptable because `kitehub.vn` records change rarely
- Enterprise tenants wanting apex root (not subdomain) custom-hostname need Cloudflare CNAME-flattening — works but niche edge cases possible

### Neutral

- `kitehub.app` held defensively — ~$10/yr insurance
- `.com` + `.net` for `kitehub` *not* defensively held; monitored via brand-watch tooling later

---

## Alternatives Considered

| Option | Summary | Why not |
|--------|---------|---------|
| All-AWS (Route 53 + ACM) | Single-vendor, tight IAM integration | Weak custom-hostname story for SaaS, more code to write |
| `.vn` for tenants too | Max VN trust | Registrar complexity × N tenants; blocks global scaling |
| Self-hosted DNS (PowerDNS + Let's Encrypt) | Zero vendor lock | Operational burden; not core competence |
| Skip custom domain entirely (subdomain only) | Simplest | Breaks PREMIUM/ENTERPRISE value prop per `rules.md` DOM-01 |

---

## Implementation Checklist

- [ ] Procurement: confirm `kitehub.vn` available; register via Matbao (2-year term)
- [ ] Procurement: register `kitehub.me` + `kitehub.app` via Cloudflare Registrar
- [ ] DNS: import zones into Cloudflare; set up team accounts with least-privilege API tokens
- [ ] Terraform: complete `modules/dns/` (this PR ships skeleton)
- [ ] Cloudflare: enable **Custom Hostnames** feature (Business plan required — revisit when tenant count justifies ~$200/mo)
- [ ] SSL: Universal SSL on for `kitehub.me` wildcard
- [ ] Subdomain rules: sync with backend `DomainService` to match rules.md slug regex
- [ ] Runbook: drill failover scenario once before GA
- [ ] Monitoring: Cloudflare Analytics + synthetic check from external region (per GAP-115)

---

## Log

- **2026-04-21** (v1.0.0) — ADR created as Wave 9-B deliverable closing GAP-191 decision points 1–7. Draft status: pending procurement confirmation of `kitehub.vn` availability; if taken, fall back to `.com.vn` (secondary) or brand rename escalated to stakeholders.
