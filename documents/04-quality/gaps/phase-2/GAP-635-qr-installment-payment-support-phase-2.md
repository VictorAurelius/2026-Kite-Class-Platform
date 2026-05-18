# GAP-635: QR installment payment support — Phase 2 P3 medium-center 3-month installment scope

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-05-18 (outside-in audit `2026-05-18-phase-1-5-qr-payment-outside-in.md` §5.1 + failure-mode matrix agent)
**Affects:** P3 medium center (50-200 HS) với học phí cao 6-12M VND/HS; Phase 2 scope expansion; competitive parity vs MISA EMIS + DotB EMS installment support
**Phase:** phase-2 (P3 medium-center scope, post-Wave 36+)

## Problem

P3 medium center (50-200 HS, học phí ~6-12M VND/HS) thường offer "**trả góp 3 lần**" cho gia đình PH để giảm financial burden:

- Lần 1 đầu khóa (4M VND) — confirm enrollment
- Lần 2 giữa khóa ~tuần 6 (4M VND)
- Lần 3 cuối khóa ~tuần 12 (4M VND)

Phase 1.5 QR model = **single-transfer** (PH chuyển full 12M VND một lần) — KHÔNG support installment:

| Installment friction Phase 1.5 (single-transfer model) | Phase 2 installment support requirement |
|---|---|
| PH gửi 4M lần 1 → KiteHub không biết là installment hay underpayment (paired GAP-627 amount-mismatch detection) | Invoice schema chứa installment plan (3 lần × 4M) — system biết "đây là lần 1/3" |
| Owner mark-paid lần 1 = mark full invoice paid? Hay PARTIAL? | Per-installment QR (3 mã QR riêng) HOẶC unified QR với memo encoding installment number |
| PH có thể quên lần 2 (sau 6 tuần) | Late-payment reminder system per installment cycle |
| Owner phải tự track 1/3, 2/3, 3/3 manually | UI hiển thị progress "Đã thu 8M/12M (2/3 installments)" |
| Refund flow phức tạp (refund full 12M? hay chỉ paid portion?) | Refund flow per-installment (only refund paid portion) |
| Reconciliation cuối khóa khó audit | Installment audit trail = parent invoice + 3 child payment_records |

Competitor benchmark:
- **MISA EMIS** support installment qua eInvoice integration (paired MeInvoice từ GAP-634)
- **DotB EMS** support installment cho trung tâm vừa-lớn
- **Easy Edu / Mona eLMS** support installment

KHÔNG có installment support = **competitive disadvantage** khi P3 medium-center segment evaluate vs MISA/DotB.

## Root Cause

Phase 1.5 scope **đúng đắn** target P1 solo + P2 small-center (<50 HS) với single-transfer model. P3 medium-center scope = Phase 2 (per audit §5.2 + `release-1-plan-2026.md` §3 Phase progression). Installment support belongs Phase 2 P3 scope, không phải Phase 1.5.

Per `outside-in-coverage-trigger.md` §3 — Phase 2 P3 scope = user-facing critical (PH installment UX directly affects retention) → outside-in audit (persona walkthrough + benchmark + failure-mode) đã capture trong Wave 93 audit. Gap formalize follow-up Phase 2 scope.

## Proposed Fix

### Phase A: Domain modeling (Wave 36+ Phase 2 pre-launch)

1. **Invoice schema extension** — `invoice` table thêm `installment_plan_id` FK; new `installment_plan` table với columns:
   - `id`, `tenant_id`, `total_amount`, `installment_count`, `installment_dates[]`, `installment_amounts[]`
   - `created_at`, `updated_at`
2. **Payment_record schema** — track `installment_number` (1/3, 2/3, 3/3) + `paid_at` per record
3. **Business rules document** — `documents/01-business/payment/installment-rules.md`:
   - BR-INSTALLMENT-001: Installment count = 2-12 lần (config key `kitehub.payment.installment.max-count`)
   - BR-INSTALLMENT-002: Installment interval ≥7 ngày between dates
   - BR-INSTALLMENT-003: Total installment amounts MUST equal invoice total
   - BR-INSTALLMENT-004: Late-payment reminder D-3 + D-day + D+3 + D+7 escalation
   - BR-INSTALLMENT-005: Refund only refunds paid installments, not future scheduled

### Phase B: API + UI implementation

1. **API endpoints** — `POST /api/v1/invoices/{id}/installment-plan` (Owner setup), `GET /api/v1/invoices/{id}/installments` (status query)
2. **QR generation strategy** — evaluate options:
   - **Option 1**: Per-installment QR (3 QR codes generated cho 3 installments) — clearer for PH, more files
   - **Option 2**: Unified QR với memo encoding `"INV-XXX-1/3"` — single QR, deterministic mapping qua memo parsing
3. **Owner UI**: Setup installment plan trong invoice creation flow (UI mockup needed)
4. **PH UI**: Email + Zalo OA notification per installment cycle với QR + amount + due date
5. **Late-payment reminder system** — async job daily check installment due_date, send reminder per BR-INSTALLMENT-004
6. **Refund per-installment** — extend GAP-629 refund SOP với installment-aware refund (only refund paid portion)

### Phase C: Integration với partnership Phase 2

1. **MISA MeInvoice integration** (paired GAP-634) — eInvoice cancel/correction flow per-installment when refund triggered
2. **VietQR EduPay NAPAS** (paired GAP-633) — webhook reconcile per-installment payment

### Phase relevance

- **Phase 2 (post-Wave 36)** default — P3 medium-center scope launch
- **Dependency** — GAP-625 P0 (KYC), GAP-628 P1 (batch reconcile), GAP-629 P1 (refund SOP); installment is layer ON TOP of foundation
- **Not blocking Phase 1.5 launch** — P3 scope deferred per `release-1-plan-2026.md` §3

## Acceptance Criteria

- [ ] Invoice + installment_plan + payment_record schema designed (ERD ship trong `documents/02-architecture/data-model/payment-installment.md`)
- [ ] Business rules document filed (`documents/01-business/payment/installment-rules.md`) với BR-INSTALLMENT-001..005
- [ ] API contract designed (`documents/01-business/payment/api-contract.md` extended với installment endpoints)
- [ ] QR strategy decision (Option 1 vs Option 2) documented trong ADR `documents/02-architecture/adr/ADR-NNN-installment-qr-strategy.md`
- [ ] UI mockups (Owner setup + PH notification) reviewed
- [ ] Late-payment reminder system designed (async job + Resend email + Zalo OA when active)
- [ ] Refund flow per-installment integrated với GAP-629 SOP
- [ ] Partnership integration paths documented (NAPAS reconcile per-installment + MeInvoice cancel per-installment)
- [ ] Competitive parity verified (MISA EMIS / DotB EMS / Easy Edu installment UX benchmark)
- [ ] Status flip DONE only sau ADR ACCEPTED + Phase 2 wave plan owns implementation OR pivot cancel

## Related

- **Audit report** — `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md` §5.1 row "GAP-635" + §2.3 failure-mode matrix
- **Wave plan** — `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md` (paired same-PR Wave 93)
- **Foundation gaps Phase 1.5** — GAP-625 (KYC), GAP-627 (amount-mismatch detection — installment-aware), GAP-628 (batch reconcile — extends per-installment), GAP-629 (refund SOP — paired per-installment refund)
- **Sibling P2 gaps Phase 2** — GAP-633 (VietQR EduPay NAPAS partnership — webhook per-installment), GAP-634 (MISA MeInvoice partnership — eInvoice cancel per-installment)
- **Phase context** — `documents/03-planning/roadmap/release-1-plan-2026.md` §5 Phase 2 P3 medium-center scope
- **Industry sources** — MISA EMIS https://emis.misa.vn/bao-gia/ + DotB EMS https://dotb.vn/news/phan-mem-quan-ly-trung-tam-day-them/ + Easy Edu https://easyedu.vn/

## Log

- **2026-05-18:** Filed by Wave 93 audit team per outside-in audit §5.1 P2 trio. Triggered Phase 2 P3 medium-center installment scope gap để eliminate competitive disadvantage khi P3 segment evaluate vs MISA/DotB. Phase 1.5 single-transfer model intentional cho greenfield launch (P1 solo + P2 small-center scope); installment = Phase 2 P3 expansion. Investigation + design tasks defer Phase 2 execution (post-Wave 36+); rule ships gap NOW để track follow-up. Per `audit-to-gap-pipeline.md` §3 gap template + `gap-done-discipline.md` §1 — status OPEN per phase-2 phase classification. Per `meta-gap-priority.md` §3 Feature tier (installment UX = feature execution, không phải business-logic correctness gap — business rules clear, scope = build feature when Phase 2 hits trigger).
