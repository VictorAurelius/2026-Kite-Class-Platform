# GAP-1199: Middleware redirect loop vô hạn trên /suspended (tenant SUSPENDED)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-11 (landing-100 G2★ nip.io walk — agent-side catalog)
**Affects:** `kiteclass/kiteclass-frontend/src/middleware.ts` — mọi tenant SUSPENDED truy cập qua subdomain

## Problem

Khi tenant SUSPENDED truy cập qua subdomain Host (vd `sky-edu-test.127.0.0.1.nip.io:3000`), middleware redirect 307 → `/suspended?slug=...&status=suspended` — ĐÚNG. Nhưng request tới chính `/suspended` cũng đi qua middleware (matcher không loại trừ), resolve tenant từ Host vẫn là SUSPENDED → `TenantSuspendedError` → redirect 307 → `/suspended` lần nữa → **loop vô hạn**, browser hiện ERR_TOO_MANY_REDIRECTS. User của tenant suspended không bao giờ thấy trang thông báo thân thiện.

Bằng chứng walk 2026-06-11:

```
curl http://sky-edu-test.127.0.0.1.nip.io:3000/ → 307 → /suspended?slug=sky-edu-test&status=suspended
curl .../suspended?slug=sky-edu-test&status=suspended → 307 → chính nó (loop)
```

## Root Cause

`middleware.ts` catch `TenantSuspendedError` redirect không check pathname hiện tại — thiếu loop guard cho điểm đến của chính nó.

## Fix (shipped cùng PR này)

Loop guard trong catch: nếu `req.nextUrl.pathname === '/suspended'` → `NextResponse.next()` (pass through, page render). Regression test thêm vào `src/__tests__/middleware.test.ts` ("passes through on /suspended itself — no redirect loop").

## Acceptance Criteria

- [x] `curl .../suspended?slug=...` qua Host suspended trả 200 (không 307)
- [x] Happy redirect lần đầu (`/` → `/suspended`) vẫn hoạt động (test cũ giữ PASS)
- [x] Unit test loop-guard thêm + PASS

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3)

Xem PR body — re-walk nip.io sau rebuild: suspended tenant → 307 đúng 1 lần → trang `/suspended` render 200.

## Related

- Discovered in: landing-100 G2★ pre-walk (wave/landing-100-g2-walk-2026-06-11)
- Sister: GAP-1077 (middleware port), GAP-811 (FE middleware host→tenant)
- Cross-flow sweep: chỉ 1 redirect site trong middleware.ts (grep `NextResponse.redirect` = 1) — không có sister site

## Log

- **2026-06-11 (DONE):** Fix shipped + merged PR #2326 (squash f637b1bad) — loop-guard middleware `/suspended` + unit test; re-walk nip.io PASS (307 đúng 1 lần → render 200). Log entry bổ sung post-merge per audit-gate doc-drift flag (sync PR này).
