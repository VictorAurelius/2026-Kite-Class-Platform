# GAP-1444: beta-status FE server-side fetch sai host → fallback vĩnh viễn "Không tải được nội dung trạng thái BE"

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-10)
**Affects:** KH-10 — `kitehub-frontend/src/lib/api/beta-status.ts` + `(public)/beta-status/page.tsx` server-side fetch base URL

## Problem
Discovered Phase-2 browser walk KH-10. Endpoint trả 200 qua gateway `:9000` nhưng Next.js server-side fetch resolve sai host (`curl :3001/api/v1/beta-status` → 404) → trang luôn rơi vào fallback "Không tải được nội dung trạng thái BE", user thấy changelog cũ, không bao giờ thấy live status. Thuộc class env-coverage GAP-802/803.

## Proposed Fix
Cấu hình internal API base URL cho Next.js server-side fetch trỏ tới gateway ở local/prod (mirror env-coverage pattern).

## Acceptance Criteria
- [ ] Trang beta-status server-side fetch reach gateway → render live status (không fallback)
- [ ] Env base URL cho server-side fetch document đủ local + prod

## Related
- Discovered in: Phase-2 browser walk (flow KH-10), 2026-06-16
- Env-coverage class: GAP-802 / GAP-803
