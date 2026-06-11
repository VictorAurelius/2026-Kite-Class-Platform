# GAP-1223: Token drift 2 chiều — kit dùng Inter (sai chuẩn Be Vietnam Pro) + production container lệch 1180px marketing convention

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend / Design System
**Found:** 2026-06-11 (user-flagged khi so kit localhost:8090 vs production — "chỉ riêng font chữ, layout đã không đạt")
**Affects:** `ui_kits/{landing-personal,kiteclass-public}` (font) + `kiteclass-frontend (public)` pages (container/spacing)

## Problem

Canonical design system (`ui_kits/marketing-site/colors_and_type.css`): `--font-sans/--font-display = 'Be Vietnam Pro'` ("primary typeface — full Vietnamese diacritic") + `--container: 1180px`. Drift 2 chiều:
1. **Kit lệch font**: 2 kit mới (landing-personal 113/128 + kiteclass-public 4 screens) dùng `Inter` — agent design không đọc shared tokens → vi phạm chính design system + hard-gate "no hardcoded tokens" tinh thần.
2. **Production lệch layout**: trang (public) dùng Tailwind `container` default (~1280-1400 responsive) thay vì 1180px marketing convention → nhịp trắng/độ rộng khác kit nhìn thấy ngay.

Same class GAP-1208/1212 (thiếu/không-đọc spec nguồn) — lần này ở token layer.

## Proposed Fix

1. Kit: `--font-sans` → Be Vietnam Pro + Google Fonts link (landing-personal + 4 screens kiteclass-public + hub).
2. Production: (public) pages container → `max-w-[1180px]` (hoặc token chung) đồng nhất kit.
3. META nhỏ: kit-authoring checklist thêm "đọc colors_and_type.css tokens trước" (note vào ui_kits/README).

## Acceptance Criteria

- [ ] Kit render Be Vietnam Pro (cả heading weights)
- [ ] Production (public) container 1180px — so screenshot side-by-side khớp nhịp
- [ ] ui_kits/README note token-source mandate

## Related

- User-flagged 2026-06-11; `colors_and_type.css` canonical; same-class GAP-1208/1212; re-score artifact 2026-06-11 (đang chấm — score hiện tại phản ánh drift này)
