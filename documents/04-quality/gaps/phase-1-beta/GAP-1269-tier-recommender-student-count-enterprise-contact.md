# GAP-1269: Tier recommender theo số học viên + ENTERPRISE contact path

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` pricing / plan-selection page

## Problem

Persona audit (F9): trang chọn gói thiếu gợi ý tier phù hợp theo số học viên — owner phải tự đoán nên chọn gói nào. Tier ENTERPRISE 'custom' không có đường liên hệ → owner quan tâm ENTERPRISE không biết làm gì tiếp.

## Proposed Fix

Thêm tier recommender (nhập số học viên → gợi ý gói) + nút 'Liên hệ tư vấn' cho tier ENTERPRISE.

## Acceptance Criteria

- [ ] Pricing page gợi ý tier theo số học viên owner nhập
- [ ] Tier ENTERPRISE có nút/đường 'Liên hệ tư vấn'

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F9)
- Sister: GAP-1261 (downgrade over-cap), GAP-1262 (prorated)
