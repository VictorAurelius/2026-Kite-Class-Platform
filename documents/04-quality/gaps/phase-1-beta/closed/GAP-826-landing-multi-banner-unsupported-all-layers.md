---
id: GAP-826
title: Landing đa-banner không hỗ trợ ở cả 3 lớp (data/render/UI) — carousel CSS orphan
status: DONE
priority: P3
phase: phase-1-beta
domain: KiteClass
created: 2026-06-01
---

# GAP-826 — Landing đa-banner (multi-banner) chưa hỗ trợ + carousel CSS orphan

> Surfaced bởi state-check AI-branding landing input→render flow (2026-06-01), kích bởi user review marketing-site: "khi yêu cầu claude design tôi đã muốn 1 landing chứa được nhiều banner mà". State-check phát hiện năng lực đa-banner đã có **CSS** nhưng **bị bỏ rơi ở cả 3 lớp** — cùng class dropped-affordance với ThemeSwitcher (đã fix #2030).

## Problem

User muốn landing per-tenant hỗ trợ **nhiều banner** (carousel/rotator). State-check cho thấy năng lực này KHÔNG tồn tại ở bất kỳ lớp functional nào — chỉ còn CSS mồ côi:

| Lớp | Trạng thái đa-banner | Bằng chứng |
|---|---|---|
| **Data/API** | ❌ single | `LandingPage.heroImageUrl` = `String` đơn (entity:46); `UpdateLandingPageRequest.heroImageUrl` single — KHÔNG có `heroImages: List` / banner array |
| **Render** | ❌ single | `HeroSection.tsx` render 1 banner PNG baked (comment: "Composed banner đã bake... dùng làm HERO full-width"); không `.map` qua images, không carousel |
| **UI input** | ❌ absent | `branding-settings.tsx` chỉ set logo + 3 màu + tagline; KHÔNG có hero banner upload/quản lý. Landing editor UI = GAP-815 (OPEN) |
| **CSS** | ⚠️ orphan | `landing.css:132-202` có `.hero-slide` / `.hero-dots` / `.hero-arrow` (HERO CAROUSEL 3-layer) — **không nơi nào dùng** (dead code) |

→ Đa-banner được **thiết kế** (CSS carousel) nhưng **drop khi implement** ở data + render + UI. Giống hệt ThemeSwitcher (parity miss per `design-source-implementation-parity.md`).

## State-check toàn luồng AI-branding input→render (2026-06-01)

Tóm tắt findings (luồng: tenant cấp banner/text → AI branding → landing render):

| # | Mảnh | Trạng thái | Gap |
|---|---|---|---|
| 1 | ThemeSwitcher (marketing-site 4 theme) | ✅ FIXED #2030 (was dropped) | — |
| 2 | Multi-banner (data+render+UI) | ❌ unsupported, carousel CSS orphan | **GAP-826 (này)** |
| 3 | Landing content editor UI (hero/section self-service) | ❌ absent (chỉ SQL/API thô + logo/color) | GAP-815 OPEN |
| 4 | AI branding ↔ per-tenant landing banner | ⚠️ disconnect — kitehub-branding gen platform THEME màu; banner = dev script `compose-sky-demo-banner.mjs` thủ công, không wire tenant input | GAP-810 PARTIAL + GAP-003 OPEN |

## Proposed Fix (cần quyết định hướng)

Đa-banner là feature thật (3 lớp) → **Phase 1.5+** scope. 2 hướng, cần user chốt:

- **A. Implement multi-banner:** thêm `heroImages: List<String>` (data + V-migration) + carousel render trong HeroSection (wire `.hero-slide/.hero-dots/.hero-arrow` đã có) + UI quản lý banner trong landing editor (gộp GAP-815). Đầy đủ nhưng lớn.
- **B. Giữ single-banner, xóa CSS orphan:** nếu single-banner là design thật (1 banner baked đủ cho Phase 1 BETA), **remove dead carousel CSS** (`landing.css:132-202`) để tránh hiểu nhầm "có carousel". Nhỏ, sạch.

Khuyến nghị: **B cho Phase 1 BETA** (1 banner đủ MVP, xóa orphan tránh drift) + defer A sang Phase 1.5 nếu user thật sự cần rotator. Chốt theo user.

## Acceptance Criteria

- [x] User chốt hướng A (implement) — directive 2026-06-11
- [x] Nếu A: `heroImages` data field + carousel render + UI; browser-verify carousel hoạt động (per `design-source-implementation-parity.md` §3.2 runtime click-verify)
- [x] Nếu B: xóa `.hero-slide/.hero-dots/.hero-arrow` khỏi landing.css + ghi nhận single-banner là design canonical; defer-gap cho multi-banner Phase 1.5

## Update 2026-06-01 — outside-in 3-agent confirm

Outside-in audit landing-input (persona + benchmark + failure-mode) **đồng thuận multi-banner carousel = DEFER Phase 1.5** (không phải Phase 1 MVP). User chốt option A (carousel xoay live) cho Phase 1.5; prototype browser-verified + deployed (PR #2033, `marketing-site/carousel-demo.html`) làm design reference. Phase 1 = single banner + lead-form (GAP-828) + safety (GAP-827).

## Related

- `design-source-implementation-parity.md` v1.1.0 — đa-banner = cùng dropped-affordance class với ThemeSwitcher; §3 row 2 (states/variants) + row 5 (copied-but-unwired = orphan CSS)
- GAP-827 (input safety) + GAP-828 (conversion scope) — sister landing-input gaps (3-agent outside-in 2026-06-01)
- ADR-037 (AI branding stack) — banner gen route (GPT 5.5)
- GAP-815 (landing content editor UI) — UI lớp nhập input
- GAP-810 (banner image assets) PARTIAL + GAP-003 (AI branding) OPEN — banner generation
- `cross-flow-bug-class-sweep.md` — orphan CSS = sister site của ThemeSwitcher dead-code

## Log

- **2026-06-11 (DONE — hướng A, 3 lớp):** User chốt A. Ship PR #2326: **Data** `hero_images` JSONB V96 + DTO/sanitizer per-element + presigned regenerate per-element + seeder (Sky 2 slide, Hà/Nhì 1); **Render** `HeroBannerCarousel` client (crossfade + dots/arrows + auto-rotate 5s + pause + reduced-motion) trong khung phải GAP-1210, ≥2 ảnh carousel / 1 ảnh tĩnh / fallback heroImageUrl; **UI** card "Banner landing" trong branding-settings (list + add-by-URL + remove + reorder + save). Upload-by-file → follow-up GAP-1211 (endpoint riêng, logo|favicon overwrite-slot không tái dùng được). Design source: kit `ui_kits/landing-personal` (113/128). Runtime verified: DB rows + API heroImages array + browser sky 2-slide render + assets 200. Tests BE 12/12 + FE 10/10 + builds PASS.
