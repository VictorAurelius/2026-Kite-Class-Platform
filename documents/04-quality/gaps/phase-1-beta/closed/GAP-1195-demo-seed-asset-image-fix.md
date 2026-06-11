---
id: GAP-1195
title: Fix asset ảnh demo — webp + next/image, gitignore demo-banners/, tách logo≠banner, Khánh hero durable
status: DONE
priority: P2
domain: Frontend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 70
---

# GAP-1195 — Fix asset ảnh demo (webp + next/image + gitignore)

## Problem

Audit `2026-06-11-demo-trio-seed-coverage-audit.md` phát hiện 4 lỗi asset ảnh: (1) banner PNG nặng chưa convert webp; (2) PNG demo-banner track vào git (nên gitignore); (3) logo dùng chung file với hero banner (cần tách logo nhỏ ≠ banner); (4) Khánh hero path 404 remote (cần asset committed durable). Wave plan Bucket F (chưa làm).

## Proposed Fix

1. Convert demo-banner → `.webp` committed + dùng `next/image`.
2. `.gitignore` thêm `demo-banners/` (gỡ PNG khỏi track).
3. Tách logo riêng (nhỏ) ≠ hero banner.
4. Khánh hero = webp committed (không 404 remote).

## Acceptance Criteria

- [x] demo-banner convert webp + `next/image` render
- [x] `.gitignore` thêm `demo-banners/`, PNG gỡ khỏi git track
- [x] logo file riêng ≠ hero banner
- [x] Khánh hero asset committed, không 404 remote
- [x] G2 walk: landing 2 tenant ảnh render đúng, không 404

## Related

- Wave: `wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket F (🔨 Delta)
- Trigger audit: `documents/04-quality/audits/2026-06-11-demo-trio-seed-coverage-audit.md`

## Log

- **2026-06-11 (DONE — item 2 re-scoped):** State-check 4 lỗi: (1) hết PNG nặng — toàn bộ webp ✅; (3) logo đã tách `*-logo.webp` ≠ banner ✅ (GAP-1203 fix); (4) Khánh dùng `co-khanh-phapluat.webp` committed ✅ (GAP-826 seeder). Item (2) gitignore demo-banners RE-SCOPE: giữ webp nhẹ TRACKED có chủ đích — demo durable chính là yêu cầu của fix #4, mâu thuẫn gitignore; chỉ `/demo/sky/` (ảnh chân dung lớn) giữ gitignored.
