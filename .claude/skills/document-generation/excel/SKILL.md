---
name: document-generation-excel
description: "Use when user asks to generate an Excel / xlsx — attendance report, roster, financial breakdown, grade sheet, 'báo cáo điểm danh', 'xuất Excel'. Wave 5 ships weekly attendance report only; additional templates land in later waves per GAP-047 roadmap."
---

# Document Generation — Excel (XSSF)

Excel strategy for `com.kiteclass.core.module.document.DocumentGenerationService` (ADR-019 Facade + Strategy). Auto-wired via Spring's `List<Generator>` injection.

## When to use

- User needs a multi-column tabular report with totals / percentages / formulas
- Output should be editable after delivery (unlike PDF)
- Wave 5 supports `templateId = "attendance"` only

## Formula-first rule (MANDATORY)

Prefer `SUMIF` / `COUNTIF` / `SUM` / `IFERROR` formulas over pre-computed values. Users editing a cell (e.g. changing a P to A) MUST see totals + percentages auto-update. If a value can be derived, write a formula.

**Color convention:**

- **Blue input cells** — user-editable (attendance codes, headcounts, raw amounts)
- **Black formula cells** — computed values (COUNTIF, SUM)
- **Green cross-reference cells** — percentages / ratios / references across rows

## How it works

1. Caller hands `DocumentRequest(format=XLSX, templateId, tenantId, data)` to facade.
2. `XlsxGenerator` routes by templateId; `attendance` → `AttendanceReportBuilder`.
3. Builder creates `XSSFWorkbook`:
   - Row 0: merged title "Báo cáo điểm danh — Lớp X — Tuần YYYY-MM-DD"
   - Row 2: header row — Student | Thứ 2–7 | Có mặt | Vắng | Tỷ lệ
   - Rows 3..N: one per student; day cells are inputs, trailing columns are `COUNTIF` / `IFERROR` formulas
   - Row N+1: `Tổng cộng` summary with `SUM` + column `COUNTIF` formulas
   - Freeze pane at row 3
4. Returns `DocumentResponse(bytes, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, "attendance-{weekStart}.xlsx")`.

## Gotchas

- **VN weekday labels:** `Thứ 2` through `Thứ 7` (Monday–Saturday). Do NOT localise with `Locale.of("vi")` — hardcode these in the header row; POI renders label text literally, not via `DateFormatSymbols`.
- **Student name column width:** composite names (`Nguyễn Phạm Hồng Ánh Tuấn`) need ~40+ chars; set column width ≥ 8000 units (≈ 24 Excel chars).
- **Percentage format:** `0.00%` not `0%`. Attendance rate of 83.33% should not truncate to "83%".
- **No VBA / macros:** Wave 5 scope is plain xlsx. POI can write macros but we don't ship them.
- **Currency (future grade / billing templates):** VND format `#,##0\ "đ"` — note the `\ ` (backslash-space) escapes the literal space.

## Reference

- `reference/excel-formula-patterns.md` — VN financial / academic formula catalog

## Out of scope (Wave 5)

- Branding-package integration (Sub-PR 5.5 tints header row + injects logo image anchor).
- HTTP endpoints (Sub-PR 5.5 — download only, no preview).
- Additional templates beyond `attendance` (GAP-208, Wave 7).
- Excel macros / VBA preservation.
- Async queue for bulk generation (GAP-210).
