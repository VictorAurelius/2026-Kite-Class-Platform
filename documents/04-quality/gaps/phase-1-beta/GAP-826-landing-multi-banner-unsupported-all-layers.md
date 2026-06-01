---
id: GAP-826
title: Landing đa-banner không hỗ trợ ở cả 3 lớp (data/render/UI) — carousel CSS orphan
status: OPEN
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

- [ ] User chốt hướng A (implement) hoặc B (remove orphan + defer)
- [ ] Nếu A: `heroImages` data field + carousel render + UI; browser-verify carousel hoạt động (per `design-source-implementation-parity.md` §3.2 runtime click-verify)
- [ ] Nếu B: xóa `.hero-slide/.hero-dots/.hero-arrow` khỏi landing.css + ghi nhận single-banner là design canonical; defer-gap cho multi-banner Phase 1.5

## Related

- `design-source-implementation-parity.md` v1.1.0 — đa-banner = cùng dropped-affordance class với ThemeSwitcher; §3 row 2 (states/variants) + row 5 (copied-but-unwired = orphan CSS)
- GAP-815 (landing content editor UI) — UI lớp nhập input
- GAP-810 (banner image assets) PARTIAL + GAP-003 (AI branding) OPEN — banner generation
- `cross-flow-bug-class-sweep.md` — orphan CSS = sister site của ThemeSwitcher dead-code
