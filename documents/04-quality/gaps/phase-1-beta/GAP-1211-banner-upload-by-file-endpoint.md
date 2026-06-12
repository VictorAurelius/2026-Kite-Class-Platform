# GAP-1211: Banner landing thiếu upload-by-file endpoint riêng — admin UI hiện add-by-URL

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Mixed
**Found:** 2026-06-11 (GAP-826 lớp 3 implementation — agent verify endpoint thật trước khi wire)
**Affects:** `kiteclass-core` settings/branding endpoints + `kiteclass-frontend/components/settings/landing-banner-settings.tsx`

## Problem

GAP-826 lớp 3 ship UI quản lý banner (list + add-by-URL + remove + reorder + save). Upload file trực tiếp CHƯA wire được vì BE chỉ có `/settings/branding/{logo|favicon}` với semantics **overwrite single-slot** — tái dùng cho banner sẽ clobber logo. Tenant thật muốn thêm banner phải có URL sẵn (MinIO/external) — friction.

## Proposed Fix

BE endpoint `POST /api/v1/settings/branding/banners` (multipart, mirror logo upload per GAP-804 contract: @RequestPart MultipartFile, MIME/size validation per pre-handoff §2.5) → store MinIO `banners/{uuid}` → trả stable URL (object key, per GAP-1204 không persist presigned). FE: file-picker trong landing-banner-settings → upload → append URL vào list.

## Acceptance Criteria

- [ ] Upload banner qua settings UI → xuất hiện trong list + render carousel
- [ ] MIME (image/*) + size limit validate server-side (415/413)
- [ ] URL stable (không presigned persist)
- [ ] Logo/favicon không bị ảnh hưởng

## Related

- Parent: GAP-826 (3 lớp — DONE với add-by-URL), GAP-804 (logo upload contract), GAP-1204 (stable URL), GAP-815 (editor UI tổng)

## Log

- **2026-06-12:** `WAVE_QUALITY_TARGET_DEFER: GAP-1211` — user-approved (AskUserQuestion) tách khỏi landing-100 closure. Lý do: endpoint + file-picker đã ship (90%); residual = runtime verify "upload qua settings UI → carousel" mà recipe G2★ landing chỉ walk landing render, không walk settings. Verify tự nhiên tại **branding-100 G2★** (settings/wizard walk) — gap add vào plan `wave-2026-06-11-branding-100.md` gaps list.
