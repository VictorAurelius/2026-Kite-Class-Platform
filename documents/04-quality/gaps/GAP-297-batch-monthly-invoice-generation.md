# GAP-297: Batch Monthly Invoice Generation UX + Auto-Send

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (kiteclass-core invoice module) + Frontend (owner end-of-month screen)
**Found:** 2026-05-04 (Wave 17 Bucket B — P2 persona review)
**Affects:** P1 Solo (15 students), P2 Small Center (60), P3 Medium (300), P5 K-12 (1200) — every multi-student tenant

---

## Problem

End of month, P2 owner needs to generate ~60 invoices (one per active student × class tuition) trong ≤5 phút, send to parents (Zalo + email), track delivery. Currently:

- Invoice domain model exists (`InvoiceRepository`, `Payment` entity, `InstallmentPlanServiceImpl`) ✅
- **No batch endpoint** thấy được cho "generate invoices for all active students this month"
- **No auto-dispatch** to Zalo/SMS (depends on GAP-063) or email
- No "missing invoice" guard (e.g., student joined mid-month — should they be invoiced?)

P2 review evidence: AC-FIN-001 PARTIAL — infrastructure ✅ but no batch UX.

## Root Cause

Invoice module focused on per-invoice CRUD; batch month-end workflow not designed. Likely scope-cut from Wave 1 MVP.

## Proposed Fix

1. New endpoint `POST /api/invoices/batch-generate?month=YYYY-MM&tenantId=...` → enumerates active enrollments × class tuition × pro-rata if mid-month → returns preview list (count + total revenue).
2. Confirm endpoint `POST /api/invoices/batch-confirm` → persists invoices + enqueues notification jobs.
3. UI: "Tạo hóa đơn tháng" button on owner dashboard → preview drawer → Confirm → toast "60 hóa đơn đã tạo, đang gửi cha mẹ".
4. Hook into Zalo/SMS dispatch (GAP-063) when ready; fallback to email + in-app inbox until then.
5. Audit log per batch operation (idempotent: re-run same month doesn't double-create).

## Acceptance Criteria

- [ ] `POST /api/invoices/batch-generate` endpoint with preview semantics (no persistence)
- [ ] `POST /api/invoices/batch-confirm` persists + emits InvoiceCreated events to outbox
- [ ] Idempotency: batch-confirm twice for same month does NOT duplicate invoices (unique constraint or merge)
- [ ] Pro-rata for mid-month enrollment (cross-link [GAP-300](GAP-300-mid-term-class-transfer-prorate.md))
- [ ] UI on owner dashboard: ≤3 clicks total; mobile-friendly; preview before commit
- [ ] Notification dispatch: integrate with email + GAP-063 (Zalo/SMS) when available
- [ ] Performance: 60 invoices in ≤5 sec batch-generate; 300 (P3) in ≤15 sec
- [ ] Test: integration test seeding 60 enrollments → batch-generate → assert 60 invoice rows + 60 events

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-FIN-001
- Depends on: [GAP-063](GAP-063-sms-zalo-notification-integration.md) for notification dispatch
- Depends on: [GAP-300](GAP-300-mid-term-class-transfer-prorate.md) for prorate edge case
- Cluster: end-of-month closeout (with GAP-298 + GAP-299) — recommend bundling into wave "P2 GA Closeout"
