# GAP-1471: Hủy yêu cầu thanh toán đang chờ (cancel pending subscription payment)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P2
**Domain:** Frontend+Backend
**Found:** 2026-06-17 (KH-3 G2 walk)
**Affects:** `kitehub/kitehub-subscription/.../service/SubscriptionService.java`, `.../controller/SubscriptionController.java`, `kitehub/kitehub-frontend/src/components/billing/PendingPaymentBanner.tsx`, `.../app/(customer)/billing/payment/[id]/page.tsx`

## Problem

Owner của KiteHub khi khởi tạo/nâng cấp/hạ gói sẽ nhận một thanh toán VietQR ở trạng thái **PENDING**; subscription lưu `pendingPaymentId` + `pendingTier`. Trước GAP này KHÔNG có cách nào để hủy riêng yêu cầu thanh toán đang chờ:

- `SubscriptionService.upgradeSubscription` ném `"Subscription already has a pending upgrade payment"` (HTTP 400) khi owner cố tạo lại → owner bị **chặn cứng**.
- Affordance hủy duy nhất là `DELETE /subscriptions/{id}` (`cancelSubscription`) — nhưng nó hủy **toàn bộ** subscription (rớt về FREE / kết thúc gói), sai mục đích.

Phát hiện thực tế trong **KH-3 G2 walk 2026-06-17**: một payment tạo ở mock-mode bake sẵn URL QR cũ/sai, owner bị chặn cứng không thể tạo payment mới vì payment PENDING cũ không hủy được.

## Proposed Fix

Thêm luồng "hủy yêu cầu thanh toán đang chờ" (BE endpoint + service + FE button + tests):

### Backend (`kitehub-subscription`)
- `SubscriptionController`: thêm `DELETE /api/platform/subscriptions/{id}/pending-payment` (key theo subscription id, mirror `upgrade`/`downgrade`), trả 200 + `SubscriptionResponse`. Auth mirror `cancelSubscription`/`upgradeSubscription` (`requireOwnedSubscription` + gateway `X-Tenant-Id`, không nới lỏng).
- `SubscriptionService.cancelPendingPayment(UUID)`:
  - Không có `pendingPaymentId` → 400 `"Không có yêu cầu thanh toán nào đang chờ"`.
  - Payment đã `COMPLETED` → 400 `"Không thể hủy thanh toán đã được xác nhận"`.
  - Payment `PENDING` → soft-cancel (`cancel()` → status CANCELLED + `softDelete()` → deleted=true).
  - Clear `pendingTier` + `pendingPaymentId`; tier hiện tại giữ nguyên. Riêng create-flow (status PENDING) còn set CANCELLED để không bị GAP-1080 idempotency chặn lại.

### Frontend (`kitehub-frontend`)
- `endpoints.ts`: thêm `subscriptions.cancelPendingPayment(id)`.
- `use-subscriptions.ts`: thêm `useCancelPendingPayment()` (invalidate `['subscriptions']` + `['instances']`).
- `PendingPaymentBanner.tsx`: thêm nút "Hủy yêu cầu thanh toán" + AlertDialog xác nhận → gọi mutation với `pending.subscriptionId` → toast + banner biến mất.
- Trang chi tiết thanh toán `billing/payment/[id]/page.tsx`: thêm action "Hủy" khi PENDING (dùng `payment.subscriptionId`), success → redirect `/billing`.
- `PendingPaymentStatus` type + `PendingPaymentStatusResponse` (BE đã populate `subscriptionId`) bổ sung field `subscriptionId`.

## Acceptance Criteria

- [x] BE `DELETE /subscriptions/{id}/pending-payment` soft-cancel payment + clear pending state (200 + SubscriptionResponse)
- [x] Reject khi không có pending (400) + khi payment đã COMPLETED (400)
- [x] Create-flow PENDING subscription được set CANCELLED để owner tạo lại sạch (tránh GAP-1080 re-block)
- [x] FE: nút "Hủy yêu cầu thanh toán" trên `PendingPaymentBanner` + trang payment detail → confirm → mutation → toast + banner biến mất
- [x] Unit test (SubscriptionServiceTest) + controller test (SubscriptionControllerTest) + component test (PendingPaymentBanner) — chạy PASS local
- [x] BE `./mvnw test` (SubscriptionServiceTest 24/0/0 + SubscriptionControllerTest 5/0/0) + FE `pnpm build` (EXIT 0) + `pnpm lint` (0 lỗi mới) PASS local
- [ ] Coordinator docker-rebuild kitehub-subscription + kitehub-frontend + human G2 re-walk xác minh live (per `feature-ship-runtime-walk-mandate.md` — CHƯA flip DONE)

## Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

Bug class = "owner tạo pending VietQR payment nhưng không có cách hủy riêng". Quét mọi nơi `setPendingPaymentId(...)`:

| Flow | Verdict | Lý do |
|---|---|---|
| `createSubscription` (create-flow) | **FIX** | Endpoint xử lý + set CANCELLED để GAP-1080 không re-block |
| `upgradeSubscription` (upgrade-flow) | **FIX** | Luồng gốc — clear pending, owner tạo lại được |
| `downgradeSubscription` | **EXEMPT** | Chỉ set `pendingTier`, KHÔNG tạo payment → không có pending payment để hủy (đảo lịch hạ gói là feature khác, ngoài scope) |
| `manualRenewal` (`SubscriptionRenewalService`) | **FIX-by-generality** | Pending renewal payment được `getPendingPaymentStatus` surface → banner hủy generic theo `subscriptionId` chạy đúng. Bản thân `manualRenewal` idempotent (skip nếu đã pending) nên không chặn cứng |
| `reactivate` (`OwnerBillingService`) | **FIX-by-generality** | Pending reactivation payment được surface + hủy generic. `reactivate()` idempotent (trả pending hiện có) nên không chặn cứng |

→ Endpoint key theo `subscriptionId` + banner surface MỌI pending payment ⇒ phủ tất cả luồng tạo pending payment. Không cần fix thêm.

## Related

- Discovered in: KH-3 G2 walk (Flow Verification Campaign), 2026-06-17
- Sibling: `clearPendingUpgrade` (admin-reject path) — `cancelPendingPayment` là phiên bản owner-initiated, có soft-delete payment
- Guard: GAP-1080 create-idempotency (lý do create-flow phải set CANCELLED), GAP-1015 `TenantOwnershipGuard` (auth không nới lỏng)
