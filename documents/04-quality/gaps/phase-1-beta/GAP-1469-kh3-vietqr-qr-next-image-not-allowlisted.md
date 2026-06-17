# GAP-1469: KH-3 VietQR QR 400 — `img.vietqr.io` không trong `next.config` image allowlist

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P2
**Domain:** Frontend
**Found:** 2026-06-17 (KH-3 G2 walk — nâng gói subscription)
**Affects:** `kitehub-frontend` `next.config.js` + `components/billing/QRCodeDisplay.tsx`

## Problem

Khi nâng gói (KH-3 subscription upgrade), **mã QR VietQR không load** — console `image:1 → 400` (×3).

Root cause: `QRCodeDisplay.tsx` render QR bằng **Next.js `<Image>`** (`next/image`), nên src đi qua **Next image optimizer** `/_next/image?url=<qrUrl>`. Optimizer chỉ chấp nhận hostname có trong `next.config.js` `images.remotePatterns` — hiện chỉ có `*.amazonaws.com`, **thiếu `img.vietqr.io`** → optimizer trả **400** cho mọi QR URL trỏ `img.vietqr.io`.

Chain cụ thể:
- `VietQRService` primary path gọi `api.vietqr.io/v2/generate` (cần `VIETQR_API_KEY`). Local `.env` KHÔNG set key → fail → **fallback** trả `https://img.vietqr.io/image/<bank>-<account>-<template>.jpg?...` (URL này trả HTTP 200 thật khi load trực tiếp).
- FE đưa URL đó vào `<Image>` → optimizer chặn vì `img.vietqr.io` chưa allowlist → 400.

Vì sao KH-3 PASS 2026-06-09: lúc đó `VIETQR_API_KEY` được set → primary path trả `data:` URL (Next `<Image>` pass-through data URL, không cần allowlist) → QR render OK. Khi rơi vào fallback remote URL thì lộ bug allowlist.

## Proposed Fix

Allowlist 2 host QR vào `next.config.js` `images.remotePatterns` của `kitehub-frontend`:
- **`placehold.co`** — MOCK QR local dev. `VietQRService` có `payment.vietqr.mock-mode` mặc định `true` → trả `https://placehold.co/300x300/...?text=MOCK+QR...` (`VietQRService.java:89`). **Đây là nguyên nhân 400 THẬT trong local walk** (`_next/image?url=placehold.co...` → optimizer 400).
- **`img.vietqr.io`** — fallback QR thật khi `mock-mode=false` + không có API key (production path).

(Primary VietQR API data-URL path không cần allowlist.) Disable mock: set `payment.vietqr.mock-mode=false` + `VIETQR_*` config để dùng QR thật.

## Acceptance Criteria

- [ ] QR VietQR render được trong flow nâng gói khi BE rơi vào fallback `img.vietqr.io` URL (không còn `image:1 → 400`).
- [ ] `next.config.js` `images.remotePatterns` chứa `img.vietqr.io`.
- [ ] G2 re-walk: mở trang thanh toán PENDING → QR hiển thị (sau coordinator rebuild kitehub-frontend).

## Related

- Discovered: phiên 2026-06-17 KH-3 G2 walk
- `VietQRService.java` (primary api.vietqr.io + fallback img.vietqr.io, GAP-1361 circuit-breaker fallback)
- `kitehub-kiteclass-boundary.md` §2 — KH-3 subscription = kitehub-frontend `:3001`
- Follow-up (không thuộc gap này): local `.env` set `VIETQR_API_KEY` + real bank account để dùng primary data-URL path như walk 2026-06-09.
