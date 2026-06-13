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
| **BR-DOMAIN-013** | Verify idempotent | Verify lại domain đã VERIFIED → trả current state (HTTP 200 no-op), KHÔNG 400. Verify CERT_PROVISIONING → re-poll cert issuance. Verify NONE/FAILED → IllegalArgumentException (re-initiate per BR-004) | `DomainService.verifyCustomDomain()` (GAP-1024) |
| **BR-DOMAIN-014** | Cert provisioning seam | Sau DNS verified → CERT_PROVISIONING → request cert qua `CertProvisioningService` interface. v1 Phase 1 BETA = `StubCertProvisioningService` auto-issue đồng bộ (no real CA). Real ACM/Cloudflare Custom Hostname deferred Phase 1.5+ (BR-008, ADR-018) | `CertProvisioningService` + `StubCertProvisioningService` (GAP-1024) |
| **BR-DOMAIN-015** | Timeout sweep | PENDING_VERIFY quá `timeout-hours` (đo từ `updatedAt` — last activity) → FAILED qua `@Scheduled` sweep hourly | `DomainVerificationTimeoutScheduler` (GAP-1024, BR-003) |

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

## 3.1 Implementation status (GAP-1024 — state machine completion)

State machine giờ wired đầy đủ (trước GAP-1024 nhảy thẳng `PENDING_VERIFY → VERIFIED`, không cert step, không timeout job, verify-lại-VERIFIED trả 400):

| Transition | Implemented? | Cơ chế |
|---|---|---|
| `NONE → PENDING_VERIFY` | ✅ | `DomainService.initiateCustomDomain()` (token issue) |
| `PENDING_VERIFY → CERT_PROVISIONING` | ✅ | `verifyCustomDomain()` sau DNS TXT verified (BR-005) |
| `CERT_PROVISIONING → VERIFIED` | ✅ (stub cert) | `CertProvisioningService.requestCertificate()` ISSUED → VERIFIED + `domainVerifiedAt` |
| `CERT_PROVISIONING` giữ nguyên (cert PENDING) | ✅ | Real async CA path: cert PENDING → stay CERT_PROVISIONING, re-poll khi verify lại |
| `PENDING_VERIFY → FAILED` (timeout) | ✅ | `DomainVerificationTimeoutScheduler` hourly sweep (BR-015) |
| `* → FAILED` (cert failed) | ✅ | `requestCertificate()` FAILED → FAILED |
| Verify idempotent (VERIFIED no-op) | ✅ | BR-013 |
| `FAILED → PENDING_VERIFY` (re-initiate) | ✅ | `initiateCustomDomain()` regen token (BR-004) |

**Cert provisioning — Phase 1 stub (deferred Phase 1.5+):** `StubCertProvisioningService` auto-issues synchronously → state machine reaches VERIFIED locally without a real CA. Real AWS ACM (DNS-validated) / Cloudflare-for-SaaS Custom Hostname integration deferred Phase 1.5+ (vendor dependency per BR-008 + ADR-018). VERIFIED hiện = "DNS ownership proven + cert stub issued"; gateway-route registration + real TLS termination cũng deferred Phase 1.5+ (cùng vendor scope) — nên VERIFIED ở local CHƯA route traffic thực tới instance (giới hạn môi trường + vendor scope, không phải bug chặn flow).

**Timeout đo từ `updatedAt`:** `DomainVerificationTimeoutScheduler` query `domainStatus=PENDING_VERIFY AND updatedAt < now-timeoutHours AND deleted=false`. `updatedAt` (BaseEntity `@LastModifiedDate`) = last activity → timeout là "no-recent-activity" chứ không phải strict "since-initiate" (bất kỳ instance write nào cũng bump `updatedAt`). Dedicated `domainVerifyInitiatedAt` column cho strict timeout = Phase 1.5+ refinement (cần instances-table migration per `instances-table-triad-discipline.md`).

## 4. Configuration Keys

```yaml
# application.yml (default dev)
kitehub:
  domain:
    verification:
      timeout-hours: 48     # BR-DOMAIN-003 + BR-DOMAIN-015 (timeout sweep threshold)
      mock-mode: true       # BR-DOMAIN-009

# application-production.yml (target — track via GAP-811/GAP-812 follow-up)
kitehub:
  domain:
    verification:
      timeout-hours: 48
      mock-mode: false      # BR-DOMAIN-010
```

> **Lưu ý config key:** GAP-1024 wave plan đề xuất key `kitehub.domain.verify-timeout-hours` default 72 — KHÔNG áp dụng. Giữ key hiện hành `kitehub.domain.verification.timeout-hours` default **48** (đã wired `DomainVerificationConfig`, khớp BR-003 đã documented). Đổi giá trị 48→72 sẽ cần business-logic-review (per `business-logic-review.md` §2) + đồng bộ BR-003 — defer (không trong scope GAP-1024).

## 5. Related

- **Use cases:** [`use-cases.md`](use-cases.md)
- **API contract:** [`api-contract.md`](api-contract.md)
- **Sister scope:** [`../domain-management/rules.md`](../domain-management/rules.md) (subdomain lifecycle)
- **Operations:** [`../../../05-guides/operations/custom-domain-verify-runbook.md`](../../../05-guides/operations/custom-domain-verify-runbook.md)
- **Gap:** [GAP-812](../../../04-quality/gaps/phase-1-beta/GAP-812-custom-domain-dns-ssl-completion.md)

## 6. Log

- **2026-06-13:** GAP-1024 (Wave kitehub-biz-100 Bucket BE-5) — state machine completion. Thêm BR-DOMAIN-013 (verify idempotent), BR-DOMAIN-014 (cert provisioning seam — `CertProvisioningService` interface + `StubCertProvisioningService` Phase 1 stub), BR-DOMAIN-015 (timeout sweep `DomainVerificationTimeoutScheduler`). `DomainService.verifyCustomDomain()` giờ wire `PENDING_VERIFY → CERT_PROVISIONING → VERIFIED` + idempotent (VERIFIED no-op 200, không 400) + FAILED throw (re-initiate). §3.1 implementation-status table mới. Config key giữ `kitehub.domain.verification.timeout-hours=48` (KHÔNG đổi sang `verify-timeout-hours`/72 như wave plan đề xuất — giữ khớp BR-003). Real ACM/Cloudflare cert + gateway-route deferred Phase 1.5+ (vendor dependency).
- **2026-06-01:** Doc created — Wave tenant-domain-1 Bucket D (GAP-812). Codifies BR-DOMAIN-001..012 + state machine extended với CERT_PROVISIONING state (v1.1).
