# GAP-1094: Link `/docs/data-reset-policy` 404 — trang docs chưa tồn tại (`BetaDisclaimerBanner` prefetch dead-link)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-09 (tier-UI fix session — browser verify + BE sweep)
**Affects:** `kitehub-frontend` `components/beta-disclaimer/BetaDisclaimerBanner.tsx:126` → `<Link href="/docs/data-reset-policy">` ; route `/docs/data-reset-policy` chưa có trong app router

## Problem

Trong khi browser-verify tier-UI session 2026-06-09, console hiển thị **3× HTTP 404** cho `GET /docs/data-reset-policy?_rsc=...` trên dashboard + settings walk. Đây là Next.js `<Link>` prefetch (query `?_rsc=` = React Server Component prefetch) tới một docs page CHƯA tồn tại.

- Source link: `components/beta-disclaimer/BetaDisclaimerBanner.tsx:126` — `href="/docs/data-reset-policy"` (chèn từ GAP-560 beta-disclaimer banner; có test `BetaDisclaimerBanner.test.tsx:52/56` assert href này).
- Route đích: `find kitehub/kitehub-frontend/src/app/docs` → **không có thư mục `docs/`** → mọi navigate/prefetch tới `/docs/data-reset-policy` trả 404.

Đây là cosmetic dead-link (P3): không chặn flow chính, nhưng prefetch 404 lặp lại trên mọi page có banner gây noise console + user click "chính sách reset dữ liệu" sẽ vào trang trống.

## Root Cause

Banner GAP-560 link tới `/docs/data-reset-policy` như placeholder "full policy detail", nhưng trang docs tương ứng chưa được tạo trong app router → href trỏ vào route không tồn tại.

## Proposed Fix

Một trong hai hướng:
1. Tạo trang `kitehub-frontend/src/app/docs/data-reset-policy/page.tsx` (nội dung chính sách reset dữ liệu Phase 1 BETA), HOẶC
2. Sửa `href` trong `BetaDisclaimerBanner.tsx:126` trỏ tới docs page đã tồn tại / external policy URL hợp lệ (cập nhật test assertion cùng PR).

## Acceptance Criteria

- [ ] Không còn 404 `/docs/data-reset-policy` trên dashboard/settings walk (browser console clean)
- [ ] Link "chính sách reset dữ liệu" trong beta banner trỏ tới trang render được (hoặc external URL hợp lệ)
- [ ] Test `BetaDisclaimerBanner.test.tsx` cập nhật khớp href cuối cùng (nếu đổi href)

## Related

- Discovered in: tier-UI fix session 2026-06-09 (browser verify `:3001` tenant test-8 — 3× 404 prefetch)
- Source banner: GAP-560 (beta-disclaimer banner thêm link policy)
