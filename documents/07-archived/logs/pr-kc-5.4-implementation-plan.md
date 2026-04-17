# IMPLEMENTATION PLAN: PR 5.4 - SUBSCRIPTION & BILLING MANAGEMENT

**Branch:** `feature/KC-5.4-billing`
**Base:** `main` (commit ff50a31 - PR 5.3 merged)
**Estimate:** 5-6 giờ
**Dependencies:** PR 5.3 (Customer Dashboard) ✅

## OVERVIEW

Implement complete billing & subscription management system cho KiteHub Frontend:
- Billing overview page với current plan display
- Subscription upgrade/downgrade flow (3 steps)
- Payment page với VietQR integration + real-time polling
- Payment history với filtering & DataTable

## IMPLEMENTATION PHASES

### Phase 1: Type Definitions & API Integration (45 min)
- Update subscription & payment types to match backend DTOs
- Create React Query hooks with polling logic
- Add pricing utilities & tier comparison functions

### Phase 2: Billing Overview Page (1 hr)
- Current plan card with auto-renew toggle
- Plan comparison table
- Upgrade/Downgrade buttons

### Phase 3: Upgrade/Downgrade Flow (1.5 hr)
- Multi-step wizard (Select tier → Confirm → Payment)
- Prorated charge calculation for upgrades
- Downgrade scheduled for end of cycle

### Phase 4: Payment Page with QR Code (1.5 hr)
- VietQR code display with countdown
- Auto-refresh payment status (polling 5s)
- Payment status transitions (PENDING → COMPLETED)

### Phase 5: Payment History (1 hr)
- DataTable with TanStack Table
- Filters: Status, date range
- Download invoice (placeholder)

### Phase 6: Navigation & Polish (30 min)
- Update sidebar navigation
- Add utility functions & formatters
- Error handling & edge cases

## SUCCESS CRITERIA

✅ Billing overview displays current subscription
✅ Upgrade flow creates payment and redirects
✅ Payment page auto-refreshes status every 5s
✅ Payment history table shows all payments with filters
✅ Responsive design on all screen sizes
✅ All API integrations working

**Estimated Total:** 5-6 hours
**Complexity:** Medium-High (real-time polling, multi-step flow)

---

_Created: 2026-03-15_
_Agent: Plan (ac6179c)_
