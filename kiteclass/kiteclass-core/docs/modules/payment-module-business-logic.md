# Payment Module - Business Logic

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
Payment Module xử lý tất cả các giao dịch thanh toán trong hệ thống. Module này:
- Ghi nhận và tracking tất cả payments (invoice payments, refunds, adjustments)
- Tích hợp với payment gateways (VNPay, MoMo, ZaloPay)
- Xử lý webhook callbacks từ payment gateways
- Quản lý payment lifecycle: PENDING → PROCESSING → COMPLETED/FAILED
- Hỗ trợ nhiều payment methods (Cash, Bank Transfer, E-wallets, Credit Card)
- Payment reconciliation (đối soát)
- Retry failed payments
- Payment history và transaction logs

### Scope
**Bao gồm:**
- ✅ Multiple payment methods: Cash, Bank Transfer, VNPay, MoMo, ZaloPay
- ✅ Payment gateway integration (webhook handling)
- ✅ Payment status tracking (PENDING → PROCESSING → COMPLETED/FAILED)
- ✅ Automatic payment allocation to invoices (via Invoice Module)
- ✅ Payment reconciliation (đối soát with bank statements)
- ✅ Failed payment retry mechanism
- ✅ Payment receipts (PDF generation)
- ✅ Payment history và transaction logs
- ✅ Payment analytics và reports
- ✅ Refund processing

**Không bao gồm:**
- ❌ Direct credit card processing (PCI compliance) - Use payment gateways
- ❌ International payments (USD, EUR) - Future
- ❌ Cryptocurrency payments - Future
- ❌ Payment fraud detection - Future (integrate with 3rd party)

### Key Concepts
- **Payment**: Một giao dịch thanh toán (invoice payment hoặc refund)
- **Payment Method**: Phương thức thanh toán (Cash, Bank Transfer, VNPay, etc.)
- **Payment Gateway**: Cổng thanh toán (VNPay, MoMo, ZaloPay)
- **Transaction ID**: Mã giao dịch từ payment gateway
- **Webhook**: Callback từ payment gateway để notify payment status
- **Payment Reconciliation**: Đối soát giao dịch với bank statements
- **Payment Receipt**: Phiếu thu (PDF) cho confirmed payments
- **Payment Status**: PENDING (chờ), PROCESSING (đang xử lý), COMPLETED (thành công), FAILED (thất bại), CANCELLED (hủy)

---

## Business Rules

### BR-PAY-001: Payment Uniqueness per Transaction
**Quy tắc**: Mỗi payment có unique transaction_id từ gateway, tránh duplicate processing.

**Validation**:
```java
// When webhook received
Payment existingPayment = paymentRepository.findByTransactionId(transactionId);

if (existingPayment != null) {
  if (existingPayment.getStatus() == PaymentStatus.COMPLETED) {
    // Already processed, return idempotent response
    return ResponseEntity.ok("Payment already processed");
  } else {
    // Update existing payment instead of creating new
    existingPayment.setStatus(newStatus);
    return paymentRepository.save(existingPayment);
  }
}
```

**Lý do**: Webhook có thể gửi nhiều lần (retry), cần idempotency để tránh double-charging.

---

### BR-PAY-002: Payment Status Lifecycle
**Quy tắc**: Payment status transitions theo lifecycle định sẵn.

**State Machine**:
```
PENDING: Payment initiated, chờ confirmation
  ↓ (gateway processing)
PROCESSING: Gateway đang xử lý transaction
  ↓ (webhook success)
COMPLETED: Payment successful
  ↓ (webhook failure)
FAILED: Payment failed (insufficient funds, etc.)

PENDING/PROCESSING → CANCELLED: User cancels before completion
```

**Allowed Transitions**:
```
PENDING → PROCESSING (payment submitted to gateway)
PENDING → CANCELLED (user cancels)
PROCESSING → COMPLETED (webhook success)
PROCESSING → FAILED (webhook failure)
PROCESSING → CANCELLED (timeout, user cancels)
FAILED → PENDING (retry payment)
```

**Forbidden**:
- COMPLETED → any (final state, immutable)
- CANCELLED → any except PENDING (can retry)

---

### BR-PAY-003: Payment Method Validation
**Quy tắc**: Mỗi payment method có validation rules riêng.

**Validation by Method**:
```java
CASH:
  - No external validation required
  - Requires receipt_number (manual input by staff)
  - Requires received_by (staff user_id)

BANK_TRANSFER:
  - Requires bank_transaction_id (reference number)
  - Requires transfer_date
  - Optional: screenshot/proof of transfer

VNPAY/MOMO/ZALOPAY:
  - Requires transaction_id from gateway
  - Requires payment_url (redirect URL)
  - Must have webhook callback URL configured
  - Transaction must complete within timeout (15 minutes)

CREDIT_CARD (future):
  - Requires card token (no raw card data)
  - Requires CVV verification
  - 3D Secure authentication
```

---

### BR-PAY-004: Payment Amount Validation
**Quy tắc**: Payment amount phải match invoice balance và không vượt quá allowed limits.

**Validation**:
```java
Payment validation:
  1. amount > 0 (positive for payments, negative for refunds)
  2. For INVOICE_PAYMENT:
     - amount <= invoice.balance_due
     - amount >= minimum_payment (100,000 VND, configurable)
  3. For REFUND:
     - amount <= invoice.paid_amount
     - Must have approved RefundRequest
  4. Daily limit per payment method:
     - CASH: 50,000,000 VND/day
     - BANK_TRANSFER: 500,000,000 VND/day
     - VNPAY/MOMO/ZALOPAY: 200,000,000 VND/transaction
```

**Exceptions**:
- Admin có thể override limits với approval
- Installment payments có thể nhỏ hơn minimum_payment

---

### BR-PAY-005: Webhook Signature Verification
**Quy tắc**: Tất cả webhook callbacks phải verify signature để đảm bảo authenticity.

**Verification Process**:
```java
// VNPay webhook signature verification
String receivedSignature = request.getParameter("vnp_SecureHash");
String secretKey = vnpayConfig.getSecretKey();

// Build signature string from params (sorted by key)
Map<String, String> params = new TreeMap<>(request.getParameterMap());
params.remove("vnp_SecureHash"); // exclude signature itself

String signData = params.entrySet().stream()
  .map(e -> e.getKey() + "=" + e.getValue())
  .collect(Collectors.joining("&"));

String calculatedSignature = HmacSHA512(signData, secretKey);

if (!calculatedSignature.equals(receivedSignature)) {
  throw new InvalidSignatureException("Webhook signature mismatch - potential fraud");
}
```

**Lý do**: Prevent fraudulent webhook calls từ attackers.

---

### BR-PAY-006: Payment Timeout Rules
**Quy tắc**: Payment qua gateway phải complete trong timeout period, nếu không tự động cancel.

**Timeout Settings**:
```java
Payment timeout by method:
  VNPAY: 15 minutes (from payment_url generated)
  MOMO: 15 minutes
  ZALOPAY: 15 minutes
  BANK_TRANSFER: No timeout (manual confirmation)
  CASH: No timeout (manual confirmation)

// Cron job chạy mỗi 5 phút
@Scheduled(cron = "0 */5 * * * *")
void cancelTimedOutPayments() {
  LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);

  List<Payment> timedOutPayments = paymentRepository.findByStatusAndCreatedAtBefore(
    PaymentStatus.PROCESSING, cutoffTime
  );

  for (Payment payment : timedOutPayments) {
    if (payment.requiresGateway()) {
      payment.setStatus(PaymentStatus.FAILED);
      payment.setFailureReason("Payment timeout - exceeded 15 minutes");
      paymentRepository.save(payment);
      publishEvent(new PaymentFailedEvent(payment));
    }
  }
}
```

---

### BR-PAY-007: Payment Reconciliation Rules
**Quy tắc**: Payments phải được reconcile với bank statements để đảm bảo accuracy.

**Reconciliation Process**:
```java
Daily reconciliation (end of day):
  1. Export all payments for the day (status = COMPLETED)
  2. Download bank statements (API integration or manual)
  3. Match payments với bank transactions:
     - By transaction_id
     - By amount + date + reference
  4. Flag unmatched payments:
     - Missing in bank: Potential failed payment (mark for review)
     - Extra in bank: Missing payment record (create payment)
  5. Generate reconciliation report:
     - Matched: X payments, Y VND
     - Unmatched: Z payments, W VND (requires investigation)
```

**Auto-reconciliation criteria**:
```
Match if:
  (payment.transaction_id == bank_transaction.reference)
  AND
  (payment.amount == bank_transaction.amount)
  AND
  (payment.payment_date within bank_transaction.date ± 1 day)
```

---

### BR-PAY-008: Refund Payment Rules
**Quy tắc**: Refund payments phải follow specific rules và require approval.

**Validation**:
```java
Refund validation:
  1. Original payment must exist và status = COMPLETED
  2. Refund amount <= original payment amount
  3. RefundRequest must be approved (from Invoice Module)
  4. Refund method = original payment method (hoặc BANK_TRANSFER nếu not possible)
  5. Admin approval required for refunds > 5,000,000 VND

Refund processing:
  - Create Payment record với:
    - payment_type = REFUND
    - amount = negative value (e.g., -9,500,000)
    - reference_payment_id = original payment_id
  - Update invoice (handled by Invoice Module)
  - If original payment via gateway: initiate gateway refund
  - If original payment via CASH: create payout task for Finance team
```

---

## Use Cases

### UC-PAY-001: Create Payment for Invoice (Student)

**Actor**: Student

**Preconditions**:
- Invoice exists với status = PENDING or OVERDUE
- Invoice balance > 0

**Trigger**: Student nhấn "Pay Now" trong invoice details

**Luồng chính**:
1. Student views invoice details (UC-INV-002)
2. Student nhấn "Pay Now"
3. System hiển thị payment options:
   ```
   Invoice: INV-2026-00123
   Balance Due: 10,000,000 VND

   Select Payment Method:
   [ ] QR Code Payment (VNPay, MoMo, ZaloPay) - Instant
   [ ] Bank Transfer - Manual confirmation
   [ ] Cash at Center - Manual confirmation

   Amount to Pay: [10,000,000] VND (min: 100,000, max: balance)

   [Continue]
   ```
4. Student chọn "QR Code Payment" và nhấn "Continue"
5. System validates (BR-PAY-004):
   - Amount <= balance_due ✅
   - Amount >= minimum_payment ✅
6. System creates Payment record:
   ```java
   Payment payment = Payment.builder()
     .invoiceId(invoice.getId())
     .studentId(student.getId())
     .amount(new BigDecimal("10000000"))
     .paymentMethod(PaymentMethod.VNPAY)
     .paymentType(PaymentType.INVOICE_PAYMENT)
     .status(PaymentStatus.PENDING)
     .build();
   ```
7. System initiates payment gateway (UC-PAY-002)
8. System redirects student to payment gateway URL

**Postconditions**:
- Payment record created với status = PENDING
- Student redirected to gateway để complete payment
- Payment will be updated via webhook (UC-PAY-003)

**Business Rules**: BR-PAY-002, BR-PAY-003, BR-PAY-004

---

### UC-PAY-002: Initiate Gateway Payment (VNPay)

**Actor**: System

**Preconditions**:
- Payment record created với method = VNPAY
- VNPay credentials configured

**Trigger**: UC-PAY-001 step 7 (student chọn QR payment)

**Luồng chính**:
1. System prepares VNPay payment request:
   ```java
   VNPayRequest request = VNPayRequest.builder()
     .version("2.1.0")
     .command("pay")
     .tmnCode(vnpayConfig.getTmnCode())
     .amount(payment.getAmount().multiply(new BigDecimal("100"))) // VNPay uses cents
     .currencyCode("VND")
     .txnRef(payment.getId().toString()) // Our payment ID
     .orderInfo("Thanh toan hoc phi - INV-" + invoice.getInvoiceNumber())
     .returnUrl(appConfig.getBaseUrl() + "/api/v1/payments/vnpay/return")
     .ipAddress(request.getRemoteAddr())
     .createDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
     .build();
   ```
2. System generates secure hash:
   ```java
   String hashData = buildHashData(request); // Sort params, join with &
   String secureHash = HmacSHA512(hashData, vnpayConfig.getSecretKey());
   request.setSecureHash(secureHash);
   ```
3. System builds payment URL:
   ```java
   String paymentUrl = vnpayConfig.getGatewayUrl() + "?" + buildQueryString(request);
   // https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Version=2.1.0&...
   ```
4. System updates payment:
   ```java
   payment.setPaymentUrl(paymentUrl);
   payment.setTransactionId(generateTransactionId()); // VNPAY-{timestamp}-{random}
   payment.setStatus(PaymentStatus.PROCESSING);
   payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
   paymentRepository.save(payment);
   ```
5. System returns payment URL to caller (UC-PAY-001)

**Postconditions**:
- Payment status = PROCESSING
- payment_url generated
- Student sẽ được redirect to VNPay gateway
- Webhook sẽ receive callback khi payment completes

**Business Rules**: BR-PAY-003, BR-PAY-006

**Notes**:
- Similar flow cho MoMo và ZaloPay với API specs khác nhau
- Each gateway có signature algorithm riêng

---

### UC-PAY-003: Process Payment Gateway Webhook (VNPay)

**Actor**: VNPay Gateway (external system)

**Preconditions**:
- Payment đã initiated (UC-PAY-002)
- Webhook URL configured trong VNPay dashboard

**Trigger**: Student completes payment trên VNPay gateway

**Luồng chính**:
1. VNPay sends webhook callback:
   ```http
   GET /api/v1/payments/vnpay/callback?
     vnp_Amount=1000000000&
     vnp_BankCode=NCB&
     vnp_CardType=ATM&
     vnp_OrderInfo=Thanh+toan+hoc+phi+-+INV-2026-00123&
     vnp_PayDate=20260128143000&
     vnp_ResponseCode=00&
     vnp_TmnCode=KITECLASS&
     vnp_TransactionNo=14123456&
     vnp_TxnRef=789&
     vnp_SecureHash=abc123...
   ```
2. System verifies signature (BR-PAY-005):
   ```java
   String receivedHash = request.getParameter("vnp_SecureHash");
   String calculatedHash = calculateVNPaySignature(request.getParameterMap());

   if (!receivedHash.equals(calculatedHash)) {
     throw new InvalidSignatureException("Invalid webhook signature");
   }
   ```
3. System extracts payment data:
   ```java
   String txnRef = request.getParameter("vnp_TxnRef"); // Our payment ID
   String responseCode = request.getParameter("vnp_ResponseCode"); // 00 = success
   String transactionNo = request.getParameter("vnp_TransactionNo"); // VNPay transaction ID
   BigDecimal amount = new BigDecimal(request.getParameter("vnp_Amount")).divide(new BigDecimal("100"));
   ```
4. System retrieves payment:
   ```java
   Payment payment = paymentRepository.findById(Long.parseLong(txnRef))
     .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + txnRef));
   ```
5. System validates payment (BR-PAY-001):
   - Check if already processed (idempotency)
   - Verify amount matches
6. System updates payment based on response code:
   ```java
   if ("00".equals(responseCode)) {
     // Success
     payment.setStatus(PaymentStatus.COMPLETED);
     payment.setGatewayTransactionId(transactionNo);
     payment.setCompletedAt(LocalDateTime.now());
     payment.setGatewayResponse(buildGatewayResponse(request));

     paymentRepository.save(payment);
     publishEvent(new PaymentCompletedEvent(payment));

   } else {
     // Failed
     payment.setStatus(PaymentStatus.FAILED);
     payment.setFailureReason(getVNPayErrorMessage(responseCode));
     payment.setGatewayResponse(buildGatewayResponse(request));

     paymentRepository.save(payment);
     publishEvent(new PaymentFailedEvent(payment));
   }
   ```
7. System returns acknowledgment to VNPay:
   ```json
   {"RspCode": "00", "Message": "Confirm Success"}
   ```
8. System gửi notification cho student:
   - Success: "Thanh toán thành công! Số tiền: 10,000,000 VND"
   - Failed: "Thanh toán thất bại: {reason}. Vui lòng thử lại."

**Postconditions**:
- Payment status updated (COMPLETED or FAILED)
- Invoice updated (via PAYMENT_COMPLETED event → Invoice Module)
- Student nhận notification
- Payment receipt generated (if COMPLETED)

**Business Rules**: BR-PAY-001, BR-PAY-002, BR-PAY-005

**Alternative Flows**:

**A1: Duplicate webhook (idempotency)**
- Tại bước 5, nếu payment already COMPLETED
- System returns success response to VNPay
- System does NOT update payment again
- System does NOT publish duplicate events

**A2: Invalid signature**
- Tại bước 2, signature verification fails
- System logs security alert
- System returns error to VNPay
- System does NOT update payment

---

### UC-PAY-004: Record Cash Payment (Staff)

**Actor**: Staff (at center, role = CASHIER or ADMIN)

**Preconditions**:
- Student comes to center to pay cash
- Invoice exists

**Trigger**: Student pays cash at center

**Luồng chính**:
1. Student hands cash to staff
2. Staff logs into system
3. Staff navigates to "Record Payment" page
4. Staff searches for student và selects invoice
5. System hiển thị invoice details:
   ```
   Invoice: INV-2026-00123
   Student: Nguyen Van A
   Balance Due: 10,000,000 VND

   Record Cash Payment:
   Amount Received: [________] VND
   Receipt Number: [________] (auto-generated, editable)
   Notes: [________] (optional)

   [Record Payment]
   ```
6. Staff nhập amount: 10,000,000 VND
7. Staff nhấn "Record Payment"
8. System validates (BR-PAY-003, BR-PAY-004):
   - Amount > 0 ✅
   - Amount <= balance_due ✅
   - Receipt number unique ✅
9. System creates Payment record:
   ```java
   Payment payment = Payment.builder()
     .invoiceId(invoice.getId())
     .studentId(student.getId())
     .amount(new BigDecimal("10000000"))
     .paymentMethod(PaymentMethod.CASH)
     .paymentType(PaymentType.INVOICE_PAYMENT)
     .status(PaymentStatus.COMPLETED) // Cash is immediately confirmed
     .receiptNumber(generateReceiptNumber()) // RCPT-2026-00456
     .receivedBy(staff.getId())
     .completedAt(LocalDateTime.now())
     .build();
   ```
10. System saves payment
11. System publishes PAYMENT_COMPLETED event
12. System prints payment receipt (2 copies: student + center)
13. Staff hands receipt to student

**Postconditions**:
- Payment recorded với status = COMPLETED
- Invoice updated
- Receipt printed
- Student has proof of payment

**Business Rules**: BR-PAY-002, BR-PAY-003, BR-PAY-004

---

### UC-PAY-005: Record Bank Transfer Payment (Staff)

**Actor**: Staff (role = CASHIER or ADMIN)

**Preconditions**:
- Student has transferred money via bank
- Staff has bank transaction screenshot/reference

**Trigger**: Staff verifies bank transfer in bank statement

**Luồng chính**:
1. Staff checks bank account statement
2. Staff sees incoming transfer:
   ```
   Date: 2026-01-28 14:30
   Amount: 10,000,000 VND
   From Account: 9876543210 (Nguyen Van A)
   Reference: KITECLASS INV-2026-00123 NGUYENVANA
   Transaction ID: FT26012834567890
   ```
3. Staff extracts invoice number từ reference: INV-2026-00123
4. Staff navigates to "Record Payment" page
5. Staff searches invoice by invoice_number
6. System hiển thị invoice details
7. Staff selects "Bank Transfer" payment method
8. Staff fills form:
   ```
   Payment Method: Bank Transfer
   Amount: 10,000,000 VND
   Bank Transaction ID: FT26012834567890
   Transfer Date: 2026-01-28 14:30
   Bank: Vietcombank
   From Account: 9876543210
   Screenshot: [Upload file]
   Notes: Verified in bank statement

   [Record Payment]
   ```
9. Staff uploads screenshot (optional)
10. Staff nhấn "Record Payment"
11. System validates (BR-PAY-003):
    - Bank transaction ID unique ✅
    - Amount matches ✅
12. System creates Payment record:
    ```java
    Payment payment = Payment.builder()
      .invoiceId(invoice.getId())
      .studentId(student.getId())
      .amount(new BigDecimal("10000000"))
      .paymentMethod(PaymentMethod.BANK_TRANSFER)
      .paymentType(PaymentType.INVOICE_PAYMENT)
      .status(PaymentStatus.COMPLETED)
      .bankTransactionId("FT26012834567890")
      .transferDate(LocalDateTime.parse("2026-01-28T14:30:00"))
      .proofAttachmentUrl(uploadedFileUrl)
      .receivedBy(staff.getId())
      .completedAt(LocalDateTime.now())
      .build();
    ```
13. System saves payment
14. System publishes PAYMENT_COMPLETED event
15. System gửi email confirmation cho student

**Postconditions**:
- Payment recorded
- Invoice updated
- Student receives confirmation email

**Business Rules**: BR-PAY-003, BR-PAY-004

---

### UC-PAY-006: Generate Payment Receipt (PDF)

**Actor**: System (automated) or Staff (manual)

**Preconditions**:
- Payment exists với status = COMPLETED

**Trigger**:
- Automatic: Immediately after payment completed
- Manual: Staff nhấn "Reprint Receipt"

**Luồng chính**:
1. System retrieves payment details:
   ```java
   Payment payment = paymentRepository.findById(paymentId);
   Invoice invoice = invoiceRepository.findById(payment.getInvoiceId());
   Student student = studentRepository.findById(payment.getStudentId());
   ```
2. System generates PDF receipt với template:
   ```
   ==========================================
   KITECLASS LANGUAGE CENTER
   PAYMENT RECEIPT
   ==========================================

   Receipt #: RCPT-2026-00456
   Date: 28/01/2026 14:45

   RECEIVED FROM:
   Student: Nguyen Van A
   Email: nguyenvana@email.com
   Phone: 0901234567

   PAYMENT DETAILS:
   -------------------------------------------
   Invoice #: INV-2026-00123
   Class: English A1 (Morning - Mon, Wed, Fri)
   Payment Method: VNPay
   Transaction ID: VNPAY-14123456

   Amount Paid: 10,000,000 VND
   (Ten Million Vietnamese Dong)

   Invoice Total: 10,000,000 VND
   Previous Payments: 0 VND
   This Payment: 10,000,000 VND
   Remaining Balance: 0 VND
   -------------------------------------------

   Payment Status: COMPLETED ✓
   Received By: Tran Thi B (Staff)
   Payment Date: 28/01/2026 14:30

   NOTES:
   Thank you for your payment!
   For questions, contact billing@kiteclass.com

   ==========================================
   Signature:                    Received By:


   _________________            _________________
   Student                      Staff
   ==========================================
   ```
3. System saves PDF to storage (S3):
   ```java
   String pdfPath = "receipts/2026/01/" + payment.getReceiptNumber() + ".pdf";
   s3Service.upload(pdfBytes, pdfPath);
   ```
4. System updates payment:
   ```java
   payment.setReceiptUrl(pdfPath);
   paymentRepository.save(payment);
   ```
5. System returns PDF (download or print)

**Postconditions**:
- PDF receipt generated và saved
- Student/Staff có thể download/print receipt

---

### UC-PAY-007: View Payment History (Student)

**Actor**: Student

**Preconditions**:
- Student has made at least 1 payment

**Trigger**: Student truy cập "Payment History" page

**Luồng chính**:
1. Student nhấn "Payment History" trong navigation
2. System retrieves payments:
   ```java
   List<Payment> payments = paymentRepository.findByStudentId(studentId);
   ```
3. System hiển thị payment list:
   ```
   Payment History

   Date       | Receipt #      | Invoice #      | Amount      | Method      | Status    | Actions
   2026-01-28 | RCPT-2026-0456 | INV-2026-00123 | 10,000,000  | VNPay       | COMPLETED | [Receipt]
   2025-12-15 | RCPT-2025-0789 | INV-2025-00098 | 8,000,000   | Cash        | COMPLETED | [Receipt]
   2025-11-20 | -              | INV-2025-00087 | 5,000,000   | Bank Transfer| FAILED   | [Retry]
   ```
4. Student có thể:
   - View receipt (nhấn "Receipt" → download PDF)
   - Retry failed payment (nhấn "Retry" → UC-PAY-001)
   - Filter by date range, status, payment method

**Postconditions**:
- Student sees complete payment history
- Student có thể download receipts

---

### UC-PAY-008: Retry Failed Payment (Student)

**Actor**: Student

**Preconditions**:
- Payment exists với status = FAILED
- Related invoice chưa paid

**Trigger**: Student nhấn "Retry" trong payment history

**Luồng chính**:
1. Student views failed payment details:
   ```
   Payment Details:
   Date: 2025-11-20 10:30
   Amount: 5,000,000 VND
   Method: VNPay
   Status: FAILED
   Reason: Insufficient funds

   [Retry Payment]
   ```
2. Student nhấn "Retry Payment"
3. System validates:
   - Invoice still has balance > 0 ✅
   - Payment not already retried successfully ✅
4. System redirects to payment flow (UC-PAY-001):
   - Pre-fills amount = failed payment amount
   - Student chọn payment method (có thể khác method cũ)
5. Student completes new payment attempt
6. System creates new Payment record (không update old one)
7. System links new payment to old payment:
   ```java
   newPayment.setRetryOfPaymentId(failedPayment.getId());
   ```

**Postconditions**:
- New payment created
- Old failed payment remains FAILED (for audit trail)
- Invoice updated nếu new payment successful

---

### UC-PAY-009: Reconcile Payments (Admin)

**Actor**: Admin (Finance team)

**Preconditions**:
- Bank statement available (CSV or manual)

**Trigger**: End of day reconciliation process

**Luồng chính**:
1. Admin exports today's payments:
   ```java
   LocalDate today = LocalDate.now();
   List<Payment> todayPayments = paymentRepository.findByCompletedAtBetween(
     today.atStartOfDay(),
     today.atTime(23, 59, 59)
   );
   ```
2. Admin downloads bank statement (CSV)
3. Admin uploads bank statement to reconciliation page
4. System parses CSV:
   ```csv
   Date,Time,Transaction ID,Amount,Reference,From Account
   2026-01-28,14:30,FT26012834567890,10000000,KITECLASS INV-2026-00123,9876543210
   2026-01-28,15:45,FT26012845678901,8000000,KITECLASS INV-2026-00124,1234567890
   ```
5. System matches payments với bank transactions (BR-PAY-007):
   ```java
   for (BankTransaction bankTxn : bankTransactions) {
     // Try to match by transaction_id first
     Payment payment = paymentRepository.findByBankTransactionId(bankTxn.getTransactionId());

     if (payment == null) {
       // Try to match by amount + date + reference
       String invoiceNumber = extractInvoiceNumber(bankTxn.getReference());
       payment = paymentRepository.findByAmountAndDateAndInvoiceNumber(
         bankTxn.getAmount(),
         bankTxn.getDate(),
         invoiceNumber
       );
     }

     if (payment != null) {
       // Match found
       payment.setReconciled(true);
       payment.setReconciledAt(LocalDateTime.now());
       matchedPayments.add(payment);
     } else {
       // No match - flag for investigation
       unmatchedBankTransactions.add(bankTxn);
     }
   }

   // Check for payments in system but not in bank
   for (Payment payment : todayPayments) {
     if (!payment.isReconciled()) {
       unmatchedPayments.add(payment);
     }
   }
   ```
6. System generates reconciliation report:
   ```
   DAILY RECONCILIATION REPORT
   Date: 28/01/2026

   MATCHED PAYMENTS:
   - Total: 45 payments
   - Amount: 380,000,000 VND
   ✓ All matched successfully

   UNMATCHED - BANK TRANSACTIONS (không có payment record):
   - Transaction: FT26012856789012, Amount: 2,000,000, Reference: "Transfer"
     → Action: Create payment manually or ignore (not our transaction)

   UNMATCHED - PAYMENTS (không thấy trong bank):
   - Payment RCPT-2026-00470, Amount: 1,500,000, Method: Bank Transfer
     → Action: Contact student for proof or mark as failed

   [Download Report] [Approve Reconciliation]
   ```
7. Admin reviews unmatched items
8. Admin takes action:
   - Create missing payments
   - Mark questionable payments for follow-up
   - Update payment status if needed
9. Admin nhấn "Approve Reconciliation"
10. System marks reconciliation complete

**Postconditions**:
- All payments reconciled
- Unmatched items flagged for investigation
- Reconciliation report generated

**Business Rules**: BR-PAY-007

---

### UC-PAY-010: Process Refund Payment (Admin)

**Actor**: Admin (Finance team)

**Preconditions**:
- RefundRequest approved (from Invoice Module)
- Original payment exists và COMPLETED

**Trigger**: Admin approves refund request (UC-INV-011)

**Luồng chính**:
1. Admin approves RefundRequest trong Invoice Module
2. System retrieves original payment:
   ```java
   Invoice invoice = invoiceRepository.findById(refundRequest.getInvoiceId());
   Payment originalPayment = paymentRepository.findFirstByInvoiceIdAndStatusOrderByCompletedAtDesc(
     invoice.getId(),
     PaymentStatus.COMPLETED
   );
   ```
3. System determines refund method:
   ```java
   RefundMethod refundMethod;
   if (originalPayment.getPaymentMethod() == PaymentMethod.VNPAY ||
       originalPayment.getPaymentMethod() == PaymentMethod.MOMO) {
     refundMethod = RefundMethod.GATEWAY; // Refund via gateway
   } else {
     refundMethod = RefundMethod.BANK_TRANSFER; // Manual bank transfer
   }
   ```
4. System creates refund Payment record:
   ```java
   Payment refund = Payment.builder()
     .invoiceId(invoice.getId())
     .studentId(invoice.getStudentId())
     .amount(refundRequest.getRefundAmount().negate()) // Negative amount
     .paymentMethod(originalPayment.getPaymentMethod())
     .paymentType(PaymentType.REFUND)
     .status(PaymentStatus.PROCESSING)
     .referencePaymentId(originalPayment.getId())
     .refundRequestId(refundRequest.getId())
     .build();
   ```
5. Nếu GATEWAY refund:
   - System calls gateway refund API:
     ```java
     VNPayRefundRequest request = VNPayRefundRequest.builder()
       .transactionId(originalPayment.getGatewayTransactionId())
       .amount(refundAmount)
       .reason("Customer refund request")
       .build();

     VNPayRefundResponse response = vnpayService.refund(request);

     if (response.isSuccess()) {
       refund.setStatus(PaymentStatus.COMPLETED);
       refund.setGatewayTransactionId(response.getRefundTransactionId());
     } else {
       refund.setStatus(PaymentStatus.FAILED);
       refund.setFailureReason(response.getMessage());
     }
     ```
6. Nếu BANK_TRANSFER refund:
   - System creates payout task:
     ```java
     PayoutTask task = PayoutTask.builder()
       .paymentId(refund.getId())
       .recipientName(student.getName())
       .bankAccount(refundRequest.getBankAccount())
       .amount(refundAmount)
       .status(PayoutStatus.PENDING)
       .build();
     ```
   - Finance team manually transfers money
   - Finance team marks payout complete
   - System updates refund status to COMPLETED
7. System publishes PAYMENT_COMPLETED event (for refund)
8. System gửi refund confirmation cho student

**Postconditions**:
- Refund payment created
- For gateway: refund processed automatically
- For bank transfer: payout task created for Finance team
- Student nhận confirmation

**Business Rules**: BR-PAY-008

---

### UC-PAY-011: View Payment Analytics (Admin)

**Actor**: Admin

**Preconditions**: None

**Trigger**: Admin truy cập Analytics Dashboard

**Luồng chính**:
1. Admin mở "Payment Analytics" page
2. Admin chọn date range (default: last 30 days)
3. System calculates metrics:
   ```java
   PaymentAnalytics {
     total_payments_count: 342,
     total_payments_amount: 2_850_000_000 VND,
     successful_payments: 325 (95%),
     failed_payments: 17 (5%),

     by_method: {
       VNPAY: { count: 180, amount: 1_500_000_000 },
       MOMO: { count: 85, amount: 750_000_000 },
       BANK_TRANSFER: { count: 50, amount: 450_000_000 },
       CASH: { count: 10, amount: 150_000_000 }
     },

     average_payment_amount: 8_333_333 VND,
     largest_payment: 25_000_000 VND,

     daily_trend: [
       { date: "2026-01-01", count: 12, amount: 98_000_000 },
       { date: "2026-01-02", count: 15, amount: 125_000_000 },
       ...
     ]
   }
   ```
4. System renders charts:
   - Line chart: Daily payment volume
   - Pie chart: Payment methods distribution
   - Bar chart: Success vs Failed payments
   - Table: Top paying students

**Postconditions**:
- Admin có insights về payment trends
- Admin identify issues (high failure rate, etc.)

---

## Entity Design

### Payment Entity

**Purpose**: Ghi nhận tất cả payment transactions

**Database Schema**:
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_type VARCHAR(50) NOT NULL DEFAULT 'INVOICE_PAYMENT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Gateway payment fields
    transaction_id VARCHAR(100) UNIQUE NULL,
    gateway_transaction_id VARCHAR(100) NULL,
    payment_url TEXT NULL,
    gateway_response TEXT NULL,
    expires_at TIMESTAMP NULL,

    -- Cash/Bank transfer fields
    receipt_number VARCHAR(50) UNIQUE NULL,
    bank_transaction_id VARCHAR(100) UNIQUE NULL,
    transfer_date TIMESTAMP NULL,
    proof_attachment_url VARCHAR(500) NULL,

    -- Refund fields
    reference_payment_id BIGINT NULL,
    refund_request_id BIGINT NULL,

    -- Status tracking
    completed_at TIMESTAMP NULL,
    failed_at TIMESTAMP NULL,
    failure_reason TEXT NULL,

    -- Reconciliation
    reconciled BOOLEAN DEFAULT FALSE,
    reconciled_at TIMESTAMP NULL,

    -- Audit
    received_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_reference FOREIGN KEY (reference_payment_id)
        REFERENCES payments(id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'VNPAY', 'MOMO', 'ZALOPAY', 'CREDIT_CARD')),
    CONSTRAINT chk_payment_type CHECK (payment_type IN ('INVOICE_PAYMENT', 'REFUND', 'ADJUSTMENT')),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_payment_invoice ON payments(invoice_id);
CREATE INDEX idx_payment_student ON payments(student_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_method ON payments(payment_method);
CREATE INDEX idx_payment_transaction ON payments(transaction_id);
CREATE INDEX idx_payment_completed_at ON payments(completed_at);
```

---

### PayoutTask Entity (for refunds via bank transfer)

**Database Schema**:
```sql
CREATE TABLE payout_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    bank_account VARCHAR(50) NOT NULL,
    bank_name VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_by BIGINT NULL,
    processed_at TIMESTAMP NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payout_payment FOREIGN KEY (payment_id)
        REFERENCES payments(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payout_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_payout_payment ON payout_tasks(payment_id);
CREATE INDEX idx_payout_status ON payout_tasks(status);
```

---

## Integration Points

### 1. Invoice Module Integration
**Purpose**: Update invoice khi payment completed

**Event-Driven**:
```java
// Payment Module publishes
Event: PAYMENT_COMPLETED
Payload: { payment_id, invoice_id, amount }

// Invoice Module subscribes
@EventListener
void onPaymentCompleted(PaymentCompletedEvent event) {
    invoiceService.applyPayment(event.getInvoiceId(), event.getPaymentId(), event.getAmount());
}
```

---

### 2. Payment Gateway Integration
**APIs**:
- VNPay API: Payment initiation, refund, query transaction
- MoMo API: Payment initiation, refund
- ZaloPay API: Payment initiation, refund

**Webhooks**:
- VNPay: `POST /api/v1/payments/vnpay/callback`
- MoMo: `POST /api/v1/payments/momo/callback`
- ZaloPay: `POST /api/v1/payments/zalopay/callback`

---

## Events & Notifications

### Published Events

**1. PAYMENT_COMPLETED**
```json
{
  "event_type": "PAYMENT_COMPLETED",
  "data": {
    "payment_id": 789,
    "invoice_id": 123,
    "student_id": 1001,
    "amount": 10000000,
    "payment_method": "VNPAY",
    "completed_at": "2026-01-28T14:30:00Z"
  }
}
```

**2. PAYMENT_FAILED**
```json
{
  "event_type": "PAYMENT_FAILED",
  "data": {
    "payment_id": 790,
    "invoice_id": 124,
    "student_id": 1002,
    "amount": 5000000,
    "payment_method": "VNPAY",
    "failure_reason": "Insufficient funds"
  }
}
```

---

## Summary

Payment Module xử lý tất cả payment transactions với:
- ✅ Multiple payment methods (Cash, Bank Transfer, VNPay, MoMo, ZaloPay)
- ✅ Payment gateway integration với webhook handling
- ✅ Payment lifecycle tracking (PENDING → PROCESSING → COMPLETED/FAILED)
- ✅ Idempotent webhook processing (BR-PAY-001)
- ✅ Signature verification cho security (BR-PAY-005)
- ✅ Payment timeout auto-cancellation
- ✅ Payment reconciliation với bank statements
- ✅ Refund processing (gateway refund + manual bank transfer)
- ✅ Payment receipts (PDF generation)
- ✅ Payment analytics và reporting
- ✅ Failed payment retry mechanism

**Business Rules**: 8 rules covering uniqueness, lifecycle, method validation, amount validation, signature verification, timeouts, reconciliation, refunds

**Use Cases**: 11 use cases covering full payment lifecycle from initiation to reconciliation and analytics
