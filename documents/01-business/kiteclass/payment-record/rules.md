# Payment Record — Business Rules

**Domain:** KiteClass Core (`module.payment.record` — manual payment recording subdomain)
**Version:** 1.0
**Updated:** 2026-05-25
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/record/`
**ADR reference:** [`ADR-034-cookie-consent-vendor.md`](../../../02-architecture/adr/ADR-034-cookie-consent-vendor.md) (consent context — PDPL Art 11 audit trail; not pricing-specific)

---

## 1. Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-PAYMENT-RECORD-001 | 4-method enum `PaymentRecordMethod` | `CASH` / `BANK_TRANSFER` / `VIETQR` / `MOMO` cover VN edu manual payment market. Distinct từ gateway-oriented `PaymentMethod` (VNPAY/ZaloPay). |
| BR-PAYMENT-RECORD-002 | `amount > 0` mandatory | DB CHECK constraint V69 + `@DecimalMin("0.01")` trên `RecordPaymentRequest.amount`. Reject 0 hoặc negative → 400. |
| BR-PAYMENT-RECORD-003 | `method` mandatory | `@NotNull` trên `RecordPaymentRequest.method`. FE dropdown enforce 4 enum values. |
| BR-PAYMENT-RECORD-004 | Idempotency-Key header support | Optional `Idempotency-Key: <UUID v4>` header prevent double-submit từ FE retry loops. Service-layer dedupe theo (invoiceId, idempotencyKey) trong window 24h. |
| BR-PAYMENT-RECORD-005 | `note` max 500 chars | `@Size(max = 500)` trên `RecordPaymentRequest.note`. Match `payment_records.note` column length. |
| BR-PAYMENT-RECORD-006 | Permission: TEACHER/ADMIN/OWNER record | Per `@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")` trên `PaymentRecordController.recordPayment`. PARENT/STUDENT KHÔNG record (chỉ pay qua gateway path). |
| BR-PAYMENT-RECORD-007 | Multi-tenant isolation OWASP A01 | Service-layer enforce TenantContext + `invoice.instanceId` check; cross-tenant access → 404. |
| BR-PAYMENT-RECORD-008 | List scope: OWNER/ADMIN all; TEACHER own classes | `listPayments` filter dựa role: OWNER+ADMIN xem mọi invoice trong instance; TEACHER chỉ xem invoices liên kết classes mà teacher member. |
| BR-PAYMENT-RECORD-009 | `paidAt` default = now() | Optional field; nếu omit, service set `Instant.now()`. UTC stored; FE display Asia/Ho_Chi_Minh. |
| BR-PAYMENT-RECORD-010 | Audit trail append-only | `payment_records` rows append-only sau insert (PDPL Art 11 + Luật Quản lý Thuế 2019). `recordedBy` + `createdAt` immutable. Update/delete BANNED (correction = new row với note "Sửa GD #X"). |
| BR-PAYMENT-RECORD-011 | Distinct từ gateway `PaymentService` | Manual recording (offline-first cash/transfer/QR/MoMo) tách bạch với online gateway payment (VNPAY/ZaloPay). 2 services độc lập, 2 tables (`payment_records` vs `payments`). |

### BR-PAYMENT-RECORD-001: 4-method taxonomy (LOCKED Wave br-4 GAP-292b)

- **Value:** `PaymentRecordMethod` enum 4 values: `CASH`, `BANK_TRANSFER`, `VIETQR`, `MOMO`.
- **Rationale:** VN edu market research surveyed 15 trung tâm dạy thêm → 95%+ manual payment qua 4 channels:
  - CASH (60-70%, most common cho TT nhỏ; phụ huynh nộp tại trung tâm)
  - BANK_TRANSFER (20-30%, Vietcombank/Techcombank/MB/ACB chuyển khoản)
  - VIETQR (5-10%, bank-agnostic QR; growing 2024-2026)
  - MOMO (~5%, younger parents digital wallet)
  Adding more enums (vd ZALO_PAY non-gateway, MANUAL_OTHER) defer Phase 2+ khi persona surfaces need.
- **Source:** Wave br-4 Bucket C external benchmark (Misa Quản lý trung tâm, FastTrac) + VN bank API surveys.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-25). Stakeholder review queued via GAP-156.
- **Compliance check:** **Compliant** — Luật Quản lý Thuế 2019 Art 18 (form ghi nhận thanh toán); Nghị định 123/2020/NĐ-CP (e-invoice không bắt buộc cho TT dạy thêm cá nhân Phase 1 BETA — tracked GAP-185 MISA partnership cho Phase 1.5+); PDPL 2023 Art 11 (audit trail).
- **Review cadence:** Quarterly cho Phase 1 BETA, sau đó Annual. **Next review:** 2026-08-25. Event triggers: VN bank/wallet API change (ZaloPay manual recording demand), ≥3 tenant complaints "thiếu method".

### BR-PAYMENT-RECORD-004: Idempotency via UUID v4 header (LOCKED Wave br-4)

- **Value:** Optional header `Idempotency-Key: <UUID v4>` from FE; service dedupe (invoiceId, idempotencyKey) trong window 24h via `PaymentIdempotencyService`.
- **Rationale:** Cash recording manual = high risk double-submit (network retry / double-click button / mobile flaky network). UUID v4 generated FE-side trước submit; identical key trong 24h window → return cached response idem-200, KHÔNG persist duplicate row. Cost: 1 dedupe lookup per submit; benefit: zero duplicate amount risk.
- **Source:** Standard REST idempotency principle (Stripe/Square pattern); Wave br-4 Bucket C implementation.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-25).
- **Compliance check:** N/A — implementation invariant.
- **Review cadence:** Annual (stable). **Next review:** 2027-05-25. Event triggers: window 24h too tight/loose per analytics, persona surface duplicate.

### BR-PAYMENT-RECORD-010: Append-only audit trail (LOCKED PDPL + Tax)

- **Value:** `payment_records` rows append-only sau insert. Update/delete BANNED. Correction = new row với note explicit ("Sửa GD #X: lý do") + business logic offset cũ (negative amount linked).
- **Rationale:** PDPL 2023 Art 11 mandate audit trail tamper-proof cho payment data. Luật Quản lý Thuế 2019 Art 18 require financial records 10 năm retention. Append-only design prevent retroactive edits; corrections explicit + linked. Hiệu trưởng + kiểm toán có thể trust historical log.
- **Source:** PDPL 2023 Art 11; Luật Quản lý Thuế 2019 Art 18; Nghị định 123/2020/NĐ-CP.
- **Reviewer:** @nguyenvankiet (acting Legal scout + Tech Lead, solo-dev, 2026-05-25). Formal legal counsel review queued via GAP-156.
- **Compliance check:** **Compliant** — PDPL Art 11 + Luật Quản lý Thuế Art 18 + Nghị định 123/2020.
- **Review cadence:** Annual + event-driven trên PDPL/Tax law amendment. **Next review:** 2027-05-25 OR 30 days trong amendment.

---

## 2. Flow

### Record Manual Payment Flow

1. Teacher/Owner mở UI `(teacher)/teacher/invoices/[invoiceId]` → click "Ghi nhận thanh toán"
2. FE hiển thị `RecordPaymentDialog` modal:
   - Dropdown `method` (4 options Vietnamese display: "Tiền mặt" / "Chuyển khoản" / "VietQR" / "MoMo") — mandatory per BR-PAYMENT-RECORD-003
   - Input `amount` (BigDecimal, VND format `1.500.000đ`) — mandatory > 0 per BR-PAYMENT-RECORD-002
   - DateTimePicker `paidAt` (default now()) — optional per BR-PAYMENT-RECORD-009
   - Textarea `note` (max 500 chars) — optional per BR-PAYMENT-RECORD-005
3. Teacher điền form + click "Xác nhận"
4. FE generate UUID v4 → set header `Idempotency-Key: <uuid>` per BR-PAYMENT-RECORD-004
5. FE call `POST /api/v1/invoices/{invoiceId}/record-payment` với `RecordPaymentRequest` body
6. BE `PaymentRecordController.recordPayment` → `PaymentRecordServiceImpl.recordPayment`:
   - Validate permission per BR-PAYMENT-RECORD-006
   - Validate invoice exists + tenant match per BR-PAYMENT-RECORD-007
   - Idempotency lookup per BR-PAYMENT-RECORD-004; nếu cached → return cached response
   - Validate `amount > 0` per BR-PAYMENT-RECORD-002
   - Persist `PaymentRecord` entity (append-only per BR-PAYMENT-RECORD-010)
   - Update `Invoice.paidAmount` sum (separate `@Transactional` flow)
7. Response 201 + `ApiResponse<PaymentRecordResponse>`
8. FE toast "Đã ghi nhận thanh toán <amount>đ qua <method-display>"; refresh invoice detail page

### List Invoice Payments Flow

1. Teacher/Owner mở UI invoice detail → scroll "Lịch sử thanh toán"
2. FE call `GET /api/v1/invoices/{invoiceId}/payment-records`
3. BE return list of `PaymentRecordResponse` (scoped per BR-PAYMENT-RECORD-008)
4. FE render `PaymentRecordsTable` với columns:
   - Thời gian (`paidAt`, VN format `25/05/2026 14:30`)
   - Phương thức (`method` Vietnamese display)
   - Số tiền (`amount`, VND format `1.500.000đ`)
   - Người ghi nhận (`recordedBy` + denormalized name)
   - Ghi chú (`note` truncated 80 chars + "Xem thêm")

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| (Planned Phase 1.5+) Payment recorded confirmation | `payment-recorded-parent` | Parent email + Zalo OA backup |
| (Planned Phase 1.5+) Invoice fully paid | `invoice-paid-receipt` | Parent (e-receipt PDF attachment) |

Phase 1 BETA: KHÔNG send email (avoid spam during beta).

---

## 4. Config

| Key | Default | Description |
|-----|---------|-------------|
| `kiteclass.payment-record.idempotency.window-hours` | `24` | Window cho idempotency-key dedupe (per BR-PAYMENT-RECORD-004) |
| `kiteclass.payment-record.amount-max` | `1000000000` | Max VND per record (1 tỷ, safeguard) |
| `kiteclass.payment-record.note-max-length` | `500` | Max characters cho `note` |
| `kiteclass.payment-record.audit-immutable` | `true` | Reject update/delete operations (per BR-PAYMENT-RECORD-010) |

### Database Schema (V69 migration)

- `payment_records.id` BIGSERIAL PRIMARY KEY
- `payment_records.invoice_id` BIGINT NOT NULL REFERENCES invoices
- `payment_records.method` VARCHAR(32) NOT NULL CHECK (method IN ('CASH','BANK_TRANSFER','VIETQR','MOMO'))
- `payment_records.amount` NUMERIC(19, 2) NOT NULL CHECK (amount > 0)
- `payment_records.paid_at` TIMESTAMP NOT NULL
- `payment_records.note` VARCHAR(500)
- `payment_records.recorded_by` BIGINT NOT NULL REFERENCES users
- `payment_records.idempotency_key` VARCHAR(64) — for dedupe per BR-PAYMENT-RECORD-004
- `payment_records.created_at` TIMESTAMP DEFAULT now()
- `payment_records.instance_id` BIGINT NOT NULL (RLS multi-tenant)

### Database Indexes

- `idx_payment_records_invoice_id` — Lookup payments per invoice
- `idx_payment_records_idempotency_key` UNIQUE (instance_id, invoice_id, idempotency_key) — Dedupe
- `idx_payment_records_paid_at` — Audit query by date range
- `idx_payment_records_recorded_by` — Audit query by actor

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules trong file này derive từ: Wave br-4 Bucket C external benchmark (Misa/FastTrac), VN bank API surveys, PDPL 2023 + Luật Quản lý Thuế 2019 + Nghị định 123/2020/NĐ-CP regulatory mandates.
- **Rationale:** Rule values reflect VN trung tâm dạy thêm market dominant payment methods (CASH primary, BANK_TRANSFER + VIETQR growing). Append-only design balance UX (correction allowed via new row) vs compliance (audit trail tamper-proof per PDPL Art 11).
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead + Legal scout, solo-dev, 2026-05-25). Formal stakeholder + tax/legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3.
- **Compliance check:** **Compliant** — PDPL 2023 Art 11 (audit trail), Luật Quản lý Thuế 2019 Art 18 (financial record retention 10 năm), Nghị định 123/2020/NĐ-CP (e-invoice deferred Phase 1.5+ via GAP-185 MISA MeInvoice partnership).
- **Review cadence:** Annual + event-driven trên PDPL/Tax law amendment. **Next review:** 2027-05-25. Event triggers: VN payment regulation amendment, ≥5 tenant complaints về method coverage, MISA partnership unlock e-invoice automation.

## Log

- **2026-05-25** Initial 3-layer business docs filed per GAP-738 (Wave beta-readiness-8 Bucket B). Closes Wave br-4 Bucket C code-doc sync gap (PR #1783 GAP-292b ship code but skip 3-layer docs). Rules extracted từ `PaymentRecordController.java` + `PaymentRecordServiceImpl.java` + `PaymentRecordMethod.java` + `RecordPaymentRequest.java` + V69 migration.
