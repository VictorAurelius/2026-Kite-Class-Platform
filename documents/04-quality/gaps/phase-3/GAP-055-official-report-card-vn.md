# GAP-055: Official Report Card (Bảng điểm VN format)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / PDF Generation
**Detected:** 2026-04-14 (persona review)
**Persona blocked:** P5 K-12 School

## Problem

Trường học VN cần **bảng điểm chính thức** theo format MOE:
- Logo trường + MOE header
- Tất cả môn học với điểm TX, điểm KT, điểm TBM, xếp loại
- Tổng kết học kỳ / cả năm
- Hạnh kiểm (conduct grade — GAP-059)
- Chữ ký: GVCN, Hiệu trưởng, Phụ huynh
- Format in A4

Hiện tại không có generator → trường phải export CSV → thủ công Word/Excel.

## Proposed Fix

PDF generation service (reuse GAP-047):
- Template: `templates/report-card/vn-k12-semester.pdf.ftl`
- Data: aggregate from GradeComponent, SubjectGrade (GAP-054)
- Branding: tenant school logo + MOE standard layout
- QR code với verification link

Variants:
- Semester report card (HK1, HK2)
- Annual report card (cuối năm)
- Transcript (bảng điểm tổng hợp nhiều năm)

## Acceptance Criteria

- [ ] PDF template MOE-compliant
- [ ] Generator service
- [ ] Digital signature support
- [ ] Batch generate for class (1-click 30 report cards)
- [ ] Print-ready A4

## Dependencies

- GAP-047 (document generation)
- GAP-054 (multi-subject grades)
- GAP-059 (conduct tracking)

## Log
- 2026-04-14 — Persona review identified
