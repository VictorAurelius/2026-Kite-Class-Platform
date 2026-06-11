# GAP-1206: Mobile header dùng heroTitle dài làm brand → wrap 4 dòng chiếm nửa màn hình

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-11 (landing-100 UI review mobile 390×844)
**Affects:** `kiteclass/kiteclass-frontend/src/app/(public)/layout.tsx` header brand (`name: landing.heroTitle` fallback)

## Problem

Mobile (390px): header brand = `heroTitle` ("Lấy lại căn bản môn Toán cùng cô Hà") wrap **4 dòng**, đẩy header cao ~150px, chiếm phần lớn first viewport — xấu + che hero. Nguyên nhân: Bucket B dùng heroTitle làm brand fallback khi `centerName` chưa có (BE field đã ship per GAP-1083 nhưng seed data chưa có centerName ngắn).

## Proposed Fix

1. Header brand: clamp 1 dòng (`truncate` / `line-clamp-1`) + font nhỏ hơn ở mobile.
2. Seed `centerName` ngắn cho demo-trio ("Cô Hà Toán", "Thầy Nhì Hóa", "Sky Education") — brand dùng centerName trước heroTitle.

## Acceptance Criteria

- [x] Mobile header ≤2 dòng (ưu tiên 1) mọi tenant demo-trio
- [x] Desktop không regression
- [x] Screenshot mobile before/after

## Log

- **2026-06-11 (DONE):** Fix-pack PR #2326 Bucket A: header brand truncate + responsive size + ưu tiên centerName; Bucket B seed centerName ngắn ("Cô Hà Toán"...). Verified live mobile 390px: brand 1 dòng (screenshot).

## Related

- Discovered in: landing-100 G2★ UI review (PR #2326 session); screenshot /tmp/ui-coha-mobile.png
- Sister: GAP-1083 (centerName BE field), wave landing-100 Bucket B
