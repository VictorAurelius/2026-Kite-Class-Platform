# GAP-295: Late-cancel policy + charge decision workflow (Full / Partial / Waive)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 — UX gap; AC-EDGE-002 FAIL
**Domain:** Backend (kiteclass-core/module/clazz + payment) + Frontend (session detail UI)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher (revenue recovery edge case); P2/P3 Center (policy automation)

## Problem

Theo AC-EDGE-002, khi student cancel last-minute (<2h trước class), system PHẢI cho phép teacher quyết định: charge full / charge partial / waive — ghi log để track pattern.

Hiện trạng: KHÔNG có late-cancel policy logic. KHÔNG có UI prompt với charge decision. Teacher phải manual chargeback ngoài system → revenue leak + quan hệ khách hàng risk.

**State-check (verified 2026-05-04):**
- Grep `lateCancel|late.?cancel|cancellation.?policy|chargeBack` ở `kiteclass-core` = 0 hits
- ClassSession entity không có cancel-by-student tracking
- Payment entity không có policy-driven adjustment field

## Root Cause

Edge-case scenario chưa modeled. Solo persona review Round 1 surfaced this as practical real-world friction.

## Proposed Fix

1. **Backend:**
   - Add `cancelled_by_student_at` + `cancelled_by_student_lead_time_minutes` fields trên Attendance/ClassSession
   - `LateCancelPolicy` entity per-tenant config: `policy_minutes` (default 120) + `default_charge` (FULL/PARTIAL/WAIVE)
   - `StudentCancellationService.markLateCancel(sessionId, studentId, reason, charge_decision)`:
     - Records audit log
     - Triggers invoice adjustment (Outbox event)
     - Tracks pattern (count of late cancels per student per 30d window)
2. **API:** `POST /api/v1/sessions/{id}/students/{studentId}/late-cancel`
3. **FE:**
   - Session detail page → student row → "Đánh dấu hủy gấp" button
   - Modal với radio: "Tính đủ học phí / Tính một nửa / Bỏ qua" + reason field
   - Show student's late-cancel count history (helpful context)
4. **Analytics:** dashboard "Pattern hủy gấp" (P2 Center owners)
5. **Business rule docs:** `documents/01-business/kiteclass/attendance/rules.md` BR-LATECANCEL-001..003

## Acceptance Criteria

- [ ] Late-cancel endpoint records timestamp + lead time + decision
- [ ] Audit log captures actor + reason
- [ ] Invoice adjustment event published (Outbox)
- [ ] Per-tenant policy config supported
- [ ] FE 3-click flow
- [ ] Student late-cancel count visible in profile
- [ ] Business rules documented per 5-attribute standard

## Related

- AC-EDGE-002 (P1 review 2026-05-04)
- GAP-294 (NO_SHOW status — paired attendance edge cases)
- GAP-292 (Per-session pricing — policy interplay với billing model)

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1.
