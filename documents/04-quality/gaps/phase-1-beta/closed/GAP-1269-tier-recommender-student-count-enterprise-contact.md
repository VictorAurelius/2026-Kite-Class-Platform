# GAP-1269: Tier recommender theo số học viên + ENTERPRISE contact path

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` pricing / plan-selection page

## Problem

Persona audit (F9): trang chọn gói thiếu gợi ý tier phù hợp theo số học viên — owner phải tự đoán nên chọn gói nào. Tier ENTERPRISE 'custom' không có đường liên hệ → owner quan tâm ENTERPRISE không biết làm gì tiếp.

## Proposed Fix

Thêm tier recommender (nhập số học viên → gợi ý gói) + nút 'Liên hệ tư vấn' cho tier ENTERPRISE.

## Acceptance Criteria

- [x] Pricing page gợi ý tier theo số học viên owner nhập
- [x] Tier ENTERPRISE có nút/đường 'Liên hệ tư vấn'

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F9)
- Sister: GAP-1261 (downgrade over-cap), GAP-1262 (prorated)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. FE-1 — tier recommender theo student-count + ENTERPRISE contact path.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
