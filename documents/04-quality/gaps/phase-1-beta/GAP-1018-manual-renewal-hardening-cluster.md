# GAP-1018: Manual renewal hardening cluster — billing cycle + pending downgrade + idempotency + downgrade-to-FREE

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-06 (KH-5 subscription downgrade/renew G1 walk)
**Affects:** `SubscriptionRenewalService.manualRenewal()` + `SubscriptionService.downgradeSubscription()` (kitehub-subscription)

## Problem

KH-5 G1 walk catalog 3 hardening issue (P2, không block flow nhưng ảnh hưởng đúng đắn):

1. **Bỏ qua billing cycle (FM-6):** `manualRenewal()` line 116 + `processRenewal()` line 82 hardcode `plusMonths(1)`, bỏ qua `BillingCycle.ANNUALLY` (helper `calculateExpiryDate()` xử lý đúng nhưng không được dùng). Subscriber gói năm bấm renew → chỉ +1 tháng thay vì +1 năm.

2. **Không apply pending downgrade khi manual renew (FM-7):** chỉ `processRenewal()` (line 69-75) áp pending tier change; `manualRenewal()` không có block này. Owner schedule downgrade rồi manual renew → downgrade bị mất âm thầm, `pendingTier` kẹt.

3. **Manual renew không idempotent (FM-10a):** `manualRenewal()` không có idempotency key — double POST `/renew` → cộng dồn +2 tháng. (Liên quan idempotency pattern GAP-1004.)

4. **Downgrade cho phép tier FREE (FM-10b):** `downgradeSubscription()` line 231 chỉ chặn `newTier.ordinal() >= current`, cho phép downgrade về FREE; nhưng `createSubscription()` cấm tier FREE → bất nhất. Auto-renew sau đó tạo payment 0₫ cho gói FREE.

## Root Cause

`manualRenewal()` là implementation tối giản, chưa parity với `processRenewal()` về cycle/pending-tier/payment; downgrade thiếu lower-bound consistency với create.

## Proposed Fix

1. Dùng `calculateExpiryDate(tier, billingCycle)` thay cho hardcode `plusMonths(1)` ở cả manual + auto renew.
2. Thêm block apply `pendingTier` vào `manualRenewal()` (mirror `processRenewal()`).
3. Thêm idempotency key cho `/renew` (reuse pattern GAP-1004 / `idempotency_keys` table).
4. Quyết định business: downgrade về FREE có hợp lệ không? Nếu không → chặn ở `downgradeSubscription()`; nếu có → cho phép create FREE để nhất quán.

## Acceptance Criteria

- [ ] Renew gói ANNUALLY → +1 năm (không phải +1 tháng)
- [ ] Manual renew áp dụng pending downgrade đã schedule
- [ ] Double POST /renew cùng key → 1 lần gia hạn
- [ ] Downgrade→FREE nhất quán với create (chặn hoặc cho phép cả hai)

## Related

- Discovered in: KH-5 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh5-subscription-lifecycle.md` (FM-6/FM-7/FM-10)
- Related: GAP-1004 (idempotency pattern, KC-7), GAP-1016 (manual renew payment), GAP-627 (PENDING_BALANCE Phase 1.5)
