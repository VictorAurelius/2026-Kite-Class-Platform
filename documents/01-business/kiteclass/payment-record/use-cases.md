# Payment Record — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-05-25
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/record/`

---

## Use Cases

### UC-PAYMENT-01: Teacher ghi nhận thanh toán bằng tiền mặt (cash recording)

**Actor:** Teacher (member của class liên kết invoice) / Admin / Owner
**Precondition:** Invoice exists; user có permission per BR-PAYMENT-RECORD-006; invoice status không phải PAID hoặc CANCELLED.

**Steps:**

1. Teacher mở UI `(teacher)/teacher/invoices/[invoiceId]/detail` → click nút "Ghi nhận thanh toán"
2. FE hiển thị `RecordPaymentDialog` modal:
   - Dropdown `method` (4 options Vietnamese display): "Tiền mặt" / "Chuyển khoản" / "VietQR" / "MoMo" — mandatory per BR-PAYMENT-RECORD-003
   - Input `amount` (BigDecimal, VND format) — mandatory per BR-PAYMENT-RECORD-002; pre-fill `invoice.remainingAmount` cho convenience
   - DateTimePicker `paidAt` — default `now()` per BR-PAYMENT-RECORD-009
   - Textarea `note` — optional, max 500 chars per BR-PAYMENT-RECORD-005
3. Teacher chọn "Tiền mặt" + nhập amount `1.500.000đ` + optional note "Phụ huynh em Hồng nộp tháng 5"
4. FE generate UUID v4 → `Idempotency-Key: <uuid>` header per BR-PAYMENT-RECORD-004
5. FE call `POST /api/v1/invoices/{invoiceId}/record-payment`
6. BE `PaymentRecordController.recordPayment` → `PaymentRecordServiceImpl.recordPayment`:
   - Validate role per BR-PAYMENT-RECORD-006 (`@PreAuthorize`)
   - Validate invoice exists + `invoice.instanceId == tenantContext.instanceId` per BR-PAYMENT-RECORD-007
   - Idempotency check per BR-PAYMENT-RECORD-004 (lookup `(invoice_id, idempotency_key)` 24h window)
   - Validate `amount > 0` per BR-PAYMENT-RECORD-002
   - Persist `PaymentRecord` row (append-only per BR-PAYMENT-RECORD-010)
   - Update invoice `paidAmount` sum + status (PARTIALLY_PAID → PAID nếu full)
7. Response 201 + `PaymentRecordResponse`
8. FE toast "Đã ghi nhận thanh toán 1.500.000đ qua Tiền mặt"; refresh invoice detail

**Postcondition:**

- 1 row append vào `payment_records` với `method=CASH`, `amount=1500000`, `recorded_by=<teacherId>`, `created_at=now()`
- `invoices.paid_amount` updated
- `invoices.status` potentially updated PARTIALLY_PAID → PAID

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 400 | `method` null | "Phương thức thanh toán không được để trống" |
| 400 | `amount` null hoặc ≤ 0 | "Số tiền phải lớn hơn 0" |
| 400 | `amount > 1.000.000.000` | "Số tiền quá lớn" |
| 400 | `note > 500 chars` | "Ghi chú tối đa 500 ký tự" |
| 403 | User không phải TEACHER/ADMIN/OWNER | "Bạn không có quyền ghi nhận thanh toán" |
| 404 | Invoice không tồn tại (hoặc cross-tenant per BR-PAYMENT-RECORD-007) | "Không tìm thấy hoá đơn" |
| 409 | Invoice status `PAID` / `CANCELLED` | "Hoá đơn đã thanh toán/huỷ" |

**FE behavior:**

- VN sample data per `vn-localization-audit-checklist.md` §3: "Phụ huynh em Trần Thị Hồng", "1.500.000đ"
- `amount` input format `1.500.000đ` (dấu chấm thousands separator, đuôi `đ`)
- Generate Idempotency-Key UUID v4 mọi submit; disable submit button trong khi BE processing
- Date format `Thứ Hai, 25/05/2026 14:30` per VN convention §1
- Method dropdown helper text:
  - "Tiền mặt" → "Phụ huynh nộp tại trung tâm"
  - "Chuyển khoản" → "Vietcombank / Techcombank / MB / ACB / ..."
  - "VietQR" → "Phụ huynh scan QR thanh toán qua app ngân hàng"
  - "MoMo" → "Phụ huynh thanh toán qua ví MoMo"

### UC-PAYMENT-02: View payment history for invoice

**Actor:** Teacher / Admin / Owner / Parent (own children's invoices)
**Precondition:** Invoice exists; user có quyền view per BR-PAYMENT-RECORD-008.

**Steps:**

1. User mở UI invoice detail → scroll xuống section "Lịch sử thanh toán"
2. FE call `GET /api/v1/invoices/{invoiceId}/payment-records`
3. BE filter scope per BR-PAYMENT-RECORD-008:
   - OWNER + ADMIN → mọi invoices trong instance
   - TEACHER → chỉ invoices liên kết classes mà teacher member
   - PARENT (Phase 1.5+) → chỉ invoices của children mình
4. Return list of `PaymentRecordResponse` (sorted asc by `paidAt`)
5. FE render `PaymentRecordsTable`:
   - Thời gian (`paidAt`, VN format)
   - Phương thức (Vietnamese display)
   - Số tiền (VND format)
   - Người ghi nhận (`recordedByName`)
   - Ghi chú (truncated 80 + "Xem thêm")

**Postcondition:** History displayed; append-only per BR-PAYMENT-RECORD-010.

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 403 | User không có quyền view invoice | "Bạn không có quyền xem hoá đơn này" |
| 404 | Invoice không tồn tại | "Không tìm thấy hoá đơn" |

**FE behavior:**

- Empty state: "Chưa có thanh toán nào" + icon
- Footer row hiển thị: "Tổng đã thanh toán: <sum>đ / <invoice.totalAmount>đ"
- Status badge: "Đã thanh toán đủ" (green) / "Đang trả góp" (yellow) / "Chưa thanh toán" (red)

### UC-PAYMENT-03: Correct mistaken payment record (correction via new row)

**Actor:** Admin / Owner
**Precondition:** Existing `PaymentRecord` row có lỗi (vd nhập sai amount); user có permission per BR-PAYMENT-RECORD-006.

**Steps:**

1. Admin nhận report sai từ teacher/parent
2. Admin mở UI invoice detail → click "Ghi nhận sửa" trên row sai
3. FE hiển thị `RecordPaymentDialog` pre-fill với negative amount offset (vd nếu row sai +1.500.000đ → form pre-fill `-1.500.000đ` + note "Sửa GD #<original_id>: lý do <reason>")
4. Admin xác nhận → BE persist new row với negative amount + note linkage per BR-PAYMENT-RECORD-010
5. FE refresh: history table show 2 rows (original + correction), net sum displayed footer

**Postcondition:**

- Original row UNCHANGED (append-only per BR-PAYMENT-RECORD-010)
- New row với negative amount + note "Sửa GD #X: <reason>"
- `invoices.paid_amount` updated net sum

**Errors:** Same as UC-PAYMENT-01.

**FE behavior:**

- Correction button visible chỉ cho ADMIN/OWNER (TEACHER read-only sau insert)
- WARN modal: "Lưu ý: Hệ thống không cho phép sửa/xoá. Bạn sẽ tạo bản ghi sửa mới với số âm. Bạn có chắc?"
- Note field mandatory cho correction (FE-level enforcement)
