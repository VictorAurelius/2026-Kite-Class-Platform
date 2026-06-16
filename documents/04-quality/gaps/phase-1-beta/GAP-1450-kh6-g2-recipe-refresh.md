# GAP-1450: KH-6 G2 recipe refresh — thiếu tenant PREMIUM để verify cluster GAP-1090/1091 PREMIUM-display

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Docs
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-6)
**Affects:** KH-6 — `documents/05-guides/operations/2026-06-06-g2-recipe-kh6-ai-branding-wizard.md` §1/§2.2/§4

## Problem
Discovered Phase-2 browser walk KH-6. Instance `7862ab7e` (PREMIUM) recipe yêu cầu không tồn tại trong DB; verify phải thay bằng `g2test-an-8` instance `68e3ab87` tier BASIC. GAP-1090/1091 (đều DONE) đã verify gián tiếp qua BASIC (real-tier không hardcode), nhưng path PREMIUM-specific (§4 a/b/c: 30/30 + no CTA) chưa chạy thực tế.

## Proposed Fix
Re-seed 1 tenant PREMIUM ACTIVE (hoặc chỉ định credential PREMIUM hiện có) để chạy §4 (a)(b)(c) PREMIUM-display; HOẶC cập nhật recipe verify với BASIC + note PREMIUM cần seed riêng.

## Acceptance Criteria
- [ ] §4 PREMIUM-display verify chạy được với tenant PREMIUM thật, HOẶC recipe note rõ PREMIUM cần seed riêng

## Related
- Discovered in: Phase-2 browser walk (flow KH-6), 2026-06-16
- PREMIUM-display: GAP-1090 / GAP-1091 (DONE — verify gián tiếp qua BASIC)
