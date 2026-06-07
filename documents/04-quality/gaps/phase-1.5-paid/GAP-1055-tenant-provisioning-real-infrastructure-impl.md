# GAP-1055: TenantProvisioningSaga.provisionInfrastructure real implementation (branded-frontend infra)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-07 (Wave p0-prov-1 closure — scope-split from GAP-946)
**Affects:** `kiteclass-core` — `TenantProvisioningSaga.provisionInfrastructure` branded-frontend infrastructure layer

## Problem

`TenantProvisioningSaga.provisionInfrastructure` (kiteclass-core:136-139) is a log-only stub. The real branded-frontend per-tenant infrastructure — per-tenant DB schema / MinIO bucket / DNS record for the tenant's branded frontend instance — plus the async FAILED state-machine + retry for that infra layer are not implemented.

Acceptable for Phase 1 BETA: the real tenant DB is provisioned by the **subscription-side** `DatabaseProvisioningService.provisionDatabase()` (lifecycleEnabled prod, `application-production.yml:57` — physical DB + Flyway migration + `databaseUrl` "pending"→real), which is a separate layer from the saga's branded-frontend instance. So Phase 1 BETA tenants get a working DB; the saga's `provisionInfrastructure` only governs the branded-frontend (Next.js per-tenant) provisioning which Phase 1.5 paid multi-tenant frontend isolation requires.

Scope-split from GAP-946 (Wave p0-prov-1 closure 2026-06-07): GAP-946 closed for Phase 1 BETA scope (defensive `assertDatabaseProvisioned()` fail-loud + subscription-side DB provisioning verified); the real branded-frontend infra implementation deferred here.

## Proposed Fix

Implement `TenantProvisioningSaga.provisionInfrastructure` for the branded-frontend instance layer: provision per-tenant Next.js frontend resources (DB schema / MinIO bucket / DNS record per tenant) + wire the async FAILED state-machine + retry on that infra leg (compensate → `markFailed` → admin-driven retry, reusing the existing `GAP-952`/`ProvisioningStuckSweep` alarm path).

## Acceptance Criteria

- [ ] `TenantProvisioningSaga.provisionInfrastructure` provisions real branded-frontend infra (per-tenant DB schema / MinIO bucket / DNS) — no longer log-only stub
- [ ] Async FAILED state-machine + retry wired for the branded-frontend infra leg (compensate → markFailed → admin retry)
- [ ] Live walk on Phase 1.5 stack: provision a tenant → branded-frontend resources created + `FrontendInstance.status` DEPLOYED with verified infra (not just status flip)

## Related

- Scope-split from: GAP-946 (Phase 1 BETA scope DONE 2026-06-07; real branded-frontend infra deferred here)
- Sister: GAP-945 (saga wiring DONE — saga reachable, this is the infra-execution delta)
- Discovered in: Wave p0-prov-1 closure 2026-06-07
