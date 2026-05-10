# GAP-466: Multi-tenant Postgres Row-Level Security (RLS) defense-in-depth

**Status:** 🔵 OPEN
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

- [ ] Flyway migration enables RLS on all tenant-scoped tables (grep `tenant_id` column)
- [ ] `TenantAwareDataSourceInterceptor` sets `app.current_tenant_id` per request
- [ ] 3 RLS enforcement IT tests pass (without-context / cross-tenant-leak / native-SQL)
- [ ] Existing 725+ tests still pass
- [ ] Performance regression <5% (measure with `pgbench` or similar)
- [ ] CloudWatch metric + alarm for RLS policy violations
- [ ] Architecture doc updated (kiteclass-architecture.md §Multi-tenant)
- [ ] Runbook for incident response (rls-policy-violation.md)
- [ ] PDPL 2023 Art 23 compliance documented (data segregation evidence)

## Related

- `documents/02-architecture/kiteclass-architecture.md` §Multi-tenant — current code-level pattern documented
- Wave 18+ `TenantIsolationIT` — happy-path test (insufficient for defense-in-depth)
- `business-logic-review.md` §5 — PDPL 2023 compliance trigger
- `output-review-mandate.md` §3 row "Business logic CORRECTNESS" — defense-in-depth là correctness concern
- AWS Well-Architected SaaS Lens — Pool model RLS recommendation

## Log

- **2026-05-11**: Filed user-flagged via session question "multi-tenants là gì, tại sao hệ thống lại sử dụng?" — surfaced code-only enforcement weakness. Promoted to P1 (Phase 1 BETA hardening) vì security-critical: 5+ beta tenants live = 5+ chance of dev-error cross-tenant leak. Pre-launch hardening preferred over post-launch incident response. Estimated effort ~5-6 days; defer to Wave 55-56 candidate (after observability stack ships GAP-434/112/144).
