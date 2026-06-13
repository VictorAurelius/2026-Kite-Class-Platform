# GAP-1268: Cancel wizard — bắt buộc export data + quảng bá undo 30 ngày

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` cancellation flow

## Problem

Persona audit (F8): luồng hủy gói không ép owner export dữ liệu trước khi hủy và không quảng bá cơ chế undo/khôi phục 30 ngày → owner mất dữ liệu không mong muốn, hoặc không biết có thể hoàn tác. Mở rộng GAP-1017 (cancel lifecycle).

## Proposed Fix

Thêm cancel wizard có bước 'Tải dữ liệu về' (export) + hiển thị rõ 'có thể hoàn tác trong 30 ngày' trước khi xác nhận hủy.

## Acceptance Criteria

- [x] Cancel wizard có bước export data trước khi hủy
- [x] Hiển thị thông điệp undo/khôi phục 30 ngày trong luồng hủy

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F8)
- Parent: GAP-1017 (cancel lifecycle)
- Sister: GAP-1263 (reactivate/win-back)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. FE-1 — cancel wizard bắt buộc export-data + quảng bá undo 30 ngày.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
