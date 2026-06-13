# GAP-1268: Cancel wizard — bắt buộc export data + quảng bá undo 30 ngày

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` cancellation flow

## Problem

Persona audit (F8): luồng hủy gói không ép owner export dữ liệu trước khi hủy và không quảng bá cơ chế undo/khôi phục 30 ngày → owner mất dữ liệu không mong muốn, hoặc không biết có thể hoàn tác. Mở rộng GAP-1017 (cancel lifecycle).

## Proposed Fix

Thêm cancel wizard có bước 'Tải dữ liệu về' (export) + hiển thị rõ 'có thể hoàn tác trong 30 ngày' trước khi xác nhận hủy.

## Acceptance Criteria

- [ ] Cancel wizard có bước export data trước khi hủy
- [ ] Hiển thị thông điệp undo/khôi phục 30 ngày trong luồng hủy

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F8)
- Parent: GAP-1017 (cancel lifecycle)
- Sister: GAP-1263 (reactivate/win-back)
