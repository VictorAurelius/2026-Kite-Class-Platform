# GAP-893: `users.role` không CHECK constraint + seed `ADMIN` vs `PLATFORM_ADMIN` drift

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH auth/user/instance)
**Affects:** `kitehub-platform` module auth; `users.role` column

## Problem

`users.role VARCHAR(20) NOT NULL DEFAULT 'OWNER'` không có CHECK constraint. Code dùng đồng thời: `OWNER`, `PLATFORM_ADMIN`, `STAFF`, `ADMIN` (legacy seed V9). Drift: seed admin V9 dùng `ADMIN`, V37 update set `WHERE role = 'PLATFORM_ADMIN'` → seed admin gốc **không** được set `totp_required=TRUE` nếu chưa migrate.

Cùng pattern với `instances.tier/status/domain_status`, `oauth_attempts.provider/status`, `migration_outbox.event_type` — entity `@Enumerated(STRING)` nhưng DDL không CHECK.

## Proposed Fix

Migration V## (a) data migration `UPDATE users SET role='PLATFORM_ADMIN' WHERE role='ADMIN'` + sync TOTP requirement; (b) add CHECK constraint với enum values. Apply same pattern cho instances/oauth_attempts/migration_outbox.

## Acceptance Criteria

- [ ] Migration V## update legacy ADMIN→PLATFORM_ADMIN + CHECK
- [ ] Verify seed admin has totp_required=TRUE
- [ ] Apply CHECK pattern cho 3 sister tables
- [ ] Reference cluster doc KH 01-auth-user-instance §A2

## Discovered in

`documents/02-architecture/database/kitehub/01-auth-user-instance.md` §A2
