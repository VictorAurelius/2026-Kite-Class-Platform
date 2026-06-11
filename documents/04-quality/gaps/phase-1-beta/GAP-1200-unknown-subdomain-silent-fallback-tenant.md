# GAP-1200: Subdomain không tồn tại render thầm lặng landing của tenant fallback

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-11 (landing-100 G2★ nip.io walk — sad path catalog; trùng FM-3 pre-walk simulation)
**Affects:** `kiteclass/kiteclass-frontend/src/app/(public)/page.tsx` + `(public)/layout.tsx` fallback chain

## Problem

Truy cập subdomain không tồn tại (`khong-ton-tai.127.0.0.1.nip.io:3000`) → middleware resolve 404 → pass through không inject header (by design) → NHƯNG `page.tsx`/`layout.tsx` fallback chain (`?tenant=` → `x-tenant-id` header → `NEXT_PUBLIC_TENANT_ID` env → default UUID cứng) render **trọn vẹn landing của tenant khác** (hiện tại: Sky "Mất gốc tiếng Anh? Đã có cô Khánh"). HTTP 200, không error.

Hệ quả production: gõ sai subdomain → thấy trường khác (sai brand, confusing, có thể coi là content leak nhẹ). Pre-walk simulation FM-3 gọi đây là "green-but-wrong" — mọi resolve fail đều render trang bình thường nhưng sai tenant, che lấp lỗi resolution.

## Design context (per design-first-investigation-order)

`tenant-domain-landing-architecture.md` §7.2: fallback `NEXT_PUBLIC_TENANT_ID` là intended cho mode 1-tenant-per-deploy; middleware doc row "unknown.kiteclass.com → Pass through (let app render generic 404 / fallback)". Design nói "generic 404 / fallback" nhưng impl chỉ có "fallback" — nhánh "generic 404 cho multi-tenant-by-Host mode" chưa tồn tại.

## Proposed Fix

Khi request có Host subdomain hợp lệ (middleware đã thử resolve) nhưng resolve 404 → middleware inject header đánh dấu (vd `x-tenant-not-found: <slug>`) → page render trang "Không tìm thấy trung tâm" (branded KiteClass, gợi ý kiểm tra URL) thay vì fallback tenant. Giữ fallback env cho mode localhost/1-tenant-per-deploy.

## Acceptance Criteria

- [ ] Subdomain không tồn tại → trang "không tìm thấy trung tâm" (không render tenant khác)
- [ ] `localhost:3000` không subdomain → giữ behavior fallback hiện tại (dev mode)
- [ ] Unit test middleware + page cho nhánh not-found

## Related

- Discovered in: landing-100 G2★ pre-walk + simulation artifact `documents/04-quality/audits/persona-review/2026-06-11-pre-walk-landing-100-g2-nipio.md` (FM-3)
- Sister: GAP-1199 (suspended loop — cùng walk), GAP-811/1077 (middleware)
