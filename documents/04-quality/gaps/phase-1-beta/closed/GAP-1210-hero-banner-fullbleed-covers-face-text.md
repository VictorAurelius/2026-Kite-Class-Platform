# GAP-1210: Hero full-bleed che mặt + đè chữ trong banner — design source đặt banner trong khung bên phải

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-11 (user G2★ walk — "assets banner sẽ được đặt ở trong khung bên phải trong source của claude design... full screen đang bị che mặt, thiếu chữ trong ảnh")
**Affects:** `kiteclass-frontend/src/components/sections/HeroSection.tsx` (nhánh có heroImage)

## Problem

Wave landing-100 Bucket C rework hero sang **full-bleed background** (AI-scene + gradient scrim + HTML overlay). Banner demo chứa NGƯỜI (chân dung GV) và đôi khi chữ baked-in — không được art-direct safe-zone → scrim tối bên trái + text overlay **che mặt + đè mờ chữ trong ảnh**.

## Design-first verdict (per design-source-implementation-parity)

- Claude Design source + GAP-810 hero gốc (2026-05-29): **2 cột — copy trái, banner/ảnh GV ĐÓNG KHUNG bên phải** (không overlay lên ảnh).
- Kit marketing-site cùng pattern (visual AppMock khung phải).
- Full-bleed chỉ hợp khi ảnh là scene thuần không nội dung quan trọng vùng overlay — không đúng với banner người+chữ.
→ **Khung bên phải hợp lý.** Bucket C full-bleed = parity drop không document (vi phạm §3 checklist).

## Fix (shipped PR #2326)

HeroSection nhánh heroImage → grid 2 cột: copy trái trên nền theme gradient (giữ CTA/trust ribbons), banner trong khung phải (rounded-2xl + ring + shadow + glow, KHÔNG scrim/text trên ảnh); mobile stack copy → khung. Fallback không ảnh giữ nguyên.

## Acceptance Criteria

- [x] Banner hiển thị nguyên vẹn trong khung (mặt + chữ trong ảnh không bị che)
- [x] Copy không overlap ảnh ở mọi breakpoint
- [x] Build + tests PASS + re-walk screenshot 3 tenant

## Related

- Sister: GAP-810 (hero 2 cột gốc), GAP-958 (Bucket C rework — full-bleed này từ đó), `design-source-implementation-parity.md` §3
- Discovered in: user G2★ walk 2026-06-11
