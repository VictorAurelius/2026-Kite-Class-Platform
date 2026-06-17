# GAP-1473: Pending-payment banner sai số tiền — FE↔BE field drift `PendingPaymentStatus`

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P2
**Domain:** Frontend
**Found:** 2026-06-17 (KH-3 G2 walk — phát hiện khi làm GAP-1471)
**Affects:** `kitehub/kitehub-frontend/src/types/subscription.ts`, `.../components/billing/PendingPaymentBanner.tsx`
**Fixed in:** PR #2466 (gộp cùng GAP-1471 — chung banner + type)

## Problem

Hợp đồng FE↔BE của trạng thái thanh toán đang chờ bị lệch tên field → banner "Đang chờ xác nhận thanh toán" hiển thị **sai số tiền**:

| Ý nghĩa | BE gửi (`PendingPaymentStatusResponse`) | FE đọc (`PendingPaymentStatus`) | Hậu quả |
|---|---|---|---|
| Số tiền | `amount` (Long) | `amountVnd` | `formatVnd(pending.amountVnd)` = `formatVnd(undefined)` → banner hiện số tiền sai/`0đ` |
| SLA xác nhận | `adminConfirmSlaHours` (long, vd `24`) | `adminConfirmSla` (string) | FE mong string đã format; BE gửi number → luôn rơi về default `"trong vòng 24 giờ làm việc"`, không bao giờ phản ánh giá trị BE thật |

BE `OwnerBillingService.getPendingPaymentStatus` build `.amount(payment.getAmountVnd())` + `.adminConfirmSlaHours(adminConfirmSlaHours)` — đúng theo DTO. FE type khai báo `amountVnd` + `adminConfirmSla: string` → không khớp wire-format → JSON deserialize ra `undefined` cho 2 field này.

Pre-existing (không phải do GAP-1471). Surface khi GAP-1471 thêm `subscriptionId` vào cùng type. Đúng loại bug `check-be-fe-url-contract` nhắm tới (FE↔BE contract drift).

## Proposed Fix

FE-side (gọn nhất — đổi FE khớp wire-format BE, không đổi BE):
- `types/subscription.ts`: `amountVnd: number` → `amount: number`; `adminConfirmSla: string | null` → `adminConfirmSlaHours: number`.
- `PendingPaymentBanner.tsx`: `formatVnd(pending.amount)` (2 chỗ) + render SLA `trong vòng ${pending.adminConfirmSlaHours ?? 24} giờ làm việc`.
- Update test fixture cho khớp type.

## Acceptance Criteria

- [ ] FE `PendingPaymentStatus` field khớp BE `PendingPaymentStatusResponse` (`amount`, `adminConfirmSlaHours`).
- [ ] Banner hiển thị đúng số tiền payment đang chờ (không `undefined`/`0đ`).
- [ ] Banner hiển thị SLA theo giá trị BE (`trong vòng N giờ làm việc`).
- [ ] `pnpm build` + banner test PASS.
- [ ] Human G2 re-walk xác nhận banner số tiền đúng.

## Related

- Discovered in: PR #2466 (GAP-1471 cross-flow sweep side-finding)
- Fix shipped in: PR #2466 (cùng banner + type với GAP-1471)
- Bug class: FE↔BE contract drift (`check-be-fe-url-contract.sh` scope)
