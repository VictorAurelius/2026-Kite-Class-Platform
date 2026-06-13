# Domain Management — Business Rules

**Last verified:** 2026-04-21
**Config prefix:** `kitehub.domain.verification`
**Architecture:** [ADR-018 Domain Registrar / DNS / TLD](../../../02-architecture/adr/ADR-018-domain-registrar-dns.md)
**Runbook:** [DNS Operations](../../../05-guides/infrastructure/dns-operations.md)

## Custom-Domain Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| DOM-01 | Tier restriction | PREMIUM và ENTERPRISE only | canUseCustomDomain() |
| DOM-02 | Token format | kitehub-verify={random-uuid} | DomainService.initiateCustomDomain() |
| DOM-03 | Verification timeout | 48 giờ | `kitehub.domain.verification.timeout-hours` |
| DOM-04 | Mock mode default | true (DNS không check) | `kitehub.domain.verification.mock-mode` |
| DOM-05 | Domain uniqueness | 1 custom domain per instance toàn platform | findByCustomDomainAndDeletedFalse() |
| DOM-06 | Re-initiation allowed | Cùng instance có thể re-initiate domain | ownership check |
| DOM-07 | Backup URL | https://{subdomain}.kitehub.me | buildResponse() |
| DOM-08 | DNS record instruction | TXT: @ {token} hoặc _kitehub-verify.{domain} | buildResponse() |
| DOM-09 | Mock mode behavior | DNS not resolvable → PENDING (không FAILED) | verifyCustomDomain() |
| DOM-10 | Production mode | DNS not found → stays PENDING_VERIFY | verifyCustomDomain() |
| DOM-11 | SSL issuance (new) | Cloudflare Custom Hostnames issues Let's Encrypt cert post-verify | `kitehub.domain.ssl.provider=cloudflare` |
| DOM-12 | Max custom domains per instance | 1 active (DOM-05 enforces) | DomainService |
| DOM-13 | Tenant-initiated removal | Tenant có thể gỡ custom domain — fallback về subdomain (DOM-07) | DomainService.removeCustomDomain() |

## Domain Status States

```
NONE → PENDING_VERIFY → CERT_PROVISIONING → VERIFIED
              ↓
           FAILED (timeout sweep → implemented GAP-1024)
```

> **GAP-1024 (2026-06-13):** state machine hoàn chỉnh — `PENDING_VERIFY → CERT_PROVISIONING → VERIFIED` wired (`DomainService` + `CertProvisioningService` stub) + timeout sweep `DomainVerificationTimeoutScheduler` (PENDING_VERIFY quá `kitehub.domain.verification.timeout-hours`=48 → FAILED) + verify idempotent (VERIFIED no-op). Spec + BR đầy đủ ở sister doc [`../custom-domain/rules.md`](../custom-domain/rules.md) §2 BR-DOMAIN-013/014/015 + §3.1.

## Custom-Domain Verification Flow (state machine spec — implementation deferred)

```
         ┌──────────────┐
         │     NONE     │
         └──────┬───────┘
                │ initiateCustomDomain(domain)
                │ - validate TLD not in banned list
                │ - check uniqueness (DOM-05)
                │ - create verification token
                ▼
       ┌─────────────────┐
       │ PENDING_VERIFY  │ ◄─── check-dns polling (Cloudflare resolver)
       └─────┬──────┬────┘      every 5 min, first 1h
             │      │           every 30 min, hours 1–48
   DNS TXT   │      │ 48h timeout
   matches   │      ▼
             │    ┌──────────┐
             │    │  FAILED  │ (tenant re-initiates)
             │    └─────┬────┘
             │          │ initiateCustomDomain (retry)
             │          ▼
             │   PENDING_VERIFY (loops back)
             ▼
       ┌──────────────┐
       │   VERIFIED   │
       │              │ trigger: Cloudflare Custom Hostname creation
       │              │ → SSL cert issued via Let's Encrypt (~15 min)
       └──────┬───────┘
              │ tenant removes OR billing downgrade
              ▼
          NONE (subdomain fallback DOM-07)
```

**Implementation notes (Wave 10+):**
- Verification polling via scheduled Spring `@Scheduled` job; DNS lookup via Cloudflare 1.1.1.1 resolver API (avoid caching quirks)
- Cloudflare API call happens once on VERIFIED transition; idempotent by design
- SSL cert renewal is Cloudflare's responsibility — we only track hostname status via their webhook

## Subdomain Policy (kitehub.me)

### Reserved prefixes (cannot be assigned as tenant slug)

The following first-level labels are **reserved** on `*.kitehub.me` and MUST reject tenant slug requests:

| Category | Reserved labels |
|----------|----------------|
| Platform ops | `api`, `app`, `www`, `admin`, `auth`, `gateway`, `cdn` |
| Content / marketing | `blog`, `docs`, `help`, `support`, `status`, `press`, `about` |
| Infrastructure | `mail`, `smtp`, `ns`, `mx`, `ftp`, `webmail`, `autoconfig`, `autodiscover` |
| Environment | `dev`, `staging`, `test`, `qa`, `preview`, `beta`, `alpha`, `demo` |
| Internal product | `hub`, `class`, `core`, `branding`, `billing`, `subscription` |
| Legal / security | `legal`, `security`, `abuse`, `dmca`, `privacy` |
| Brand protection | `kite`, `kitehub`, `kiteclass`, `kite-hub`, `kite-class` |

**Rule:** reserved list synced into backend config `kitehub.domain.reserved-slugs` — must be queryable without code deploy.

### Slug generation rules

| # | Rule |
|---|------|
| SLG-01 | Lowercase ASCII only (`a–z`, `0–9`, `-`) |
| SLG-02 | Length: 3–32 characters |
| SLG-03 | Must start with a letter (`a–z`) |
| SLG-04 | No consecutive hyphens (`--`) |
| SLG-05 | No trailing hyphen |
| SLG-06 | Regex: `^[a-z][a-z0-9]*(-[a-z0-9]+)*$` |
| SLG-07 | Not in reserved list (case-insensitive match) |
| SLG-08 | Not homoglyph of reserved (e.g., `adm1n` — normalize zero/one swaps before compare) |
| SLG-09 | Uniqueness scoped globally per TLD (no two tenants share a slug on `.kitehub.me`) |
| SLG-10 | Immutable once provisioned — tenants wanting different slug use custom-domain flow |

## Config

```yaml
kitehub:
  domain:
    verification:
      timeout-hours: 48
      mock-mode: ${DOMAIN_VERIFICATION_MOCK:true}
      poll-cloudflare-resolver: ${DOMAIN_VERIFY_USE_CLOUDFLARE:false}
    ssl:
      provider: cloudflare                  # per ADR-018 §4
      custom-hostname-plan: business        # Cloudflare plan for SaaS custom hostnames
    reserved-slugs:
      source: classpath:reserved-slugs.txt  # sync to frontend via public JSON for form validation
      case-insensitive: true
```

## Related

- ADR: [ADR-018 — Domain Registrar / DNS / TLD](../../../02-architecture/adr/ADR-018-domain-registrar-dns.md) (this rules.md is its operational enforcement)
- Runbook: [`documents/05-guides/infrastructure/dns-operations.md`](../../../05-guides/infrastructure/dns-operations.md)
- Gap: GAP-191 (closed by this update + ADR-018)
- Use-cases: `use-cases.md` (same folder)
- API contract: `api-contract.md` (same folder)

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — Luật An ninh mạng 2018 Art 26 (DNS-level security); PDPL (custom-domain may carry tenant PII in subdomain).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Custom-domain feature expansion, DNS provider swap.

## Log

- **2026-06-13** GAP-1024 (Wave kitehub-biz-100 Bucket BE-5) — domain verification state machine completion: CERT_PROVISIONING step + timeout sweep scheduler (DOM-03 timeout giờ enforced) + idempotent verify. Canonical spec + BR ở sister doc `../custom-domain/rules.md`. DOM-03 timeout value đo từ `updatedAt`; key `kitehub.domain.verification.timeout-hours` (note: custom-domain doc dùng 48, domain-management doc DOM-03 ghi 48 — đồng nhất).
- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
