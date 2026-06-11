# GAP-1206: Mobile header dùng heroTitle dài làm brand → wrap 4 dòng chiếm nửa màn hình

**Status:** 🔵 OPEN
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

- [ ] Mobile header ≤2 dòng (ưu tiên 1) mọi tenant demo-trio
- [ ] Desktop không regression
- [ ] Screenshot mobile before/after

## Related

- Discovered in: landing-100 G2★ UI review (PR #2326 session); screenshot /tmp/ui-coha-mobile.png
- Sister: GAP-1083 (centerName BE field), wave landing-100 Bucket B
