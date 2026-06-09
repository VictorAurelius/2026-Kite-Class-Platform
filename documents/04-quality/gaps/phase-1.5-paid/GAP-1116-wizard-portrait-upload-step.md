# GAP-1116: AI Branding wizard thiếu bước upload chân dung (asset chính của banner; count theo user-type)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-10 (discuss wizard 6-bước với user — design critique câu 2)
**Affects:** `kitehub-frontend` branding wizard (`LogoStep` / bước asset) + `kitehub-branding` `AssetStorageController` / `BrandingAsset` (assetType)

## Problem

Wizard bước 2 (`LogoStep`) chỉ upload **logo**. Trong khi thiết kế banner là **3 lớp: text + chân dung + icon chủ đề** (per thesis banner design + `compose-sky-demo-banner.mjs`) → **chân dung là asset chính** để render banner, hiện wizard không thu thập.

Số lượng chân dung phụ thuộc **user-type** ([[GAP-1115]]): GV đơn lẻ = 1 người, trung tâm = nhiều người. Wizard chưa có cả input user-type lẫn bước upload chân dung → banner-compose ([[GAP-1117]]) thiếu asset đầu vào.

## Proposed Fix

1. Thêm **Portrait upload step** (sau Audience / user-type, trước/ghép Template) — upload 1..N ảnh chân dung; count tối đa gợi ý theo user-type ([[GAP-1115]]).
2. BE: lưu `BrandingAsset` với `assetType=PORTRAIT` (1..N per instance) — phối hợp dedup logic [[GAP-1112]] (replace-by-assetType cho LOGO vẫn giữ; PORTRAIT cho phép nhiều).
3. Portrait assets feed sang banner-compose ([[GAP-1117]]) làm lớp giữa.
4. Reuse asset picker ([[GAP-1112]] #3 pattern) cho portrait đã upload.

## Acceptance Criteria

- [ ] Portrait upload step trong wizard (1..N theo user-type)
- [ ] `BrandingAsset assetType=PORTRAIT` persisted (cho phép nhiều); IT verify
- [ ] Portrait feed sang banner render ([[GAP-1117]])
- [ ] Browser walk: solo = 1 portrait, center = nhiều — render đúng

## Related

- Discovered in: discuss wizard 6-bước 2026-06-10 (user design critique câu 2)
- Depends: [[GAP-1115]] (user-type quyết count)
- Feeds: [[GAP-1117]] (banner compose dùng portrait)
- Asset infra: [[GAP-1112]] (logo upload UX — dedup/preview/picker)
- Design: thesis banner 3-lớp + `compose-sky-demo-banner.mjs`, `ai-branding-guidelines.md` §4.1

## Log

- **2026-06-10:** Filed từ discuss wizard với user — banner cần chân dung (asset chính) nhưng wizard chỉ upload logo. Count theo user-type ([[GAP-1115]]). Per `discovery-to-gap-inline-filing.md`. GAP-ID từ block reserve 1115-1118.
