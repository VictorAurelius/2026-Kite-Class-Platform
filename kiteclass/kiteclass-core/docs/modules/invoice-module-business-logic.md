# Invoice Module - Business Logic

## Mục lục
1. [Tổng quan](#tổng-quan)
2. [Business Rules](#business-rules)
3. [Use Cases](#use-cases)
4. [Entity Design](#entity-design)
5. [Integration Points](#integration-points)
6. [Events & Notifications](#events--notifications)

---

## Tổng quan

### Mục đích
Invoice Module quản lý hóa đơn học phí (tuition invoices) cho học viên. Module này:
- Tự động tạo hóa đơn khi enrollment được tạo
- Quản lý lifecycle của invoice: DRAFT → PENDING → PAID/OVERDUE/CANCELLED
- Hỗ trợ thanh toán trả góp (installments)
- Tích hợp với Payment Module để track payments
- Tạo QR code payment cho VNPay, MoMo, ZaloPay
- Gửi nhắc nhở thanh toán (payment reminders)
- Tính phí trễ hạn (late fees) cho overdue invoices
- Quản lý debt tracking và báo cáo công nợ

### Scope
**Bao gồm:**
- ✅ Auto-generate invoice khi enrollment created
- ✅ Invoice statuses: DRAFT, PENDING, PAID, OVERDUE, CANCELLED
- ✅ Installment payment plans (trả góp)
- ✅ QR code payment generation
- ✅ Late fee calculation cho overdue invoices
- ✅ Payment reminders (email/SMS) before due date
- ✅ Debt tracking và aging reports
- ✅ Invoice adjustments (discounts, additional charges)
- ✅ Refund handling
- ✅ Export invoices (PDF, Excel)

**Không bao gồm:**
- ❌ Payment processing (handled by Payment Module)
- ❌ Tax calculation (Vietnamese VAT) - Future feature
- ❌ Multi-currency support - Future feature
- ❌ Accounting system integration - Future feature

### Key Concepts
- **Invoice**: Hóa đơn học phí cho 1 enrollment
- **Invoice Item**: Line items trong invoice (tuition, materials, registration fee, etc.)
- **Installment Plan**: Kế hoạch trả góp với nhiều installments
- **Installment**: Một kỳ thanh toán trong installment plan
- **Due Date**: Hạn thanh toán
- **Late Fee**: Phí trễ hạn tính theo % hoặc fixed amount
- **QR Code Payment**: Mã QR để thanh toán qua VNPay/MoMo/ZaloPay
- **Debt Aging**: Phân loại công nợ theo thời gian quá hạn (0-30 days, 31-60 days, 60+ days)

---

## Business Rules

### BR-INV-001: Invoice Auto-Generation on Enrollment
**Quy tắc**: Hệ thống tự động tạo invoice khi enrollment được tạo với status = ACTIVE.

**Logic**:
```java
When ENROLLMENT_CREATED event received:
  1. Get course price from Class.course_id
  2. Create Invoice:
     - student_id = enrollment.student_id
     - class_id = enrollment.class_id
     - total_amount = course.price
     - due_date = enrollment.enrolled_at + payment_term (default: 7 days)
     - status = PENDING
  3. Create InvoiceItem:
     - type = TUITION
     - description = "Học phí khóa {course.name}"
     - amount = course.price
  4. Publish INVOICE_CREATED event
```

**Exceptions**:
- Nếu class có special pricing (ví dụ: early bird discount), override course.price
- Nếu student là scholarship recipient, apply discount trong InvoiceAdjustment

---

### BR-INV-002: Invoice Status Lifecycle
**Quy tắc**: Invoice status transitions theo lifecycle định sẵn.

**State Machine**:
```
DRAFT: Initial state (manual invoices chưa finalize)
  ↓ (finalize)
PENDING: Chờ thanh toán, đã gửi cho student
  ↓ (payment received)
PAID: Đã thanh toán đủ
  ↓ (refund issued)
REFUNDED: Đã hoàn tiền (partial or full)

PENDING → OVERDUE: Tự động chuyển khi current_date > due_date AND status = PENDING
PENDING/OVERDUE → CANCELLED: Admin cancel invoice (lý do: enrollment cancelled, etc.)
```

**Allowed Transitions**:
```
DRAFT → PENDING (finalize)
DRAFT → CANCELLED (admin cancel before send)
PENDING → PAID (full payment)
PENDING → OVERDUE (automatic, cron job)
PENDING → CANCELLED (admin cancel)
OVERDUE → PAID (late payment)
OVERDUE → CANCELLED (admin cancel)
PAID → REFUNDED (refund issued)
```

**Forbidden**:
- PAID → PENDING (cannot unpay)
- CANCELLED → any (final state)

---

### BR-INV-003: Installment Plan Rules
**Quy tắc**: Installment plan phải thỏa mãn các điều kiện về số kỳ, amount, và due dates.

**Validation**:
```java
InstallmentPlan validation:
  1. Number of installments: 2 <= n <= 12
  2. Sum of installment amounts = invoice.total_amount
  3. Each installment.due_date > previous.due_date
  4. First installment.due_date >= invoice.due_date
  5. Minimum installment amount >= 500,000 VND (configurable)

Example:
  Invoice total: 10,000,000 VND
  Installments: 4 kỳ
  Plan:
    - Installment 1: 2,500,000 VND, due: 2026-02-01
    - Installment 2: 2,500,000 VND, due: 2026-03-01
    - Installment 3: 2,500,000 VND, due: 2026-04-01
    - Installment 4: 2,500,000 VND, due: 2026-05-01
```

**Business Logic**:
- Installment plan chỉ apply cho invoices >= 5,000,000 VND
- Student phải được approve trước khi apply installment plan (credit check - manual)
- Nếu 1 installment overdue > 15 days, entire plan bị cancel và invoice due immediately

---

### BR-INV-004: Late Fee Calculation
**Quy tắc**: Late fee được tính tự động khi invoice overdue.

**Formula**:
```java
Late fee calculation:
  base_amount = invoice.total_amount - invoice.paid_amount
  days_overdue = current_date - invoice.due_date

  if (days_overdue <= 0):
    late_fee = 0
  else:
    // Option 1: Percentage per day (default)
    late_fee_percent = 0.1% per day (configurable, max 10%)
    late_fee = base_amount * late_fee_percent * days_overdue

    // Option 2: Fixed amount per period
    // late_fee = 50,000 VND per week

    // Cap late fee at max amount
    max_late_fee = base_amount * 10% (configurable)
    late_fee = min(late_fee, max_late_fee)

Example:
  Invoice amount: 10,000,000 VND
  Days overdue: 30 days
  Late fee: 10,000,000 * 0.1% * 30 = 300,000 VND (3%)
```

**Notes**:
- Late fee được add vào invoice như InvoiceAdjustment (type = LATE_FEE)
- Late fee được tính lại mỗi ngày bởi cron job
- Student có thể request waive late fee (requires admin approval)

---

### BR-INV-005: Payment Allocation Rules
**Quy tắc**: Khi payment được nhận, phân bổ vào invoice items theo thứ tự ưu tiên.

**Allocation Priority**:
```
1. Late fees (InvoiceAdjustment type = LATE_FEE)
2. Original invoice items (TUITION, MATERIALS, etc.)
3. Other adjustments

Example:
  Invoice:
    - Tuition: 10,000,000 VND
    - Late fee: 300,000 VND
    - Total: 10,300,000 VND

  Payment received: 5,000,000 VND
  Allocation:
    - Late fee: 300,000 VND (fully paid)
    - Tuition: 4,700,000 VND (partially paid)
    - Remaining balance: 5,300,000 VND
```

**Status Update**:
```java
if (invoice.paid_amount >= invoice.total_amount):
  invoice.status = PAID
  invoice.paid_at = now()
else:
  invoice.status = PENDING or OVERDUE (depending on due_date)
```

---

### BR-INV-006: Invoice Adjustment Rules
**Quy tắc**: Invoice adjustments (discounts, additional charges, late fees) phải được log và require approval.

**Adjustment Types**:
```
DISCOUNT: Giảm giá (early bird, scholarship, sibling discount, etc.)
  - amount: negative value
  - requires: approval if > 10% of invoice total

ADDITIONAL_CHARGE: Phí phụ thu (materials, exam fee, etc.)
  - amount: positive value
  - requires: approval if > 20% of original invoice

LATE_FEE: Phí trễ hạn
  - amount: positive value
  - auto-calculated by system (BR-INV-004)

REFUND: Hoàn tiền
  - amount: negative value
  - requires: admin approval always
```

**Validation**:
```java
Adjustment validation:
  1. Total amount after adjustments >= 0 (cannot negative invoice)
  2. Discounts cannot exceed 100% of original amount
  3. All adjustments require reason notes
  4. Large adjustments (> threshold) require admin approval
```

---

### BR-INV-007: QR Code Payment Expiry
**Quy tắc**: QR code payment chỉ hợp lệ trong thời gian giới hạn.

**Validation**:
```java
QR Code generation:
  - Generated on-demand when student views invoice
  - Contains: invoice_id, amount, student_info, merchant_info
  - Expiry: 15 minutes (for security)
  - After expiry: Student must refresh page to generate new QR code

QR Code format (VNPay/MoMo standard):
  - Bank transfer info with invoice_id in description
  - Amount: exact invoice balance (total - paid)
  - Content: "KITECLASS INV{invoice_id} {student_name}"
```

---

### BR-INV-008: Refund Validation Rules
**Quy tắc**: Refund chỉ được issue khi enrollment cancelled và invoice đã paid.

**Validation**:
```java
Refund allowed if:
  1. Invoice status = PAID
  2. Enrollment status = CANCELLED or WITHDRAWN
  3. Refund requested within refund window (depends on class start date)
  4. Refund amount <= paid_amount

Refund calculation:
  if (class not started):
    refund_amount = paid_amount - registration_fee (non-refundable)
  else if (class progress < 25%):
    refund_amount = paid_amount * 75%
  else if (class progress < 50%):
    refund_amount = paid_amount * 50%
  else:
    refund_amount = 0 (no refund after 50% progress)

registration_fee = 500,000 VND (configurable, non-refundable)
```

**Process**:
1. Student/Admin requests refund
2. System calculates refund_amount
3. Admin approves refund
4. Create Payment record (type = REFUND, amount = negative)
5. Update invoice: status = REFUNDED, refund_amount
6. Publish INVOICE_REFUNDED event

---

## Use Cases

### UC-INV-001: Auto-Create Invoice on Enrollment

**Actor**: System (automated)

**Preconditions**:
- Student enrolled vào class (Enrollment created with status = ACTIVE)
- Class has course with price configured

**Trigger**: `ENROLLMENT_CREATED` event received

**Luồng chính**:
1. System receives ENROLLMENT_CREATED event:
   ```json
   {
     "enrollment_id": 123,
     "student_id": 1001,
     "class_id": 45,
     "course_id": 10,
     "enrolled_at": "2026-01-28T10:00:00Z"
   }
   ```
2. System fetches course price:
   ```java
   Course course = courseService.getById(10);
   BigDecimal coursePrice = course.getPrice(); // 10,000,000 VND
   ```
3. System calculates due_date:
   ```java
   LocalDate dueDate = enrollment.enrolledAt.plusDays(paymentTerm); // default: 7 days
   // Due date: 2026-02-04
   ```
4. System creates Invoice:
   ```java
   Invoice invoice = Invoice.builder()
     .studentId(1001)
     .classId(45)
     .enrollmentId(123)
     .invoiceNumber(generateInvoiceNumber()) // INV-2026-001234
     .totalAmount(coursePrice)
     .paidAmount(BigDecimal.ZERO)
     .dueDate(dueDate)
     .status(InvoiceStatus.PENDING)
     .build();
   ```
5. System creates InvoiceItem:
   ```java
   InvoiceItem item = InvoiceItem.builder()
     .invoiceId(invoice.getId())
     .type(InvoiceItemType.TUITION)
     .description("Học phí khóa " + course.getName())
     .quantity(1)
     .unitPrice(coursePrice)
     .amount(coursePrice)
     .build();
   ```
6. System saves Invoice và InvoiceItem
7. System publishes INVOICE_CREATED event
8. System gửi invoice notification cho student (email với PDF attachment)

**Postconditions**:
- Invoice được tạo với status = PENDING
- Student nhận email với invoice details và payment instructions
- Invoice hiển thị trong student dashboard

**Business Rules**: BR-INV-001, BR-INV-002

---

### UC-INV-002: View Invoice Details (Student)

**Actor**: Student

**Preconditions**:
- Student has at least 1 invoice
- Student đã login

**Trigger**: Student truy cập "My Invoices" page

**Luồng chính**:
1. Student nhấn "My Invoices" trong navigation
2. System retrieves invoices cho student:
   ```java
   List<Invoice> invoices = invoiceService.getByStudentId(studentId);
   ```
3. System hiển thị invoice list:
   ```
   Invoice #      | Class           | Amount      | Paid     | Balance    | Due Date   | Status
   INV-2026-00123 | English A1      | 10,000,000  | 0        | 10,000,000 | 2026-02-04 | PENDING
   INV-2026-00098 | Math Basic      | 8,000,000   | 8,000,000| 0          | 2025-12-20 | PAID
   ```
4. Student chọn 1 invoice để view details
5. System hiển thị invoice details:
   - Header: Invoice number, date, due date, status
   - Bill to: Student name, email, phone
   - Invoice items:
     ```
     Description                | Quantity | Unit Price  | Amount
     Học phí khóa English A1   | 1        | 10,000,000  | 10,000,000
     ```
   - Adjustments (if any):
     ```
     Early Bird Discount        | -500,000
     Late Fee (15 days)         | +150,000
     ```
   - Summary:
     ```
     Subtotal:        10,000,000 VND
     Adjustments:       -350,000 VND
     Total Amount:     9,650,000 VND
     Paid Amount:              0 VND
     Balance Due:      9,650,000 VND
     ```
   - Payment history (if any)
   - Action buttons:
     - "Pay Now" (UC-INV-004)
     - "Request Installment Plan" (UC-INV-005)
     - "Download PDF"

**Postconditions**:
- Student sees complete invoice details
- Student có thể proceed to payment

---

### UC-INV-003: Generate Invoice PDF

**Actor**: Student, Teacher, Admin

**Preconditions**:
- Invoice exists
- User có quyền view invoice (student owns it, teacher teaches class, admin)

**Trigger**: User nhấn "Download PDF" trong invoice details

**Luồng chính**:
1. User nhấn "Download PDF"
2. System generates PDF với template:
   ```
   ==========================================
   KITECLASS LANGUAGE CENTER
   Address: 123 Nguyen Hue St., District 1, HCMC
   Phone: 028-1234-5678
   Email: billing@kiteclass.com
   Tax Code: 0123456789
   ==========================================

   INVOICE
   Invoice #: INV-2026-00123
   Date: 28/01/2026
   Due Date: 04/02/2026

   BILL TO:
   Student: Nguyen Van A
   Email: nguyenvana@email.com
   Phone: 0901234567
   Class: English A1 (Morning - Mon, Wed, Fri)

   INVOICE DETAILS:
   -------------------------------------------
   Description              | Qty | Unit Price    | Amount
   Học phí khóa English A1 | 1   | 10,000,000   | 10,000,000

   ADJUSTMENTS:
   Early Bird Discount (-5%)                    -500,000
   -------------------------------------------

   Subtotal:                                  10,000,000 VND
   Total Adjustments:                           -500,000 VND
   TOTAL AMOUNT:                               9,500,000 VND
   Paid Amount:                                        0 VND
   BALANCE DUE:                                9,500,000 VND

   PAYMENT INFORMATION:
   Bank: Vietcombank
   Account Number: 0123456789
   Account Name: KITECLASS EDUCATION JSC
   Content: KITECLASS INV-2026-00123 NGUYENVANA

   Or scan QR code to pay via VNPay/MoMo:
   [QR CODE IMAGE]

   Notes:
   - Please include invoice number in payment content
   - Late fee: 0.1% per day after due date
   - For questions, contact billing@kiteclass.com

   ==========================================
   ```
3. System saves PDF to storage (S3)
4. System downloads PDF to user device
5. System logs PDF generation for audit

**Postconditions**:
- PDF được tạo và download
- User có thể print hoặc share PDF

---

### UC-INV-004: Pay Invoice via QR Code

**Actor**: Student

**Preconditions**:
- Invoice exists với status = PENDING or OVERDUE
- Balance due > 0

**Trigger**: Student nhấn "Pay Now" trong invoice details

**Luồng chính**:
1. Student nhấn "Pay Now"
2. System hiển thị payment options:
   - QR Code Payment (VNPay, MoMo, ZaloPay)
   - Bank Transfer
   - Cash Payment (at center)
3. Student chọn "QR Code Payment"
4. System generates QR code:
   ```java
   QRCodePayload payload = QRCodePayload.builder()
     .invoiceId(invoice.getId())
     .amount(invoice.getBalanceDue())
     .studentName(student.getName())
     .description("KITECLASS INV-" + invoice.getInvoiceNumber() + " " + student.getName())
     .expiresAt(LocalDateTime.now().plusMinutes(15))
     .build();

   String qrCode = qrCodeService.generate(payload);
   ```
5. System hiển thị QR code với instructions:
   ```
   Scan mã QR bằng app ngân hàng/ví điện tử
   Số tiền: 9,500,000 VND
   Nội dung: KITECLASS INV-2026-00123 NGUYENVANA

   [QR CODE IMAGE]

   QR code hết hạn sau: 14:58

   Hỗ trợ: VNPay, MoMo, ZaloPay, Banking apps
   ```
6. Student scan QR code bằng banking app
7. Student xác nhận payment trong app
8. Payment gateway processes payment
9. Payment gateway gửi webhook callback về system (UC-PAY-003 in Payment Module)
10. System receives payment confirmation
11. System updates invoice (UC-INV-006)
12. System shows success message: "Thanh toán thành công! Invoice đã được cập nhật."

**Postconditions**:
- Payment được process (handled by Payment Module)
- Invoice status updated (PENDING → PAID if full payment)
- Student nhận email confirmation

**Business Rules**: BR-INV-007

**Alternative Flows**:

**A1: QR code hết hạn**
- Tại bước 6, nếu student scan sau 15 minutes
- System shows error: "QR code đã hết hạn. Vui lòng refresh trang để tạo mã mới."
- Student refresh page → back to step 4

**A2: Partial payment**
- Tại bước 8, student chỉ pay 5,000,000 VND (không đủ balance 9,500,000 VND)
- System updates invoice.paid_amount = 5,000,000
- Invoice status remains PENDING
- Balance due = 4,500,000 VND
- Student có thể pay remaining balance later

---

### UC-INV-005: Request Installment Plan (Student)

**Actor**: Student

**Preconditions**:
- Invoice exists với status = PENDING
- Invoice amount >= 5,000,000 VND (minimum for installment)
- Student chưa có installment plan cho invoice này

**Trigger**: Student nhấn "Request Installment Plan"

**Luồng chính**:
1. Student mở invoice details
2. Student nhấn "Request Installment Plan"
3. System validates eligibility:
   - Invoice amount >= 5,000,000 VND ✅
   - No existing installment plan ✅
   - Enrollment status = ACTIVE ✅
4. System hiển thị installment plan form:
   ```
   Invoice Total: 10,000,000 VND
   Number of Installments: [Dropdown: 2, 3, 4, 6, 12 months]
   Selected: 4 installments

   Proposed Plan:
   Installment 1: 2,500,000 VND - Due: 01/02/2026
   Installment 2: 2,500,000 VND - Due: 01/03/2026
   Installment 3: 2,500,000 VND - Due: 01/04/2026
   Installment 4: 2,500,000 VND - Due: 01/05/2026

   [☑] Tôi đồng ý với điều khoản trả góp
   [Submit Request]
   ```
5. Student reviews plan và nhấn "Submit Request"
6. System validates plan (BR-INV-003):
   - 2 <= installments <= 12 ✅
   - Sum = total_amount ✅
   - Each amount >= 500,000 VND ✅
7. System creates InstallmentPlanRequest:
   ```java
   InstallmentPlanRequest request = InstallmentPlanRequest.builder()
     .invoiceId(invoice.getId())
     .studentId(student.getId())
     .numberOfInstallments(4)
     .totalAmount(10_000_000)
     .status(RequestStatus.PENDING)
     .build();
   ```
8. System gửi notification cho Admin/Billing team to approve
9. System shows message: "Yêu cầu trả góp đã được gửi. Chúng tôi sẽ phản hồi trong 24h."

**Postconditions**:
- InstallmentPlanRequest created với status = PENDING
- Admin receives notification để review (UC-INV-007)
- Student waits for approval

**Business Rules**: BR-INV-003

**Alternative Flows**:

**A1: Invoice amount quá nhỏ**
- Tại bước 3, nếu invoice amount < 5,000,000 VND
- System shows error: "Trả góp chỉ áp dụng cho hóa đơn từ 5,000,000 VND trở lên."
- End use case

---

### UC-INV-006: Update Invoice After Payment

**Actor**: System (automated)

**Preconditions**:
- Payment đã được confirmed (from Payment Module)
- Invoice exists

**Trigger**: `PAYMENT_COMPLETED` event received from Payment Module

**Luồng chính**:
1. System receives event:
   ```json
   {
     "event_type": "PAYMENT_COMPLETED",
     "data": {
       "payment_id": 789,
       "invoice_id": 123,
       "amount": 5_000_000,
       "paid_at": "2026-01-28T14:30:00Z",
       "payment_method": "VNPAY"
     }
   }
   ```
2. System retrieves Invoice:
   ```java
   Invoice invoice = invoiceRepository.findById(123);
   ```
3. System applies payment theo BR-INV-005 (allocation priority):
   ```java
   BigDecimal remainingPayment = 5_000_000;

   // Step 1: Pay late fees first
   for (InvoiceAdjustment lateFee : invoice.getLateFees()) {
     if (remainingPayment >= lateFee.getUnpaidAmount()) {
       lateFee.setPaidAmount(lateFee.getAmount());
       remainingPayment -= lateFee.getUnpaidAmount();
     } else {
       lateFee.setPaidAmount(lateFee.getPaidAmount() + remainingPayment);
       remainingPayment = 0;
       break;
     }
   }

   // Step 2: Pay invoice items
   for (InvoiceItem item : invoice.getItems()) {
     if (remainingPayment >= item.getUnpaidAmount()) {
       item.setPaidAmount(item.getAmount());
       remainingPayment -= item.getUnpaidAmount();
     } else {
       item.setPaidAmount(item.getPaidAmount() + remainingPayment);
       remainingPayment = 0;
       break;
     }
   }
   ```
4. System updates invoice totals:
   ```java
   invoice.setPaidAmount(invoice.getPaidAmount() + 5_000_000);
   BigDecimal balanceDue = invoice.getTotalAmount() - invoice.getPaidAmount();
   ```
5. System updates invoice status:
   ```java
   if (balanceDue <= 0) {
     invoice.setStatus(InvoiceStatus.PAID);
     invoice.setPaidAt(LocalDateTime.now());
   } else {
     // Remains PENDING or OVERDUE
   }
   ```
6. System saves invoice
7. System publishes INVOICE_PAID event (if fully paid)
8. System gửi payment confirmation email cho student

**Postconditions**:
- Invoice.paid_amount updated
- Invoice status updated (→ PAID if full payment)
- Student nhận email confirmation
- Enrollment status may be updated (from PENDING_PAYMENT → ACTIVE)

**Business Rules**: BR-INV-005

---

### UC-INV-007: Approve/Reject Installment Plan Request (Admin)

**Actor**: Admin (Billing team)

**Preconditions**:
- InstallmentPlanRequest exists với status = PENDING

**Trigger**: Admin receives notification về new request

**Luồng chính**:
1. Admin mở "Installment Requests" page
2. System hiển thị pending requests:
   ```
   Student      | Invoice #      | Amount      | Installments | Requested Date | Action
   Nguyen Van A | INV-2026-00123 | 10,000,000  | 4 months     | 2026-01-28     | [View]
   ```
3. Admin chọn request để review
4. System hiển thị request details:
   ```
   Student: Nguyen Van A
   Email: nguyenvana@email.com
   Invoice: INV-2026-00123 - English A1
   Total Amount: 10,000,000 VND

   Proposed Plan:
   - Installment 1: 2,500,000 VND - Due: 01/02/2026
   - Installment 2: 2,500,000 VND - Due: 01/03/2026
   - Installment 3: 2,500,000 VND - Due: 01/04/2026
   - Installment 4: 2,500,000 VND - Due: 01/05/2026

   Student History:
   - Total invoices: 2
   - Paid on time: 1
   - Overdue: 1 (5 days late)
   - Credit score: 75/100 (Medium risk)

   [Approve] [Reject] [Request More Info]
   ```
5. Admin reviews student credit history
6. Admin nhấn "Approve"
7. System creates InstallmentPlan:
   ```java
   InstallmentPlan plan = InstallmentPlan.builder()
     .invoiceId(invoice.getId())
     .numberOfInstallments(4)
     .status(PlanStatus.ACTIVE)
     .build();

   // Create installments
   for (int i = 0; i < 4; i++) {
     Installment installment = Installment.builder()
       .planId(plan.getId())
       .installmentNumber(i + 1)
       .amount(2_500_000)
       .dueDate(LocalDate.of(2026, 2 + i, 1))
       .status(InstallmentStatus.PENDING)
       .build();
   }
   ```
8. System updates InstallmentPlanRequest.status = APPROVED
9. System updates Invoice.due_date = first installment due date
10. System publishes INSTALLMENT_PLAN_APPROVED event
11. System gửi approval email cho student with plan details

**Postconditions**:
- InstallmentPlan created và active
- Invoice due date updated
- Student nhận email với installment schedule
- Student có thể pay theo installments

**Business Rules**: BR-INV-003

**Alternative Flows**:

**A1: Reject request**
- Tại bước 6, admin nhấn "Reject"
- System shows rejection reason form
- Admin nhập reason: "Lịch sử thanh toán không tốt"
- System updates request.status = REJECTED, saves reason
- System gửi rejection email cho student
- Student có thể submit new request hoặc pay full amount

---

### UC-INV-008: Calculate and Apply Late Fee

**Actor**: System (cron job, chạy daily)

**Preconditions**: None

**Trigger**: Cron job chạy hàng ngày lúc 00:00

**Luồng chính**:
1. Cron job triggers at midnight
2. System queries overdue invoices:
   ```java
   LocalDate today = LocalDate.now();
   List<Invoice> overdueInvoices = invoiceRepository.findByStatusAndDueDateBefore(
     InvoiceStatus.OVERDUE, today
   );
   ```
3. Với mỗi overdue invoice:
   ```java
   for (Invoice invoice : overdueInvoices) {
     // Calculate days overdue
     long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);

     // Calculate late fee (BR-INV-004)
     BigDecimal baseAmount = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
     BigDecimal dailyRate = new BigDecimal("0.001"); // 0.1% per day
     BigDecimal lateFee = baseAmount.multiply(dailyRate).multiply(new BigDecimal(daysOverdue));

     // Cap at 10% of base amount
     BigDecimal maxLateFee = baseAmount.multiply(new BigDecimal("0.10"));
     lateFee = lateFee.min(maxLateFee);

     // Check if late fee already exists
     InvoiceAdjustment existingLateFee = invoice.getAdjustments()
       .stream()
       .filter(adj -> adj.getType() == AdjustmentType.LATE_FEE)
       .findFirst()
       .orElse(null);

     if (existingLateFee != null) {
       // Update existing late fee
       existingLateFee.setAmount(lateFee);
       existingLateFee.setDescription("Phí trễ hạn (" + daysOverdue + " ngày)");
     } else {
       // Create new late fee adjustment
       InvoiceAdjustment lateFeeAdj = InvoiceAdjustment.builder()
         .invoiceId(invoice.getId())
         .type(AdjustmentType.LATE_FEE)
         .description("Phí trễ hạn (" + daysOverdue + " ngày)")
         .amount(lateFee)
         .build();
       invoice.addAdjustment(lateFeeAdj);
     }

     // Recalculate total amount
     invoice.recalculateTotalAmount();
   }
   ```
4. System saves updated invoices
5. System publishes LATE_FEE_APPLIED event for each invoice
6. System gửi overdue notice email cho students (with updated late fee)

**Postconditions**:
- Late fees calculated và added to overdue invoices
- Invoice total amounts updated
- Students nhận overdue notices

**Business Rules**: BR-INV-004

---

### UC-INV-009: Apply Discount/Adjustment (Admin)

**Actor**: Admin

**Preconditions**:
- Invoice exists
- Admin has BILLING_ADMIN permission

**Trigger**: Admin cần apply discount hoặc additional charge

**Luồng chính**:
1. Admin mở invoice details
2. Admin nhấn "Apply Adjustment"
3. System hiển thị adjustment form:
   ```
   Adjustment Type: [Dropdown: Discount, Additional Charge, Waive Late Fee]
   Description: [Textbox]
   Amount: [Number] VND
   Reason: [Textbox, required]

   Current Total: 10,000,000 VND
   Adjustment: -500,000 VND (discount)
   New Total: 9,500,000 VND

   [Require Approval] (checkbox, auto-checked if amount > threshold)
   ```
4. Admin fills form:
   - Type: Discount
   - Description: "Early Bird Discount - Enrolled 1 month before class start"
   - Amount: -500,000 VND
   - Reason: "Promotion for early enrollment"
5. Admin nhấn "Apply"
6. System validates (BR-INV-006):
   - Amount hợp lệ (discount không vượt quá 100% invoice)
   - Reason provided ✅
   - Check if requires approval (discount > 10% of total)
7. Nếu requires approval:
   - System creates AdjustmentApprovalRequest
   - System gửi notification cho senior admin
   - Shows message: "Adjustment submitted for approval"
8. Nếu không require approval hoặc đã approved:
   - System creates InvoiceAdjustment:
     ```java
     InvoiceAdjustment adjustment = InvoiceAdjustment.builder()
       .invoiceId(invoice.getId())
       .type(AdjustmentType.DISCOUNT)
       .description("Early Bird Discount - Enrolled 1 month before class start")
       .amount(new BigDecimal("-500000"))
       .reason("Promotion for early enrollment")
       .appliedBy(admin.getId())
       .appliedAt(LocalDateTime.now())
       .build();
     ```
   - System recalculates invoice total:
     ```java
     invoice.recalculateTotalAmount(); // 10,000,000 - 500,000 = 9,500,000
     ```
   - System saves adjustment và invoice
   - System publishes INVOICE_ADJUSTED event
   - System gửi notification cho student về adjustment
   - Shows success message

**Postconditions**:
- InvoiceAdjustment created
- Invoice total amount updated
- Student nhận notification
- Adjustment logged for audit

**Business Rules**: BR-INV-006

---

### UC-INV-010: Request Refund (Student/Admin)

**Actor**: Student hoặc Admin

**Preconditions**:
- Invoice exists với status = PAID
- Enrollment cancelled (status = CANCELLED or WITHDRAWN)
- Within refund window

**Trigger**: Student/Admin nhấn "Request Refund"

**Luồng chính**:
1. Student mở invoice details (invoice đã PAID)
2. Student nhấn "Request Refund" (button chỉ hiện khi eligible)
3. System validates refund eligibility (BR-INV-008):
   - Invoice status = PAID ✅
   - Enrollment status = CANCELLED ✅
   - Within refund window ✅
4. System calculates refund amount:
   ```java
   BigDecimal paidAmount = invoice.getPaidAmount(); // 10,000,000
   BigDecimal registrationFee = new BigDecimal("500000"); // non-refundable
   int classProgress = classService.getProgress(invoice.getClassId()); // 0%

   BigDecimal refundAmount;
   if (classProgress == 0) {
     refundAmount = paidAmount.subtract(registrationFee); // 9,500,000
   } else if (classProgress < 25) {
     refundAmount = paidAmount.multiply(new BigDecimal("0.75")); // 75%
   } else if (classProgress < 50) {
     refundAmount = paidAmount.multiply(new BigDecimal("0.50")); // 50%
   } else {
     refundAmount = BigDecimal.ZERO; // No refund
   }
   ```
5. System hiển thị refund calculator:
   ```
   Refund Calculation:
   Paid Amount: 10,000,000 VND
   Registration Fee (non-refundable): -500,000 VND
   Class Progress: 0%
   Refund Rate: 100% (of refundable amount)

   Estimated Refund: 9,500,000 VND

   Refund Method:
   [ ] Bank Transfer (Account: _______________)
   [ ] Cash at Center
   [ ] Credit to Account (for future classes)

   Reason for Refund: [Textbox, required]

   [Submit Refund Request]
   ```
6. Student fills form và nhấn "Submit Refund Request"
7. System creates RefundRequest:
   ```java
   RefundRequest request = RefundRequest.builder()
     .invoiceId(invoice.getId())
     .requestedBy(student.getId())
     .refundAmount(new BigDecimal("9500000"))
     .refundMethod(RefundMethod.BANK_TRANSFER)
     .bankAccount("0123456789")
     .reason("Chuyển công tác ra Hà Nội")
     .status(RefundRequestStatus.PENDING)
     .build();
   ```
8. System gửi notification cho Admin to approve
9. System shows message: "Yêu cầu hoàn tiền đã được gửi. Chúng tôi sẽ xử lý trong 3-5 ngày làm việc."

**Postconditions**:
- RefundRequest created với status = PENDING
- Admin receives notification để process (UC-INV-011)
- Student waits for refund processing

**Business Rules**: BR-INV-008

---

### UC-INV-011: Process Refund (Admin)

**Actor**: Admin (Billing team)

**Preconditions**:
- RefundRequest exists với status = PENDING

**Trigger**: Admin receives refund request notification

**Luồng chính**:
1. Admin mở "Refund Requests" page
2. System hiển thị pending requests
3. Admin chọn request để review
4. System hiển thị request details với refund calculation breakdown
5. Admin verifies:
   - Enrollment cancelled ✅
   - Calculation correct ✅
   - Bank account valid ✅
6. Admin nhấn "Approve Refund"
7. System prompts for confirmation:
   ```
   Confirm Refund:
   Amount: 9,500,000 VND
   Method: Bank Transfer to 0123456789
   Student: Nguyen Van A

   [Confirm] [Cancel]
   ```
8. Admin nhấn "Confirm"
9. System processes refund:
   ```java
   // Create Payment record (type = REFUND)
   Payment refund = Payment.builder()
     .invoiceId(invoice.getId())
     .amount(new BigDecimal("-9500000")) // negative amount
     .paymentMethod(PaymentMethod.BANK_TRANSFER)
     .paymentType(PaymentType.REFUND)
     .status(PaymentStatus.COMPLETED)
     .processedBy(admin.getId())
     .build();

   // Update invoice
   invoice.setPaidAmount(invoice.getPaidAmount().subtract(refundAmount));
   invoice.setRefundAmount(refundAmount);
   invoice.setStatus(InvoiceStatus.REFUNDED);

   // Update refund request
   request.setStatus(RefundRequestStatus.APPROVED);
   request.setProcessedBy(admin.getId());
   request.setProcessedAt(LocalDateTime.now());
   ```
10. System publishes INVOICE_REFUNDED event
11. System tạo refund transaction task cho Finance team
12. System gửi refund confirmation email cho student
13. System shows success message: "Refund approved. Finance team sẽ chuyển tiền trong 3-5 ngày."

**Postconditions**:
- Payment (REFUND) created
- Invoice status = REFUNDED
- RefundRequest status = APPROVED
- Student nhận confirmation email
- Finance team receives task to transfer money

**Business Rules**: BR-INV-008

---

### UC-INV-012: View Debt Aging Report (Admin)

**Actor**: Admin

**Preconditions**: None

**Trigger**: Admin muốn xem debt aging report

**Luồng chính**:
1. Admin truy cập "Reports > Debt Aging"
2. System queries overdue invoices:
   ```java
   List<Invoice> overdueInvoices = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
   ```
3. System categorizes debts by aging:
   ```java
   Map<String, List<Invoice>> agingBuckets = new HashMap<>();
   LocalDate today = LocalDate.now();

   for (Invoice invoice : overdueInvoices) {
     long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
     String bucket;

     if (daysOverdue <= 30) {
       bucket = "0-30 days";
     } else if (daysOverdue <= 60) {
       bucket = "31-60 days";
     } else if (daysOverdue <= 90) {
       bucket = "61-90 days";
     } else {
       bucket = "90+ days";
     }

     agingBuckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(invoice);
   }
   ```
4. System calculates summary:
   ```java
   DebtAgingSummary {
     total_overdue_invoices: 45,
     total_overdue_amount: 320,500,000 VND,

     breakdown: {
       "0-30 days": { count: 25, amount: 180,000,000 },
       "31-60 days": { count: 12, amount: 95,500,000 },
       "61-90 days": { count: 5, amount: 30,000,000 },
       "90+ days": { count: 3, amount: 15,000,000 }
     }
   }
   ```
5. System renders report:
   ```
   DEBT AGING REPORT
   As of: 28/01/2026

   Summary:
   Total Overdue Invoices: 45
   Total Overdue Amount: 320,500,000 VND

   Aging Breakdown:
   Age Range    | Count | Total Amount    | % of Total
   0-30 days    | 25    | 180,000,000    | 56.2%
   31-60 days   | 12    | 95,500,000     | 29.8%
   61-90 days   | 5     | 30,000,000     | 9.4%
   90+ days     | 3     | 15,000,000     | 4.7%

   Detailed List (90+ days - High Risk):
   Student      | Invoice #      | Amount      | Days Overdue | Late Fee
   Tran Thi B   | INV-2025-00567 | 8,000,000   | 95           | 800,000
   Le Van C     | INV-2025-00489 | 5,000,000   | 102          | 500,000
   Pham Thi D   | INV-2025-00423 | 2,000,000   | 120          | 200,000

   [Export Excel] [Send Collection Notices]
   ```

**Postconditions**:
- Admin có overview về debt situation
- Admin identify high-risk accounts để follow up

---

## Entity Design

### Invoice Entity

**Purpose**: Hóa đơn học phí cho enrollment

**Database Schema**:
```sql
CREATE TABLE invoices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    enrollment_id BIGINT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    refund_amount DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    due_date DATE NOT NULL,
    issued_date DATE NOT NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_invoice_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES enrollments(id) ON DELETE CASCADE,
    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'PENDING', 'PAID', 'OVERDUE', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_invoice_amounts CHECK (paid_amount <= total_amount AND paid_amount >= 0)
);

CREATE INDEX idx_invoice_student ON invoices(student_id);
CREATE INDEX idx_invoice_class ON invoices(class_id);
CREATE INDEX idx_invoice_enrollment ON invoices(enrollment_id);
CREATE INDEX idx_invoice_status ON invoices(status);
CREATE INDEX idx_invoice_due_date ON invoices(due_date);
```

---

### InvoiceItem Entity

**Database Schema**:
```sql
CREATE TABLE invoice_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(15,2) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,

    CONSTRAINT fk_item_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_item_type CHECK (type IN ('TUITION', 'MATERIALS', 'REGISTRATION_FEE', 'EXAM_FEE', 'OTHER'))
);

CREATE INDEX idx_item_invoice ON invoice_items(invoice_id);
```

---

### InvoiceAdjustment Entity

**Database Schema**:
```sql
CREATE TABLE invoice_adjustments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    reason TEXT,
    applied_by BIGINT,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_adjustment_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_adjustment_type CHECK (type IN ('DISCOUNT', 'ADDITIONAL_CHARGE', 'LATE_FEE', 'REFUND'))
);

CREATE INDEX idx_adjustment_invoice ON invoice_adjustments(invoice_id);
```

---

### InstallmentPlan Entity

**Database Schema**:
```sql
CREATE TABLE installment_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL UNIQUE,
    number_of_installments INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_plan_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_plan_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE installments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    installment_number INT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP NULL,

    CONSTRAINT fk_installment_plan FOREIGN KEY (plan_id)
        REFERENCES installment_plans(id) ON DELETE CASCADE,
    CONSTRAINT chk_installment_status CHECK (status IN ('PENDING', 'PAID', 'OVERDUE')),
    UNIQUE (plan_id, installment_number)
);

CREATE INDEX idx_installment_plan ON installments(plan_id);
CREATE INDEX idx_installment_due_date ON installments(due_date);
```

---

### RefundRequest Entity

**Database Schema**:
```sql
CREATE TABLE refund_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    refund_amount DECIMAL(15,2) NOT NULL,
    refund_method VARCHAR(50) NOT NULL,
    bank_account VARCHAR(50) NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_by BIGINT NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refund_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_refund_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')),
    CONSTRAINT chk_refund_method CHECK (refund_method IN ('BANK_TRANSFER', 'CASH', 'CREDIT'))
);

CREATE INDEX idx_refund_invoice ON refund_requests(invoice_id);
CREATE INDEX idx_refund_status ON refund_requests(status);
```

---

## Integration Points

### 1. Enrollment Module Integration
**Purpose**: Auto-create invoice khi enrollment created

**Event-Driven**:
```java
@EventListener
void onEnrollmentCreated(EnrollmentCreatedEvent event) {
    invoiceService.createInvoiceForEnrollment(event.getEnrollmentId());
}
```

---

### 2. Payment Module Integration
**Purpose**: Update invoice khi payment received

**Event-Driven**:
```java
@EventListener
void onPaymentCompleted(PaymentCompletedEvent event) {
    invoiceService.applyPayment(event.getInvoiceId(), event.getPaymentId(), event.getAmount());
}
```

---

### 3. Class Module Integration
**Purpose**: Get class progress cho refund calculation

**API**:
```java
GET /api/v1/classes/{classId}/progress
Response: { progress_percent: 25 }
```

---

## Events & Notifications

### Published Events

**1. INVOICE_CREATED**
```json
{
  "event_type": "INVOICE_CREATED",
  "data": {
    "invoice_id": 123,
    "student_id": 1001,
    "amount": 10000000,
    "due_date": "2026-02-04"
  }
}
```

**2. INVOICE_PAID**
```json
{
  "event_type": "INVOICE_PAID",
  "data": {
    "invoice_id": 123,
    "student_id": 1001,
    "paid_amount": 10000000,
    "paid_at": "2026-01-30T14:30:00Z"
  }
}
```

**3. LATE_FEE_APPLIED**
```json
{
  "event_type": "LATE_FEE_APPLIED",
  "data": {
    "invoice_id": 123,
    "late_fee_amount": 300000,
    "days_overdue": 30
  }
}
```

---

## Summary

Invoice Module quản lý hóa đơn học phí với:
- ✅ Auto-generation from enrollments
- ✅ Lifecycle management (DRAFT → PENDING → PAID/OVERDUE/CANCELLED/REFUNDED)
- ✅ Installment plans (trả góp 2-12 tháng)
- ✅ Late fee calculation (0.1%/day, max 10%)
- ✅ QR code payments
- ✅ Refund handling với tiered rates
- ✅ Debt aging reports
- ✅ Payment allocation rules

**Business Rules**: 8 rules covering auto-generation, lifecycle, installments, late fees, payment allocation, adjustments, QR codes, refunds

**Use Cases**: 12 use cases covering full invoice lifecycle from creation to refund processing
