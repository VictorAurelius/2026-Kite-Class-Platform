# GAP-627: Payment-amount mismatch detection — PH chuyển sai số tiền alert + UI workflow

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed
**Detected:** 2026-05-18
**Related PRs:** []
**Related Docs:** [`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md), [`documents/03-planning/roadmap/release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md) §4 Phase 1.5 PAID

## Current State (verified 2026-05-18)

> Per `.claude/rules/audit-to-gap-pipeline.md` §2.5 — state-check xác nhận payment-amount mismatch UI workflow scope là **greenfield**. Existing KiteHub subscription `PaymentController` chỉ handle exact-amount payment qua webhook (auto-reconcile), KHÔNG cover Owner manual mark-paid với actual-vs-expected delta detection.

| Piece | File / Path | Status |
|-------|-------------|--------|
| Owner UI "actual amount received" input field tại mark-paid step | Chưa build | ❌ missing |
| Mismatch detection logic (compare actual vs expected) | Chưa build | ❌ missing |
| Mismatch alert badge trên invoice | Chưa build | ❌ missing |
| Partial-payment status `PENDING_BALANCE` (tách rời PENDING / PAID / OVERDUE) | Chưa build (current status enum: PENDING / PAID / OVERDUE only) | ❌ missing |
| Templated Zalo/email notification cho PH về mismatch | Chưa build | ❌ missing |
| Overpayment refund workflow (manual SOP) | Chưa build (re-scope GAP-183 sẽ cover refund chung) | 🟡 partial — depends GAP-629 |

**Grep commands run:**

```bash
# Verify mismatch detection not yet implemented
grep -rl "amount_mismatch\|partialPayment\|pendingBalance\|actualAmount" \
  kitehub/kitehub-subscription/src kitehub/kitehub-platform/src \
  --include="*.java" 2>/dev/null
# Result: 0 files — greenfield

grep -rl "MismatchAlert\|PaymentMismatchBadge\|PartialPaymentBanner" \
  kitehub/kitehub-frontend/src --include="*.tsx" 2>/dev/null
# Result: 0 files — greenfield

# Verify current invoice status enum scope
grep -rl "PENDING.*PAID.*OVERDUE\|InvoiceStatus" \
  kitehub/kitehub-subscription/src --include="*.java" 2>/dev/null \
  | head -3
# Result: existing enum binary (PAID | NOT PAID); no PENDING_BALANCE state
```

## Problem

PH (parent) chuyển khoản học phí qua QR có thể nhập sai số tiền — typo (VND 1,450,000 thay vì 1,500,000), lazy round-down (VND 1,450,000 vì PH nghĩ "gần đủ"), partial payment intent (VND 750,000 trong tháng này, 750,000 tháng sau), hoặc overpayment (VND 1,550,000 vì PH muốn deposit). KiteHub UI hiện tại **KHÔNG có mechanism surface mismatch** — Owner phải tự check banking app + chat lại PH qua Zalo/SMS bằng tay.

**Failure mode nếu KHÔNG có gap này:**

- **Outside-in agent 1 (Persona walkthrough)** TOP 3 UX friction item 2: "No payment-amount mismatch detection — chị Hằng tháng nào cũng có 3-5 PH chuyển thiếu/dư; chị phải mở banking app xem từng giao dịch, chat từng PH qua Zalo, rất mất thời gian; sau 1 tháng 5+ trường hợp pending vì PH không reply"
- **Outside-in agent 3 (Failure-mode matrix)** scenario `P1-payment-amount-mismatch-untracked`: "PH chuyển VND 1,450,000 thay vì 1,500,000; Owner mark `PAID` để đóng case → invoice PAID nhưng thực tế thiếu VND 50,000; cuối kỳ kế toán phát hiện thiếu → conflict với PH 2 tháng sau, không có audit trail rõ"
- **Persona walkthrough finding** thầy Tâm (P1 solo teacher) scenario: "thầy có 15 HS, mỗi tháng 2-3 PH chuyển dư VND 100K-500K vì lý do gối đầu kỳ sau; thầy track bằng Google Sheets riêng, KHÔNG sync với KiteHub → kế toán cuối quý mất 4h reconcile thủ công"

**Why P0 (foundation block):** Mismatch detection là **core invariant của payment system** — nếu không track mismatch tại moment mark-paid, mọi P1+P2 follow-up (refund workflow, batch reconcile, evidence storage) đều build trên foundation lỗi. Mismatch state phải có ngay từ DB schema + UI để không retrofit sau.

## Context

Phase 1.5 PAID payment scope chốt 2026-05-18 via outside-in audit (3-agent convergence). Mismatch scope phân loại:

| Mismatch type | Likelihood (per persona audit) | Resolution path |
|---|---|---|
| **Underpay typo** (PH chuyển ít hơn invoice) | 30-40% PH per tháng theo P2 audit | Owner alert PH "chuyển bổ sung VND X"; invoice status `PENDING_BALANCE` |
| **Overpay deposit intent** (PH chuyển dư cho kỳ sau) | 10-15% P2 audit | Owner option "Áp dụng vào kỳ sau" (credit) OR "Hoàn trả" (refund SOP) |
| **Partial payment intent** (PH chia 2-3 lần) | 5-10% P2 audit | Owner accept partial + track remaining balance |
| **Wrong-amount mistake** (PH chuyển nhầm số tiền của HS khác) | 1-2% P2 audit | Owner refund + PH transfer lại |

Gap này là **3rd P0 trong cluster 3 foundation gaps** (GAP-625/626/627) **MUST close trước Phase 1.5 launch**. Sibling: GAP-625 (KYC + binding + audit log) + GAP-626 (PDPL consent + DSAR).

**Tách rời cluster = half-shipped:**

- GAP-625 audit log without mismatch tracking → audit trail incomplete (chỉ ghi "đã thu" không ghi "thu bao nhiêu thực tế vs expected")
- GAP-626 consent without mismatch → consent UI không cover "đồng ý xử lý mismatch" scope

## Evidence

- **Audit report:** [`2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md) §2.1 Persona TOP 3 UX friction (item 2) + §3 3-agent convergence "No idempotency = double-payment risk" related row
- **Persona walkthrough finding (agent transcript `a22e8469ba8bceef5`):** P2 Chị Hằng simulated workflow tháng 5: 30 HS × 1 invoice = 30 marks/tháng; 5/30 mismatch (3 underpay + 1 overpay + 1 partial); chị mất ~30 phút/tháng chat thủ công Zalo + check banking app + tự note Google Sheet
- **Failure-mode scenario (agent transcript `a2615874804195b90`):** Actor=Owner × Class=accounting-correctness × Phase=runtime — "VND 50,000 underpay × 12 HS × 12 tháng = VND 7,200,000/năm leak nếu Owner mark PAID without tracking; cuối năm kế toán phát hiện gap → tenant churn vì stressed; reverse 12 invoice 2 năm sau impossible vì PH đã quên"
- **Benchmark pitfall (agent transcript `a1ee5d6e141e07b42`):** "VnResource manual quản lý: có cột 'Số tiền thực thu' tách biệt 'Số tiền học phí' — Owner nhập 2 giá trị mỗi marking; KiteHub Phase 1.5 phải có scope tương đương minimum"

## Proposed Fix

### Sub-item (a) — Owner UI input "actual amount received" field

**Frontend (kitehub-frontend):**

- Extend mark-paid Modal (per GAP-625 sub-item c immutable audit log) với new field:

```text
Số tiền PH chuyển (thực tế): [____________] VND
   (số tiền KiteHub kỳ vọng: VND 1,500,000)

[ Tự động điền số kỳ vọng ] [ Nhập số khác ]
```

- Default auto-fill `expected_amount` (invoice total); Owner có thể override bằng cách click "Nhập số khác" → input field appears
- Validation: actual_amount > 0 + actual_amount ≤ expected_amount × 2 (sanity guard against typo Owner — VND 15,000,000 thay vì 1,500,000)
- Sau submit, mismatch detection logic trigger (sub-item b)

### Sub-item (b) — Mismatch detection logic + new invoice status

**Backend:**

- New enum value `InvoiceStatus.PENDING_BALANCE` thêm vào existing enum (PENDING | PAID | OVERDUE | **PENDING_BALANCE**)
- Logic mark-paid endpoint (extend GAP-625 endpoint):

```text
expected = invoice.amount_total
actual = request.actual_amount_received
delta = actual - expected

IF delta = 0:
   invoice.status = PAID
ELSE IF delta < 0:  // underpay
   invoice.status = PENDING_BALANCE
   invoice.outstanding_balance = ABS(delta)
   trigger_notification(PH, type=UNDERPAY, amount=ABS(delta))
ELSE IF delta > 0:  // overpay
   invoice.status = PAID  // invoice itself closed
   create_credit_note(tenant, ph, amount=delta, type=OVERPAY_CREDIT)
   trigger_notification(PH, type=OVERPAY, amount=delta, options=[apply_next_invoice, refund])
```

- New tables:
  - `invoice_outstanding_balance` (cho underpay tracking — `invoice_id` FK, `outstanding_amount`, `created_at`, `resolved_at` nullable)
  - `payment_credit_note` (cho overpay tracking — `credit_id` UUID, `ph_id`, `tenant_id`, `amount`, `created_at`, `applied_to_invoice_id` nullable, `refunded_at` nullable)
- Audit log entry (per GAP-625) includes both `expected_amount` + `actual_amount` + `delta` + `delta_type` (`EXACT | UNDERPAY | OVERPAY | PARTIAL`)

### Sub-item (c) — Mismatch alert badge + UI surface

**Frontend:**

- Invoice list view: badge "⚠️ Thiếu VND 50,000" (red) hoặc "💰 Dư VND 50,000 (credit)" (green) hiển thị on invoice card
- Invoice detail page: timeline component show payment history (multiple mark-paid events for partial payments) + current outstanding balance prominently
- Owner dashboard widget: count of `PENDING_BALANCE` invoices + total outstanding (forecast Q3 leakage)

### Sub-item (d) — Templated PH notification (Zalo/email)

**Backend:**

- New email template `payment_mismatch_underpay.template.vi.html`:

```text
Xin chào {{ph_name}},

Trung tâm {{tenant_name}} đã nhận VND {{actual_amount}} từ bạn cho hóa đơn {{invoice_id}}.

Số tiền hóa đơn: VND {{expected_amount}}
Số tiền đã chuyển: VND {{actual_amount}}
Còn thiếu: VND {{outstanding_balance}}

Vui lòng chuyển khoản bổ sung VND {{outstanding_balance}} qua QR sau đây:
[QR image]

Nội dung chuyển khoản: {{invoice_id}} BS  (BS = bổ sung)

Cảm ơn bạn.
KiteHub via {{tenant_name}}
```

- Tương tự template `payment_mismatch_overpay.template.vi.html` (notify PH about credit + offer apply next invoice OR refund)
- Tương tự template `payment_mismatch_partial.template.vi.html` (acknowledge partial + remind balance)
- Notification trigger via existing `kitehub-email` service (Resend integration); fallback Zalo OA Phase 2 (when active per CLAUDE.md note)
- PH can opt-out per `payment_notification_preference` table (per `pre-handoff-self-test-completeness.md` §2.11 i18n + opt-out)

### Sub-item (e) — Partial-payment status tracking

**Backend:**

- New table `invoice_payment_attempt` (columns: `attempt_id` UUID, `invoice_id` FK, `actual_amount_received`, `marked_paid_at`, `marked_by_owner_id`, `delta_type`, `notes`)
- Each mark-paid action inserts row (allows multiple partial payments toward same invoice)
- Invoice status transitions: PENDING → PENDING_BALANCE (after 1st partial) → PENDING_BALANCE (after 2nd partial if still incomplete) → PAID (when cumulative actual = expected)
- View `invoice_payment_summary` aggregates: `SUM(actual_amount_received) AS cumulative_paid, expected_amount, expected_amount - cumulative_paid AS remaining`

## Acceptance Criteria

- [ ] **Owner UI sub-item:** Mark-paid Modal ship với "Số tiền PH chuyển (thực tế)" field — default auto-fill expected, Owner can override; validation min 1 VND + max 2× expected; submitted value persists DB row trong `invoice_payment_attempt`
- [ ] **Mismatch detection sub-item:** `InvoiceStatus.PENDING_BALANCE` enum value added + Flyway migration applied production; mark-paid endpoint computes delta + branches per logic; underpay path inserts `invoice_outstanding_balance` row; overpay path inserts `payment_credit_note` row; EXACT path no extra row
- [ ] **Alert badge sub-item:** Invoice list view renders red/green badge correctly based on `outstanding_balance` / `credit_note` presence; Owner dashboard widget shows count + total outstanding accurate
- [ ] **PH notification sub-item:** 3 email templates ship + tested send via Resend (staging): underpay (with QR bổ sung), overpay (with options apply/refund), partial (acknowledge + remind); email sent within 5 phút of Owner mark-paid action; PH opt-out preference respected
- [ ] **Partial payment sub-item:** `invoice_payment_attempt` table tracks multiple payments per invoice; view `invoice_payment_summary` aggregates cumulative correctly; status transitions PENDING → PENDING_BALANCE → PAID work via test scenarios (2 partial payments summing to expected, edge case 3 partial)
- [ ] **End-to-end test scenario PASS** per `.claude/rules/pre-handoff-self-test-completeness.md` §2.6 Payment flow + §2.9 Background job (notification send):
  - (a) Owner mark-paid invoice VND 1,500,000 expected, input actual VND 1,450,000 → status `PENDING_BALANCE`, outstanding VND 50,000, email sent PH (verify Resend dashboard "delivered")
  - (b) PH chuyển bổ sung VND 50,000, Owner mark-paid 2nd attempt → status `PAID`, outstanding 0, audit log shows 2 attempts
  - (c) Owner mark-paid invoice VND 1,500,000 expected, input actual VND 1,550,000 → status `PAID` + credit note VND 50,000 created, email sent PH with options (apply/refund)
  - (d) PH choose "apply next invoice" → credit_note `applied_to_invoice_id` set; next invoice issued reduces by VND 50,000
  - (e) Owner dashboard widget reflects accurate counts across (a) + (c) scenarios
- [ ] **Reviewer manual:** DB query verifies `invoice_payment_attempt` row count = number of mark-paid actions; `invoice_outstanding_balance` row exists only for invoices with status `PENDING_BALANCE`; `payment_credit_note` row exists only for overpay scenarios; `SUM(actual) = expected` invariant holds for status `PAID`
- [ ] **Documentation:** 3-layer docs created — `documents/01-business/payment/amount-mismatch/{rules,use-cases,api-contract}.md`; rules.md cites BR-PAYMENT-MISMATCH-001 through BR-PAYMENT-MISMATCH-006; api-contract.md spec endpoints + error codes

## Related

- **Audit origin:** [`2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md)
- **Paired wave plan:** `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md` (paired same-PR by coordinator)
- **Sibling P0 gaps (ship together as foundation block):** GAP-625 (KYC + binding + audit log foundation — required dependency for mark-paid endpoint extension), GAP-626 (PDPL consent — required for PH notification consent verify)
- **P1 follow-up gaps:** GAP-628 (batch reconcile P2 — extends mismatch detection at scale), GAP-629 (refund SOP — handles overpay refund path), GAP-630 (evidence storage — screenshot capture at mark-paid moment), GAP-631 (KYC quarterly refresh), GAP-632 (mark-paid override approval — for Owner correct mistakes)
- **Re-scoped existing:** [`GAP-108`](GAP-108-payment-invoice-config-hardcoded.md) (payment-invoice config — extend with mismatch threshold config keys), [`GAP-183`](GAP-183-refund-dispute-resolution-policy.md) (refund — covers overpay refund flow), GAP-185 (VAT — re-scope MISA partnership), [`GAP-594`](GAP-594-refund-policy-30-day-money-back.md) (30-day money-back align)
- **Phase 1.5 plan:** [`release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md) §4
- **Rules:** [`outside-in-coverage-trigger.md`](../../../.claude/rules/outside-in-coverage-trigger.md), [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §2.5, [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.6 §2.9 §2.11, [`release-deploy-standard.md`](../../../.claude/rules/release-deploy-standard.md) §3.1
- **Business invariant ref:** Accounting double-entry principle — every payment event must record `expected` + `actual` + `delta`; immutable audit log per GAP-625 sub-item c

## Log

- **2026-05-18** — Initial write-up. Filed via Wave 93 outside-in audit (3-agent convergence per `outside-in-coverage-trigger.md`). State-check confirms greenfield scope — existing subscription `PaymentController` covers webhook auto-reconcile only, không cover Owner manual mark-paid with actual-vs-expected delta. Priority P0 — blocking Phase 1.5 PAID launch trigger because mismatch state must exist in DB schema + UI from day 1 (retrofit later = data migration nightmare for invoices already marked PAID without delta tracking). Cluster member GAP-625 + GAP-626 + GAP-627 foundation block atomicity rationale: GAP-625 audit log empty without GAP-627 delta tracking + GAP-626 consent empty without GAP-627 mismatch event coverage.
