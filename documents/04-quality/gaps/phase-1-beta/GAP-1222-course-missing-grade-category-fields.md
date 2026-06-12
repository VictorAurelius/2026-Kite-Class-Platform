# GAP-1222: Course thiếu field grade/category chuẩn → catalog filter "Cấp lớp" + persona-reco phải heuristic client-side

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-11 (GAP-274 port — catalog filter + reco)
**Affects:** `kiteclass-core` Course entity/search + `(public)/catalog`

## Problem

Course chỉ có `level` + `category` free-text — không có gradeLevel chuẩn hoá → filter "Cấp lớp" (Lớp 4/5/Ôn thi) + block "Gợi ý cho con anh/chị" map bằng heuristic keyword client-side trên page data (kit note 2 yêu cầu server query).

## Proposed Fix

Thêm `gradeLevel` (enum/int range) vào Course + search param + index; FE chuyển filter/reco sang server query.

## Acceptance Criteria

- [ ] Filter cấp lớp = server-side param
- [ ] Reco map gradeLevel+goal server-side

## Related

- GAP-274 port notes; kit kiteclass-public catalog spec

## Log

- **2026-06-12:** User-approved DEFER khỏi wave landing-100 (AskUserQuestion — heuristic client-side hoạt động đúng cho 3 tenant demo, không trừ điểm rubric; fix schema ~1 ngày ROI thấp khi catalog chỉ seed data). Trailer `WAVE_QUALITY_TARGET_DEFER: GAP-1222` ghi trong PR fix GAP-1221. Fix đúng (gradeLevel enum + migration + server query) execute khi catalog có course thật (Phase 1.5).
