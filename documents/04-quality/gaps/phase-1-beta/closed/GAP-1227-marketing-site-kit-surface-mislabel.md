# GAP-1227: Kit marketing-site mislabel surface — ghi design-target = kiteclass (public)/page.tsx trong khi đúng là kitehub-frontend apex

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Design System
**Found:** 2026-06-11 (user-flagged lần 2: "marketing-site đây là cho kiteclass hay kitehub, tôi nhớ đã có bắt lỗi này rồi")
**Affects:** `ui_kits/marketing-site/README.md` + `ui_kits/index.html` card

## Problem

Kit README ghi "design target for production `kiteclass-frontend (public)/page.tsx`" — SAI surface: đó là per-tenant landing. Trang marketing/beta-signup platform thật = `kitehub-frontend (public)` (LandingShell + KH-1 request-beta-access, apex kitehub.me) per `kitehub-kiteclass-boundary` §2 — bẫy trùng-tên "marketing" §2.1 (quảng bá sản phẩm KiteClass ≠ surface KiteClass). **Downstream cost đã trả:** mislabel này là root khiến Bucket F wave landing-100 port platform-pitch copy vào tenant landing (GAP-1205) — user bắt lần 1 qua chuỗi đó, lần 2 trực tiếp hôm nay.

## Fix (shipped PR #2326)

README header + cảnh báo surface KITEHUB + cross-ref boundary §2.1; index card sửa tương ứng. Boundary rule đã có sẵn (06-09) — đây là sweep artifact docs theo rule đó (cross-flow class: mislabel KH/KC trong design docs).

## Acceptance Criteria

- [x] README + card ghi đúng surface (kitehub-frontend :3001 / apex)
- [x] Cross-ref boundary §2.1 + GAP-1205 chain documented

## Related

- `kitehub-kiteclass-boundary.md` §2.1 (trùng-tên trap); GAP-1205 (hệ quả lần 1); GAP-274 (kit kiteclass-public — đúng surface KC)
