# GAP-882: `invoices.status` enum vs CHECK drift — persist SENT/REFUNDED vi phạm CHECK

**Status:** 🟢 DONE
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

- [x] Migration update `invoices.status` CHECK + backfill data — **V86 already did this** (UPPERCASE 7-value `chk_invoices_status`); V92 adds defensive idempotent re-assert
- [x] Add `invoice_items.item_type` CHECK matching entity — **V92** `chk_invoice_items_type` (NULL OR TUITION/MATERIALS/REGISTRATION_FEE/EXAM_FEE/OTHER) + explicit lowercase→UPPERCASE backfill
- [x] IT test verify all enum values persist — `InvoiceEnumCheckConstraintTest` replays V1..V92 on Testcontainers Postgres, iterates `InvoiceStatus.values()` + `InvoiceItemType.values()` (accept) + rejects invalid; full suite 1635 tests 0-fail
- [x] Reference cluster doc 04-finance §A4 — canonical UPPERCASE set documented in V92 migration + this gap; drift §A4 described now resolved

## Discovered in

`documents/02-architecture/database/kiteclass/04-finance.md` §A4

## Resolution (Wave p0-1 Bucket A, 2026-06-07)

Fix-time state-check (`audit-to-gap-pipeline.md` §2.8) revised scope vs plan: `invoices.status` drift was **already fixed by V86** (UPPERCASE 7-value CHECK + backfill matching the 7-value `InvoiceStatus` enum) — the gap's primary P0 (persist SENT/REFUNDED → CHECK violation) was no longer reproducible. The genuine remaining drift was **`invoice_items.item_type`** — `VARCHAR(50)` with NO CHECK at all (column comment still lowercase) vs 5-value UPPERCASE `InvoiceItemType` enum. Both Java enums already canonical UPPERCASE → no enum source change, no caller migration.

**V92** (`V92__invoice_enum_check_canonical.sql`): (1) `invoices.status` defensive idempotent re-assert (no-op on V86 chain); (2) `invoice_items.item_type` new `chk_invoice_items_type` + explicit backfill (`'material'→'MATERIALS'`, since `UPPER('material')`='MATERIAL' non-canonical).

**G3 live walk (production-parity, 2026-06-07):** rebuilt kiteclass-core → V92 applied success=t on live `kiteclass_shared` DB. `chk_invoice_items_type` def verified UPPERCASE 5-value. Behavioral (rollback txn): INSERT `item_type='MATERIALS'` → INSERT 0 1 (accept); INSERT `item_type='material'` → ERROR violates `chk_invoice_items_type` (reject). Per `feature-ship-runtime-walk-mandate.md` §3.

## Log

- **2026-06-07 (Wave p0-1 Bucket A):** Status OPEN → 🟢 DONE. V92 + InvoiceEnumCheckConstraintTest (1635 tests 0-fail) + G3 live DB walk (V92 applied + accept/reject behavioral verified). Scope revised per §2.8 — V86 pre-fixed invoices.status; genuine drift = invoice_items.item_type CHECK. PR #2241.
