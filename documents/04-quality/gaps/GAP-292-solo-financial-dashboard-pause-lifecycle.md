# GAP-292: Solo financial dashboard + PAUSED instance lifecycle

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend (kiteclass-frontend/billing) + Backend (kitehub-platform InstanceStatus) + Subscription billing
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** P1 Solo Teacher (vacation/maternity pause + financial visibility), P2 Center (similar pause needs)

## Problem

**Two related sub-issues bundled** because both touch solo lifecycle + finance:

### 5.1 No monthly income summary view
P1 AC-FIN-003: "Teacher có thể xem monthly income summary (tổng thu / tổng outstanding / tổng chi nếu có) trong 1 trang."
- Billing list page exists (`kiteclass-frontend/src/app/(dashboard)/billing/page.tsx` 4.9K)
- Billing detail page exists (`billing/[id]/page.tsx` 6.1K)
- **No** `summary`/`dashboard`/`stats` subdir; no monthly aggregate.

### 5.2 No PAUSED instance state
P1 AC-EXIT-002: "Teacher có thể 'pause' account (tạm dừng dạy 2-3 tháng nghỉ hè / nghỉ thai sản) với data preserved 30 ngày miễn phí."
- `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/domain/enums/InstanceStatus.java:10` has 6 states: PENDING / TRIAL / ACTIVE / SUSPENDED / DELETED / PURGED
- **No PAUSED state**. SUSPENDED comment: "subscription expired or payment failed" — billing-failure-driven, NOT user-initiated pause.

Solo teacher's natural rhythm (summer break, maternity leave) → forced to either pay through inactive months OR cancel + lose data. Drives churn at the natural pause moments.

## Root Cause

Both features omitted — financial dashboard out-of-scope v1 (focus was per-invoice flow); lifecycle states modeled after subscription failures, not user-driven pauses.

## Proposed Fix

### 5.1
1. Add `/billing/summary` route showing monthly: Thu (paid) / Outstanding / Net (paid - outstanding) for last 12 months as table + simple chart.
2. Reuse `useInvoices` data filtered by status; aggregate client-side OR add backend `GET /api/v1/billing/summary?from=&to=` endpoint.

### 5.2
1. Add `PAUSED` value to `InstanceStatus` enum + migration.
2. State machine transition: ACTIVE → PAUSED (user-initiated) → ACTIVE (resume within 30d, free) OR SUSPENDED (after 30d if no resume + no billing).
3. Subscription billing pauses while PAUSED (no charge, no usage tracking).
4. After 30d PAUSED → auto-transition to "Archived" (read-only export-only); after 36mo → DELETED per PDPL retention.
5. Frontend Settings page: "Pause account" button with confirmation modal (states the 30-day free preservation policy).
6. Notify students: when paused, automated message via Zalo/SMS (depends on GAP-063).

## Acceptance Criteria

### Sub 5.1
- [ ] `/billing/summary` route showing 12-month income table + chart
- [ ] AC-FIN-003 PASS

### Sub 5.2
- [ ] PAUSED state added to InstanceStatus enum + migration + state machine validation
- [ ] Pause button in Settings with confirmation
- [ ] Billing paused during PAUSED window
- [ ] 30-day auto-transition logic + scheduled job
- [ ] Student notification on pause (depends GAP-063)
- [ ] AC-EXIT-002 PASS
- [ ] Business rules added per `business-logic-review.md` §2 for "30-day free pause" + "36-month retention" thresholds

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §3 §6
- AC: AC-FIN-003, AC-EXIT-002
- Sibling: GAP-063 (notification on pause)
- Sibling: GAP-185 (billing terms — pause billing rules)
- Compliance: PDPL 2023 Art 23 (data retention thresholds)

## Log

- 2026-05-04 — Created from Wave 17 Bucket A. State-check: InstanceStatus enum 6 states confirmed at line 10-37; no PAUSED. Billing folder has list + detail only.
