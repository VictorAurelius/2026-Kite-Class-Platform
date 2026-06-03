# GAP-879: KC Finance — `payments` vs `payment_records` 2 hệ song song không có FK liên kết

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB / Business
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC finance)
**Affects:** `kiteclass-core` module finance; bảng `payments` (V1) + `payment_records` (V69)

## Problem

Cluster có 2 hệ thanh toán song song khác hẳn schema/enum/RLS/BaseEntity, KHÔNG có FK liên kết:

| Khía cạnh | `payments` (V1) | `payment_records` (V69) |
|---|---|---|
| Định hướng | Cổng online (VNPay/MoMo/ZaloPay) | Thủ công tại trung tâm |
| Enum method | lowercase comment | UPPERCASE + CHECK |
| Kiểu amount | DECIMAL(12,2) | NUMERIC(19,2) |
| Idempotency | `payment_idempotency_keys` (V61) | shared `idempotency_keys` (V66) scope=PAYMENT |
| RLS DB | ✅ V58/V59 | ❌ chưa (post-V58/V59) |
| BaseEntity | KHÔNG | CÓ |

Phase 1 BETA `payment_records` canonical (luồng GAP-292b/GAP-705 dùng). `payments` legacy + entity drift NẶNG (xem GAP-880).

## Proposed Fix

Document `payment_records` canonical trong business doc + add RLS cho `payment_records` (V69 missing). Cân nhắc deprecate `payments` schema/entity hoặc reconcile bằng 1 migration explicit.

## Acceptance Criteria

- [ ] Business doc tuyên bố canonical = `payment_records`
- [ ] RLS migration cho `payment_records`
- [ ] Deprecate/reconcile decision documented
- [ ] Reference cluster doc 04-finance §A1

## Discovered in

`documents/02-architecture/database/kiteclass/04-finance.md` §A1
