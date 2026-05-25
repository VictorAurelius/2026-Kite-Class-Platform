---
domain: payment-invoice
project: kiteclass
audience: mixed
last-updated: 2026-05-26
version: 1.1.0
---

# Payment & Invoice — API Contract

> Source-of-truth: 5 controllers tại `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{payment,invoice}/controller/`
> Tổng số endpoint: **31** (8 PaymentController + 3 PaymentWebhookController + 10 InvoiceController + 5 RefundRequestController + 5 InstallmentPlanController)
> Wave br-6 Bucket A (2026-05-26) — sync với 5 controllers verified empirical; closes GAP-231.

---

## 0. Endpoint index (drift detector compatible)

Bảng này phục vụ `scripts/check-cross-layer-contract-drift.sh` heuristic v1 — leading-`|` table format ensures detector grep `^\|?\s*(GET|POST|PUT|DELETE|PATCH)?\s*/api/...` match. Chi tiết schema xem §3-§7.

| Endpoint declaration |
|---|
| GET /api/v1/invoices/{id} |
| GET /api/v1/invoices/{id}/items |
| GET /api/v1/invoices/student/{studentId} |
| POST /api/v1/invoices/{id}/adjustments |
| POST /api/v1/invoices/{id}/late-fees |
| GET /api/v1/invoices/overdue |
| GET /api/v1/invoices/student/{studentId}/unpaid |
| GET /api/v1/invoices/student/{studentId}/overdue |
| POST /api/v1/invoices/{id}/mark-paid |
| PUT /api/v1/invoices/{id}/cancel |
| POST /api/v1/payments |
| POST /api/v1/payments/installments |
| GET /api/v1/payments/{id} |
| GET /api/v1/payments/invoice/{invoiceId} |
| GET /api/v1/payments/pending |
| PUT /api/v1/payments/{id}/cancel |
| POST /api/v1/payments/{id}/refund |
| GET /api/v1/payments/{id}/status |
| GET /api/v1/payments/webhook/vnpay |
| POST /api/v1/payments/webhook/momo |
| POST /api/v1/payments/webhook/zalopay |
| POST /api/v1/installment-plans |
| GET /api/v1/installment-plans/{id} |
| PUT /api/v1/installment-plans/{id}/approve |
| PUT /api/v1/installment-plans/{id}/reject |
| POST /api/v1/installment-plans/installments/{installmentId}/payment |
| POST /api/v1/refund-requests |
| GET /api/v1/refund-requests/{id} |
| PUT /api/v1/refund-requests/{id}/approve |
| PUT /api/v1/refund-requests/{id}/reject |
| POST /api/v1/refund-requests/{id}/process |

---

## 1. Enums

### 1.1 PaymentMethod (canonical — kiteclass school payment domain)

Single source of truth: `com.kiteclass.core.module.payment.enums.PaymentMethod` (Java) ↔ `kiteclass-frontend/src/types/payment.ts` enum (TypeScript).

| Value | Vietnamese label | Online gateway? | Use case |
|---|---|---|---|
| `CASH` | Tiền mặt | ❌ (offline) | Phụ huynh nộp tiền mặt tại trung tâm |
| `BANK_TRANSFER` | Chuyển khoản | ❌ (offline) | Phụ huynh chuyển khoản, admin verify |
| `MOMO` | Ví MoMo | ✅ | E-wallet — quét QR thanh toán |
| `VNPAY` | VNPay | ✅ | Payment gateway VN |
| `ZALOPAY` | ZaloPay | ✅ | E-wallet ZaloPay |
| `CREDIT_CARD` | Thẻ tín dụng | ✅ | Visa/MasterCard (Phase 2+) |

**Note:** đây là PaymentMethod **school payment** scope (invoice + installment cho học phí học sinh). KiteHub subscription billing dùng enum riêng `com.kitehub.platform.domain.enums.PaymentMethod` (VIETQR/MOMO/VNPAY/BANK_TRANSFER/MANUAL) — xem `documents/01-business/kitehub/subscription-billing/api-contract.md`. Domain boundary cố ý giữ tách bạch (school invoice payment ≠ subscription tier payment).

**GAP-739 (Wave beta-readiness-8 Bucket C 2026-05-25):** removed orphan duplicate `com.kiteclass.core.common.constant.PaymentMethod` (zero consumers), synced FE union ↔ BE enum.

### 1.2 PaymentStatus

`com.kiteclass.core.module.payment.enums.PaymentStatus`:

| Value | Mô tả |
|---|---|
| `PENDING` | Payment đã khởi tạo, chưa xử lý bởi gateway |
| `PROCESSING` | Gateway đang xử lý payment |
| `COMPLETED` | Payment đã thành công |
| `FAILED` | Payment thất bại hoặc bị hủy |
| `REFUNDED` | Payment đã được hoàn tiền |

### 1.3 InvoiceStatus

`com.kiteclass.core.common.constant.InvoiceStatus`:

| Value | Mô tả |
|---|---|
| `DRAFT` | Hoá đơn nháp, chưa gửi |
| `SENT` | Đã gửi cho phụ huynh, chờ thanh toán |
| `PAID` | Đã thanh toán đầy đủ |
| `PARTIAL` | Đã thanh toán một phần (còn balance) |
| `OVERDUE` | Quá hạn thanh toán |
| `CANCELLED` | Đã hủy |
| `REFUNDED` | Đã hoàn tiền |

### 1.4 InvoiceItemType

`com.kiteclass.core.common.constant.InvoiceItemType`:

| Value | Mô tả |
|---|---|
| `TUITION` | Học phí |
| `MATERIALS` | Tài liệu, sách giáo trình |
| `REGISTRATION_FEE` | Phí ghi danh |
| `EXAM_FEE` | Lệ phí thi |
| `OTHER` | Khoản khác |

### 1.5 InvoiceAdjustmentType

`com.kiteclass.core.common.constant.InvoiceAdjustmentType`:

| Value | Mô tả | Amount sign |
|---|---|---|
| `DISCOUNT` | Giảm giá | Negative |
| `LATE_FEE` | Phí quá hạn | Positive |
| `ADDITIONAL_CHARGE` | Phụ phí | Positive |
| `REFUND` | Hoàn tiền | Negative |

### 1.6 InstallmentPlanStatus

`com.kiteclass.core.common.constant.InstallmentPlanStatus`:

| Value | Mô tả |
|---|---|
| `PENDING` | Chờ admin duyệt |
| `APPROVED` | Đã duyệt, sẵn sàng thanh toán |
| `REJECTED` | Bị từ chối |
| `ACTIVE` | Đang trong quá trình thanh toán |
| `COMPLETED` | Đã hoàn tất các đợt thanh toán |
| `CANCELLED` | Đã hủy |

### 1.7 InstallmentStatus

`com.kiteclass.core.common.constant.InstallmentStatus`:

| Value | Mô tả |
|---|---|
| `PENDING` | Chờ thanh toán |
| `PAID` | Đã thanh toán |
| `OVERDUE` | Quá hạn |
| `CANCELLED` | Đã hủy |

### 1.8 RefundStatus

`com.kiteclass.core.common.constant.RefundStatus`:

| Value | Mô tả |
|---|---|
| `PENDING` | Chờ admin duyệt |
| `APPROVED` | Đã duyệt, chờ xử lý hoàn tiền |
| `REJECTED` | Bị từ chối |
| `COMPLETED` | Đã hoàn tiền |
| `CANCELLED` | Đã hủy |

---

## 2. Common conventions

- **Base path:** `/api/v1/{resource}` (`payments`, `invoices`, `installment-plans`, `refund-requests`)
- **Authentication:** `Bearer JWT` qua header `Authorization`; userId được resolve qua `UserContext` (populated bởi `TenantFilterInterceptor` từ Gateway header `X-User-Id`)
- **Authorization:** đa số endpoint yêu cầu authenticated user. Admin endpoints (`/approve`, `/reject`, `/process`, `/mark-paid`) mặc định yêu cầu vai trò admin/finance (per `rules.md`). Webhook endpoints `/payments/webhook/*` PUBLIC — không auth, validate bằng signature.
- **Response envelope:** đa số endpoint trả `ApiResponse<T>` shape `{ success, data, error }` (per `kiteclass-core/.../common/dto/ApiResponse`). Một số endpoint (Refund + Installment) trả về raw DTO trực tiếp (không bọc envelope) — tham khảo từng endpoint.
- **Pagination:** endpoint list dùng Spring `Pageable` (query params `page`, `size`, `sort`), trả `Page<T>` (`content[]`, `totalElements`, `totalPages`, `number`, `size`).
- **Tiền tệ:** mọi field `BigDecimal` dùng VND, định dạng `1.500.000đ` khi hiển thị FE; truyền JSON là số thập phân (`1500000.00`).
- **Date:** `LocalDate` → `YYYY-MM-DD`; `LocalDateTime` → `YYYY-MM-DDTHH:mm:ss`.
- **Error envelope:** xem §7.

---

## 3. Invoice endpoints (10)

### 3.1 `GET /api/v1/invoices/{id}`

Lấy hoá đơn theo ID.

- **Auth:** Bearer JWT
- **Path params:** `id` (Long, required) — invoice ID
- **Response:** `200 ApiResponse<InvoiceResponse>` (xem §5.1)
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND` (invoice không tồn tại hoặc khác tenant)

### 3.2 `GET /api/v1/invoices/{id}/items`

Lấy danh sách line items của hoá đơn.

- **Auth:** Bearer JWT
- **Path params:** `id` (Long, required) — invoice ID
- **Response:** `200 ApiResponse<List<InvoiceItemResponse>>` (xem §5.2)
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND`

### 3.3 `GET /api/v1/invoices/student/{studentId}`

Lấy tất cả hoá đơn của một học sinh, paginated.

- **Auth:** Bearer JWT
- **Path params:** `studentId` (Long, required)
- **Query params:** `page` (Integer, default 0), `size` (Integer, default 20), `sort` (String, optional — vd `issueDate,desc`)
- **Response:** `200 ApiResponse<Page<InvoiceResponse>>`
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND` (student không tồn tại)

### 3.4 `POST /api/v1/invoices/{id}/adjustments`

Áp dụng adjustment (giảm giá / phụ phí / hoàn tiền) cho hoá đơn.

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required) — invoice ID
- **Request body:** `ApplyAdjustmentRequest` (xem §6.1)
- **Response:** `200 ApiResponse<InvoiceResponse>` — invoice sau khi cập nhật
- **Errors:** `400 VALIDATION_ERROR` (DTO sai), `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 INVOICE_NOT_ADJUSTABLE` (vd invoice CANCELLED)

### 3.5 `POST /api/v1/invoices/{id}/late-fees`

Tính và áp phí quá hạn cho hoá đơn.

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Request body:** none
- **Response:** `200 ApiResponse<InvoiceResponse>` — invoice sau khi áp late fee
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 INVOICE_NOT_OVERDUE` (invoice chưa quá hạn)

### 3.6 `GET /api/v1/invoices/overdue`

Lấy danh sách hoá đơn quá hạn (toàn tenant), paginated.

- **Auth:** Bearer JWT (admin/finance role)
- **Query params:** `page`, `size`, `sort`
- **Response:** `200 ApiResponse<Page<InvoiceResponse>>`
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`

### 3.7 `GET /api/v1/invoices/student/{studentId}/unpaid`

Lấy hoá đơn chưa thanh toán của một học sinh, paginated.

- **Auth:** Bearer JWT
- **Path params:** `studentId` (Long, required)
- **Query params:** `page`, `size`, `sort`
- **Response:** `200 ApiResponse<Page<InvoiceResponse>>`
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND`

### 3.8 `GET /api/v1/invoices/student/{studentId}/overdue`

Lấy hoá đơn quá hạn của một học sinh, paginated.

- **Auth:** Bearer JWT
- **Path params:** `studentId` (Long, required)
- **Query params:** `page`, `size`, `sort`
- **Response:** `200 ApiResponse<Page<InvoiceResponse>>`
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND`

### 3.9 `POST /api/v1/invoices/{id}/mark-paid`

Đánh dấu hoá đơn đã thanh toán thủ công (vd admin xác nhận chuyển khoản).

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Request body:** none
- **Response:** `200 ApiResponse<InvoiceResponse>` — invoice status flip → PAID
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 INVOICE_ALREADY_PAID` (đã PAID)

### 3.10 `PUT /api/v1/invoices/{id}/cancel`

Hủy hoá đơn.

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Response:** `200 ApiResponse<InvoiceResponse>` — invoice status flip → CANCELLED
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 INVOICE_NOT_CANCELLABLE` (vd đã PAID)

---

## 4. Payment endpoints (8)

### 4.1 `POST /api/v1/payments`

Khởi tạo thanh toán cho một hoá đơn.

- **Auth:** Bearer JWT
- **Request body:** `CreatePaymentRequest` (xem §6.2)
- **Response:** `201 ApiResponse<PaymentResponse>` — bao gồm `paymentUrl` (cho online gateway), `qrCodeUrl` (cho MoMo/ZaloPay)
- **Errors:** `400 VALIDATION_ERROR`, `401 UNAUTHENTICATED` (`AUTH_REQUIRED`), `404 INVOICE_NOT_FOUND`, `409 INVOICE_NOT_PAYABLE` (đã PAID hoặc CANCELLED), `409 AMOUNT_EXCEEDS_BALANCE`, `502 PAYMENT_GATEWAY_TIMEOUT` (gateway lỗi)

**FE side-effect:** với online method (VNPAY/MOMO/ZALOPAY/CREDIT_CARD), FE redirect user tới `paymentUrl` hoặc hiển thị QR từ `qrCodeUrl`.

### 4.2 `POST /api/v1/payments/installments`

Thanh toán một đợt installment cụ thể.

- **Auth:** Bearer JWT
- **Request body:** `CreateInstallmentPaymentRequest` (xem §6.3)
- **Response:** `201 ApiResponse<PaymentResponse>`
- **Errors:** giống §4.1 nhưng thay `INVOICE_NOT_FOUND` → `INSTALLMENT_NOT_FOUND`; `409 INSTALLMENT_NOT_DUE` (chưa đến hạn), `409 INSTALLMENT_ALREADY_PAID`

### 4.3 `GET /api/v1/payments/{id}`

Lấy payment theo ID.

- **Auth:** Bearer JWT
- **Path params:** `id` (Long, required) — payment ID
- **Response:** `200 ApiResponse<PaymentResponse>`
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND`

### 4.4 `GET /api/v1/payments/invoice/{invoiceId}`

Lấy tất cả payments của một hoá đơn.

- **Auth:** Bearer JWT
- **Path params:** `invoiceId` (Long, required)
- **Response:** `200 ApiResponse<List<PaymentResponse>>`
- **Errors:** `401 UNAUTHENTICATED`, `404 INVOICE_NOT_FOUND`

### 4.5 `GET /api/v1/payments/pending`

Lấy danh sách payments đang ở trạng thái PENDING (toàn tenant), paginated.

- **Auth:** Bearer JWT (admin/finance role)
- **Query params:** `page`, `size`, `sort`
- **Response:** `200 ApiResponse<Page<PaymentResponse>>`
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`

### 4.6 `PUT /api/v1/payments/{id}/cancel`

Hủy một payment đang PENDING.

- **Auth:** Bearer JWT
- **Path params:** `id` (Long, required)
- **Response:** `204 No Content` (no body)
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND`, `409 PAYMENT_NOT_CANCELLABLE` (đã COMPLETED/FAILED/REFUNDED)

### 4.7 `POST /api/v1/payments/{id}/refund`

Xử lý refund cho một payment đã COMPLETED.

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Response:** `204 No Content`
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 PAYMENT_NOT_REFUNDABLE` (chưa COMPLETED hoặc đã REFUNDED), `502 PAYMENT_GATEWAY_TIMEOUT`

### 4.8 `GET /api/v1/payments/{id}/status`

Truy vấn realtime trạng thái payment từ gateway.

- **Auth:** Bearer JWT
- **Path params:** `id` (Long, required)
- **Response:** `200 ApiResponse<PaymentStatusResponse>` — `{ status: PaymentStatus }`
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND`, `502 PAYMENT_GATEWAY_TIMEOUT`

---

## 5. Webhook endpoints (3) — PUBLIC, no auth

Webhooks PHẢI verify signature/MAC trước khi update payment status. PaymentService dispatch dựa trên `PaymentMethod`. Webhook handler trả thành công ngay cả khi business processing fail, tránh gateway retry vô tận (gateway sẽ retry nếu HTTP != 2xx).

### 5.1 `GET /api/v1/payments/webhook/vnpay`

VNPay return callback (synchronous redirect — VNPay dùng GET với query params).

- **Auth:** PUBLIC — signature verify qua param `vnp_SecureHash`
- **Query params:** `Map<String, String>` toàn bộ params từ VNPay (`vnp_TxnRef`, `vnp_Amount`, `vnp_ResponseCode`, `vnp_SecureHash`, ...)
- **Response:**
  - `200 "success"` (text/plain) khi xử lý OK
  - `500 "error"` khi exception (PaymentService.processWebhookCallback throw)
- **Signature verification:** HMAC-SHA512 với secret từ config `vnpay.hash-secret`; compare `vnp_SecureHash` param.
- **Idempotency:** payment được lookup qua `vnp_TxnRef`; duplicate callback sẽ no-op (status đã updated).

### 5.2 `POST /api/v1/payments/webhook/momo`

MoMo IPN callback (async POST từ MoMo server).

- **Auth:** PUBLIC — signature verify qua field `signature` trong body
- **Request body:** `MomoCallbackRequest` — `partnerCode`, `orderId`, `requestId`, `amount`, `orderInfo`, `orderType`, `transId`, `resultCode`, `message`, `payType`, `responseTime`, `extraData`, `signature` (+ `extraParams` capture các field bổ sung qua `@JsonAnySetter`)
- **Response:**
  - `200 { "message": "success" }`
  - `500 { "message": "error" }`
- **Signature verification:** HMAC-SHA256 với secret key MoMo (config `momo.secret-key`); compare field `signature`.
- **Idempotency:** payment lookup qua `orderId`; duplicate no-op.

### 5.3 `POST /api/v1/payments/webhook/zalopay`

ZaloPay callback (async POST).

- **Auth:** PUBLIC — MAC verify qua field `mac` trong body
- **Request body:** `ZalopayCallbackRequest` — `data` (JSON string chứa transaction info), `mac` (HMAC), `type`, `app_id` (mapped `appId`), `app_trans_id` (mapped `appTransId`)
- **Response:**
  - `200 { "return_code": 1 }` (thành công)
  - `500 { "return_code": 0 }` (lỗi)
- **MAC verification:** HMAC-SHA256 với key2 từ config `zalopay.key2`; compare `mac` với `HMAC(data, key2)`.
- **Idempotency:** payment lookup qua `app_trans_id`; duplicate no-op.

---

## 6. Installment plan endpoints (5)

### 6.1 `POST /api/v1/installment-plans`

Học sinh / phụ huynh yêu cầu installment plan cho một invoice.

- **Auth:** Bearer JWT
- **Request body:** `CreateInstallmentPlanRequest` — `invoiceId` (Long, required, positive), `numberOfInstallments` (Integer, required, 2-12)
- **Response:** `201 InstallmentPlanResponse` (raw — không bọc `ApiResponse`)
- **Errors:** `400 VALIDATION_ERROR`, `401 UNAUTHENTICATED`, `404 INVOICE_NOT_FOUND`, `409 INSTALLMENT_NOT_ALLOWED` (vd invoice đã PAID, hoặc đã có plan PENDING/APPROVED)

### 6.2 `GET /api/v1/installment-plans/{id}`

Lấy chi tiết installment plan kèm lịch trình các đợt.

- **Auth:** Bearer JWT
- **Path params:** `id` (Long, required) — plan ID
- **Response:** `200 InstallmentPlanResponse` (raw, include `installments[]`)
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND`

### 6.3 `PUT /api/v1/installment-plans/{id}/approve`

Admin duyệt installment plan.

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Query params:** `approvedBy` (Long, required) — user ID admin
- **Response:** `200 InstallmentPlanResponse` (status → APPROVED, populate `approvedAt`, `approvedBy`)
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 PLAN_NOT_PENDING` (status không phải PENDING)

### 6.4 `PUT /api/v1/installment-plans/{id}/reject`

Admin từ chối installment plan.

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Query params:** `reason` (String, required) — lý do từ chối
- **Response:** `200 InstallmentPlanResponse` (status → REJECTED, populate `rejectionReason`)
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 PLAN_NOT_PENDING`

### 6.5 `POST /api/v1/installment-plans/installments/{installmentId}/payment`

Ghi nhận thanh toán cho một đợt installment (manual recording — vd admin nhập sau khi nhận chuyển khoản).

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `installmentId` (Long, required)
- **Query params:** `amount` (BigDecimal, required) — số tiền đã thanh toán
- **Response:** `200 InstallmentPlanResponse` (toàn bộ plan với installment cập nhật)
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 INSTALLMENT_NOT_FOUND`, `409 INSTALLMENT_ALREADY_PAID`, `400 AMOUNT_EXCEEDS_DUE`

**Note:** endpoint này khác `POST /api/v1/payments/installments` (§4.2) — endpoint kia khởi tạo payment qua gateway, endpoint này chỉ record thanh toán đã xảy ra offline.

---

## 7. Refund request endpoints (5)

### 7.1 `POST /api/v1/refund-requests`

Học sinh / phụ huynh yêu cầu hoàn tiền.

- **Auth:** Bearer JWT
- **Request body:** `CreateRefundRequestRequest` — `invoiceId` (Long, required, positive), `refundAmount` (BigDecimal, required, positive), `reason` (String, required, not blank)
- **Response:** `201 RefundRequestResponse` (raw — không bọc `ApiResponse`)
- **Errors:** `400 VALIDATION_ERROR`, `401 UNAUTHENTICATED`, `404 INVOICE_NOT_FOUND`, `409 REFUND_AMOUNT_EXCEEDS_PAID` (refund > amount paid)

### 7.2 `GET /api/v1/refund-requests/{id}`

Lấy chi tiết refund request.

- **Auth:** Bearer JWT
- **Path params:** `id` (Long, required)
- **Response:** `200 RefundRequestResponse` (raw)
- **Errors:** `401 UNAUTHENTICATED`, `404 NOT_FOUND`

### 7.3 `PUT /api/v1/refund-requests/{id}/approve`

Admin duyệt yêu cầu hoàn tiền.

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Query params:** `approvedBy` (Long, required) — user ID admin
- **Response:** `200 RefundRequestResponse` (status → APPROVED, populate `approvedAt`, `approvedBy`)
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 REFUND_NOT_PENDING`

### 7.4 `PUT /api/v1/refund-requests/{id}/reject`

Admin từ chối yêu cầu hoàn tiền.

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Query params:** `rejectedBy` (Long, required), `reason` (String, required)
- **Response:** `200 RefundRequestResponse` (status → REJECTED)
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 REFUND_NOT_PENDING`

### 7.5 `POST /api/v1/refund-requests/{id}/process`

Thực hiện refund đã được duyệt (gọi gateway hoàn tiền + cập nhật invoice).

- **Auth:** Bearer JWT (admin/finance role)
- **Path params:** `id` (Long, required)
- **Response:** `200 RefundRequestResponse` (status → COMPLETED, populate `processedAt`)
- **Errors:** `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 REFUND_NOT_APPROVED` (chưa duyệt), `502 PAYMENT_GATEWAY_TIMEOUT`

---

## 8. Response DTOs

### 8.1 InvoiceResponse

`com.kiteclass.core.module.invoice.dto.InvoiceResponse`:

| Field | Type | Mô tả |
|---|---|---|
| `id` | Long | Invoice ID |
| `invoiceNumber` | String | Mã hoá đơn (vd `INV-2026-05-001`) |
| `studentId` | Long | Học sinh |
| `classId` | Long | Lớp học |
| `enrollmentId` | Long | Enrollment (nullable) |
| `status` | InvoiceStatus | DRAFT/SENT/PAID/PARTIAL/OVERDUE/CANCELLED/REFUNDED |
| `issueDate` | LocalDate | Ngày phát hành |
| `dueDate` | LocalDate | Hạn thanh toán |
| `periodStart` | LocalDate | Bắt đầu kỳ thanh toán |
| `periodEnd` | LocalDate | Kết thúc kỳ thanh toán |
| `subtotal` | BigDecimal | Tổng line items |
| `discount` | BigDecimal | Giảm giá |
| `total` | BigDecimal | Tổng cuối |
| `amountPaid` | BigDecimal | Đã thanh toán |
| `balanceDue` | BigDecimal | Còn lại |
| `paidAt` | LocalDateTime | Thời điểm thanh toán (nullable) |
| `notes` | String | Ghi chú |
| `items` | List<InvoiceItemResponse> | Line items |
| `adjustments` | List<InvoiceAdjustmentResponse> | Adjustments |
| `createdAt` | LocalDateTime | Audit |
| `updatedAt` | LocalDateTime | Audit |

### 8.2 InvoiceItemResponse

| Field | Type | Mô tả |
|---|---|---|
| `id` | Long | Item ID |
| `type` | InvoiceItemType | TUITION/MATERIALS/REGISTRATION_FEE/EXAM_FEE/OTHER |
| `description` | String | Mô tả |
| `quantity` | Integer | Số lượng |
| `unitPrice` | BigDecimal | Đơn giá |
| `amount` | BigDecimal | Thành tiền (`quantity * unitPrice`) |
| `referenceId` | Long | Reference (vd course ID, exam ID) — nullable |

### 8.3 InvoiceAdjustmentResponse

| Field | Type | Mô tả |
|---|---|---|
| `id` | Long | Adjustment ID |
| `type` | InvoiceAdjustmentType | DISCOUNT/LATE_FEE/ADDITIONAL_CHARGE/REFUND |
| `description` | String | Mô tả |
| `amount` | BigDecimal | Số tiền (sign theo type) |
| `reason` | String | Lý do (nullable) |

### 8.4 PaymentResponse

`com.kiteclass.core.module.payment.dto.PaymentResponse`:

| Field | Type | Mô tả |
|---|---|---|
| `id` | Long | Payment ID |
| `paymentNumber` | String | Mã payment (vd `PAY-2026-05-001`) |
| `transactionId` | String | Gateway transaction ID (nullable cho offline) |
| `invoiceId` | Long | Invoice (nullable nếu pay installment) |
| `installmentId` | Long | Installment (nullable nếu pay invoice trực tiếp) |
| `amount` | BigDecimal | Số tiền |
| `paymentMethod` | PaymentMethod | CASH/BANK_TRANSFER/MOMO/VNPAY/ZALOPAY/CREDIT_CARD |
| `paymentStatus` | PaymentStatus | PENDING/PROCESSING/COMPLETED/FAILED/REFUNDED |
| `paymentUrl` | String | URL redirect (online gateway only) — nullable |
| `qrCodeUrl` | String | URL QR code (MoMo/ZaloPay) — nullable |
| `receiptNumber` | String | Số biên lai (nullable) |
| `receiptUrl` | String | URL biên lai PDF (nullable) |
| `initiatedAt` | LocalDateTime | Thời điểm khởi tạo |
| `expiresAt` | LocalDateTime | Hết hạn (online gateway) — nullable |
| `completedAt` | LocalDateTime | Hoàn tất (nullable) |
| `failureReason` | String | Lý do thất bại (nullable) |

### 8.5 PaymentStatusResponse

| Field | Type | Mô tả |
|---|---|---|
| `status` | PaymentStatus | PENDING/PROCESSING/COMPLETED/FAILED/REFUNDED |

### 8.6 InstallmentPlanResponse

`com.kiteclass.core.module.invoice.dto.InstallmentPlanResponse`:

| Field | Type | Mô tả |
|---|---|---|
| `id` | Long | Plan ID |
| `invoiceId` | Long | Invoice gốc |
| `numberOfInstallments` | Integer | Số đợt (2-12) |
| `status` | InstallmentPlanStatus | PENDING/APPROVED/REJECTED/ACTIVE/COMPLETED/CANCELLED |
| `requestedAt` | LocalDateTime | Thời điểm yêu cầu |
| `approvedAt` | LocalDateTime | Thời điểm duyệt (nullable) |
| `approvedBy` | Long | User ID duyệt (nullable) |
| `rejectedAt` | LocalDateTime | Thời điểm từ chối (nullable) |
| `rejectionReason` | String | Lý do từ chối (nullable) |
| `installments` | List<InstallmentResponse> | Các đợt thanh toán |
| `createdAt` | LocalDateTime | Audit |
| `updatedAt` | LocalDateTime | Audit |

### 8.7 InstallmentResponse

| Field | Type | Mô tả |
|---|---|---|
| `id` | Long | Installment ID |
| `installmentNumber` | Integer | Thứ tự đợt (1, 2, ...) |
| `amount` | BigDecimal | Số tiền đợt này |
| `dueDate` | LocalDate | Hạn đợt này |
| `paidAmount` | BigDecimal | Đã thanh toán |
| `status` | InstallmentStatus | PENDING/PAID/OVERDUE/CANCELLED |
| `paidAt` | LocalDateTime | Thời điểm thanh toán (nullable) |

### 8.8 RefundRequestResponse

`com.kiteclass.core.module.invoice.dto.RefundRequestResponse`:

| Field | Type | Mô tả |
|---|---|---|
| `id` | Long | Refund request ID |
| `invoiceId` | Long | Invoice gốc |
| `refundAmount` | BigDecimal | Số tiền hoàn |
| `reason` | String | Lý do yêu cầu |
| `status` | RefundStatus | PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED |
| `requestedBy` | Long | User ID yêu cầu |
| `requestedAt` | LocalDateTime | Thời điểm yêu cầu |
| `approvedBy` | Long | User ID duyệt (nullable) |
| `approvedAt` | LocalDateTime | Thời điểm duyệt (nullable) |
| `rejectedBy` | Long | User ID từ chối (nullable) |
| `rejectedAt` | LocalDateTime | Thời điểm từ chối (nullable) |
| `rejectionReason` | String | Lý do từ chối (nullable) |
| `processedAt` | LocalDateTime | Thời điểm hoàn tất (nullable) |
| `createdAt` | LocalDateTime | Audit |
| `updatedAt` | LocalDateTime | Audit |

---

## 9. Request DTOs

### 9.1 ApplyAdjustmentRequest

| Field | Type | Required | Validation |
|---|---|---|---|
| `type` | InvoiceAdjustmentType | ✅ | `@NotNull` |
| `description` | String | ✅ | `@NotBlank`, `@Size(max = 255)` |
| `amount` | BigDecimal | ✅ | `@NotNull` (positive cho fees, negative cho discount/refund) |
| `reason` | String | ❌ | optional |

### 9.2 CreatePaymentRequest

| Field | Type | Required | Validation |
|---|---|---|---|
| `invoiceId` | Long | ✅ | `@NotNull` |
| `amount` | BigDecimal | ✅ | `@NotNull`, `@DecimalMin("0.01")` |
| `paymentMethod` | PaymentMethod | ✅ | `@NotNull` (CASH/BANK_TRANSFER/MOMO/VNPAY/ZALOPAY/CREDIT_CARD) |
| `ipAddress` | String | ❌ | optional (server fallback từ request) |

### 9.3 CreateInstallmentPaymentRequest

| Field | Type | Required | Validation |
|---|---|---|---|
| `installmentId` | Long | ✅ | `@NotNull` |
| `amount` | BigDecimal | ✅ | `@NotNull`, `@DecimalMin("0.01")` |
| `paymentMethod` | PaymentMethod | ✅ | `@NotNull` |
| `ipAddress` | String | ❌ | optional |

### 9.4 CreateInstallmentPlanRequest

| Field | Type | Required | Validation |
|---|---|---|---|
| `invoiceId` | Long | ✅ | `@NotNull`, `@Positive` |
| `numberOfInstallments` | Integer | ✅ | `@NotNull`, `@Min(2)`, `@Max(12)` |

### 9.5 CreateRefundRequestRequest

| Field | Type | Required | Validation |
|---|---|---|---|
| `invoiceId` | Long | ✅ | `@NotNull`, `@Positive` |
| `refundAmount` | BigDecimal | ✅ | `@NotNull`, `@Positive` |
| `reason` | String | ✅ | `@NotBlank` |

### 9.6 MomoCallbackRequest

| Field | Type | Mô tả |
|---|---|---|
| `partnerCode` | String | Mã đối tác MoMo |
| `orderId` | String | Order ID (chính là `paymentNumber`) |
| `requestId` | String | Request ID |
| `amount` | String | Số tiền (String per MoMo spec) |
| `orderInfo` | String | Thông tin order |
| `orderType` | String | Loại order |
| `transId` | String | Transaction ID từ MoMo |
| `resultCode` | String | Mã kết quả (0 = success) |
| `message` | String | Message từ MoMo |
| `payType` | String | Loại payment |
| `responseTime` | String | Timestamp response |
| `extraData` | String | Extra data |
| `signature` | String | HMAC-SHA256 signature (verify) |
| `extraParams` | Map<String,String> | `@JsonAnySetter` — chứa field bổ sung |

### 9.7 ZalopayCallbackRequest

| Field | Type | Mô tả |
|---|---|---|
| `data` | String | JSON string chứa transaction info |
| `mac` | String | HMAC-SHA256 MAC (verify) |
| `type` | String | Loại callback |
| `appId` (`@JsonProperty("app_id")`) | String | App ID |
| `appTransId` (`@JsonProperty("app_trans_id")`) | String | App transaction ID |
| `extraParams` | Map<String,String> | `@JsonAnySetter` |

---

## 10. Error response envelope

Đa số endpoint dùng `ApiResponse<T>` shape:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVOICE_NOT_FOUND",
    "message": "Invoice không tồn tại hoặc đã bị xóa",
    "details": { "invoiceId": 123 }
  },
  "timestamp": "2026-05-26T10:30:00"
}
```

Endpoint trả raw DTO (Refund + Installment) dùng Spring default error format khi exception:

```json
{
  "timestamp": "2026-05-26T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Refund request not found: 123",
  "path": "/api/v1/refund-requests/123"
}
```

### 10.1 Standard error codes

| HTTP | Code | Mô tả |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Request body không hợp lệ (vd missing field, regex fail) |
| 401 | `AUTH_REQUIRED` / `UNAUTHENTICATED` | Thiếu JWT hoặc UserContext không có |
| 403 | `FORBIDDEN` | Không đủ quyền (role-based) |
| 404 | `NOT_FOUND` (+ resource-specific như `INVOICE_NOT_FOUND`) | Resource không tồn tại trong tenant |
| 409 | `CONFLICT_*` (state-based, vd `INVOICE_ALREADY_PAID`, `PLAN_NOT_PENDING`) | Vi phạm state machine |
| 500 | `INTERNAL` | Lỗi server không xác định |
| 502 | `PAYMENT_GATEWAY_TIMEOUT` | Gateway lỗi hoặc timeout |

### 10.2 Domain-specific error codes

| Code | Endpoint | Trigger |
|---|---|---|
| `INVOICE_NOT_PAYABLE` | POST /payments | Invoice CANCELLED hoặc đã PAID |
| `INVOICE_NOT_ADJUSTABLE` | POST /invoices/{id}/adjustments | Invoice CANCELLED |
| `INVOICE_NOT_OVERDUE` | POST /invoices/{id}/late-fees | Invoice chưa quá hạn |
| `INVOICE_ALREADY_PAID` | POST /invoices/{id}/mark-paid | Đã PAID rồi |
| `INVOICE_NOT_CANCELLABLE` | PUT /invoices/{id}/cancel | Đã PAID hoặc đã REFUNDED |
| `AMOUNT_EXCEEDS_BALANCE` | POST /payments | `amount` > `balanceDue` |
| `INSTALLMENT_NOT_FOUND` | POST /payments/installments | Installment ID không tồn tại |
| `INSTALLMENT_NOT_DUE` | POST /payments/installments | Chưa đến `dueDate` |
| `INSTALLMENT_ALREADY_PAID` | POST /payments/installments | Đã PAID |
| `AMOUNT_EXCEEDS_DUE` | POST /installment-plans/installments/{id}/payment | `amount` > `amount - paidAmount` |
| `INSTALLMENT_NOT_ALLOWED` | POST /installment-plans | Invoice đã PAID hoặc đã có plan active |
| `PLAN_NOT_PENDING` | PUT /installment-plans/{id}/{approve,reject} | Plan không ở status PENDING |
| `REFUND_AMOUNT_EXCEEDS_PAID` | POST /refund-requests | `refundAmount` > `amountPaid` |
| `REFUND_NOT_PENDING` | PUT /refund-requests/{id}/{approve,reject} | Refund không ở status PENDING |
| `REFUND_NOT_APPROVED` | POST /refund-requests/{id}/process | Refund chưa APPROVED |
| `PAYMENT_NOT_CANCELLABLE` | PUT /payments/{id}/cancel | Status không phải PENDING |
| `PAYMENT_NOT_REFUNDABLE` | POST /payments/{id}/refund | Status không phải COMPLETED hoặc đã REFUNDED |
| `WEBHOOK_SIGNATURE_INVALID` | webhook handlers | Signature/MAC không match |
| `PAYMENT_GATEWAY_TIMEOUT` | online payment ops | Gateway response > timeout (config `payment.gateway.timeout-ms`) |

---

## 11. State machines

### 11.1 Invoice lifecycle

```
DRAFT ──issue──> SENT ──pay full──> PAID
                  │                   │
                  ├──pay partial──> PARTIAL ──pay rest──> PAID
                  │                   │
                  ├──late──> OVERDUE ──pay full──> PAID
                  │                   │
                  ├──cancel──> CANCELLED
                  │
                  └──refund──> REFUNDED (từ PAID)
```

### 11.2 Payment lifecycle

```
PENDING ──gateway initiate──> PROCESSING ──gateway success──> COMPLETED ──refund──> REFUNDED
                                  │
                                  ├──gateway fail──> FAILED
                                  │
                                  └──user cancel──> FAILED (PENDING only)
```

### 11.3 Installment plan lifecycle

```
PENDING ──admin approve──> APPROVED ──first payment──> ACTIVE ──all paid──> COMPLETED
   │                          │                          │
   ├──admin reject──> REJECTED│                          │
   │                          └──admin/user cancel──> CANCELLED
   └──cancel──> CANCELLED
```

### 11.4 Refund request lifecycle

```
PENDING ──admin approve──> APPROVED ──process──> COMPLETED
   │                          │
   ├──admin reject──> REJECTED│
   │                          │
   └──cancel──> CANCELLED     │
```

---

## 12. Cross-references

- **Use Cases:** `documents/01-business/kiteclass/payment-invoice/use-cases.md` — UC-PAY-01 → UC-PAY-11
- **Business Rules:** `documents/01-business/kiteclass/payment-invoice/rules.md` — BR-PAY-xxx (gateway config, retry policy, refund window)
- **Java source:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{payment,invoice}/`
- **FE consumer:** `kiteclass/kiteclass-frontend/src/lib/api/invoices.ts`, `payment-records.ts`, `kiteclass-frontend/src/types/payment.ts`
- **Integration tests:** `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/integration/{Invoice,Payment}FlowIntegrationTest.java`, `kiteclass-core/src/test/.../module/payment/PaymentIntegrationTest.java`
- **Gap:** [GAP-231](../../../04-quality/gaps/phase-1-beta/GAP-231-api-contract-payment-invoice-zero-doc.md) (closed Wave br-6 Bucket A)
- **Sister domain:** `documents/01-business/kitehub/subscription-billing/api-contract.md` (KiteHub subscription billing — separate PaymentMethod enum)

---

## 13. Wave history

- **2026-05-26 (Wave br-6 Bucket A):** sync với 5 controllers (31 endpoints verified). Closes GAP-231. Drift fixes vs prior version: (1) `InvoiceStatus.SENT` (not `ISSUED`); (2) `InvoiceStatus.PARTIAL` (not `PARTIALLY_PAID`); (3) endpoint count 32 → 31 (PaymentController has 8 method-level mappings, not 9 — gap filing 2026-04-26 included class-level @RequestMapping in count); (4) docs `POST /payments/{id}/refund` đã đúng `204 No Content` (controller returns Void); (5) installment + refund endpoints return raw DTO (no `ApiResponse` envelope); (6) webhook DTOs schema documented per `MomoCallbackRequest` + `ZalopayCallbackRequest` (Wave Bucket beta-readiness Wave 88 typed callback refactor); (7) UC + BR refs documented; (8) state machines documented.
- **2026-05-25 (GAP-739):** PaymentMethod canonical consolidation (removed duplicate enum).
- **2026-04-26 (initial):** stub created, ~22-25 endpoints listed. PARTIAL coverage flagged by post-Wave-7 API audit → GAP-231 filed.
