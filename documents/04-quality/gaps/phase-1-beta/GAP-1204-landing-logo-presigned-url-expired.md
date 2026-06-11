# GAP-1204: Landing logo dùng presigned MinIO URL hết hạn persist trong DB — logo vỡ trên landing public

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-11 (landing-100 UI review — header sky-education logo broken-image icon)
**Affects:** `landing_pages.logo_url` (kiteclass_shared) + flow apply branding → landing

## Problem

Landing sky-education render broken-image icon ở header. `landing_pages.logo_url` = presigned MinIO URL (`http://localhost:9100/kite-branding-assets/...?X-Amz-Expires=604800&X-Amz-Date=20260529...`) — ký 2026-05-29, expiry 7 ngày → **hết hạn 06-05**, mọi request sau đó 403 → logo vỡ vĩnh viễn cho tới khi re-apply branding.

Đây là **sister instance của GAP-1072** (DONE — Settings không render logo + presigned hết hạn) trên surface khác: GAP-1072 fix surface Settings (KC-10), nhưng landing_pages.logo_url persisted presigned URL chưa được sweep (cross-flow-bug-class-sweep miss — bug class "persist presigned URL có TTL vào DB" còn site landing).

## Proposed Fix

Không persist presigned URL: lưu object key (hoặc stable proxy URL `/api/v1/branding/assets/...`) vào `landing_pages.logo_url`; FE/BE resolve presigned lúc serve (hoặc public-read bucket cho landing assets). Sweep mọi cột *_url khác đang giữ presigned (`hero_image_url`, branding assets).

## Acceptance Criteria

- [ ] `landing_pages.logo_url` không còn chứa `X-Amz-Signature`
- [ ] Logo sky render sau >7 ngày không cần re-apply
- [ ] Sweep evidence các cột URL khác (per cross-flow-bug-class-sweep §3)

## Related

- Sister: GAP-1072 (Settings surface — DONE), GAP-1198 (CSP img-src MinIO)
- Discovered in: landing-100 G2★ UI review (PR #2326 session)
