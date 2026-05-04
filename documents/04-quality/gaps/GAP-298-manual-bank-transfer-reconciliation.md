# GAP-298: Manual Bank-Transfer Reconciliation UI

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core payment module) + Frontend (owner billing screen)
**Found:** 2026-05-04 (Wave 17 Bucket B — P2 persona review)
**Affects:** P1, P2, P3 (anywhere parents pay via direct bank transfer to owner's personal account — VN norm at small scale)

---

## Problem

VN parents at P2 scale frequently pay tuition by direct bank transfer to owner's personal account (Vietcombank/MB/Techcombank), with reference text like "HoaiAnh thang5 toan9A". Owner sees bank notification → needs to mark matching invoice as paid. Currently:

- `Payment` entity supports multiple methods ✅
- **No UI** to manually reconcile a bank ref number with an outstanding invoice
- **No name-mismatch handling** (parent uses different name than registered student/parent)
- Auto-match against bank webhook is out-of-scope (P2 doesn't have bank API integration)

P2 review evidence: AC-FIN-003 FAIL — no controller endpoint for manual link, no UI screen.

## Root Cause

Payment domain assumed online gateway flow (MoMo/VNPay auto-confirm via webhook). Direct bank transfer is "outside the system" today; owner has no path to log it.

## Proposed Fix

1. New endpoint `POST /api/payments/manual-bank-transfer` accepting: invoiceId, amount, transfer date, bank ref number, optional payer name (when differs).
2. Service marks invoice as paid + creates Payment row with method=BANK_TRANSFER + reconciled=true + linked ref.
3. UI: on invoice detail → "Đối chiếu chuyển khoản" button → modal form (4 fields) → submit → invoice status updates + parent notified (GAP-063 when ready).
4. Bulk-search helper: filter unpaid invoices by parent name fuzzy match (handles "Nguyễn Thị B" vs "Nguyen Thi B" vs "Bich Hoai") → owner picks correct one.
5. Audit log on each manual reconciliation (who, when, ref, amount).

## Acceptance Criteria

- [ ] `POST /api/payments/manual-bank-transfer` endpoint with validation (amount > 0, invoice unpaid, ref unique per tenant)
- [ ] Fuzzy parent-name search endpoint `GET /api/invoices/search?name=...` returning invoice candidates
- [ ] UI modal: ≤4 fields, autocomplete parent name, mobile-friendly
- [ ] Idempotency: same ref number cannot reconcile twice (409 Conflict)
- [ ] Audit log per reconciliation
- [ ] Test: manual reconcile updates invoice status → emits PaymentCompleted event → commission recalculation triggered (when GAP-057 lands)

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-FIN-003
- Cluster: end-of-month closeout (with GAP-297 + GAP-299)
- Soft-depends on: [GAP-063](GAP-063-sms-zalo-notification-integration.md) for parent notification on reconciliation
