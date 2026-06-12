# Terraform Module — DNS (Cloudflare)

**Status:** Skeleton — minimal stub shipping as part of GAP-191 (Wave 9-B).
Full resource definitions land in a follow-up infrastructure wave once Cloudflare API credentials are provisioned and `kitehub.vn` registration completes.

**Reference:** [ADR-018 Domain Registrar / DNS](../../../../documents/02-architecture/adr/ADR-018-domain-registrar-dns.md)
**Runbook:** [DNS Operations](../../../../documents/05-guides/infrastructure/dns-operations.md)
**Owner:** Infrastructure / Ops lead

---

## Purpose

Manages DNS zones + records for the KiteHub platform:

- `kitehub.vn` — marketing site (A/AAAA, MX, TXT for domain proof + SPF/DKIM, CAA)
- `kitehub.me` — multi-tenant platform (wildcard A/AAAA for `*.kitehub.me`, root records)
- `kitehub.app` — defensive; redirect to `.vn`

Tenant-specific subdomain records are created at **runtime** by the provisioning Saga calling the Cloudflare API adapter — NOT managed by Terraform to avoid drift.

---

## Intended Resources (to be implemented)

| Resource | Purpose | Managed here? |
|----------|---------|---------------|
| `cloudflare_zone` (×3) | Zones for kitehub.vn, kitehub.me, kitehub.app | Yes |
| `cloudflare_record` root A/AAAA | Point apex to ALB | Yes |
| `cloudflare_record` wildcard `*` on kitehub.me | Fallback for tenants pre-provisioned subdomain | Yes |
| `cloudflare_record` MX / SPF / DKIM / DMARC | Email DNS (coordinates with GAP-021 email infra) | Yes |
| `cloudflare_record` CAA | Restrict cert issuance to Let's Encrypt + Cloudflare | Yes |
| `cloudflare_ruleset` (redirect) | `kitehub.app` → `kitehub.vn` 301 | Yes |
| `cloudflare_custom_hostname` | Tenant custom-domain SaaS feature | **No — runtime** via backend adapter |
| `cloudflare_record` tenant A `{slug}.kitehub.me` | Per-tenant subdomain | **No — runtime** via backend adapter |

---

## Inputs (planned)

| Name | Type | Description |
|------|------|-------------|
| `cloudflare_api_token` | string, sensitive | Scoped to Zone:Edit + DNS:Edit for the 3 managed zones |
| `alb_ipv4` | string | Primary ALB IPv4 (origin) |
| `alb_ipv6` | string | Primary ALB IPv6 (origin) |
| `kitehub_vn_enabled` | bool | Flag — false until `.vn` registration completes |

## Outputs (planned)

| Name | Description |
|------|-------------|
| `kiteclass_com_zone_id` | Consumed by runtime adapter for per-tenant record CRUD |
| `name_servers` | For registrar NS pointer setup |

---

## Current Skeleton

`main.tf` is a minimal placeholder that declares the `cloudflare` provider and module inputs but creates no live resources. This keeps `terraform init` + CI validation green until real credentials + registration land.

---

## Enablement Steps (post-registration)

1. Provision Cloudflare API token with scoped permissions (see ADR-018 §5)
2. Store token in AWS Secrets Manager (`kitehub/cloudflare-api-token`)
3. Populate module inputs in parent `main.tf` of `terraform-aws/`
4. Run `terraform plan` — expect creation of zones + records
5. Once `kitehub.vn` zone activates in Cloudflare, update NS records at Matbao
6. Apply; verify via `dig @1.1.1.1 kitehub.vn`

---

## Related

- GAP-191 §Proposed Fix items 5–6 (Terraform + SSL automation)
- Custom-hostname runtime adapter: backend kitehub-subscription `DomainService` (planned Wave 10+)
