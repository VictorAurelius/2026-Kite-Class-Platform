# GAP-1074: Session per-tab — mở URL ở tab mới bắt login lại (GAP-830 trade-off)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend (auth/security UX)
**Found:** 2026-06-08 (KC-1 G2 — user login 1 tab, mở URL tab khác → bắt login lại)
**Affects:** `kiteclass-frontend/src/lib/auth/jwt-storage.ts` + `stores/auth-store.ts` (sessionStorage)

## Problem

Login ở 1 tab; mở URL KiteClass ở tab khác → **bắt login lại**. Root cause: token lưu `sessionStorage` (per-tab, KHÔNG share cross-tab) — cố ý theo **GAP-830** (2 tab khác tenant clobber token nhau trong localStorage → cross-tenant leak). Trade-off "đóng tab = re-login" ghi "Acceptable" lúc đó. NHƯNG UX thật khó chịu cho owner mở link tab mới.

**Đây là DESIGN/SECURITY decision, không phải bug đơn thuần** — cần chốt trade-off.

## Proposed Fix (cần user chốt)

- **Option A (giữ nguyên):** sessionStorage per-tab — secure, re-login mỗi tab. Status quo.
- **Option B (khuyến nghị):** localStorage với key **scoped theo tenant** (`accessToken:<tenantId>`) — vừa cross-tab persist (hết re-login) vừa per-tenant isolated (giải đúng lo ngại GAP-830 mà không hy sinh UX). Cần refactor jwt-storage + auth-store + verify isolation 2-tenant.
- **Option C:** localStorage thuần (cross-tab) — bỏ GAP-830 isolation (regression security, KHÔNG khuyến nghị).

Security-sensitive → KHÔNG fix inline; scope proper sau khi user chốt option.

## Acceptance Criteria

- [ ] User chốt option (A/B/C)
- [ ] Nếu B: tenant-scoped localStorage + verify cross-tab persist + 2-tenant isolation không leak
- [ ] Re-walk: login tab 1 → mở tab 2 → không bắt login lại (nếu B/C)

## Related

- Discovered in: KC-1 G2 walk 2026-06-08
- GAP-830 (sessionStorage per-tab decision — nguồn trade-off)
- Security-sensitive → defer per small-gap-inline-fix §1 (c) low-risk FAIL
