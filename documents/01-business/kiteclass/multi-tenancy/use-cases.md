---
audience: dev
---

# Multi-tenancy — Use Cases

**Domain:** kiteclass / multi-tenancy
**Layer:** Layer 2 (use-cases.md) of 3-layer business docs per CLAUDE.md §Business Logic Documents
**Owner:** Backend (security-critical) + Compliance
**Created:** 2026-06-15 (GAP-1322 — backfill missing Layer-2)
**Implements:** BR-MULTITENANT-001 (dual-layer tenant isolation)

> Multi-tenancy is a **cross-cutting** domain — it has no dedicated end-user screens or REST
> endpoints. These use cases describe the request-lifecycle flows that enforce tenant isolation
> for **every** tenant-scoped request. They are grounded in the gateway tenant resolver +
> kiteclass-core tenant interceptor + Postgres RLS (see `api-contract.md` Layer 3 + `rules.md`
> Layer 1 BR-MULTITENANT-001 code references).

---

## UC-MT-01: Resolve tenant at the gateway

**Actor:** Any client (browser / mobile / API consumer) reaching the platform via `kitehub-gateway`.
**Precondition:** An `Instance` row exists for the tenant with status `ACTIVE` or `TRIAL`.

**Steps:**
1. Client sends a request to the gateway (host like `truong-abc.kitehub.me`, or `localhost` in local dev with `X-Instance-Subdomain: truong-abc`).
2. `TenantResolverGatewayFilterFactory` resolves the tenant in order:
   1. `X-Instance-Subdomain` header (local-dev override).
   2. Subdomain extracted from the `Host` header (strip the configured base domain `.kitehub.me`).
   3. Custom-domain lookup (`InstanceRepository.findByCustomDomain(host)`).
   4. JWT `tenantId` claim fallback (GAP-711) — best-effort claim read when subdomain resolution fails (e.g. apex/`localhost`); signature is **not** verified here (that is `JwtAuthenticationGatewayFilter`'s job).
3. Gateway verifies the resolved `Instance.status ∈ {ACTIVE, TRIAL}`.
4. Gateway injects `X-Tenant-Id: <instance UUID>` and forwards the request downstream.

**Postcondition:** Downstream services receive a trusted, gateway-set `X-Tenant-Id`. Clients cannot self-assert `X-Tenant-Id` because the gateway overwrites it from the resolved instance.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | No subdomain, no custom domain, no usable JWT `tenantId` claim | "Cannot resolve tenant" |
| 404 | Subdomain has no matching `Instance` | "Instance not found" |
| 503 | Resolved instance status not `ACTIVE`/`TRIAL` (e.g. `SUSPENDED`, `EXPIRED`) | "Instance is &lt;status&gt;" |

---

## UC-MT-02: Activate per-request tenant context (code layer)

**Actor:** `kiteclass-core` handling a routed request.
**Precondition:** Request carries a gateway-injected `X-Tenant-Id` (and, for audit, `X-User-Id` / `X-User-Reference-Id`).

**Steps:**
1. `TenantFilterInterceptor.preHandle` reads `X-Tenant-Id`, parses it to a `UUID`.
2. Sets `TenantContext` (ThreadLocal) to that tenant UUID.
3. Enables the Hibernate `@FilterDef("tenantFilter")` with `tenantId` so every `BaseEntity` query is auto-filtered to the current tenant (Layer 1 of BR-MULTITENANT-001).
4. Reads `X-User-Id` (JWT `sub`, UUID) into `UserContext` for JPA auditing, and `X-User-Reference-Id` (numeric domain id) for ownership authz.
5. After the request completes, `afterCompletion` clears both ThreadLocals to prevent cross-request leakage.

**Postcondition:** Hibernate-mediated queries return only rows where `instance_id = current tenant`.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| (none) | Invalid `X-Tenant-Id` UUID format | Logged warning; tenant filter not enabled — request proceeds and fails at the DB/service layer (UC-MT-05 covers fail-closed paths) |

---

## UC-MT-03: Enforce isolation at the database layer (RLS)

**Actor:** `kiteclass-core` service method annotated `@Transactional`.
**Precondition:** `TenantContext` is set for the current thread (UC-MT-02).

**Steps:**
1. `TenantAwareDataSourceInterceptor` runs on every `@Transactional` boundary.
2. It reads `TenantContext.getCurrentTenant()` and issues `SELECT set_config('app.current_tenant_id', :tenantId, true)` — i.e. `SET LOCAL`, scoped to the transaction.
3. Postgres Row-Level Security policy `tenant_isolation USING (instance_id = current_setting('app.current_tenant_id', true)::uuid)` is `ENABLE`d + `FORCE`d on every tenant-scoped `kiteclass-core` table (V58 migration).
4. The DB rejects any row whose `instance_id` does not match the GUC — even for raw native SQL, projection DTOs, or `JdbcTemplate` queries that bypass the Hibernate filter.
5. The interceptor is idempotent across nested `@Transactional` propagation (sets the GUC once per physical transaction).

**Postcondition:** Cross-tenant reads/writes are structurally impossible at the database layer, independent of any application-code mistake (defense-in-depth Layer 2).

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | `@Transactional` entered with no `TenantContext` set | `TenantNotSetException` → `TENANT_NOT_SET` (see UC-MT-05) |

---

## UC-MT-04: Deny cross-tenant access

**Actor:** Any authenticated user whose active tenant is tenant B.
**Precondition:** A resource (e.g. a student, class, attendance row) belongs to tenant A.

**Steps:**
1. User (tenant B context) requests tenant A's resource by id.
2. Hibernate tenant filter (UC-MT-02) + RLS policy (UC-MT-03) both scope the query to tenant B.
3. The row for tenant A is invisible → repository returns empty.
4. Service maps the empty result to a `404 NOT_FOUND` (never a cross-tenant `200`).

**Postcondition:** A user can never read or mutate another tenant's data; absence is indistinguishable from "not found" (no existence oracle).

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Resource belongs to a different tenant | Standard not-found message for the resource type |

**Evidence:** `RLSEnforcementIT`, `TenantIsolationIT`.

---

## UC-MT-05: Fail-closed on tenant-scoped aggregate endpoints

**Actor:** Any client hitting a tenant-scoped aggregate path without a resolvable tenant.
**Precondition:** Request targets a path under `TENANT_REQUIRED_PATH_PREFIXES` (currently the reports prefix `/api/v1/reports/`).

**Steps:**
1. `TenantFilterInterceptor.preHandle` finds no resolvable `X-Tenant-Id`.
2. For a tenant-required prefix it **rejects early** with HTTP 400 instead of running unfiltered (which would leak cross-tenant aggregates) — GAP-1039 fail-closed.
3. The response mirrors the `TENANT_NOT_SET` error contract returned by the service-layer `GlobalExceptionHandler` so clients get a consistent shape regardless of where the request was rejected.

**Postcondition:** No aggregate query ever runs without a tenant scope.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Tenant-required path with no resolvable `X-Tenant-Id` | "Tenant context not set for current thread. Ensure X-Tenant-Id header is provided in request." (`errorCode: TENANT_NOT_SET`) |

> **Scope note:** Fail-closed is deliberately narrow (reports prefix today) so genuinely public endpoints (auth, signup, marketing landing, DSAR) are unaffected. New tenant-scoped aggregate endpoints should be added to `TENANT_REQUIRED_PATH_PREFIXES`.

---

## Related

- Layer 1: [`rules.md`](./rules.md) — BR-MULTITENANT-001 (dual-layer isolation, code references)
- Layer 3: [`api-contract.md`](./api-contract.md) — tenant header + RLS GUC contract
- Architecture: [`documents/02-architecture/kiteclass-architecture.md`](../../../02-architecture/kiteclass-architecture.md) §Multi-Tenant Isolation
- Gateway resolver: `kitehub/kitehub-gateway/.../filter/TenantResolverGatewayFilterFactory.java` (GAP-711)
- Runbook: [`documents/05-guides/operations/runbooks/rls-policy-violation.md`](../../../05-guides/operations/runbooks/rls-policy-violation.md)
- Gaps: GAP-466 (RLS defense-in-depth), GAP-711 (JWT tenant fallback), GAP-1039 (fail-closed), GAP-1322 (this Layer-2 backfill)

---

## Log

- **2026-06-15** (GAP-1322): Created Layer-2 use-cases.md (UC-MT-01..05) backfilling the missing 3-layer file for the multi-tenancy domain. Flows grounded in `TenantResolverGatewayFilterFactory`, `TenantFilterInterceptor`, `TenantAwareDataSourceInterceptor`, and V58 RLS migration. Same class as GAP-664 (kitehub preferences/email drift); this is the kiteclass branch.
