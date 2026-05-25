# Payment Record — API Contract

> Extracted from: `PaymentRecordController`, `RecordPaymentRequest`, `PaymentRecordResponse`, `PaymentRecordMethod`
> Base path: `/api/v1/invoices`
> Wave reference: Wave beta-readiness-4 Bucket C (GAP-292b) PR #1783

## Endpoints

### POST `/api/v1/invoices/{invoiceId}/record-payment`

Ghi nhận manual payment received từ phụ huynh / học sinh tại trung tâm (cash / bank / QR / MoMo).

- **Auth:** Required — `@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")` per BR-PAYMENT-RECORD-006
- **Headers:**
  - `Idempotency-Key` (UUID v4, optional) — per BR-PAYMENT-RECORD-004 dedupe within 24h window
- **Path:** `invoiceId` (Long, required) — invoice being paid
- **Request:** `RecordPaymentRequest`
  ```json
  {
    "method": "CASH",
    "amount": 1500000.00,
    "paidAt": "2026-05-25T07:30:00Z",
    "note": "Phụ huynh em Trần Thị Hồng nộp học phí tháng 5"
  }
  ```
  - `method` (enum `PaymentRecordMethod`, required per BR-PAYMENT-RECORD-003): `CASH` | `BANK_TRANSFER` | `VIETQR` | `MOMO`
  - `amount` (BigDecimal, required, > 0 per BR-PAYMENT-RECORD-002; precision 19/2)
  - `paidAt` (Instant ISO-8601, optional — defaults `now()` per BR-PAYMENT-RECORD-009)
  - `note` (String, optional, max 500 chars per BR-PAYMENT-RECORD-005)
- **Response:** `201 Created` + `ApiResponse<PaymentRecordResponse>`
  ```json
  {
    "success": true,
    "data": {
      "id": 12345,
      "invoiceId": 7890,
      "method": "CASH",
      "amount": 1500000.00,
      "paidAt": "2026-05-25T07:30:00Z",
      "note": "Phụ huynh em Trần Thị Hồng nộp học phí tháng 5",
      "recordedBy": 1,
      "createdAt": "2026-05-25T07:30:15.234Z"
    }
  }
  ```
- **Side-effects:**
  - 1 row append vào `payment_records` (append-only per BR-PAYMENT-RECORD-010)
  - `invoices.paid_amount` sum updated
  - `invoices.status` potentially `PARTIALLY_PAID` → `PAID` nếu full
- **Idempotency:** Identical `Idempotency-Key` trong window 24h → return cached response (HTTP 200 cached, không 201 new) per BR-PAYMENT-RECORD-004
- **Errors:**
  | Code | Condition | Message |
  |------|-----------|---------|
  | `400` | `method` null | "Phương thức thanh toán không được để trống" |
  | `400` | `amount` null hoặc < 0.01 | "Số tiền phải lớn hơn 0" |
  | `400` | `amount > 1.000.000.000` | "Số tiền quá lớn" |
  | `400` | `note > 500 chars` | "Ghi chú tối đa 500 ký tự" |
  | `403` | Role không match per BR-PAYMENT-RECORD-006 | "Bạn không có quyền ghi nhận thanh toán" |
  | `404` | Invoice không tồn tại OR cross-tenant per BR-PAYMENT-RECORD-007 | "Không tìm thấy hoá đơn" |
  | `409` | Invoice status `PAID` / `CANCELLED` | "Hoá đơn đã thanh toán/huỷ" |

### GET `/api/v1/invoices/{invoiceId}/payment-records`

Liệt kê tất cả payment records của một invoice (scoped per BR-PAYMENT-RECORD-008).

- **Auth:** Required — `@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")`
- **Path:** `invoiceId` (Long, required)
- **Response:** `200 OK` + `ApiResponse<List<PaymentRecordResponse>>`
- **Scope:**
  - OWNER + ADMIN + PLATFORM_ADMIN → mọi invoices trong instance
  - TEACHER → chỉ invoices liên kết classes mà teacher member
  - PARENT (Phase 1.5+) → chỉ invoices của children own
- **Errors:**
  | Code | Condition | Message |
  |------|-----------|---------|
  | `403` | User không có quyền view invoice | "Bạn không có quyền xem hoá đơn này" |
  | `404` | Invoice không tồn tại | "Không tìm thấy hoá đơn" |

## DTOs

### RecordPaymentRequest

Source: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/record/dto/RecordPaymentRequest.java`

| Field | Type | Required | Validation | Description |
|-------|------|---------|-----------|-------------|
| method | PaymentRecordMethod | ✅ | `@NotNull` | Enum 4 values per BR-PAYMENT-RECORD-001 |
| amount | BigDecimal | ✅ | `@NotNull` + `@DecimalMin("0.01")` | VND amount, precision 19/2 |
| paidAt | Instant | ⚠️ optional | — | UTC; defaults `Instant.now()` if omitted |
| note | String | ⚠️ optional | `@Size(max = 500)` | Max 500 chars |

### PaymentRecordResponse

Source: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/record/dto/PaymentRecordResponse.java`

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Payment record ID |
| invoiceId | Long | Reference invoice ID |
| method | PaymentRecordMethod | Enum raw value |
| amount | BigDecimal | Amount in VND |
| paidAt | Instant | Timestamp payment physically received |
| note | String | Optional teacher note |
| recordedBy | Long | Actor user ID |
| createdAt | Instant | DB row creation timestamp |

### PaymentRecordMethod enum

Source: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/record/entity/PaymentRecordMethod.java`

| Enum value | Display (VN) | Use case |
|-----------|--------------|----------|
| `CASH` | Tiền mặt | Phụ huynh nộp tại trung tâm (60-70% TT nhỏ) |
| `BANK_TRANSFER` | Chuyển khoản ngân hàng | Vietcombank/Techcombank/MB/ACB chuyển khoản |
| `VIETQR` | VietQR | Scan QR thanh toán bank-agnostic |
| `MOMO` | MoMo | Ví MoMo digital wallet |

## Distinction từ gateway PaymentService

| Aspect | Manual `PaymentRecord` | Gateway `Payment` (existing) |
|--------|----------------------|------------------------------|
| **Path** | `POST /api/v1/invoices/{id}/record-payment` | `POST /api/v1/payments/...` (VNPAY/ZaloPay flow) |
| **Method enum** | `PaymentRecordMethod` (CASH/BANK/QR/MOMO) | `PaymentMethod` (VNPAY/ZALOPAY/MOMO_GATEWAY) |
| **Table** | `payment_records` | `payments` |
| **Trigger** | Teacher manual after physical receipt | Online flow, gateway callback |
| **Audit** | Append-only (BR-PAYMENT-RECORD-010) | Gateway-driven state machine |

2 services độc lập, không cross-reference (BR-PAYMENT-RECORD-011).

## Cross-references

- **Use Cases:** UC-PAYMENT-01, UC-PAYMENT-02, UC-PAYMENT-03 (see `use-cases.md`)
- **Business Rules:** BR-PAYMENT-RECORD-001..011 (see `rules.md`)
- **Service:** `PaymentRecordServiceImpl.recordPayment(invoiceId, request, recordedByUserId, idempotencyKey)`
- **Idempotency:** `PaymentIdempotencyService` dedupe per (instanceId, invoiceId, idempotencyKey) trong 24h
- **Migration:** V69 — `payment_records` table + indexes per `rules.md` §4
- **Compliance:** PDPL 2023 Art 11 (audit trail), Luật Quản lý Thuế 2019 Art 18 (retention 10 năm), Nghị định 123/2020/NĐ-CP (e-invoice deferred Phase 1.5+ via GAP-185 MISA partnership)
