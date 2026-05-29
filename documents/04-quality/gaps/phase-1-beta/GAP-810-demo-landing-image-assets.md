---
id: GAP-810
title: Demo landing image assets — banner hero + ảnh GV + slots wiring
status: PARTIAL
priority: P2
phase: phase-1-beta
domain: Mixed
created: 2026-05-29
---

# GAP-810 — Demo landing image assets (banner hero + ảnh GV)

## Problem

Landing public của tenant (vd Sky Education) render **không có ảnh**: hero chỉ text trên nền nhạt, đội ngũ GV chỉ hiển thị initials. User kỳ vọng landing "chuẩn chỉ" — banner promo (slogan + ảnh GV + CTA), ảnh chân dung GV thật.

## Root Cause

1. **FE không truyền `slots`**: `page.tsx` render `<TemplateRenderer data={landingData} />` thiếu prop `slots` → mọi section nhận `sectionSlots = undefined` → rơi về DEFAULT hardcode. `heroImageUrl` + `teacherBio` BE trả về nhưng không bao giờ vào slot/prop → ảnh không hiển thị dù data có.
2. **BE thiếu asset field**: `LandingPage` entity chỉ có `heroImageUrl` + `logoUrl`; không có danh sách GV có ảnh, testimonial avatar, gallery, OG, favicon.
3. **Seed thiếu**: `BrandingDataSeeder` không tạo `landing_page` row (row lazy-create lúc GET, inherit branding nhưng KHÔNG set heroImageUrl/slogan).
4. AI Branding nội bộ sinh hero = stub (`hero = logoUrl`) + mock mode; bản thật defer Phase 2 (GAP-003).

## Decisions (chốt 2026-05-29)

- **Asset**: GV = ảnh chân dung thật (Đỗ Lan Khánh, môn Pháp Luật và Đời Sống / demo gắn tenant Sky tiếng Anh); hero banner = compose HTML→Playwright render (KHÔNG AI baked-text — chữ Việt crisp). Asset image **local-only, gitignored** (`kiteclass-frontend/public/demo/`, `documents/asset.png`, `documents/sky-banner-draft.png`) — không commit binary lên remote.
- **Storage**: static Next `public/demo/sky/` (URL ổn định, offline). Tenant thật vẫn upload qua MinIO.
- **Slogan**: "Mất gốc tiếng Anh? Đã có cô Khánh".
- **AI Branding re-scope (META, Phase 2)**: banner vừa làm chứng minh hướng **template-composer** (form → parameterized HTML → headless render → PNG) tốt hơn AI image-gen cho asset có chữ (crisp VN, $0, editable). Capture cho Phase 2 ADR + reconsider GAP-003 scope.

## Done (committed, build-verified)

- FE `page.tsx`: build `SectionSlotMap` (hero image + teachers) + pass to TemplateRenderer.
- FE `HeroSection`: 2-column banner layout khi có hero image (slogan text + CTA + ảnh GV circle + ring), text real HTML (accessible/responsive); fallback centered.
- `.gitignore`: demo asset binaries local-only.
- Asset local: `hero-banner.webp` (1200×630, dùng OG), `teacher-do-lan-khanh.webp` (512²), `hero.webp`.
- `pnpm --filter kiteclass-frontend build` PASS (✓ compiled + 59/59 prerender).

## Remaining (PARTIAL → DONE)

- [ ] BE seed Sky `landing_page` row: `heroImageUrl` (ảnh GV banner) + `heroTitle` (slogan) + `tagline`. (inject LandingPageRepository vào BrandingDataSeeder; idempotent, tie instance_id = SKY_TENANT_ID e8ff87e1).
- [ ] (Phase A.2) teachers[] DB table + DTO + seed Đỗ Lan Khánh → Teachers section render ảnh thật (hiện GV đã xuất hiện ở hero banner).
- [ ] OG per-tenant (`generateMetadata` + `openGraph.images`) — defer Phase B (asset gitignored không crawl trên demo local).
- [ ] RST walk local stack (per `feature-ship-runtime-walk-mandate`): BE curl landing → heroImageUrl/slogan; gateway e2e; browser render banner. Verify 3 lớp trước DONE.

## Acceptance Criteria

- [ ] Browse Sky landing → hero hiển thị banner (slogan + ảnh cô Khánh + CTA) trên nền navy theme.
- [ ] Ảnh asset KHÔNG xuất hiện trong `git ls-files` (gitignored, verified).
- [ ] FE build PASS; BE `mvn test` PASS.
- [ ] RST walk evidence 3 lớp paste vào closure block.

## Log

- **2026-05-29:** Gap created + Phase A FE wiring shipped (banner-style hero + slots fix + gitignore, commit 44209856; build-verified). BE seed + walk remaining → PARTIAL. AI-Branding template-composer insight captured cho Phase 2 (GAP-003 re-scope).
