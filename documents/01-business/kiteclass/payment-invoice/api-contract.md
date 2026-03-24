# Payment & Invoice — API Contract

> Extracted from: `InvoiceController`, `PaymentController`, `InstallmentPlanController`, `RefundRequestController`, `PaymentWebhookController`
> Base paths: `/api/v1/invoices`, `/api/v1/payments`, `/api/v1/installment-plans`, `/api/v1/refund-requests`

## Invoice Endpoints

### GET `/api/v1/invoices/{id}`
Get invoice by ID. **Response:** `ApiResponse<InvoiceResponse>` (200)

### GET `/api/v1/invoices/{id}/items`
Get line items for an invoice. **Response:** `ApiResponse<List<InvoiceItemResponse>>` (200)

### GET `/api/v1/invoices/student/{studentId}`
Get all invoices for a student. Paginated. **Response:** `Page<InvoiceResponse>` (200)

### POST `/api/v1/invoices/{id}/adjustments`
Apply adjustment (discount/surcharge) to an invoice.
- **Request:** `ApplyAdjustmentRequest` — `type` (DISCOUNT/SURCHARGE), `description` (String), `amount` (BigDecimal), `reason` (String)
- **Response:** `ApiResponse<InvoiceResponse>` (200)

### POST `/api/v1/invoices/{id}/late-fees`
Calculate and apply late fees. **Response:** `ApiResponse<InvoiceResponse>` (200)

### GET `/api/v1/invoices/overdue` | `/student/{studentId}/unpaid` | `/student/{studentId}/overdue`
Query invoices by status. All paginated.

### POST `/api/v1/invoices/{id}/mark-paid`
Manually mark invoice as paid. **Response:** `ApiResponse<InvoiceResponse>` (200)

### PUT `/api/v1/invoices/{id}/cancel`
Cancel an invoice. **Response:** `ApiResponse<InvoiceResponse>` (200)

## Payment Endpoints

### POST `/api/v1/payments`
Initiate a payment for an invoice.
- **Request:** `CreatePaymentRequest` — `invoiceId` (Long, required), `amount` (BigDecimal, required), `paymentMethod` (VNPAY/MOMO/ZALOPAY/BANK_TRANSFER/CASH), `ipAddress` (String)
- **Response:** `ApiResponse<PaymentResponse>` (201) — includes `paymentUrl`, `qrCodeUrl` for online methods

### POST `/api/v1/payments/installments`
Pay a specific installment.
- **Request:** `CreateInstallmentPaymentRequest` — `installmentId` (Long), `amount` (BigDecimal), `paymentMethod` (enum), `ipAddress` (String)
- **Response:** `ApiResponse<PaymentResponse>` (201)

### GET `/api/v1/payments/{id}` | `/invoice/{invoiceId}` | `/pending`
Query payments. Pending endpoint is paginated.

### PUT `/api/v1/payments/{id}/cancel`
Cancel a pending payment. **Response:** `ApiResponse<PaymentResponse>` (200)

### POST `/api/v1/payments/{id}/refund`
Process refund for a completed payment. **Response:** `ApiResponse<PaymentResponse>` (200)

### GET `/api/v1/payments/{id}/status`
Query real-time payment status from gateway. **Response:** `ApiResponse<PaymentStatusResponse>` (200)

## Installment Plan Endpoints

### POST `/api/v1/installment-plans`
Request an installment plan for an invoice.
- **Request:** `CreateInstallmentPlanRequest` — `invoiceId` (Long), `numberOfInstallments` (Integer)
- **Response:** `ApiResponse<InstallmentPlanResponse>` (201)

### GET `/api/v1/installment-plans/{id}`
Get plan details with installment schedule. **Response:** `ApiResponse<InstallmentPlanResponse>` (200)

### PUT `/api/v1/installment-plans/{id}/approve` | `/{id}/reject`
Admin approves or rejects a plan. **Response:** `ApiResponse<InstallmentPlanResponse>` (200)

### POST `/api/v1/installment-plans/installments/{installmentId}/payment`
Pay a specific installment in a plan. **Response:** `ApiResponse<PaymentResponse>` (201)

## Refund Request Endpoints

### POST `/api/v1/refund-requests`
Student requests a refund.
- **Request:** `CreateRefundRequestRequest` — `invoiceId` (Long), `refundAmount` (BigDecimal), `reason` (String)
- **Response:** `ApiResponse<RefundRequestResponse>` (201)

### GET `/api/v1/refund-requests/{id}`
Get refund request details. **Response:** `ApiResponse<RefundRequestResponse>` (200)

### PUT `/api/v1/refund-requests/{id}/approve` | `/{id}/reject`
Admin approves or rejects. **Response:** `ApiResponse<RefundRequestResponse>` (200)

### POST `/api/v1/refund-requests/{id}/process`
Execute an approved refund. **Response:** `ApiResponse<RefundRequestResponse>` (200)

## Webhook Endpoints (PUBLIC — no auth)

### GET `/api/v1/payments/webhook/vnpay`
VNPay return callback. Verifies signature, updates payment status.

### POST `/api/v1/payments/webhook/momo`
MoMo IPN callback. Verifies signature, updates payment status.

### POST `/api/v1/payments/webhook/zalopay`
ZaloPay callback. Verifies MAC, updates payment status.

## Key DTOs

### InvoiceResponse
`id`, `invoiceNumber`, `studentId`, `classId`, `enrollmentId`, `status` (DRAFT/ISSUED/PAID/PARTIALLY_PAID/OVERDUE/CANCELLED), `issueDate`, `dueDate`, `periodStart`, `periodEnd`, `subtotal`, `discount`, `total`, `amountPaid`, `balanceDue`, `paidAt`, `notes`, `items[]`, `adjustments[]`, `createdAt`, `updatedAt`

### PaymentResponse
`id`, `paymentNumber`, `transactionId`, `invoiceId`, `installmentId`, `amount`, `paymentMethod`, `paymentStatus` (PENDING/COMPLETED/FAILED/CANCELLED/REFUNDED), `paymentUrl`, `qrCodeUrl`, `receiptNumber`, `receiptUrl`, `initiatedAt`, `expiresAt`, `completedAt`, `failureReason`

## Cross-references
- **Use Cases:** UC-PAY-01 → UC-PAY-11
- **Business Rules:** BR-PAY-xxx (see `rules.md`)
