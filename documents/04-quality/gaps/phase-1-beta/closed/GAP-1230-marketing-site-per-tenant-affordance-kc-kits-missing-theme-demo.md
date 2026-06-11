# GAP-1230: GAP-1227 chưa triệt để — marketing-site (KH) còn affordance per-tenant (ThemeSwitcher) + 4 KC dashboard kits THIẾU tenant-theme demo

**Status:** 🟢 DONE — phần (a)+(c) fixed cùng PR file gap; phần (b) 4 KC kits shipped Bucket I wave ui-kits-100 (2026-06-11)
**Priority:** 🟠 P1 (in-wave per quality-target gate §2.5)
**Domain:** Design System
**Found:** 2026-06-11 (user-flagged lần 3 chuỗi GAP-1227: "giới thiệu kitehub nhưng nội dung bên trong vẫn là kiteclass; bộ Chủ đề theo giáo viên đang của kitehub => kitehub không cần => toàn bộ UI kiteclass cần")
**Affects:** `ui_kits/marketing-site/` (KH apex) + `ui_kits/{kiteclass-student,kiteclass-teacher,kiteclass-parent,kiteclass-pro-v2}` (KC dashboards)

## Problem

GAP-1227 (DONE #2326) fix LABEL surface của marketing-site nhưng chưa sweep CONTENT-level:

1. **Affordance per-tenant nằm sai surface:** `primitives.jsx` `ThemeSwitcher` "Chủ đề theo giáo viên" (theme Cô Hà/Thầy Nhì) render trong `index.html` áp theme TOÀN trang marketing — per-teacher theming là tính năng per-tenant của **KiteClass**; KH apex brand KiteHub CỐ ĐỊNH (sky+orange) per boundary §2.
2. **Phân định content:** apex giới thiệu SẢN PHẨM KiteClass là đúng thiết kế (boundary §2.1 "quảng bá sản phẩm KiteClass ≠ surface KiteClass") — app-mock minh hoạ OK; nhưng cần content-contract rõ trong README để class này không tái phát (user point 1).
3. **Sweep ngược (cross-flow):** "toàn bộ UI kiteclass cần" tenant-theme demo — kiểm tra 6 KC kits: `landing-personal` ✅ + `kiteclass-public` ✅ có switcher 3-GV; **4 kit dashboard THIẾU** (`kiteclass-student`, `kiteclass-teacher`, `kiteclass-parent`, `kiteclass-pro-v2`) — UI per-tenant phải demo được brand tenant áp toàn bộ (token `hsl(var(--*))` đã sẵn, thiếu theme classes + switcher demo).

## Proposed Fix

- **(a) — fixed cùng PR này:** gỡ `ThemeSwitcher` khỏi `marketing-site/index.html` (KH brand cố định) + deprecation note trong `primitives.jsx` (pattern reference cho KC kits).
- **(b) — Bucket I (agent):** add tenant-theme demo vào 4 KC dashboard kits — port pattern 3-GV switcher từ `landing-personal`/`kiteclass-public` (theme classes `.theme-ha/.theme-nhi/.theme-khanh` set trên `<body>` + cập nhật brand identity), token-compliant, mỗi kit ghi chú "production: theme từ `branding` package per ADR-009, switcher chỉ là demo".
- **(c) — fixed cùng PR này:** content-contract section trong `marketing-site/README.md` (platform voice + illustration vs affordance).

## Acceptance Criteria

- [x] (a) marketing-site không còn ThemeSwitcher render; brand KH cố định
- [x] (c) README content-contract documented (boundary §2.1 cite)
- [x] (b) 4 KC dashboard kits có tenant-theme demo 3-GV hoạt động runtime (per `design-source-implementation-parity` §3.2 — không inert)
- [x] check-ui-kits-landing.sh PASS + screens render đúng theme khi switch

## Related

- GAP-1227 (DONE — label fix; gap này = content-level follow-up, DONE-never-reopens per gap-done-discipline §3)
- GAP-1230 phần (b) host: wave ui-kits-100 Bucket I; `kitehub-kiteclass-boundary.md` §2/§2.1; ADR-009 branding package
- Discovered in: user-flagged 2026-06-11 wave ui-kits-100 session

## Log

- **2026-06-11 (Bucket I wave ui-kits-100):** phần (b) shipped → Status PARTIAL → DONE. Tạo shared `_shared/scripts/tenant-theme-demo.{css,js}` (DRY 1 file cho 4 kit), port pattern 3-GV demo-trio từ `kiteclass-public/about.html` + `landing-personal` (Cô Hà `#2563EB` / Thầy Nhì `#16A34A` / Cô Khánh `#EA580C`). Floating switcher set class `kc-demo-{ha|nhi|khanh}` trên `<html>`+`<body>` (giữ class kit có sẵn) → override token `--primary`/`--accent`/`--ring` (HSL khớp `_shared/colors_and_type.css`) + cập nhật brand identity (mark+name+tag) trong widget runtime THẬT, không inert (per `design-source-implementation-parity.md` §3.2). Selector `html.kc-demo-*, body.kc-demo-*` thắng mọi kit dù token định nghĩa trên `:root` (teacher/pro-v2) hay `.theme-kiteclass-*` (student/parent). Wire 68 file (screens depth-2 `../../` + index depth-1 `../`) qua 4 kit: student 14 / teacher 25 / parent 18 / pro-v2 11. README mỗi kit thêm section "Tenant-theme demo (GAP-1230)" + production note ADR-009. Verify: `node --check` JS OK · `check-ui-kits-landing.sh` PASS parity 12/12 · `check-gap-status-csv.sh` PASS · `check-gap-folder-location.sh --warn` PASS · 0 double-insert. Production: theme thật từ `branding` package per ADR-009 — switcher chỉ là demo affordance.
