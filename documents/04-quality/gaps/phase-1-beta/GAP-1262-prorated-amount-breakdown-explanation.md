# GAP-1262: Prorated amount breakdown giải thích cho owner (+ proration khi đổi billing-cycle)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` plan-change / billing UI

## Problem

Persona audit (F11) + benchmark audit (F11): rule SUB-10 tính prorated khi đổi gói nhưng KHÔNG giải thích con số cho owner — owner thấy một số tiền 'lạ' không hiểu vì sao. Ngoài ra cũng chưa cover proration khi đổi chu kỳ MONTHLY ↔ ANNUALLY.

## Proposed Fix

Hiển thị breakdown prorated (vd 'còn 12 ngày gói cũ → chỉ trả chênh 600.000đ'). Bổ sung proration cho đổi billing-cycle.

## Acceptance Criteria

- [ ] UI plan-change hiển thị breakdown prorated rõ ràng
- [ ] Đổi MONTHLY ↔ ANNUALLY tính + hiển thị prorated đúng

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F11)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F11)
- Sister: GAP-1261 (downgrade over-cap)
