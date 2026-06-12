# GAP-1238: CSP /preview gate bằng isDev — local Docker (production build) chặn iframe wizard :3001

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (fix-before-walk)
**Domain:** Frontend
**Found:** 2026-06-12 (pre-walk persona simulation #1 — `2026-06-12-pre-walk-branding-100-fullai.md`)
**Affects:** `kiteclass-frontend/next.config.js` — frame-ancestors `/preview`

## Problem

#2363 gate origin wizard bằng `isDev = NODE_ENV !== 'production'`. Container local Docker chạy production build → `isDev=false` → `frame-ancestors` loại `http://localhost:3001` → wizard nhúng iframe `/preview` bị browser CSP chặn → WYSIWYG preview trống. Blocker chung cả 2 nhánh TEMPLATE + FULL_AI khi G1/G2 walk local.

## Fix shipped (cùng PR)

`KITEHUB_WIZARD_ORIGIN ?? 'http://localhost:3001'` — env-driven thay vì suy từ NODE_ENV; default phủ local Docker + dev; production AWS set `''` để tắt (chỉ còn kitehub.me). Dùng `??` để empty-string disable được.

## Acceptance Criteria

- [x] `pnpm --filter kiteclass-frontend build` PASS với fix
- [x] frame-ancestors chứa `localhost:3001` khi env không set (default) — verify runtime curl ở pre-walk checklist B
- [x] Production có cơ chế tắt (env `''`)

## Related

- Pre-walk sim artifact (#2365) failure mode #1 · GAP-1215 (#2363 nguồn isDev gate) · per small-gap-inline-fix
