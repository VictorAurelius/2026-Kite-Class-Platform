# GAP-880: Entity `Payment` ↔ bảng `payments` drift NẶNG — 12+ cột entity không có DB

**Status:** 🟡 PARTIAL (70%)
**Priority:** 🔴 P0
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC finance)
**Affects:** `kiteclass-core` module finance; entity `Payment` vs `payments` (V1+V26)

## Problem

Entity JPA `Payment` khai báo 12+ cột KHÔNG tồn tại trong `payments` table (V1+V26): `installment_id`, `gateway_transaction_id`, `payment_url`, `gateway_response`, `receipt_number`, `initiated_at`, `expires_at`, `completed_at`, `failed_at`, `refunded_at`, `failure_reason`, `payment_status` vs DB `status`. Entity declare `transaction_id NOT NULL UNIQUE`, DB nullable không-unique. Enum `PaymentMethod`/`PaymentStatus` UPPERCASE vs DB lowercase CHECK.

→ Chạy entity trên Postgres real (ddl-auto=validate hoặc none) sẽ lỗi "column does not exist". Đã rẽ nhánh hoàn toàn entity-vs-DB.

## Proposed Fix

Decide: (a) reconcile migration adds entity cột + deprecate legacy DB cột HOẶC (b) confirm `payments` legacy và `payment_records` thực dùng → drop entity `Payment` (cluster đã có GAP-879). Decide trước khi ship Phase 1.5 paid payment integration.

## Acceptance Criteria

- [ ] Decision documented: reconcile vs deprecate
- [ ] Nếu reconcile: migration add 12 cột
- [ ] Nếu deprecate: drop entity `Payment` + remove repository
- [ ] Reference cluster doc 04-finance §A2 + GAP-879

## Discovered in

`documents/02-architecture/database/kiteclass/04-finance.md` §A2
