# GAP-1083: LandingPageResponse thiếu field BE (centerName + problemSolution/howItWorks/trustStrip/zaloUrl) → landing-100 sections fallback default thay vì per-tenant data

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-09 (Wave landing-100 integration — Đợt 2 flagged + F-slot wiring gap)
**Affects:** `kiteclass-core` LandingPage entity + LandingPageResponse DTO + mapper + migration; `kiteclass-frontend` landing F sections render default thay vì tenant data

## Problem

Wave landing-100 ship FE đầy đủ (anti-fab empty-state, hero rework, theme safety, 4 section mới, SEO/OG) NHƯNG vài field per-tenant chưa có trong BE `LandingPageResponse` → FE đọc future-proof nhưng fallback default:

1. **`centerName`** (Bucket B): nav/footer + JsonLd ưu tiên `centerName` (tên trung tâm) thay vì `heroTitle` (slogan). LandingPageResponse CHƯA có field → fallback `heroTitle` (no regression nhưng chưa đúng intent).
2. **F-section fields** (Bucket F): `problemSolution.items` / `howItWorks.steps` / `trustStrip.signals` / `zaloUrl` — 4 section mới đọc slot data; LandingPageResponse chưa expose → sections render **default VN copy** (không bịa fake, nhưng không per-tenant) + FloatingCTA ẩn Zalo (thiếu zaloUrl).

KHÔNG phải build break — FE degrade an toàn (default/hide). Đây là data-binding completeness gap cần BE schema.

## Proposed Fix

kiteclass-core: thêm field vào LandingPage entity + Flyway migration + LandingPageResponse DTO + mapper (per `design-patterns.md` §3.12 triad):
- `center_name` VARCHAR
- `landing_zalo_url` VARCHAR
- problemSolution/howItWorks/trustStrip: jsonb HOẶC related tables (painPoints/steps/signals)

Sau BE: wire page.tsx slot map (`slots.problemSolution/howItWorks/trustStrip` + `landingData.zaloUrl`) từ response.

## Acceptance Criteria

- [x] LandingPageResponse có centerName + zaloUrl + F-section data (problemSolution/howItWorks/trustStrip) — entity + V95 migration + DTO + mapper + request DTO; `./mvnw compile` + LandingPageMapperTest/Sanitizer/ServiceTest PASS
- [x] page.tsx wire 4 F-section slots + zaloUrl từ response — problemSolution/howItWorks/trustStrip slots emitted only when BE non-empty; zaloUrl flows via TemplateRenderer→FloatingCTA; centerName via resolveCenterName; `pnpm --filter kiteclass-frontend build` PASS
- [x] G1 walk: 3 tenant demo-trio render F sections với per-tenant data (không default chung) — requires runtime browser walk against demo-trio seed

## Log

- **2026-06-11 (DONE):** AC#3 runtime closed — fix-pack PR #2326 seed F-section data per-tenant (Bucket B) + render verified live nip.io demo-trio (problemSolution/howItWorks/trustStrip hiển thị đúng audience, screenshots). AC#1 BE + AC#2 FE đã ship PR #2275.

## Related

- Discovered in: Wave landing-100 integration 2026-06-09 (Đợt 2 base-mismatch → F slots unwireable + BE field absent)
- Wave: landing-100 (FE deliverable DONE; BE field enrichment = this follow-up)
- Sibling: GAP-805 (seed demo-trio), GAP-828/595/596 (F sections shipped FE)
