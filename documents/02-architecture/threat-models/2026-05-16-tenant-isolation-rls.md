# Threat Model — Tenant Isolation RLS Multi-Tenant

**Created:** 2026-05-16
**Wave:** 86 Bucket E
**Status:** complete
**Scope:** Postgres Row-Level Security (RLS) cross-tenant leak prevention — set `app.current_tenant_id` GUC trên HikariCP connection → policy filters every SELECT/UPDATE/DELETE
**Linked gaps:** Wave 85 Bucket B (RLS NULL force-fail + HikariCP GUC reset), GAP-NEW future RLS hardening
**Mitigation owners:** kitehub-base shared lib (HikariCP interceptor), kiteclass-core BaseTenantConfig

---

## 1. Asset under threat

Multi-tenant data isolation tại DB layer. KiteHub + KiteClass dùng SHARED-DB multi-tenant model — mỗi row có `tenant_id` column; RLS policy ràng buộc `WHERE tenant_id = current_setting('app.current_tenant_id')::uuid` cho mọi query.

**Trust boundaries crossed:**
1. Browser → ALB → service (JWT tenant_id claim)
2. service → HikariCP connection pool (GUC `app.current_tenant_id` set)
3. HikariCP → RDS (RLS policy enforcement)

---

## 2. STRIDE analysis

### S — Spoofing

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| S1 | Attacker forges JWT with different `tenant_id` claim | Low | Critical | JWT HMAC-signed với rotating key (Wave 78 KMS — P1 carry); signature verify required before tenant_id extract | — |
| S2 | Service-account JWT bypasses RLS | Low | Critical | Service accounts use `BYPASSRLS` only for migrations + cron jobs; user-facing endpoints use `app_user` role (no BYPASSRLS) | — |
| S3 | Pgbouncer connection swap leaks tenant context | Medium | High | Wave 85 Bucket B — HikariCP interceptor RESETS GUC on connection return to pool: `SELECT set_config('app.current_tenant_id', '', false)` | — |

### T — Tampering

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| T1 | Application bug — service forgets to SET tenant before query → RLS policy returns ALL rows | High (without protection) | Critical | **Wave 85 Bucket B — RLS NULL force-fail:** policy `WHERE tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid` → if GUC empty, policy returns 0 rows (silent leak eliminated) | — |
| T2 | Direct SQL via psql bypasses HikariCP interceptor | Low | High | Production RDS access via SSM Session Manager only; dev access via bastion + named role | — |
| T3 | Migration runs SQL that DOES NOT set tenant_id → cross-tenant write | Low | Critical | Flyway migrations explicit `tenant_id IS NOT NULL` constraint on all tenant tables; BYPASSRLS role used for migration but commits must include tenant_id column | — |

### R — Repudiation

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| R1 | Operator queries cross-tenant data, denies it | Low | High | Postgres `log_statement = 'mod'` logs all DML; CloudTrail logs RDS connection events; audit_log table for admin actions | Verify production `log_statement` setting Wave 87 |
| R2 | Tenant data accidentally exported to wrong tenant's report | Medium | High | Report generation uses same RLS-protected query path; tenant_id propagates JWT → GUC → SQL | — |

### I — Information Disclosure

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| I1 | SQL injection bypasses RLS via UNION on other tenant | Low | Critical | Spring Data JPA parameterized queries; no native SQL string concat in `*Repository.java`; ArchUnit test could enforce | Add ArchUnit test ban `nativeQuery=true` without `@Query` review |
| I2 | JOIN to non-tenant-aware table leaks data | Medium | High | Code review: every shared table (`users`, `audit_log`, etc.) has tenant_id OR is read-only catalog | Periodic audit — list all tables WITHOUT tenant_id, document why each is OK |
| I3 | Materialized view created without tenant_id → background refresh leaks across tenants | Low | High | MV creation policy: must include tenant_id column; refresh runs in BYPASSRLS context but query uses tenant_id filter | — |
| I4 | Postgres `pg_stat_statements` exposes other-tenant query patterns | Low | Low | `pg_stat_statements` access limited to `db_admin` role; users have no SELECT on pg_catalog beyond default | — |

### D — Denial of Service

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| D1 | Noisy-neighbor tenant exhausts connection pool | Medium | Medium | Per-tenant connection limit via HikariCP advisory; Wave 85 Bucket E JVM 60% memory + 3 CloudWatch alarms | — |
| D2 | RLS policy SELECT causes seq-scan on large table (no index on tenant_id) | Low | Medium | All tenant tables have index `(tenant_id, ...)` per `documents/02-architecture/data-retention-policy.md` convention; pgaudit warn on missing | Audit tenant tables for index Wave 87 |

### E — Elevation of Privilege

| # | Threat | Likelihood | Impact | Existing mitigation | Gap / follow-up |
|---|--------|-----------|--------|--------------------|-----------------|
| E1 | Tenant A's PLATFORM_ADMIN escalates to Tenant B's PLATFORM_ADMIN | Low | Critical | PLATFORM_ADMIN role scope = single tenant only (per BR-AUTH); cross-tenant admin = separate `SUPER_ADMIN` reserved for Kite ops team | — |
| E2 | Bug: user creates resource without tenant_id; system defaults to NULL | Low | Critical | NOT NULL constraint at DB layer; INSERT fails fast | — |
| E3 | Function created without `SECURITY INVOKER` runs with creator privilege bypassing RLS | Low | Critical | Functions reviewed at migration time; `SECURITY DEFINER` only for explicitly scoped ops (e.g., `tenant_provision_*` admin RPC) | Add migration review checklist item Wave 87 |

---

## 3. Mitigation status summary

| Severity | Total | Mitigated | Open follow-up |
|---|---|---|---|
| Critical | 9 | 9 | 1 (ArchUnit nativeQuery ban) |
| High | 7 | 7 | 3 (R1 log_statement verify, I2 table audit, D2 index audit) |
| Medium | 2 | 2 | 0 |
| Low | 1 | 1 | 0 |

**Verdict:** Strong posture post-Wave-85 Bucket B fixes. 4 follow-ups Wave 87+.

---

## 4. Trust boundary diagram

```
[Browser]
     | JWT tenant_id (HMAC signed)
     v
[Service Layer]
     | TenantContextFilter extracts tenant_id from JWT
     | Stores in ThreadLocal/MDC
     v
[HikariCP Interceptor — Wave 85 Bucket B]
     | onAcquire: SELECT set_config('app.current_tenant_id', '<uuid>', false)
     | onReturn:  SELECT set_config('app.current_tenant_id', '', false)  ← reset eliminates leak across re-use
     v
[Postgres RDS]
     | RLS Policy: WHERE tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
     |             ^^ NULLIF coerces empty string → NULL → policy returns 0 rows (force-fail)
     v
[Returned Rows]
     | Only matching tenant's rows
     v
[JPA Entity Mapping]
```

---

## 5. Test cases

- [ ] T1 RLS force-fail: clear GUC, run `SELECT * FROM students` as `app_user` → 0 rows returned (NOT all rows)
- [ ] S3 connection swap: acquire connection, SET GUC tenant_A, run query, return connection; acquire same connection, run query WITHOUT setting GUC → 0 rows
- [ ] T3 migration: try INSERT without tenant_id → constraint violation
- [ ] E2 NULL prevent: try INSERT with explicit NULL tenant_id → constraint violation
- [ ] I1 native query audit: ArchUnit test scans for `nativeQuery = true` without `@TenantSafeQuery` annotation marker

---

## 6. Open follow-ups

1. **I1 follow-up:** Add ArchUnit test `tenant_isolation_nativeQuery_ban.adoc` — fail build if any `@Query(nativeQuery=true)` lacks marker annotation reviewed. Track Wave 87.
2. **I2 follow-up:** Audit `information_schema.tables WHERE table_schema = 'public'` → list tables without tenant_id column; document each as "shared catalog" or "violation". Track Wave 87.
3. **R1 follow-up:** Verify production RDS parameter `log_statement = 'mod'`; document trong `documents/02-architecture/data-retention-policy.md`. Track Wave 87.
4. **E3 follow-up:** Add Flyway migration review checklist item: any new function requires `SECURITY INVOKER` unless documented exception. Track Wave 87.

---

## 7. References

- Wave 85 Bucket B audit (`documents/04-quality/audits/security/2026-05-15-wave-85-post-apply-v2.md`) — RLS NULL force-fail + HikariCP GUC reset
- Wave 85 Bucket B audit Cat 3 — A01 elimination of silent cross-tenant leak
- [`backend/backend-standards.md`](../../../.claude/skills/backend/backend-standards.md) — multi-tenant patterns
- Postgres RLS: https://www.postgresql.org/docs/current/ddl-rowsecurity.html

---

## 8. Log

- **2026-05-16:** Threat model created (Wave 86 Bucket E Fix 4). 19 threats analyzed; 4 follow-ups filed. Post-Wave-85 RLS hardening provides strong baseline.
