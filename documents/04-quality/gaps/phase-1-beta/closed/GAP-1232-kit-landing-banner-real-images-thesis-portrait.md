# GAP-1232: Kit landing-personal hero + about dùng placeholder (emoji/gradient) thay vì ảnh THẬT từ `documents/08-thesis/portrait` — lệch production

**Status:** 🟢 DONE 2026-06-12 — fixed cùng PR file gap này (Wave ui-kits-100 follow-up)
**Priority:** 🟡 P2
**Domain:** Design System
**Found:** 2026-06-12 (user-flagged: "banner trong UI kits của landing page kiteclass cũng cần dùng ảnh thật từ documents/08-thesis/portrait")
**Affects:** `ui_kits/landing-personal/` hero carousel + `ui_kits/kiteclass-public/screens/about.html` story-photo

## Problem

Production landing dùng **banner AI-scene thật** (`/demo-banners/*.webp` — sinh từ `documents/08-thesis/portrait`, seeded per thesis §4.1-4.2) làm ảnh nền hero per GAP-810 pattern. Nhưng kit (design source of truth per `frontend-standards.md` §3.1):
- `landing-personal` hero carousel: 3 slide = **emoji to (📐🎯👩‍🏫) + text trên gradient** — không có ảnh thật
- `kiteclass-public/about.html` `.story-photo`: **gradient + emoji 👩‍🏫** thay chân dung GV

Kit ↔ production parity vỡ đúng chiều production→kit back-port (lesson #2326): production có ảnh thật, kit mock — reviewer nhìn kit không thấy thiết kế thật.

## Fix (shipped cùng PR)

1. Copy 3 banner webp optimized (47-90KB, đã committed production) → `ui_kits/_shared/assets/banners/` (Pages site root = ui_kits nên kit reference được)
2. Convert 3 chân dung `portrait-goc.png` (1.5-2MB) → `_shared/assets/portraits/{ha,nhi,khanh}.webp` 480w (8-24KB, PIL quality 80)
3. `landing-personal` slide: ảnh thật làm NỀN qua CSS var `--banner-img` per theme class (Cô Hà/Thầy Nhì/Cô Khánh — theme switcher tự swap, zero JS change) + scrim tối 30→78% giữ text-overlay ≥4.5:1 (pattern GAP-810: KHÔNG bake text) + bỏ emoji; fallback gradient khi thiếu ảnh
4. `about.html` `.story-photo`: chân dung thật per theme qua `--portrait-img` + `role="img"` aria-label; fallback gradient

## Acceptance Criteria

- [x] Hero carousel landing-personal render banner thật 3 GV theo theme switcher (runtime swap)
- [x] about.html chân dung thật per theme
- [x] Text overlay đạt contrast (scrim documented trong CSS comment)
- [x] Asset nhẹ (≤100KB/ảnh) committed trong `_shared/assets/` — Pages serve được
- [x] check-ui-kits-landing.sh parity PASS

## Related

- GAP-810 (hero ảnh nền + overlay pattern — production), GAP-826 (carousel), GAP-1210 (hero 2 cột), GAP-274 (theme switcher), GAP-366 §3.1 (kit-as-source-of-truth — chiều production→kit)
- Nguồn ảnh: `documents/08-thesis/portrait/` (tracked, PNG gốc) → webp optimized
- Discovered in: user-flagged 2026-06-12 (chuỗi follow-up GAP-1229 favicon/asset reuse)
