# GAP-293: Monthly income summary dashboard (Thu / Outstanding / Net)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 — UX gap; AC-FIN-003 FAIL + AC-FIN-004 PARTIAL
**Domain:** Backend (kiteclass-core/module/invoice analytics) + Frontend (billing dashboard)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher (financial overview); P2/P3 Center (revenue tracking)

## Problem

Theo AC-FIN-003, teacher PHẢI có thể xem monthly income summary (tổng thu / tổng outstanding / tổng chi nếu có) trong 1 trang. Theo AC-FIN-004, teacher PHẢI có dedicated "Outstanding" tab để track học sinh chưa đóng + reminder action.

Hiện trạng: `billing/page.tsx` chỉ là invoice list table với pagination + status filter. KHÔNG có summary view, KHÔNG có monthly aggregation, KHÔNG có outstanding tab.

**State-check (verified 2026-05-04):**
- `kiteclass-frontend/src/app/(dashboard)/billing/page.tsx` (4.9K) — invoice list + filter UI only
- `kiteclass-frontend/src/hooks/use-invoices.ts` — fetches invoice list, no aggregation
- Grep `incomeSummary|monthlyRevenue|outstanding.*payment|RevenueSummary` ở core + frontend = 0 hits
- Backend KHÔNG có `/api/v1/invoices/summary` endpoint

## Root Cause

Billing UI thiết kế phase 1 = invoice CRUD. Analytics deferred. Solo persona need thấy "tháng này thu được bao nhiêu" tại first-class concern.

## Proposed Fix

1. **Backend (kiteclass-core/module/invoice):**
   - `InvoiceSummaryService.monthlySummary(tenantId, year)`:
     - Returns `[{ month, totalCollected, totalOutstanding, totalRefunded, netRevenue }]`
   - Optional expense module (deferred — FREE tier không cần)
2. **API:** `GET /api/v1/invoices/summary?year=2026` + `GET /api/v1/invoices/outstanding`
3. **FE:**
   - `billing/page.tsx` thêm summary card row trên top: "Tháng này: 12.5M đã thu / 3.2M chưa thu"
   - New tab `billing/outstanding/page.tsx` — list students chưa đóng + amount + due date + "Send reminder" button
   - Chart: 12-month bar chart (recharts hoặc tương tự)
4. **Reminder button:** open Zalo deep-link prefilled message (depend GAP-063 cho automated sending)

## Acceptance Criteria

- [ ] Summary endpoint returns correct aggregations (verify với fixture invoice data)
- [ ] FE shows current month summary on top
- [ ] Outstanding tab lists unpaid invoices with reminder action
- [ ] 12-month chart renders correctly
- [ ] Mobile responsive (summary stacks vertically)
- [ ] Tenant-scoped (no cross-tenant data leak)
- [ ] Performance: summary query <200ms p95 với 1000 invoices

## Related

- AC-FIN-003 + AC-FIN-004 (P1 review 2026-05-04)
- GAP-063 (Zalo reminder integration)
- GAP-292 (Per-session pricing — affects how revenue is calculated)

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1.
