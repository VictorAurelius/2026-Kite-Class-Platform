# GAP-968: Logo upload thiếu Zalo OA share preview (1200x630 OG image)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Logo upload + share UX) — VN-specific Zalo OA culture
**Defer-to:** After Wave flow-kh3 finish (Phase 1.5 Zalo OA wave per benchmark B5)

## Problem

BR-SET-11 logo upload S3. NHƯNG bác Hùng marketing chính qua Zalo OA — cần preview "logo sẽ trông thế nào trên Zalo card share". Upload logo 4MB PNG → S3 OK nhưng khi share Zalo → logo blur (Zalo cần 1200x630 OG image). Bác Hùng đổ lỗi "phần mềm KiteClass tệ". Per benchmark B5: 95% VN users prefer Zalo over email. Surfaced: persona Finding 3.4 + benchmark B5.

## Proposed Fix

Auto-generate Zalo OG variant (1200x630 PNG) khi upload logo. Use AWS Lambda + Sharp resize. Store side-by-side. FE settings page show preview "Hình ảnh khi share trên Zalo" với generated variant.

## Acceptance Criteria

- [ ] Logo upload triggers auto-resize → 1200x630 OG variant
- [ ] `<meta property="og:image">` tag uses 1200x630 variant
- [ ] FE preview hiển thị Zalo share card mockup

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,external-benchmark}.md
- Sister: GAP-819 (Zalo OA active push — Phase 1.5+)
- Flow Verification Campaign §4 row KC-1
