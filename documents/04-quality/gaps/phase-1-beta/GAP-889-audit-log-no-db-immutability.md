# GAP-889: `audit_log` (V35) không có DB-level append-only enforcement

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB / Security / Compliance
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC compliance)
**Affects:** `kiteclass-core` module compliance; `audit_log` table

## Problem

`audit_log` extends BaseEntity → có `deleted BOOLEAN` + `updated_at` + `version` + `updated_by`. Caller bắt buộc đi qua `AuditLogWriter` (javadoc: "Direct repository.save is discouraged") nhưng KHÔNG có cơ chế DB-level chặn UPDATE/DELETE.

Compared to `admin_audit_logs` (V60 RLS UPDATE/DELETE = false) hoặc `child_protection_audit_log` (V54 REVOKE DELETE), `audit_log` v1 lỏng nhất — code path khác (test/migration/raw SQL) có thể mutate row.

Compliance drift giữa generations audit schema (Wave 4 vs Wave 85).

## Proposed Fix

Migration apply pattern V54/V60: REVOKE UPDATE/DELETE từ app role + RLS policy block. Hoặc trigger BEFORE UPDATE/DELETE raise exception. Test path qua test profile/sudo role.

## Acceptance Criteria

- [ ] Migration V## block UPDATE/DELETE on `audit_log`
- [ ] Test isolation profile bypass mechanism documented
- [ ] Reference cluster doc 07-compliance-audit §A2

## Discovered in

`documents/02-architecture/database/kiteclass/07-compliance-audit.md` §A2
