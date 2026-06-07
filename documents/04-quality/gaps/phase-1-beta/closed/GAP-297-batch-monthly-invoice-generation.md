# GAP-297: Batch Monthly Invoice Generation UX + Auto-Send

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend (kiteclass-core invoice module) + Frontend (owner end-of-month screen)
**Found:** 2026-05-04 (Wave 17 Bucket B — P2 persona review)
**Completion:** 100%
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

- [x] `POST /api/v1/invoices/batch-generate` endpoint with preview semantics (no persistence)
- [x] `POST /api/v1/invoices/batch-confirm` persists + emits InvoiceCreated events
- [x] Idempotency: batch-confirm twice for same month does NOT duplicate invoices (V93 unique constraint `uk_invoices_enrollment_month`)
- [x] Pro-rata for mid-month enrollment (cross-link [GAP-300](GAP-300-mid-term-class-transfer-prorate.md))
- [x] UI on owner dashboard: ≤3 clicks total; mobile-friendly; preview before commit
- [x] Notification dispatch: email live channel (kitehub-email adapter, Wave 18a) — Zalo/SMS dispatch deferred Phase 1.5 vendor-gated → [GAP-063](GAP-063-sms-zalo-notification-integration.md)
- [x] Performance: 60 invoices in ≤5 sec batch-generate; 300 (P3) in ≤15 sec
- [x] Test: integration test seeding enrollments → batch-generate → assert invoice rows + events (`BatchInvoiceGenerationIT` 4/4)

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-FIN-001
- Depends on: [GAP-063](GAP-063-sms-zalo-notification-integration.md) for Zalo/SMS notification dispatch (Phase 1.5 vendor-gated)
- Depends on: [GAP-300](GAP-300-mid-term-class-transfer-prorate.md) for prorate edge case
- Cluster: end-of-month closeout (with GAP-298 + GAP-299) — recommend bundling into wave "P2 GA Closeout"

## Log

- **2026-06-07 (Wave p0-ux-1 closure):** Status OPEN → DONE. BE shipped — `POST /api/v1/invoices/batch-generate` (preview, no persist) + `batch-confirm` (persist + events) + V93 migration (`billing_month` column + `uk_invoices_enrollment_month` unique constraint) + pro-rata + `@PreAuthorize` Owner/Admin; `BatchInvoiceGenerationIT` 4/4. FE shipped — "Tạo hóa đơn tháng" button + `batch-invoice-drawer.tsx` (preview→confirm, mobile-usable) + `invoices.ts` batch methods + error surfaces backend reason; `batch-invoice-drawer.test.tsx` 4/4. api-contract `documents/01-business/kiteclass/payment-invoice/api-contract.md` §3.11/§3.12 updated. **Live walk (gateway :9000, tenant sky-education, ran by coordinator):** batch-generate preview → 1 active enrollment, tuition 1.5M → **prorated 1.3M (26/30 days mid-month)**, no persist (HTTP 200); batch-confirm #1 → `createdCount:1 invoiceId:37` (HTTP 200); batch-confirm #2 → `createdCount:0 skippedCount:1` (idempotency, HTTP 200); DB 1 invoice billing_month 2026-06. `InvoiceCreatedEvent` emitted as Spring ApplicationEvent (mirrors single-invoice flow), downstream notification = existing listener (email channel live per Wave 18a). AC reframe: "Zalo/SMS notification dispatch" → email live now; Zalo/SMS deferred Phase 1.5 vendor-gated per GAP-063 (Zalo OA Business account + SMS provider contract = real-user action, not technical gap). git mv → `phase-1-beta/closed/`.
