---
id: GAP-715
title: admin_audit_log insert fails — `Could not convert 'java.lang.String' to '[B'` (Postgres-specific type binding, sister of GAP-710)
status: OPEN
priority: P0
phase: phase-1-beta
audience: dev
found: 2026-05-22
related: [GAP-710, GAP-707, GAP-710]
---

# GAP-715 — admin_audit_log binding error

## Problem

Every PLATFORM_ADMIN privileged action attempts to persist a row in `admin_audit_log` (per Wave 92 Bucket D + Wave 94c audit suite enrichment GAP-521). The insert fails with:

```
WARN c.k.s.audit.AdminAuditAspect - Failed to persist admin_audit_log row
  (action=BETA_REQUEST_APPROVE, target=beta_access_request/6):
  Could not convert 'java.lang.String' to '[B' using
  'org.hibernate.type.descriptor.java.StringJavaType' to unwrap
```

`[B` = Java byte array. A column in `admin_audit_log` is declared as binary (likely `bytea` or similar Postgres-specific type) but receives a String via Hibernate. This is the SAME bug class fixed by GAP-710 (LoginAuditLog INET vs String) — different audit table, same H2/Postgres binding hazard.

## Evidence

Live log 2026-05-22 11:00:44 during admin approve POST `/api/v1/admin/beta-requests/6/approve`:
```
2026-05-22 11:00:44.061 WARN c.k.s.audit.AdminAuditAspect
  - Failed to persist admin_audit_log row (action=BETA_REQUEST_APPROVE, ...)
  Could not convert 'java.lang.String' to '[B'
```

**Impact analysis:**
- HTTP response: 200 (parent transaction succeeded because AdminAuditAspect has REQUIRES_NEW isolation per `audit-service-isolation.md` v1.0.0)
- Audit row: NOT persisted → compliance gap (PDPL Art 11 admin action audit trail incomplete)
- Pattern: EVERY admin action loses audit row silently

This is **P0 compliance** — admin actions losing audit trail violates retention mandate.

## Root Cause

Likely a column in `admin_audit_log` (e.g., `metadata_hash`, `signature`, `fingerprint`, encrypted payload) is declared `bytea` in migration but mapped to Java `String` in entity OR vice versa. Need to:

1. Inspect `admin_audit_log` schema (`docker exec kite-postgres psql -U kitehub -d kitehub -c "\d admin_audit_log"`)
2. Identify the binary column
3. Fix entity mapping (`@JdbcTypeCode(SqlTypes.VARBINARY)` OR migrate column to `text`)
4. Add Testcontainers IT test per `postgres-specific-type-testcontainers.md` mandate

## Proposed Fix

**Option A — Entity mapping fix (recommended if column is genuinely binary):**

```java
@Column(name = "fingerprint", columnDefinition = "bytea")
@JdbcTypeCode(SqlTypes.VARBINARY)
private byte[] fingerprint;
```

**Option B — Column type migration (recommended if column is semantically String):**

Add Flyway migration `V62__admin_audit_log_binary_to_text.sql`:
```sql
ALTER TABLE admin_audit_log
  ALTER COLUMN <col_name> TYPE text USING encode(<col_name>, 'hex');
```

Then update entity `columnDefinition = "text"` + Java field stays `String`.

**Option C — Combined (Option B + add Testcontainers IT per `postgres-specific-type-testcontainers.md` §4):**

Mandatory for any Postgres-specific type per the rule.

## Acceptance Criteria

- [ ] Identify which column triggers `[B` conversion error
- [ ] Apply fix (Option A or B + IT test per Option C)
- [ ] Live verify: admin approve beta-request → admin_audit_log row inserted (verify via `SELECT * FROM admin_audit_log WHERE action='BETA_REQUEST_APPROVE' AND target='beta_access_request/X'`)
- [ ] Testcontainers IT test exercises CRUD round-trip per `postgres-specific-type-testcontainers.md` §4
- [ ] No regression in other audit-log writes (LoginAuditLog, ChildProtectionAuditLog, etc.)
- [ ] grep all admin operations → verify audit_log row persists for each

## Related

- Triggered by: Wave 105 fix-Bucket-E session 2026-05-22 (during GAP-711/712/713 fix verify)
- Sister rule: `postgres-specific-type-testcontainers.md` v1.0.0 (mandates Testcontainers IT for Postgres-specific types)
- Sister rule: `audit-service-isolation.md` v1.0.0 (REQUIRES_NEW isolation kept admin action HTTP 200 despite audit fail)
- Sister gap: GAP-710 (LoginAuditLog INET binding, same bug class, fixed Wave 72b)
- Wave 105 candidate scope (P0 — compliance impact)
