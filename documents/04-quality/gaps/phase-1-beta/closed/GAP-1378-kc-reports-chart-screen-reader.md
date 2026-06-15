# GAP-1378: KC reports MonthlyBarChart data-value không screen-reader-accessible — WCAG 1.1.1

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-14 (UI review full audit, AUDIT-2026-06-14-ui-review-full)
**Affects:** `kiteclass/kiteclass-frontend/src/components/reports/monthly-bar-chart.tsx`

## Problem

`MonthlyBarChart` (dùng trong /reports cho doanh thu + điểm danh 12 tháng) có:
- `<svg role="img" aria-label="Biểu đồ cột theo tháng">` — aria-label **generic**, không truyền data values.
- Per-bar `<title>{month}: {value}</title>` bên trong `<rect>` — nhưng khi parent `<svg>` có `role="img"` + `aria-label`, SVG trở thành atomic image → child `<title>` bị **mask**, screen reader chỉ đọc aria-label generic, KHÔNG đọc được giá trị từng tháng.

Hệ quả WCAG 1.1.1 Non-text Content (Level A): user screen-reader không tiếp cận được data thực (doanh thu/tỷ lệ điểm danh từng tháng) — chỉ biết "có một biểu đồ cột". Thiếu sr-only data-table fallback.

## Root Cause

Chart chỉ dựa `role="img"` + aria-label tĩnh; không cung cấp text alternative chứa data thực (data table hoặc aria-label động liệt kê điểm).

## Proposed Fix

Thêm sr-only `<table>` (hoặc `<dl>`) liệt kê month → value song song SVG (visual giữ nguyên), HOẶC build aria-label động liệt kê các điểm. Pattern khả dụng cho cả 2 chart (revenue + attendance).

## Acceptance Criteria

- [x] Screen reader đọc được giá trị từng tháng (sr-only `<table>` month→value, caption = label)
- [x] Visual chart không đổi (SVG render y nguyên, chỉ thêm `aria-hidden`)
- [x] Áp dụng cho cả revenue + attendance chart trên /reports (qua prop `label`)

## Resolution

**Fixed:** 2026-06-15 (branch `fix/audit-fixH-ui-2026-06-14`)

`kiteclass-frontend/src/components/reports/monthly-bar-chart.tsx`:
- Thêm sr-only `<table>` (caption = `label`, cột "Tháng" / "Giá trị") liệt kê month→`formatValue(value)` cho mọi điểm — text alternative thực (WCAG 1.1.1).
- SVG bỏ `role="img"` + `aria-label` generic; wrapper SVG + x-axis labels đánh `aria-hidden="true"` (decorative) → không còn mask child `<title>` + tránh SR đọc trùng.
- Thêm prop `label` (default "Biểu đồ cột theo tháng"); `reports/page.tsx` truyền `label="Doanh thu theo tháng"` + `label="Tỷ lệ điểm danh theo tháng"`.

**Test:** thêm `components/reports/__tests__/monthly-bar-chart.test.tsx` (table accessible name + per-month value cells + no `role="img"` + generic caption fallback); cập nhật `reports/__tests__/reports-page.test.tsx` (KPI/title dùng `getAllByText`; chart assert qua `getByRole('table', {name})` thay vì `role="img"`).

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, P2)
- Source: `kiteclass/kiteclass-frontend/src/components/reports/monthly-bar-chart.tsx:86-127`
- WCAG 1.1.1 Non-text Content (Level A)
