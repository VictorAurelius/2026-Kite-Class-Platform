---
audience: dev
---

# GAP-790 — Gateway `/api/v1/staff-invitations/**` route missing TenantResolver filter

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Gateway
**Found:** 2026-05-28 (Wave Phase 2 Beta Wave A Bucket C GAP-783 re-walk surfaced sister bug)
**Phase:** phase-1-beta

## Problem

POST/GET/PUT `/api/v1/staff-invitations/**` through gateway (port 9000) → **HTTP 403 `TENANT_CONTEXT_MISSING` "X-Tenant-Id header missing"** when client (FE / browser localhost) does NOT manually attach `X-Tenant-Id` header.

Empirical reproduction 2026-05-28T09:56Z với Owner JWT (`owner.test@test.vn` / tenant_id `877dff9d-c354-4faf-8c44-3c17196dbf24`):

```bash
# WITHOUT X-Tenant-Id → 403 TENANT_CONTEXT_MISSING
curl -X POST http://localhost:9000/api/v1/staff-invitations \
  -H "Authorization: Bearer <Owner JWT with tenantId claim>" \
  -H 'Content-Type: application/json' \
  -d '{"email":"...","fullName":"...","role":"TEACHER"}'
# → 403 {"error":"TENANT_CONTEXT_MISSING","detail":"X-Tenant-Id header missing"}

# WITH explicit X-Tenant-Id → HTTP 201 ✓
curl -X POST http://localhost:9000/api/v1/staff-invitations \
  -H "Authorization: Bearer <Owner JWT>" \
  -H "X-Tenant-Id: 877dff9d-c354-4faf-8c44-3c17196dbf24" \
  -H 'Content-Type: application/json' \
  -d '{...}'
# → 201 Created
```

## Root Cause

Gateway route `staff-invitations` (defined `kitehub/kitehub-gateway/src/main/resources/application.yml` line 578-586) routes `/api/v1/staff-invitations/**` → `kitehub-subscription:8080` with **only** `CircuitBreaker` filter applied. Missing the `TenantResolver` filter that other tenant-scoped routes have.

Compare with `instance-apis` route (line 603-608) which DOES have `- TenantResolver` filter:

```yaml
- id: instance-apis
  uri: ${KITECLASS_CORE_URL:http://kiteclass-core:8080}
  predicates:
    - Path=/api/v1/**
  filters:
    - TenantResolver       # ← MISSING from staff-invitations route
    - name: CircuitBreaker
      ...
```

Since `staff-invitations` Path predicate is more specific than `/api/v1/**`, it matches FIRST → bypasses TenantResolver entirely.

`TenantResolverGatewayFilterFactory` already supports the unblock path needed: §79-98 documents "JWT tenantId claim fallback (GAP-711 — Wave 105 Bucket E fix)" which resolves tenant from JWT claim when subdomain-based resolution fails (e.g., `localhost` access). Adding `- TenantResolver` to the `staff-invitations` route activates this fallback automatically since Wave 104 Bucket A enriches Owner JWT with `tenantId` claim (GAP-704 DONE).

**Bug class:** sister to Bug #21 (FE missing X-Tenant-Id header) — both manifest as "tenant-scoped endpoint missing X-Tenant-Id resolution", different layer (FE vs gateway routing). Per `cross-flow-bug-class-sweep.md` §1.

## Proposed Fix

Single 1-line addition to `kitehub/kitehub-gateway/src/main/resources/application.yml` `staff-invitations` route filters list:

```yaml
- id: staff-invitations
  uri: http://kitehub-subscription:8080
  predicates:
    - Path=/api/v1/staff-invitations/**
  filters:
    - TenantResolver       # ← ADD
    - name: CircuitBreaker
      args:
        name: subscriptionCircuitBreaker
        fallbackUri: forward:/fallback/subscription
```

Smoke test post-fix:
```bash
JWT=$(curl -s -X POST http://localhost:9000/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | grep -oP '"accessToken":"\K[^"]+')
curl -X POST http://localhost:9000/api/v1/staff-invitations \
  -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"email":"staff+verify@test.vn","fullName":"Staff Verify","role":"TEACHER"}'
# Expected: HTTP 201 (without manual X-Tenant-Id header)
```

## Acceptance Criteria

- [ ] `staff-invitations` route in gateway `application.yml` includes `- TenantResolver` filter
- [ ] Smoke test (curl above) returns 201 WITHOUT manual X-Tenant-Id header
- [ ] FE Owner walk POST `/api/v1/staff-invitations` from browser localhost succeeds
- [ ] Sister routes audit per `cross-flow-bug-class-sweep.md` §3: enumerate other tenant-scoped routes lacking TenantResolver filter

## Sweep candidate routes (per cross-flow-bug-class-sweep.md §3)

Other gateway routes that may have same class signature (tenant-scoped endpoints without TenantResolver filter). To verify in fix PR:
- `kitehub-onboarding-progress` (line 592)
- Other kitehub-* specific routes that precede `/api/v1/**` catch-all

If sister routes need fix too → batch in same PR per `feature-ship-runtime-walk-mandate.md` §3.4 catalog-then-batch protocol.

## Related

- **Sister gap (closed):** GAP-783 (this gap surfaced during GAP-783 verify re-walk; bug class re-classified)
- **Parent ship:** GAP-704 (Wave 104 Bucket A) — JWT tenantId claim enables TenantResolver's JWT fallback path used by this fix
- **Sister fix precedent:** GAP-711 Wave 105 Bucket E — TenantResolver JWT claim fallback
- **Sister class bug:** Bug #21 Wave A Bucket B (FE missing X-Tenant-Id) — same class signature, FE-layer side
- **Rules:** `cross-flow-bug-class-sweep.md` §1, `pre-handoff-self-test-completeness.md` §3, `audit-to-gap-pipeline.md` §2.8

## Log

- **2026-05-28** — Found Wave Phase 2 Beta Wave A Bucket C while empirically verifying GAP-783 + GAP-704 DONE state. GAP-783 chain healthy; this is a separable concern at gateway routing layer. Filed P1 (not P0) because workaround exists (FE can manually attach X-Tenant-Id), but should fix to unblock anonymous-style flows that depend on JWT claim resolution.
