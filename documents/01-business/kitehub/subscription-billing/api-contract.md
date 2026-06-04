# Subscription & Billing — API Contract

## Enums

### PaymentMethod (canonical — kitehub subscription billing domain)

Single source of truth: `com.kitehub.platform.domain.enums.PaymentMethod` (Java) ↔
`kitehub-frontend/src/types/payment.ts` (TypeScript union).

| Value | Vietnamese label | Online gateway? | Use case |
|---|---|---|---|
| `VIETQR` | VietQR | ✅ (scan QR) | Default cho VN center owners (SUB-11) |
| `MOMO` | Ví MoMo | ✅ | E-wallet phổ biến |
| `VNPAY` | VNPay | ✅ | Payment gateway VN |
| `BANK_TRANSFER` | Chuyển khoản ngân hàng | ❌ (manual) | Direct bank transfer + admin verify |
| `MANUAL` | Thủ công | ❌ (admin entry) | Admin nhập tay payment record (refund, comp) |

**Note:** đây là PaymentMethod **subscription billing** scope. KiteClass school payment dùng
enum riêng `com.kiteclass.core.module.payment.enums.PaymentMethod` (CASH/BANK_TRANSFER/MOMO/VNPAY/ZALOPAY/CREDIT_CARD) —
xem `documents/01-business/kiteclass/payment-invoice/api-contract.md`. Domain boundary cố ý
giữ tách bạch (subscription tier-payment ≠ school invoice/installment payment).

**GAP-739 (Wave beta-readiness-8 Bucket C 2026-05-25):** consolidated duplicate enum, synced FE union ↔ BE enum.

---

## POST /api/platform/subscriptions
**Use case:** UC-SUB-01
**Auth:** Bearer token (Owner)
**Request:**
```json
{
  "instanceId": "uuid",
  "tier": "BASIC",
  "billingCycle": "MONTHLY",
  "autoRenew": true
}
```
**Response 201:**
```json
{
  "id": "subscription-uuid",
  "instanceId": "instance-uuid",
  "tier": "FREE",
  "pendingTier": "BASIC",
  "billingCycle": "MONTHLY",
  "priceVnd": 500000,
  "status": "PENDING",
  "pendingPaymentId": "payment-uuid",
  "autoRenew": true,
  "startedAt": null,
  "expiresAt": null,
  "isActive": false,
  "isExpired": false
}
```
**Contract (SUB-20, Phase 1 BETA):** Create-first-paid áp dụng cùng pattern manual VietQR như PATCH /upgrade — subscription được tạo với `status=PENDING, tier=FREE, pendingTier=<requested>`, kèm Payment PENDING. Backend KHÔNG mark `status=ACTIVE` hoặc activate instance trước khi admin confirm payment. FE PHẢI redirect sang `/billing/payment/{pendingPaymentId}` hiển thị QR/thông tin chuyển khoản. Sau admin confirm payment (UC-SUB-07), backend gọi `applyPendingUpgrade` → tier flip sang `requested`, status flip ACTIVE, instance activate, subscription-created email gửi.

**Errors:** 400 FREE tier (`Cannot create subscription for FREE tier`), 409 duplicate active subscription

---

## GET /api/platform/subscriptions/{id}
**Auth:** Bearer token
**Response 200:** SubscriptionResponse object
**Errors:** 404 not found

---

## GET /api/platform/subscriptions/instance/{instanceId}/active
**Use case:** UC-SUB-01 (check current state)
**Auth:** Bearer token
**Response 200:** Active SubscriptionResponse
**Errors:** 404 no active subscription

---

## GET /api/platform/subscriptions/instance/{instanceId}
**Auth:** Bearer token
**Response 200:** `[SubscriptionResponse]` (all subscriptions, including history)

---

## PATCH /api/platform/subscriptions/{id}/upgrade
**Use case:** UC-SUB-02
**Auth:** Bearer token (Owner)
**Request:**
```json
{ "newTier": "PREMIUM" }
```
**Response 200:** Updated SubscriptionResponse with `pendingTier` + `pendingPaymentId`
```json
{
  "id": "subscription-uuid",
  "instanceId": "instance-uuid",
  "tier": "BASIC",
  "billingCycle": "MONTHLY",
  "priceVnd": 299000,
  "status": "ACTIVE",
  "startedAt": "2026-06-01T00:00:00",
  "expiresAt": "2026-07-01T00:00:00",
  "autoRenew": true,
  "pendingTier": "PREMIUM",
  "pendingPaymentId": "payment-uuid",
  "isActive": true,
  "isExpired": false
}
```
**Contract:** Phase 1 BETA upgrade does **not** apply the new tier before payment confirmation. FE must redirect to `/billing/payment/{pendingPaymentId}` when present. If `pendingPaymentId` is null (zero-amount/no-payment case), FE may return to `/billing` with success copy.

**Errors:**
- 400 invalid tier direction / invalid amount
- 404 subscription not found
- 409 existing pending payment for a different target tier (`error.code: "UPGRADE_PAYMENT_PENDING"`)

---

## PATCH /api/platform/subscriptions/{id}/downgrade
**Use case:** UC-SUB-03
**Auth:** Bearer token (Owner)
**Request:**
```json
{ "newTier": "BASIC" }
```
**Response 200:** Updated SubscriptionResponse with pendingTier set

---

## DELETE /api/platform/subscriptions/{id}
**Use case:** UC-SUB-04
**Auth:** Bearer token (Owner)
**Request params:** `?immediate=false` (default: end-of-cycle)
**Response 204:** No content

---

## POST /api/platform/subscriptions/{id}/renew
**Use case:** UC-SUB-05
**Auth:** Bearer token (Owner)
**Response 204:** No content
**Errors:** 404 not found

---

## GET /api/platform/subscriptions/expiring
**Auth:** Bearer token (Admin)
**Response 200:** `[SubscriptionResponse]` (expiring in next 30 days)

---

## Note: UC-SUB-06 — Automated Expiration Scheduler (no HTTP endpoint)

`SubscriptionExpirationChecker` runs daily (scheduler-triggered, no endpoint):

| Time | Action |
|------|--------|
| 9 AM | Scan ACTIVE subscriptions expiring in 7/3/1 days → send `renewal-reminder` emails |
| 10 AM | Mark expired ACTIVE subscriptions → `EXPIRED` |
| 10 AM | Suspend instances if grace period (3 days) elapsed (SUB-04) |

Monitor via `GET /api/platform/subscriptions/expiring` and instance status.

---

## GET /api/platform/payments
**Use case:** Admin payment ledger
**Auth:** Bearer token (Owner | Staff)

**Query params (offset pagination — default):**
- `status` (enum, optional) — `PENDING | COMPLETED | FAILED | CANCELLED`
- `page` (int, default `0`) — zero-based page index
- `size` (int, default `50`, max `200`) — auto-capped server-side, không throw 400
- Sort fixed `createdAt,desc`

**Query params (cursor pagination — Wave 85 Bucket D D-AC1, recommended cho dataset >1M rows):**
- `cursor` (string, opaque base64 của `id` row cuối từ page trước; mutually exclusive với `page`)
- `size` (int, default `50`, max `200`)
- Sort fixed `id ASC` khi cursor mode active

**Response 200 (offset mode):**
```json
{
  "content": [/* PaymentResponse[] */],
  "totalElements": 5432,
  "totalPages": 109,
  "page": 0,
  "size": 50,
  "first": true,
  "last": false
}
```

**Response 200 (cursor mode):**
```json
{
  "content": [/* PaymentResponse[] */],
  "size": 50,
  "nextCursor": "eyJpZCI6ImRlZi00NTYifQ==",
  "hasNext": true
}
```

**Errors:** 400 nếu truyền cả `page` lẫn `cursor`; 401 unauthenticated; 403 nếu role khác Owner/Staff.

**Performance note (GAP-432 Wave 41 + Wave 85 D-AC1):** trước Wave 41, endpoint này gọi `paymentRepository.findAll()` (full-table scan) → đã bound Pageable với default 50 + max 200 hard cap. Cursor mode khuyến nghị cho admin payment ledger khi tenant base >100 trung tâm × 1000 payments/tháng → vượt 1M rows trong 12 tháng.

---

## GET /api/platform/payments/{id}
**Auth:** Bearer token (Owner | Staff)
**Response 200:** PaymentResponse
```json
{
  "id": "payment-uuid",
  "subscriptionId": "subscription-uuid",
  "amountVnd": 120000,
  "currency": "VND",
  "paymentMethod": "VIETQR",
  "status": "PENDING",
  "qrCodeUrl": "https://img.vietqr.io/image/...",
  "transactionId": null,
  "bankCode": "VCB",
  "accountNumber": "1234567890",
  "accountName": "CONG TY KITECLASS",
  "paymentContent": "KITEHUB ABCD1234",
  "paidAt": null,
  "createdAt": "2026-06-04T09:30:00",
  "updatedAt": "2026-06-04T09:30:00"
}
```
**Errors:** 404 not found

---

## GET /api/platform/payments/{id}/qr-code
**Use case:** UC-SUB-02
**Auth:** Bearer token (Owner | Staff)
**Response 200:**
```json
{ "qrCodeUrl": "https://img.vietqr.io/image/..." }
```
**Fallback:** Nếu VietQR API fail, backend có thể trả public VietQR image URL từ bank/account/paymentContent đã cấu hình. FE vẫn hiển thị manual bank info từ PaymentResponse.

---

## GET /api/platform/payments/subscription/{subscriptionId}
**Auth:** Bearer token
**Response 200:** `[PaymentResponse]` (lịch sử payment của subscription cụ thể — bounded by FK)

---

## Admin endpoints — authentication note (GAP-938, Wave flow-kh3)

> Tất cả admin endpoint dưới đây (`/api/platform/admin/**`) yêu cầu **JWT với role `PLATFORM_ADMIN`** forward qua gateway. Gateway extract role từ JWT và set header `X-User-Id` + `X-User-Roles` cho downstream services. Spring Security trong `kitehub-subscription` đọc header, map sang `ROLE_PLATFORM_ADMIN` và enforce qua `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` ở mỗi handler.
>
> Cơ chế `X-Admin-Key` cũ (qua `AdminApiKeyInterceptor`) đã bị xóa trong PR GAP-938. Wave 79 default-deny migration khiến interceptor đó trở thành dead code (Spring Security block request trước khi interceptor chạy), và việc giữ lại tạo ra surface attack thừa cộng với drift giữa doc và code.

---

## GET /api/platform/admin/payments/pending
**Use case:** UC-SUB-07
**Auth:** JWT với role `PLATFORM_ADMIN` (gateway forward `X-User-Roles`)
**Response 200:** `[PaymentResponse]` pending payments cần đối soát thủ công.

---

## POST /api/platform/admin/payments/{id}/confirm
**Use case:** UC-SUB-07
**Auth:** JWT với role `PLATFORM_ADMIN` (gateway forward `X-User-Roles`)
**Request:**
```json
{ "transactionId": "VCB-20260604-000123" }
```
**Response 200:** PaymentResponse with `status=COMPLETED`, `transactionId`, `paidAt` set.
**Side effect:** Nếu payment thuộc upgrade flow, subscription áp dụng `pendingTier`, cập nhật `priceVnd`, clear `pendingTier` + `pendingPaymentId`.
**Errors:** 400 missing transactionId; 401 thiếu/invalid JWT; 403 user không có role `PLATFORM_ADMIN`; 404 payment not found; 409 payment not PENDING.

---

## POST /api/platform/admin/payments/{id}/reject
**Use case:** UC-SUB-07
**Auth:** JWT với role `PLATFORM_ADMIN` (gateway forward `X-User-Roles`)
**Request:**
```json
{ "reason": "Không khớp statement ngân hàng hoặc sai nội dung chuyển khoản" }
```
**Response 200:** PaymentResponse with `status=FAILED`.
**Side effect:** Subscription giữ tier hiện tại; pending state được clear để owner tạo yêu cầu thanh toán mới sạch.
**Errors:** 400 missing reason; 401 thiếu/invalid JWT; 403 user không có role `PLATFORM_ADMIN`; 404 payment not found; 409 payment not PENDING.
