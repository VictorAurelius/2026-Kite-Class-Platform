# Payment & Invoice — Business Rules

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## 1. Rules

### Invoice Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-INV-001 | Auto-generate on enrollment | ENROLLMENT_CREATED event -> create invoice with status PENDING |
| BR-INV-002 | Status lifecycle | DRAFT -> PENDING -> PAID/OVERDUE/CANCELLED; PAID -> REFUNDED |
| BR-INV-003 | Installment 2-12 periods | Sum of installments = total_amount, min 500,000 VND each |
| BR-INV-004 | Late fee 0.1%/day | `late_fee = base_amount * 0.1% * days_overdue`, capped at 10% |
| BR-INV-005 | Payment allocation priority | Late fees first, then tuition, then other adjustments |
| BR-INV-006 | Installment plan min amount | Only for invoices >= 5,000,000 VND |
| BR-INV-007 | Overdue installment cancels plan | 1 installment overdue > 15 days -> entire plan cancelled |
| BR-INV-008 | Multi-tenant isolation | All queries filtered by `instance_id` |

**Invoice statuses:** DRAFT, PENDING, PAID, OVERDUE, CANCELLED, REFUNDED

### Payment Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-PAY-001 | Idempotent webhook | Duplicate transaction_id returns existing result |
| BR-PAY-002 | Status lifecycle | PENDING -> PROCESSING -> COMPLETED/FAILED; FAILED -> PENDING (retry) |
| BR-PAY-003 | Method validation | CASH: receipt_number; BANK_TRANSFER: bank_transaction_id; Gateway: transaction_id |
| BR-PAY-004 | Amount validation | amount > 0, amount <= invoice.balance_due, min 100,000 VND |
| BR-PAY-005 | Webhook signature required | All gateway webhooks must verify HMAC signature |
| BR-PAY-006 | Gateway timeout 15 min | Auto-cancel PROCESSING payments after 15 minutes |
| BR-PAY-007 | Refund needs approval | Refund amount <= invoice.paid_amount, requires approved RefundRequest |

**Payment methods:** CASH, BANK_TRANSFER, VNPAY, MOMO, ZALOPAY

**Payment statuses:** PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED

---

## 2. Flow

### Invoice Generation Flow
1. Enrollment created -> system creates invoice
2. Invoice items: TUITION (course.price), optional MATERIALS, REGISTRATION_FEE
3. Apply discounts (scholarship, early bird) as InvoiceAdjustment
4. Set due_date = enrolled_at + payment_term (default 7 days)
5. Status = PENDING, student notified

### Payment Flow (Gateway)
1. Student selects payment method (VNPay/MoMo/ZaloPay)
2. System creates Payment(PENDING), generates payment_url
3. Student redirected to gateway
4. Gateway processes -> webhook callback
5. Verify webhook signature (BR-PAY-005)
6. Update payment: PROCESSING -> COMPLETED or FAILED
7. If COMPLETED: allocate to invoice, update invoice status
8. If no webhook in 15 min: auto-cancel (BR-PAY-006)

### Late Fee Cron (Daily)
1. Find invoices where status = PENDING and current_date > due_date
2. Update status to OVERDUE
3. Calculate late fee (BR-INV-004)
4. Add as InvoiceAdjustment(type=LATE_FEE)
5. Send payment reminder email

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| (Planned) Invoice created | invoice-created | Student email |
| (Planned) Payment confirmed | payment-confirmed | Student email |
| (Planned) Invoice overdue | payment-overdue | Student email |
| (Planned) 3 days before due | payment-reminder | Student email |

> Email templates not yet implemented.

---

## 4. Config

| Key | Default | Description |
|-----|---------|-------------|
| `invoice.payment-term-days` | `7` | Days to pay after enrollment |
| `invoice.late-fee-percent-per-day` | `0.1` | Late fee daily percentage |
| `invoice.late-fee-max-percent` | `10` | Maximum late fee cap |
| `invoice.installment.min-amount` | `500000` | Min amount per installment (VND) |
| `invoice.installment.min-invoice` | `5000000` | Min invoice for installment plan |
| `invoice.installment.max-periods` | `12` | Max installment periods |
| `invoice.installment.overdue-cancel-days` | `15` | Days before plan cancellation |
| `payment.gateway-timeout-minutes` | `15` | Gateway payment timeout |
| `payment.minimum-amount` | `100000` | Min payment amount (VND) |
| `payment.daily-limit.cash` | `50000000` | Cash daily limit (VND) |
| `payment.daily-limit.bank` | `500000000` | Bank transfer daily limit |
| `payment.daily-limit.gateway` | `200000000` | Gateway per-transaction limit |

### Database Indexes
- `idx_invoices_student_id` — Invoices per student
- `idx_invoices_class_id` — Invoices per class
- `idx_invoices_status` — Filter by status
- `idx_invoices_due_date` — Overdue detection
- `idx_payments_invoice_id` — Payments per invoice
- `idx_payments_transaction_id` — Gateway transaction lookup (unique)
- `idx_payments_status` — Filter by status
