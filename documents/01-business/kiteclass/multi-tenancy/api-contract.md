---
audience: dev
---

# Multi-tenancy — API Contract

**Domain:** kiteclass / multi-tenancy
**Layer:** Layer 3 (api-contract.md) of 3-layer business docs per CLAUDE.md §Business Logic Documents
**Owner:** Backend (security-critical) + Compliance
**Created:** 2026-06-15 (GAP-1322 — backfill missing Layer-3)
**Covers use cases:** UC-MT-01 → UC-MT-05 (see [`use-cases.md`](./use-cases.md))

> Multi-tenancy is a **cross-cutting** contract, not a set of REST endpoints. It is enforced for
> **every** tenant-scoped request by (1) the gateway tenant resolver, (2) the kiteclass-core
> request interceptor, and (3) Postgres Row-Level Security. This file documents the **header
> contract** + **error contract** + **RLS GUC contract** that all downstream APIs inherit.

---

## 1. Header contract

| Header | Direction | Type | Set by | Purpose |
|--------|-----------|------|--------|---------|
| `X-Instance-Subdomain` | client → gateway | string | Client (local dev only) | Local-dev override for subdomain resolution when `Host` is `localhost` |
| `X-Tenant-Id` | gateway → service | UUID (`Instance.id`) | **Gateway** (`TenantResolverGatewayFilterFactory`) — overwrites any client-supplied value | Active tenant for the request; drives Hibernate tenant filter + RLS GUC |
| `X-User-Id` | gateway → service | UUID (JWT `sub`) | Gateway (from verified JWT) | JPA auditing (`created_by` / `updated_by`) |
| `X-User-Reference-Id` | gateway → service | numeric (`users.reference_id`) | Gateway | Ownership authz (= `parents.id` / `teachers.id` / `students.id`); nullable for admin/owner |

**Trust boundary:** `X-Tenant-Id` is authoritative **only** because the gateway resolves it from the `Instance` and injects it; any client-supplied `X-Tenant-Id` reaching the gateway is replaced. Downstream services MUST treat `X-Tenant-Id` as trusted input from the gateway, never from the public edge.

---

## 2. Tenant resolution order (gateway)

`TenantResolverGatewayFilterFactory` resolves the tenant in this precedence (UC-MT-01):

1. `X-Instance-Subdomain` request header (local dev).
2. Subdomain from `Host` (strip configured base domain, default `.kitehub.me`).
3. Custom domain → `InstanceRepository.findByCustomDomain(host)`.
4. JWT `tenantId` claim fallback (GAP-711) — best-effort claim read; signature verified separately by `JwtAuthenticationGatewayFilter`.

On success the gateway verifies `Instance.status ∈ {ACTIVE, TRIAL}` and injects `X-Tenant-Id`.

---

## 3. RLS GUC contract (database layer)

Per BR-MULTITENANT-001 Layer 2 (UC-MT-03):

| Aspect | Contract |
|--------|----------|
| GUC name | `app.current_tenant_id` |
| Set how | `SELECT set_config('app.current_tenant_id', :tenantId, true)` (`SET LOCAL`, transaction-scoped) at every `@Transactional` boundary via `TenantAwareDataSourceInterceptor` |
| Policy | `tenant_isolation USING (instance_id = current_setting('app.current_tenant_id', true)::uuid)` |
| Scope | `ENABLE` + `FORCE ROW LEVEL SECURITY` on every tenant-scoped `kiteclass-core` table (V58 migration). `kitehub-subscription` is `ENABLE`-only (V34) until it carries per-request tenant context |
| Bypass resistance | Applies even to native SQL / projection DTO / `JdbcTemplate` queries that skip the Hibernate filter |

---

## 4. Error contract

| Code | errorCode | Where | Condition |
|------|-----------|-------|-----------|
| 400 | (gateway) | Gateway | Tenant cannot be resolved from subdomain / custom domain / JWT claim — "Cannot resolve tenant" |
| 404 | (gateway) | Gateway | Subdomain has no matching `Instance` — "Instance not found" |
| 503 | (gateway) | Gateway | Resolved instance status not `ACTIVE`/`TRIAL` (e.g. `SUSPENDED`) — "Instance is &lt;status&gt;" |
| 400 | `TENANT_NOT_SET` | kiteclass-core | Tenant-required aggregate path (e.g. the reports prefix `/api/v1/reports/`) reached with no resolvable `X-Tenant-Id` — fail-closed (GAP-1039), or any `@Transactional` method entered with no `TenantContext` |
| 404 | resource not-found | kiteclass-core | Cross-tenant resource access — row invisible under tenant scope (UC-MT-04); never leaked as a cross-tenant `200` |

`TENANT_NOT_SET` body shape (mirrors `GlobalExceptionHandler.handleTenantNotSet`):

```json
{
  "errorCode": "TENANT_NOT_SET",
  "message": "Tenant context not set for current thread. Ensure X-Tenant-Id header is provided in request.",
  "path": "/api/v1/reports/..."
}
```

---

## 5. Fail-closed path prefixes

Tenant-required prefixes are rejected early (HTTP 400 `TENANT_NOT_SET`) when no tenant is resolvable, instead of running unfiltered:

| Prefix | Rationale |
|--------|-----------|
| `/api/v1/reports/` | Aggregate/report endpoints would leak cross-tenant aggregates if run unfiltered (GAP-1039) |

> Deliberately narrow — public endpoints (auth, signup, marketing landing, DSAR) are unaffected. Add new tenant-scoped aggregate prefixes to `TenantFilterInterceptor.TENANT_REQUIRED_PATH_PREFIXES`.

---

## 6. Code references

- Gateway resolver: `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/TenantResolverGatewayFilterFactory.java`
- Request interceptor: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/config/TenantFilterInterceptor.java`
- Thread context: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/context/TenantContext.java`
- RLS GUC interceptor: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/datasource/TenantAwareDataSourceInterceptor.java`
- RLS migration: `kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql`

---

## Related

- Layer 1: [`rules.md`](./rules.md) — BR-MULTITENANT-001
- Layer 2: [`use-cases.md`](./use-cases.md) — UC-MT-01 → UC-MT-05
- Gaps: GAP-466, GAP-711, GAP-1039, GAP-1322
- Sibling class: GAP-664 (kitehub preferences/email 3-layer drift)

---

## Log

- **2026-06-15** (GAP-1322): Created Layer-3 api-contract.md backfilling the missing 3-layer file. Documents the header contract (`X-Tenant-Id` / `X-Instance-Subdomain` / `X-User-Id` / `X-User-Reference-Id`), gateway resolution order, RLS GUC contract, and error contract — grounded in gateway + kiteclass-core code. Multi-tenancy is cross-cutting (no dedicated REST endpoints); this contract is inherited by all tenant-scoped APIs.
