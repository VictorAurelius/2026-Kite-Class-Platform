# GAP-890: `leads` + `contact_messages` entity nhưng không có migration

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC branding/marketing)
**Affects:** `kiteclass-core` module marketing; entities `Lead` + `ContactMessage`

## Problem

Entity JPA `Lead` (`@Table("leads")`) + `ContactMessage` (`@Table("contact_messages")`) khai báo đầy đủ cột + `tenantFilter`. **KHÔNG có migration nào V1..V77 tạo 2 bảng này** (verified `grep -E "CREATE TABLE (leads|contact_messages)"` = rỗng).

Chạy module marketing trên DB chỉ migration (không ddl-auto=update) → query qua `LeadRepository`/`ContactMessageRepository` lỗi "relation does not exist". Lead capture form ở `landing_pages` (POST `/api/v1/.../leads`) sẽ 500.

Pattern tương tự GAP-809 đã sửa cho `landing_pages` (V75 walk fix) — `leads` + `contact_messages` chưa được sửa.

## Proposed Fix

Migration V## CREATE TABLE leads + contact_messages theo template V75 + entity declaration (instance_id NOT NULL + audit + soft-delete + RLS).

## Acceptance Criteria

- [ ] Migration V## creates 2 tables matching entities
- [ ] RLS policy added (per GAP-885 pattern)
- [ ] IT test verify Lead/ContactMessage save flow
- [ ] Reference cluster doc 08-branding-marketing §A1 + GAP-809

## Discovered in

`documents/02-architecture/database/kiteclass/08-branding-marketing.md` §A1

## Log

- **2026-06-03** DONE — entity-drift fixed (V79) + verified `Wave14EntityDriftMigrationsIT` (Flyway V1..V86 real Postgres, 19 tests PASS) + schema-drift PASS. Wave 14 DB completion.
