---
title: Pre-Mutation Audit — Wave 85 Bucket B RLS hardening (V59/V60 kc-core + V50 kh-subscription)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 85
gaps: [GAP-466, GAP-577-prep]
---

# Pre-Mutation Audit — Wave 85 Bucket B RLS Policies + Admin-Bypass + HikariCP Reset + NULL Force-Fail + Immutable admin_audit_logs

Per `.claude/rules/pre-mutation-state-check.md` §3. Database schema changes = production-critical mutation; no terraform but Flyway DDL applies to RDS.

## Scope

Wave 85 Bucket B: ship 8 P0/P1 AC enhancements ON TOP OF existing RLS infrastructure (V58 kc-core + V34 kh-subscription đã ship Wave 56). KHÔNG tạo redundant V50-V52 migrations cho 12 tables vì:

1. **Existing state:** V58 kc-core đã ENABLE + FORCE RLS trên 50+ tables (`students`, `classes`, `attendance`, `grades`, `payments`, `invoices`, `audit_log`, …) với policy `USING (instance_id = NULLIF(current_setting('app.current_tenant_id', true), '''')::uuid)`. V34 kh-subscription đã ENABLE RLS (non-forced) trên 12 tables.
2. **Plan tables không tồn tại trong kh-subscription:** Wave 85 plan §3 Bucket B liệt kê 12 tables (`tenants`, `users`, `students`, `classes`, `attendances`, `grades`, `payments`, `invoices`, `subscriptions`, `audit_logs`, `notifications`, `staff_invitations`). Trong đó các table giáo dục (students/classes/attendances/grades) thuộc kc-core (đã V58-ed). `tenants`/`subscriptions` = kh-subscription `instances`+`subscriptions` đã V34-ed. `audit_logs`/`notifications` mơ hồ — không tạo placeholder schemas.
3. **Pragmatic scope shift:** ship 8 AC enhancements thực sự P0 — admin-bypass, NULL force-fail, immutable admin_audit_logs, HikariCP reset — thay vì tạo redundant migrations.

### Files mutated (3 migrations + 2 application.yml + tests)

| File | Action | Why |
|---|---|---|
| `kiteclass/kiteclass-core/src/main/resources/db/migration/V59__rls_admin_bypass_and_null_force_fail.sql` | NEW | B-AC1/7/8 — admin-bypass clause + NULL force-fail (DROP NULLIF fallback) |
| `kiteclass/kiteclass-core/src/main/resources/db/migration/V60__create_admin_audit_logs.sql` | NEW | B-AC2/7 — immutable admin_audit_logs table với INSERT-only policy |
| `kitehub/kitehub-subscription/src/main/resources/db/migration/V50__rls_admin_bypass_null_force_fail_audit_logs.sql` | NEW | Sister migration — same 3 ACs cho kh-subscription |
| `kiteclass/kiteclass-core/src/main/resources/application.yml` | UPDATE | B-AC6 — HikariCP `connection-init-sql` RESET app.current_tenant_id |
| `kitehub/kitehub-subscription/src/main/resources/application.yml` | UPDATE | Same B-AC6 |
| `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/datasource/RLSHardeningIT.java` | NEW | Integration tests: 4 P0 ACs verify live |

## Commands run (Tier 1 read-only per agent-aws-access.md §2.1)

```bash
# Existing migrations + RLS state
ls kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql
ls kiteclass/kiteclass-core/src/main/resources/db/migration/V*.sql
cat kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql
cat kitehub/kitehub-subscription/src/main/resources/db/migration/V34__enable_rls_tenant_scoped_tables.sql

# TenantContext + interceptor implementation
find kitehub kiteclass -name "TenantContext*" -o -name "TenantAware*"
cat kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/datasource/TenantAwareDataSourceInterceptor.java
cat kitehub/kitehub-platform/src/main/java/com/kitehub/shared/logging/TenantContextFilter.java

# Existing tests
ls kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/datasource/RLSEnforcementIT.java

# HikariCP config
grep -B 1 -A 10 "hikari:" kiteclass/kiteclass-core/src/main/resources/application.yml \
    kitehub/kitehub-subscription/src/main/resources/application.yml
```

## Findings

### Real changes (must verify intent)

| # | Resource | Action | Root cause | Risk |
|---|---|---|---|---|
| 1 | kc-core RLS policy ON ~50 tables | DROP + CREATE policy with strengthened predicate (admin-bypass + NULL force-fail) | B-AC7 + B-AC8 | Re-issue DDL on existing tables; if RLS predicate breaks → default-deny all queries → P0 incident. Mitigation: TenantContext active (Wave 56 baseline). Test coverage RLSHardeningIT verify live. |
| 2 | kh-subscription RLS policy ON 12 tables | Same strengthening | Same | Same. Mitigation: kh-subscription does NOT force RLS yet (table-owner bypasses); risk lower. |
| 3 | `admin_audit_logs` table | CREATE (both kc-core + kh-subscription) | B-AC2/7 PDPL Art 11 compliance | Append-only via RLS policy chặn UPDATE/DELETE. Backup considerations: standard pg_dump. |
| 4 | HikariCP `connection-init-sql` | SET property | B-AC6 cross-tenant leak prevention defense-in-depth | Adds 1 round-trip per connection borrow. Mitigation: HikariCP runs it once on connection creation (not borrow). |

### Phantom updates (no real change)

None — all changes are real DDL/config.

### Verdict

**Safe to apply** với điều kiện:
- Pre-deploy: ensure all in-flight tx complete (RLS predicate change atomic per table)
- Post-deploy: verify `RLSHardeningIT` PASS trên staging trước production
- Rollback: V59-rollback.sql + V60-rollback.sql ship cùng (B-AC3) — drop new policy + restore V58 predicate

## Prior actions verified (per audit-to-gap-pipeline.md §2.8)

| Action | When | Where verified |
|---|---|---|
| V58 kc-core RLS enabled + forced trên 50+ tables | Wave 56 (2026-04-25) | `V58__enable_rls_tenant_scoped_tables.sql` lines 100-114 |
| V34 kh-subscription RLS enabled (non-forced) trên 12 tables | Wave 56 | `V34__enable_rls_tenant_scoped_tables.sql` header lines 1-21 |
| TenantAwareDataSourceInterceptor AOP @Transactional → SET LOCAL | Wave 56 | `TenantAwareDataSourceInterceptor.java` lines 78-129 |
| RLSEnforcementIT covers 4 base scenarios | Wave 56 | `RLSEnforcementIT.java` lines 38-44 |
| TenantContextFilter MDC propagation | (earlier) | `TenantContextFilter.java` line 31 |

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| Apply V59/V60 (kc-core) + V50 (kh-subscription) via Flyway on next deploy | Coordinator + Spring Boot startup | Atomic per-table DROP+CREATE policy in DO block |
| Run RLSHardeningIT integration tests | CI | PR build verifies pre-merge |
| Verify HikariCP `connection-init-sql` does not regress connection acquisition latency | Coordinator | Spot check Spring Boot Actuator metrics post-deploy |

**Concurrent op check:** No other production mutation ops in flight per `gh run list --status in_progress`. Per `concurrent-production-mutation-ops.md` §4 — schema migration + deploy must serialize: this PR's migration applies first via Flyway startup, then deploy follows.

## Recommendations

1. **APPLY** — fixes P0 CRIT cross-tenant leak prevention (GAP-466 closure prep + GAP-577 admin hardening prep)
2. Post-merge: monitor RDS for slow queries with new RLS predicate (admin-bypass `OR` clause should not affect non-admin path)
3. Watch-for: tenant-context-less background jobs that previously worked via `NULLIF('','')` fallback will now fail (intended — surfacing silent leaks)

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md` §3 Bucket B (8 AC enhancements)
- GAP-466: Postgres RLS policies enable
- GAP-577 (prep): platform admin hardening — admin_audit_logs immutability is precondition
- Rule: `.claude/rules/pre-mutation-state-check.md` §3
- Rule: `.claude/rules/concurrent-production-mutation-ops.md` §3.3 — schema migration + deploy serialize
- Existing migration: V58 kc-core, V34 kh-subscription
- Existing test: RLSEnforcementIT
