# GAP-881: Entity `Invoice` cần `deleted` + `enrollment_id` columns trong DB

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC finance)
**Affects:** `kiteclass-core` module finance; entity `Invoice extends BaseEntity` vs `invoices` table

## Problem

Entity `Invoice extends BaseEntity` kỳ vọng cột `deleted BOOLEAN NOT NULL` + `version` + thêm `enrollment_id` (+ unique `uk_invoices_enrollment`, index `idx_invoices_enrollment`, `idx_invoices_deleted`). Migration V1+V26 KHÔNG tạo `invoices.deleted` lẫn `invoices.enrollment_id`.

Hibernate `tenantFilter` + soft-delete filter dựa `deleted` → query thật sẽ lỗi nếu DB không có cột.

## Proposed Fix

Migration backfill `ALTER TABLE invoices ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE` + `ADD COLUMN enrollment_id BIGINT` + unique + indexes per entity.

## Acceptance Criteria

- [ ] Migration V## add `deleted` + `enrollment_id` + indexes
- [ ] Backfill `deleted=FALSE` for existing rows
- [ ] Entity `findAll()` works on Postgres with tenantFilter
- [ ] Reference cluster doc 04-finance §A3

## Discovered in

`documents/02-architecture/database/kiteclass/04-finance.md` §A3

## Log

- **2026-06-03** DONE — entity-drift fixed (V79) + verified `Wave14EntityDriftMigrationsIT` (Flyway V1..V86 real Postgres, 19 tests PASS) + schema-drift PASS. Wave 14 DB completion.
