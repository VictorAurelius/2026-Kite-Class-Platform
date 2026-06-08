# GAP-1065: Zalo OA ID env-var inconsistency — 2 site hardcode + thiếu template entry

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-08 (Zalo OA Option A activation prep — discovery khi verify code wiring)
**Affects:** `kitehub/kitehub-frontend/src/app/(public)/LandingShellSSR.tsx`, `kitehub/kitehub-frontend/src/app/(public)/waitlist/page.tsx`, `kitehub/kitehub-frontend/.env.example`

## Problem

GAP-660 (Wave 98 DONE) ship passive CTA Zalo OA qua env var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` (fallback `'kitehub'`). Khi chuẩn bị activate OA thật (Option A — set env var với OA ID), phát hiện 2 vấn đề env-var consistency:

1. **2 site hardcode `https://zalo.me/kitehub`** (bỏ qua env var) → set env var xong 2 chỗ này vẫn trỏ placeholder, 404 sau khi OA active:
   - `LandingShellSSR.tsx:192` — footer landing, có text "Zalo OA: zalo.me/kitehub (chờ kích hoạt)"
   - `waitlist/page.tsx:111` — nút CTA waitlist
2. **`NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` KHÔNG khai báo trong `.env.example`** — runbook §4.1 nói có trong `.env.production.template` nhưng thực tế không có template entry nào → operator không biết phải set var này.

Sister-site bypass điển hình (kiểu `cross-flow-bug-class-sweep.md`): pattern env-var-driven áp dụng đúng cho `SupportMenu.tsx` + `Footer.tsx` nhưng 2 site landing/waitlist hardcode → khi GAP-660 ship đã miss sweep 2 chỗ này.

## Proposed Fix

1. Thay hardcode `https://zalo.me/kitehub` ở 2 file bằng pattern env-var-driven (giống `Footer.tsx:22`): `process.env.NEXT_PUBLIC_KITEHUB_ZALO_OA_ID ?? 'kitehub'`. Cân nhắc extract shared helper `lib/zalo.ts` (4 site cùng pattern) — defer nếu muốn minimal.
2. Thêm `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID=kitehub` (placeholder) vào `kitehub/kitehub-frontend/.env.example` với comment.
3. Verify `pnpm --filter kitehub-frontend build` PASS (per `fe-build-local-verify.md`).

## Acceptance Criteria

- [x] `LandingShellSSR.tsx` + `waitlist/page.tsx` đọc env var thay vì hardcode `zalo.me/kitehub`
- [x] `.env.example` khai báo `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID`
- [x] `pnpm --filter kitehub-frontend build` PASS local (exit 0, 2026-06-08)
- [x] Sweep xác nhận không còn site nào hardcode `zalo.me/kitehub` (grep clean)

## FE build local-verify (per fe-build-local-verify.md §3)

`pnpm --filter kitehub-frontend build` PASS local trước push (exit 0, 2026-06-08).
Grep sweep `zalo.me/kitehub` trong `src/**` → CLEAN (chỉ còn pattern env-driven `process.env.NEXT_PUBLIC_KITEHUB_ZALO_OA_ID`).

**Lưu ý:** Fix này chỉ đảm bảo code đọc env var đúng. OA "active" thật (zalo.me/<id> → 200) phụ thuộc bước tạo OA của dev + set `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` với OA ID thật (tracked trong runbook account-prep/zalo-oa-setup-runbook.md, parent GAP-660).

## Related

- Discovered in: session 2026-06-08 (Zalo OA Option A activation prep)
- Parent: GAP-660 (Zalo OA passive CTA Wave 98 DONE) — this gap fixes 2 sites missed in GAP-660 sweep
- Setup runbook: `documents/05-guides/account-prep/zalo-oa-setup-runbook.md` §4.1 (env var convention — claim `.env.production.template` chưa đúng)
- Rule: `cross-flow-bug-class-sweep.md` (sister-site sweep miss), `fe-build-local-verify.md` (build verify before push)
