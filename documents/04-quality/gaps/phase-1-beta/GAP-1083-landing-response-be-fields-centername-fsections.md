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

- [ ] LandingPageResponse có centerName + zaloUrl + F-section data
- [ ] page.tsx wire 4 F-section slots + zaloUrl từ response
- [ ] G1 walk: 3 tenant demo-trio render F sections với per-tenant data (không default chung)

## Related

- Discovered in: Wave landing-100 integration 2026-06-09 (Đợt 2 base-mismatch → F slots unwireable + BE field absent)
- Wave: landing-100 (FE deliverable DONE; BE field enrichment = this follow-up)
- Sibling: GAP-805 (seed demo-trio), GAP-828/595/596 (F sections shipped FE)
