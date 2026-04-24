# Excel Formula Patterns — Vietnamese Financial & Academic Reports

Catalog of formula patterns that surface in VN K-12 / education SaaS reports. All use POI's `cell.setCellFormula(...)` (no `=` prefix in Java — POI adds it).

## Attendance & Roster

| Pattern | Formula | Notes |
|---------|---------|-------|
| Count Present | `COUNTIF(B3:H3,"P")` | Single row, day columns B–H |
| Count Absent | `COUNTIF(B3:H3,"A")` | |
| Count Late / Excused | `COUNTIF(B3:H3,"L")` / `COUNTIF(B3:H3,"E")` | |
| Attendance % | `IFERROR(I3/(I3+J3),0)` | Guard div-by-zero when student has no data |
| Class total Present (column) | `COUNTIF(B3:B30,"P")` | Column sum across students |
| Class avg attendance | `AVERAGE(K3:K30)` | Over per-student percentages |

## Grades

| Pattern | Formula | Notes |
|---------|---------|-------|
| Weighted average | `SUMPRODUCT(B2:D2,B$1:D$1)/SUM(B$1:D$1)` | Row 1 weights, row 2+ scores |
| Pass / Fail | `IF(G2>=5,"Đạt","Không đạt")` | VN threshold 5.0 |
| Classify (Giỏi / Khá / TB / Yếu) | `IF(G2>=8,"Giỏi",IF(G2>=6.5,"Khá",IF(G2>=5,"TB","Yếu")))` | Nested IF; acceptable for ≤4 buckets |
| Percentile rank | `PERCENTRANK($G$2:$G$30,G2)` | |

## Financial (VND)

| Pattern | Formula | Notes |
|---------|---------|-------|
| VAT amount | `B2*0.08` | 8% rate — parametrise via named range `VAT_RATE` when template matures |
| Subtotal | `SUM(B2:B10)` | |
| Grand total with VAT | `B11+B11*VAT_RATE` | Use named range; makes rate swap trivial |
| Running total | `SUM($B$2:B2)` | Column copy anchors top |
| Installment balance | `B2-SUMIF($C$2:C2,"Thu",$D$2:D2)` | Subtract cumulative collected amount |

## Cross-reference

| Pattern | Formula | Notes |
|---------|---------|-------|
| Lookup student name | `VLOOKUP(A2,Roster!A:B,2,FALSE)` | Exact match only in VN roster data |
| Sum if class matches | `SUMIF(Roster!C:C,"10A1",Roster!D:D)` | |
| Count distinct classes | `SUMPRODUCT(1/COUNTIF(C2:C30,C2:C30))` | Classic distinct-count idiom |

## Gotchas

- **POI formula string:** pass without leading `=`. Wrong: `"=SUM(A:A)"`. Right: `"SUM(A:A)"`.
- **Absolute vs relative refs:** `$A$1` in formulas is preserved when copying; use `$` explicitly.
- **Named ranges:** POI supports them via `wb.createName()` — prefer named ranges over magic numbers in templates.
- **Locale:** xlsx formulas are English regardless of Excel UI language. VN Excel users will see `SUM`, not `TỔNG`. Don't try to translate.
