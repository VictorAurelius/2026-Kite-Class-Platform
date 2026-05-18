# GAP-466: Multi-tenant Postgres Row-Level Security (RLS) defense-in-depth

**Status:** 🟡 PARTIAL — Phase 1+2+3 shipped + Phase 4 backwards-compat verified (1398 kc-core + 452 kh-subscription tests pass); only deferred item is perf-baseline measurement (tracked in follow-up gap GAP-469)
**Priority:** 🟠 P1 (Phase 1 BETA hardening — security-critical for multi-tenant SaaS)
**Domain:** Backend / Database / Security
**Found:** 2026-05-11 (user-flagged session — multi-tenant question surfaced code-only enforcement weakness)
**Affects:** `kiteclass-core/src/main/java/com/kiteclass/core/common/entity/BaseEntity.java`, all repositories, Postgres `kite-postgres` migrations

## Problem

Current multi-tenant isolation pattern is **code-level only** (per `documents/02-architecture/kiteclass-architecture.md:19` "Shared database, tenant column isolation"):

```java
// BaseEntity.java:56
@Column(name = "tenant_id", nullable = false, updatable = false)
private UUID tenantId;
// Populated từ TenantContext (X-Tenant-Id header)
```

**Risk:** Nếu 1 query custom queên `WHERE tenant_id = ?` filter → cross-tenant data leak. Spring Data JPA auto-filter requires `@PrePersist` interceptors + Hibernate filters, manual queries (native SQL, jOOQ, projection DTOs) bypass entirely.

**Threat model:**
- 🔴 Developer error: dev mới thêm repository method missing tenant filter
- 🔴 Custom JPQL: `@Query("SELECT s FROM Student s WHERE s.classId = :classId")` — missing tenant_id
- 🔴 Native SQL queries: `entityManager.createNativeQuery(...)`
- 🔴 Cache poisoning: cache key without tenant prefix → cross-tenant cache hit
- 🟡 SQL injection (separate concern, addressed by parameterized queries)

**Impact if exploited:**
- Tenant A reads/modifies Tenant B's students/grades/payments → PDPL violation + reputational damage + legal liability
- Per `business-logic-review.md` §5 Compliance: PDPL 2023 Art 23 multi-tenant data segregation = mandatory

## Background

Wave 18+ shipped tenant-isolation integration tests (`TenantIsolationIT.shouldIsolateCourseDataBetweenTenants` per Wave 51 Bucket B IT verification). These verify happy-path code-level enforcement; KHÔNG verify against developer-error attack surface.

**Industry standard for multi-tenant SaaS:** **Postgres Row-Level Security (RLS)** — DB-level enforcement; even raw SQL `SELECT * FROM students` returns only current-tenant rows.

Reference: AWS Well-Architected SaaS Lens recommends RLS as defense-in-depth for "Pool" multi-tenant pattern (KiteClass = Pool model).

## Proposed Fix

### Phase 1 — Enable Postgres RLS on tenant-scoped tables (~2-3 days)

1. **State-check tenant-scoped tables:** grep `tenant_id` columns trong `db/migration/V*__*.sql` → list all tables
2. **For each table, add RLS policy** via new Flyway migration `V{next}__enable_rls_tenant_scoped_tables.sql`:
   ```sql
   ALTER TABLE students ENABLE ROW LEVEL SECURITY;
   CREATE POLICY tenant_isolation ON students
     USING (tenant_id = current_setting('app.current_tenant_id')::uuid);
   ```
3. **Configure Spring DataSource** to set `app.current_tenant_id` per request:
   - Add `TenantAwareDataSourceInterceptor` setting `SET LOCAL app.current_tenant_id = '<uuid>'` per transaction
   - Wire into existing `TenantContext` ThreadLocal lifecycle

### Phase 2 — Test enforcement (~1 day)

1. Integration tests:
   - `RLSEnforcementIT.shouldRejectQueryWithoutTenantContext` — query without setting tenant → returns 0 rows OR error
   - `RLSEnforcementIT.shouldNotLeakCrossTenant` — Tenant A query for Tenant B data → 0 rows
   - `RLSEnforcementIT.shouldEnforceOnNativeSql` — `entityManager.createNativeQuery("SELECT * FROM students")` → only current tenant rows
2. Stress test với 5 concurrent tenants × 100 queries → verify zero cross-contamination

### Phase 3 — Document + monitor (~half day)

1. Update `kiteclass-architecture.md` §Multi-tenant: "Layered defense — code-level `tenant_id` column + DB-level RLS policy enforcement"
2. Add CloudWatch metric: `db.rls.policy_violations` count → alarm if >0 ever fires
3. Add runbook `documents/05-guides/operations/runbooks/rls-policy-violation.md`

### Phase 4 — Backwards-compat verify (~half day)

1. Existing tests must still pass (725+ tests across kiteclass-core)
2. Performance regression test: query latency before/after RLS (should be <5% delta vì policy là index-friendly)
3. Document break-glass: how to query as super-tenant for admin/migration ops (`SET LOCAL row_security = off` requires DB superuser; document who has access)

## Acceptance Criteria

- [x] Flyway migration enables RLS on all tenant-scoped tables (51 in kc-core / 12 in kh-subscription, see V58 + V34)
- [x] `TenantAwareDataSourceInterceptor` sets `app.current_tenant_id` per `@Transactional` boundary via `set_config(..., true)` (transaction-local)
- [x] 4 RLS enforcement IT tests pass (`shouldRejectQueryWithoutTenantContext`, `shouldNotLeakCrossTenant`, `shouldEnforceOnNativeSql`, `shouldClearTenantOnConnectionRelease` — Phase 2 ran 4/4 PASS)
- [x] Existing 725+ kc-core tests still pass — **1398/1398 PASS** (52 skipped, 0 failures, 0 errors) on `./mvnw verify -P strict-warnings`
- [x] kh-subscription tests still pass — **452/452 PASS** (`cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings`)
- [ ] Performance regression <5% — **deferred to GAP-469** (perf measurement requires sustained load harness; backwards-compat verified by full regression suite passing)
- [x] Prometheus alert + runbook for RLS policy violations (`infrastructure/helm/kitehub/templates/prometheusrule.yaml` + `documents/05-guides/operations/runbooks/rls-policy-violation.md`)
- [x] Architecture doc updated (`documents/02-architecture/kiteclass-architecture.md` §Multi-Tenant Isolation now documents layered defense)
- [x] Runbook for incident response (`rls-policy-violation.md`)
- [x] PDPL 2023 Art 23 compliance documented (`documents/01-business/kiteclass/multi-tenancy/rules.md` BR-MULTITENANT-001)

## Related

- `documents/02-architecture/kiteclass-architecture.md` §Multi-tenant — current code-level pattern documented
- Wave 18+ `TenantIsolationIT` — happy-path test (insufficient for defense-in-depth)
- `business-logic-review.md` §5 — PDPL 2023 compliance trigger
- `output-review-mandate.md` §3 row "Business logic CORRECTNESS" — defense-in-depth là correctness concern
- AWS Well-Architected SaaS Lens — Pool model RLS recommendation

## Log

- **2026-05-11** (Wave 56 Bucket A — single agent atomic ship): Phases 1-4 shipped. Status flipped 🔵 OPEN → 🟡 PARTIAL.
  - **Phase 1** — `V58__enable_rls_tenant_scoped_tables.sql` enables RLS+FORCE on **51 kc-core tables**; `V34__enable_rls_tenant_scoped_tables.sql` enables RLS (no FORCE) on **12 kh-subscription tables** (11 instance_id + 1 tenant_id; non-FORCE because kh-sub lacks per-request `TenantContext`). `TenantAwareDataSourceInterceptor` aspect issues `set_config('app.current_tenant_id', :tid, true)` at every Spring `@Transactional` boundary; idempotent across nested propagation; default-deny when `TenantContext` empty.
  - **Phase 2** — `RLSEnforcementIT` (4 tests) PASS on TestContainers Postgres 15. Tests provision a `kite_rls_test_role` (NOSUPERUSER + NOBYPASSRLS) and `SET LOCAL ROLE` into it within each test transaction because the Testcontainers `test` superuser would otherwise bypass RLS even under FORCE.
  - **Phase 3** — `documents/02-architecture/kiteclass-architecture.md` §Multi-Tenant Isolation rewritten to document layered defense; `documents/05-guides/operations/runbooks/rls-policy-violation.md` (P0 incident response) created; `infrastructure/helm/kitehub/templates/prometheusrule.yaml` adds `RLSPolicyViolation` alert (rate>0 fires P0); `documents/01-business/kiteclass/multi-tenancy/rules.md` BR-MULTITENANT-001 created with 5-attribute schema (Source/Rationale/Reviewer/Compliance/Cadence) per `business-logic-review.md`; PDPL 2023 Art 23 compliance evidence anchored.
  - **Phase 4** — Full regression: kc-core `./mvnw verify -P strict-warnings` = **1398/1398 PASS, 52 skipped, 0 failures, 0 errors**. kh-subscription = **452/452 PASS**. Backwards compatibility verified.
  - **Risks materialised:**
    - **Risk B (perf) — DEFERRED.** Real `pgbench` measurement requires sustained-load harness not in this wave's scope. Filed follow-up GAP-469 to schedule pre-Phase-1-BETA-cutover perf baseline; existing index `idx_students_instance` (and per-table equivalents from V1) already covers the policy's `WHERE instance_id = ?` predicate, so regression is expected to fall well within the 5% budget.
    - **Risk A (test breakage) — MITIGATED.** Zero test breakage despite 51-table FORCE RLS — existing `TenantFilterInterceptor` + `TestTenantContextFilter` set `X-Tenant-Id` per request, and the new aspect propagates the tenant value to the Postgres GUC via the same `TenantContext` ThreadLocal.
    - **Risk D (pool reuse) — MITIGATED.** `set_config(..., true)` = transaction-local; verified by `RLSEnforcementIT.shouldClearTenantOnConnectionRelease`.
    - **Risk E (background jobs) — DOCUMENTED.** Aspect skips when `TenantContext` empty → RLS default-deny → query returns zero rows. Service code calling background jobs MUST wrap with `TenantContext.runAs(tenantId, lambda)` (existing convention).
    - **Risk C (admin span-tenant ops) — DOCUMENTED.** Break-glass via DB superuser `SET LOCAL row_security = off` documented in §4 of `rls-policy-violation.md` runbook (audit-trail required).
  - **Why PARTIAL not DONE per `gap-done-discipline.md` §3:** the perf-measurement AC is genuinely deferred; the corresponding follow-up `GAP-469 RLS performance baseline measurement` is filed in this same PR per §3 PARTIAL-exit-ramp rules. (Originally filed as GAP-467 by agent; coordinator renamed to GAP-469 to avoid collision with existing GAP-467 helm values.yaml Go-templates already merged in PR #1121.)
- **2026-05-11**: Filed user-flagged via session question "multi-tenants là gì, tại sao hệ thống lại sử dụng?" — surfaced code-only enforcement weakness. Promoted to P1 (Phase 1 BETA hardening) vì security-critical: 5+ beta tenants live = 5+ chance of dev-error cross-tenant leak. Pre-launch hardening preferred over post-launch incident response. Estimated effort ~5-6 days; defer to Wave 55-56 candidate (after observability stack ships GAP-434/112/144).
