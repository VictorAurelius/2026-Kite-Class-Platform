# Payment & Invoice — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## Use Cases — Invoice

### UC-PAY-01: Auto-Generate Invoice on Enrollment

**Actor:** System
**Precondition:** ENROLLMENT_CREATED event received

**Steps:**
1. System: Receive enrollment event per BR-INV-001
2. System: Create invoice with items (TUITION from course.price)
3. System: Set due_date = enrolled_at + payment_term (default 7 days)
4. System: Set status = PENDING per BR-INV-002
5. System: Filter by instance_id per BR-INV-008

**Postcondition:** Invoice created with PENDING status

---

### UC-PAY-02: Add Invoice Adjustment

**Actor:** Admin
**Precondition:** Invoice exists, not CANCELLED/PAID

**Steps:**
1. FE: Display invoice detail with items
2. Admin: POST /{id}/adjustments — add discount, scholarship, or fee adjustment
3. System: Recalculate total_amount and balance_due
4. FE: Refresh invoice with updated total

**Postcondition:** Adjustment applied, balance updated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invalid adjustment amount | "Adjustment amount invalid" |
| 404 | Invoice not found | "Invoice not found" |

---

### UC-PAY-03: Apply Late Fees

**Actor:** System (daily cron) / Admin (manual)
**Precondition:** Invoice OVERDUE, past due_date

**Steps:**
1. System: Find overdue invoices, calculate late fee per BR-INV-004 (0.1%/day, max 10%)
2. System: Add late fee as InvoiceAdjustment(type=LATE_FEE)
3. System: Update invoice status PENDING -> OVERDUE per BR-INV-002

**Postcondition:** Late fee applied, invoice marked OVERDUE

---

### UC-PAY-04: Cancel Invoice

**Actor:** Admin
**Precondition:** Invoice not PAID

**Steps:**
1. Admin: PUT /{id}/cancel with cancellation reason
2. System: Transition status to CANCELLED per BR-INV-002
3. FE: Toast success, invoice marked cancelled

**Postcondition:** Invoice cancelled

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invoice already paid | "Cannot cancel a paid invoice" |
| 404 | Invoice not found | "Invoice not found" |

---

### UC-PAY-05: Mark Invoice as Paid (Cash/Bank)

**Actor:** Admin / Cashier
**Precondition:** Invoice PENDING or OVERDUE

**Steps:**
1. Admin: POST /{id}/mark-paid with payment reference
2. System: Allocate payment per BR-INV-005 (late fees first, then tuition)
3. System: Transition to PAID per BR-INV-002
4. FE: Toast success

**Postcondition:** Invoice fully paid

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Already paid | "Invoice is already paid" |

---

## Use Cases — Payment

### UC-PAY-06: Create Gateway Payment (VNPay/MoMo/ZaloPay)

**Actor:** Student
**Precondition:** Invoice PENDING or OVERDUE, amount valid per BR-PAY-004

**Steps:**
1. FE: Display payment method selection
2. Student: Choose VNPAY/MOMO/ZALOPAY, confirm amount
3. System: Validate amount > 0, amount <= balance_due, min 100,000 VND per BR-PAY-004
4. System: Create Payment(PENDING), generate payment_url per BR-PAY-003
5. FE: Redirect student to gateway
6. System: Start 15-min timeout per BR-PAY-006

**Postcondition:** Payment created, student redirected to gateway

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Amount below minimum | "Minimum payment amount is 100,000 VND" |
| 400 | Amount exceeds balance | "Payment amount exceeds invoice balance" |

---

### UC-PAY-07: Process Gateway Webhook

**Actor:** Payment Gateway (VNPay/MoMo/ZaloPay)
**Precondition:** Payment exists in PENDING/PROCESSING state

**Steps:**
1. Gateway: POST webhook callback with transaction result
2. System: Verify HMAC signature per BR-PAY-005
3. System: Check idempotency — duplicate transaction_id returns existing result per BR-PAY-001
4. System: Update payment PROCESSING -> COMPLETED or FAILED per BR-PAY-002
5. System: If COMPLETED, allocate to invoice per BR-INV-005, update invoice status

**Postcondition:** Payment finalized, invoice updated if successful

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invalid signature | "Webhook signature verification failed" |
| 404 | Payment not found | "Payment not found for transaction" |

---

### UC-PAY-08: Cancel Payment

**Actor:** Student / Admin
**Precondition:** Payment in PENDING state

**Steps:**
1. User: PUT /{id}/cancel
2. System: Validate payment is PENDING per BR-PAY-002
3. System: Transition to CANCELLED
4. FE: Toast success, return to invoice

**Postcondition:** Payment cancelled

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Not in PENDING state | "Only pending payments can be cancelled" |

---

## Use Cases — Installment Plan

### UC-PAY-09: Create Installment Plan

**Actor:** Student / Admin
**Precondition:** Invoice >= 5,000,000 VND per BR-INV-006

**Steps:**
1. FE: Display installment form (2-12 terms)
2. User: Select number of installments per BR-INV-003
3. System: Validate min 500,000 VND per term per BR-INV-003
4. System: Create plan with installment schedule, status = PENDING_APPROVAL; auto-cancelled if any installment overdue > 15 days (BR-INV-007)
5. FE: Toast success, await approval

**Postcondition:** Installment plan created pending approval

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invoice < 5M VND | "Invoice amount too low for installment plan" |
| 400 | Term < 500K VND | "Minimum installment amount is 500,000 VND" |
| 400 | Periods outside 2-12 | "Installment periods must be between 2 and 12" |

---

### UC-PAY-10: Approve / Reject Installment Plan

**Actor:** Admin
**Precondition:** Plan in PENDING_APPROVAL state

**Steps:**
1. Admin: Review plan, click Approve or Reject
2. System: If approved, activate installment schedule
3. System: If rejected, return to student with reason

**Postcondition:** Plan approved (active) or rejected

---

## Use Cases — Refund

### UC-PAY-11: Request and Process Refund

**Actor:** Student (request) / Admin (approve + process)
**Precondition:** Invoice PAID, refund amount <= paid_amount per BR-PAY-007

**Steps:**
1. Student: POST /refund-requests with reason and amount
2. Admin: Review, PUT /{id}/approve or /{id}/reject
3. Admin: POST /{id}/process — execute refund via original payment method
4. System: Create refund payment, update invoice status per BR-INV-002

**Postcondition:** Refund processed, invoice status updated to REFUNDED

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Amount exceeds paid | "Refund amount exceeds paid amount" |
| 400 | Already refunded | "Invoice already refunded" |
| 404 | Request not found | "Refund request not found" |
