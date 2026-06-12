# Custom Domain — Business Rules

**Last verified:** 2026-06-01
**Created:** Wave tenant-domain-1 Bucket D (GAP-812)
**Sister doc:** `domain-management/` covers subdomain (kitehub.me) lifecycle; doc này focus on Custom Domain DNS verify + SSL provisioning (Premium/Enterprise tier).
**Config prefix:** `kitehub.domain.verification`
**Architecture:** [ADR-018 Domain Registrar / DNS / TLD](../../../02-architecture/adr/ADR-018-domain-registrar-dns.md)
**Runbook:** [Custom Domain Verify Runbook](../../../05-guides/operations/custom-domain-verify-runbook.md)

## 1. Scope

Custom Domain feature cho phép tenant (Center Owner) gắn domain riêng (vd `lop.skyedu.vn`) vào instance KiteClass — thay vì chỉ dùng subdomain `{subdomain}.kitehub.me`. Backup URL subdomain LUÔN hoạt động song song.

**Tier restriction:** PREMIUM + ENTERPRISE only (per `Instance.canUseCustomDomain()`).

## 2. Business Rules (BR-DOMAIN-*)

| ID | Rule | Value | Config Key / Code |
|----|------|-------|-------------------|
| **BR-DOMAIN-001** | Token uniqueness | Mỗi instance có 1 token verify unique; format `kitehub-verify={uuid}` | `DomainService.initiateCustomDomain()` |
| **BR-DOMAIN-002** | Status transitions | NONE → PENDING_VERIFY → CERT_PROVISIONING → VERIFIED; FAILED reachable từ mọi non-terminal state | `Instance.DomainStatus` enum |
| **BR-DOMAIN-003** | Verify timeout | 48 giờ mặc định (PENDING_VERIFY → FAILED nếu TXT không xuất hiện) | `kitehub.domain.verification.timeout-hours=48` |
| **BR-DOMAIN-004** | Re-verify allowed | FAILED → PENDING_VERIFY khi tenant trigger re-verify (regenerate token) | `DomainService.initiateCustomDomain()` (same instance ownership) |
| **BR-DOMAIN-005** | TXT record convention | Preferred: `_kitehub-verify.{domain}` TXT = `kitehub-verify={token}`. Fallback apex: `{domain}` TXT chứa token | `DnsTxtLookupService.verifyTxtRecord()` |
| **BR-DOMAIN-006** | Domain uniqueness | 1 custom domain per platform; cùng instance được re-initiate | `InstanceRepository.findByCustomDomainAndDeletedFalse()` |
| **BR-DOMAIN-007** | Backup URL always available | `https://{subdomain}.kitehub.me` hoạt động trong lúc cert provision + sau khi VERIFIED (parallel routing) | `DomainService.buildResponse()` |
| **BR-DOMAIN-008** | SSL provider | v1 deferred: Cloudflare for SaaS preferred; AWS ACM fallback (terraform scaffold) | ADR-018 + `acm-tenant-domains.tf` |
| **BR-DOMAIN-009** | Mock mode (dev) | `mockMode=true` (default dev/test profile) — DNS không resolvable → giữ PENDING (không FAILED) | `DomainVerificationConfig.mockMode` |
| **BR-DOMAIN-010** | Mock mode (prod) | `mockMode=false` ở `application-production.yml` — DNS lookup real qua JNDI; record vắng → vẫn PENDING (chờ timeout job) | Same |
| **BR-DOMAIN-011** | Tier check | Tier PREMIUM/ENTERPRISE only — throw IllegalArgumentException nếu thấp hơn | `Instance.canUseCustomDomain()` → `PricingTier.allowsCustomDomain()` |
| **BR-DOMAIN-012** | Tenant-initiated removal | Tenant gỡ custom domain → status reset NONE, fallback subdomain | `DomainService.removeCustomDomain()` |

## 3. State Machine

```
                  ┌──────────────┐
                  │     NONE     │  (default)
                  └──────┬───────┘
                         │ initiateCustomDomain(domain)
                         │   - tier check (BR-011)
                         │   - uniqueness check (BR-006)
                         │   - token issue (BR-001)
                         ▼
                  ┌────────────────────┐
        ┌─────────│  PENDING_VERIFY    │◄────────┐
        │         └─────────┬──────────┘         │
        │                   │ verifyCustomDomain │
        │                   │   - DNS TXT lookup │ re-verify
        │                   │   - match? (BR-005)│ (BR-004)
        │ timeout 48h       ▼                    │
        │         ┌────────────────────┐         │
        │         │ CERT_PROVISIONING  │ (v1.1+) │
        │         └─────────┬──────────┘         │
        │                   │ cert active        │
        │                   ▼                    │
        │         ┌────────────────────┐         │
        │         │      VERIFIED      │         │
        │         └─────────┬──────────┘         │
        │                   │ removeCustomDomain │
        │                   ▼                    │
        │         ┌──────────────┐               │
        │         │     NONE     │               │
        │         └──────────────┘               │
        ▼                                        │
  ┌────────────┐                                 │
  │   FAILED   │─────────────────────────────────┘
  └────────────┘   re-initiate (regenerate token)
```

## 4. Configuration Keys

```yaml
# application.yml (default dev)
kitehub:
  domain:
    verification:
      timeout-hours: 48     # BR-DOMAIN-003
      mock-mode: true       # BR-DOMAIN-009

# application-production.yml (target — track via GAP-811/GAP-812 follow-up)
kitehub:
  domain:
    verification:
      timeout-hours: 48
      mock-mode: false      # BR-DOMAIN-010
```

## 5. Related

- **Use cases:** [`use-cases.md`](use-cases.md)
- **API contract:** [`api-contract.md`](api-contract.md)
- **Sister scope:** [`../domain-management/rules.md`](../domain-management/rules.md) (subdomain lifecycle)
- **Operations:** [`../../../05-guides/operations/custom-domain-verify-runbook.md`](../../../05-guides/operations/custom-domain-verify-runbook.md)
- **Gap:** [GAP-812](../../../04-quality/gaps/phase-1-beta/GAP-812-custom-domain-dns-ssl-completion.md)

## 6. Log

- **2026-06-01:** Doc created — Wave tenant-domain-1 Bucket D (GAP-812). Codifies BR-DOMAIN-001..012 + state machine extended với CERT_PROVISIONING state (v1.1).
