# GAP-300: Batch monthly invoice generator + cash-payment printable PDF receipt + bank-transfer manual reconcile UX

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Feature-P0 — single biggest daily-pain blocker for P2 owners; without it, owner workflow is "60 invoices in spreadsheet")
**Domain:** Backend / Frontend
**Found:** 2026-05-04 (P2 Small Center persona review round 1)
**Persona blocked:** P2 Small Tutoring Center; also P3, P5
**Wave:** TBD (Cluster B candidate per review report §Wave-pack)

## Problem

Three financial ACs share the invoice/payment surface and one shipping fix; they should ship together:

1. **AC-FIN-001 (P2 owner):** No batch-monthly-invoice-generator. `InvoiceController` has per-invoice GET/POST + `mark-paid` + adjustments + late-fees + cancel — but NO endpoint to enumerate active students and generate 60 invoices in one run. grep `batch.*invoice|generateMonthly|monthlyInvoice|InvoiceBatch` in `kiteclass-core/.../module/invoice/` returns 0. Owner today must create one-by-one.
2. **AC-FIN-002 (P2 owner):** No PDF receipt generator. grep `receipt|pdf.*invoice` in invoice + payment modules returns mapper/event/repo only, no PDF renderer. `PaymentMethod.java` enum has cash, but a parent who paid 1M VND cash needs a printed receipt with center letterhead — unsupported today.
3. **AC-FIN-003 (P2 owner):** Manual bank-transfer reconcile flow not surfaced. Webhook controllers handle automated VNPay/MoMo/ZaloPay; but most VN tutoring-center parents do plain bank-transfer with their phone banking apps — owner needs a UX to find an unpaid invoice + paste bank-ref + mark paid. Today possible via `mark-paid` endpoint but no UX.

## Root Cause

Invoice/payment was built per-record-first, designed around the auto-payment-gateway happy path (vnpay/momo/zalopay webhooks). The "owner enters cash receipt for tuition collected last night" use case wasn't enumerated — it's central to small-center economics where 70%+ of payment is cash or plain bank transfer.

## Proposed Fix

| Sub-task | Surface | Estimate |
|---|---|---|
| `POST /api/v1/invoices/batch-generate` — enumerates active enrollments × class.monthlyTuition (depends on GAP-296 tuition column) → preview + commit | Backend | 1d |
| Batch dispatch hook → fan-out to GAP-063 notification (Zalo + email) | Backend | 0.5d |
| PDF receipt generator (Apache PDFBox / OpenPDF) — center letterhead from BrandingProvider, invoice + payment data | Backend | 1.5d |
| `GET /api/v1/invoices/{id}/receipt.pdf` returns PDF | Backend | 0.5d |
| Frontend "Mark cash paid" modal → enter amount + date + ref → POST `mark-paid` → PDF download | Frontend | 0.5d |
| Frontend "Reconcile bank transfer" search-unpaid-by-amount UX → match → mark paid + ref | Frontend | 1d |

## Acceptance Criteria

- [ ] Batch-generate run on a P2-shaped tenant (60 active enrollments × 1 class each) produces 60 invoice rows in <5 seconds + dispatches notifications
- [ ] Cash payment flow: open unpaid invoice → mark paid (cash) → download PDF receipt with center letterhead + amount + parent name + date + invoice ID
- [ ] Bank-transfer flow: enter `1500000` + parent name → see matching unpaid invoice list → click match → enter bank ref → invoice closes
- [ ] AC-FIN-001/002/003 (P2 owner) flip PASS in next P2 review
- [ ] Receipt PDF passes a 5-second visual check (logo present, no broken layout, VND currency formatting)

## Related

- Audit: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` §3 (top-2 critical finding)
- Dependencies: GAP-296 (class.monthlyTuition column required), GAP-063 (notification dispatch on batch generate)
- Existing infra: `InvoiceOverdueScheduler.java` (overdue flagging exists)
- Reference AC: `documents/00-brd/persona-criteria/P2-small-center.md` §3
