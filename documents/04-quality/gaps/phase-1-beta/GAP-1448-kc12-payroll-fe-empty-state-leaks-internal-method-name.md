# GAP-1448: Payroll admin FE empty-state leak tên method backend nội bộ ra UI owner

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KC-12)
**Affects:** KC-12 — `kiteclass-frontend/src/app/(dashboard)/admin/payroll/page.tsx:223-225`

## Problem
Discovered Phase-2 browser walk KC-12. Empty-state payroll hiển thị "Kỳ lương được tạo bởi PayrollService.calculate(...)" — leak reference method backend nội bộ ra UI owner-facing. Minor: input date-filter render `mm/dd/yyyy` native giữa UI tiếng Việt (browser-locale dependent, i18n nit).

## Proposed Fix
Thay reference method nội bộ bằng copy user-facing (vd "Kỳ lương sẽ xuất hiện sau khi chạy bảng lương (Phase 2)"). Cân nhắc fix date-filter format theo locale vi.

## Acceptance Criteria
- [ ] Empty-state payroll không còn tên method `PayrollService.calculate`
- [ ] (Tùy chọn) date-filter render theo locale vi

## Related
- Discovered in: Phase-2 browser walk (flow KC-12), 2026-06-16
