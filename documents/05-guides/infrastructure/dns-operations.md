# DNS Operations Runbook

**Status:** Draft (implementation deferred — depends on Cloudflare Business plan procurement)
**Last-Reviewed:** 2026-04-21
**Owner:** Infrastructure / Ops lead
**Depends on:** [ADR-018 Domain Registrar / DNS](../02-architecture/adr/ADR-018-domain-registrar-dns.md)
**Audience:** on-call engineer, SRE, founding team during incident

---

## 0. Quick Reference

| Concern | First action |
|---------|--------------|
| `kitehub.vn` down | Check Cloudflare dashboard → Zone status → contact Matbao registrar |
| `*.kitehub.me` tenant subdomain not resolving | Check Cloudflare `kitehub.me` zone → Terraform drift |
| Tenant custom domain SSL expired | Check Cloudflare Custom Hostnames → Let's Encrypt renewal status |
| DDoS surge | Cloudflare → Under Attack mode (1-click) |
| DNS propagation slow | Lower TTL → 60s → wait → re-check resolver cache |

---

## 1. Zones Managed

| Zone | Registrar | DNS Provider | Terraform managed? |
|------|-----------|--------------|-------------------|
| `kitehub.vn` | Matbao (or PA Vietnam) | Cloudflare | Yes (`modules/dns/` — root records only) |
| `kitehub.me` | Cloudflare Registrar | Cloudflare | Yes (root + per-tenant subdomain records) |
| `kitehub.app` (defensive) | Cloudflare Registrar | Cloudflare | Yes (redirect to `.vn`) |
| `{customer-domain}` (tenant custom) | Tenant's registrar | Tenant's DNS → CNAME to us | No — Cloudflare Custom Hostnames manages SSL |

---

## 2. Tenant Subdomain Provisioning

**Trigger:** Instance provisioning Saga completes (per ADR-004).

**Automation:**
1. Provisioning service calls Cloudflare DNS API (via Terraform at bootstrap; per-tenant via runtime adapter)
2. Creates `{slug}.kitehub.me` A record pointing to ALB (or AAAA for IPv6)
3. Universal SSL auto-issues cert for `*.kitehub.me` — no per-tenant cert work
4. Propagation: Cloudflare edge within 60s; global DNS resolvers within 5 min

**Manual fallback (if API fails):**
1. Cloudflare dashboard → `kitehub.me` zone → Add record
2. Type: A, Name: `{slug}`, Value: `<ALB IP>`, TTL: Auto (300s), Proxied: ON
3. Verify: `dig {slug}.kitehub.me +short` (should return Cloudflare IPs, not ALB direct)

---

## 3. Tenant Custom-Domain Onboarding

**Precondition:** Tenant has PREMIUM or ENTERPRISE tier (DOM-01).

**Flow:**
1. Tenant enters their domain `classes.example.edu.vn` → backend calls `initiateCustomDomain()` → receives verification token (DOM-02)
2. Tenant adds TXT record: `_kitehub-verify.classes.example.edu.vn` = `kitehub-verify={uuid}`
3. Tenant adds CNAME: `classes.example.edu.vn` → `{slug}.kitehub.me`
4. Backend scheduled job polls DNS every 5 min for first hour, then every 30 min up to 48h
5. On TXT match → status becomes VERIFIED → backend calls Cloudflare Custom Hostnames API
6. Cloudflare issues Let's Encrypt cert via HTTP-01 validation (~5–15 min)
7. Tenant site serves HTTPS from `classes.example.edu.vn`

**SSL renewal:** Cloudflare auto-renews every 60 days. If renewal fails (e.g., tenant removed CNAME), Custom Hostnames alert fires → on-call contacts tenant.

---

## 4. Incident Response

### 4.1 DNS resolution failure

**Symptoms:** users report site unreachable; external synthetic check failing.

**Triage (5 min):**
1. `dig kitehub.vn +trace` — is Cloudflare returning answer?
2. Cloudflare Analytics → Traffic tab → is origin reachable?
3. Cloudflare Dashboard → Zone → check for "Degraded" banner

**If Cloudflare is the problem:**
- Check status.cloudflare.com
- If Cloudflare global outage: there is no plan B in MVP. Document the incident; customer comms via status page (if exists) or Twitter
- Post-mortem: consider multi-provider DNS (AWS Route 53 secondary) as Q3 work

**If zone config drifted:**
- Compare `infrastructure/terraform-aws/modules/dns/` state vs live Cloudflare
- `terraform plan` in dns module → apply if safe
- Never hand-edit Cloudflare records for automated zones (drift compounds)

### 4.2 DDoS attack

1. Cloudflare Dashboard → "I'm Under Attack" mode (1-click)
2. Monitor traffic in Analytics → Firewall Events
3. If attack persists > 1h: enable Rate Limiting rules; consider adding IP reputation challenges
4. Keep Under Attack mode on for minimum 30 min past attack end; disabling too fast invites retry

### 4.3 Origin failover (manual)

**Scenario:** ALB in ap-southeast-1 unhealthy; need to reroute.

1. Lower A/AAAA record TTL to 60s (Cloudflare: set TTL manually)
2. Wait 5 min for propagation
3. Update A record to secondary region ALB IP
4. Verify via multiple external resolvers: `dig +short @8.8.8.8`, `@1.1.1.1`, `@9.9.9.9`
5. Once stable, restore TTL to 300s
6. File incident retro within 24h

**Note:** multi-region is post-GA; during MVP, failover = fix origin or stay down. Document the outage.

---

## 5. Common Operations

### 5.1 Adding a new reserved subdomain prefix

1. Edit `documents/01-business/kitehub/domain-management/rules.md` §Reserved prefixes
2. Update backend config `kitehub.domain.reserved-slugs` source file
3. Deploy backend
4. Push corresponding frontend validator list (public JSON asset) so client-side form rejection matches

### 5.2 Retiring a tenant (off-boarding)

1. Backend marks instance `OFF_BOARDING` (per `tenant-off-boarding-runbook.md`)
2. DNS record retained for 30-day grace period (`{slug}.kitehub.me` 404s or redirects to off-boarding page)
3. Day 31: DNS record removed via Terraform
4. Custom hostname (if any) removed from Cloudflare Custom Hostnames → cert revoked

### 5.3 Rotating Cloudflare API tokens

Every 90 days:
1. Cloudflare dashboard → API tokens → create new token with same scoped perms
2. Update `terraform-aws/secrets.tf` or secret manager entry
3. Run `terraform apply` with no drift expected — re-plan verifies
4. Revoke old token

---

## 6. Monitoring + Alerts (deferred to Wave 7 ops)

Per GAP-115 log aggregation + monitoring:

| Alert | Threshold | Paging |
|-------|-----------|--------|
| `kitehub.vn` DNS query failures | >5% error rate over 5 min | on-call page |
| `*.kitehub.me` SSL cert expiry | <14 days to expiry | ticket (Cloudflare usually renews silently) |
| Cloudflare API 5xx (Terraform/Saga) | >3 consecutive failures | ops channel |
| Custom Hostnames verification stuck >48h | per hostname | tenant email (auto) + CS channel |

---

## 7. Checklist for New Tenant Custom-Domain

Ops-facing checklist to share with tenant:

- [ ] Tenant verifies they own the domain (WHOIS / corporate email proof)
- [ ] Tenant on PREMIUM or ENTERPRISE tier (DOM-01)
- [ ] Tenant receives token via `initiateCustomDomain` API
- [ ] Tenant adds TXT + CNAME records (screenshots in admin portal as assistance)
- [ ] Backend verification succeeds within 48h
- [ ] Cloudflare Custom Hostname provisioned
- [ ] SSL cert issued (confirm via `openssl s_client -connect {domain}:443`)
- [ ] Tenant tests: register new class → see their domain in URL bar
- [ ] Ticket closed only after tenant acknowledges

---

## 8. Related

- ADR: [ADR-018](../02-architecture/adr/ADR-018-domain-registrar-dns.md)
- Rules: [`documents/01-business/kitehub/domain-management/rules.md`](../01-business/kitehub/domain-management/rules.md)
- Gap: GAP-191 (this runbook closes §Proposed Fix item 8)
- Terraform: [`infrastructure/terraform-aws/modules/dns/`](../../infrastructure/terraform-aws/modules/dns/)

## Log

- **2026-04-21** (v1.0.0) — Runbook created as Wave 9-B deliverable closing GAP-191. Runbook is SPEC; operational enablement (Cloudflare Business plan, first tenant custom-domain drill) tracked as follow-up infrastructure work after ADR-018 procurement completes.
