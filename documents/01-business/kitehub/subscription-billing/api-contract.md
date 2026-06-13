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

## GET /api/platform/subscriptions/instance/{instanceId}/pending-payment-status
**Use case:** UC-SUB-07 / owner "đang chờ xác nhận" screen (GAP-1257-BE)
**Auth:** Bearer token (Owner | Staff) — `X-Tenant-Id` bound to the instance (`TenantOwnershipGuard`)
**Response 200:**
```json
{
  "hasPendingPayment": true,
  "subscriptionId": "subscription-uuid",
  "pendingPaymentId": "payment-uuid",
  "amount": 500000,
  "currency": "VND",
  "status": "PENDING",
  "tier": "BASIC",
  "createdAt": "2026-06-13T09:00:00",
  "expiresAt": "2026-06-14T09:00:00",
  "adminConfirmSlaHours": 24
}
```
**Contract:** Reads the instance's in-flight pending payment (the subscription holding `pendingPaymentId`).
`expiresAt` is the derived admin-confirm SLA deadline = `payment.createdAt + adminConfirmSlaHours`
(SUB-19 admin confirm is the capture source). When no payment is in flight, returns
`{ "hasPendingPayment": false, "adminConfirmSlaHours": 24 }`. FE polls this for the waiting screen.
**Config:** `kitehub.payment.admin-confirm-sla-hours` (default `24`).
**Errors:** 403 cross-tenant.

---

## GET /api/platform/subscriptions/instance/{instanceId}/downgrade-preview
**Use case:** UC-SUB-03 over-cap impact preview (GAP-1261)
**Auth:** Bearer token (Owner | Staff) — tenant-bound
**Query params:** `targetTier` (enum `FREE|BASIC|PREMIUM|ENTERPRISE`, required) — must be strictly lower than current tier
**Response 200:**
```json
{
  "currentTier": "PREMIUM",
  "targetTier": "BASIC",
  "currentMaxStudents": 200,
  "targetMaxStudents": 50,
  "currentMaxTeachers": 20,
  "targetMaxTeachers": 5,
  "currentStorageMb": 10240,
  "targetStorageMb": 2048,
  "customDomainCurrentlyAllowed": true,
  "customDomainTargetAllowed": false,
  "customDomainWillBeDisabled": true,
  "hasActiveCustomDomain": true,
  "warnings": ["Gói BASIC giới hạn 50 học sinh (gói hiện tại PREMIUM cho 200).", "..."],
  "usageDataNote": "Số liệu sử dụng thực tế ... so sánh giới hạn (cap) ..."
}
```
**Contract:** Compares entitlement caps of the current vs target tier (from `PricingTier`) + flags the
real custom-domain loss (read from `instances.custom_domain`). Live usage counters (students/storage
used) live in the per-tenant kiteclass-core DB and are NOT available here — `usageDataNote` documents
this; the owner compares the shrunk caps against their own known usage.
**Errors:** 400 `targetTier` missing OR not strictly lower than current; 403 cross-tenant; 404 instance not found.

---

## POST /api/platform/subscriptions/instance/{instanceId}/reactivate
**Use case:** UC-SUB-04 win-back reactivation (GAP-1263-BE)
**Auth:** Bearer token (Owner) — tenant-bound
**Response 200:**
```json
{
  "instanceId": "instance-uuid",
  "outcome": "PAYMENT_REQUIRED",
  "churnType": "INVOLUNTARY",
  "subscriptionId": "subscription-uuid",
  "pendingPaymentId": "payment-uuid",
  "amount": 500000,
  "currency": "VND",
  "message": "Vui lòng thanh toán để kích hoạt lại trung tâm..."
}
```
**Contract:** Phase 1 BETA manual-VietQR gate (mirrors GAP-1016 manual renewal). For a SUSPENDED
instance, creates a PENDING reactivation payment + sets `subscription.pendingPaymentId`; the instance
flips back to ACTIVE only after admin confirm (existing `applyConfirmedRenewal` path). **Idempotent** —
a repeat call while a reactivation payment is in flight returns the same payment.
- `outcome`: `PAYMENT_REQUIRED` (suspended, payment created/returned) | `ALREADY_ACTIVE` (idempotent no-op) | `NO_SUBSCRIPTION` (no subscription to revive → create a fresh one).
- `churnType`: `VOLUNTARY` (subscription CANCELLED) | `INVOLUNTARY` (non-payment lapse, SUB-24) | `NONE`.
- PURGED instance → 409 (data removed, create new); DELETED instance (fraud-block / admin tombstone) → 409 (contact support). Distinguishes the fraud tombstone from a voluntary cancel (merely SUSPENDED → reactivatable).
**Errors:** 403 cross-tenant; 404 instance not found; 409 tombstone (PURGED/DELETED) OR never-activated (PENDING).

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
  "txnRef": "KH3SUB1A2B3C4D",
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

## GET /api/platform/payments/{id}/receipt
**Use case:** UC-SUB-07 non-VAT receipt / biên nhận (GAP-1266)
**Auth:** Bearer token (Owner | Staff)
**Response 200:**
```json
{
  "receiptNumber": "BN-2026-1A2B3C4D",
  "paymentId": "payment-uuid",
  "subscriptionId": "subscription-uuid",
  "instanceId": "instance-uuid",
  "organizationName": "Trung tâm Demo",
  "tier": "BASIC",
  "billingCycle": "MONTHLY",
  "amountVnd": 500000,
  "currency": "VND",
  "paymentMethod": "VIETQR",
  "transactionId": "VCB-20260613-001",
  "paidAt": "2026-06-13T10:00:00",
  "issuedAt": "2026-06-13T10:05:00",
  "note": "Đây là biên nhận thanh toán (không phải hóa đơn GTGT)..."
}
```
**Contract:** Phase 1 BETA **non-VAT** receipt (biên nhận), NOT a VAT e-invoice (hóa đơn GTGT —
deferred to MISA MeInvoice partnership GAP-185/634). Derived on-demand from the completed payment
row (no separate storage). Available only after the payment is COMPLETED. `receiptNumber` is
deterministic: `BN-<year>-<8 uppercase hex of payment id>`. Also emailed to the owner on payment
confirm (template `payment-confirmed`).
**Errors:** 400 payment not COMPLETED; 404 payment not found.

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

---

## POST /api/platform/webhooks/payment

**Use case:** UC-SUB-08 — SePay payment notification webhook (Wave flow-kh3-2)
**Auth:** `Authorization: Apikey <kitehub.payment.sepay.api-key>` header (NOT JWT, NOT HMAC body-signature)
**Source:** SePay merchant gateway (https://sepay.vn) — Free 50tx/tháng tier covers Phase 1 BETA.
**Idempotency:** webhook PHẢI idempotent on `id` (SePay transaction ID) — replay same `id` → HTTP 200 + early-return no double-process. Backed by UNIQUE constraint on `payments.transaction_id`.

**Request (SePay payload shape — strict):**
```json
{
  "id": 92704902,
  "gateway": "Vietcombank",
  "transactionDate": "2026-06-04 09:30:01",
  "accountNumber": "1234567890",
  "subAccount": null,
  "code": null,
  "content": "KH3SUB1A2B3C4D Thanh toan goi BASIC",
  "transferType": "in",
  "description": "BankAPINotify KH3SUB1A2B3C4D Thanh toan goi BASIC",
  "transferAmount": 10000,
  "referenceCode": "FT26152709876543",
  "accumulated": 0
}
```

**Matching logic:**
1. Verify `transferType == "in"` (else 200 + ignore — outbound transaction)
2. Extract `txnRef` from `description` (regex `KH3SUB[A-F0-9]{8}`)
3. `findPaymentByTxnRef(txnRef)` — exact-match query, NOT `LIKE %?%` substring (cross-tenant collision guard per `pre-handoff-self-test-completeness.md` §2.6 row d)
4. Verify `transferAmount == payment.amountVnd` (else 400 amount mismatch)
5. Check idempotency: if `payment.transaction_id == sepay.id` → HTTP 200 early-return
6. `payment.complete(sepay.id)` + `subscriptionService.applyPendingUpgrade(subscriptionId, paymentId)` (existing state machine)

**Response 200:** `{"status":"success"}` (always 200 for valid Apikey; logic errors logged not surfaced — SePay retries on non-200 → idempotency mandatory)

**Errors:**
- 401: missing/invalid `Authorization: Apikey` header
- 400: malformed JSON, missing `transferAmount`, missing `description`
- 400: `txnRef` extracted but `findPaymentByTxnRef` returns empty (orphan payment notify)
- 400: amount mismatch (payment exists but `transferAmount != payment.amountVnd`)

**Config keys (kitehub-subscription `application.yml`):**
| Key | Type | Default | Description |
|---|---|---|---|
| `kitehub.payment.sepay.api-key` | string | (empty — REQUIRED for prod) | SePay API key configured in SePay dashboard → KH webhook URL |
| `kitehub.payment.sepay.webhook-path` | string | `/api/platform/webhooks/payment` | Endpoint path (informational, not used at runtime) |
| `kitehub.payment.beta-mode.enabled` | boolean | `false` | When `true`, override payment.amountVnd to `override-amount-vnd` at createPayment time (Phase 1 BETA symbolic transfer) |
| `kitehub.payment.beta-mode.override-amount-vnd` | long | `10000` | Symbolic amount in VND (bank minimum is 1k; 10k chosen per failure-mode audit 2026-06-04 — VCB/MBB/TCB accept) |

**FE consumption note (Bucket D):** FE subscribes WebSocket `/topic/payments/{paymentId}` and receives `paymentCompleted` event after webhook flips Payment.status → COMPLETED. Display BetaModeBanner when `NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE=true` (FE env flag mirrors BE `beta-mode.enabled`).

**Cross-references:**
- Bucket A (GAP-975): adds `Payment.txnRef` field + V64 migration (`payments.txn_ref VARCHAR(32) UNIQUE`) + extends `createPayment` with beta-amount override
- Bucket B (GAP-976): rewrites existing `PaymentWebhookController` from generic HMAC body-signature → SePay `Authorization: Apikey` header + payload field adapter
- Bucket C (GAP-974): `applyPendingUpgrade` emits `SUBSCRIPTION_ACTIVATED` outbox event → `subscription-activated.html` email arrives MailHog/Resend
- Bucket D (GAP-977): FE WS subscribe + BetaModeBanner conditional render

---

## Notification channel seam + in-app notifications (GAP-1265)

KiteHub owner notifications flow through a thin `NotificationChannel` abstraction (email is not the
only channel). Phase 1 wires **EMAIL** (primary, via `EmailServiceClient` outbox path) + **IN_APP**
(durable persistent-banner fallback, table `in_app_notifications`). SMS / **Zalo OA** / PUSH are
documented stubs deferred to GAP-063b — a future bean implements `NotificationChannel` + registers,
and `OwnerNotificationDispatcher` picks it up automatically (no full Zalo build in this scope).

**Side effect — payment confirm (GAP-1257-BE + GAP-1266):** every confirm path
(`POST /admin/payments/{id}/confirm`, SePay webhook, legacy gateway webhook) dispatches a
`payment-confirmed` notification (email `payment-confirmed` template + in-app banner) carrying the
non-VAT receipt summary. Best-effort: a notify failure never blocks payment capture.

**Win-back (GAP-1263-BE):** `OwnerNotificationDispatcher.sendWinBack(instance, voluntary)` ships the
`winback-reactivate` email + banner (CTA → reactivate). Provided as a seam for the suspend/cancel
scheduler paths (cross-bucket handoff) — the reactivate endpoint above is the CTA target.

### GET /api/platform/notifications/in-app/instance/{instanceId}
**Auth:** Bearer token (Owner | Staff) — tenant-bound
**Query params:** `unreadOnly` (boolean, default `false`)
**Response 200:** `[InAppNotificationResponse]` (newest-first) — `{ id, notificationType, title, body, actionUrl, read, createdAt, readAt }`

### PATCH /api/platform/notifications/in-app/instance/{instanceId}/{notificationId}/read
**Auth:** Bearer token (Owner | Staff) — tenant-bound
**Response 200:** `InAppNotificationResponse` with `read=true`
**Errors:** 403 cross-tenant; 404 notification not found for this instance
