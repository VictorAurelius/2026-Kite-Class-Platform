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
- [x] Empty-state payroll không còn tên method `PayrollService.calculate` — thay bằng copy user-facing "Kỳ lương sẽ xuất hiện sau khi chạy bảng lương — chức năng 'Chạy bảng lương' sẽ có ở Phase 2" (grep `PayrollService.calculate` trong src → clean).
- [ ] (Tùy chọn) date-filter render theo locale vi — defer (cosmetic i18n nit, không trong scope P3 này).

## Fix (Phase-3 coordinator inline, 2026-06-16)
- `kiteclass-frontend/.../admin/payroll/page.tsx:221-226` — bỏ `<code>PayrollService.calculate(...)</code>` leak, thay copy user-facing.
- Status PARTIAL: text-leak fixed; optional date-locale deferred; runtime confirm tại consolidated walk.

## Related
- Discovered in: Phase-2 browser walk (flow KC-12), 2026-06-16
- Fixed in: Wave flow-fix-1 Phase-3 (coordinator inline)
