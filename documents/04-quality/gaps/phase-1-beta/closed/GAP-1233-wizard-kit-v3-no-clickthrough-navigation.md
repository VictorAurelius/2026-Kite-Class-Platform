# GAP-1233: Kit AI branding wizard v3 thiếu click-through navigation — phải quay về index để xem bước tiếp

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Design System
**Found:** 2026-06-12 (user-flagged khi review các bước AI branding trong ui_kits)
**Affects:** `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/v3/` (11 screens)

## Problem

Figma prototype trình bày được cả luồng thao tác (click qua từng bước + rẽ nhánh). Kit HTML v3 (canonical design source cho cụm GAP-1134/1147/1135/1021) lại chỉ có screen tĩnh:

- Nút "Tiếp tục"/"Quay lại"/"Tạo banner" là `<button>` trơ — 0 href, 0 onclick.
- 11 screens × 0 inter-screen href (verified `grep href=" *.html"` 2026-06-12).
- Muốn xem bước tiếp phải quay về `index.html` → chọn screen kế — mất đúng giá trị flow walk.

Đặc biệt nghiêm trọng với wizard vì: (a) bản chất là state machine tuyến tính có rẽ nhánh ADR-037 (TEMPLATE → step7 vs FULL_AI → step6-portrait → step8) — nhánh không thể "đi thử" được; (b) các kit anh em (`kiteclass-student` 111 hrefs, `kitehub-admin` 153 hrefs, `kitehub-pro-v2` 182 hrefs) đã wire điều hướng giữa screens — wizard kit drift convention nội bộ; (c) README kit ghi mục đích "ready for human vibe-check" — vibe-check flow không thực hiện trọn vẹn được.

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** kit screens thuộc 1 flow nhưng 0 inter-screen href — CTA trơ, duyệt phải qua index.

**Grep run:** đếm `href="*.html"` (loại index/`../`/`_shared`) per kit screens dir trên toàn `ui_kits/` (2026-06-12).

| # | Site | Screens / hrefs | Verdict | Reason |
|---|---|---|---|---|
| 1 | `ai-branding-wizard-v2/v3/screens` | 11 / 0 | **FIX** (PR này) | Case gốc — canonical design source |
| 2 | `ai-branding-wizard-v2/screens` (v2) | 28 / 0 | EXEMPT | Archive reference — README cấm sửa từng screen |
| 3 | `kiteclass-teacher/screens` | 24 / 17 (16 screens 0-href) | DEFER | State-variants (attendance-marking→saved, grade-entry-editing→finalized, dark/empty/error) chưa link mô phỏng kết quả — cùng class nhưng nhẹ (base screens có nav), scope 16 file → follow-up nếu cần vibe-check state flow |
| 4 | `landing-personal`, `marketing-site`, `kitehub-story-v2` | 1-2 / 0 | EXEMPT | Single-page, không phải flow |
| 5 | Các kit còn lại (student/parent/public/pro-v2/admin) | 39-182 hrefs | EXEMPT | Đã wire đầy đủ |

## Fix shipped (cùng PR)

1. **Stepper thành link nhảy bước:** 8 vị trí stepper trên mỗi screen → `<a class="wiz-step" href="...">` (bước hiện tại giữ `<div>` không link); hover affordance CSS.
2. **Footer CTA wire theo flow:** 1→2→3→4→5→7(template)→8-generating→8-ready→9; Quay lại ngược chiều; step1 "Dùng mặc định" → step9 (mô phỏng AI tự chọn).
3. **Rẽ nhánh ADR-037 đi thử được:** step3-mode lock-note thêm link "Xem nhánh FULL_AI (mô phỏng PREMIUM)" → step6-portrait → "Tạo banner" → step8-generating.
4. **Mô phỏng kết quả async:** step8-generating footer có link "Thành công"/"Lỗi" → step8-ready/failed; failed có "Dùng mẫu dựng sẵn" → ready, "Thử lại" → generating.
5. CSS `a.wiz-step, a.btn` reset anchor defaults trong `v3/styles.css` (token-discipline giữ nguyên).

`step9-preview` "Triển khai & lên sóng" giữ là button trơ — terminal action (SSE deploy), v3 không có screen deployed.

## Acceptance Criteria

- [x] 11/11 v3 screens có inter-screen navigation (stepper + footer CTA) — link integrity check: mọi `href="step*.html"` resolve (verified script 2026-06-12)
- [x] 2 nhánh ADR-037 walk được trọn bằng click: TEMPLATE (1→…→5→7→8→9) + FULL_AI (3→6→8→9)
- [x] 3 trạng thái banner (generating/failed/ready) chuyển qua lại được bằng link mô phỏng
- [x] Sweep cross-kit documented (bảng trên) — teacher state-variants DEFER có ghi nhận

## Related

- Discovered in: session 2026-06-12 (user review wizard steps)
- Design source: GAP-1212 (v3 refresh, Wave ui-kits-100 Bucket D)
- Sister convention: các kit student/admin/pro-v2 đã có inter-screen nav
- DEFER scope: kiteclass-teacher 16 state-variant screens (chưa file gap riêng — nhẹ, base nav có; file khi cần state-flow vibe-check)
