---
audience: dev
---

# GAP-767 — KH `/faq` route HTTP 404 (anonymous FAQ chưa wired)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-05-27 (Wave 106 RST Mảng A3)
**Affects:** KH anonymous user — FAQ discovery
**Phase:** phase-1-beta

## Problem

`documents/05-guides/user-manual/anonymous/faq.md` đã được tạo trong Wave 79 Bucket F1 (per `user-manual-content-standard.md` v1.0.0). Nhưng FE route chưa wired:

- `/faq` → HTTP 404
- `/help/anonymous/faq` → HTTP 404
- `/help` → HTTP ? (cần probe)

Anonymous user landing `/` → cần tìm FAQ → fail nhanh.

## Root Cause

Wave 79 ship docs source (`documents/05-guides/user-manual/anonymous/faq.md`) nhưng MDX route binding trong `kitehub-frontend/src/app/help/anonymous/` chưa được tạo. Per `user-manual-content-standard.md` §1 scope: rule applies to BOTH `documents/05-guides/user-manual/**` AND `kitehub/kitehub-frontend/src/app/help/**`.

## Proposed Fix (defer Đợt 107 — anonymous user manual P2/P3 personas continued from Wave 79)

Tạo route `kitehub-frontend/src/app/help/anonymous/faq/page.tsx` consuming `documents/05-guides/user-manual/anonymous/faq.md` content (MDX import OR copy).

Cũng wire `/help/anonymous/{index,pricing,beta-access,terms}` (4 sister pages from Wave 79 Bucket F1).

Per `user-manual-content-standard.md` §3 discoverability matrix Anonymous persona, FAQ cần ≥3 entry points:
1. Landing top nav "Trợ giúp" hoặc footer "Tài liệu"
2. Beta request form fail-state "Có thắc mắc? Xem FAQ"
3. Google indexable URL `/help/anonymous/faq` public

## Acceptance Criteria

- [ ] `/help/anonymous/faq` HTTP 200
- [ ] Content Vietnamese narrative từ source `documents/05-guides/user-manual/anonymous/faq.md`
- [ ] ≥3 entry points per persona matrix
- [ ] E2E spec paired (route + title assertion)

## Related

- Wave 79 Bucket F1 (anonymous user-manual 5-page prototype)
- `user-manual-content-standard.md` §1 scope + §3 discoverability matrix
- GAP-537 (Wave 79 user manual P2/P3 continued)
- Rule: `e2e-rst-test-layer-boundary.md` §3
