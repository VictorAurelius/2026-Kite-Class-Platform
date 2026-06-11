---
id: GAP-1195
title: Fix asset ảnh demo — webp + next/image, gitignore demo-banners/, tách logo≠banner, Khánh hero durable
status: OPEN
priority: P2
domain: Frontend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 0
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

- [ ] demo-banner convert webp + `next/image` render
- [ ] `.gitignore` thêm `demo-banners/`, PNG gỡ khỏi git track
- [ ] logo file riêng ≠ hero banner
- [ ] Khánh hero asset committed, không 404 remote
- [ ] G2 walk: landing 2 tenant ảnh render đúng, không 404

## Related

- Wave: `wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket F (🔨 Delta)
- Trigger audit: `documents/04-quality/audits/2026-06-11-demo-trio-seed-coverage-audit.md`
