# GAP-882: `invoices.status` enum vs CHECK drift — persist SENT/REFUNDED vi phạm CHECK

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC finance)
**Affects:** `kiteclass-core` module finance; `invoices.status`, `invoice_items.item_type`

## Problem

`invoices.status` CHECK = 6 lowercase (`draft, pending, partially_paid, paid, overdue, cancelled`). Entity enum `InvoiceStatus` = 7 UPPERCASE (`DRAFT, SENT, PAID, PARTIAL, OVERDUE, CANCELLED, REFUNDED`). Mismatch kép: hoa/thường + tập giá trị (`SENT`/`REFUNDED`/`PARTIAL` không có CHECK; `pending`/`partially_paid` không có enum).

Persist `SENT`/`REFUNDED` → DB CHECK violation. `invoice_items.item_type` cũng có UPPERCASE enum vs lowercase DB comment (no CHECK).

## Proposed Fix

Decide canonical enum set (likely entity UPPERCASE per `design-patterns.md`). Migration update CHECK + lowercase→UPPERCASE backfill. Apply same fix `invoice_items.item_type`.

## Acceptance Criteria

- [ ] Migration V## update `invoices.status` CHECK + backfill data
- [ ] Add `invoice_items.item_type` CHECK matching entity
- [ ] IT test verify all enum values persist
- [ ] Reference cluster doc 04-finance §A4

## Discovered in

`documents/02-architecture/database/kiteclass/04-finance.md` §A4
