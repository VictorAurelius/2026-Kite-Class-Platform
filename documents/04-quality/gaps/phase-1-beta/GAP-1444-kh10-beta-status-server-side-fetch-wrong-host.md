# GAP-1444: beta-status FE server-side fetch sai host → fallback vĩnh viễn "Không tải được nội dung trạng thái BE"

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-10)
**Affects:** KH-10 — `kitehub-frontend/src/lib/api/beta-status.ts` + `(public)/beta-status/page.tsx` server-side fetch base URL

## Problem
Discovered Phase-2 browser walk KH-10. Endpoint trả 200 qua gateway `:9000` nhưng Next.js server-side fetch resolve sai host (`curl :3001/api/v1/beta-status` → 404) → trang luôn rơi vào fallback "Không tải được nội dung trạng thái BE", user thấy changelog cũ, không bao giờ thấy live status. Thuộc class env-coverage GAP-802/803.

## Proposed Fix
Cấu hình internal API base URL cho Next.js server-side fetch trỏ tới gateway ở local/prod (mirror env-coverage pattern).

## Acceptance Criteria
- [x] Trang beta-status server-side fetch reach gateway → render live status (không fallback) — `beta-status.ts` server/browser base-URL split (`INTERNAL_API_URL` server-side, mirror kiteclass `public.ts`). Local runtime confirm pending walk.
- [x] Env base URL cho server-side fetch document đủ local + prod — compose `kitehub-frontend` env `INTERNAL_API_URL=http://kite-gateway:9000` + `.env.docker.example` documented + prod PM2 `pm2-ecosystem.config.js` (KH+KC) `https://api.kitehub.me` (live-verify GAP-1455).

## Fix (Phase-3 coordinator inline, 2026-06-16)
- `kitehub-frontend/.../lib/api/beta-status.ts` — `resolveBaseUrl()` server/browser split (mirror `kiteclass-frontend/.../lib/api/public.ts`).
- `kitehub/docker-compose.kitehub.yml` — `kitehub-frontend` env `INTERNAL_API_URL: http://kite-gateway:9000`.
- `kitehub-frontend/.env.docker.example` — documented `INTERNAL_API_URL`.
- Prod parity (`local-fix-production-parity-check.md`): `infrastructure/fe-host/pm2-ecosystem.config.js` — `INTERNAL_API_URL=https://api.kitehub.me` cho cả kitehub-frontend + kiteclass-frontend (latent class chung — KC dùng INTERNAL_API_URL trong code nhưng PM2 chưa set).
- Build: `pnpm build` kitehub-frontend exit 0.
- Status PARTIAL: code + config shipped; local SSR runtime + prod live-verify (AWS restore-blocked) pending → GAP-1455.

## Related
- Discovered in: Phase-2 browser walk (flow KH-10), 2026-06-16
- Env-coverage class: GAP-802 / GAP-803
- Prod live-verify follow-up: GAP-1455
- Fixed in: Wave flow-fix-1 Phase-3 (coordinator inline)
