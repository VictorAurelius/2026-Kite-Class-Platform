# GAP-1378: KC reports MonthlyBarChart data-value không screen-reader-accessible — WCAG 1.1.1

**Status:** 🔵 OPEN
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

- [ ] Screen reader đọc được giá trị từng tháng (qua sr-only data-table hoặc aria-label động)
- [ ] Visual chart không đổi
- [ ] Áp dụng cho cả revenue + attendance chart trên /reports

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, P2)
- Source: `kiteclass/kiteclass-frontend/src/components/reports/monthly-bar-chart.tsx:86-127`
- WCAG 1.1.1 Non-text Content (Level A)
