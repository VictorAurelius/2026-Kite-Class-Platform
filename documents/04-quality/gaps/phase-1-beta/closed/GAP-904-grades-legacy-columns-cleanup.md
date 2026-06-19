# GAP-904: `grades` cleanup 8 cột legacy V1 sau khi entity V64 stable

**Status:** 🟢 DONE (wave-gap-audit-p1-1 2026-06-19 — substantive work shipped + CI-verified; residual cosmetic doc-ref/AC-checkbox only per verify pass)
**Priority:** 🟢 P3
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC attendance-grading)
**Affects:** `kiteclass-core` `grades` table

## Problem

V64 (entity align) giữ 8 cột legacy V1 nullable: `grade_type` (entity dùng cho UK V74), `title`, `score`, `max_score`, `weight`, `feedback`, `graded_date`, `graded_by`. Entity `Grade.java` chỉ map `grade_type` + bỏ qua phần còn lại.

CHECK lệch: `chk_grades_score` chỉ ràng `score <= max_score` (cột legacy), KHÔNG ràng `final_score` (cột entity dùng thực).

UK đảo 2 lần: V64 tạo UK 2 cột → V74 đảo sang UK 3 cột (thêm `grade_type`).

## Proposed Fix

Cleanup migration future DROP 8 cột legacy sau khi xác nhận zero usage (V64 comment đã ghi nhận). Add CHECK cho `final_score` thay vì `score` legacy.

## Acceptance Criteria

- [ ] Verify zero usage cột legacy
- [ ] Migration V## DROP 8 cột + update CHECK
- [ ] Reference cluster doc 03-attendance-grading §B

## Discovered in

`documents/02-architecture/database/kiteclass/03-attendance-grading.md` §B
