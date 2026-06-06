# GAP-1016: Manual renewal miễn phí — không tạo payment + reactivate instance bị suspend miễn phí

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (KH-5 subscription renew G1 walk)
**Affects:** `SubscriptionRenewalService.manualRenewal()` (kitehub-subscription)

## Problem

KH-5 G1 walk (live): `POST /api/platform/subscriptions/{id}/renew` gọi `manualRenewal()` — gia hạn `expiresAt` +1 tháng + set status ACTIVE + reactivate instance nếu đang SUSPENDED, NHƯNG **không tạo payment record nào**.

Walk evidence: renew subscription `81cf38cd…` → HTTP 204, `expiresAt` 2026-06-23 → 2026-07-23 (+1 tháng), `version` 0→1, NHƯNG `SELECT count(*) FROM payments WHERE subscription_id=…` vẫn = 1 (không có payment mới).

So sánh: `processRenewal()` (scheduler auto-renew) ở line 78-79 TẠO `createRenewalPayment()` PENDING; `manualRenewal()` (line 105-132, API path) KHÔNG. Hệ quả:
- Owner tự bấm "Gia hạn" → nhận thêm 1 tháng miễn phí, không qua cổng thanh toán VietQR.
- Owner có instance bị SUSPENDED (do non-payment) → tự renew → instance reactivate miễn phí, bypass việc thu tiền.

Revenue leak + bypass payment gate (SUB-20 manual VietQR). P1.

## Root Cause

`manualRenewal()` được viết như một "extend cycle" thuần, thiếu bước tạo PENDING payment + chờ confirm như `processRenewal()` / upgrade flow. Code comment không ghi chủ ý "free renewal".

## Proposed Fix

`manualRenewal()` nên tạo PENDING payment (reuse `createRenewalPayment()`) + set `pendingPaymentId`, KHÔNG tự gia hạn `expiresAt`/ACTIVE/reactivate ngay. Việc gia hạn + reactivate chỉ xảy ra sau khi admin confirm payment (giống upgrade flow `applyPendingUpgrade`). Hoặc nếu business chấp nhận manual renew tức thì cho tier FREE → document rõ + chặn cho tier paid.

## Acceptance Criteria

- [ ] Manual renew tier paid → tạo PENDING payment, chưa gia hạn cho tới khi confirm
- [ ] Manual renew KHÔNG reactivate instance SUSPENDED trước khi payment confirmed
- [ ] IT verify payment record được tạo + expiry chỉ extend sau confirm

## Related

- Discovered in: KH-5 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh5-subscription-lifecycle.md` (FM-3)
- Related: KH-4 manual VietQR + admin confirm flow (cùng payment gate SUB-20)
